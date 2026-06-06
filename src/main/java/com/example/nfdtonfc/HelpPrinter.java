package com.example.nfdtonfc;

public class HelpPrinter {

    private HelpPrinter() {}

    public static void printHelp() {
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

    public static void printVersion() {
        String version = HelpPrinter.class.getPackage().getImplementationVersion();
        System.out.println("nfdtonfc " + (version != null ? version : "unknown"));
    }
}
