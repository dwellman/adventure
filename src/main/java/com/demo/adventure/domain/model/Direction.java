package com.demo.adventure.domain.model;

import java.util.Locale;

public enum Direction {
    N, NE, E, SE, S, SW, W, NW, UP, DOWN, PORTAL;

    public static final Direction NORTH = N;
    public static final Direction NORTHEAST = NE;
    public static final Direction EAST = E;
    public static final Direction SOUTHEAST = SE;
    public static final Direction SOUTH = S;
    public static final Direction SOUTHWEST = SW;
    public static final Direction WEST = W;
    public static final Direction NORTHWEST = NW;

    public static Direction oppositeOf(Direction direction) {
        if (direction == null) {
            return null;
        }
        return switch (direction) {
            case N -> S;
            case NE -> SW;
            case E -> W;
            case SE -> NW;
            case S -> N;
            case SW -> NE;
            case W -> E;
            case NW -> SE;
            case UP -> DOWN;
            case DOWN -> UP;
            case PORTAL -> PORTAL;
        };
    }

    /**
     * Parse a direction token accepting both short (E) and long (EAST or NORTH_EAST) names.
     *
     * @param text direction text
     * @return matched direction
     */
    public static Direction parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Direction is required");
        }
        String normalized = text.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "N", "NORTH" -> N;
            case "NE", "NORTH_EAST", "NORTHEAST" -> NE;
            case "E", "EAST" -> E;
            case "SE", "SOUTH_EAST", "SOUTHEAST" -> SE;
            case "S", "SOUTH" -> S;
            case "SW", "SOUTH_WEST", "SOUTHWEST" -> SW;
            case "W", "WEST" -> W;
            case "NW", "NORTH_WEST", "NORTHWEST" -> NW;
            case "UP" -> UP;
            case "DOWN" -> DOWN;
            case "PORTAL" -> PORTAL;
            default -> throw new IllegalArgumentException("Unknown direction: " + text);
        };
    }

    /**
     * Render the long-form name for serialization (e.g., EAST instead of E).
     *
     * @return long-form name
     */
    public String toLongName() {
        return switch (this) {
            case N -> "NORTH";
            case NE -> "NORTH_EAST";
            case E -> "EAST";
            case SE -> "SOUTH_EAST";
            case S -> "SOUTH";
            case SW -> "SOUTH_WEST";
            case W -> "WEST";
            case NW -> "NORTH_WEST";
            case UP -> "UP";
            case DOWN -> "DOWN";
            case PORTAL -> "PORTAL";
        };
    }
}
