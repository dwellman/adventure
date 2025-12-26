package com.demo.adventure.engine.integrity;

public record GameIntegrityIssue(
        GameIntegritySeverity severity,
        String code,
        String message,
        String context
) {
    public GameIntegrityIssue {
        severity = severity == null ? GameIntegritySeverity.ERROR : severity;
        code = code == null ? "" : code;
        message = message == null ? "" : message;
        context = context == null ? "" : context;
    }
}
