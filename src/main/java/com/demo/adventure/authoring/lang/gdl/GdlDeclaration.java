package com.demo.adventure.authoring.lang.gdl;

import java.util.Map;

public record GdlDeclaration(
        GdlDeclarationType type,
        String subjectId,
        String fixtureId,
        Map<String, GdlAttribute> attributes,
        int line,
        int column
) {
}
