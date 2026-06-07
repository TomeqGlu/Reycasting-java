package com.raycasting;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class SpriteManager {
    private static final int GRID_COLS = 8;
    private static final int SPRITE_SIZE = 64;
    private static final int SPRITE_STRIDE = 65;

    private BufferedImage[][] sprites; // [row][col]

    public SpriteManager(String resourcePath) {
        loadSprites(resourcePath);
    }

    private void loadSprites(String resourcePath) {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Nie znaleziono pliku sprite'ów: " + resourcePath);
            }

            BufferedImage spriteSheet = ImageIO.read(inputStream);

            // Obsługa sprite sheetów broni, np. 326x62 z 5 klatkami.
            // Ważne: klatki nie zawsze są ułożone dokładnie co 62 px. Dla 326x62
            // stary kod brał starty 0,62,124..., przez co w animacji wpadały linie/separatory.
            if (spriteSheet.getHeight() < SPRITE_SIZE || spriteSheet.getWidth() < SPRITE_SIZE) {
                int frameHeight = spriteSheet.getHeight();
                int frameWidth = frameHeight;
                int frameCount = Math.max(1, (int) Math.round(spriteSheet.getWidth() / (double) frameWidth));
                int stride = frameCount <= 1 ? frameWidth : (spriteSheet.getWidth() - frameWidth) / (frameCount - 1);

                sprites = new BufferedImage[1][frameCount];
                for (int i = 0; i < frameCount; i++) {
                    int sourceX = Math.min(i * stride, spriteSheet.getWidth() - frameWidth);
                    BufferedImage frame = spriteSheet.getSubimage(sourceX, 0, frameWidth, frameHeight);
                    sprites[0][i] = makeTransparent(cropWeaponFrame(frame));
                }

                System.out.println("SpriteManager loaded weapon: "
                        + spriteSheet.getWidth() + "x" + spriteSheet.getHeight()
                        + " (" + frameCount + " frames, stride " + stride + ")");
                return;
            }

            int rows = spriteSheet.getHeight() >= SPRITE_SIZE
                    ? ((spriteSheet.getHeight() - SPRITE_SIZE) / SPRITE_STRIDE) + 1
                    : 0;
            int cols = spriteSheet.getWidth() >= SPRITE_SIZE
                    ? ((spriteSheet.getWidth() - SPRITE_SIZE) / SPRITE_STRIDE) + 1
                    : 0;

            sprites = new BufferedImage[rows][cols];

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int sourceX = col * SPRITE_STRIDE;
                    int sourceY = row * SPRITE_STRIDE;

                    if (sourceX + SPRITE_SIZE <= spriteSheet.getWidth()
                            && sourceY + SPRITE_SIZE <= spriteSheet.getHeight()) {
                        BufferedImage rawSprite = spriteSheet.getSubimage(
                                sourceX, sourceY, SPRITE_SIZE, SPRITE_SIZE);
                        sprites[row][col] = makeTransparent(rawSprite);
                    }
                }
            }

            System.out.println("SpriteManager loaded: " + rows + " rows, " + cols + " cols");

        } catch (IOException e) {
            throw new RuntimeException("Błąd ładowania sprite'ów", e);
        }
    }

    private BufferedImage cropWeaponFrame(BufferedImage src) {
        // Broń ma osobny, niski arkusz klatek. Na części klatek przy lewej krawędzi
        // potrafi zostać jasna linia separatora, więc usuwamy kilka pikseli źródła
        // już na etapie ładowania, dla każdej klatki animacji.
        int cropLeft = Math.min(4, src.getWidth() - 1);
        int cropRight = Math.min(2, src.getWidth() - cropLeft - 1);
        int cropTop = 0;
        int cropBottom = 0;

        int width = Math.max(1, src.getWidth() - cropLeft - cropRight);
        int height = Math.max(1, src.getHeight() - cropTop - cropBottom);

        return src.getSubimage(cropLeft, cropTop, width, height);
    }

    private BufferedImage makeTransparent(BufferedImage src) {
        BufferedImage dst = new BufferedImage(
                src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int rgb = argb & 0x00FFFFFF;
                int alpha = (argb >> 24) & 0xFF;

                if (alpha < 200) {
                    dst.setRGB(x, y, argb);
                } else if (isMagenta(rgb)) {
                    dst.setRGB(x, y, 0x00000000);
                } else {
                    dst.setRGB(x, y, argb);
                }
            }
        }
        return dst;
    }

    private boolean isMagenta(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        if (g >= 30) return false;
        if (r > 200 && b > 200) return true;

        if (r >= 130 && b >= 120 && r <= 180 && b <= 160) {
            int colorDiff = Math.abs(r - b);
            return colorDiff <= 30;
        }

        return false;
    }

    public BufferedImage getSprite(int row, int col) {
        if (row >= 0 && row < sprites.length && col >= 0 && col < sprites[row].length) {
            return sprites[row][col];
        }
        return null;
    }

    public int getSpritePixel(int row, int col, int x, int y) {
        BufferedImage sprite = getSprite(row, col);
        if (sprite == null) return 0;
        x = Math.floorMod(x, sprite.getWidth());
        y = Math.floorMod(y, sprite.getHeight());
        return sprite.getRGB(x, y);
    }

    public int getRows() {
        return sprites.length;
    }

    public int getCols() {
        return sprites.length == 0 ? 0 : sprites[0].length;
    }
}
