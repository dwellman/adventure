package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class HelpCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.HELP;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.printHelp();
        return CommandOutcome.none();
    }
}
