package com.example.nfdtonfc;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ConsoleEncoder.init();
        new App(args).run();
    }
}
