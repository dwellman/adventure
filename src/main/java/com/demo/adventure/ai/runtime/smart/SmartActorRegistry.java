package com.demo.adventure.ai.runtime.smart;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SmartActorRegistry {
    private final Map<UUID, SmartActorSpec> specsByActorId;

    private SmartActorRegistry(Map<UUID, SmartActorSpec> specsByActorId) {
        this.specsByActorId = specsByActorId == null ? Map.of() : Map.copyOf(specsByActorId);
    }

    public static SmartActorRegistry create(KernelRegistry registry, List<SmartActorSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return new SmartActorRegistry(Map.of());
        }
        Map<UUID, SmartActorSpec> entries = new LinkedHashMap<>();
        for (SmartActorSpec spec : specs) {
            if (spec == null) {
                continue;
            }
            UUID actorId = SmartActorIdCodec.uuid("actor", spec.actorKey());
            if (entries.putIfAbsent(actorId, spec) != null) {
                throw new IllegalArgumentException("Duplicate smart actor key: " + spec.actorKey());
            }
            if (registry == null || !(registry.get(actorId) instanceof Actor)) {
                throw new IllegalArgumentException("Smart actor key not found in registry: " + spec.actorKey());
            }
        }
        return new SmartActorRegistry(entries);
    }

    public boolean isEmpty() {
        return specsByActorId.isEmpty();
    }

    public Map<UUID, SmartActorSpec> entries() {
        return Collections.unmodifiableMap(specsByActorId);
    }

    public SmartActorSpec specFor(UUID actorId) {
        return specsByActorId.get(actorId);
    }
}
