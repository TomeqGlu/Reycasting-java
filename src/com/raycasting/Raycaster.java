package com.raycasting;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Raycaster {
    private Map map;
    private Player player;
    private GameState gameState;
    private TextureManager textureManager;

    private static final double MAX_LIGHT_DISTANCE = 8.0; // im wieksze tym wolniejsze ciemnienie
    private static final double MIN_LIGHT = 0.25; // im nizsze tym dalsze sciany ciemniejsze

    public Raycaster(Map map, Player player, TextureManager textureManager) {
        this.map = map;
        this.player = player;
        this.textureManager = textureManager;
        this.gameState = GameState.TOP_DOWN_VIEW;
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void castAllRays(int screenWidth) {
    }

    public void draw(BufferedImage buffer) {
        if (gameState == GameState.TOP_DOWN_VIEW) {
            drawTopDownView(buffer);
        } else {
            drawTextured3D(buffer);
        }

        drawHud(buffer);
    }

    private void drawTopDownView(BufferedImage buffer) {
        Graphics2D g = buffer.createGraphics();

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());

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
                    g.drawString(String.valueOf(cell), offsetX + x * cellSize + 8, offsetY + y * cellSize + 16);
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

        g.dispose();
    }

    private void drawTextured3D(BufferedImage buffer) {
        int screenWidth = buffer.getWidth();
        int screenHeight = buffer.getHeight();

        int[] pixels = ((DataBufferInt) buffer.getRaster().getDataBuffer()).getData();

        renderBackground(pixels, screenWidth, screenHeight);

        double playerX = player.getX();
        double playerY = player.getY();

        double dirX = Math.cos(player.getAngle());
        double dirY = Math.sin(player.getAngle());

        double planeLength = Math.tan(player.getFOV() / 2.0);

        double planeX = -dirY * planeLength;
        double planeY = dirX * planeLength;

        for (int screenX = 0; screenX < screenWidth; screenX++) {
            double cameraX = 2.0 * screenX / screenWidth - 1.0;

            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;

            RayHit hit = castRay(playerX, playerY, rayDirX, rayDirY);

            drawWallColumn(
                    pixels,
                    screenWidth,
                    screenHeight,
                    screenX,
                    hit,
                    rayDirX,
                    rayDirY);
        }
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

        distance = Math.max(distance, 0.15);

        double hitX = playerX + rayDirX * distance;
        double hitY = playerY + rayDirY * distance;

        return new RayHit(hitX, hitY, distance, wallType, hitVerticalWall);
    }

    private void drawWallColumn(
            int[] pixels,
            int screenWidth,
            int screenHeight,
            int screenX,
            RayHit hit,
            double rayDirX,
            double rayDirY) {
        BufferedImage texture = textureManager.getTexture(hit.wallType);

        int lineHeight = (int) (screenHeight / hit.distance);

        int drawStart = -lineHeight / 2 + screenHeight / 2;
        int drawEnd = lineHeight / 2 + screenHeight / 2;

        int clippedStart = Math.max(drawStart, 0);
        int clippedEnd = Math.min(drawEnd, screenHeight - 1);

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

    private void drawHud(BufferedImage buffer) {
        Graphics2D g = buffer.createGraphics();

        int width = buffer.getWidth();
        int height = buffer.getHeight();
        int hudHeight = 96;
        int hudY = height - hudHeight;

        g.setColor(new Color(0, 35, 120));
        g.fillRect(0, hudY, width, hudHeight);

        g.setColor(new Color(20, 90, 200));
        g.drawRect(0, hudY, width - 1, hudHeight - 1);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 18));

        g.drawString("JAVA RAYCASTING", 24, hudY + 28);
        g.drawString("MODE: " + gameState, 24, hudY + 58);

        g.drawString("HEALTH", 260, hudY + 28);
        g.drawString("100%", 280, hudY + 58);

        g.drawString("AMMO", 410, hudY + 28);
        g.drawString("50", 430, hudY + 58);

        g.drawString("POS", 540, hudY + 28);
        g.drawString(
                String.format("%.2f / %.2f", player.getX(), player.getY()),
                540,
                hudY + 58);

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.drawString("WASD - ruch | Mysz - obrót | SPACJA - tryb | 1 - mapa | 2 - tekstury", 24, hudY + 84);

        g.dispose();
    }

    private void renderBackground(int[] pixels, int screenWidth, int screenHeight) {
        int ceilingColor = 0x87CEEB;
        int floorColor = 0x5A3E2B;

        for (int y = 0; y < screenHeight; y++) {
            int color = y < screenHeight / 2 ? ceilingColor : floorColor;

            for (int x = 0; x < screenWidth; x++) {
                pixels[y * screenWidth + x] = color;
            }
        }
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