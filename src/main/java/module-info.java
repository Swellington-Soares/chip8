module dev.swell.chip {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;


    opens dev.swell.chip8 to javafx.fxml;
    exports dev.swell.chip8;
}