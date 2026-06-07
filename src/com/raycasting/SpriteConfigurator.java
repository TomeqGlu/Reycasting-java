package com.raycasting;

import java.util.List;

public class SpriteConfigurator {

    public SpriteConfigurator(SpriteManager manager) {
    }

    public SpriteEntity createGuard() {
        SpriteEntity guard = new SpriteEntity();

        guard.addAnimation("walk_toward",
            new SpriteAnimation(List.of(
                new int[]{0,0}, new int[]{1,0}, new int[]{2,0}, new int[]{3,0}, new int[]{4,0}
            ), 120, true));

        guard.addAnimation("walk_toward_left",
            new SpriteAnimation(List.of(
                new int[]{0,1}, new int[]{1,1}, new int[]{2,1}, new int[]{3,1}, new int[]{4,1}
            ), 120, true));

        guard.addAnimation("walk_left",
            new SpriteAnimation(List.of(
                new int[]{0,2}, new int[]{1,2}, new int[]{2,2}, new int[]{3,2}, new int[]{4,2}
            ), 120, true));

        guard.addAnimation("walk_back_left",
            new SpriteAnimation(List.of(
                new int[]{0,3}, new int[]{1,3}, new int[]{2,3}, new int[]{3,3}, new int[]{4,3}
            ), 120, true));

        guard.addAnimation("walk_back",
            new SpriteAnimation(List.of(
                new int[]{0,4}, new int[]{1,4}, new int[]{2,4}, new int[]{3,4}, new int[]{4,4}
            ), 120, true));

        guard.addAnimation("walk_back_right",
            new SpriteAnimation(List.of(
                new int[]{0,5}, new int[]{1,5}, new int[]{2,5}, new int[]{3,5}, new int[]{4,5}
            ), 120, true));

        guard.addAnimation("walk_right",
            new SpriteAnimation(List.of(
                new int[]{0,6}, new int[]{1,6}, new int[]{2,6}, new int[]{3,6}, new int[]{4,6}
            ), 120, true));

        guard.addAnimation("walk_right_toward",
            new SpriteAnimation(List.of(
                new int[]{0,7}, new int[]{1,7}, new int[]{2,7}, new int[]{3,7}, new int[]{4,7}
            ), 120, true));

        guard.addAnimation("attack",
            new SpriteAnimation(List.of(
                new int[]{6,0}, new int[]{6,1}, new int[]{6,2}
            ), 80));

        guard.addAnimation("death",
            new SpriteAnimation(List.of(
                new int[]{5,0}, new int[]{5,1}, new int[]{5,2}, new int[]{5,3}, new int[]{5,4}
            ), 150));

        guard.addAnimation("idle",
            new SpriteAnimation(List.of(new int[]{0,0}), 500, true));

        guard.playAnimation("idle");
        return guard;
    }



    public SpriteEntity createGuardDog() {
        SpriteEntity dog = new SpriteEntity();

        // GuardDog.png:
        // - animacje ruchu są w tych samych kolumnach co Guard/Guardian,
        //   ale mają 4 klatki zamiast 5: wiersze 0..3 w danej kolumnie,
        // - śmierć: 5. wiersz arkusza, czyli indeks row=4, klatki w kolumnach 0..3,
        // - atak: 6. wiersz arkusza, czyli indeks row=5, klatki w kolumnach 0..2.
        dog.addAnimation("walk_toward",
            new SpriteAnimation(List.of(
                new int[]{0,0}, new int[]{1,0}, new int[]{2,0}, new int[]{3,0}
            ), 110, true));

        dog.addAnimation("walk_toward_left",
            new SpriteAnimation(List.of(
                new int[]{0,1}, new int[]{1,1}, new int[]{2,1}, new int[]{3,1}
            ), 110, true));

        dog.addAnimation("walk_left",
            new SpriteAnimation(List.of(
                new int[]{0,2}, new int[]{1,2}, new int[]{2,2}, new int[]{3,2}
            ), 110, true));

        dog.addAnimation("walk_back_left",
            new SpriteAnimation(List.of(
                new int[]{0,3}, new int[]{1,3}, new int[]{2,3}, new int[]{3,3}
            ), 110, true));

        dog.addAnimation("walk_back",
            new SpriteAnimation(List.of(
                new int[]{0,4}, new int[]{1,4}, new int[]{2,4}, new int[]{3,4}
            ), 110, true));

        dog.addAnimation("walk_back_right",
            new SpriteAnimation(List.of(
                new int[]{0,5}, new int[]{1,5}, new int[]{2,5}, new int[]{3,5}
            ), 110, true));

        dog.addAnimation("walk_right",
            new SpriteAnimation(List.of(
                new int[]{0,6}, new int[]{1,6}, new int[]{2,6}, new int[]{3,6}
            ), 110, true));

        dog.addAnimation("walk_right_toward",
            new SpriteAnimation(List.of(
                new int[]{0,7}, new int[]{1,7}, new int[]{2,7}, new int[]{3,7}
            ), 110, true));

        dog.addAnimation("death",
            new SpriteAnimation(List.of(
                new int[]{4,0}, new int[]{4,1}, new int[]{4,2}, new int[]{4,3}
            ), 130));

        dog.addAnimation("attack",
            new SpriteAnimation(List.of(
                new int[]{5,0}, new int[]{5,1}, new int[]{5,2}
            ), 85));

        dog.addAnimation("idle",
            new SpriteAnimation(List.of(new int[]{0,0}), 500, true));

        dog.playAnimation("idle");
        return dog;
    }


    public SpriteEntity createWeaponPistol() {
        SpriteEntity pistol = new SpriteEntity();

        pistol.addAnimation("idle", new SpriteAnimation(List.of(new int[]{0,0}), 0));

        // Broń ma jeden rząd i kilka kolumn, więc indeksy to {row, col}: {0,0}, {0,1}, ...
        pistol.addAnimation("shoot", new SpriteAnimation(List.of(
            new int[]{0,0}, new int[]{0,1}, new int[]{0,2}, new int[]{0,3}, new int[]{0,4}
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
