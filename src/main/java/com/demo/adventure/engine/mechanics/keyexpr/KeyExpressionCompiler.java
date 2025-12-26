package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AttributeAccessNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryOperator;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BooleanLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.IdentifierNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.NumberLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.StringLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryOperator;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class KeyExpressionCompiler {

    private record OperatorInfo(int precedence, boolean rightAssociative, int arity) {
    }

    private final Map<TokenType, OperatorInfo> operators = new HashMap<>();

    public KeyExpressionCompiler() {
        operators.put(TokenType.NOT, new OperatorInfo(5, true, 1));
        operators.put(TokenType.NEGATE, new OperatorInfo(5, true, 1));

        operators.put(TokenType.STAR, new OperatorInfo(4, false, 2));
        operators.put(TokenType.SLASH, new OperatorInfo(4, false, 2));

        operators.put(TokenType.PLUS, new OperatorInfo(3, false, 2));
        operators.put(TokenType.MINUS, new OperatorInfo(3, false, 2));

        operators.put(TokenType.EQUAL, new OperatorInfo(2, false, 2));
        operators.put(TokenType.NOT_EQUAL, new OperatorInfo(2, false, 2));
        operators.put(TokenType.LESS_THAN, new OperatorInfo(2, false, 2));
        operators.put(TokenType.LESS_THAN_OR_EQUAL, new OperatorInfo(2, false, 2));
        operators.put(TokenType.GREATER_THAN, new OperatorInfo(2, false, 2));
        operators.put(TokenType.GREATER_THAN_OR_EQUAL, new OperatorInfo(2, false, 2));

        operators.put(TokenType.AND, new OperatorInfo(1, false, 2));
        operators.put(TokenType.OR, new OperatorInfo(0, false, 2));
    }

    public KeyExpressionNode compile(String input) throws KeyExpressionCompileException {
        List<Token> tokens = new KeyExpressionScanner(input).getTokens();
        return compileTokens(tokens, input == null ? "" : input, true);
    }

    private KeyExpressionNode compileTokens(List<Token> tokens, String input, boolean allowFunctionCalls)
            throws KeyExpressionCompileException {
        Deque<Token> operatorStack = new LinkedList<>();
        Deque<KeyExpressionNode> expressionStack = new LinkedList<>();

        TokenType previousType = null;
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            TokenType type = token.type;
            if (type == TokenType.MINUS && isUnaryPosition(previousType)) {
                token = new Token(TokenType.NEGATE, token.lexeme, token.literal, token.line, token.column);
                type = TokenType.NEGATE;
            } else if (type == TokenType.PLUS && isUnaryPosition(previousType)) {
                throw new KeyExpressionCompileException("Unexpected unary '+'", token.column, input);
            }

            switch (type) {
                case TRUE -> expressionStack.push(new BooleanLiteralNode(true));
                case FALSE -> expressionStack.push(new BooleanLiteralNode(false));
                case NUMBER -> expressionStack.push(new NumberLiteralNode((Number) token.literal));
                case IDENTIFIER, FUNCTION -> {
                    if (isFunctionCall(tokens, index)) {
                        if (!allowFunctionCalls) {
                            throw new KeyExpressionCompileException(
                                    "Nested function calls are not supported yet", token.column, input
                            );
                        }
                        FunctionCallParseResult functionCall = parseFunctionCall(tokens, index, input);
                        expressionStack.push(functionCall.node());
                        index = functionCall.nextIndex();
                        previousType = TokenType.IDENTIFIER;
                        break;
                    }
                    if (isAttributeAccess(tokens, index)) {
                        AttributeAccessParseResult access = parseAttributeAccess(tokens, index, input);
                        expressionStack.push(access.node());
                        index = access.nextIndex();
                        previousType = TokenType.IDENTIFIER;
                        break;
                    }
                    expressionStack.push(new IdentifierNode(token.literal.toString()));
                }
                case LITERAL_STRING -> expressionStack.push(new StringLiteralNode(token.literal.toString()));
                case INTERPOLATED_STRING_START -> {
                    if (isSimpleString(tokens, index)) {
                        Token literalToken = tokens.get(index + 1);
                        expressionStack.push(new StringLiteralNode(literalToken.literal.toString()));
                        index += 2;
                    } else {
                        throw new KeyExpressionCompileException(
                                "Interpolated strings not supported yet", token.column, input
                        );
                    }
                }
                case INTERPOLATED_STRING_END -> throw new KeyExpressionCompileException(
                        "Unexpected end of string", token.column, input
                );
                case LEFT_PAREN -> operatorStack.push(token);
                case RIGHT_PAREN -> popUntilLeftParen(operatorStack, expressionStack, token, input);
                case NOT, NEGATE, AND, OR, PLUS, MINUS, STAR, SLASH,
                        EQUAL, NOT_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL ->
                        pushOperator(token, operatorStack, expressionStack, input);
                case EOL -> {
                    break;
                }
                default -> throw new KeyExpressionCompileException(
                        "Unexpected token '" + token.lexeme + "'", token.column, input
                );
            }

            if (type != TokenType.EOL) {
                previousType = type;
            }
        }

        while (!operatorStack.isEmpty()) {
            Token op = operatorStack.pop();
            if (op.type == TokenType.LEFT_PAREN) {
                throw new KeyExpressionCompileException("Missing closing parenthesis", op.column, input);
            }
            applyOperator(op, expressionStack, input);
        }

        if (expressionStack.size() != 1) {
            throw new KeyExpressionCompileException("Unexpected end of input", input.length(), input);
        }

        return expressionStack.pop();
    }

    private boolean isFunctionCall(List<Token> tokens, int index) {
        return index + 1 < tokens.size() && tokens.get(index + 1).type == TokenType.LEFT_PAREN;
    }

    private boolean isAttributeAccess(List<Token> tokens, int index) {
        return index + 1 < tokens.size() && tokens.get(index + 1).type == TokenType.DOT;
    }

    private boolean isSimpleString(List<Token> tokens, int index) {
        return index + 2 < tokens.size()
                && tokens.get(index + 1).type == TokenType.LITERAL_STRING
                && tokens.get(index + 2).type == TokenType.INTERPOLATED_STRING_END;
    }

    private void pushOperator(
            Token token, Deque<Token> operatorStack, Deque<KeyExpressionNode> expressionStack, String input
    ) throws KeyExpressionCompileException {
        OperatorInfo currentInfo = operators.get(token.type);
        if (currentInfo == null) {
            throw new KeyExpressionCompileException(
                    "Unsupported operator '" + token.lexeme + "'", token.column, input
            );
        }

        while (!operatorStack.isEmpty()) {
            Token top = operatorStack.peek();
            if (top.type == TokenType.LEFT_PAREN) {
                break;
            }

            OperatorInfo topInfo = operators.get(top.type);
            if (topInfo == null) {
                break;
            }

            boolean shouldPop = currentInfo.rightAssociative
                    ? currentInfo.precedence < topInfo.precedence
                    : currentInfo.precedence <= topInfo.precedence;

            if (shouldPop) {
                operatorStack.pop();
                applyOperator(top, expressionStack, input);
            } else {
                break;
            }
        }

        operatorStack.push(token);
    }

    private void popUntilLeftParen(
            Deque<Token> operatorStack, Deque<KeyExpressionNode> expressionStack, Token token, String input
    ) throws KeyExpressionCompileException {
        while (!operatorStack.isEmpty() && operatorStack.peek().type != TokenType.LEFT_PAREN) {
            Token op = operatorStack.pop();
            applyOperator(op, expressionStack, input);
        }

        if (operatorStack.isEmpty()) {
            throw new KeyExpressionCompileException("Unexpected closing parenthesis", token.column, input);
        }

        operatorStack.pop();
    }

    private void applyOperator(
            Token operatorToken, Deque<KeyExpressionNode> expressionStack, String input
    ) throws KeyExpressionCompileException {
        OperatorInfo info = operators.get(operatorToken.type);
        if (info == null) {
            throw new KeyExpressionCompileException(
                    "Unsupported operator '" + operatorToken.lexeme + "'", operatorToken.column, input
            );
        }

        if (info.arity == 1) {
            if (expressionStack.isEmpty()) {
                throw new KeyExpressionCompileException("Missing operand for operator", operatorToken.column, input);
            }
            KeyExpressionNode operand = expressionStack.pop();
            expressionStack.push(new UnaryNode(toUnaryOperator(operatorToken.type), operand));
            return;
        }

        if (expressionStack.size() < 2) {
            throw new KeyExpressionCompileException("Missing operand for operator", operatorToken.column, input);
        }

        KeyExpressionNode right = expressionStack.pop();
        KeyExpressionNode left = expressionStack.pop();
        expressionStack.push(new BinaryNode(left, toBinaryOperator(operatorToken.type), right));
    }

    private record FunctionCallParseResult(FunctionCallNode node, int nextIndex) {
    }

    private record AttributeAccessParseResult(AttributeAccessNode node, int nextIndex) {
    }

    private FunctionCallParseResult parseFunctionCall(
            List<Token> tokens, int functionIndex, String input
    ) throws KeyExpressionCompileException {
        if (functionIndex + 1 >= tokens.size() || tokens.get(functionIndex + 1).type != TokenType.LEFT_PAREN) {
            throw new KeyExpressionCompileException("Malformed function call", functionIndex, input);
        }

        Token functionToken = tokens.get(functionIndex);
        int parenDepth = 1;
        int argStart = functionIndex + 2;
        List<KeyExpressionNode> args = new ArrayList<>();

        int index = functionIndex + 2;
        while (index < tokens.size()) {
            Token token = tokens.get(index);

            if (token.type == TokenType.LEFT_PAREN) {
                parenDepth++;
            } else if (token.type == TokenType.RIGHT_PAREN) {
                parenDepth--;
                if (parenDepth == 0) {
                    if (index > argStart) {
                        args.add(compileTokens(argumentSlice(tokens, argStart, index), input, false));
                    }
                    return new FunctionCallParseResult(new FunctionCallNode(functionToken.lexeme, args), index);
                }
            } else if (token.type == TokenType.COMMA && parenDepth == 1) {
                if (index == argStart) {
                    throw new KeyExpressionCompileException("Missing argument in function call", token.column, input);
                }
                args.add(compileTokens(argumentSlice(tokens, argStart, index), input, false));
                argStart = index + 1;
            }

            index++;
        }

        throw new KeyExpressionCompileException("Unterminated function call", functionToken.column, input);
    }

    private List<Token> argumentSlice(List<Token> tokens, int startInclusive, int endExclusive) {
        List<Token> slice = new ArrayList<>(tokens.subList(startInclusive, endExclusive));
        slice.add(new Token(TokenType.EOL, "", endExclusive));
        return slice;
    }

    private UnaryOperator toUnaryOperator(TokenType type) throws KeyExpressionCompileException {
        if (type == TokenType.NOT) {
            return UnaryOperator.NOT;
        }
        if (type == TokenType.NEGATE) {
            return UnaryOperator.NEGATE;
        }
        throw new KeyExpressionCompileException("Unsupported unary operator", 0, "");
    }

    private BinaryOperator toBinaryOperator(TokenType type) throws KeyExpressionCompileException {
        return switch (type) {
            case AND -> BinaryOperator.AND;
            case OR -> BinaryOperator.OR;
            case PLUS -> BinaryOperator.ADD;
            case MINUS -> BinaryOperator.SUBTRACT;
            case STAR -> BinaryOperator.MULTIPLY;
            case SLASH -> BinaryOperator.DIVIDE;
            case EQUAL -> BinaryOperator.EQUAL;
            case NOT_EQUAL -> BinaryOperator.NOT_EQUAL;
            case LESS_THAN -> BinaryOperator.LESS_THAN;
            case LESS_THAN_OR_EQUAL -> BinaryOperator.LESS_THAN_OR_EQUAL;
            case GREATER_THAN -> BinaryOperator.GREATER_THAN;
            case GREATER_THAN_OR_EQUAL -> BinaryOperator.GREATER_THAN_OR_EQUAL;
            default -> throw new KeyExpressionCompileException("Unsupported binary operator", 0, "");
        };
    }

    private boolean isUnaryPosition(TokenType previousType) {
        if (previousType == null) {
            return true;
        }
        return switch (previousType) {
            case LEFT_PAREN, COMMA,
                    NOT, NEGATE,
                    AND, OR,
                    PLUS, MINUS, STAR, SLASH,
                    EQUAL, NOT_EQUAL,
                    LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL -> true;
            default -> false;
        };
    }

    private AttributeAccessParseResult parseAttributeAccess(
            List<Token> tokens, int startIndex, String input
    ) throws KeyExpressionCompileException {
        Token rootToken = tokens.get(startIndex);
        if (rootToken.type != TokenType.IDENTIFIER && rootToken.type != TokenType.FUNCTION) {
            throw new KeyExpressionCompileException("Expected identifier before '.'", rootToken.column, input);
        }

        String root = rootToken.literal.toString();
        int index = startIndex + 1;
        List<AccessSegment> segments = new ArrayList<>();

        while (index < tokens.size() && tokens.get(index).type == TokenType.DOT) {
            if (index + 1 >= tokens.size()) {
                throw new KeyExpressionCompileException("Missing attribute segment", tokens.get(index).column, input);
            }
            Token segmentToken = tokens.get(index + 1);
            if (isFixtureSegment(tokens, index + 1)) {
                FixtureParseResult fixture = parseFixtureSegment(tokens, index + 1, input);
                segments.add(new AccessSegment.FixtureSegment(fixture.name()));
                index = fixture.nextIndex() + 1;
            } else if (segmentToken.type == TokenType.IDENTIFIER) {
                segments.add(new AccessSegment.PropertySegment(segmentToken.literal.toString()));
                index += 2;
            } else {
                throw new KeyExpressionCompileException("Expected attribute segment", segmentToken.column, input);
            }
        }

        if (segments.isEmpty()) {
            throw new KeyExpressionCompileException("Expected attribute segment after '.'", rootToken.column, input);
        }

        return new AttributeAccessParseResult(new AttributeAccessNode(root, segments), index - 1);
    }

    private boolean isFixtureSegment(List<Token> tokens, int index) {
        if (index >= tokens.size()) {
            return false;
        }
        Token token = tokens.get(index);
        if (!"FIXTURE".equalsIgnoreCase(token.lexeme)) {
            return false;
        }
        return index + 1 < tokens.size() && tokens.get(index + 1).type == TokenType.LEFT_PAREN;
    }

    private record FixtureParseResult(String name, int nextIndex) {
    }

    private FixtureParseResult parseFixtureSegment(List<Token> tokens, int index, String input)
            throws KeyExpressionCompileException {
        if (index + 1 >= tokens.size() || tokens.get(index + 1).type != TokenType.LEFT_PAREN) {
            throw new KeyExpressionCompileException("Expected '(' after fixture", tokens.get(index).column, input);
        }
        int stringStart = index + 2;
        StringParseResult stringResult = parseStringLiteral(tokens, stringStart, input);
        int afterString = stringResult.nextIndex() + 1;
        if (afterString >= tokens.size() || tokens.get(afterString).type != TokenType.RIGHT_PAREN) {
            throw new KeyExpressionCompileException("Expected ')' after fixture", tokens.get(index).column, input);
        }
        return new FixtureParseResult(stringResult.value(), afterString);
    }

    private record StringParseResult(String value, int nextIndex) {
    }

    private StringParseResult parseStringLiteral(List<Token> tokens, int startIndex, String input)
            throws KeyExpressionCompileException {
        if (startIndex + 2 >= tokens.size()) {
            throw new KeyExpressionCompileException("Expected string literal", startIndex, input);
        }
        Token start = tokens.get(startIndex);
        Token literal = tokens.get(startIndex + 1);
        Token end = tokens.get(startIndex + 2);
        if (start.type != TokenType.INTERPOLATED_STRING_START
                || literal.type != TokenType.LITERAL_STRING
                || end.type != TokenType.INTERPOLATED_STRING_END) {
            throw new KeyExpressionCompileException("Expected string literal", start.column, input);
        }
        return new StringParseResult(literal.literal.toString(), startIndex + 2);
    }
}
