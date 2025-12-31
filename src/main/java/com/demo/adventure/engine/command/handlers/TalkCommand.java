package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.runtime.CommandContext;

public final class TalkCommand implements GameCommandHandler {
    @Override
    public CommandAction action() {
        return CommandAction.TALK;
    }

    @Override
    public CommandOutcome handle(CommandContext context, Command command) {
        context.talk(command.target());
        return CommandOutcome.none();
    }
}
