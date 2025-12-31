package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.command.CommandAction;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class CommandHandlers {

    private CommandHandlers() {
    }

    public static Map<CommandAction, GameCommandHandler> defaultHandlers() {
        Map<CommandAction, GameCommandHandler> handlers = new EnumMap<>(CommandAction.class);
        register(handlers, new QuitCommand());
        register(handlers, new HelpCommand());
        register(handlers, new LookCommand());
        register(handlers, new InspectCommand());
        register(handlers, new ListenCommand());
        register(handlers, new InventoryCommand());
        register(handlers, new ExploreCommand());
        register(handlers, new CraftCommand());
        register(handlers, new HowCommand());
        register(handlers, new DiceCommand());
        register(handlers, new TakeCommand());
        register(handlers, new DropCommand());
        register(handlers, new OpenCommand());
        register(handlers, new TalkCommand());
        register(handlers, new UseCommand());
        register(handlers, new AttackCommand());
        register(handlers, new FleeCommand());
        register(handlers, new PutCommand());
        register(handlers, new GoCommand());
        return Collections.unmodifiableMap(handlers);
    }

    private static void register(Map<CommandAction, GameCommandHandler> handlers, GameCommandHandler handler) {
        handlers.put(handler.action(), handler);
    }
}
