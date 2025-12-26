package com.demo.adventure.authoring.save.build;

import java.util.UUID;

/**
 * Single validation or build issue encountered while constructing a world.
 *
 * @param code       machine-readable error code
 * @param message    human-readable description
 * @param entityType type/category of the entity involved
 * @param entityId   optional entity identifier
 */
public record WorldBuildProblem(
        String code,
        String message,
        String entityType,
        UUID entityId
) {
}
