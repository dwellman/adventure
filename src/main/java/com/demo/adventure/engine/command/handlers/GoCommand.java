package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.engine.flow.trigger.TriggerType;

import java.util.UUID;

public final class GoCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.GO;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) throws GameBuilderException {
        Direction direction = context.parseDirection(command.target());
        UUID next = context.move(direction);
        if (next == null) {
            context.narrate("You can't go that way.");
            return CommandOutcome.none();
        }
        context.setCurrentPlot(next);
        Plot plot = context.currentPlot();
        CommandOutcome outcome = context.resolveTriggerOutcome(context.fireTrigger(TriggerType.ON_ENTER, plot, null));
        if (outcome.endGame() || outcome.skipTurnAdvance()) {
            return outcome;
        }
        context.describe();
        return CommandOutcome.none();
    }
}
