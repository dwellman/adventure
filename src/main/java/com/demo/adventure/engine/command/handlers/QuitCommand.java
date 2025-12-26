package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;

public final class QuitCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.QUIT;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.print("Thanks for playing.");
        return CommandOutcome.endGameOutcome();
    }
}
