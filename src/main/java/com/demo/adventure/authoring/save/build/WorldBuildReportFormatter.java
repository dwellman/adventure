package com.demo.adventure.authoring.save.build;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Pretty-printer for {@link WorldBuildReport}.
 */
public final class WorldBuildReportFormatter {
    private WorldBuildReportFormatter() {
    }

    /**
     * Render a report as a simple multi-line string.
     *
     * @param report report to render
     * @return formatted lines (empty when report is null)
     */
    public static String format(WorldBuildReport report) {
        if (report == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("World Build Report").append('\n');
        List<WorldBuildProblem> problems = report.getProblems();
        problems.stream()
                .sorted(problemComparator())
                .forEach(p -> sb.append(renderProblem(p)).append('\n'));
        return sb.toString().trim();
    }

    private static Comparator<WorldBuildProblem> problemComparator() {
        return Comparator
                .comparing(WorldBuildProblem::code, Comparator.nullsLast(String::compareTo))
                .thenComparing(WorldBuildProblem::entityType, Comparator.nullsLast(String::compareTo))
                .thenComparing(WorldBuildReportFormatter::entityIdString, Comparator.nullsLast(String::compareTo))
                .thenComparing(WorldBuildProblem::message, Comparator.nullsLast(String::compareTo));
    }

    private static String entityIdString(WorldBuildProblem p) {
        UUID id = p.entityId();
        return id == null ? null : id.toString();
    }

    private static String renderProblem(WorldBuildProblem p) {
        String code = p.code() == null ? "" : p.code();
        String entityType = p.entityType() == null ? "" : p.entityType();
        String entityId = p.entityId() == null ? "null" : p.entityId().toString();
        String message = p.message() == null ? "" : p.message();
        return code + " | " + entityType + " | " + entityId + " | " + message;
    }
}
