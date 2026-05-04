package com.raycasting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class GameWindow extends JPanel implements Runnable, KeyListener, MouseMotionListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Thread gameThread;
    private boolean running = false;

    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    private Map map;
    private Player player;
    private Raycaster raycaster;

    private boolean[] keys = new boolean[256];
    private int mouseX;
    private int mouseY;
    private int lastMouseX = -1;

    public GameWindow() {
        map = new Map();
        player = new Player(2.5, 2.5);

        TextureManager textureManager = new TextureManager("/com/raycasting/textures/wolfwall1.png");

        raycaster = new Raycaster(map, player, textureManager);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(this);
        addMouseMotionListener(this);

        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();

        System.out.println("=== Java Raycasting Project ===");
        System.out.println("SPACJA - przełącz tryb");
        System.out.println("1 - TOP_DOWN_VIEW");
        System.out.println("2 - TEXTURED_3D");
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void stop() {
        running = false;

        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        final long TARGET_TIME = 1000 / 60;

        while (running) {
            long startTime = System.currentTimeMillis();

            update();
            render();
            repaint();

            long timeTaken = System.currentTimeMillis() - startTime;
            long sleepTime = TARGET_TIME - timeTaken;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void update() {
        handleKeyboardInput();
        handleMouseInput();
    }

    private void handleKeyboardInput() {
        double moveX = 0;
        double moveY = 0;

        double angle = player.getAngle();
        double speed = player.getMoveSpeed();

        if (keys[KeyEvent.VK_W]) {
            moveX += Math.cos(angle) * speed;
            moveY += Math.sin(angle) * speed;
        }

        if (keys[KeyEvent.VK_S]) {
            moveX -= Math.cos(angle) * speed;
            moveY -= Math.sin(angle) * speed;
        }

        if (keys[KeyEvent.VK_A]) {
            moveX += Math.cos(angle - Math.PI / 2) * speed;
            moveY += Math.sin(angle - Math.PI / 2) * speed;
        }

        if (keys[KeyEvent.VK_D]) {
            moveX += Math.cos(angle + Math.PI / 2) * speed;
            moveY += Math.sin(angle + Math.PI / 2) * speed;
        }

        moveWithWallSliding(moveX, moveY);

        if (keys[KeyEvent.VK_LEFT]) {
            player.rotateLeft();
        }

        if (keys[KeyEvent.VK_RIGHT]) {
            player.rotateRight();
        }
    }

    private void moveWithWallSliding(double moveX, double moveY) {
        double oldX = player.getX();
        double oldY = player.getY();

        double newX = oldX + moveX;
        double newY = oldY + moveY;

        if (canStandAt(newX, oldY)) {
            player.setPosition(newX, oldY);
        }

        if (canStandAt(player.getX(), newY)) {
            player.setPosition(player.getX(), newY);
        }
    }

    private boolean canStandAt(double x, double y) {
        double radius = 0.22;

        return !map.isWall((int) (x - radius), (int) (y - radius))
                && !map.isWall((int) (x + radius), (int) (y - radius))
                && !map.isWall((int) (x - radius), (int) (y + radius))
                && !map.isWall((int) (x + radius), (int) (y + radius));
    }

    private void handleMouseInput() {
        if (lastMouseX != -1) {
            int deltaX = mouseX - lastMouseX;

            if (deltaX != 0) {
                double rotation = deltaX * 0.005;
                player.rotateBy(rotation);
            }
        }

        lastMouseX = mouseX;
    }

    private void render() {
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(0, 0, getWidth(), getHeight());

        raycaster.draw(buffer);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (buffer != null) {
            g.drawImage(buffer, 0, 0, null);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode >= 0 && keyCode < keys.length) {
            keys[keyCode] = true;
        }

        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                cycleGameState();
                break;

            case KeyEvent.VK_ESCAPE:
                running = false;
                System.exit(0);
                break;

            case KeyEvent.VK_1:
                raycaster.setGameState(GameState.TOP_DOWN_VIEW);
                System.out.println("Tryb: TOP_DOWN_VIEW");
                break;

            case KeyEvent.VK_2:
                raycaster.setGameState(GameState.TEXTURED_3D);
                System.out.println("Tryb: TEXTURED_3D");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode >= 0 && keyCode < keys.length) {
            keys[keyCode] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    private void cycleGameState() {
        if (raycaster.getGameState() == GameState.TOP_DOWN_VIEW) {
            raycaster.setGameState(GameState.TEXTURED_3D);
            System.out.println("Tryb: TEXTURED_3D");
        } else {
            raycaster.setGameState(GameState.TOP_DOWN_VIEW);
            System.out.println("Tryb: TOP_DOWN_VIEW");
        }
    }
}