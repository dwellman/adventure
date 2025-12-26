package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GdlCompilerTest {

    @Test
    void compilesGameWithForwardReferencesAndContains() throws GdlCompileException {
        String gdl = String.join("\n",
                "thing(\"room\").fixture(\"self\").name=\"Room\".region=\"ISLAND\".locationX=0.locationY=0",
                "thing(\"room\").fixture(\"north\").leadsTo=\"hall\".keyString=\"HAS(\\\"Key\\\")\"",
                "thing(\"room\").fixture(\"table\").name=\"Table\".description=\"Wooden\".contains=[\"key\"]",
                "thing(\"hall\").fixture(\"self\").name=\"Hall\".region=\"ISLAND\".locationX=0.locationY=1",
                "thing(\"key\").fixture(\"self\").kind=\"item\".name=\"Key\".description=\"A key\"",
                "actor(\"hero\").fixture(\"self\").name=\"Hero\".description=\"You are here\".owner=\"room\".player=true"
        );

        GameSave save = new GdlCompiler().compile(gdl);

        assertThat(save.plots()).hasSize(2);
        assertThat(save.gates()).hasSize(1);
        assertThat(save.fixtures()).hasSize(1);
        assertThat(save.items()).hasSize(1);
        assertThat(save.actors()).hasSize(1);

        UUID roomId = uuid("plot", "room");
        UUID hallId = uuid("plot", "hall");
        UUID fixtureId = uuid("fixture", "room-table");

        assertThat(save.startPlotId()).isEqualTo(roomId);
        assertThat(save.preamble()).isEqualTo("You are here");

        WorldRecipe.GateSpec gate = save.gates().get(0);
        assertThat(gate.fromPlotId()).isEqualTo(roomId);
        assertThat(gate.toPlotId()).isEqualTo(hallId);
        assertThat(gate.direction()).isEqualTo(Direction.N);
        assertThat(gate.keyString()).isEqualTo("HAS(\"Key\")");

        assertThat(save.fixtures().get(0).id()).isEqualTo(fixtureId);
        assertThat(save.items().get(0).ownerId()).isEqualTo(fixtureId);
        assertThat(save.actors().get(0).ownerId()).isEqualTo(roomId);
    }

    @Test
    void rejectsMissingRequiredAttributes() {
        String gdl = "thing(\"room\").fixture(\"self\").name=\"Room\"";

        assertThatThrownBy(() -> new GdlCompiler().compile(gdl))
                .isInstanceOf(GdlCompileException.class)
                .hasMessageContaining("Missing required attribute");
    }

    @Test
    void rejectsUnquotedExpressions() {
        String gdl = "thing(\"room\").fixture(\"self\").name=\"Room\".region=\"ISLAND\".keyString=HAS(\"Key\")";

        assertThatThrownBy(() -> new GdlCompiler().compile(gdl))
                .isInstanceOf(GdlCompileException.class)
                .hasMessageContaining("Expected literal value");
    }

    private static UUID uuid(String kind, String key) {
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }
}
