package com.demo.adventure.engine.command;

public final class CommandOutputs {
    private static final CommandOutput NOOP = new CommandOutput() {
        @Override
        public void emit(String text) {
            // no-op
        }

        @Override
        public void printHelp() {
            // no-op
        }
    };

    private CommandOutputs() {
    }

    public static CommandOutput noop() {
        return NOOP;
    }
}
