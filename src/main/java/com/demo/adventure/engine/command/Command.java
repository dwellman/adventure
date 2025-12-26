package com.demo.adventure.engine.command;

public record Command(
        CommandAction action,
        String argument,
        String target,
        String preposition,
        String object,
        CommandParseError error
) {
    public static Command from(CommandAction action, CommandPhrase phrase) {
        CommandPhrase safePhrase = phrase == null ? CommandPhrase.empty() : phrase;
        return new Command(
                action,
                safePhrase.raw(),
                safePhrase.target(),
                safePhrase.preposition(),
                safePhrase.object(),
                null
        );
    }

    public static Command unknown() {
        return new Command(CommandAction.UNKNOWN, "", "", null, null, null);
    }

    public static Command error(CommandParseError error) {
        return new Command(CommandAction.UNKNOWN, "", "", null, null, error);
    }

    public boolean hasError() {
        return error != null;
    }
}
