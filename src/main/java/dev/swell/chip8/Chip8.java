package dev.swell.chip8;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

public class Chip8 {

    private static final int MEMORY_SIZE = 4096;
    private static final int FONTSET_START = 0x50;
    private static final int PROGRAM_START = 0x200;
    private static final int DISPLAY_WIDTH = 64;
    private static final int DISPLAY_HEIGHT = 32;


    private GraphicsContext gc;
    private Canvas displayCanvas;
    private boolean started = false;


    private AnimationTimer animTimer;

    private byte[] memory = new byte[MEMORY_SIZE]; //memória do chip, geralmente 4kb
    private byte[] V = new byte[16]; //registradores
    private int I = 0; //endereço de índice
    private int pc = PROGRAM_START; //contador do programa
    private int sp = 0;//ponteiro da pilha
    private int delayTimer = 0;
    private int soundTimer = 0;
    private int[] stack = new int[16];//pilha
    private boolean[][] gfx = new boolean[DISPLAY_WIDTH][DISPLAY_HEIGHT];
    private boolean[] keys = new boolean[16];
    private boolean drawFlag = false;

    private final Random random = new Random();

    private static final byte[] CHIP8_FONTSET = new byte[]{
            (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
            (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
            (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
            (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
            (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
            (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
            (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
            (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
            (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  // F
    };
    private static final int FPS = 10;
    private double scaleX = 8.0;
    private double scaleY = 8.0;
    private Clip clip;


    public Chip8()  {
        reset();

    }

    public void loadRom(String filename) throws IOException {
        shutdown();
        reset();
        var path = Paths.get(filename);
        byte[] rom = Files.readAllBytes(path);
        System.arraycopy(rom, 0, memory, PROGRAM_START, rom.length);
        start();
    }

    private void start() {
        started = true;
        animTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (int i = 0; i < FPS; i++) {
                    cycle();
                    if (drawFlag) render();
                    if (soundTimer > 0) beep();
                }
                updateTimers();
            }
        };
        animTimer.start();
    }

    private void beep() {
        if (!started || (clip != null && clip.isRunning())) return;
        try {
            float sampleRate = 44100;
            int ms = 70;
            int len = (int) (sampleRate * ms / 1000);
            byte[] data = new byte[len];
            double freq = 1000;
            for (int i = 0; i < len; i++) {
                double v = Math.sin(2 * Math.PI * freq * i / sampleRate);
                data[i] = (byte) (v * 127);
            }
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
            clip = AudioSystem.getClip();
            clip.open(af, data, 0, len);
            clip.start();
        } catch (Exception e) {
        }
    }

    private void render() {
        if (displayCanvas == null) return;

        Platform.runLater(()->{
            gc.clearRect(0, 0, displayCanvas.getWidth(), displayCanvas.getHeight());

            for (int i = 0; i < DISPLAY_WIDTH; i++) {
                for (int j = 0; j < DISPLAY_HEIGHT; j++) {
                    if (gfx[i][j]) {
                        gc.setFill(Color.RED);
                    } else {
                        gc.setFill(Color.BLACK);
                    }
                    gc.fillRect(i * scaleX, j * scaleY, scaleX, scaleY);
                }
            }
            drawFlag = false;
        });

    }

    private void reset() {
        started = false;
        memory = new byte[MEMORY_SIZE];
        V = new byte[16];
        I = 0;
        pc = PROGRAM_START;
        stack = new int[16];
        sp = 0;
        delayTimer = 0;
        soundTimer = 0;
        gfx = new boolean[DISPLAY_WIDTH][DISPLAY_HEIGHT];
        drawFlag = true;
        keys = new boolean[16];
        System.arraycopy(CHIP8_FONTSET, 0, memory, FONTSET_START, CHIP8_FONTSET.length);
    }

    private void shutdown() {
        if (started) {
            if (animTimer != null) {
                animTimer.stop();
                animTimer = null;
            }
        }
    }

    public void setDisplay(Canvas canvasDisplay) {
        this.gc = canvasDisplay.getGraphicsContext2D();
        this.displayCanvas = canvasDisplay;
        scaleX = canvasDisplay.getWidth() / DISPLAY_WIDTH;
        scaleY = canvasDisplay.getHeight() / DISPLAY_HEIGHT;
    }

    public boolean IsStarted() {
        return started;
    }

    public void sendKeyPress(int key) {
        if (!started || key < 0) return;
        keys[key] = true;
    }

    public void sendKeyRelease(int key) {
        if (!started || key < 0) return;
        keys[key] = false;
    }

    private void clearDisplay() {
        for (int i = 0; i < DISPLAY_WIDTH; i++) {
            for (int j = 0; j < DISPLAY_HEIGHT; j++) {
                gfx[i][j] = false;
            }
        }
        drawFlag = true;
    }

    private void updateTimers() {
        if (delayTimer > 0) delayTimer--;
        if (soundTimer > 0) soundTimer--;
    }

    Opcode fetch(int instruction) {
        Opcode n = new Opcode(instruction);
        pc += 2;
        return n;
    }

    private void cycle() {
        Opcode opcode = fetch((memory[pc] & 0xFF) << 8 | (memory[pc + 1] & 0xFF));
        try {
            execute(opcode);
        } catch (Exception e) {
            IO.println(e);
            shutdown();
        }
    }

    private void execute(Opcode opcode) {
        switch (opcode.getInstruction()) {
            case 0:
                if (opcode.getOpcode() == 0x00e0) {
                    clearDisplay();
                } else if (opcode.getOpcode() == 0x00ee) {
                    sp--;
                    pc = stack[sp];
                }
                opcode.debug();
                break;
            case 0x1000:
                pc = opcode.getNnn();
                opcode.debug();
                break;
            case 0x2000:
                stack[sp] = pc;
                sp++;
                pc = opcode.getNnn();
                opcode.debug();
                break;
            case 0x3000:
                if ((V[opcode.getX()] & 0xff) == opcode.getKk()) pc += 2;
                opcode.debug();
                break;
            case 0x4000:
                if ((V[opcode.getX()] & 0xff) != opcode.getKk()) pc += 2;
                opcode.debug();
                break;
            case 0x5000:
                if (opcode.getN() == 0 && V[opcode.getX()] == V[opcode.getY()]) pc += 2;
                opcode.debug();
                break;
            case 0x6000:
                V[opcode.getX()] = (byte) opcode.getKk();
                opcode.debug();
                break;
            case 0x7000:
                V[opcode.getX()] = (byte) ((V[opcode.getX()] & 0xff) + opcode.getKk());
                opcode.debug();
                break;
            case 0x8000:
                switch (opcode.getN()) {
                    case 0:
                        V[opcode.getX()] = V[opcode.getY()];
                        break;
                    case 1:
                        V[opcode.getX()] = (byte) ((V[opcode.getX()] | V[opcode.getY()]) & 0xff);

                        break;
                    case 2:
                        V[opcode.getX()] = (byte) ((V[opcode.getX()] & V[opcode.getY()]) & 0xFF);

                        break;
                    case 3:
                        V[opcode.getX()] = (byte) ((V[opcode.getX()] ^ V[opcode.getY()]) & 0xFF);

                        break;
                    case 4:
                        int sum = (V[opcode.getX()] & 0xFF) + (V[opcode.getY()] & 0xFF);
                        V[0xF] = (byte) ((sum > 0xFF) ? 1 : 0);
                        V[opcode.getX()] = (byte) (sum & 0xFF);

                        break;
                    case 5:
                        V[0xF] = (byte) ((V[opcode.getX()] & 0xFF) > (V[opcode.getY()] & 0xFF) ? 1 : 0);
                        V[opcode.getX()] = (byte) ((V[opcode.getX()] & 0xFF) - (V[opcode.getY()] & 0xFF));

                        break;
                    case 6:
                        V[0xF] = (byte) ((V[opcode.getX()] & 0x01));
                        V[opcode.getX()] = (byte) ((V[opcode.getX()] & 0xFF) >> 1);

                        break;
                    case 7:
                        V[0xF] = (byte) ((V[opcode.getY()] & 0xFF) > (V[opcode.getX()] & 0xFF) ? 1 : 0);
                        V[opcode.getX()] = (byte) ((V[opcode.getY()] & 0xFF) - (V[opcode.getX()] & 0xFF));
                        opcode.debug();
                        break;
                    case 0xe:
                        V[0xF] = (byte) ((V[opcode.getX()] & 0x80) >> 7);
                        V[opcode.getX()] = (byte) ((V[opcode.getX()] & 0xFF) << 1);

                        break;
                }
                opcode.debug();
                break;
            case 0x9000:
                if (opcode.getN() == 0 && V[opcode.getX()] != V[opcode.getY()]) pc += 2;
                opcode.debug();
                break;
            case 0xA000:
                I = opcode.getNnn();
                opcode.debug();
                break;
            case 0xB000:
                pc = opcode.getNnn() + (V[0] & 0xff);
                opcode.debug();
                break;
            case 0xc000:
                V[opcode.getX()] = (byte) (random.nextInt(256) & opcode.getKk());
                opcode.debug();
                break;
            case 0xD000:
                int vx = V[opcode.getX()] & 0xFF;
                int vy = V[opcode.getY()] & 0xFF;
                int height = opcode.getN();
                V[0xF] = 0;
                for (int row = 0; row < height; row++) {
                    int spriteByte = memory[I + row] & 0xFF;
                    for (int col = 0; col < 8; col++) {
                        int pixel = (spriteByte >> (7 - col)) & 1;
                        int xpos = (vx + col) % DISPLAY_WIDTH;
                        int ypos = (vy + row) % DISPLAY_HEIGHT;
                        boolean prev = gfx[xpos][ypos];
                        boolean newPixel = (pixel == 1);
                        gfx[xpos][ypos] = prev ^ newPixel;
                        if (prev && !gfx[xpos][ypos]) V[0xF] = 1;
                    }
                }
                drawFlag = true;
                opcode.debug();
                break;
            case 0xe000:
                if (opcode.getKk() == 0x9e) {
                    if (keys[V[opcode.getX()] & 0xff]) pc += 2;
                } else if (opcode.getKk() == 0xa1) {
                    if (!keys[V[opcode.getX()] & 0xff]) pc += 2;
                }
                opcode.debug();
                break;
            case 0xf000:
                switch (opcode.getKk()) {
                    case 0x07:
                        V[opcode.getX()] = (byte) delayTimer;
                        break;
                    case 0x0a: {
                        boolean pressed = false;
                        for (int i = 0; i < 16; i++) {
                            if (keys[i]) {
                                V[opcode.getX()] = (byte) i;
                                pressed = true;
                                break;
                            }
                        }
                        if (!pressed) pc -= 2;
                        break;
                    }
                    case 0x15:
                        delayTimer = V[opcode.getX()] & 0xff;
                        break;
                    case 0x18:
                        soundTimer = V[opcode.getX()] & 0xff;
                        break;
                    case 0x1e:
                        I = (I + (V[opcode.getX()] & 0xFF)) & 0xffff;
                        break;
                    case 0x29:
                        I = FONTSET_START + ((V[opcode.getX()] & 0xff) * 5);
                        break;
                    case 0x33: {
                        int val = V[opcode.getX()] & 0xff;
                        memory[I] = (byte) (val / 100);
                        memory[I + 1] = (byte) ((val / 10) % 10);
                        memory[I + 2] = (byte) (val % 10);
                        break;
                    }
                    case 0x55:
                        if (opcode.getX() + 1 >= 0) System.arraycopy(V, 0, memory, I, opcode.getX() + 1);
                        break;
                    case 0x65:
                        if (opcode.getX() + 1 >= 0) System.arraycopy(memory, I, V, 0, opcode.getX() + 1);
                        break;
                    default:
                        break;
                }
                opcode.debug();
        }
    }
}
