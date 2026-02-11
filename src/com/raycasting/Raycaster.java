package com.raycasting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

public class Raycaster {
    private Map map;
    private Player player;
    private GameState gameState;

    // Widok z góry, TODO: przenieść do osobnej klasy, ale na razie niech będzie tu
    private List<RayResult> rayResults;

    public Raycaster(Map map, Player player) {
        this.map = map;
        this.player = player;
        this.gameState = GameState.TOP_DOWN_VIEW;
        this.rayResults = new ArrayList<>();
    }

    public void setGameState(GameState state) {
        this.gameState = state;
    }

    public GameState getGameState() {
        return gameState;
    }

    // Pojedyńczy strzał z lasera, taki laserowy miernik odleglosci
    public RayResult castRay(double rayAngle) {
        rayAngle = normalizeAngle(rayAngle);

        // Pozycja startowa
        double rayX = player.getX();
        double rayY = player.getY();

        // Kierunek promienia
        double rayDirX = Math.cos(rayAngle);
        double rayDirY = Math.sin(rayAngle);

        // Pozycja na mapie, trzyba rzutowac na inty przez mape w gridzie
        int mapX = (int) rayX;
        int mapY = (int) rayY;

        // Długość promienia od aktualnej pozycji do następnej linii X lub Y
        double deltaDistX = (rayDirX == 0) ? 1e30 : Math.abs(1 / rayDirX);
        double deltaDistY = (rayDirY == 0) ? 1e30 : Math.abs(1 / rayDirY);

        // Odległość do następnej ściany
        double perpWallDist = 0;

        // Kierunek kroku (+1 lub -1)
        int stepX, stepY;

        // Początkowa odległość do pierwszej linii
        double sideDistX, sideDistY;

        // krok i odległość do pierwszej linii
        if (rayDirX < 0) {
            stepX = -1;
            sideDistX = (rayX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0 - rayX) * deltaDistX;
        }

        if (rayDirY < 0) {
            stepY = -1;
            sideDistY = (rayY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0 - rayY) * deltaDistY;
        }

        // Wykrycie kolizji promienia
        boolean hit = false;
        boolean hitVertical = false; // Pionowa czy pozioma ściana, TODO: potzrebne do tekstur
        int wallType = 0;

        while (!hit) {
            // Skocz do następnej kwadratu mapy, w kierunku X lub Y
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
                hitVertical = false;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
                hitVertical = true;
            }

            if (mapX < 0 || mapX >= map.getWidth() ||
                    mapY < 0 || mapY >= map.getHeight()) {
                hit = true; // Poza mapą
                wallType = 1;
            } else if (map.getCell(mapX, mapY) > 0) {
                hit = true;
                wallType = map.getCell(mapX, mapY);
            }
        }

        // Oblicz odległość do ściany
        if (!hitVertical) {
            perpWallDist = sideDistX - deltaDistX;
        } else {
            perpWallDist = sideDistY - deltaDistY;
        }

        // Punkt trafienia
        double hitX = rayX + rayDirX * perpWallDist;
        double hitY = rayY + rayDirY * perpWallDist;

        return new RayResult(hitX, hitY, perpWallDist, wallType, hitVertical, rayAngle);
    }

    // TODO: Zrobić rzucanie wieloma promienniami w FOV dla tryby 3D
    public void castAllRays(int screenWidth) {
        rayResults.clear();

        if (gameState == GameState.TOP_DOWN_VIEW) {
            // kilka promieni dla efektu
            int numRays = 60;
            for (int i = 0; i < numRays; i++) {
                double rayAngle = player.getAngle() - player.getFOV() / 2 +
                        (player.getFOV() * i / numRays);
                rayResults.add(castRay(rayAngle));
            }
        } else if (gameState == GameState.WOLFENSTEIN_3D) {
            // promień dla każdej kolumny pikseli
            for (int x = 0; x < screenWidth; x++) {
                double cameraX = 2 * x / (double) screenWidth - 1; // -1 do 1
                double rayAngle = player.getAngle() + player.getFOV() / 2 * cameraX;
                rayResults.add(castRay(rayAngle));
            }
        }
    }

    // Rysowanie promieni
    // TODO: Zachować różne stany rysowania dla demonstracji
    public void draw(Graphics2D g, int screenWidth, int screenHeight) {
        switch (gameState) {
            case TOP_DOWN_VIEW:
                drawTopDownView(g, screenWidth, screenHeight);
                break;
            case WOLFENSTEIN_3D:
                drawWolfenstein3D(g, screenWidth, screenHeight);
                break;
            case RAYCAST_2D:
                drawRaycast2D(g, screenWidth, screenHeight);
                break;
            case TEXTURED_3D:
                drawTextured3D(g, screenWidth, screenHeight);
                break;
        }
    }

    private void drawTopDownView(Graphics2D g, int screenWidth, int screenHeight) {
        // Skala dla mapy
        int cellSize = 20;
        int mapOffsetX = 20;
        int mapOffsetY = 20;

        // Rysuj siatkę mapy
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                int cell = map.getCell(x, y);

                // Kolor w zależności od typu komórki
                Color color;
                if (cell == 0) {
                    color = Color.WHITE; // puste
                } else {
                    // TODO: Zamieniń na tekstury, na razie kolorki
                    float hue = (cell - 1) / 9.0f;
                    color = Color.getHSBColor(hue, 0.8f, 0.8f);
                }

                g.setColor(color);
                g.fillRect(mapOffsetX + x * cellSize,
                        mapOffsetY + y * cellSize,
                        cellSize, cellSize);

                // Obramowanie
                g.setColor(Color.GRAY);
                g.drawRect(mapOffsetX + x * cellSize,
                        mapOffsetY + y * cellSize,
                        cellSize, cellSize);

                // Numer typu ściany
                if (cell > 0) {
                    g.setColor(Color.BLACK);
                    g.drawString(String.valueOf(cell),
                            mapOffsetX + x * cellSize + cellSize / 2 - 3,
                            mapOffsetY + y * cellSize + cellSize / 2 + 3);
                }
            }
        }

        // Rysuj gracza
        int playerScreenX = mapOffsetX + (int) (player.getX() * cellSize);
        int playerScreenY = mapOffsetY + (int) (player.getY() * cellSize);

        g.setColor(Color.RED);
        g.fillOval(playerScreenX - 5, playerScreenY - 5, 10, 10);

        // Rysuj FOV (linia)
        int lineEndX = playerScreenX + (int) (Math.cos(player.getAngle()) * 20);
        int lineEndY = playerScreenY + (int) (Math.sin(player.getAngle()) * 20);
        g.setColor(Color.RED);
        g.drawLine(playerScreenX, playerScreenY, lineEndX, lineEndY);

        // Rysuj promienie
        g.setColor(new Color(255, 100, 100, 150)); // Półprzezroczysty różowy
        for (RayResult ray : rayResults) {
            int rayEndX = mapOffsetX + (int) (ray.hitX * cellSize);
            int rayEndY = mapOffsetY + (int) (ray.hitY * cellSize);
            g.drawLine(playerScreenX, playerScreenY, rayEndX, rayEndY);
        }

        // Legenda
        g.setColor(Color.BLACK);
        g.drawString("WIDOK Z GÓRY (TOP_DOWN_VIEW)", 20, screenHeight - 60);
        g.drawString("Sterowanie: WASD - ruch, Mysz - obrót, SPACJA - zmień tryb", 20, screenHeight - 40);
        g.drawString(player.toString(), 20, screenHeight - 20);
    }

    // Rysuj w 3D, jak w wolfenstainie
    private void drawWolfenstein3D(Graphics2D g, int screenWidth, int screenHeight) {
        // Tło (niebo i podłoga)
        g.setColor(new Color(100, 150, 255)); // Niebieskie niebo
        g.fillRect(0, 0, screenWidth, screenHeight / 2);

        g.setColor(new Color(80, 60, 40)); // Brązowa podłoga
        g.fillRect(0, screenHeight / 2, screenWidth, screenHeight / 2);

        // Rysuj ściany
        if (rayResults.size() > 0) {
            int stripWidth = screenWidth / rayResults.size();

            for (int i = 0; i < rayResults.size(); i++) {
                RayResult ray = rayResults.get(i);

                // TODO: Uwaga na przyszłosć, żeby nie dzielić przez zero
                double perpWallDist = Math.max(ray.distance, 0.0001);

                // Wysokość ściany
                int lineHeight = (int) (screenHeight / perpWallDist);

                // Oblicz górę i dół ściany
                int drawStart = -lineHeight / 2 + screenHeight / 2;
                int drawEnd = lineHeight / 2 + screenHeight / 2;

                // Ogranicz do ekranu
                if (drawStart < 0)
                    drawStart = 0;
                if (drawEnd >= screenHeight)
                    drawEnd = screenHeight - 1;

                // Kolor ściany w zależności od typu i orientacji
                Color wallColor = getWallColor(ray.wallType, ray.hitVertical);

                // Cień w zależności od odległości
                float brightness = (float) Math.min(1.0, 1.0 / (perpWallDist * 0.5));
                wallColor = new Color(
                        (int) (wallColor.getRed() * brightness),
                        (int) (wallColor.getGreen() * brightness),
                        (int) (wallColor.getBlue() * brightness));

                // Ppasek ściany
                g.setColor(wallColor);
                g.fillRect(i * stripWidth, drawStart, stripWidth, drawEnd - drawStart);

                // Rysowanie ciemniejszej krawędzi, zludznie 3D
                if (ray.hitVertical) {
                    g.setColor(wallColor.darker());
                    g.drawRect(i * stripWidth, drawStart, stripWidth, drawEnd - drawStart);
                }
            }
        }

        // HUD
        g.setColor(Color.WHITE);
        g.drawString("WOLFENSTEIN 3D MODE", 20, 30);
        g.drawString("Sterowanie: WASD - ruch, Mysz - obrót, SPACJA - zmień tryb", 20, 50);
        g.drawString(player.toString(), 20, 70);
        g.drawString("Liczba promieni: " + rayResults.size(), 20, 90);
    }

    // 2D
    private void drawRaycast2D(Graphics2D g, int screenWidth, int screenHeight) {
        drawTopDownView(g, screenWidth, screenHeight); // Na razie to samo co top-down
        g.setColor(Color.BLACK);
        g.drawString("RAYCAST 2D DEBUG MODE", 20, screenHeight - 80);
    }

    // 3D
    private void drawTextured3D(Graphics2D g, int screenWidth, int screenHeight) {
        drawWolfenstein3D(g, screenWidth, screenHeight); // Na razie bez tekstur
        g.setColor(Color.WHITE);
        g.drawString("TEXTURED 3D MODE (NOT IMPLEMENTED)", 20, 110);
    }

    // Kolorki ścian
    // TODO: Zamienić na tekstury, na razie kolorki
    private Color getWallColor(int wallType, boolean isVertical) {
        // Podstawowe kolory dla różnych typów ścian
        switch (wallType) {
            case 1:
                return isVertical ? Color.RED.darker() : Color.RED;
            case 2:
                return isVertical ? Color.GREEN.darker() : Color.GREEN;
            case 3:
                return isVertical ? Color.BLUE.darker() : Color.BLUE;
            case 4:
                return isVertical ? Color.YELLOW.darker() : Color.YELLOW;
            case 5:
                return isVertical ? Color.MAGENTA.darker() : Color.MAGENTA;
            case 6:
                return isVertical ? Color.CYAN.darker() : Color.CYAN;
            case 7:
                return isVertical ? Color.ORANGE.darker() : Color.ORANGE;
            case 8:
                return isVertical ? Color.PINK.darker() : Color.PINK;
            case 9:
                return isVertical ? Color.GRAY.darker() : Color.GRAY;
            default:
                return Color.WHITE;
        }
    }

    private double normalizeAngle(double angle) {
        while (angle < 0)
            angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI)
            angle -= 2 * Math.PI;
        return angle;
    }

    public static class RayResult {
        public final double hitX, hitY;
        public final double distance;
        public final int wallType;
        public final boolean hitVertical;
        public final double angle;

        public RayResult(double hitX, double hitY, double distance,
                int wallType, boolean hitVertical, double angle) {
            this.hitX = hitX;
            this.hitY = hitY;
            this.distance = distance;
            this.wallType = wallType;
            this.hitVertical = hitVertical;
            this.angle = angle;
        }
    }
}