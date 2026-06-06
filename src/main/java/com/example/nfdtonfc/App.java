package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class App {

    private final String[] args;

    public App(String[] args) {
        this.args = args;
    }

    public void run() throws IOException {
        if (args.length == 1 && args[0].equals("--version")) {
            HelpPrinter.printVersion();
            System.exit(0);
        }

        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            HelpPrinter.printHelp();
            System.exit(args.length == 0 ? 1 : 0);
        }

        Options options = Options.parse(args);

        if (options.targets.isEmpty()) {
            System.err.println("Error: no file or directory specified.");
            System.exit(1);
        }

        convert(options);
    }

    private void convert(Options options) throws IOException {
        Scanner scanner = options.force ? null : new Scanner(System.in);
        ConflictResolver resolver = new ConflictResolver(options.force, scanner);
        Converter converter = new Converter(resolver);

        for (String arg : options.targets) {
            List<Path> paths = expandGlob(arg);
            if (paths.isEmpty()) {
                System.err.println("Not found: " + arg);
                continue;
            }
            for (Path path : paths) {
                converter.convert(path);
            }
        }

        if (scanner != null) {
            scanner.close();
        }
    }

    private List<Path> expandGlob(String arg) throws IOException {
        Path argPath = Path.of(arg);

        if (Files.exists(argPath)) {
            return List.of(argPath);
        }

        // glob 패턴 처리
        Path parent = argPath.getParent();
        String pattern = argPath.getFileName().toString();
        Path baseDir = parent != null ? parent : Path.of(".");

        if (!Files.isDirectory(baseDir)) {
            return List.of();
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<Path> matched = new ArrayList<>();
        try (Stream<Path> stream = Files.list(baseDir)) {
            stream.filter(p -> matcher.matches(p.getFileName()))
                  .sorted()
                  .forEach(matched::add);
        }
        return matched;
    }
}
