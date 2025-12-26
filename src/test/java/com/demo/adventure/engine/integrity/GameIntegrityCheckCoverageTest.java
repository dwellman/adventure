package com.demo.adventure.engine.integrity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameIntegrityCheckCoverageTest {

    @Test
    void evaluateFlagsIssuesForBrokenGame() throws Exception {
        GameIntegrityCheck check = new GameIntegrityCheck();
        GameIntegrityConfig config = new GameIntegrityConfig(2, 20, 5);

        GameIntegrityReport report = check.evaluate("src/test/resources/integrity/mini.yaml", config);
        List<String> codes = report.issues().stream().map(GameIntegrityIssue::code).toList();

        assertThat(codes).contains("E_TRIGGER_TARGET_MISSING", "E_TRIGGER_ACTION_TARGET_MISSING");
        assertThat(codes).contains("E_HIDDEN_NO_REVEAL");
        assertThat(codes).contains("E_KEYEXPR_PARSE");
        assertThat(codes).contains("E_KEYEXPR_REF");
        assertThat(codes).contains("E_CRAFTING_SKILL_MISSING");
    }

    @Test
    void utilityMethodsHandleLabelsAndTargets() throws Exception {
        String reversed = (String) invokeStatic("reverseGateLabel", new Class<?>[]{String.class}, "A -> B");
        String revealed = (String) invokeStatic("resolveRevealTarget", new Class<?>[]{String.class, String.class, String.class}, "@OBJECT", "Target", "Object");
        String normalized = (String) invokeStatic("normalizeLabel", new Class<?>[]{String.class}, "  Key  ");
        boolean special = (boolean) invokeStatic("isSpecialTarget", new Class<?>[]{String.class}, "@PLAYER");

        assertThat(reversed).isEqualTo("B -> A");
        assertThat(revealed).isEqualTo("OBJECT");
        assertThat(normalized).isEqualTo("KEY");
        assertThat(special).isTrue();
    }

    @Test
    void runReachabilityHandlesNullGame() throws Exception {
        GameIntegrityCheck check = new GameIntegrityCheck();
        Object result = invoke(check,
                "runReachability",
                new Class<?>[]{
                        Class.forName("com.demo.adventure.engine.integrity.GameIntegrityCheck$GameContext"),
                        GameIntegrityConfig.class,
                        Class.forName("com.demo.adventure.engine.integrity.GameIntegrityCheck$DiceMode"),
                        List.class
                },
                null,
                GameIntegrityConfig.defaults(),
                null,
                List.of(Set.of())
        );

        Object summary = invoke(result, "summary", new Class<?>[]{});
        boolean winFound = (boolean) invoke(summary, "winFound", new Class<?>[]{});
        boolean searchExhausted = (boolean) invoke(summary, "searchExhausted", new Class<?>[]{});

        assertThat(winFound).isFalse();
        assertThat(searchExhausted).isTrue();
    }

    private static Object invokeStatic(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = GameIntegrityCheck.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
