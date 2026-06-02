package com.example.nfdtonfc;

import java.util.ArrayList;
import java.util.List;

public class Options {

    public final boolean force;
    public final List<String> targets;

    private Options(boolean force, List<String> targets) {
        this.force = force;
        this.targets = targets;
    }

    public static Options parse(String[] args) {
        boolean force = false;
        List<String> targets = new ArrayList<>();
        for (String arg : args) {
            if (arg.equals("-f") || arg.equals("--force")) {
                force = true;
            } else {
                targets.add(arg);
            }
        }
        return new Options(force, targets);
    }
}
