package com.example.nfdtonfc;

import java.io.PrintStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

public class ConsoleEncoder {

    private ConsoleEncoder() {}

    public static void init() {
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
}
