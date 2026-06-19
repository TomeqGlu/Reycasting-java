package com.raycasting;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.util.HashSet;
import java.util.Set;

public class SoundManager {
    
    // Ścieżki względne do folderu sounds (w bieżącym katalogu)
    private static final String PLAYER_SHOT = "sounds/player_shoot.wav";
    private static final String GUARD_SHOT = "sounds/guard_shoot.wav";
    private static final String BACKGROUND_MUSIC = "sounds/GetThemBeforeTheyGetYou.wav";
    private static final String LOW_HEALTH_MUSIC = "sounds/low_health_music.wav";

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

        if (backgroundMusic != null && !backgroundMusic.isRunning()) {
            backgroundMusic.setFramePosition(0);
            backgroundMusic.start();
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

    private void playOnce(String filePath, float volumeDb) {
        Clip clip = createClip(filePath);
        if (clip == null) return;

        setVolume(clip, volumeDb);
        clip.start();
        
        // Automatyczne zamknięcie clipu po odtworzeniu (dla krótkich dźwięków)
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                if (clip.isRunning()) {
                    Thread.sleep(100);
                }
                clip.close();
            } catch (InterruptedException e) {
                clip.close();
            }
        }).start();
    }

    private Clip loop(String filePath, float volumeDb) {
        Clip clip = createClip(filePath);
        if (clip == null) return null;

        setVolume(clip, volumeDb);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
        return clip;
    }

    private Clip createClip(String filePath) {
        if (missingOrBrokenSounds.contains(filePath)) {
            System.out.println("Pomijanie wcześniej brakującego pliku: " + filePath);
            return null;
        }

        try {
            File audioFile = new File(filePath);
            
            if (!audioFile.exists()) {
                System.out.println("Plik nie istnieje: " + audioFile.getAbsolutePath());
                missingOrBrokenSounds.add(filePath);
                return null;
            }
            
            System.out.println("Ładowanie dźwięku: " + audioFile.getAbsolutePath());
            
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                System.out.println("Załadowano pomyślnie: " + filePath);
                return clip;
            }
            
        } catch (UnsupportedAudioFileException e) {
            System.out.println("Niewspierany format pliku " + filePath + ": " + e.getMessage());
            missingOrBrokenSounds.add(filePath);
            return null;
        } catch (IOException e) {
            System.out.println("Błąd IO przy odczycie pliku " + filePath + ": " + e.getMessage());
            missingOrBrokenSounds.add(filePath);
            return null;
        } catch (LineUnavailableException e) {
            System.out.println("Linia audio niedostępna: " + e.getMessage());
            missingOrBrokenSounds.add(filePath);
            return null;
        } catch (Exception e) {
            System.out.println("Nieoczekiwany błąd przy ładowaniu " + filePath + ": " + e.getMessage());
            missingOrBrokenSounds.add(filePath);
            return null;
        }
    }

    private void stopClip(Clip clip) {
        if (clip == null) return;

        try {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.flush();
            clip.close();
        } catch (Exception e) {
            System.out.println("Błąd przy zatrzymywaniu clipu: " + e.getMessage());
        }
    }

    private boolean isRunning(Clip clip) {
        return clip != null && clip.isRunning();
    }

    private void setVolume(Clip clip, float volumeDb) {
        if (clip == null) return;
        
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            System.out.println("Kontrola głośności niedostępna dla tego clipu");
            return;
        }

        try {
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gain.getMinimum();
            float max = gain.getMaximum();
            float safeVolume = Math.max(min, Math.min(max, volumeDb));
            gain.setValue(safeVolume);
            System.out.println("Ustawiono głośność na: " + safeVolume + " dB");
        } catch (Exception e) {
            System.out.println("Błąd przy ustawianiu głośności: " + e.getMessage());
        }
    }
}