package com.raycasting;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class SpriteEntity {
    private Map<String, SpriteAnimation> animations = new HashMap<>();
    private SpriteAnimation currentAnimation;
    private String currentAnimationName;

    public void addAnimation(String name, SpriteAnimation animation) {
        animations.put(name, animation);
        if (currentAnimation == null) {
            currentAnimation = animation;
            currentAnimationName = name;
        }
    }

    public void playAnimation(String name) {
        SpriteAnimation animation = animations.get(name);
        if (animation != null) {
            currentAnimation = animation;
            currentAnimationName = name;
            currentAnimation.reset();
        }
    }

    public BufferedImage getCurrentSprite(SpriteManager manager) {
        return currentAnimation == null ? null : currentAnimation.getCurrentFrame(manager);
    }

    public void update(long now) {
        if (currentAnimation != null) {
            currentAnimation.update(now);
        }
    }

    public boolean isCurrentAnimation(String name) {
        return name != null && name.equals(currentAnimationName);
    }

    public int getCurrentFrameIndex() {
        return currentAnimation == null ? 0 : currentAnimation.getCurrentFrameIndex();
    }
}
