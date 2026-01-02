package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;

import java.util.UUID;

final class RuntimeNavigation {
    private final GameRuntime runtime;

    RuntimeNavigation(GameRuntime runtime) {
        this.runtime = runtime;
    }

    UUID move(Direction direction) {
        return tryMove(direction).nextPlotId();
    }

    MoveResult tryMove(Direction direction) {
        if (direction == null) {
            return MoveResult.none();
        }
        for (Gate gate : runtime.exits()) {
            Direction dir = gate.directionFrom(runtime.currentPlotId());
            if (dir == direction) {
                if (!runtime.isGateOpen(gate, runtime.registry(), runtime.playerId(), runtime.currentPlotId())) {
                    String desc = runtime.stripGateDestinationTag(gate.getDescriptionFrom(runtime.currentPlotId()));
                    String reason = (desc == null || desc.isBlank()) ? "That way is blocked." : runtime.ensurePeriod(desc);
                    return MoveResult.blocked(reason);
                }
                return MoveResult.moved(gate.otherSide(runtime.currentPlotId()));
            }
        }
        return MoveResult.none();
    }

    Direction parseDirection(String token) {
        try {
            return Direction.parse(token);
        } catch (Exception ex) {
            return null;
        }
    }
}
