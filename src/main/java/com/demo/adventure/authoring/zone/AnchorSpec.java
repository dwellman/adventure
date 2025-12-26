package com.demo.adventure.authoring.zone;

import java.util.Objects;

public record AnchorSpec(
        String key,
        String name,
        AnchorRole role,
        String description
) {
    public AnchorSpec {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(role, "role");
    }
}
