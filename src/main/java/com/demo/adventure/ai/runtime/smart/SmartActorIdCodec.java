package com.demo.adventure.ai.runtime.smart;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class SmartActorIdCodec {
    private SmartActorIdCodec() {
    }

    static UUID uuid(String kind, String key) {
        if (kind == null || kind.isBlank() || key == null || key.isBlank()) {
            throw new IllegalArgumentException("kind and key are required");
        }
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }
}
