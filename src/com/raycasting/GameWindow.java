package com.raycasting;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.Toolkit;
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
    private SpriteManager dogSpriteManager;
    private SpriteManager weaponManager;
    private TextureManager textureManager;
    private SoundManager soundManager;

    private boolean[] keys = new boolean[256];

    private Robot mouseRobot;
    private boolean mouseCaptured = false;
    private boolean recenteringMouse = false;

    // Czułość myszy. Zwiększ, jeśli obrót jest za wolny; zmniejsz, jeśli za szybki.
    private static final double MOUSE_SENSITIVITY = 0.0045;

    public GameWindow() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);

        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addFocusListener(this);

        buffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = buffer.createGraphics();

        initMouseCapture();

        loadManagers();
        resetGame();

        System.out.println("=== Java Raycasting Project ===");
        System.out.println("WASD - przemieszczanie się");
        System.out.println("Strzałki / Mysz - rotacja widoku");
        System.out.println("SPACE - strzał z pistoletu");
        System.out.println("LPM - alternatywnie strzał");
        System.out.println("PPM przytrzymany - widok mapy z góry");
        System.out.println("R - restart po wygranej/przegranej");
        System.out.println("ESC - wyjście");
    }

    private void initMouseCapture() {
        try {
            mouseRobot = new Robot();
        } catch (AWTException e) {
            mouseRobot = null;
            System.out.println("Nie udało się włączyć trybu przechwytywania myszy. Obrót będzie działał klasycznie.");
        }
    }

    private void captureMouse() {
        if (mouseRobot == null) {
            return;
        }

        mouseCaptured = true;
        setCursor(createHiddenCursor());
        recenterMouse();
    }

    private void releaseMouse() {
        mouseCaptured = false;
        recenteringMouse = false;
        setCursor(Cursor.getDefaultCursor());
    }

    private Cursor createHiddenCursor() {
        BufferedImage transparentCursorImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        return Toolkit.getDefaultToolkit().createCustomCursor(
                transparentCursorImage,
                new Point(0, 0),
                "hidden-cursor"
        );
    }

    private void recenterMouse() {
        if (mouseRobot == null || !isShowing()) {
            return;
        }

        Point screenLocation = getLocationOnScreen();
        int centerX = screenLocation.x + getWidth() / 2;
        int centerY = screenLocation.y + getHeight() / 2;

        mouseRobot.mouseMove(centerX, centerY);
        recenteringMouse = false;
    }

    private void loadManagers() {
        textureManager = new TextureManager("com/raycasting/textures/wolfwall1.png");
        spriteManager = new SpriteManager("/com/raycasting/sprites/Guard.png");
        dogSpriteManager = new SpriteManager("/com/raycasting/sprites/GuardDog.png");
        weaponManager = new SpriteManager("/com/raycasting/sprites/pistol.PNG");
        soundManager = new SoundManager();
    }

    private void resetGame() {
        map = new Map();

        // Start na dole mapy, lekko skierowany w stronę środka poziomu.
        player = new Player(2.5, 22.5);
        player.setAngle(Math.PI * 1.75);

        raycaster = new Raycaster(map, player, textureManager, spriteManager);
        // Widok 3D jest teraz domyślny. Mapa z góry pojawia się tylko podczas przytrzymania PPM.
        raycaster.setGameState(GameState.TEXTURED_3D);
        raycaster.setSoundManager(soundManager);
        soundManager.startBackgroundMusic();

        SpriteConfigurator config = new SpriteConfigurator(spriteManager);
        SpriteEntity pistolEntity = config.createWeaponPistol();
        raycaster.setWeaponSprite(pistolEntity, weaponManager);

        addGuards(config);

        Arrays.fill(keys, false);

        System.out.println("Nowa gra rozpoczęta.");
    }

    private void addGuards(SpriteConfigurator config) {
        // Przeciwnicy są rozmieszczeni siatką po całej mapie, ale każdy spawn jest
        // przesuwany do najbliższego pustego pola 0. Połowa to strażnicy, połowa to psy.
        int[][] preferredCells = {
            {4, 3}, {10, 3}, {16, 3}, {22, 3}, {28, 3},
            {4, 8}, {10, 8}, {16, 8}, {22, 8}, {28, 8},
            {4, 13}, {10, 13}, {16, 13}, {22, 13}, {28, 13},
            {4, 18}, {10, 18}, {16, 18}, {22, 18}, {28, 18}
        };

        int enemyIndex = 0;
        for (int[] cell : preferredCells) {
            int[] emptyCell = findNearestEmptyCell(cell[0], cell[1]);

            if (emptyCell == null) {
                System.out.println("Pominąłem przeciwnika przy " + cell[0] + "/" + cell[1] + " - brak pustego pola.");
                continue;
            }

            double enemyX = emptyCell[0] + 0.5;
            double enemyY = emptyCell[1] + 0.5;

            // Nie stawiamy przeciwnika bezpośrednio przy starcie gracza.
            if (Math.hypot(enemyX - player.getX(), enemyY - player.getY()) < 4.0) {
                continue;
            }

            boolean shouldSpawnDog = enemyIndex % 2 == 1;
            MovingSprite enemy;

            if (shouldSpawnDog) {
                enemy = new MovingSprite(enemyX, enemyY, config.createGuardDog(), dogSpriteManager);
                enemy.speed = 0.030;
                enemy.STOP_DISTANCE = 0.75;
                enemy.setRangedAttacker(false); // pies zadaje obrażenia tylko bezpośrednio przy graczu
            } else {
                enemy = new MovingSprite(enemyX, enemyY, config.createGuard(), spriteManager);
                enemy.speed = 0.018;
                enemy.STOP_DISTANCE = 3.0; // strażnik strzela z ok. 3 pól
                enemy.setRangedAttacker(true);
            }

            raycaster.addMovingSprite(enemy);
            enemyIndex++;
        }
    }

    private int[] findNearestEmptyCell(int startX, int startY) {
        if (isEmptyCell(startX, startY)) {
            return new int[]{startX, startY};
        }

        for (int radius = 1; radius <= 4; radius++) {
            for (int y = startY - radius; y <= startY + radius; y++) {
                for (int x = startX - radius; x <= startX + radius; x++) {
                    if (Math.abs(x - startX) != radius && Math.abs(y - startY) != radius) {
                        continue;
                    }

                    if (isEmptyCell(x, y)) {
                        return new int[]{x, y};
                    }
                }
            }
        }

        return null;
    }

    private boolean isEmptyCell(int x, int y) {
        return x > 0
                && x < map.getWidth() - 1
                && y > 0
                && y < map.getHeight() - 1
                && map.getCell(x, y) == 0;
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
        if (soundManager != null) {
            soundManager.stopAllMusic();
        }

        try {
            if (gameThread != null) {
                gameThread.join();
            }
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
        if (raycaster != null && raycaster.isGameOver()) {
            return;
        }

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
        if (!mouseCaptured || mouseRobot == null || !isShowing() || !hasFocus()) {
            return;
        }

        Point mousePosition = MouseInfo.getPointerInfo().getLocation();
        Point screenLocation = getLocationOnScreen();

        int centerX = screenLocation.x + getWidth() / 2;
        int centerY = screenLocation.y + getHeight() / 2;
        int deltaX = mousePosition.x - centerX;

        // Mała martwa strefa usuwa mikroruchy po automatycznym zawróceniu kursora.
        if (Math.abs(deltaX) > 1) {
            player.rotateBy(deltaX * MOUSE_SENSITIVITY);
            mouseRobot.mouseMove(centerX, centerY);
        }
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
                if (!raycaster.isGameOver()) {
                    raycaster.shoot();
                }
                break;

            case KeyEvent.VK_R:
                if (raycaster.isGameOver()) {
                    resetGame();
                }
                break;

            case KeyEvent.VK_ESCAPE:
                releaseMouse();
                running = false;
                System.exit(0);
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
        // Obrót jest liczony w update() z globalnej pozycji kursora.
        // To jest stabilniejsze niż poleganie na mouseMoved(), bo Robot generuje
        // własne eventy i na niektórych systemach Swing potrafi je zgubić.
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        requestFocus();
        captureMouse();

        if (e.getButton() == MouseEvent.BUTTON3 && !raycaster.isGameOver()) {
            // Przytrzymanie prawego przycisku myszy pokazuje mapę z góry.
            // W tym trybie Raycaster nie rysuje HUD-u ani broni.
            raycaster.setGameState(GameState.TOP_DOWN_VIEW);
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1 && !raycaster.isGameOver()) {
            raycaster.shoot();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3 && raycaster != null && !raycaster.isGameOver()) {
            raycaster.setGameState(GameState.TEXTURED_3D);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void focusGained(FocusEvent e) {
        Arrays.fill(keys, false);
        if (mouseCaptured) {
            recenterMouse();
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        Arrays.fill(keys, false);
        releaseMouse();
    }

}
