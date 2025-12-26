package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.engine.flow.trigger.TriggerType;

public final class TakeCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.TAKE;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) throws GameBuilderException {
        Item taken = context.take(command.target());
        if (taken == null) {
            return CommandOutcome.none();
        }
        return context.resolveTriggerOutcome(context.fireTrigger(TriggerType.ON_TAKE, taken, null));
    }
}
