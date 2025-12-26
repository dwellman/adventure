package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class HowCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.HOW;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.how(command.target());
        return CommandOutcome.none();
    }
}
