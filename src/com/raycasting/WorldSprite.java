package com.raycasting;

public class WorldSprite {
    public double x, y;      // pozycja w świecie
    public double distance;  // odległość od gracza (do sortowania)
    public SpriteEntity entity;
    public String currentAnim;

    public WorldSprite(double x, double y, SpriteEntity entity, String startAnim) {
        this.x = x;
        this.y = y;
        this.entity = entity;
        this.currentAnim = startAnim;
        entity.playAnimation(startAnim);
    }

    public void update(long now) {
        entity.update(now);
    }
}