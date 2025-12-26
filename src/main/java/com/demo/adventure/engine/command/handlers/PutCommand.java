package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class PutCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.PUT;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.put(command.target(), command.object());
        return CommandOutcome.none();
    }
}
