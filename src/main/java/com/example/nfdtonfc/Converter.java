package com.example.nfdtonfc;

import java.io.IOException;
import java.util.Arrays;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Stream;

public class Converter {

    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private static final MethodHandle RENAME_HANDLE = initRenameHandle();

    private static MethodHandle initRenameHandle() {
        if (!IS_MAC) {
            return null;
        }
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup libc = SymbolLookup.libraryLookup("libSystem.B.dylib", Arena.global());
            return linker.downcallHandle(
                libc.find("rename").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
        } catch (Throwable e) {
            System.err.println("[nfdtonfc] Warning: failed to load rename() from libSystem: " + e.getMessage());
            return null;
        }
    }

    private final ConflictResolver resolver;

    public Converter(ConflictResolver resolver) {
        this.resolver = resolver;
    }

    public void convert(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            convertDirectory(path);
        } else {
            convertFile(path);
        }
    }

    private void convertDirectory(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path path : paths) {
                convertFile(path);
            }
        }
    }

    private void convertFile(Path path) throws IOException {
        String originalName = path.getFileName().toString();
        String normalizedName = Normalizer.normalize(originalName, Normalizer.Form.NFC);

        if (needsNoConversion(originalName, normalizedName)) {
            return;
        }

        Path target = path.resolveSibling(normalizedName);

        if (Files.exists(target) && !Files.isSameFile(path, target)) {
            ConflictResolver.Action action = resolver.resolve(normalizedName);
            if (action == ConflictResolver.Action.OVERWRITE) {
                rename(path, normalizedName);
                System.out.println("Renamed (overwritten): " + originalName + " -> " + normalizedName);
            } else {
                System.out.println("Skipped: " + originalName);
            }
        } else {
            rename(path, normalizedName);
            System.out.println("Renamed: " + originalName + " -> " + normalizedName);
        }
    }

    private boolean needsNoConversion(String originalName, String normalizedName) {
        if (!IS_MAC) {
            return originalName.equals(normalizedName);
        }
        // macOS JVM은 파일명을 NFC로 정규화해서 반환하므로 문자열 비교로는 NFD 판별 불가
        // NFC와 NFD가 다른 경우(비ASCII)라면 on-disk가 NFD일 수 있으므로 항상 rename 시도
        byte[] nfcBytes = normalizedName.getBytes(StandardCharsets.UTF_8);
        byte[] nfdBytes = Normalizer.normalize(originalName, Normalizer.Form.NFD).getBytes(StandardCharsets.UTF_8);
        return Arrays.equals(nfcBytes, nfdBytes);
    }

    private void rename(Path source, String targetName) throws IOException {
        Path target = source.resolveSibling(targetName);
        if (IS_MAC && RENAME_HANDLE != null) {
            renameDirect(source, targetName);
        } else if (Files.exists(target)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(source, target);
        }
    }

    private void renameDirect(Path source, String targetName) throws IOException {
        String parentStr = source.getParent() != null ? source.getParent().toString() + "/" : "";
        String tempName = parentStr + ".nfdtonfc_tmp_" + System.nanoTime();
        String dstPath = parentStr + targetName;

        // APFS는 NFD↔NFC를 같은 파일로 취급해 직접 rename을 no-op으로 처리함
        // 임시 이름을 거치는 2단계 rename으로 우회: NFD -> temp -> NFC
        invokeRename(source.toString(), tempName);
        invokeRename(tempName, dstPath);
    }

    private void invokeRename(String src, String dst) throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            byte[] srcBytes = src.getBytes(StandardCharsets.UTF_8);
            byte[] dstBytes = dst.getBytes(StandardCharsets.UTF_8);

            MemorySegment srcSeg = arena.allocate(srcBytes.length + 1);
            MemorySegment dstSeg = arena.allocate(dstBytes.length + 1);

            srcSeg.asByteBuffer().put(srcBytes).put((byte) 0);
            dstSeg.asByteBuffer().put(dstBytes).put((byte) 0);

            int result = (int) RENAME_HANDLE.invoke(srcSeg, dstSeg);
            if (result != 0) {
                throw new IOException("rename() failed: " + src + " -> " + dst);
            }
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException("rename() error: " + e.getMessage(), e);
        }
    }
}
