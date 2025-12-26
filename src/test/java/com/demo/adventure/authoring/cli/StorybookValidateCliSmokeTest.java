package com.demo.adventure.authoring.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import com.demo.adventure.test.ConsoleCaptureExtension;
import static org.assertj.core.api.Assertions.assertThat;

class StorybookValidateCliSmokeTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void validatesStorybookYaml() {
        Path input = Path.of("src", "main", "resources", "storybook", "gdl-demo", "game.yaml");
        assertThat(Files.exists(input)).isTrue();

        console.reset();
        int code = new StorybookValidateCli().run(new String[]{
                input.toString()
        });
        assertThat(code).isEqualTo(0);

        String outText = console.output();
        String errText = console.error();
        assertThat(outText).contains("Validation OK");
        assertThat(errText.trim()).isEmpty();
    }
}
