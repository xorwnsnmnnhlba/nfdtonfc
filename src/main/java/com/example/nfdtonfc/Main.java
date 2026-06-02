package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

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

        Options options = Options.parse(args);

        if (options.targets.isEmpty()) {
            System.err.println("Error: no file or directory specified.");
            System.exit(1);
        }

        Scanner scanner = options.force ? null : new Scanner(System.in);
        ConflictResolver resolver = new ConflictResolver(options.force, scanner);
        Converter converter = new Converter(resolver);

        for (String arg : options.targets) {
            Path path = Path.of(arg);
            if (!Files.exists(path)) {
                System.err.println("Not found: " + arg);
                continue;
            }
            converter.convert(path);
        }

        if (scanner != null) {
            scanner.close();
        }
    }
}
