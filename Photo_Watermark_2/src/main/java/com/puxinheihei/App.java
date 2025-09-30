package com.puxinheihei;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            URL fxmlUrl = getClass().getResource("/view/main.fxml");
            if (fxmlUrl == null) {
                throw new RuntimeException("无法找到FXML文件：/view/main.fxml");
            }
            Parent root = FXMLLoader.load(fxmlUrl);

            Scene scene = new Scene(root, 1200, 800);

            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            primaryStage.setTitle("Photo Watermark 2");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("启动失败: " + e.getMessage());
        }
    }

    private void showErrorDialog(String message) {
        System.err.println("错误: " + message);
    }

    public static void main(String[] args) {
        // 确保JavaFX运行时正确初始化
        try {
            System.out.println("启动 Photo Watermark 2 应用...");
            launch(args);
        } catch (Exception e) {
            System.err.println("应用启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}