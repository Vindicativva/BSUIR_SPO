package jilba;

/**
 * Перечисление типов лексем (токенов), выделяемых лексическим анализатором
 * при разборе исходного кода на упрощенном диалекте JavaScript.
 */
public enum TokenType {

    // Ключевые слова (Keywords)
    FUNCTION,
    VAR,
    LET,
    CONST,
    IF,
    ELSE,
    WHILE,
    DO,
    FOR,
    OF,
    IN,
    SWITCH,
    CASE,
    DEFAULT,
    RETURN,
    BREAK,
    CONTINUE,

    // Разделители и знаки пунктуации (Delimiters & Punctuators)
    LBRACE,      // {
    RBRACE,      // }
    LPAREN,      // (
    RPAREN,      // )
    LBRACKET,    // [
    RBRACKET,    // ]
    SEMICOLON,   // ;
    COLON,       // :
    COMMA,       // ,
    QUESTION,    // ?
    DOT,         // .

    // Операторы присваивания (Assignment Operators)
    ASSIGN_OP,   // =, +=, -=, *=, /=, %=, **=, <<=, >>=, >>>=, &=, ^=, |=, &&=, ||=, ??=

    // Операторы обновления (Update Operators)
    UPDATE_OP,   // ++, --

    // Прочие операторы (Comparison, Arithmetic, Logical, Arrow Operators)
    OPERATOR,    // ==, ===, !=, !==, <=, >=, =>, **, &&, ||, ??, <<, >>, +, -, *, /, %, etc.

    // Литералы и идентификаторы (Literals & Identifiers)
    IDENTIFIER,
    NUMBER,
    STRING,

    // Конец потока токенов (End of File / Input)
    EOF
}
