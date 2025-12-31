package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.UseResult;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.engine.flow.trigger.TriggerType;

public final class UseCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.USE;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) throws GameBuilderException {
        UseResult result = context.use(command.target(), command.preposition(), command.object());
        if (!result.valid()) {
            return CommandOutcome.none();
        }
        return context.resolveTriggerOutcome(context.fireTrigger(
                TriggerType.ON_USE, result.source(), result.object()
        ));
    }
}
