package com.raycasting;

import java.util.List;

public class SpriteConfigurator {

    public SpriteConfigurator(SpriteManager manager) {
    }

    public SpriteEntity createGuard() {
        // NPCowie - STRAŻNICY (Guard.png: 7x8 grid)
        // ===========================================
        // Wiersze animacji: 
        //   0=walk_toward, 1=walk_toward_left, 2=walk_left, 3=walk_back_left,
        //   4=walk_back, 5=walk_back_right, 6=walk_right, 7=walk_right_toward
        // Specjalne: kolumna 5=death(5fr), kolumna 6=attack(3fr)
        // 
        // DODAJ NOWEGO STRAŻNIKA:
        //   1. Tworzymy nowy plik Guardian.png (taka sama struktura jak Guard.png)
        //   2. Duplikujemy tę metodę createGuard() -> createGuardian()
        //   3. W GameWindow zmieniamy sprite'y: loadSprites("Guardian.png")
        //   4. Zmieniamy punkt startowy i trasę patrolu (poniżej)
        //
        // PUNKT STARTOWY: w GameWindow.java, linia z addMovingSprite()
        //   raycaster.addMovingSprite(x, y, spriteConfig.createGuard());
        //
        // TRASA PATROLU: w MovingSprite.java, setPatrolPoints() metoda
        //   patrolPoints = {{x1,y1}, {x2,y2}, {x3,y3}}  <- wyznaczaj ścieżkę
        //   STOP_DISTANCE = 1.5  <- dystans do ataku (można zmienić)
        //
        SpriteEntity guard = new SpriteEntity();

        guard.addAnimation("walk_toward",
            new SpriteAnimation(List.of(
                new int[]{0,0}, new int[]{1,0}, new int[]{2,0}, new int[]{3,0}, new int[]{4,0}
            ), 120));

        guard.addAnimation("walk_toward_left",
            new SpriteAnimation(List.of(
                new int[]{0,1}, new int[]{1,1}, new int[]{2,1}, new int[]{3,1}, new int[]{4,1}
            ), 120));

        guard.addAnimation("walk_left",
            new SpriteAnimation(List.of(
                new int[]{0,2}, new int[]{1,2}, new int[]{2,2}, new int[]{3,2}, new int[]{4,2}
            ), 120));

        guard.addAnimation("walk_back_left",
            new SpriteAnimation(List.of(
                new int[]{0,3}, new int[]{1,3}, new int[]{2,3}, new int[]{3,3}, new int[]{4,3}
            ), 120));

        guard.addAnimation("walk_back",
            new SpriteAnimation(List.of(
                new int[]{0,4}, new int[]{1,4}, new int[]{2,4}, new int[]{3,4}, new int[]{4,4}
            ), 120));

        guard.addAnimation("walk_back_right",
            new SpriteAnimation(List.of(
                new int[]{0,5}, new int[]{1,5}, new int[]{2,5}, new int[]{3,5}, new int[]{4,5}
            ), 120));

        guard.addAnimation("walk_right",
            new SpriteAnimation(List.of(
                new int[]{0,6}, new int[]{1,6}, new int[]{2,6}, new int[]{3,6}, new int[]{4,6}
            ), 120));

        guard.addAnimation("walk_right_toward",
            new SpriteAnimation(List.of(
                new int[]{0,7}, new int[]{1,7}, new int[]{2,7}, new int[]{3,7}, new int[]{4,7}
            ), 120));

        guard.addAnimation("attack",
            new SpriteAnimation(List.of(
                new int[]{6,0}, new int[]{6,1}, new int[]{6,2}
            ), 80));

        guard.addAnimation("death",
            new SpriteAnimation(List.of(
                new int[]{5,0}, new int[]{5,1}, new int[]{5,2}, new int[]{5,3}, new int[]{5,4}
            ), 150));

        guard.addAnimation("idle",
            new SpriteAnimation(List.of(new int[]{0,0}), 500));

        guard.playAnimation("idle");
        return guard;
    }

    public SpriteEntity createWeaponPistol() {
        SpriteEntity pistol = new SpriteEntity();
        
        // BROŃ - PISTOLET (pistol.png: 326x62)
        // =====================================
        // Klatki: (0,0), (1,0), (2,0), (3,0), (4,0) - każda 62x62
        // Aby zmienić na inną broń: zmień pistol.png na knife.png, shotgun.png itd
        
        pistol.addAnimation("idle", new SpriteAnimation(List.of(new int[]{0,0}), 0));
        
        // Animacja strzału - przechodzi przez wszystkie klatki
        // (frameDuration: 30ms = gładka, szybka animacja bez migania)
        pistol.addAnimation("shoot", new SpriteAnimation(List.of(
            new int[]{0,0}, new int[]{1,0}, new int[]{2,0}, new int[]{3,0}, new int[]{4,0}
        ), 30));
        
        pistol.playAnimation("idle");
        return pistol;
    }

    public SpriteEntity createItemHealth() {
        SpriteEntity health = new SpriteEntity();
        health.addAnimation("idle", new SpriteAnimation(List.of(new int[]{7,0}), 0));
        health.addAnimation("picked", new SpriteAnimation(List.of(new int[]{7,1}), 100));
        return health;
    }
}