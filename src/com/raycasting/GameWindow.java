package com.raycasting;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JPanel;

public class GameWindow extends JPanel implements Runnable, KeyListener, MouseMotionListener, MouseListener, FocusListener {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Thread gameThread;
    private boolean running = false;

    private BufferedImage buffer;
    private Graphics2D bufferGraphics;

    private Map map;
    private Player player;
    private Raycaster raycaster;
    private SpriteManager spriteManager;
    private SpriteManager weaponManager;
    private MovingSprite guard;

    private boolean[] keys = new boolean[256];
    private int mouseX;
    private int lastMouseX = -1;

    public GameWindow() {
        map = new Map();
        player = new Player(2.5, 2.5);

        // POPRAWNIE ŚCIEŻKI
        TextureManager textureManager = new TextureManager("com/raycasting/textures/wolfwall1.png");
        
        // ========== ZAŁADUJ SPRITE'Y NPC ==========
        // spriteManager = new SpriteManager("/com/raycasting/sprites/[NAZWA].PNG");
        // Zmień [NAZWA] na plik sprite'u strażnika
        // Dostępne NPC: Guard.png, Guardian.png [dodaj własne]
        spriteManager = new SpriteManager("/com/raycasting/sprites/Guard.png");
        
        // ========== ZAŁADUJ SPRITE'Y BRONI ==========
        // weaponManager = new SpriteManager("/com/raycasting/sprites/[NAZWA].PNG");
        // Zmień [NAZWA] na plik sprite'u broni
        // Dostępne broń: pistol.PNG, knife.png [dodaj własne]
        weaponManager = new SpriteManager("/com/raycasting/sprites/pistol.PNG");

        raycaster = new Raycaster(map, player, textureManager, spriteManager);

        SpriteConfigurator config = new SpriteConfigurator(spriteManager);
        SpriteEntity guardEntity = config.createGuard();

        // ========== DODAJ STRAŻNIKÓW ==========
        // Punkt startowy strażnika: (x, y)
        guard = new MovingSprite(5.5, 5.5, guardEntity);
        
        // TRASA PATROLU - wyznaczają ścieżkę strażnika
        // Każdy punkt {x, y} to pozycja, którą odwiedzi strażnik
        // DODAJ WIĘCEJ STRAŻNIKÓW: duplikuj poniższe linie z innymi (x,y) i trasami
        double[][] patrolRoute = {
            {5.5, 5.5},
            {8.5, 5.5},
            {8.5, 8.5},
            {5.5, 8.5},
            {5.5, 5.5}
        };
        guard.setPatrolRoute(patrolRoute);
        guard.speed = 0.02;

        raycaster.addMovingSprite(guard);

        // ========== KONFIGURACJA BRONI ==========
        // Załaduj broń gracza (pistolet)
        // Aby zmienić na inną broń: zmień pistol.PNG na knife.png, shotgun.png itd
        // 
        // Dostępne broń:
        //   - pistol.PNG: 326x62 (5 klatek), 25 dmg, cooldown 300ms
        //   - knife.png: [dodaj swoją broń]
        //   - shotgun.png: [dodaj swoją broń]
        //
        // Aby przełączać się między bronią w grze:
        //   1. Załaduj dodatkowe sprite'y tutaj
        //   2. Zmień w SpriteConfigurator: createWeaponKnife() zamiast createWeaponPistol()
        //   3. Nastaw przyciski: np KEY_1 = pistolet, KEY_2 = nóż, KEY_3 = shotgun
        //
        SpriteEntity pistolEntity = config.createWeaponPistol();
        raycaster.setWeaponSprite(pistolEntity, weaponManager);

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addFocusListener(this);

        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();

        System.out.println("=== Java Raycasting Project ===");
        System.out.println("WASD - przemieszanie sie");
        System.out.println("Strzalki / Mysz - rotacja widoku");
        System.out.println("SPACE - strzal z pistoletu");
        System.out.println("LPM - alternatywnie strzal");
        System.out.println("TAB - przełącz tryb widoku");
        System.out.println("1 - TOP_DOWN_VIEW");
        System.out.println("2 - TEXTURED_3D");
        System.out.println("ESC - wyjscie");
        System.out.println("Strażnicy poruszają się wobec gracza i strzelają!");
        System.out.println("Zabit wszystkich strażników aby wygrać!");
    }

    public void start() {
        if (running) {
            return;
        }

        requestFocusInWindow();

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
        double radius = 0.30;

        return !map.isWall((int) (x - radius), (int) (y - radius))
                && !map.isWall((int) (x + radius), (int) (y - radius))
                && !map.isWall((int) (x - radius), (int) (y + radius))
                && !map.isWall((int) (x + radius), (int) (y + radius));
    }

    private void handleMouseInput() {
        if (!hasFocus()) {
            lastMouseX = -1;
            return;
        }

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
                raycaster.shoot();
                break;

            case KeyEvent.VK_TAB:
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
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            raycaster.shoot();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void focusGained(FocusEvent e) {
        lastMouseX = -1;
        Arrays.fill(keys, false);
    }

    @Override
    public void focusLost(FocusEvent e) {
        lastMouseX = -1;
        Arrays.fill(keys, false);
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