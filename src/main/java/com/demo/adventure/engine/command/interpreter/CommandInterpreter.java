package com.demo.adventure.engine.command.interpreter;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandNode;
import com.demo.adventure.engine.command.TokenType;

import java.util.Map;

public final class CommandInterpreter {

    private final CommandCompiler compiler = new CommandCompiler();

    public void setExtraKeywords(Map<String, TokenType> extraKeywords) {
        compiler.setExtraKeywords(extraKeywords);
    }

    public Command interpret(String input) {
        return interpret(compiler.compile(input));
    }

    public Command interpret(CommandNode node) {
        if (node == null) {
            return Command.unknown();
        }
        if (node instanceof CommandNode.Verb verb) {
            return Command.from(verb.action(), verb.phrase());
        }
        if (node instanceof CommandNode.Error error) {
            return Command.error(error.error());
        }
        return Command.unknown();
    }
}
