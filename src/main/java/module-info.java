module test {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens test to javafx.fxml;
    exports test;
}
