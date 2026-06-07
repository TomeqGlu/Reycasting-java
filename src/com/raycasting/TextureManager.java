package com.raycasting;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class TextureManager {
    private static final int GRID_SIZE = 8;
    private static final int TEXTURE_COUNT = 64;

    // Wolfensteinowe tekstury zwykle mają 64x64
    private static final int TILE_SIZE = 64;
    private static final int TILE_STRIDE = 65; // 64 px tekstury + 1 px odstępu

    private final BufferedImage[] textures = new BufferedImage[TEXTURE_COUNT + 1];

    public TextureManager(String resourcePath) {
        loadTextures(resourcePath);
    }

    private void loadTextures(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)){
            if (inputStream == null) {
                throw new RuntimeException("Nie znaleziono pliku tekstur: " + resourcePath);
            }

            BufferedImage spriteSheet = ImageIO.read(inputStream);

            System.out.println("TEXTURES LOADED: "
                    + spriteSheet.getWidth() + "x" + spriteSheet.getHeight());

            for (int i = 1; i <= TEXTURE_COUNT; i++) {
                int index = i - 1;

                int gridX = index % GRID_SIZE;
                int gridY = index / GRID_SIZE;

                int sourceX = gridX * TILE_STRIDE;
                int sourceY = gridY * TILE_STRIDE;

                if (sourceX + TILE_SIZE > spriteSheet.getWidth()
                        || sourceY + TILE_SIZE > spriteSheet.getHeight()) {
                    System.out.println("Pomijam teksturę " + i + " - poza obrazkiem");
                    textures[i] = textures[1];
                    continue;
                }

                textures[i] = spriteSheet.getSubimage(
                        sourceX,
                        sourceY,
                        TILE_SIZE,
                        TILE_SIZE);
            }

        } catch (IOException e) {
            throw new RuntimeException("Błąd podczas ładowania tekstur", e);
        }
    }

    public BufferedImage getTexture(int textureId) {
        if (textureId < 1 || textureId > TEXTURE_COUNT || textures[textureId] == null) {
            return textures[1];
        }

        return textures[textureId];
    }

    public int getTexturePixel(int textureId, int x, int y) {
        BufferedImage texture = getTexture(textureId);

        x = Math.floorMod(x, texture.getWidth());
        y = Math.floorMod(y, texture.getHeight());

        return texture.getRGB(x, y);
    }
}