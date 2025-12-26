package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorSpecLoaderTest {

    @Test
    void loadsMansionSmartActors() throws Exception {
        Path path = Path.of("src/main/resources/games/mansion/world/smart-actors.yaml");
        List<SmartActorSpec> specs = SmartActorSpecLoader.load(path);

        assertThat(specs).hasSize(5);
        SmartActorSpec butler = specs.stream()
                .filter(spec -> "butler".equals(spec.actorKey()))
                .findFirst()
                .orElseThrow();

        assertThat(butler.history()).isNotNull();
        assertThat(butler.history().storeKey()).isEqualTo("mansion:butler");
        assertThat(butler.history().seeds()).isNotEmpty();
        assertThat(butler.policy().allowedVerbs()).contains("LOOK");
    }
}
