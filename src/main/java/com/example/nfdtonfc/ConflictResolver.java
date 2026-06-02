package com.example.nfdtonfc;

import java.util.Scanner;

public class ConflictResolver {

    public enum Action { OVERWRITE, SKIP }

    private boolean forceAll;
    private boolean skipAll;
    private final Scanner scanner;

    public ConflictResolver(boolean forceAll, Scanner scanner) {
        this.forceAll = forceAll;
        this.scanner = scanner;
    }

    public Action resolve(String conflictingName) {
        if (forceAll) {
            return Action.OVERWRITE;
        }
        if (skipAll) {
            return Action.SKIP;
        }

        System.out.println("'" + conflictingName + "' already exists.");
        System.out.println("  y  Overwrite this file");
        System.out.println("  n  Skip this file");
        System.out.println("  a  Overwrite this and all remaining conflicts");
        System.out.println("  s  Skip this and all remaining conflicts");
        System.out.print("Overwrite? [y/n/a/s] ");

        String input = scanner.nextLine().trim().toLowerCase();
        switch (input) {
            case "y" -> { return Action.OVERWRITE; }
            case "a" -> { forceAll = true; return Action.OVERWRITE; }
            case "s" -> { skipAll = true; return Action.SKIP; }
        }
        return Action.SKIP;
    }
}
