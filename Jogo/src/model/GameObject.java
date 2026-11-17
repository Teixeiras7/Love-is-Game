package model;

import java.awt.geom.Rectangle2D;

public abstract class GameObject {
    protected double x, y, width, height;

    protected GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Hitbox um pouco menor que o desenho, pra colisão ficar justa.
    private Rectangle2D.Double hitbox() {
        return new Rectangle2D.Double(x + 2, y + 2, width - 4, height - 4);
    }

    public boolean collidesWith(GameObject other) {
        return hitbox().intersects(other.hitbox());
    }

    public double centerX() { return x + width / 2; }
    public double bottom() { return y + height; }
    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    public int getWidth() { return (int) width; }
    public int getHeight() { return (int) height; }
}
