package com.raycasting;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class SpriteManager {
    private static final int GRID_COLS = 8;
    private static final int SPRITE_SIZE = 64;
    private static final int SPRITE_STRIDE = 65; // 64 + 1px odstępu
    private static final int KEY_COLOR = 0xFF00FF; // różowy #FF00FF (magenta) - RGB without alpha

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
            
            // Obsługa małych sprite sheetów (np. broń)
            if (spriteSheet.getHeight() < SPRITE_SIZE || spriteSheet.getWidth() < SPRITE_SIZE) {
                // Jeśli to sprite z kilkoma klatkami w jednym rzędzie (np. broń 326x62)
                // Wyciągnij WSZYSTKIE klatki (każda 62x62)
                int frameHeight = spriteSheet.getHeight();
                int frameWidth = frameHeight;  // Kwadratowe klatki
                int frameCount = spriteSheet.getWidth() / frameWidth;
                
                sprites = new BufferedImage[1][frameCount];
                for (int i = 0; i < frameCount; i++) {
                    BufferedImage frame = spriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
                    sprites[0][i] = makeTransparent(frame);
                }
                System.out.println("SpriteManager loaded weapon: " + spriteSheet.getWidth() + "x" + spriteSheet.getHeight() + " (" + frameCount + " frames, " + frameWidth + "x" + frameHeight + " each)");
                return;
            }
            
            int rows = spriteSheet.getHeight() >= SPRITE_SIZE ?
                    ((spriteSheet.getHeight() - SPRITE_SIZE) / SPRITE_STRIDE) + 1 : 0;
            int cols = spriteSheet.getWidth() >= SPRITE_SIZE ?
                    ((spriteSheet.getWidth() - SPRITE_SIZE) / SPRITE_STRIDE) + 1 : 0;

            sprites = new BufferedImage[rows][cols];

            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    int sourceX = col * SPRITE_STRIDE;
                    int sourceY = row * SPRITE_STRIDE;

                    if (sourceX + SPRITE_SIZE <= spriteSheet.getWidth() &&
                        sourceY + SPRITE_SIZE <= spriteSheet.getHeight()) {
                        
                        BufferedImage rawSprite = spriteSheet.getSubimage(
                            sourceX, sourceY, SPRITE_SIZE, SPRITE_SIZE
                        );
                        sprites[row][col] = makeTransparent(rawSprite);
                    }
                }
            }

            System.out.println("SpriteManager loaded: " + rows + " rows, " + cols + " cols");

        } catch (IOException e) {
            throw new RuntimeException("Błąd ładowania sprite'ów", e);
        }
    }

    /**
     * Zamienia różowy kolor tła (#FF00FF) na przezroczysty (alpha=0)
     */
    private BufferedImage makeTransparent(BufferedImage src) {
        BufferedImage dst = new BufferedImage(
            src.getWidth(), src.getHeight(), 
            BufferedImage.TYPE_INT_ARGB
        );

        int transparentCount = 0;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                int rgb = argb & 0x00FFFFFF;
                int alpha = (argb >> 24) & 0xFF;

                // Jeśli pixel już ma przezroczystość (alpha < 200), pomiń
                if (alpha < 200) {
                    dst.setRGB(x, y, argb);  // Pozostaw takim jakim jest
                } else if (isMagenta(rgb)) {
                    dst.setRGB(x, y, 0x00000000); // Uczyń w pełni przezroczystym
                    transparentCount++;
                } else {
                    // Zachowaj normalnie
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

        // Detekcja TYLKO magenty (#FF00FF i #980088):
        // - Zielony kanał ZAWSZE prawie 0
        // - Red i Blue wysokie i PODOBNE (mała różnica)
        // - Zignoruj ciemne szare/czarne pixele
        
        if (g >= 30) return false;  // Zielony zbyt wysoki - to nie magenta
        
        // Dla jasnej magenty: R,B > 200
        if (r > 200 && b > 200) return true;
        
        // Dla ciemnej magenty #980088: R≈152, B≈136 (różnica ~16)
        // ale upewnij się że to magenta a nie czarny shadow
        if (r >= 130 && b >= 120 && r <= 180 && b <= 160) {
            int colorDiff = Math.abs(r - b);
            return colorDiff <= 30;  // Ścisłe podobieństwo R i B
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
        x = Math.floorMod(x, SPRITE_SIZE);
        y = Math.floorMod(y, SPRITE_SIZE);
        return sprite.getRGB(x, y);
    }

    public int getRows() { return sprites.length; }
    public int getCols() { return sprites[0].length; }
}