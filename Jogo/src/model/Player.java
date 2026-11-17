package model;

public class Player extends GameObject {
    public static final int RUN_W = 22, RUN_H = 54, SLIDE_W = 56, SLIDE_H = 34;
    private static final double GRAVITY = 0.55, JUMP_SPEED = -9.5, SHORT_HOP = -4;

    private final double groundY;
    private double vy;
    private boolean onGround = true, sliding;

    public Player(double x, double groundY) {
        super(x, groundY - RUN_H, RUN_W, RUN_H);
        this.groundY = groundY;
    }

    // jump/down = teclas seguradas neste frame
    void update(boolean jump, boolean down) {
        if (onGround && jump) {
            vy = JUMP_SPEED;
            onGround = false;
        }
        sliding = onGround && down;
        width = sliding ? SLIDE_W : RUN_W;
        height = sliding ? SLIDE_H : RUN_H;
        if (onGround) {
            y = groundY - height;
            return;
        }
        if (!jump && vy < SHORT_HOP) vy = SHORT_HOP; // soltou cedo = pulo curto
        vy += down ? GRAVITY * 3 : GRAVITY;          // baixo no ar = desce rápido
        y += vy;
        if (y + height >= groundY) {
            y = groundY - height;
            vy = 0;
            onGround = true;
        }
    }

    public boolean isSliding() { return sliding; }
    public double getVy() { return vy; }
    public boolean isOnGround() { return onGround; }
}
