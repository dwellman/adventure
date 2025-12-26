package com.demo.adventure.engine.command;

public interface CommandOutput {
    void emit(String text);

    void printHelp();
}
