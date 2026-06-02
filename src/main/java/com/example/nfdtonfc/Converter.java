package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Stream;

public class Converter {

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

        if (Files.exists(target)) {
            ConflictResolver.Action action = resolver.resolve(normalizedName);
            if (action == ConflictResolver.Action.OVERWRITE) {
                Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Renamed (overwritten): " + originalName + " -> " + normalizedName);
            } else {
                System.out.println("Skipped: " + originalName);
            }
        } else {
            Files.move(path, target);
            System.out.println("Renamed: " + originalName + " -> " + normalizedName);
        }
    }
}
