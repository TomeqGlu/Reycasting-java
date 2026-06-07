package com.raycasting;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;

public class SpriteEntity {
    private Map<String, SpriteAnimation> animations = new HashMap<>();
    private SpriteAnimation currentAnimation;

    public void addAnimation(String name, SpriteAnimation animation) {
        animations.put(name, animation);
        if (currentAnimation == null) currentAnimation = animation;
    }

    public void playAnimation(String name) {
        SpriteAnimation anim = animations.get(name);
        if (anim != null && anim != currentAnimation) {
            currentAnimation = anim;
            currentAnimation.reset();
        }
    }

    public BufferedImage getCurrentSprite(SpriteManager manager) {
        return currentAnimation.getCurrentFrame(manager);
    }

    public void update(long now) {
        if (currentAnimation != null) {
            currentAnimation.update(now);
        }
    }
}