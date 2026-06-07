package com.raycasting;

import java.awt.image.BufferedImage;
import java.util.Random;

public class MovingSprite {
    public double x, y;
    public double targetX, targetY;
    public double speed = 0.025;
    public SpriteEntity entity;
    private String currentAnim = "idle";

    private double[][] patrolPoints;
    private int currentPatrolIndex = 0;

    private static final Random RANDOM = new Random();

    // HP and combat
    private int hp = 75;
    private boolean isAlive = true;
    private long lastShotTime = 0;
    private static final long SHOOT_COOLDOWN = 1500;
    private static final double ATTACK_RANGE = 8.0;
    private boolean isAttacking = false;
    private boolean attackDamageDealt = false;

    // true = strażnik strzela z dystansu, false = pies gryzie tylko z bliska.
    private boolean rangedAttacker = true;

    // Manager sprite'ów przypisany do konkretnego przeciwnika.
    // Dzięki temu w jednej grze mogą być jednocześnie Guard.png i GuardDog.png.
    private SpriteManager ownSpriteManager;

    // Dystans zatrzymania - można regulować poniżej
    public double STOP_DISTANCE = 1.5;

    public MovingSprite(double x, double y, SpriteEntity entity) {
        this(x, y, entity, null);
    }

    public MovingSprite(double x, double y, SpriteEntity entity, SpriteManager ownSpriteManager) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.entity = entity;
        this.ownSpriteManager = ownSpriteManager;
    }

    public void setPatrolRoute(double[][] points) {
        this.patrolPoints = points;
        if (points.length > 0) {
            this.targetX = points[0][0];
            this.targetY = points[0][1];
        }
    }

    public void setTarget(double targetX, double targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }

    private boolean canMoveTo(double newX, double newY, Map map) {
        double radius = 0.35;
        return !map.isWall((int) (newX - radius), (int) (newY - radius)) &&
               !map.isWall((int) (newX + radius), (int) (newY - radius)) &&
               !map.isWall((int) (newX - radius), (int) (newY + radius)) &&
               !map.isWall((int) (newX + radius), (int) (newY + radius));
    }

    private void updateAnimation(double dx, double dy) {
        if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
            playIfChanged("idle");
            return;
        }

        double angle = Math.atan2(dy, dx);
        double degAngle = Math.toDegrees(angle);

        String newAnim;
        if (degAngle >= -22.5 && degAngle < 22.5) {
            newAnim = "walk_right";
        } else if (degAngle >= 22.5 && degAngle < 67.5) {
            newAnim = "walk_right_toward";
        } else if (degAngle >= 67.5 && degAngle < 112.5) {
            newAnim = "walk_toward";
        } else if (degAngle >= 112.5 && degAngle < 157.5) {
            newAnim = "walk_toward_left";
        } else if (degAngle >= 157.5 || degAngle < -157.5) {
            newAnim = "walk_left";
        } else if (degAngle >= -157.5 && degAngle < -112.5) {
            newAnim = "walk_back_left";
        } else if (degAngle >= -112.5 && degAngle < -67.5) {
            newAnim = "walk_back";
        } else if (degAngle >= -67.5 && degAngle < -22.5) {
            newAnim = "walk_back_right";
        } else {
            newAnim = "walk_toward";
        }

        playIfChanged(newAnim);
    }

    private void updateAnimationRelativeToPlayer(double moveX, double moveY, Player player) {
        if (Math.abs(moveX) < 0.01 && Math.abs(moveY) < 0.01) {
            playIfChanged("idle");
            return;
        }

        double moveAngle = Math.atan2(moveY, moveX);
        double angleToPlayer = Math.atan2(player.getY() - y, player.getX() - x);
        double relative = normalizeAngleToPi(moveAngle - angleToPlayer);
        double deg = Math.toDegrees(relative);

        String newAnim;
        if (deg >= -22.5 && deg < 22.5) {
            newAnim = "walk_toward";
        } else if (deg >= 22.5 && deg < 67.5) {
            newAnim = "walk_toward_left";
        } else if (deg >= 67.5 && deg < 112.5) {
            newAnim = "walk_left";
        } else if (deg >= 112.5 && deg < 157.5) {
            newAnim = "walk_back_left";
        } else if (deg >= 157.5 || deg < -157.5) {
            newAnim = "walk_back";
        } else if (deg >= -157.5 && deg < -112.5) {
            newAnim = "walk_back_right";
        } else if (deg >= -112.5 && deg < -67.5) {
            newAnim = "walk_right";
        } else if (deg >= -67.5 && deg < -22.5) {
            newAnim = "walk_right_toward";
        } else {
            newAnim = "walk_toward";
        }

        playIfChanged(newAnim);
    }

    private double normalizeAngleToPi(double angle) {
        while (angle <= -Math.PI) angle += Math.PI * 2;
        while (angle > Math.PI) angle -= Math.PI * 2;
        return angle;
    }

    private void playIfChanged(String animationName) {
        if (!animationName.equals(currentAnim)) {
            entity.playAnimation(animationName);
            currentAnim = animationName;
        }
    }

    public void update(long now, Map map) {
        updateRandomWander(now, map, null);
    }

    public void updateRandomWander(long now, Map map, Player player) {
        entity.update(now);

        if (!isAlive) return;

        chooseRandomTargetIfNeeded(map);

        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 0.05) {
            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;

            double newX = x + moveX;
            double newY = y + moveY;

            boolean canMoveX = canMoveTo(newX, y, map);
            boolean canMoveY = canMoveTo(x, newY, map);

            if (canMoveX) x = newX;
            if (canMoveY) y = newY;

            if (!canMoveX || !canMoveY) {
                chooseRandomTarget(map);
            }

            if (player != null) {
                updateAnimationRelativeToPlayer(moveX, moveY, player);
            } else {
                updateAnimation(moveX, moveY);
            }
        } else {
            chooseRandomTarget(map);
            playIfChanged("idle");
        }
    }

    private void chooseRandomTargetIfNeeded(Map map) {
        double distance = Math.hypot(targetX - x, targetY - y);
        if (distance <= 0.05) {
            chooseRandomTarget(map);
        }
    }

    private void goToNextPatrolPointOrRandomTarget(Map map) {
        if (patrolPoints != null && patrolPoints.length > 0) {
            currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.length;
            targetX = patrolPoints[currentPatrolIndex][0];
            targetY = patrolPoints[currentPatrolIndex][1];
        } else {
            chooseRandomTarget(map);
        }
    }

    private void chooseRandomTarget(Map map) {
        int centerX = (int) x;
        int centerY = (int) y;
        int radius = 5;

        for (int attempts = 0; attempts < 40; attempts++) {
            int minX = Math.max(1, centerX - radius);
            int maxX = Math.min(map.getWidth() - 2, centerX + radius);
            int minY = Math.max(1, centerY - radius);
            int maxY = Math.min(map.getHeight() - 2, centerY + radius);

            int cellX = minX + RANDOM.nextInt(Math.max(1, maxX - minX + 1));
            int cellY = minY + RANDOM.nextInt(Math.max(1, maxY - minY + 1));

            if (!map.isWall(cellX, cellY) && Math.hypot(cellX + 0.5 - x, cellY + 0.5 - y) > 1.0) {
                targetX = cellX + 0.5;
                targetY = cellY + 0.5;
                return;
            }
        }

        targetX = x;
        targetY = y;
    }

    public void updateToChasePlayer(long now, Map map, Player player) {
        entity.update(now);

        if (!isAlive) return;

        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.hypot(dx, dy);

        if (distance > STOP_DISTANCE) {
            isAttacking = false;

            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;

            double newX = x + moveX;
            double newY = y + moveY;

            boolean canMoveX = canMoveTo(newX, y, map);
            boolean canMoveY = canMoveTo(x, newY, map);

            if (canMoveX) x = newX;
            if (canMoveY) y = newY;

            updateAnimationRelativeToPlayer(moveX, moveY, player);
        } else {
            startAttack();
        }
    }

    public BufferedImage getCurrentSprite(SpriteManager fallbackManager) {
        SpriteManager managerToUse = ownSpriteManager != null ? ownSpriteManager : fallbackManager;
        return entity.getCurrentSprite(managerToUse);
    }

    public void setOwnSpriteManager(SpriteManager ownSpriteManager) {
        this.ownSpriteManager = ownSpriteManager;
    }

    public boolean isRangedAttacker() {
        return rangedAttacker;
    }

    public void setRangedAttacker(boolean rangedAttacker) {
        this.rangedAttacker = rangedAttacker;
    }

    public int getHP() {
        return hp;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void takeDamage(int damage) {
        hp = Math.max(0, hp - damage);
        if (hp <= 0) {
            isAlive = false;
            entity.playAnimation("death");
            currentAnim = "death";
        }
    }

    public boolean canShoot() {
        return System.currentTimeMillis() - lastShotTime >= SHOOT_COOLDOWN;
    }

    public void recordShot() {
        lastShotTime = System.currentTimeMillis();
    }

    public double getDistanceTo(double px, double py) {
        return Math.hypot(x - px, y - py);
    }

    public boolean isInAttackRange(double px, double py) {
        return getDistanceTo(px, py) <= ATTACK_RANGE;
    }

    public void startAttack() {
        if (!isAttacking) {
            isAttacking = true;
            attackDamageDealt = false;
            entity.playAnimation("attack");
            currentAnim = "attack";
        }
    }

    public void restartAttackAnimation() {
        isAttacking = true;
        attackDamageDealt = false;
        entity.playAnimation("attack");
        currentAnim = "attack";
    }

    public void stopAttack() {
        isAttacking = false;
        attackDamageDealt = false;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    /**
     * Obrażenia od strażnika powinny wejść dopiero w momencie strzału,
     * czyli na 3. klatce animacji attack: indeks 2.
     */
    public boolean shouldDealAttackDamageNow() {
        return isAttacking
                && !attackDamageDealt
                && entity.isCurrentAnimation("attack")
                && entity.getCurrentFrameIndex() >= 2;
    }

    public void markAttackDamageDealt() {
        attackDamageDealt = true;
    }
}
