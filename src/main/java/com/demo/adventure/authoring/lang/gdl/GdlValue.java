package com.demo.adventure.authoring.lang.gdl;

import java.util.List;

public sealed interface GdlValue permits
        GdlValue.GdlString,
        GdlValue.GdlNumber,
        GdlValue.GdlBoolean,
        GdlValue.GdlList {

    record GdlString(String value) implements GdlValue {
    }

    record GdlNumber(double value) implements GdlValue {
    }

    record GdlBoolean(boolean value) implements GdlValue {
    }

    record GdlList(List<GdlValue> values) implements GdlValue {
    }
}
