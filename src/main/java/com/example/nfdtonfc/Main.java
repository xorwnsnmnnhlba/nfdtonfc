package com.example.nfdtonfc;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        initEncoding();

        if (args.length == 1 && args[0].equals("--version")) {
            printVersion();
            System.exit(0);
        }

        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            printHelp();
            System.exit(args.length == 0 ? 1 : 0);
        }

        Options options = Options.parse(args);

        if (options.targets.isEmpty()) {
            System.err.println("Error: no file or directory specified.");
            System.exit(1);
        }

        run(options);
    }

    private static void initEncoding() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            setWindowsConsoleUtf8();
        }
    }

    private static void setWindowsConsoleUtf8() {
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
            MethodHandle setConsoleOutputCP = linker.downcallHandle(
                kernel32.find("SetConsoleOutputCP").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
            );
            setConsoleOutputCP.invoke(65001);
        } catch (Throwable ignored) {}
    }

    private static void printVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        System.out.println("nfdtonfc " + (version != null ? version : "unknown"));
    }

    private static void printHelp() {
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
    }

    private static void run(Options options) throws IOException {
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
