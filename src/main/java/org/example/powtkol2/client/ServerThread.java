package org.example.powtkol2.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.IOException;
import java.util.function.Consumer;

import javafx.scene.paint.Color;
import org.example.powtkol2.Dot;

public class ServerThread implements Runnable{
    private static  final String server_address = "localhost";
    private static final  int PORT = 5001;

    private Socket socket;
    private PrintWriter writer;
    private Consumer<Dot> dotConsumer;

    public void setDotConsumer(Consumer<Dot> consumer) {
        this.dotConsumer = consumer;
    }

    public ServerThread() {
        try {
            socket = new Socket(server_address, PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected");
            new Thread(this).start();
        } catch (IOException e) {
            System.err.println("Unable to connect to server: " + e.getMessage());
        }
    }
    public void send(double x, double y, double r, Color color) {
        if (writer != null) {
            String message = Dot.toMessage(x, y, color, r);
            writer.println(message);
        } else {
            System.err.println("Output stream not initialized.");
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                Dot dot = Dot.fromMessage(line);
                if (dotConsumer != null) {
                    dotConsumer.accept(dot);
                }
            }
        } catch (IOException e) {
            System.err.println("Error receiving message: " + e.getMessage());
        }
    }
}

