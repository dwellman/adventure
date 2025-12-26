package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class LookCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.LOOK;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        if (command.target().isBlank()) {
            context.describe();
        } else {
            context.look(command.target());
        }
        return CommandOutcome.none();
    }
}
