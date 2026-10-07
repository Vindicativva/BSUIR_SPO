package jilba;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GilbMetricAnalyzerTest {

    private GilbMetricAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new GilbMetricAnalyzer(new JsAstParser(new JsLexer()));
    }

    @Test
    @DisplayName("Анализ демонстрационного кода processGameSession")
    void testAnalyzeProcessGameSession() {
        String js = """
            function processGameSession(playerSessions, baseConfig) {
                let totalScore = 0;
                let sessionIdx = 0;

                while (sessionIdx < playerSessions.length) {
                    for (const item of playerSessions[sessionIdx].inventory) {
                        if (item.isEquipped) {
                            switch (item.rarity) {
                                case 1:
                                    totalScore += 10;
                                    break;
                                case 2:
                                    totalScore += 25;
                                    break;
                                case 3:
                                    totalScore += 50;
                                    break;
                                default:
                                    totalScore += 5;
                                    break;
                            }
                        }
                    }
                    sessionIdx++;
                }

                let retryCooldown = 3;
                do {
                    totalScore += retryCooldown;
                    retryCooldown--;
                } while (retryCooldown > 0);

                for (let rule in baseConfig) {
                    totalScore += (baseConfig[rule] > 0) ? 2 : 1;
                }

                for (let j = 0; j < 3; j++) {
                    if (totalScore > 100) {
                        totalScore -= 5;
                    } else {
                        totalScore += 2;
                    }
                }

                console.log(totalScore);
                return totalScore;
            }
            """;

        GilbMetricResult result = analyzer.analyze(js);

        assertThat(result.absoluteComplexity()).isEqualTo(11);
        assertThat(result.totalStatements()).isEqualTo(32);
        assertThat(result.relativeComplexity()).isEqualTo(11.0 / 32.0);
        assertThat(result.maxNestingLevel()).isEqualTo(6);
        assertThat(result.astTreeString()).contains("processGameSession");
        assertThat(result.toString()).contains("CL=11", "N_total=32", "CLI=6");
    }

    @Test
    @DisplayName("Код без условных операторов: CL=0, cl=0.0, CLI=0")
    void testNoConditionals() {
        String js = """
            function compute() {
                let x = 10;
                return x;
            }
            """;

        GilbMetricResult result = analyzer.analyze(js);

        assertThat(result.absoluteComplexity()).isEqualTo(0);
        assertThat(result.totalStatements()).isEqualTo(2);
        assertThat(result.relativeComplexity()).isEqualTo(0.0);
        assertThat(result.maxNestingLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("Пустая функция: totalStatements=0, relativeComplexity=0.0")
    void testEmptyFunction() {
        String js = "function empty() {}";

        GilbMetricResult result = analyzer.analyze(js);

        assertThat(result.absoluteComplexity()).isEqualTo(0);
        assertThat(result.totalStatements()).isEqualTo(0);
        assertThat(result.relativeComplexity()).isEqualTo(0.0);
        assertThat(result.maxNestingLevel()).isEqualTo(0);
    }

    @Test
    @DisplayName("Глубокая вложенность условий")
    void testDeepNesting() {
        String js = """
            function nested(x) {
                while (x > 0) {
                    if (x > 10) {
                        for (let i = 0; i < 5; i++) {
                            if (i == 2) {
                                break;
                            }
                        }
                    }
                }
            }
            """;

        GilbMetricResult result = analyzer.analyze(js);

        assertThat(result.absoluteComplexity()).isEqualTo(4);
        assertThat(result.maxNestingLevel()).isEqualTo(4);
    }

    @Test
    @DisplayName("Fail-fast валидация некорректного ввода")
    void testValidation() {
        assertThatThrownBy(() -> analyzer.analyze((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("sourceCode must not be null");

        assertThatThrownBy(() -> analyzer.analyze("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceCode must not be blank");
    }

    @Test
    @DisplayName("Компактный конструктор GilbMetricResult проверяет неотрицательность")
    void testRecordValidation() {
        assertThatThrownBy(() -> new GilbMetricResult(-1, 5, 0.0, 1, "tree"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GilbMetricResult(1, -5, 0.0, 1, "tree"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GilbMetricResult(1, 5, 0.0, -1, "tree"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GilbMetricResult(1, 5, 0.0, 1, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Тернарный оператор увеличивает уровень вложенности, но не добавляет оператор в N_total")
    void testTernaryMetrics() {
        String js = """
            function testTernary(a) {
                let x = (a > 0) ? 1 : 2;
                return x;
            }
            """;

        GilbMetricResult result = analyzer.analyze(js);

        assertThat(result.totalStatements()).isEqualTo(2);
        assertThat(result.absoluteComplexity()).isEqualTo(1);
        assertThat(result.maxNestingLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("Каждый case последовательно увеличивает уровень вложенности предыдущего")
    void testSwitchCaseSequentialNesting() {
        String js = """
            function testSwitch(x) {
                switch (x) {
                    case 1:
                        let a = 1;
                        break;
                    case 2:
                        let b = 2;
                        break;
                    case 3:
                        let c = 3;
                        break;
                    default:
                        let d = 4;
                        break;
                }
            }
            """;

        GilbMetricResult result = analyzer.analyze(js);

        assertThat(result.absoluteComplexity()).isEqualTo(3);
        assertThat(result.maxNestingLevel()).isEqualTo(3);
    }
}
