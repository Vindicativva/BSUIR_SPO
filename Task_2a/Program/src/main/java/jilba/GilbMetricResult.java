package jilba;

import java.util.Objects;

/**
 * Неизменяемый результат расчета метрик сложности Джилба (Gilb's metric)
 * для анализируемого исходного кода на JavaScript.
 *
 * @param absoluteComplexity абсолютная сложность CL (число узлов с {@code isConditional() == true})
 * @param totalStatements    общее число операторов N_total (стейтменты тела, исключая FUNCTION)
 * @param relativeComplexity относительная сложность cl = CL / N_total (0.0, если N_total == 0)
 * @param maxNestingLevel    максимальный уровень вложенности управляющих конструкций CLI
 * @param astTreeString      форматированное строковое представление всего AST-дерева
 */
public record GilbMetricResult(
        int absoluteComplexity,
        int totalStatements,
        double relativeComplexity,
        int maxNestingLevel,
        String astTreeString
) {

    public GilbMetricResult {
        Objects.requireNonNull(astTreeString, "astTreeString must not be null");
        if (absoluteComplexity < 0) {
            throw new IllegalArgumentException("absoluteComplexity must not be negative: " + absoluteComplexity);
        }
        if (totalStatements < 0) {
            throw new IllegalArgumentException("totalStatements must not be negative: " + totalStatements);
        }
        if (maxNestingLevel < 0) {
            throw new IllegalArgumentException("maxNestingLevel must not be negative: " + maxNestingLevel);
        }
    }

    /**
     * Псевдоним для совместимости, возвращающий общее число операторов {@link #totalStatements()}.
     *
     * @return общее число операторов
     */
    public int totalNodes() {
        return totalStatements;
    }

    @Override
    public String toString() {
        return String.format(
                "GilbMetricResult [CL=%d, N_total=%d, cl=%.4f, CLI=%d]",
                absoluteComplexity,
                totalStatements,
                relativeComplexity,
                maxNestingLevel
        );
    }
}
