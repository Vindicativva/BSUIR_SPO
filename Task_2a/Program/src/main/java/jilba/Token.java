package jilba;

import java.util.Objects;

/**
 * Неизменяемая запись (record), представляющая токен (лексему) в исходном коде.
 *
 * @param type     тип токена ({@link TokenType})
 * @param text     текстовое значение токена в исходном коде
 * @param startPos начальная позиция токена в исходном коде (индекс символа, включительно)
 * @param endPos   конечная позиция токена в исходном коде (индекс символа, исключительно)
 */
public record Token(TokenType type, String text, int startPos, int endPos) {

    public Token {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(text, "text must not be null");
        if (startPos < 0) {
            throw new IllegalArgumentException("startPos cannot be negative: " + startPos);
        }
        if (endPos < startPos) {
            throw new IllegalArgumentException(
                    "endPos (" + endPos + ") cannot be less than startPos (" + startPos + ")"
            );
        }
    }
}
