package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class CraftCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.CRAFT;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.craft(command.target());
        return CommandOutcome.none();
    }
}
