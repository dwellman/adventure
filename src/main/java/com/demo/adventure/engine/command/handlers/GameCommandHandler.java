package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.support.exceptions.GameBuilderException;

public interface GameCommandHandler {
    CommandAction action();

    CommandOutcome handle(CommandContext context, Command command) throws GameBuilderException;
}
