package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class OpenCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.OPEN;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.open(command.target());
        return CommandOutcome.none();
    }
}
