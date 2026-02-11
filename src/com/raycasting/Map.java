package com.raycasting;

public class Map {
    private int[][] grid;
    private int width;
    private int height;

    // 0 = puste, 1-9 = różne typy ścian
    // TODO: Dodać wiecej typów textur
    private static final int[][] DEFAULT_MAP = {
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 3, 3, 3, 0, 1 },
            { 1, 0, 0, 2, 0, 0, 2, 0, 0, 0, 0, 3, 0, 3, 0, 1 },
            { 1, 0, 0, 2, 0, 0, 2, 0, 0, 0, 0, 3, 0, 3, 0, 1 },
            { 1, 0, 0, 2, 2, 2, 2, 0, 0, 0, 0, 3, 0, 3, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 4, 4, 4, 4, 4, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 4, 0, 5, 5, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 4, 0, 5, 5, 0, 4, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 4, 0, 0, 0, 0, 4, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 4, 4, 4, 4, 4, 4, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }
    };

    public Map() {
        this(DEFAULT_MAP);
    }

    public Map(int[][] customMap) {
        this.grid = customMap;
        this.height = customMap.length;
        this.width = customMap[0].length;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return true; // Poza mapą = ściana
        }
        return grid[y][x] != 0;
    }

    public int getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return 1; // Poza mapą = ściana typu 1
        }
        return grid[y][x];
    }

    public void setCell(int x, int y, int value) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[y][x] = value;
        }
    }

    public void printMap() {
        System.out.println("Mapa " + width + "x" + height + ":");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(grid[y][x] + " ");
            }
            System.out.println();
        }
    }
}