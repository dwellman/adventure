package com.demo.adventure.engine.command.interpreter;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommandScannerTest {

    @Test
    void scansQuotedStringsAndPrepositions() {
        List<Token> tokens = CommandScanner.scan("use \"rusty key\" on door");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.USE,
                TokenType.STRING,
                TokenType.ON,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
        assertThat(tokens.get(1).lexeme).isEqualTo("rusty key");
    }

    @Test
    void normalizesGoToMoveToken() {
        List<Token> tokens = CommandScanner.scan("go to hallway");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.MOVE,
                TokenType.TO,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }

    @Test
    void scansMoveAsCanonicalMoveToken() {
        List<Token> tokens = CommandScanner.scan("move to hallway");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.MOVE,
                TokenType.TO,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }

    @Test
    void scansIntoPreposition() {
        List<Token> tokens = CommandScanner.scan("put key into box");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.PUT,
                TokenType.IDENTIFIER,
                TokenType.INTO,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }

    @Test
    void scansAttackAsStrikeToken() {
        List<Token> tokens = CommandScanner.scan("attack goblin");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.STRIKE,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }

    @Test
    void scansRunAwayAsMoveAndAwayTokens() {
        List<Token> tokens = CommandScanner.scan("run away");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.MOVE,
                TokenType.AWAY,
                TokenType.EOL
        );
    }

    @Test
    void scansRunWithDirectionAsMoveThenDirection() {
        List<Token> tokens = CommandScanner.scan("run north");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.MOVE,
                TokenType.NORTH,
                TokenType.EOL
        );
    }

    @Test
    void scansWestShorthandAsDirection() {
        List<Token> tokens = CommandScanner.scan("w");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.WEST,
                TokenType.EOL
        );
    }

    @Test
    void scansNorthwestWithoutSeparator() {
        List<Token> tokens = CommandScanner.scan("northwest");
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NORTH_WEST,
                TokenType.EOL
        );
    }

    @Test
    void scansWithAndFromPrepositions() {
        List<Token> withTokens = CommandScanner.scan("use key with door");
        assertThat(withTokens).extracting(t -> t.type).containsExactly(
                TokenType.USE,
                TokenType.IDENTIFIER,
                TokenType.WITH,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
        List<Token> fromTokens = CommandScanner.scan("take key from chest");
        assertThat(fromTokens).extracting(t -> t.type).containsExactly(
                TokenType.TAKE,
                TokenType.IDENTIFIER,
                TokenType.FROM,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }
}
