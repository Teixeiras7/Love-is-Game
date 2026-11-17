package model;

public class Obstacle extends GameObject {
    // Tamanhos = hitbox (um pouco menor que o desenho, pra colisão ser justa).
    public enum Type {
        PINE_SMALL(11, 28), PINE_TALL(17, 44), PINE_DOUBLE(26, 39), BIRD(18, 10), HEART(11, 9), LIFE(12, 10);

        public final int w, h;

        Type(int w, int h) {
            this.w = w;
            this.h = h;
        }
    }

    private final Type type;

    Obstacle(Type type, double x, double bottomY) {
        super(x, bottomY - type.h, type.w, type.h);
        this.type = type;
    }

    void move(double dx) { x -= dx; }
    boolean isGone() { return x + width < -30; }
    public Type getType() { return type; }
}
