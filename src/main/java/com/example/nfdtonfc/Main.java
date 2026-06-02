package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    static void main(String[] args) throws IOException {
        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            System.out.println("Usage: nfdtonfc <file-or-directory> [file-or-directory ...]");
            System.out.println();
            System.out.println("Convert filenames from NFD to NFC Unicode normalization.");
            System.out.println();
            System.out.println("Arguments:");
            System.out.println("  <file-or-directory>  One or more files or directories to process.");
            System.out.println("                       Directories are processed recursively.");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --help               Show this help message and exit.");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("  nfdtonfc file.txt");
            System.out.println("  nfdtonfc ~/Downloads");
            System.out.println("  nfdtonfc dir1 dir2 file.txt");
            System.exit(args.length == 0 ? 1 : 0);
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
