package jilba;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Лексический анализатор (токенизатор) для упрощенного диалекта JavaScript.
 * Преобразует исходный текст программы в последовательность лексем ({@link Token}).
 */
@Component
public class JsLexer {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("function", TokenType.FUNCTION),
            Map.entry("var", TokenType.VAR),
            Map.entry("let", TokenType.LET),
            Map.entry("const", TokenType.CONST),
            Map.entry("if", TokenType.IF),
            Map.entry("else", TokenType.ELSE),
            Map.entry("while", TokenType.WHILE),
            Map.entry("do", TokenType.DO),
            Map.entry("for", TokenType.FOR),
            Map.entry("of", TokenType.OF),
            Map.entry("in", TokenType.IN),
            Map.entry("switch", TokenType.SWITCH),
            Map.entry("case", TokenType.CASE),
            Map.entry("default", TokenType.DEFAULT),
            Map.entry("return", TokenType.RETURN),
            Map.entry("break", TokenType.BREAK),
            Map.entry("continue", TokenType.CONTINUE)
    );

    /**
     * Выполняет лексический разбор исходного кода на упрощенном JavaScript.
     *
     * @param source исходный текст программы
     * @return список распознанных токенов, завершающийся токеном {@link TokenType#EOF}
     * @throws NullPointerException если source равен null
     */
    public List<Token> tokenize(String source) {
        Objects.requireNonNull(source, "source must not be null");
        List<Token> tokens = new ArrayList<>();
        int length = source.length();
        int pos = 0;

        while (pos < length) {
            char c = source.charAt(pos);

            if (Character.isWhitespace(c)) {
                pos++;
                continue;
            }

            // Строковые литералы
            if (c == '"' || c == '\'' || c == '`') {
                int start = pos;
                char quote = c;
                pos++;
                while (pos < length && source.charAt(pos) != quote) {
                    if (source.charAt(pos) == '\\' && pos + 1 < length) {
                        pos += 2;
                    } else {
                        pos++;
                    }
                }
                if (pos < length) {
                    pos++; // закрывающая кавычка
                }
                tokens.add(new Token(TokenType.STRING, source.substring(start, pos), start, pos));
                continue;
            }

            // Числовые литералы
            if (Character.isDigit(c)) {
                int start = pos;
                while (pos < length && (Character.isDigit(source.charAt(pos)) || source.charAt(pos) == '.')) {
                    pos++;
                }
                tokens.add(new Token(TokenType.NUMBER, source.substring(start, pos), start, pos));
                continue;
            }

            // Идентификаторы и ключевые слова
            if (Character.isLetter(c) || c == '_' || c == '$') {
                int start = pos;
                while (pos < length && (Character.isLetterOrDigit(source.charAt(pos))
                        || source.charAt(pos) == '_' || source.charAt(pos) == '$')) {
                    pos++;
                }
                String text = source.substring(start, pos);
                TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
                tokens.add(new Token(type, text, start, pos));
                continue;
            }

            // 4-символьные операторы
            if (pos + 4 <= length) {
                String s4 = source.substring(pos, pos + 4);
                if (s4.equals(">>>=")) {
                    tokens.add(new Token(TokenType.ASSIGN_OP, s4, pos, pos + 4));
                    pos += 4;
                    continue;
                }
            }

            // 3-символьные операторы
            if (pos + 3 <= length) {
                String s3 = source.substring(pos, pos + 3);
                if (s3.equals("===") || s3.equals("!==") || s3.equals(">>>")) {
                    tokens.add(new Token(TokenType.OPERATOR, s3, pos, pos + 3));
                    pos += 3;
                    continue;
                }
                if (s3.equals("**=") || s3.equals("<<=") || s3.equals(">>=")
                        || s3.equals("&&=") || s3.equals("||=") || s3.equals("??=")) {
                    tokens.add(new Token(TokenType.ASSIGN_OP, s3, pos, pos + 3));
                    pos += 3;
                    continue;
                }
            }

            // 2-символьные операторы
            if (pos + 2 <= length) {
                String s2 = source.substring(pos, pos + 2);
                if (s2.equals("+=") || s2.equals("-=") || s2.equals("*=") || s2.equals("/=")
                        || s2.equals("%=") || s2.equals("&=") || s2.equals("|=") || s2.equals("^=")) {
                    tokens.add(new Token(TokenType.ASSIGN_OP, s2, pos, pos + 2));
                    pos += 2;
                    continue;
                }
                if (s2.equals("++") || s2.equals("--")) {
                    tokens.add(new Token(TokenType.UPDATE_OP, s2, pos, pos + 2));
                    pos += 2;
                    continue;
                }
                if (s2.equals("==") || s2.equals("!=") || s2.equals("<=") || s2.equals(">=")
                        || s2.equals("=>") || s2.equals("**") || s2.equals("&&") || s2.equals("||")
                        || s2.equals("??") || s2.equals("<<") || s2.equals(">>")) {
                    tokens.add(new Token(TokenType.OPERATOR, s2, pos, pos + 2));
                    pos += 2;
                    continue;
                }
            }

            // Одиночные разделители и операторы
            TokenType singleType = switch (c) {
                case '{' -> TokenType.LBRACE;
                case '}' -> TokenType.RBRACE;
                case '(' -> TokenType.LPAREN;
                case ')' -> TokenType.RPAREN;
                case '[' -> TokenType.LBRACKET;
                case ']' -> TokenType.RBRACKET;
                case ';' -> TokenType.SEMICOLON;
                case ':' -> TokenType.COLON;
                case ',' -> TokenType.COMMA;
                case '?' -> TokenType.QUESTION;
                case '.' -> TokenType.DOT;
                case '=' -> TokenType.ASSIGN_OP;
                default -> TokenType.OPERATOR;
            };
            tokens.add(new Token(singleType, String.valueOf(c), pos, pos + 1));
            pos++;
        }

        tokens.add(new Token(TokenType.EOF, "", length, length));
        return List.copyOf(tokens);
    }
}
