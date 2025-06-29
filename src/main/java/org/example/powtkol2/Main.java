package org.example.powtkol2;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Application;
import org.example.powtkol2.client.ServerThread;
import org.example.powtkol2.server.Server;

import static javafx.application.Application.launch;

public class Main extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("plik.fxml"));
        Server server = new Server();
        server.connect("dots.db");
        new Thread(() -> server.start()).start();
        ServerThread serverThread = new ServerThread();
        Controller controller = new Controller(server, serverThread);
        loader.setController(controller);
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Circle Drawing App");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
