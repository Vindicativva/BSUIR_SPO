package jilba;

import jilba.ui.MainController;
import jilba.ui.ResultController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class GilbConfigTest {

    @Test
    @DisplayName("Поднятие контекста Spring Framework и внедрение зависимостей")
    void testSpringContext() {
        try (var context = new AnnotationConfigApplicationContext(GilbConfig.class)) {
            assertThat(context.containsBean("gilbConfig")).isTrue();

            JsLexer lexer = context.getBean(JsLexer.class);
            assertThat(lexer).isNotNull();

            JsAstParser parser = context.getBean(JsAstParser.class);
            assertThat(parser).isNotNull();

            GilbMetricAnalyzer analyzer = context.getBean(GilbMetricAnalyzer.class);
            assertThat(analyzer).isNotNull();

            MainController mainController = context.getBean(MainController.class);
            assertThat(mainController).isNotNull();

            ResultController resultController = context.getBean(ResultController.class);
            assertThat(resultController).isNotNull();

            GilbMetricResult result = analyzer.analyze("function test() { let a = 1; return a; }");
            assertThat(result).isNotNull();
            assertThat(result.totalStatements()).isEqualTo(2);
            assertThat(result.absoluteComplexity()).isEqualTo(0);
        }
    }
}
