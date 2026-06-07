package com.raycasting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

public class Raycaster {
    private Map map;
    private Player player;
    private GameState gameState;
    private TextureManager textureManager;
    private SpriteManager spriteManager;
    private SpriteManager weaponManager;
    private SoundManager soundManager;
    
    private List<MovingSprite> movingSprites = new ArrayList<>();
    private SpriteEntity weaponSprite;
    private boolean isShooting = false;
    private long shootEndTime = 0;
    private boolean gameOver = false;
    private boolean playerWon = false;
    private String gameOverReason = "";
    private long damageFlashEndTime = 0;

    private static final int HUD_HEIGHT = 96;
    private static final int WEAPON_SIZE = 126;
    private static final double GUARD_SIGHT_RANGE = 3.8;
    private static final int GUARD_DAMAGE = 10;
    private static final int PLAYER_DAMAGE = 25;

    private static final double MAX_LIGHT_DISTANCE = 8.0;
    private static final double MIN_LIGHT = 0.25;

    private static final boolean USE_TEXTURED_FLOOR = true;
    private static final boolean USE_TEXTURED_CEILING = false;

    private static final int FLOOR_TEXTURE_ID = 8;
    private static final int CEILING_TEXTURE_ID = 50;

    private static final int FLOOR_COLOR = 0x3A3A3A;
    private static final int CEILING_COLOR = 0x555555;

    private static final double CEILING_TEXTURE_MAX_DISTANCE = 4.0;

    public Raycaster(Map map, Player player, TextureManager textureManager) {
        this.map = map;
        this.player = player;
        this.textureManager = textureManager;
        this.gameState = GameState.TOP_DOWN_VIEW;
        this.spriteManager = null;
        this.weaponManager = null;
    }

    public Raycaster(Map map, Player player, TextureManager textureManager, SpriteManager spriteManager) {
        this.map = map;
        this.player = player;
        this.textureManager = textureManager;
        this.spriteManager = spriteManager;
        this.gameState = GameState.TOP_DOWN_VIEW;
        this.weaponManager = null;
    }

    public void setSpriteManager(SpriteManager spriteManager) {
        this.spriteManager = spriteManager;
    }

    public void setWeaponSprite(SpriteEntity weapon, SpriteManager manager) {
        this.weaponSprite = weapon;
        this.weaponManager = manager;
    }

    public void setSoundManager(SoundManager soundManager) {
        this.soundManager = soundManager;
    }

    public void shoot() {
        if (!isShooting && weaponSprite != null && player.isAlive()) {
            isShooting = true;
            player.recordShot();
            weaponSprite.playAnimation("shoot");
            if (soundManager != null) {
                soundManager.playPlayerShot();
            }
            shootEndTime = System.currentTimeMillis() + 200;
            
            // Check if we hit a guard
            for (MovingSprite guard : movingSprites) {
                if (!guard.isAlive()) continue;
                
                double dx = guard.x - player.getX();
                double dy = guard.y - player.getY();
                double dist = Math.hypot(dx, dy);
                
                // Check if guard is close to ray direction
                double guardAngle = Math.atan2(dy, dx);
                double angleDiff = Math.abs(player.getAngle() - guardAngle);
                
                // Normalize angle difference
                while (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                
                // If within a small angle (±15 degrees) and distance is reasonable
                if (angleDiff < Math.PI / 12
                        && dist < 10.0
                        && hasLineOfSight(player.getX(), player.getY(), guard.x, guard.y)) {
                    guard.takeDamage(PLAYER_DAMAGE);
                    System.out.println("Strzeliłeś w strażnika! HP: " + guard.getHP());
                    if (!guard.isAlive()) {
                        System.out.println("Strażnik zabity!");
                    }
                    break;
                }
            }
            
            System.out.println("Pew!");
        }
    }

    public void shootAtPlayer(MovingSprite guard) {
        if (guard.canShoot() && guard.isAlive() && player.isAlive()) {
            // Tu tylko rozpoczynamy animację ataku.
            // Obrażenia wejdą dopiero później, na 3. klatce animacji attack.
            guard.recordShot();
            guard.restartAttackAnimation();
        }
    }

    private void applyEnemyAttackDamage(MovingSprite enemy) {
        if (!enemy.isAlive() || !player.isAlive()) {
            return;
        }

        enemy.markAttackDamageDealt();

        double distanceToPlayer = enemy.getDistanceTo(player.getX(), player.getY());

        if (enemy.isRangedAttacker()) {
            // Strażnik z bronią trafia tylko wtedy, gdy w klatce strzału nadal widzi gracza.
            if (!hasLineOfSight(enemy.x, enemy.y, player.getX(), player.getY())) {
                return;
            }

            if (soundManager != null) {
                soundManager.playGuardShot();
            }
        } else {
            // GuardDog nie strzela. Zadaje obrażenia tylko, jeśli w klatce ataku
            // jest bezpośrednio przy graczu.
            if (distanceToPlayer > enemy.STOP_DISTANCE + 0.25) {
                return;
            }
        }

        player.takeDamage(GUARD_DAMAGE);
        damageFlashEndTime = System.currentTimeMillis() + 180;

        System.out.println("Ouch! Przeciwnik trafił! Życie: " + player.getHP());
        if (!player.isAlive()) {
            gameOver = true;
            gameOverReason = enemy.isRangedAttacker()
                    ? "WYELIMINOWANY PRZEZ STRAŻNIKA"
                    : "ZAGRYZIONY PRZEZ PSA";
            playerWon = false;
        }
    }

    private boolean hasLineOfSight(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = Math.hypot(dx, dy);
        
        if (dist == 0) return true;
        
        // Check several points along the line
        int steps = Math.max(10, (int)(dist * 4));
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double checkX = x1 + dx * t;
            double checkY = y1 + dy * t;
            
            if (map.isWall((int)checkX, (int)checkY)) {
                return false;
            }
        }
        return true;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getGameOverReason() {
        return gameOverReason;
    }

    public void addMovingSprite(MovingSprite sprite) {
        movingSprites.add(sprite);
    }

    public void removeMovingSprite(MovingSprite sprite) {
        movingSprites.remove(sprite);
    }

    public void clearMovingSprites() {
        movingSprites.clear();
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void draw(BufferedImage buffer) {
        updateMusicState();

        if (gameOver) {
            drawGameOver(buffer);
            return;
        }

        if (!movingSprites.isEmpty() && countAliveSprites() == 0) {
            gameOver = true;
            playerWon = true;
            gameOverReason = "YOU PURGE NAZIS!";
            drawGameOver(buffer);
            return;
        }

        if (!player.isAlive()) {
            gameOver = true;
            gameOverReason = "ZABITY";
            playerWon = false;
            drawGameOver(buffer);
            return;
        }

        clearBuffer(buffer);

        if (gameState == GameState.TOP_DOWN_VIEW) {
            drawTopDownView(buffer);
        } else {
            drawTextured3D(buffer);
        }

        drawHud(buffer);
        drawWeapon(buffer);
        drawDamageFlash(buffer);
    }

    private void updateMusicState() {
        if (soundManager == null) return;

        if (!player.isAlive() || gameOver) {
            soundManager.stopLowHealthMusic();
            return;
        }

        if (player.getHP() < 50) {
            soundManager.startLowHealthMusic();
        } else {
            soundManager.stopLowHealthMusic();
            soundManager.startBackgroundMusic();
        }
    }

    private void drawGameOver(BufferedImage buffer) {
        clearBuffer(buffer);
        Graphics2D g = buffer.createGraphics();
        
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
        
        g.setColor(playerWon ? Color.GREEN : Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 56));
        String title = playerWon ? "YOU PURGE NAZIS!" : "GAME OVER";
        g.drawString(title, buffer.getWidth() / 2 - 260, buffer.getHeight() / 2 - 60);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.drawString(gameOverReason, buffer.getWidth() / 2 - 180, buffer.getHeight() / 2 + 10);

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        g.drawString("Strażnicy pozostali: " + countAliveSprites(),
            buffer.getWidth() / 2 - 150, buffer.getHeight() / 2 + 55);
        g.drawString("Naciśnij R, aby zacząć od nowa",
            buffer.getWidth() / 2 - 170, buffer.getHeight() / 2 + 90);
        
        g.dispose();
    }

    private int countAliveSprites() {
        int count = 0;
        for (MovingSprite sprite : movingSprites) {
            if (sprite.isAlive()) count++;
        }
        return count;
    }

    private void clearBuffer(BufferedImage buffer) {
        int[] pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0x000000;
        }
    }

    private int getViewHeight(BufferedImage buffer) {
        return buffer.getHeight() - HUD_HEIGHT;
    }

    private void drawTopDownView(BufferedImage buffer) {
        Graphics2D g = buffer.createGraphics();

        int viewHeight = getViewHeight(buffer);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, buffer.getWidth(), viewHeight);

        int cellSize = 24;
        int offsetX = 20;
        int offsetY = 20;

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int cell = map.getCell(x, y);

                g.setColor(cell == 0 ? Color.WHITE : Color.DARK_GRAY);
                g.fillRect(offsetX + x * cellSize, offsetY + y * cellSize, cellSize, cellSize);

                g.setColor(Color.GRAY);
                g.drawRect(offsetX + x * cellSize, offsetY + y * cellSize, cellSize, cellSize);

                if (cell > 0) {
                    g.setColor(Color.YELLOW);
                    g.drawString(
                            String.valueOf(cell),
                            offsetX + x * cellSize + 8,
                            offsetY + y * cellSize + 16);
                }
            }
        }

        int playerScreenX = offsetX + (int) (player.getX() * cellSize);
        int playerScreenY = offsetY + (int) (player.getY() * cellSize);

        g.setColor(Color.RED);
        g.fillOval(playerScreenX - 5, playerScreenY - 5, 10, 10);

        double dirX = Math.cos(player.getAngle());
        double dirY = Math.sin(player.getAngle());

        double planeLength = Math.tan(player.getFOV() / 2.0);
        double planeX = -dirY * planeLength;
        double planeY = dirX * planeLength;

        g.setColor(Color.YELLOW);

        int rayCount = 80;

        for (int i = 0; i < rayCount; i++) {
            double cameraX = 2.0 * i / rayCount - 1.0;

            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            RayHit hit = castRay(player.getX(), player.getY(), rayDirX, rayDirY);

            int hitScreenX = offsetX + (int) (hit.hitX * cellSize);
            int hitScreenY = offsetY + (int) (hit.hitY * cellSize);

            g.drawLine(playerScreenX, playerScreenY, hitScreenX, hitScreenY);
        }

        for (MovingSprite sprite : movingSprites) {
            int spriteScreenX = offsetX + (int) (sprite.x * cellSize);
            int spriteScreenY = offsetY + (int) (sprite.y * cellSize);

            // Żywi przeciwnicy są zieloni, martwi są czerwoni.
            g.setColor(sprite.isAlive() ? Color.GREEN : Color.RED);
            g.fillRect(spriteScreenX - 4, spriteScreenY - 4, 8, 8);
        }

        g.dispose();
    }

    private void drawTextured3D(BufferedImage buffer) {
        int screenWidth = buffer.getWidth();
        int viewHeight = getViewHeight(buffer);
        int[] pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();

        if (USE_TEXTURED_CEILING) {
            renderCeiling(pixels, screenWidth, viewHeight);
        } else {
            renderSolidCeiling(pixels, screenWidth, viewHeight);
        }

        if (USE_TEXTURED_FLOOR) {
            renderFloor(pixels, screenWidth, viewHeight);
        } else {
            renderSolidFloor(pixels, screenWidth, viewHeight);
        }

        double[] zBuffer = renderWalls(pixels, screenWidth, viewHeight);

        drawSprites(pixels, screenWidth, viewHeight, zBuffer);
    }

    private void renderSolidCeiling(int[] pixels, int screenWidth, int viewHeight) {
        for (int y = 0; y < viewHeight / 2; y++) {
            for (int x = 0; x < screenWidth; x++) {
                pixels[y * screenWidth + x] = CEILING_COLOR;
            }
        }
    }

    private void renderSolidFloor(int[] pixels, int screenWidth, int viewHeight) {
        for (int y = viewHeight / 2; y < viewHeight; y++) {
            for (int x = 0; x < screenWidth; x++) {
                pixels[y * screenWidth + x] = FLOOR_COLOR;
            }
        }
    }

    private void renderCeiling(int[] pixels, int screenWidth, int viewHeight) {
        BufferedImage texture = textureManager.getTexture(CEILING_TEXTURE_ID);

        double playerX = player.getX();
        double playerY = player.getY();

        double dirX = Math.cos(player.getAngle());
        double dirY = Math.sin(player.getAngle());

        double planeLength = Math.tan(player.getFOV() / 2.0);
        double planeX = -dirY * planeLength;
        double planeY = dirX * planeLength;

        double rayDirLeftX = dirX - planeX;
        double rayDirLeftY = dirY - planeY;
        double rayDirRightX = dirX + planeX;
        double rayDirRightY = dirY + planeY;

        double cameraHeight = viewHeight / 2.0;

        for (int y = 0; y < viewHeight / 2; y++) {
            int distanceFromCenter = viewHeight / 2 - y;
            double rowDistance = cameraHeight / distanceFromCenter;

            if (rowDistance > CEILING_TEXTURE_MAX_DISTANCE) {
                for (int x = 0; x < screenWidth; x++) {
                    pixels[y * screenWidth + x] = CEILING_COLOR;
                }
                continue;
            }

            double stepX = rowDistance * (rayDirRightX - rayDirLeftX) / screenWidth;
            double stepY = rowDistance * (rayDirRightY - rayDirLeftY) / screenWidth;

            double floorX = playerX + rowDistance * rayDirLeftX;
            double floorY = playerY + rowDistance * rayDirLeftY;

            double light = calculateLight(rowDistance) * 0.75;

            for (int x = 0; x < screenWidth; x++) {
                int cellX = (int) floorX;
                int cellY = (int) floorY;

                int textureX = (int) (texture.getWidth() * (floorX - cellX));
                int textureY = (int) (texture.getHeight() * (floorY - cellY));

                textureX = clamp(textureX, 0, texture.getWidth() - 1);
                textureY = clamp(textureY, 0, texture.getHeight() - 1);

                int color = texture.getRGB(textureX, textureY);
                color = applyLight(color, light);

                pixels[y * screenWidth + x] = color;

                floorX += stepX;
                floorY += stepY;
            }
        }
    }

    private void renderFloor(int[] pixels, int screenWidth, int viewHeight) {
        BufferedImage texture = textureManager.getTexture(FLOOR_TEXTURE_ID);

        double playerX = player.getX();
        double playerY = player.getY();

        double dirX = Math.cos(player.getAngle());
        double dirY = Math.sin(player.getAngle());

        double planeLength = Math.tan(player.getFOV() / 2.0);
        double planeX = -dirY * planeLength;
        double planeY = dirX * planeLength;

        double rayDirLeftX = dirX - planeX;
        double rayDirLeftY = dirY - planeY;
        double rayDirRightX = dirX + planeX;
        double rayDirRightY = dirY + planeY;

        double cameraHeight = viewHeight / 2.0;

        for (int y = viewHeight / 2 + 1; y < viewHeight; y++) {
            int distanceFromCenter = y - viewHeight / 2;
            double rowDistance = cameraHeight / distanceFromCenter;

            double stepX = rowDistance * (rayDirRightX - rayDirLeftX) / screenWidth;
            double stepY = rowDistance * (rayDirRightY - rayDirLeftY) / screenWidth;

            double floorX = playerX + rowDistance * rayDirLeftX;
            double floorY = playerY + rowDistance * rayDirLeftY;

            double light = calculateLight(rowDistance);

            for (int x = 0; x < screenWidth; x++) {
                int cellX = (int) floorX;
                int cellY = (int) floorY;

                int textureX = (int) (texture.getWidth() * (floorX - cellX));
                int textureY = (int) (texture.getHeight() * (floorY - cellY));

                textureX = clamp(textureX, 0, texture.getWidth() - 1);
                textureY = clamp(textureY, 0, texture.getHeight() - 1);

                int color = texture.getRGB(textureX, textureY);
                color = applyLight(color, light);

                pixels[y * screenWidth + x] = color;

                floorX += stepX;
                floorY += stepY;
            }
        }
    }

    private double[] renderWalls(int[] pixels, int screenWidth, int viewHeight) {
        double playerX = player.getX();
        double playerY = player.getY();

        double dirX = Math.cos(player.getAngle());
        double dirY = Math.sin(player.getAngle());

        double planeLength = Math.tan(player.getFOV() / 2.0);
        double planeX = -dirY * planeLength;
        double planeY = dirX * planeLength;

        double[] zBuffer = new double[screenWidth];

        for (int screenX = 0; screenX < screenWidth; screenX++) {
            double cameraX = 2.0 * screenX / screenWidth - 1.0;

            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            RayHit hit = castRay(playerX, playerY, rayDirX, rayDirY);
            zBuffer[screenX] = hit.distance;

            drawWallColumn(
                    pixels,
                    screenWidth,
                    viewHeight,
                    screenX,
                    hit,
                    rayDirX,
                    rayDirY);
        }

        return zBuffer;
    }

    private RayHit castRay(double playerX, double playerY, double rayDirX, double rayDirY) {
        int mapX = (int) playerX;
        int mapY = (int) playerY;

        double deltaDistX = rayDirX == 0 ? 1e30 : Math.abs(1.0 / rayDirX);
        double deltaDistY = rayDirY == 0 ? 1e30 : Math.abs(1.0 / rayDirY);

        int stepX;
        int stepY;

        double sideDistX;
        double sideDistY;

        if (rayDirX < 0) {
            stepX = -1;
            sideDistX = (playerX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0 - playerX) * deltaDistX;
        }

        if (rayDirY < 0) {
            stepY = -1;
            sideDistY = (playerY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0 - playerY) * deltaDistY;
        }

        boolean hitVerticalWall = false;
        int wallType;

        while (true) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
                hitVerticalWall = true;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
                hitVerticalWall = false;
            }

            wallType = map.getCell(mapX, mapY);

            if (wallType > 0) {
                break;
            }
        }

        double distance;

        if (hitVerticalWall) {
            distance = (mapX - playerX + (1.0 - stepX) / 2.0) / rayDirX;
        } else {
            distance = (mapY - playerY + (1.0 - stepY) / 2.0) / rayDirY;
        }

        distance = Math.max(distance, 0.35);

        double hitX = playerX + rayDirX * distance;
        double hitY = playerY + rayDirY * distance;

        return new RayHit(hitX, hitY, distance, wallType, hitVerticalWall);
    }

    private void drawWallColumn(
            int[] pixels,
            int screenWidth,
            int viewHeight,
            int screenX,
            RayHit hit,
            double rayDirX,
            double rayDirY) {
        BufferedImage texture = textureManager.getTexture(hit.wallType);

        int lineHeight = (int) (viewHeight / hit.distance);

        int drawStart = -lineHeight / 2 + viewHeight / 2;
        int drawEnd = lineHeight / 2 + viewHeight / 2;

        int clippedStart = Math.max(drawStart, 0);
        int clippedEnd = Math.min(drawEnd, viewHeight - 1);

        double wallX = hit.hitVerticalWall ? hit.hitY : hit.hitX;
        wallX -= Math.floor(wallX);

        int textureX = (int) (wallX * texture.getWidth());

        if (hit.hitVerticalWall && rayDirX > 0) {
            textureX = texture.getWidth() - textureX - 1;
        }

        if (!hit.hitVerticalWall && rayDirY < 0) {
            textureX = texture.getWidth() - textureX - 1;
        }

        textureX = clamp(textureX, 0, texture.getWidth() - 1);

        double light = calculateLight(hit.distance);

        if (!hit.hitVerticalWall) {
            light *= 0.75;
        }

        for (int y = clippedStart; y < clippedEnd; y++) {
            double texturePosition = (y - drawStart) / (double) lineHeight;
            int textureY = (int) (texturePosition * texture.getHeight());

            textureY = clamp(textureY, 0, texture.getHeight() - 1);

            int color = texture.getRGB(textureX, textureY);
            color = applyLight(color, light);

            pixels[y * screenWidth + screenX] = color;
        }
    }

    private void drawSprites(int[] pixels, int screenWidth, int viewHeight, double[] zBuffer) {
        if (spriteManager == null || movingSprites.isEmpty()) return;

        double playerX = player.getX();
        double playerY = player.getY();
        double playerAngle = player.getAngle();

        long now = System.currentTimeMillis();
        for (MovingSprite sprite : movingSprites) {
            if (!sprite.isAlive()) {
                sprite.update(now, map);
                continue;
            }

            double distToPlayer = sprite.getDistanceTo(playerX, playerY);
            boolean canSeePlayer = distToPlayer <= GUARD_SIGHT_RANGE
                    && hasLineOfSight(sprite.x, sprite.y, playerX, playerY);

            if (canSeePlayer) {
                sprite.updateToChasePlayer(now, map, player);

                double attackStartMargin = sprite.isRangedAttacker() ? 0.5 : 0.12;
                if (distToPlayer <= sprite.STOP_DISTANCE + attackStartMargin && sprite.canShoot()) {
                    shootAtPlayer(sprite);
                }

                if (sprite.shouldDealAttackDamageNow()) {
                    applyEnemyAttackDamage(sprite);
                }
            } else {
                // Gdy gracz jest daleko albo za ścianą, strażnik wraca do patrolu / losowego ruchu.
                sprite.updateRandomWander(now, map, player);
                sprite.stopAttack();
            }
        }

        movingSprites.sort((a, b) -> {
            double distA = Math.hypot(a.x - playerX, a.y - playerY);
            double distB = Math.hypot(b.x - playerX, b.y - playerY);
            return Double.compare(distB, distA);
        });

        double dirX = Math.cos(playerAngle);
        double dirY = Math.sin(playerAngle);
        double planeLength = Math.tan(player.getFOV() / 2.0);
        double planeX = -dirY * planeLength;
        double planeY = dirX * planeLength;

        for (MovingSprite sprite : movingSprites) {
            double spriteX = sprite.x - playerX;
            double spriteY = sprite.y - playerY;

            double invDet = 1.0 / (planeX * dirY - planeY * dirX);
            double transformX = invDet * (dirY * spriteX - dirX * spriteY);
            double transformY = invDet * (-planeY * spriteX + planeX * spriteY);

            if (transformY <= 0) continue;

            int spriteScreenX = (int) ((screenWidth / 2) * (1 + transformX / transformY));
            int spriteHeight = Math.abs((int) (viewHeight / transformY));
            int spriteWidth = spriteHeight;

            int drawStartY = -spriteHeight / 2 + viewHeight / 2;
            int drawEndY = spriteHeight / 2 + viewHeight / 2;
            int drawStartX = -spriteWidth / 2 + spriteScreenX;
            int drawEndX = spriteWidth / 2 + spriteScreenX;

            int clipStartY = Math.max(drawStartY, 0);
            int clipEndY = Math.min(drawEndY, viewHeight - 1);
            int clipStartX = Math.max(drawStartX, 0);
            int clipEndX = Math.min(drawEndX, screenWidth - 1);

            if (clipStartX >= clipEndX || clipStartY >= clipEndY) continue;

            BufferedImage spriteImg = sprite.getCurrentSprite(spriteManager);
            if (spriteImg == null) continue;

            double light = calculateLight(Math.hypot(sprite.x - playerX, sprite.y - playerY));
            light = Math.max(light, 0.3);

            for (int screenX = clipStartX; screenX < clipEndX; screenX++) {
                int textureX = (int) ((screenX - drawStartX) * spriteImg.getWidth() / (double) spriteWidth);
                textureX = clamp(textureX, 0, spriteImg.getWidth() - 1);

                if (zBuffer != null && screenX >= 0 && screenX < zBuffer.length && transformY >= zBuffer[screenX]) {
                    continue;
                }

                for (int screenY = clipStartY; screenY < clipEndY; screenY++) {
                    int textureY = (int) ((screenY - drawStartY) * spriteImg.getHeight() / (double) spriteHeight);
                    textureY = clamp(textureY, 0, spriteImg.getHeight() - 1);

                    int color = spriteImg.getRGB(textureX, textureY);
                    int alpha = (color >> 24) & 0xFF;

                    // Bardzo ważne: rysujemy tylko nieprzezroczyste piksele.
                    // Poprzedni warunek z || przepuszczał przezroczyste piksele i robił z nich czarne tło.
                    if (alpha > 128) {
                        color = applyLight(color, light);
                        pixels[screenY * screenWidth + screenX] = color;
                    }
                }
            }
        }
    }

    private void drawWeapon(BufferedImage buffer) {
        if (weaponSprite == null || weaponManager == null) return;

        long now = System.currentTimeMillis();
        if (isShooting && now >= shootEndTime) {
            isShooting = false;
            weaponSprite.playAnimation("idle");
        }
        
        weaponSprite.update(now);
        BufferedImage weaponImg = weaponSprite.getCurrentSprite(weaponManager);
        
        if (weaponImg != null) {
            Graphics2D g = buffer.createGraphics();
            // NEAREST_NEIGHBOR nie miesza koloru krawędzi z przezroczystością,
            // a mocniejsze przycięcie lewej strony usuwa biały 1px pasek ze sprite'a broni.
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                              java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int weaponSize = WEAPON_SIZE + 12;
            int weaponX = (buffer.getWidth() - weaponSize) / 2;

            // REGULACJA POZYCJI BRONI:
            // - mniejsza wartość WEAPON_VERTICAL_OFFSET podnosi broń,
            // - większa wartość WEAPON_VERTICAL_OFFSET opuszcza broń.
            // Przykłady: -8 = wyżej, 0 = neutralnie, 12 = niżej.
            int WEAPON_VERTICAL_OFFSET = 0;
            int weaponY = buffer.getHeight() - weaponSize - HUD_HEIGHT + WEAPON_VERTICAL_OFFSET;

            // Przycinamy każdą klatkę animacji, nie tylko idle.
            // Lewa krawędź ma większy crop, bo tam pojawiał się biały pasek z arkusza.
            int cropLeft = Math.min(16, Math.max(0, weaponImg.getWidth() / 4));
            int cropRight = Math.min(3, Math.max(0, weaponImg.getWidth() / 12));
            int cropTop = 1;
            int cropBottom = 1;
            int srcX1 = cropLeft;
            int srcY1 = cropTop;
            int srcX2 = Math.max(srcX1 + 1, weaponImg.getWidth() - cropRight);
            int srcY2 = Math.max(srcY1 + 1, weaponImg.getHeight() - cropBottom);

            g.drawImage(
                weaponImg,
                weaponX,
                weaponY,
                weaponX + weaponSize,
                weaponY + weaponSize,
                srcX1,
                srcY1,
                srcX2,
                srcY2,
                null
            );
            
            g.dispose();
        }
    }

    private double calculateLight(double distance) {
        double light = 1.0 - (distance / MAX_LIGHT_DISTANCE);

        if (light < MIN_LIGHT) {
            light = MIN_LIGHT;
        }

        if (light > 1.0) {
            light = 1.0;
        }

        return light;
    }

    private int applyLight(int color, double light) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int) (r * light);
        g = (int) (g * light);
        b = (int) (b * light);

        return (r << 16) | (g << 8) | b;
    }

    private void drawDamageFlash(BufferedImage buffer) {
        long now = System.currentTimeMillis();
        if (now >= damageFlashEndTime) {
            return;
        }

        double progress = (damageFlashEndTime - now) / 180.0;
        int alpha = (int) (120 * Math.max(0.0, Math.min(1.0, progress)));

        Graphics2D g = buffer.createGraphics();
        g.setColor(new Color(255, 0, 0, alpha));
        g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
        g.dispose();
    }

    private void drawHud(BufferedImage buffer) {
        Graphics2D g = buffer.createGraphics();

        int width = buffer.getWidth();
        int height = buffer.getHeight();
        int hudY = height - HUD_HEIGHT;

        g.setColor(new Color(0, 35, 120));
        g.fillRect(0, hudY, width, HUD_HEIGHT);

        g.setColor(new Color(20, 90, 200));
        g.drawRect(0, hudY, width - 1, HUD_HEIGHT - 1);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));

        g.drawString("JAVA RAYCASTING", 24, hudY + 28);
        g.drawString("MODE: " + gameState, 24, hudY + 58);

        g.setColor(player.getHP() <= 20 ? Color.RED : Color.WHITE);
        g.drawString("HEALTH", 260, hudY + 28);
        g.drawString(player.getHP() + "%", 280, hudY + 58);

        g.setColor(Color.WHITE);
        g.drawString("AMMO", 410, hudY + 28);
        g.drawString("∞", 430, hudY + 58);

        g.drawString("POS", 540, hudY + 28);
        g.drawString(
                String.format("%.2f / %.2f", player.getX(), player.getY()),
                540,
                hudY + 58);

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.drawString("WASD - ruch | Mysz - obrot | LPM - strzal | SPACE - broń | 1 - mapa | 2 - tekstury", 24, hudY + 84);

        g.dispose();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class RayHit {
        double hitX;
        double hitY;
        double distance;
        int wallType;
        boolean hitVerticalWall;

        RayHit(double hitX, double hitY, double distance, int wallType, boolean hitVerticalWall) {
            this.hitX = hitX;
            this.hitY = hitY;
            this.distance = distance;
            this.wallType = wallType;
            this.hitVerticalWall = hitVerticalWall;
        }
    }
}