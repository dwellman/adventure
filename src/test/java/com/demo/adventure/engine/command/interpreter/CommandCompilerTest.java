package com.demo.adventure.engine.command.interpreter;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.TokenType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommandCompilerTest {

    private final CommandInterpreter interpreter = new CommandInterpreter();

    private Command parse(String input) {
        return interpreter.interpret(input);
    }

    @Test
    void parsesDirectionOnlyAsMove() {
        Command cmd = parse("n");
        assertThat(cmd.action()).isEqualTo(CommandAction.GO);
        assertThat(cmd.target()).isEqualTo("n");
    }

    @Test
    void parsesWestShorthandAsMove() {
        Command cmd = parse("w");
        assertThat(cmd.action()).isEqualTo(CommandAction.GO);
        assertThat(cmd.target()).isEqualTo("w");
    }

    @Test
    void parsesNorthwestAsMove() {
        Command cmd = parse("northwest");
        assertThat(cmd.action()).isEqualTo(CommandAction.GO);
        assertThat(cmd.target()).isEqualTo("northwest");
    }

    @Test
    void parsesGoWithDirection() {
        Command cmd = parse("go north");
        assertThat(cmd.action()).isEqualTo(CommandAction.GO);
        assertThat(cmd.target()).isEqualTo("north");
    }

    @Test
    void parsesLookWithDirection() {
        Command cmd = parse("look east");
        assertThat(cmd.action()).isEqualTo(CommandAction.LOOK);
        assertThat(cmd.target()).isEqualTo("east");
    }

    @Test
    void parsesLookWithTarget() {
        Command cmd = parse("look torch");
        assertThat(cmd.action()).isEqualTo(CommandAction.LOOK);
        assertThat(cmd.target()).isEqualTo("torch");
    }

    @Test
    void parsesLookAroundAsLook() {
        Command cmd = parse("look around");
        assertThat(cmd.action()).isEqualTo(CommandAction.LOOK);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesLookAroundSentenceAsLook() {
        Command cmd = parse("look around and tell me what I see");
        assertThat(cmd.action()).isEqualTo(CommandAction.LOOK);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesLookArroundAsLook() {
        Command cmd = parse("look arround");
        assertThat(cmd.action()).isEqualTo(CommandAction.LOOK);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesLookArroundSentenceAsLook() {
        Command cmd = parse("look arround and tell me what I see");
        assertThat(cmd.action()).isEqualTo(CommandAction.LOOK);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesCraftFromMakeToken() {
        Command cmd = parse("craft torch");
        assertThat(cmd.action()).isEqualTo(CommandAction.CRAFT);
        assertThat(cmd.target()).isEqualTo("torch");
    }

    @Test
    void parsesCustomAliasFromExtraKeywords() {
        interpreter.setExtraKeywords(Map.of("EXAMINE", TokenType.INSPECT));
        Command cmd = parse("examine map");
        assertThat(cmd.action()).isEqualTo(CommandAction.INSPECT);
        assertThat(cmd.target()).isEqualTo("map");
    }

    @Test
    void parsesHowWithCraftArg() {
        Command cmd = parse("how craft torch");
        assertThat(cmd.action()).isEqualTo(CommandAction.HOW);
        assertThat(cmd.target()).isEqualTo("craft torch");
    }

    @Test
    void parsesSearchIgnoringArguments() {
        Command cmd = parse("search bag");
        assertThat(cmd.action()).isEqualTo(CommandAction.EXPLORE);
    }

    @Test
    void parsesQuotedTargets() {
        Command cmd = parse("take \"lit torch\"");
        assertThat(cmd.action()).isEqualTo(CommandAction.TAKE);
        assertThat(cmd.target()).isEqualTo("lit torch");
    }

    @Test
    void parsesAttackWithTarget() {
        Command cmd = parse("attack goblin");
        assertThat(cmd.action()).isEqualTo(CommandAction.ATTACK);
        assertThat(cmd.target()).isEqualTo("goblin");
    }

    @Test
    void parsesFleeWithoutArguments() {
        Command cmd = parse("flee");
        assertThat(cmd.action()).isEqualTo(CommandAction.FLEE);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesRunAwayAsFlee() {
        Command cmd = parse("run away");
        assertThat(cmd.action()).isEqualTo(CommandAction.FLEE);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesRunAsFlee() {
        Command cmd = parse("run");
        assertThat(cmd.action()).isEqualTo(CommandAction.FLEE);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesRunWithDirectionAsMove() {
        Command cmd = parse("run north");
        assertThat(cmd.action()).isEqualTo(CommandAction.GO);
        assertThat(cmd.target()).isEqualTo("north");
    }

    @Test
    void parsesDiceCommand() {
        Command cmd = parse("dice(20,15)");
        assertThat(cmd.action()).isEqualTo(CommandAction.DICE);
        assertThat(cmd.argument()).isEqualTo("20 15");
    }

    @Test
    void parsesTalkWithTarget() {
        Command cmd = parse("talk butler");
        assertThat(cmd.action()).isEqualTo(CommandAction.TALK);
        assertThat(cmd.target()).isEqualTo("butler");
    }

    @Test
    void parsesTalkWithLeadingTo() {
        Command cmd = parse("talk to butler");
        assertThat(cmd.action()).isEqualTo(CommandAction.TALK);
        assertThat(cmd.target()).isEqualTo("butler");
    }

    @Test
    void parsesAtMentionAsTalk() {
        Command cmd = parse("@butler");
        assertThat(cmd.action()).isEqualTo(CommandAction.TALK);
        assertThat(cmd.target()).isEqualTo("butler");
    }

    @Test
    void parsesAtMentionWithFullName() {
        Command cmd = parse("@Elias Crane");
        assertThat(cmd.action()).isEqualTo(CommandAction.TALK);
        assertThat(cmd.target()).isEqualTo("Elias Crane");
    }

    @Test
    void parsesUnknownWhenBlank() {
        assertThat(parse("")).isEqualTo(Command.unknown());
    }

    @Test
    void parsesInventoryWithoutArguments() {
        Command cmd = parse("inventory");
        assertThat(cmd.action()).isEqualTo(CommandAction.INVENTORY);
        assertThat(cmd.target()).isEmpty();
    }

    @Test
    void parsesPrepositionArguments() {
        Command cmd = parse("use key on door");
        assertThat(cmd.action()).isEqualTo(CommandAction.USE);
        assertThat(cmd.argument()).isEqualTo("key on door");
        assertThat(cmd.target()).isEqualTo("key");
        assertThat(cmd.preposition()).isEqualTo("on");
        assertThat(cmd.object()).isEqualTo("door");
    }

    @Test
    void parsesWithPreposition() {
        Command cmd = parse("use key with door");
        assertThat(cmd.action()).isEqualTo(CommandAction.USE);
        assertThat(cmd.argument()).isEqualTo("key with door");
        assertThat(cmd.target()).isEqualTo("key");
        assertThat(cmd.preposition()).isEqualTo("with");
        assertThat(cmd.object()).isEqualTo("door");
    }

    @Test
    void parsesFromPreposition() {
        Command cmd = parse("take key from chest");
        assertThat(cmd.action()).isEqualTo(CommandAction.TAKE);
        assertThat(cmd.argument()).isEqualTo("key from chest");
        assertThat(cmd.target()).isEqualTo("key");
        assertThat(cmd.preposition()).isEqualTo("from");
        assertThat(cmd.object()).isEqualTo("chest");
    }

    @Test
    void reportsParseErrors() {
        Command cmd = parse("use on door");
        assertThat(cmd.hasError()).isTrue();
        assertThat(cmd.error().message()).contains("Expected target");
    }
}
