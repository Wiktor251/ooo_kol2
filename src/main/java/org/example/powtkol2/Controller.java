package org.example.powtkol2;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.example.powtkol2.client.ServerThread;
import org.example.powtkol2.server.Server;

public class Controller {

    private final Server server;
    private final ServerThread serverThread;

    public Controller(Server server, ServerThread serverThread){
        this.server = server;
        this.serverThread = serverThread;
        this.serverThread.setDotConsumer(dot -> {
            Platform.runLater(() -> drawDot(dot));
        });
    }

    @FXML
    private Slider radiusSlider;
    @FXML
    private Canvas canvas;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private void initialize() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    @FXML
    private void onMouseClicked(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        double r = radiusSlider.getValue();
        Color color = colorPicker.getValue();

        /*GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(color);
        gc.fillOval(x-r/2,y-r/2,r,r);*/
        serverThread.send(x, y, r, color);
    }

    @FXML
    private void onStartServerClicked() {
        System.out.println("Start Server clicked");
    }

    @FXML
    private void onConnectClicked() {
        System.out.println("Connect clicked");
    }

    private void drawDot(Dot dot) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        System.out.println("Drawing dot: " + dot);
        gc.setFill(dot.color());
        gc.fillOval(dot.x() - dot.r() / 2, dot.y() - dot.r() / 2, dot.r(), dot.r());
    }
}
