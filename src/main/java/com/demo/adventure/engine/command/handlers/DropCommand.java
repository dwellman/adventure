package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class DropCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.DROP;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.drop(command.target());
        return CommandOutcome.none();
    }
}
