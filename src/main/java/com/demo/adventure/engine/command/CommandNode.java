package com.demo.adventure.engine.command;

public sealed interface CommandNode permits
        CommandNode.Verb,
        CommandNode.Error,
        CommandNode.Unknown {

    record Verb(CommandAction action, CommandPhrase phrase) implements CommandNode {
    }

    record Error(CommandParseError error) implements CommandNode {
    }

    record Unknown() implements CommandNode {
    }
}
