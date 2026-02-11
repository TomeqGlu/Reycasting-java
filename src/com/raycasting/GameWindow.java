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
    private static final String TITLE = "Java Raycasting";

    private Thread gameThread;
    private boolean running = false;

    // Bufor
    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    // Komponenty
    private Map map;
    private Player player;
    private Raycaster raycaster;

    // Input
    private boolean[] keys = new boolean[256];
    private int mouseX, mouseY;
    private int lastMouseX = -1;

    public GameWindow() {
        map = new Map();
        player = new Player(2.5, 2.5); // Start, TODO: lepsze miejsce startowe
        raycaster = new Raycaster(map, player);

        // Konfiguracja panelu
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        addKeyListener(this);
        addMouseMotionListener(this);

        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();

        System.out.println("=== Java Raycasting Project ===");
        System.out.println("Mapa: " + map.getWidth() + "x" + map.getHeight());
        System.out.println("Gracz start: " + player);
        System.out.println("Tryb: " + raycaster.getGameState());
        System.out.println("Sterowanie:");
        System.out.println("  WASD - ruch");
        System.out.println("  Mysz - obrót kamery");
        System.out.println("  SPACJA - zmiana trybu gry");
        System.out.println("  ESC - wyjście");
    }

    public void start() {
        if (running)
            return;
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
        // (60 FPS)
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

        raycaster.castAllRays(getWidth());
    }

    // Input z klawiatury, niech to w końcu złapieee
    private void handleKeyboardInput() {
        // Ruch
        if (keys[KeyEvent.VK_W])
            player.moveForward();
        if (keys[KeyEvent.VK_S])
            player.moveBackward();
        if (keys[KeyEvent.VK_A])
            player.strafeLeft();
        if (keys[KeyEvent.VK_D])
            player.strafeRight();

        // Obroty klawiszami (chyba że mysz w końcu złapie)
        if (keys[KeyEvent.VK_LEFT])
            player.rotateLeft();
        if (keys[KeyEvent.VK_RIGHT])
            player.rotateRight();

        handleCollisions();
    }

    // Kolizje
    private void handleCollisions() {
        // Antycliping
        // TODO: W przyszłosci może byc problem z bronią przed graczem
        int playerMapX = (int) player.getX();
        int playerMapY = (int) player.getY();

        if (map.isWall(playerMapX, playerMapY)) {
            // Cofnij playera do poprzedniej pozycji
            // TODO : Dodac sledzenie poprzednije pozycji
            player.setPosition(Math.max(1.5, Math.min(map.getWidth() - 1.5, player.getX())),
                    Math.max(1.5, Math.min(map.getHeight() - 1.5, player.getY())));
        }
    }

    // Input z myszy, obrót kamery
    private void handleMouseInput() {
        if (lastMouseX != -1) {
            int deltaX = mouseX - lastMouseX;
            if (deltaX != 0) {
                // Obróć gracza proporcjonalnie do ruchu myszy
                double rotation = deltaX * 0.005;
                player.rotateBy(rotation);
            }
        }

        lastMouseX = mouseX;

        if (mouseX < 50) {
            try {
                int xPosition = getLocationOnScreen().x + getWidth() - 100;
                int yPosition = getLocationOnScreen().y + mouseY;

                new java.awt.Robot().mouseMove(xPosition, yPosition);
                lastMouseX = getWidth() - 100;
            } catch (java.awt.AWTException ex) {
                ex.printStackTrace();
            }
        } else if (mouseX > getWidth() - 50) {
            try {
                int xPosition = getLocationOnScreen().x + 100;
                int yPosition = getLocationOnScreen().y + mouseY;

                new java.awt.Robot().mouseMove(xPosition, yPosition);
                lastMouseX = 100;
            } catch (java.awt.AWTException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Renderowanie sceny do bufora
    private void render() {
        // Wyczyść bufor
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(0, 0, getWidth(), getHeight());

        // Renderuj raycasting
        raycaster.draw(bufferGraphics, getWidth(), getHeight());
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

        // Obsługa specjalnych klawiszy
        // TODO: Dodac zmiane trybu gry, wyjście itp.
        switch (keyCode) {
            case KeyEvent.VK_SPACE:
                // Zmiana trybu gry
                cycleGameState();
                break;

            case KeyEvent.VK_ESCAPE:
                running = false;
                System.exit(0);
                break;

            case KeyEvent.VK_1:
                raycaster.setGameState(GameState.TOP_DOWN_VIEW);
                System.out.println("Tryb: WIDOK Z GÓRY");
                break;

            case KeyEvent.VK_2:
                raycaster.setGameState(GameState.RAYCAST_2D);
                System.out.println("Tryb: RAYCAST 2D DEBUG");
                break;

            case KeyEvent.VK_3:
                raycaster.setGameState(GameState.WOLFENSTEIN_3D);
                System.out.println("Tryb: WOLFENSTEIN 3D");
                break;

            case KeyEvent.VK_4:
                raycaster.setGameState(GameState.TEXTURED_3D);
                System.out.println("Tryb: TEXTURED 3D");
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
        // Na razie nie używamy
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

    // Przełacznie między trybami
    private void cycleGameState() {
        GameState current = raycaster.getGameState();
        GameState next = null;

        switch (current) {
            case TOP_DOWN_VIEW:
                next = GameState.RAYCAST_2D;
                break;
            case RAYCAST_2D:
                next = GameState.WOLFENSTEIN_3D;
                break;
            case WOLFENSTEIN_3D:
                next = GameState.TEXTURED_3D;
                break;
            case TEXTURED_3D:
                next = GameState.TOP_DOWN_VIEW;
                break;
        }

        raycaster.setGameState(next);
        System.out.println("Przełączono na tryb: " + next);
    }
}