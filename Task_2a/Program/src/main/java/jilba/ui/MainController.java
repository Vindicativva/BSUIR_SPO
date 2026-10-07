package jilba.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jilba.GilbMetricAnalyzer;
import jilba.GilbMetricResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Контроллер главного окна приложения для ввода исходного кода и запуска анализа метрик Джилба.
 */
@Component
@RequiredArgsConstructor
public class MainController {

    private static final String DEFAULT_DEMO_CODE = """
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
            """.stripIndent();

    private final GilbMetricAnalyzer analyzer;
    private final ApplicationContext applicationContext;

    @FXML
    private TextArea codeArea;

    @FXML
    private Label filePathLabel;

    @FXML
    private Button calculateButton;

    private boolean isProgrammaticChange = false;

    /**
     * Инициализирует форму: предзаполняет область ввода демонстрационным кодом и настраивает слушатели.
     */
    @FXML
    public void initialize() {
        isProgrammaticChange = true;
        codeArea.setText(DEFAULT_DEMO_CODE);
        isProgrammaticChange = false;

        filePathLabel.setText("Файл не выбран");

        // Сброс метки файла при ручном редактировании текста
        codeArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!isProgrammaticChange) {
                filePathLabel.setText("Файл не выбран");
            }
        });
    }

    /**
     * Обработчик нажатия кнопки выбора файла через {@link FileChooser}.
     */
    @FXML
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите JavaScript-файл для анализа");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JavaScript файлы (*.js)", "*.js"),
                new FileChooser.ExtensionFilter("Все файлы (*.*)", "*.*")
        );

        Window window = codeArea.getScene() != null ? codeArea.getScene().getWindow() : null;
        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            try {
                String content = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);
                isProgrammaticChange = true;
                codeArea.setText(content);
                filePathLabel.setText(selectedFile.getName());
            } catch (IOException e) {
                showErrorAlert("Ошибка чтения файла", "Не удалось прочитать выбранный файл:", e.getMessage());
            } finally {
                isProgrammaticChange = false;
            }
        }
    }

    /**
     * Обработчик нажатия кнопки запуска расчета метрик сложности.
     */
    @FXML
    private void handleCalculate() {
        String sourceCode = codeArea.getText();
        if (sourceCode == null || sourceCode.isBlank()) {
            showErrorAlert(
                    "Ошибка валидации",
                    "Исходный код пуст",
                    "Пожалуйста, введите или загрузите JavaScript-код для анализа."
            );
            return;
        }

        try {
            GilbMetricResult result = analyzer.analyze(sourceCode);
            openResultWindow(result);
        } catch (IllegalArgumentException | NullPointerException e) {
            showErrorAlert("Синтаксическая ошибка", "Ошибка при синтаксическом разборе кода:", e.getMessage());
        } catch (Exception e) {
            showErrorAlert("Непредвиденная ошибка", "Во время анализа произошла ошибка:", e.getMessage());
        }
    }

    private void openResultWindow(GilbMetricResult result) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            ResultController controller = loader.getController();
            controller.initData(result);

            Stage stage = new Stage();
            stage.setTitle("Результаты анализа метрики Джилба");
            stage.initModality(Modality.WINDOW_MODAL);

            Window owner = codeArea.getScene() != null ? codeArea.getScene().getWindow() : null;
            if (owner != null) {
                stage.initOwner(owner);
            }

            stage.setScene(new Scene(root, 750, 650));
            stage.setResizable(true);
            stage.show();
        } catch (IOException e) {
            showErrorAlert("Ошибка интерфейса", "Не удалось загрузить окно результатов:", e.getMessage());
        }
    }

    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        Window owner = codeArea.getScene() != null ? codeArea.getScene().getWindow() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }
}
