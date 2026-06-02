package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: nfdtonfc <file-or-directory> [file-or-directory ...]");
            System.exit(1);
        }

        for (String arg : args) {
            Path path = Path.of(arg);
            if (!Files.exists(path)) {
                System.err.println("Not found: " + arg);
                continue;
            }
            if (Files.isDirectory(path)) {
                convertDirectory(path);
            } else {
                convertFile(path);
            }
        }
    }

    private static void convertDirectory(Path dir) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path path : paths) {
                convertFile(path);
            }
        }
    }

    private static void convertFile(Path path) throws IOException {
        String originalName = path.getFileName().toString();
        String normalizedName = Normalizer.normalize(originalName, Normalizer.Form.NFC);

        if (originalName.equals(normalizedName)) {
            return;
        }

        Path target = path.resolveSibling(normalizedName);
        Files.move(path, target);
        System.out.println("Renamed: " + originalName + " -> " + normalizedName);
    }
}
