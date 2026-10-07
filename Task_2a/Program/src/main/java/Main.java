import javafx.application.Application;
import jilba.ui.GilbFxApplication;

/**
 * Точка входа приложения анализа метрик Джилба.
 * Делегирует запуск JavaFX-приложению {@link GilbFxApplication}.
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(GilbFxApplication.class, args);
    }
}