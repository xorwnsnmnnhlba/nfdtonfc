package com.example.nfdtonfc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

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
