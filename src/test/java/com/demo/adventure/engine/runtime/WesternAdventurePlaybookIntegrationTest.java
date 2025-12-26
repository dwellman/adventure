package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.test.AdventurePlaybookSupport;
import com.demo.adventure.test.ConsoleCaptureExtension;
import com.demo.adventure.test.PlaybookSupport;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class WesternAdventurePlaybookIntegrationTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @ParameterizedTest
    @MethodSource("playbooks")
    void runsWesternAdventurePlaybook(PlaybookSupport.Playbook playbook) throws Exception {
        GameSave save = AdventurePlaybookSupport.loadGame(playbook);
        String output = AdventurePlaybookSupport.runPlaybook(playbook, save, console);
        AdventurePlaybookSupport.assertPlaybookOutput(output, playbook);
    }

    private static Stream<PlaybookSupport.Playbook> playbooks() {
        return Stream.of(
                PlaybookSupport.loadPlaybook("src/test/resources/playbooks/western-adventure/playbook.yaml")
        );
    }
}
