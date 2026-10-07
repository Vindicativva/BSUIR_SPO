package jilba;

import lombok.Getter;
import lombok.ToString;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Узел абстрактного синтаксического дерева (AST), представляющий синтаксическую
 * конструкцию или инструкцию в исходном коде упрощенного JavaScript.
 */
@Getter
@ToString
public class AstNode {

    private final NodeType type;
    private final String rawText;
    private final boolean isConditional;

    @Getter(lombok.AccessLevel.NONE)
    @ToString.Exclude
    private final List<AstNode> children;

    /**
     * Создает новый узел AST с указанным типом и фрагментом исходного кода.
     *
     * @param type    тип узла AST ({@link NodeType})
     * @param rawText исходный фрагмент кода, соответствующий данному узлу
     * @throws NullPointerException если type или rawText равен null
     */
    public AstNode(NodeType type, String rawText) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.rawText = Objects.requireNonNull(rawText, "rawText must not be null").trim();
        this.isConditional = type.isConditional();
        this.children = new ArrayList<>();
    }

    /**
     * Возвращает неизменяемый снимок списка дочерних узлов.
     *
     * @return неизменяемый список дочерних узлов {@link AstNode}
     */
    public List<AstNode> getChildren() {
        return List.copyOf(children);
    }

    /**
     * Добавляет дочерний узел в поддерево данного узла.
     *
     * @param child добавляемый дочерний узел
     * @throws NullPointerException если child равен null
     */
    public void addChild(AstNode child) {
        Objects.requireNonNull(child, "child must not be null");
        children.add(child);
    }

    /**
     * Выводит иерархию данного узла и его дочерних элементов в стандартный поток вывода {@link System#out}.
     */
    public void printTree() {
        printTree(System.out);
    }

    /**
     * Выводит иерархию данного узла и его дочерних элементов в указанный поток вывода {@link PrintStream}.
     *
     * @param out целевой поток вывода
     * @throws NullPointerException если out равен null
     */
    public void printTree(PrintStream out) {
        JsAstParser.prettyPrint(this, out);
    }
}
