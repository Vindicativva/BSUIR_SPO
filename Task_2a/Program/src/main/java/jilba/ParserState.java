package jilba;

import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

/**
 * Состояние синтаксического парсера при разборе потока токенов.
 * Управляет указателем текущей позиции и предоставляет вспомогательные методы
 * для навигации, сопоставления и извлечения подстрок исходного кода.
 */
@Getter
@ToString
public class ParserState {

    private final String source;
    private final List<Token> tokens;
    private int current;

    /**
     * Создает новое состояние парсера для заданного исходного кода и списка токенов.
     *
     * @param source исходный текст JavaScript-кода
     * @param tokens список токенов, полученный в результате лексического анализа
     */
    public ParserState(String source, List<Token> tokens) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens must not be null"));
        this.current = 0;
    }

    /**
     * Проверяет, достигнут ли конец потока токенов.
     *
     * @return true, если текущий токен равен {@link TokenType#EOF} или позиция превышает размер списка
     */
    public boolean isAtEnd() {
        return current >= tokens.size() || peek().type() == TokenType.EOF;
    }

    /**
     * Возвращает текущий токен без смещения указателя позиции.
     *
     * @return текущий токен
     */
    public Token peek() {
        return tokens.get(current);
    }

    /**
     * Возвращает предыдущий успешно обработанный токен.
     *
     * @return предыдущий токен
     */
    public Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Проверяет, совпадает ли тип текущего токена с переданным типом, без смещения указателя.
     *
     * @param type ожидаемый тип токена
     * @return true, если тип совпадает и конец файла не достигнут
     */
    public boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    /**
     * Перемещает указатель на следующий токен и возвращает только что прочитанный токен.
     *
     * @return прочитанный токен
     */
    public Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    /**
     * Проверяет текущий токен на соответствие ожидаемому типу и продвигает указатель,
     * либо выбрасывает {@link IllegalArgumentException} с информативным описанием ошибки.
     *
     * @param type    ожидаемый тип токена
     * @param message сообщение об ошибке при несовпадении
     * @return прочитанный токен
     */
    public Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw new IllegalArgumentException(
                message + ", found: '" + peek().text() + "' at position " + peek().startPos()
        );
    }

    /**
     * Извлекает подстроку исходного текста программы по диапазону индексов символов.
     *
     * @param start начальный индекс символа (включительно)
     * @param end   конечный индекс символа (исключительно)
     * @return подстрока исходного текста
     */
    public String substring(int start, int end) {
        return source.substring(start, end);
    }

    /**
     * Возвращает текущий индекс указателя в потоке токенов.
     *
     * @return текущий индекс
     */
    public int getCurrentIndex() {
        return current;
    }
}
