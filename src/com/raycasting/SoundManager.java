package com.raycasting;

import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.HashSet;
import java.util.Set;

public class SoundManager {
    // Dodaj pliki WAV do: src/main/resources/com/raycasting/sounds/
    // Nazwy możesz zmienić poniżej, jeśli użyjesz innych plików.
    private static final String PLAYER_SHOT = "/com/raycasting/sounds/player_shoot.wav";
    private static final String GUARD_SHOT = "/com/raycasting/sounds/guard_shoot.wav";
    private static final String BACKGROUND_MUSIC = "/com/raycasting/sounds/background_music.wav";
    private static final String LOW_HEALTH_MUSIC = "/com/raycasting/sounds/low_health_music.wav";

    private Clip backgroundMusic;
    private Clip lowHealthMusic;
    private final Set<String> missingOrBrokenSounds = new HashSet<>();

    public void playPlayerShot() {
        playOnce(PLAYER_SHOT, -4.0f);
    }

    public void playGuardShot() {
        playOnce(GUARD_SHOT, -6.0f);
    }

    public void startBackgroundMusic() {
        if (isRunning(backgroundMusic) || isRunning(lowHealthMusic)) {
            return;
        }

        backgroundMusic = loop(BACKGROUND_MUSIC, -14.0f);
    }

    public void startLowHealthMusic() {
        if (isRunning(lowHealthMusic)) {
            return;
        }

        stopClip(backgroundMusic);
        backgroundMusic = null;
        lowHealthMusic = loop(LOW_HEALTH_MUSIC, -10.0f);
    }

    public void stopLowHealthMusic() {
        if (!isRunning(lowHealthMusic)) {
            return;
        }

        stopClip(lowHealthMusic);
        lowHealthMusic = null;
        startBackgroundMusic();
    }

    public void stopAllMusic() {
        stopClip(backgroundMusic);
        stopClip(lowHealthMusic);
        backgroundMusic = null;
        lowHealthMusic = null;
    }

    private void playOnce(String resourcePath, float volumeDb) {
        Clip clip = createClip(resourcePath);
        if (clip == null) return;

        setVolume(clip, volumeDb);
        clip.start();
    }

    private Clip loop(String resourcePath, float volumeDb) {
        Clip clip = createClip(resourcePath);
        if (clip == null) return null;

        setVolume(clip, volumeDb);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
        return clip;
    }

    private Clip createClip(String resourcePath) {
        if (missingOrBrokenSounds.contains(resourcePath)) {
            return null;
        }

        try {
            InputStream rawStream = getClass().getResourceAsStream(resourcePath);
            if (rawStream == null) {
                System.out.println("Brak pliku dźwięku: " + resourcePath + " - gra działa dalej bez tego dźwięku.");
                missingOrBrokenSounds.add(resourcePath);
                return null;
            }

            try (BufferedInputStream bufferedStream = new BufferedInputStream(rawStream);
                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                return clip;
            }
        } catch (Exception e) {
            System.out.println("Nie udało się odtworzyć dźwięku " + resourcePath + ": " + e.getMessage());
            missingOrBrokenSounds.add(resourcePath);
            return null;
        }
    }

    private void stopClip(Clip clip) {
        if (clip == null) return;

        clip.stop();
        clip.flush();
        clip.close();
    }

    private boolean isRunning(Clip clip) {
        return clip != null && clip.isRunning();
    }

    private void setVolume(Clip clip, float volumeDb) {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }

        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float safeVolume = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), volumeDb));
        gain.setValue(safeVolume);
    }
}
