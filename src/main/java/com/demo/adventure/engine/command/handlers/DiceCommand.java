package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.runtime.CommandContext;

public final class DiceCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.DICE;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.rollDice(command.argument());
        return CommandOutcome.skipTurnAdvanceOutcome();
    }
}
