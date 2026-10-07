package jilba.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jilba.GilbConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * Главный класс JavaFX-приложения, интегрированный с контекстом Spring Framework.
 */
public class GilbFxApplication extends Application {

    private AnnotationConfigApplicationContext springContext;

    @Override
    public void init() {
        springContext = new AnnotationConfigApplicationContext(GilbConfig.class);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
        loader.setControllerFactory(springContext::getBean);
        Parent root = loader.load();

        Scene scene = new Scene(root, 850, 650);
        primaryStage.setTitle("Анализатор сложности потока управления (Метрика Джилба)");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(650);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }
}
