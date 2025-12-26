package com.demo.adventure.engine.command;

public record CommandPhrase(String raw, String target, String preposition, String object) {
    public static CommandPhrase empty() {
        return new CommandPhrase("", "", null, null);
    }
}
