package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("--version")) {
            String version = Main.class.getPackage().getImplementationVersion();
            System.out.println("nfdtonfc " + (version != null ? version : "unknown"));
            System.exit(0);
        }

        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            System.out.println("Usage: nfdtonfc [-f | --force] <file-or-directory> [file-or-directory ...]");
            System.out.println();
            System.out.println("Convert filenames from NFD to NFC Unicode normalization.");
            System.out.println();
            System.out.println("Arguments:");
            System.out.println("  <file-or-directory>  One or more files or directories to process.");
            System.out.println("                       Directories are processed recursively.");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  -f, --force          Overwrite existing files without prompting.");
            System.out.println("  --help               Show this help message and exit.");
            System.out.println("  --version            Show version and exit.");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("  nfdtonfc file.txt");
            System.out.println("  nfdtonfc ~/Downloads");
            System.out.println("  nfdtonfc --force dir1 dir2");
            System.exit(args.length == 0 ? 1 : 0);
        }

        boolean force = false;
        List<String> targets = new ArrayList<>();
        for (String arg : args) {
            if (arg.equals("-f") || arg.equals("--force")) {
                force = true;
            } else {
                targets.add(arg);
            }
        }

        if (targets.isEmpty()) {
            System.err.println("Error: no file or directory specified.");
            System.exit(1);
        }

        Scanner scanner = force ? null : new Scanner(System.in);

        for (String arg : targets) {
            Path path = Path.of(arg);
            if (!Files.exists(path)) {
                System.err.println("Not found: " + arg);
                continue;
            }
            if (Files.isDirectory(path)) {
                convertDirectory(path, force, scanner);
            } else {
                convertFile(path, force, scanner);
            }
        }

        if (scanner != null) {
            scanner.close();
        }
    }

    private static void convertDirectory(Path dir, boolean force, Scanner scanner) throws IOException {
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> paths = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList();
            for (Path path : paths) {
                convertFile(path, force, scanner);
            }
        }
    }

    private static void convertFile(Path path, boolean force, Scanner scanner) throws IOException {
        String originalName = path.getFileName().toString();
        String normalizedName = Normalizer.normalize(originalName, Normalizer.Form.NFC);

        if (originalName.equals(normalizedName)) {
            return;
        }

        Path target = path.resolveSibling(normalizedName);

        if (Files.exists(target)) {
            if (force) {
                Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Renamed (overwritten): " + originalName + " -> " + normalizedName);
            } else {
                System.out.print("'" + normalizedName + "' already exists. Overwrite? [y/N] ");
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("y") || input.equals("yes")) {
                    Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Renamed (overwritten): " + originalName + " -> " + normalizedName);
                } else {
                    System.out.println("Skipped: " + originalName);
                }
            }
        } else {
            Files.move(path, target);
            System.out.println("Renamed: " + originalName + " -> " + normalizedName);
        }
    }
}
