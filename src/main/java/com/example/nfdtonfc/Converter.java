package com.example.nfdtonfc;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
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
            return linker.downcallHandle(
                linker.defaultLookup().find("rename").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
        } catch (Throwable e) {
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

        if (originalName.equals(normalizedName)) {
            return;
        }

        Path target = path.resolveSibling(normalizedName);

        if (Files.exists(target) && !Files.isSameFile(path, target)) {
            ConflictResolver.Action action = resolver.resolve(normalizedName);
            if (action == ConflictResolver.Action.OVERWRITE) {
                rename(path, target);
                System.out.println("Renamed (overwritten): " + originalName + " -> " + normalizedName);
            } else {
                System.out.println("Skipped: " + originalName);
            }
        } else {
            rename(path, target);
            System.out.println("Renamed: " + originalName + " -> " + normalizedName);
        }
    }

    private void rename(Path source, Path target) throws IOException {
        if (IS_MAC && RENAME_HANDLE != null) {
            renameDirect(source, target);
        } else if (Files.exists(target)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(source, target);
        }
    }

    private void renameDirect(Path source, Path target) throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            byte[] srcBytes = source.toString().getBytes(StandardCharsets.UTF_8);
            byte[] dstBytes = target.toString().getBytes(StandardCharsets.UTF_8);

            MemorySegment srcSeg = arena.allocate(srcBytes.length + 1);
            MemorySegment dstSeg = arena.allocate(dstBytes.length + 1);

            srcSeg.asByteBuffer().put(srcBytes).put((byte) 0);
            dstSeg.asByteBuffer().put(dstBytes).put((byte) 0);

            int result = (int) RENAME_HANDLE.invoke(srcSeg, dstSeg);
            if (result != 0) {
                throw new IOException("rename() failed for: " + source);
            }
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException("rename() error: " + e.getMessage(), e);
        }
    }
}
