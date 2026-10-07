package jilba;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GilbMetricAnalyzer {

    private final JsAstParser parser;

    public GilbMetricResult analyze(String sourceCode) {
        Objects.requireNonNull(sourceCode, "sourceCode must not be null");
        if (sourceCode.isBlank()) {
            throw new IllegalArgumentException("sourceCode must not be blank");
        }

        AstNode root = parser.parse(sourceCode);
        return analyze(root);
    }

    public GilbMetricResult analyze(AstNode root) {
        Objects.requireNonNull(root, "root must not be null");

        String astTreeString = JsAstParser.toTreeString(root);
        int absoluteComplexity = countConditionalNodes(root);
        int totalStatements = countTotalStatements(root);
        double relativeComplexity = totalStatements == 0 ? 0.0 : (double) absoluteComplexity / totalStatements;
        int maxNestingLevel = calculateMaxNestingLevel(root, 0);

        return new GilbMetricResult(
                absoluteComplexity,
                totalStatements,
                relativeComplexity,
                maxNestingLevel,
                astTreeString
        );
    }

    private int countConditionalNodes(AstNode node) {
        int count = node.isConditional() ? 1 : 0;
        for (AstNode child : node.getChildren()) {
            count += countConditionalNodes(child);
        }
        return count;
    }

    private int countTotalStatements(AstNode root) {
        if (root.getType() == NodeType.FUNCTION) {
            int count = 0;
            for (AstNode child : root.getChildren()) {
                count += countAllNodes(child);
            }
            return count;
        }
        return countAllNodes(root);
    }

    private int countAllNodes(AstNode node) {
        // TERNARY — выражение внутри оператора (присваивания, return и т.д.),
        // поэтому отдельным оператором в N_total не считается.
        // Дочерние узлы тернарника (если вложены другие тернарники) обходим дальше.
        int count = (node.getType() == NodeType.TERNARY) ? 0 : 1;
        for (AstNode child : node.getChildren()) {
            count += countAllNodes(child);
        }
        return count;
    }

    private int calculateMaxNestingLevel(AstNode node, int currentDepth) {
        int depth = node.isConditional() ? currentDepth + 1 : currentDepth;

        if (node.getType() == NodeType.SWITCH) {
            int max = depth;
            int caseOffset = 0;
            for (AstNode child : node.getChildren()) {
                if (child.getType() == NodeType.CASE) {
                    caseOffset++;
                    max = Math.max(max, calculateMaxNestingLevel(child, depth + caseOffset - 1));
                } else {
                    max = Math.max(max, calculateMaxNestingLevel(child, depth + caseOffset));
                }
            }
            return max;
        }

        int max = depth;
        for (AstNode child : node.getChildren()) {
            max = Math.max(max, calculateMaxNestingLevel(child, depth));
        }
        return max;
    }
}