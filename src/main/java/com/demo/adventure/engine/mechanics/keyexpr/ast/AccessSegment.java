package com.demo.adventure.engine.mechanics.keyexpr.ast;

public sealed interface AccessSegment permits
        AccessSegment.PropertySegment,
        AccessSegment.FixtureSegment {

    record PropertySegment(String name) implements AccessSegment {
    }

    record FixtureSegment(String name) implements AccessSegment {
    }
}
