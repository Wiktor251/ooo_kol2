package org.example.powtkol2;

import javafx.scene.paint.Color;

public record Dot(double x, double y, Color color, double r) {
    public static String toMessage(double x, double y, Color color, double r){
        return x + " " + y + " " + r + " " + color;
    }
    public static Dot fromMessage(String message){
        String[] arr = message.split(" ");
        double x = Double.parseDouble(arr[0]);
        double y = Double.parseDouble(arr[1]);
        double r = Double.parseDouble(arr[2]);
        Color color = Color.web(arr[3]);

        return new Dot(x,y,color,r);
    }
    public String toMessage() {
        return x + " " + y + " " + r + " " + color.toString();
    }
}
