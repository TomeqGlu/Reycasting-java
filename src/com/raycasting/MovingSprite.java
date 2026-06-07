package com.raycasting;

import java.awt.image.BufferedImage;

public class MovingSprite {
    public double x, y;
    public double targetX, targetY;
    public double speed = 0.025;
    public SpriteEntity entity;
    private String currentAnim = "idle";
    
    private double[][] patrolPoints;
    private int currentPatrolIndex = 0;
    
    // HP and combat
    private int hp = 50;
    private boolean isAlive = true;
    private long lastShotTime = 0;
    private static final long SHOOT_COOLDOWN = 1500;
    private static final double ATTACK_RANGE = 8.0;
    private boolean isAttacking = false;
    
    // Dystans zatrzymania - można regulować poniżej
    public double STOP_DISTANCE = 1.5;  // <-- TUTAJ możesz zmienić dystans (domyślnie 1.5)
    
    public MovingSprite(double x, double y, SpriteEntity entity) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.entity = entity;
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
        return !map.isWall((int)(newX - radius), (int)(newY - radius)) &&
               !map.isWall((int)(newX + radius), (int)(newY - radius)) &&
               !map.isWall((int)(newX - radius), (int)(newY + radius)) &&
               !map.isWall((int)(newX + radius), (int)(newY + radius));
    }
    
    private void updateAnimation(double dx, double dy) {
        if (Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01) {
            if (!currentAnim.equals("idle")) {
                entity.playAnimation("idle");
                currentAnim = "idle";
            }
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
            newAnim = "walk_left_toward";
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
        
        if (!newAnim.equals(currentAnim)) {
            entity.playAnimation(newAnim);
            currentAnim = newAnim;
        }
    }
    
    public void update(long now, Map map) {
        entity.update(now);
        
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
            
            // If blocked by wall, try next patrol point
            if (!canMoveX || !canMoveY) {
                if (patrolPoints != null && patrolPoints.length > 0) {
                    currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.length;
                    targetX = patrolPoints[currentPatrolIndex][0];
                    targetY = patrolPoints[currentPatrolIndex][1];
                }
            }
            
            updateAnimation(moveX, moveY);
        } else {
            if (patrolPoints != null && patrolPoints.length > 0) {
                currentPatrolIndex = (currentPatrolIndex + 1) % patrolPoints.length;
                targetX = patrolPoints[currentPatrolIndex][0];
                targetY = patrolPoints[currentPatrolIndex][1];
            }
        }
    }

    public void updateToChasePlayer(long now, Map map, Player player) {
        entity.update(now);

        if (!isAlive) return;

        double dx = player.getX() - x;
        double dy = player.getY() - y;
        double distance = Math.hypot(dx, dy);

        // Chase the player until STOP_DISTANCE
        if (distance > STOP_DISTANCE) {
            double moveX = (dx / distance) * speed;
            double moveY = (dy / distance) * speed;
            
            double newX = x + moveX;
            double newY = y + moveY;
            
            boolean canMoveX = canMoveTo(newX, y, map);
            boolean canMoveY = canMoveTo(x, newY, map);
            
            if (canMoveX) x = newX;
            if (canMoveY) y = newY;
            
            updateAnimation(moveX, moveY);
        } else {
            // Zatrzymaj się w bliskości i zacznij animację ataku
            if (!currentAnim.equals("attack")) {
                entity.playAnimation("attack");
                currentAnim = "attack";
            }
        }
    }
    
    public BufferedImage getCurrentSprite(SpriteManager manager) {
        return entity.getCurrentSprite(manager);
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
            entity.playAnimation("attack");
            currentAnim = "attack";
        }
    }

    public void stopAttack() {
        isAttacking = false;
    }

    public boolean isAttacking() {
        return isAttacking;
    }
}