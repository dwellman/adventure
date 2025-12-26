package com.demo.adventure.authoring.save.build;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable accumulation of build or validation problems.
 */
public final class WorldBuildReport {
    private final List<WorldBuildProblem> problems;

    /** Create an empty report. */
    public WorldBuildReport() {
        this.problems = new ArrayList<>();
    }

    /**
     * Create a report seeded with existing problems.
     *
     * @param problems initial problems
     */
    public WorldBuildReport(List<WorldBuildProblem> problems) {
        this.problems = new ArrayList<>(problems == null ? List.of() : problems);
    }

    /** @return immutable view of recorded problems. */
    public List<WorldBuildProblem> getProblems() {
        return List.copyOf(problems);
    }

    /**
     * Append a single problem.
     *
     * @param problem problem to add
     */
    public void add(WorldBuildProblem problem) {
        if (problem != null) {
            problems.add(problem);
        }
    }

    /**
     * Append a list of problems.
     *
     * @param problems problems to add
     */
    public void addAll(List<WorldBuildProblem> problems) {
        if (problems != null) {
            this.problems.addAll(problems);
        }
    }

    /** @return true when any blocking problems are present. */
    public boolean hasBlockingProblems() {
        return !problems.isEmpty();
    }
}
