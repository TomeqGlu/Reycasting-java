package com.raycasting;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        System.out.println("Uruchamianie Java Raycasting...");

        // EDT (Event Dispatch Thread) Swinga
        SwingUtilities.invokeLater(() -> {
            // Stwórznie okna
            JFrame frame = new JFrame("Java Raycasting - Tomasz Głuch 16744");

            GameWindow game = new GameWindow();

            // Konfiguracja okna
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null); // Wyśrodkuj
            frame.setVisible(true);

            // Listener do zamykania
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    game.stop();
                }
            });

            game.start();

            System.out.println("Gra uruchomiona pomyślnie!");
            System.out.println("Użyj SPACJI aby przełączać tryby.");
        });
    }
}