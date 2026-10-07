package jilba;

/**
 * Перечисление типов узлов абстрактного синтаксического дерева (AST)
 * для упрощенного диалекта JavaScript.
 */
public enum NodeType {

    // Условные и управляющие узлы (Control & Conditional Nodes)

    /** Объявление функции: {@code function name(...) { ... }}. */
    FUNCTION,

    /** Условный оператор: {@code if (...) { ... }}. */
    IF,

    /** Альтернативная ветка условия: {@code else { ... }}. */
    ELSE,

    /** Цикл с предусловием: {@code while (...) { ... }}. */
    WHILE,

    /** Цикл с постусловием: {@code do { ... } while (...);}. */
    DO_WHILE,

    /** Классический цикл со счетчиком: {@code for (...; ...; ...) { ... }}. */
    FOR,

    /** Цикл обхода итерируемых объектов: {@code for (... of ...) { ... }}. */
    FOR_OF,

    /** Цикл обхода свойств объекта: {@code for (... in ...) { ... }}. */
    FOR_IN,

    /** Блок переключателя вариантов: {@code switch (...) { ... }}. */
    SWITCH,

    /** Ветка варианта в блоке switch: {@code case <val>:}. */
    CASE,

    /** Ветка по умолчанию в блоке switch: {@code default:}. */
    DEFAULT,

    /** Тернарный условный оператор: {@code condition ? expr1 : expr2}. */
    TERNARY,

    // Простые операторы (Simple Statements)

    /** Объявление переменных: {@code var}, {@code let}, {@code const}. */
    VAR_DECLARATION,

    /** Оператор присваивания: {@code =}, {@code +=}, {@code -=} и т.д. */
    ASSIGNMENT,

    /** Оператор-выражение: вызовы функций, инкременты/декременты и т.д. */
    EXPRESSION_STATEMENT,

    /** Оператор возврата из функции: {@code return ...;}. */
    RETURN,

    /** Оператор прерывания цикла/switch: {@code break;}. */
    BREAK,

    /** Оператор перехода к следующей итерации цикла: {@code continue;}. */
    CONTINUE;

    /**
     * Определяет, является ли данный тип узла условным оператором (точкой ветвления)
     * согласно метрике сложности Джилба (Gilb's metric).
     *
     * @return true для IF, WHILE, DO_WHILE, FOR, FOR_OF, FOR_IN, CASE, DEFAULT, TERNARY;
     *         false для остальных типов узлов
     */
    public boolean isConditional() {
        return switch (this) {
            case IF, WHILE, DO_WHILE, FOR, FOR_OF, FOR_IN, CASE, TERNARY -> true;
            default -> false;
        };
    }
}
