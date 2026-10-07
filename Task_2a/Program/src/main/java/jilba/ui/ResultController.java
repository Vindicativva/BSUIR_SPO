package jilba.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import jilba.GilbMetricResult;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Контроллер модального диалогового окна с результатами анализа сложности JavaScript-кода.
 */
@Component
@Scope("prototype")
public class ResultController {

    @FXML
    private TextArea astArea;

    @FXML
    private Label absoluteComplexityLabel;

    @FXML
    private Label relativeComplexityLabel;

    @FXML
    private Label maxNestingLevelLabel;

    /**
     * Заполняет элементы интерфейса данными вычисленных метрик Джилба.
     *
     * @param result результат анализа сложности программы
     */
    public void initData(GilbMetricResult result) {
        astArea.setText(result.astTreeString());
        absoluteComplexityLabel.setText(String.format("CL = %d", result.absoluteComplexity()));
        relativeComplexityLabel.setText(String.format(
                Locale.US,
                "cl = %.4f (%d / %d)",
                result.relativeComplexity(),
                result.absoluteComplexity(),
                result.totalStatements()
        ));
        maxNestingLevelLabel.setText(String.format("CLI = %d", result.maxNestingLevel()));
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) astArea.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
