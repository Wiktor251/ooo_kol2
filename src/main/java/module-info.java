module org.example.powtkol2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens org.example.powtkol2 to javafx.fxml;
    exports org.example.powtkol2;
}