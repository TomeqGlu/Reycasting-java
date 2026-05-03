package com.raycasting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class Raycaster {
    private Map map;
    private Player player;
    private GameState gameState;
    private TextureManager textureManager;

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
        // Niepotrzebne w tej wersji.
    }

    public void draw(BufferedImage buffer) {
        if (gameState == GameState.TOP_DOWN_VIEW) {
            drawTopDownView(buffer);
        } else {
            drawTextured3D(buffer);
        }
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

        g.setColor(Color.WHITE);
        g.drawString("TOP DOWN VIEW - mapa + promienie", 20, buffer.getHeight() - 40);
        g.drawString("SPACJA - przełącz na TEXTURED_3D", 20, buffer.getHeight() - 20);

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

        return new RayHit(
                hitX,
                hitY,
                distance,
                wallType,
                hitVerticalWall);
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

        double wallX;

        if (hit.hitVerticalWall) {
            wallX = hit.hitY;
        } else {
            wallX = hit.hitX;
        }

        wallX -= Math.floor(wallX);

        int textureX = (int) (wallX * texture.getWidth());

        if (hit.hitVerticalWall && rayDirX > 0) {
            textureX = texture.getWidth() - textureX - 1;
        }

        if (!hit.hitVerticalWall && rayDirY < 0) {
            textureX = texture.getWidth() - textureX - 1;
        }

        textureX = clamp(textureX, 0, texture.getWidth() - 1);

        for (int y = clippedStart; y < clippedEnd; y++) {
            double texturePosition = (y - drawStart) / (double) lineHeight;
            int textureY = (int) (texturePosition * texture.getHeight());

            textureY = clamp(textureY, 0, texture.getHeight() - 1);

            int color = texture.getRGB(textureX, textureY);

            if (!hit.hitVerticalWall) {
                color = darken(color);
            }

            pixels[y * screenWidth + screenX] = color;
        }
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

    private int darken(int color) {
        return (color & 0xFEFEFE) >> 1;
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

        RayHit(
                double hitX,
                double hitY,
                double distance,
                int wallType,
                boolean hitVerticalWall) {
            this.hitX = hitX;
            this.hitY = hitY;
            this.distance = distance;
            this.wallType = wallType;
            this.hitVerticalWall = hitVerticalWall;
        }
    }
}