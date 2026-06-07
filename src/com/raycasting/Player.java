package com.raycasting;

public class Player {
    private double x, y; // Pozycja Playera
    private double angle; // Kierunek patrzenia (w radianach)
    private double fov;
    private double moveSpeed;
    private double rotationSpeed;
    private int hp = 100;
    private boolean isAlive = true;
    private long lastShotTime = 0;
    private static final long SHOOT_COOLDOWN = 300;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.angle = Math.PI / 4; // 45 stopni
        this.fov = Math.PI / 3; // 60 stopni
        this.moveSpeed = 0.05;
        this.rotationSpeed = 0.03;
    }

    // Gettery
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public double getFOV() {
        return fov;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public double getRotationSpeed() {
        return rotationSpeed;
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
        }
    }

    public void heal(int amount) {
        hp = Math.min(100, hp + amount);
    }

    public boolean canShoot() {
        return System.currentTimeMillis() - lastShotTime >= SHOOT_COOLDOWN;
    }

    public void recordShot() {
        lastShotTime = System.currentTimeMillis();
    }

    // Settery
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    // Ruch, (a mówili że trygonometria to tylko żeby matme zdać :D)
    public void moveForward() {
        x += Math.cos(angle) * moveSpeed;
        y += Math.sin(angle) * moveSpeed;
    }

    public void moveBackward() {
        x -= Math.cos(angle) * moveSpeed;
        y -= Math.sin(angle) * moveSpeed;
    }

    public void strafeLeft() {
        x += Math.cos(angle - Math.PI / 2) * moveSpeed;
        y += Math.sin(angle - Math.PI / 2) * moveSpeed;
    }

    public void strafeRight() {
        x += Math.cos(angle + Math.PI / 2) * moveSpeed;
        y += Math.sin(angle + Math.PI / 2) * moveSpeed;
    }

    public void rotateLeft() {
        angle -= rotationSpeed;
        normalizeAngle();
    }

    public void rotateRight() {
        angle += rotationSpeed;
        normalizeAngle();
    }

    public void rotateTo(double targetAngle) {
        angle = targetAngle;
        normalizeAngle();
    }

    public void rotateBy(double delta) {
        angle += delta;
        normalizeAngle();
    }

    private void normalizeAngle() {
        while (angle < 0)
            angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI)
            angle -= 2 * Math.PI;
    }

    @Override
    public String toString() {
        return String.format("Player(%.2f, %.2f) angle: %.2f°",
                x, y, Math.toDegrees(angle));
    }
}