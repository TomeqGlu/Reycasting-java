package com.raycasting;

import java.awt.image.BufferedImage;
import java.util.List;

public class SpriteAnimation {
    private final List<int[]> frames;
    private int currentFrame = 0;
    private int frameDelay = 100;
    private long lastUpdate = 0;
    private boolean isFinished = false;
    private final boolean loop;

    public SpriteAnimation(List<int[]> frames, int frameDelayMs) {
        this(frames, frameDelayMs, false);
    }

    public SpriteAnimation(List<int[]> frames, int frameDelayMs, boolean loop) {
        this.frames = frames;
        this.frameDelay = frameDelayMs;
        this.loop = loop;
    }

    public BufferedImage getCurrentFrame(SpriteManager manager) {
        if (frames.isEmpty()) return null;
        int[] frame = frames.get(currentFrame);
        return manager.getSprite(frame[0], frame[1]);
    }

    public void update(long now) {
        if (frames.isEmpty()) return;
        if (frameDelay <= 0) return;
        if (isFinished && !loop) return;

        if (lastUpdate == 0) {
            lastUpdate = now;
            return;
        }

        if (now - lastUpdate >= frameDelay) {
            currentFrame++;

            if (currentFrame >= frames.size()) {
                if (loop) {
                    currentFrame = 0;
                } else {
                    currentFrame = frames.size() - 1;
                    isFinished = true;
                }
            }

            lastUpdate = now;
        }
    }

    public void reset() {
        currentFrame = 0;
        lastUpdate = 0;
        isFinished = false;
    }

    public void setFrameDelay(int ms) {
        this.frameDelay = ms;
    }

    public int getFrameCount() {
        return frames.size();
    }

    public boolean isFinished() {
        return isFinished;
    }
}
