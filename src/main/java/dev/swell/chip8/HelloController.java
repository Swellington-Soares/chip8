package dev.swell.chip8;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.util.Pair;

import javax.sound.sampled.LineUnavailableException;

public class HelloController {

    private Scene scene;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Canvas canvasDisplay;

    @FXML
    private GridPane keyboard;

    @FXML
    private TextField textFiledSelectedRom;


    private String lastFolder;

    private GraphicsContext gc;

    private final Map<Integer, Button> keyboardButton = new HashMap<>();


    private final Chip8 chip8 = new Chip8();

    public HelloController() throws LineUnavailableException {
    }

    @FXML
    void onButtonSelectRomAction(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open ROM");
        if (lastFolder != null) {
            fileChooser.setInitialDirectory(Paths.get(lastFolder).toFile());
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Chip 8 ROM File", "*.ch8"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File selectedFile = fileChooser.showOpenDialog(scene.getWindow());
        if (selectedFile != null) {
            textFiledSelectedRom.setText(selectedFile.getAbsolutePath());
            lastFolder = selectedFile.getParent();
            chip8.loadRom(selectedFile.getAbsolutePath());
        }

    }

    @FXML
    void initialize() throws IOException {
        Platform.runLater(() -> {
            scene = canvasDisplay.getScene();
            chip8.setDisplay(this.canvasDisplay);
            setupKeyboardUI();
            setKeyHandlers();
        });
    }

    private void setupKeyboardUI() {
        var buttons = keyboard.getChildren();
        buttons.forEach(child -> {

            if (child instanceof Button kbButton) {
                var key = Character.digit(kbButton.getText().getBytes()[0], 16);
                keyboardButton.put(key, kbButton);
            }
        });
    }

    private void setKeyHandlers() {
        scene.setOnKeyPressed(this::keyPressed);
        scene.setOnKeyReleased(this::keyReleased);
    }


    private int mapKey(javafx.scene.input.KeyCode k) {
        return switch (k) {
            case DIGIT1 -> 0x1;
            case DIGIT2 -> 0x2;
            case DIGIT3 -> 0x3;
            case DIGIT4 -> 0xC;
            case Q -> 0x4;
            case W -> 0x5;
            case E -> 0x6;
            case R -> 0xD;
            case A -> 0x7;
            case S -> 0x8;
            case D -> 0x9;
            case F -> 0xE;
            case Z -> 0xA;
            case X -> 0x0;
            case C -> 0xB;
            case V -> 0xF;
            default -> -1;
        };
    }

    private void sendKeyAction(int key, ActionKeyState action) {
        if (!chip8.IsStarted() || key < 0) return;
      //  IO.println(String.format("[%s] %s", key, action));
        if (action == ActionKeyState.PRESS) {
            setButtonAsPressed(key);
            chip8.sendKeyPress(key);
        } else if (action == ActionKeyState.RELEASE) {
            chip8.sendKeyRelease(key);
            setButtonAsReleased(key);
        }
    }

    private void setButtonAsReleased(int key) {
        keyboardButton.get(key).getStyleClass().remove("pressed");
    }

    private void setButtonAsPressed(int key) {
        if (!keyboardButton.get(key).getStyleClass().contains("pressed")) {
            keyboardButton.get(key).getStyleClass().add("pressed");
        }
    }

    private Pair<KeyCode, ActionKeyState> lastAction;

    private void keyPressed(KeyEvent event) {
        if (lastAction != null && lastAction.getValue() == ActionKeyState.PRESS) {
            return;
        }

        sendKeyAction(mapKey(event.getCode()), ActionKeyState.PRESS);
        lastAction = new Pair<>(event.getCode(), ActionKeyState.PRESS);
        IO.println("PRESS: " + lastAction);
    }

    private void keyReleased(KeyEvent event) {
        if (lastAction == null || (lastAction.getKey() != event.getCode())) {
            return;
        }

        sendKeyAction(mapKey(event.getCode()), ActionKeyState.RELEASE);
        lastAction = new Pair<>(event.getCode(), ActionKeyState.RELEASE);
        IO.println("RELEASE: " + lastAction);
    }
}
