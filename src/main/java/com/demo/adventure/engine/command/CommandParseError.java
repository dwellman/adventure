package com.demo.adventure.engine.command;

public record CommandParseError(String message, int column, String input) {
}
