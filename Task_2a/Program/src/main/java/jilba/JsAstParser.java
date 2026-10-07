package jilba;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Синтаксический анализатор (парсер) для построения абстрактного синтаксического дерева (AST)
 * упрощенного JavaScript-кода с целью последующего расчета метрик сложности Джилба (Gilb's metric).
 *
 * <p>Поддерживаемые конструкции:
 * <ul>
 *     <li>Объявления функций ({@link NodeType#FUNCTION})</li>
 *     <li>Объявления переменных ({@link NodeType#VAR_DECLARATION})</li>
 *     <li>Условные операторы {@code if / else} ({@link NodeType#IF}, {@link NodeType#ELSE})</li>
 *     <li>Циклы {@code while} ({@link NodeType#WHILE}) и {@code do-while} ({@link NodeType#DO_WHILE})</li>
 *     <li>Циклы обхода коллекций ({@link NodeType#FOR_OF}, {@link NodeType#FOR_IN})</li>
 *     <li>Классические циклы {@code for} ({@link NodeType#FOR})</li>
 *     <li>Конструкции {@code switch} ({@link NodeType#SWITCH}, {@link NodeType#CASE}, {@link NodeType#DEFAULT})</li>
 *     <li>Тернарные условные операторы ({@link NodeType#TERNARY}) внутри выражений и присваиваний</li>
 *     <li>Операторы присваивания ({@link NodeType#ASSIGNMENT}), выражений ({@link NodeType#EXPRESSION_STATEMENT})</li>
 *     <li>Управляющие операторы {@link NodeType#RETURN}, {@link NodeType#BREAK}, {@link NodeType#CONTINUE}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JsAstParser {

    private final JsLexer lexer;

    /**
     * Выполняет синтаксический разбор исходного кода JavaScript и возвращает корневой узел AST.
     *
     * @param sourceCode исходный текст программы
     * @return корневой узел дерева {@link AstNode}
     * @throws NullPointerException     если sourceCode равен null
     * @throws IllegalArgumentException если sourceCode пуст или синтаксически некорректен
     */
    public AstNode parse(String sourceCode) {
        Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        if (sourceCode.isBlank()) {
            throw new IllegalArgumentException("sourceCode must not be blank");
        }

        List<Token> tokens = lexer.tokenize(sourceCode);
        ParserState state = new ParserState(sourceCode, tokens);

        List<AstNode> topLevelNodes = new ArrayList<>();
        while (!state.isAtEnd()) {
            topLevelNodes.add(parseStatement(state));
        }

        if (topLevelNodes.isEmpty()) {
            throw new IllegalArgumentException("No statements found in sourceCode");
        }

        if (topLevelNodes.size() == 1) {
            return topLevelNodes.getFirst();
        }

        // Если в коде несколько инструкций на верхнем уровне, объединяем в синтетическую функцию
        AstNode root = new AstNode(NodeType.FUNCTION, "function main()");
        for (AstNode node : topLevelNodes) {
            root.addChild(node);
        }
        return root;
    }

    /**
     * Выводит иерархию AST-дерева в стандартный поток вывода {@link System#out} с отступами.
     *
     * @param root корневой узел дерева
     */
    public void printTree(AstNode root) {
        prettyPrint(root, System.out);
    }

    /**
     * Статический метод для вывода иерархии AST-дерева в стандартный поток вывода {@link System#out}.
     *
     * @param root корневой узел дерева
     */
    public static void prettyPrint(AstNode root) {
        prettyPrint(root, System.out);
    }

    /**
     * Выводит иерархию AST-дерева в указанный поток вывода {@link PrintStream} с отступами.
     *
     * @param root корневой узел дерева
     * @param out  целевой поток вывода
     */
    public static void prettyPrint(AstNode root, PrintStream out) {
        Objects.requireNonNull(root, "root must not be null");
        Objects.requireNonNull(out, "out must not be null");
        printNode(root, out, 0);
    }

    /**
     * Формирует отформатированное строковое представление всей иерархии AST-дерева.
     *
     * @param root корневой узел дерева
     * @return форматированная строка с отступами в 4 пробела
     */
    public static String toTreeString(AstNode root) {
        Objects.requireNonNull(root, "root must not be null");
        StringBuilder sb = new StringBuilder();
        appendNodeString(root, sb, 0);
        return sb.toString();
    }

    /**
     * Рассчитывает метрики сложности Джилба для заданного AST-дерева.
     *
     * @param root корневой узел дерева
     * @return результат анализа {@link GilbMetricResult}
     */
    public static GilbMetricResult calculateGilbMetric(AstNode root) {
        return new GilbMetricAnalyzer(new JsAstParser(new JsLexer())).analyze(root);
    }

    private static void printNode(AstNode node, PrintStream out, int depth) {
        String indent = "    ".repeat(depth);
        String condSuffix = node.isConditional() ? " (conditional)" : "";
        out.printf("%s[%s] %s%s%n", indent, node.getType(), node.getRawText(), condSuffix);
        for (AstNode child : node.getChildren()) {
            printNode(child, out, depth + 1);
        }
    }

    private static void appendNodeString(AstNode node, StringBuilder sb, int depth) {
        String indent = "    ".repeat(depth);
        String condSuffix = node.isConditional() ? " (conditional)" : "";
        sb.append(String.format("%s[%s] %s%s%n", indent, node.getType(), node.getRawText(), condSuffix));
        for (AstNode child : node.getChildren()) {
            appendNodeString(child, sb, depth + 1);
        }
    }

    // ==========================================
    // Рекурсивный спуск: разбор инструкций
    // ==========================================

    private AstNode parseStatement(ParserState state) {
        Token currentToken = state.peek();

        return switch (currentToken.type()) {
            case FUNCTION -> parseFunction(state);
            case VAR, LET, CONST -> parseVarDeclaration(state);
            case WHILE -> parseWhile(state);
            case DO -> parseDoWhile(state);
            case FOR -> parseFor(state);
            case IF -> parseIf(state);
            case SWITCH -> parseSwitch(state);
            case RETURN -> parseReturn(state);
            case BREAK -> parseBreak(state);
            case CONTINUE -> parseContinue(state);
            default -> parseExpressionOrAssignment(state);
        };
    }

    private AstNode parseFunction(ParserState state) {
        Token funcToken = state.consume(TokenType.FUNCTION, "Expected 'function'");
        int startPos = funcToken.startPos();

        if (state.check(TokenType.IDENTIFIER)) {
            state.advance();
        }

        state.consume(TokenType.LPAREN, "Expected '(' after function identifier");
        skipBalanced(state, TokenType.LPAREN, TokenType.RPAREN);
        Token endParen = state.previous();

        String rawHeader = state.substring(startPos, endParen.endPos()).trim();
        AstNode funcNode = new AstNode(NodeType.FUNCTION, rawHeader);

        state.consume(TokenType.LBRACE, "Expected '{' to start function body");
        while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
            funcNode.addChild(parseStatement(state));
        }
        state.consume(TokenType.RBRACE, "Expected '}' at the end of function body");

        return funcNode;
    }

    private AstNode parseVarDeclaration(ParserState state) {
        Token startTok = state.advance(); // consume var / let / const
        int startPos = startTok.startPos();

        List<Token> stmtTokens = new ArrayList<>();
        stmtTokens.add(startTok);

        int depth = 0;
        while (!state.isAtEnd()) {
            if (depth == 0 && state.check(TokenType.SEMICOLON)) {
                break;
            }
            Token t = state.advance();
            stmtTokens.add(t);
            if (t.type() == TokenType.LPAREN || t.type() == TokenType.LBRACKET || t.type() == TokenType.LBRACE) {
                depth++;
            } else if (t.type() == TokenType.RPAREN || t.type() == TokenType.RBRACKET || t.type() == TokenType.RBRACE) {
                depth--;
            }
        }

        Token semi = state.consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        String rawText = state.substring(startPos, semi.endPos()).trim();
        AstNode varNode = new AstNode(NodeType.VAR_DECLARATION, rawText);

        extractAndAttachTernaries(state, stmtTokens, varNode);
        return varNode;
    }

    private AstNode parseWhile(ParserState state) {
        Token whileTok = state.consume(TokenType.WHILE, "Expected 'while'");
        int startPos = whileTok.startPos();

        state.consume(TokenType.LPAREN, "Expected '(' after while");
        skipBalanced(state, TokenType.LPAREN, TokenType.RPAREN);
        Token endParen = state.previous();

        String rawHeader = state.substring(startPos, endParen.endPos()).trim();
        AstNode whileNode = new AstNode(NodeType.WHILE, rawHeader);

        state.consume(TokenType.LBRACE, "Expected '{' after while condition");
        while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
            whileNode.addChild(parseStatement(state));
        }
        state.consume(TokenType.RBRACE, "Expected '}' after while body");

        return whileNode;
    }

    private AstNode parseDoWhile(ParserState state) {
        state.consume(TokenType.DO, "Expected 'do'");
        state.consume(TokenType.LBRACE, "Expected '{' after do");

        List<AstNode> bodyStatements = new ArrayList<>();
        while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
            bodyStatements.add(parseStatement(state));
        }
        state.consume(TokenType.RBRACE, "Expected '}' after do body");

        state.consume(TokenType.WHILE, "Expected 'while' after do block");
        state.consume(TokenType.LPAREN, "Expected '(' after while in do-while");
        int condStart = state.peek().startPos();
        skipBalanced(state, TokenType.LPAREN, TokenType.RPAREN);
        Token endParen = state.previous();
        int condEnd = state.getTokens().get(state.getCurrentIndex() - 2).endPos();

        if (state.check(TokenType.SEMICOLON)) {
            state.advance();
        }

        String conditionText = state.substring(condStart, condEnd).trim();
        String rawText = "do ... while (" + conditionText + ");";
        AstNode doWhileNode = new AstNode(NodeType.DO_WHILE, rawText);

        for (AstNode stmt : bodyStatements) {
            doWhileNode.addChild(stmt);
        }

        return doWhileNode;
    }

    private AstNode parseFor(ParserState state) {
        Token forTok = state.consume(TokenType.FOR, "Expected 'for'");
        int startPos = forTok.startPos();

        state.consume(TokenType.LPAREN, "Expected '(' after for");

        // Сбор токенов внутри заголовка for(...)
        List<Token> headerTokens = new ArrayList<>();
        int parenDepth = 1;
        while (parenDepth > 0 && !state.isAtEnd()) {
            Token t = state.advance();
            if (t.type() == TokenType.LPAREN) {
                parenDepth++;
            } else if (t.type() == TokenType.RPAREN) {
                parenDepth--;
            }
            if (parenDepth > 0) {
                headerTokens.add(t);
            }
        }
        Token endParen = state.previous();

        // Проверка наличия 'of' или 'in' на верхнем уровне скобок
        NodeType forType = NodeType.FOR;
        int innerDepth = 0;
        for (Token tok : headerTokens) {
            if (tok.type() == TokenType.LPAREN || tok.type() == TokenType.LBRACKET) {
                innerDepth++;
            } else if (tok.type() == TokenType.RPAREN || tok.type() == TokenType.RBRACKET) {
                innerDepth--;
            } else if (innerDepth == 0) {
                if (tok.type() == TokenType.OF) {
                    forType = NodeType.FOR_OF;
                    break;
                }
                if (tok.type() == TokenType.IN) {
                    forType = NodeType.FOR_IN;
                    break;
                }
            }
        }

        String rawHeader = state.substring(startPos, endParen.endPos()).trim();
        AstNode forNode = new AstNode(forType, rawHeader);

        state.consume(TokenType.LBRACE, "Expected '{' after for header");
        while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
            forNode.addChild(parseStatement(state));
        }
        state.consume(TokenType.RBRACE, "Expected '}' after for body");

        return forNode;
    }

    private AstNode parseIf(ParserState state) {
        Token ifTok = state.consume(TokenType.IF, "Expected 'if'");
        int startPos = ifTok.startPos();

        state.consume(TokenType.LPAREN, "Expected '(' after if");
        skipBalanced(state, TokenType.LPAREN, TokenType.RPAREN);
        Token endParen = state.previous();

        String rawHeader = state.substring(startPos, endParen.endPos()).trim();
        AstNode ifNode = new AstNode(NodeType.IF, rawHeader);

        state.consume(TokenType.LBRACE, "Expected '{' after if condition");
        while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
            ifNode.addChild(parseStatement(state));
        }
        state.consume(TokenType.RBRACE, "Expected '}' after if block");

        // Обработка опциональной ветки else
        if (state.check(TokenType.ELSE)) {
            state.advance(); // consume 'else'
            if (state.check(TokenType.IF)) {
                // 'else if'
                AstNode elseNode = new AstNode(NodeType.ELSE, "else");
                elseNode.addChild(parseIf(state));
                ifNode.addChild(elseNode);
            } else if (state.check(TokenType.LBRACE)) {
                // 'else { ... }'
                AstNode elseNode = new AstNode(NodeType.ELSE, "else");
                state.consume(TokenType.LBRACE, "Expected '{' after else");
                while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
                    elseNode.addChild(parseStatement(state));
                }
                state.consume(TokenType.RBRACE, "Expected '}' after else block");
                ifNode.addChild(elseNode);
            }
        }

        return ifNode;
    }

    private AstNode parseSwitch(ParserState state) {
        Token switchTok = state.consume(TokenType.SWITCH, "Expected 'switch'");
        int startPos = switchTok.startPos();

        state.consume(TokenType.LPAREN, "Expected '(' after switch");
        skipBalanced(state, TokenType.LPAREN, TokenType.RPAREN);
        Token endParen = state.previous();

        String rawHeader = state.substring(startPos, endParen.endPos()).trim();
        AstNode switchNode = new AstNode(NodeType.SWITCH, rawHeader);

        state.consume(TokenType.LBRACE, "Expected '{' after switch header");
        while (!state.check(TokenType.RBRACE) && !state.isAtEnd()) {
            if (state.check(TokenType.CASE)) {
                Token caseTok = state.advance();
                int caseStart = caseTok.startPos();
                while (!state.check(TokenType.COLON) && !state.isAtEnd()) {
                    state.advance();
                }
                Token colon = state.consume(TokenType.COLON, "Expected ':' after case value");
                String rawCase = state.substring(caseStart, colon.endPos()).trim();
                AstNode caseNode = new AstNode(NodeType.CASE, rawCase);
                switchNode.addChild(caseNode);

                while (!state.check(TokenType.CASE) && !state.check(TokenType.DEFAULT)
                        && !state.check(TokenType.RBRACE) && !state.isAtEnd()) {
                    caseNode.addChild(parseStatement(state));
                }
            } else if (state.check(TokenType.DEFAULT)) {
                Token defaultTok = state.advance();
                Token colon = state.consume(TokenType.COLON, "Expected ':' after default");
                String rawDefault = state.substring(defaultTok.startPos(), colon.endPos()).trim();
                AstNode defaultNode = new AstNode(NodeType.DEFAULT, rawDefault);
                switchNode.addChild(defaultNode);

                while (!state.check(TokenType.CASE) && !state.check(TokenType.DEFAULT)
                        && !state.check(TokenType.RBRACE) && !state.isAtEnd()) {
                    defaultNode.addChild(parseStatement(state));
                }
            } else {
                state.advance();
            }
        }
        state.consume(TokenType.RBRACE, "Expected '}' after switch body");

        return switchNode;
    }

    private AstNode parseReturn(ParserState state) {
        Token retTok = state.consume(TokenType.RETURN, "Expected 'return'");
        int startPos = retTok.startPos();

        List<Token> stmtTokens = new ArrayList<>();
        stmtTokens.add(retTok);

        int depth = 0;
        while (!state.isAtEnd()) {
            if (depth == 0 && state.check(TokenType.SEMICOLON)) {
                break;
            }
            Token t = state.advance();
            stmtTokens.add(t);
            if (t.type() == TokenType.LPAREN || t.type() == TokenType.LBRACKET || t.type() == TokenType.LBRACE) {
                depth++;
            } else if (t.type() == TokenType.RPAREN || t.type() == TokenType.RBRACKET || t.type() == TokenType.RBRACE) {
                depth--;
            }
        }

        Token semi = state.consume(TokenType.SEMICOLON, "Expected ';' after return");
        String rawText = state.substring(startPos, semi.endPos()).trim();
        AstNode returnNode = new AstNode(NodeType.RETURN, rawText);

        extractAndAttachTernaries(state, stmtTokens, returnNode);
        return returnNode;
    }

    private AstNode parseBreak(ParserState state) {
        state.consume(TokenType.BREAK, "Expected 'break'");
        if (state.check(TokenType.SEMICOLON)) {
            state.advance();
        }
        return new AstNode(NodeType.BREAK, "break;");
    }

    private AstNode parseContinue(ParserState state) {
        state.consume(TokenType.CONTINUE, "Expected 'continue'");
        if (state.check(TokenType.SEMICOLON)) {
            state.advance();
        }
        return new AstNode(NodeType.CONTINUE, "continue;");
    }

    private AstNode parseExpressionOrAssignment(ParserState state) {
        int startPos = state.peek().startPos();

        List<Token> stmtTokens = new ArrayList<>();
        int depth = 0;
        boolean hasTopLevelAssignment = false;

        while (!state.isAtEnd()) {
            if (depth == 0 && state.check(TokenType.SEMICOLON)) {
                break;
            }
            Token t = state.advance();
            stmtTokens.add(t);

            if (depth == 0 && t.type() == TokenType.ASSIGN_OP) {
                hasTopLevelAssignment = true;
            }

            if (t.type() == TokenType.LPAREN || t.type() == TokenType.LBRACKET || t.type() == TokenType.LBRACE) {
                depth++;
            } else if (t.type() == TokenType.RPAREN || t.type() == TokenType.RBRACKET || t.type() == TokenType.RBRACE) {
                depth--;
            }
        }

        Token semi = state.consume(TokenType.SEMICOLON, "Expected ';' at end of statement");
        String rawText = state.substring(startPos, semi.endPos()).trim();

        NodeType nodeType = hasTopLevelAssignment ? NodeType.ASSIGNMENT : NodeType.EXPRESSION_STATEMENT;
        AstNode statementNode = new AstNode(nodeType, rawText);

        extractAndAttachTernaries(state, stmtTokens, statementNode);
        return statementNode;
    }

    /**
     * Находит тернарные операторы в списке токенов инструкции и прикрепляет дочерние узлы TERNARY.
     */
    /**
     * Находит тернарные операторы в списке токенов инструкции и прикрепляет их с учетом вложенности.
     * Если внутри одного тернарника встречается следующий — он становится дочерним узлом первого,
     * увеличивая уровень вложенности для глубоких выражений.
     */
    private void extractAndAttachTernaries(ParserState state, List<Token> tokens, AstNode parentNode) {
        int i = 0;
        int count = tokens.size();
        AstNode currentTarget = parentNode;

        while (i < count) {
            if (tokens.get(i).type() == TokenType.QUESTION) {
                int questionIdx = i;

                int condStartIdx = questionIdx - 1;
                int depth = 0;
                while (condStartIdx > 0) {
                    Token t = tokens.get(condStartIdx);
                    if (t.type() == TokenType.RPAREN || t.type() == TokenType.RBRACKET) {
                        depth++;
                    } else if (t.type() == TokenType.LPAREN || t.type() == TokenType.LBRACKET) {
                        if (depth > 0) {
                            depth--;
                        } else {
                            break;
                        }
                    } else if (depth == 0) {
                        if (t.type() == TokenType.ASSIGN_OP || t.type() == TokenType.COMMA
                                || t.type() == TokenType.RETURN || t.type() == TokenType.COLON) {
                            condStartIdx++;
                            break;
                        }
                    }
                    if (condStartIdx == 0) {
                        break;
                    }
                    condStartIdx--;
                }

                int colonIdx = -1;
                int forwardDepth = 0;
                for (int j = questionIdx + 1; j < count; j++) {
                    Token t = tokens.get(j);
                    if (t.type() == TokenType.LPAREN || t.type() == TokenType.LBRACKET) {
                        forwardDepth++;
                    } else if (t.type() == TokenType.RPAREN || t.type() == TokenType.RBRACKET) {
                        forwardDepth--;
                    } else if (forwardDepth == 0 && t.type() == TokenType.COLON) {
                        colonIdx = j;
                        break;
                    }
                }

                if (colonIdx != -1) {
                    int falseEndIdx = colonIdx + 1;
                    int exprDepth = 0;
                    for (int k = colonIdx + 1; k < count; k++) {
                        Token t = tokens.get(k);
                        if (t.type() == TokenType.LPAREN || t.type() == TokenType.LBRACKET) {
                            exprDepth++;
                        } else if (t.type() == TokenType.RPAREN || t.type() == TokenType.RBRACKET) {
                            if (exprDepth > 0) {
                                exprDepth--;
                            } else {
                                break;
                            }
                        } else if (exprDepth == 0 && (t.type() == TokenType.COMMA || t.type() == TokenType.SEMICOLON)) {
                            break;
                        }
                        falseEndIdx = k;
                    }

                    int ternaryStart = tokens.get(condStartIdx).startPos();
                    int ternaryEnd = tokens.get(falseEndIdx).endPos();
                    String ternaryRaw = state.substring(ternaryStart, ternaryEnd).trim();

                    AstNode ternaryNode = new AstNode(NodeType.TERNARY, ternaryRaw);
                    currentTarget.addChild(ternaryNode);
                    // Для цепочки тернарников (a ? b : c ? d : e) вкладываем следующий тернарник внутрь предыдущего
                    currentTarget = ternaryNode;

                    i = colonIdx;
                    continue;
                }
            }
            i++;
        }
    }

    private void skipBalanced(ParserState state, TokenType open, TokenType close) {
        int depth = 1;
        while (depth > 0 && !state.isAtEnd()) {
            Token t = state.advance();
            if (t.type() == open) {
                depth++;
            } else if (t.type() == close) {
                depth--;
            }
        }
    }
}
