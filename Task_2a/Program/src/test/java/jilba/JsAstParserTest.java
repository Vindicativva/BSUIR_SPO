package jilba;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsAstParserTest {

    private JsAstParser parser;

    @BeforeEach
    void setUp() {
        parser = new JsAstParser(new JsLexer());
    }

    private String normalize(String str) {
        return str.replace("\r\n", "\n").trim();
    }

    @Test
    @DisplayName("Проверка парсинга while, вложенного for...of и инкремента")
    void testLoops() {
        String js = """
            function testLoops(items) {
                let count = 0;
                while (count < items.length) {
                    for (const x of items) {
                        count++;
                    }
                }
            }
            """;

        AstNode root = parser.parse(js);

        String expected = """
            [FUNCTION] function testLoops(items)
                [VAR_DECLARATION] let count = 0;
                [WHILE] while (count < items.length) (conditional)
                    [FOR_OF] for (const x of items) (conditional)
                        [EXPRESSION_STATEMENT] count++;
            """;

        assertThat(normalize(JsAstParser.toTreeString(root))).isEqualTo(normalize(expected));
    }

    @Test
    @DisplayName("Проверка парсинга цикла do...while")
    void testDoWhile() {
        String js = """
            function testDoWhile() {
                let retries = 3;
                do {
                    retries--;
                } while (retries > 0);
            }
            """;

        AstNode root = parser.parse(js);

        String expected = """
            [FUNCTION] function testDoWhile()
                [VAR_DECLARATION] let retries = 3;
                [DO_WHILE] do ... while (retries > 0); (conditional)
                    [EXPRESSION_STATEMENT] retries--;
            """;

        assertThat(normalize(JsAstParser.toTreeString(root))).isEqualTo(normalize(expected));
    }

    @Test
    @DisplayName("Проверка switch, веток case, default и операторов break")
    void testSwitch() {
        String js = """
            function testSwitch(val) {
                let res = 0;
                switch (val) {
                    case 1:
                        res = 10;
                        break;
                    default:
                        res = 5;
                        break;
                }
            }
            """;

        AstNode root = parser.parse(js);

        String expected = """
            [FUNCTION] function testSwitch(val)
                [VAR_DECLARATION] let res = 0;
                [SWITCH] switch (val)
                    [CASE] case 1: (conditional)
                        [ASSIGNMENT] res = 10;
                        [BREAK] break;
                    [DEFAULT] default:
                        [ASSIGNMENT] res = 5;
                        [BREAK] break;
            """;

        assertThat(normalize(JsAstParser.toTreeString(root))).isEqualTo(normalize(expected));
    }

    @Test
    @DisplayName("Проверка циклов for...in, классического for и тернарного оператора в присваивании")
    void testTernaryAndFor() {
        String js = """
            function testTernaryAndFor(obj) {
                let sum = 0;
                for (let k in obj) {
                    sum += (k > 0) ? 2 : 1;
                }
                for (let j = 0; j < 3; j++) {
                    sum += 1;
                }
            }
            """;

        AstNode root = parser.parse(js);

        String expected = """
            [FUNCTION] function testTernaryAndFor(obj)
                [VAR_DECLARATION] let sum = 0;
                [FOR_IN] for (let k in obj) (conditional)
                    [ASSIGNMENT] sum += (k > 0) ? 2 : 1;
                        [TERNARY] (k > 0) ? 2 : 1 (conditional)
                [FOR] for (let j = 0; j < 3; j++) (conditional)
                    [ASSIGNMENT] sum += 1;
            """;

        assertThat(normalize(JsAstParser.toTreeString(root))).isEqualTo(normalize(expected));
    }

    @Test
    @DisplayName("Проверка цепочки if ... else if, вызовов функций и return")
    void testBranchingAndReturn() {
        String js = """
            function testBranching(score) {
                if (score > 100) {
                    score -= 5;
                } else if (score > 50) {
                    score += 2;
                }
                console.log(score);
                return score;
            }
            """;

        AstNode root = parser.parse(js);

        String expected = """
            [FUNCTION] function testBranching(score)
                [IF] if (score > 100) (conditional)
                    [ASSIGNMENT] score -= 5;
                    [ELSE] else
                        [IF] if (score > 50) (conditional)
                            [ASSIGNMENT] score += 2;
                [EXPRESSION_STATEMENT] console.log(score);
                [RETURN] return score;
            """;

        assertThat(normalize(JsAstParser.toTreeString(root))).isEqualTo(normalize(expected));
    }

    @Test
    @DisplayName("Интеграционный тест на полном демонстрационном коде processGameSession")
    void testProcessGameSession() {
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

        AstNode root = parser.parse(js);

        assertThat(root).isNotNull();
        assertThat(root.getType()).isEqualTo(NodeType.FUNCTION);
        assertThat(root.getChildren()).isNotEmpty();

        GilbMetricResult metric = JsAstParser.calculateGilbMetric(root);
        assertThat(metric.absoluteComplexity()).isGreaterThan(0);
        assertThat(metric.absoluteComplexity()).isEqualTo(11);
        assertThat(metric.totalStatements()).isEqualTo(32);
        assertThat(metric.maxNestingLevel()).isEqualTo(6);
        assertThat(metric.relativeComplexity()).isEqualTo(11.0 / 32.0);
    }
}
