package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

public class GameModel {
    // Resolução "pixel art" interna; a View amplia pra tela.
    public static final int WIDTH = 384, HEIGHT = 216, GROUND_Y = 184, PLAYER_X = 48;
    public static final int MENU_STORY = 0, MENU_FREE = 1, MENU_OPTIONS = 2, MENU_EXIT = 3, MENU_SIZE = 4;
    public static final int STORY_LENGTH = 20000; // ~75s de corrida até ele

    public static final int START_LIVES = 3;
    public static final int END_TICKS = 600; // ~10s de final (abraço + zoom) antes de voltar ao menu
    private static final int INVINCIBLE_TICKS = 90, HEART_POINTS = 50, BIRDS_FROM = 1500;
    private static final int TICKS_PER_CHAR = 2;
    // Pássaro baixo: passa por cima dela deslizando, mas acerta ela em pé. Alto: só acerta se pular.
    private static final int BIRD_LOW = 44, BIRD_HIGH = 68;
    private static final double START_SPEED = 3.2, MAX_SPEED = 7.5, STORY_MAX_SPEED = 5.6;
    private static final Preferences PREFS = Preferences.userNodeForPackage(GameModel.class);

    public enum Mode { STORY, FREE }

    private final Random random = new Random();
    private GameState state = GameState.MENU;
    private Mode mode = Mode.FREE;
    private int menuIndex, lives, hearts, invincible, shake, tick, endTick, lifeGained;
    private int highScore = PREFS.getInt("highScore", 0);
    private double distance, speed, nextSpawn;
    private Player player = new Player(PLAYER_X, GROUND_Y);
    private List<Obstacle> obstacles = new ArrayList<>();

    public void moveMenuSelection(int delta) {
        menuIndex = Math.floorMod(menuIndex + delta, MENU_SIZE);
    }

    public void start(Mode mode) {
        this.mode = mode;
        distance = 0;
        speed = START_SPEED;
        lives = START_LIVES;
        hearts = invincible = shake = endTick = lifeGained = 0;
        nextSpawn = 350;
        player = new Player(PLAYER_X, GROUND_Y);
        obstacles = new ArrayList<>();
        state = GameState.PLAYING;
    }

    // ---------- cutscene antes do modo história ----------
    private int introLine, introTick;

    public void startIntro() {
        mode = Mode.STORY;
        distance = 0;
        introLine = introTick = 0;
        state = GameState.INTRO;
    }

    // ENTER: termina de mostrar a fala; se já terminou, vai pra próxima.
    public void advanceIntro() {
        if (state != GameState.INTRO) return;
        if (isTyping()) {
            introTick = getFala().texto().length() * TICKS_PER_CHAR;
        } else if (++introLine < Falas.INTRO.size()) {
            introTick = 0;
        } else {
            start(Mode.STORY);
        }
    }

    public void skipIntro() { start(Mode.STORY); }
    public Falas.Fala getFala() { return Falas.INTRO.get(introLine); }
    public int getVisibleChars() { return Math.min(getFala().texto().length(), introTick / TICKS_PER_CHAR); }
    public boolean isTyping() { return getVisibleChars() < getFala().texto().length(); }

    public void openOptions() { state = GameState.OPTIONS; }
    public void closeOptions() { state = GameState.MENU; }
    public void restart() { start(mode); }
    public void pause() { state = GameState.PAUSED; }
    public void resume() { state = GameState.PLAYING; }

    public void backToMenu() {
        mode = Mode.FREE;
        obstacles = new ArrayList<>();
        player = new Player(PLAYER_X, GROUND_Y);
        state = GameState.MENU;
    }

    // Chamado ~60x por segundo pelo controller.
    public void tick(boolean jump, boolean down) {
        if (state == GameState.PAUSED) return;
        tick++;
        if (shake > 0) shake--;
        switch (state) {
            case MENU, OPTIONS -> {
                distance += 3; // cenário passando atrás do menu
                player.update(false, false);
            }
            case INTRO -> introTick++;
            case PLAYING -> update(jump, down);
            case STORY_END -> {
                endTick++;
                player.update(false, false);
                if (endTick >= END_TICKS) backToMenu(); // fim da animação do abraço
            }
            default -> { }
        }
    }

    private void update(boolean jump, boolean down) {
        speed = mode == Mode.STORY ? storySpeed() : Math.min(MAX_SPEED, START_SPEED + distance * 0.00008);
        distance += speed;
        player.update(jump, down);
        if (invincible > 0) invincible--;
        if (lifeGained > 0) lifeGained--;
        spawnObstacles();

        Iterator<Obstacle> it = obstacles.iterator();
        while (it.hasNext()) {
            Obstacle o = it.next();
            o.move(o.getType() == Obstacle.Type.BIRD ? speed + 0.8 : speed);
            if (o.isGone()) {
                it.remove();
            } else if (o.collidesWith(player)) {
                if (o.getType() == Obstacle.Type.HEART) {
                    hearts++;
                    it.remove();
                } else if (o.getType() == Obstacle.Type.LIFE) {
                    lives = Math.min(START_LIVES, lives + 1);
                    lifeGained = 60;
                    it.remove();
                } else if (invincible == 0) {
                    hit();
                }
            }
        }

        if (state == GameState.PLAYING && mode == Mode.STORY && distance >= STORY_LENGTH) {
            distance = STORY_LENGTH;
            state = GameState.STORY_END;
        }
    }

    private double storySpeed() {
        double s = START_SPEED + (STORY_MAX_SPEED - START_SPEED) * Math.min(1, distance / (STORY_LENGTH * 0.9));
        double remaining = STORY_LENGTH - distance;
        return Math.min(s, Math.max(1.2, remaining / 50)); // desacelera chegando nele
    }

    private void hit() {
        lives--;
        invincible = INVINCIBLE_TICKS;
        shake = 12;
        if (lives > 0) return;
        state = GameState.GAME_OVER;
        if (mode == Mode.FREE && getScore() > highScore) {
            highScore = getScore();
            PREFS.putInt("highScore", highScore);
        }
    }

    private void spawnObstacles() {
        if (distance < nextSpawn) return;
        if (mode == Mode.STORY && distance + WIDTH > STORY_LENGTH - 100) return; // reta final livre
        obstacles.add(randomObstacle());
        nextSpawn = distance + speed * (48 + random.nextInt(50));
    }

    private Obstacle randomObstacle() {
        double x = WIDTH + 4;
        int roll = random.nextInt(100);
        boolean lifeOnScreen = obstacles.stream().anyMatch(o -> o.getType() == Obstacle.Type.LIFE);
        if (lives < START_LIVES && !lifeOnScreen && roll < 18) { // vida extra só depois de perder uma
            return new Obstacle(Obstacle.Type.LIFE, x, GROUND_Y - 62 - random.nextInt(20));
        }
        if (roll < 12) return new Obstacle(Obstacle.Type.HEART, x, GROUND_Y - 62 - random.nextInt(20));
        if (distance > BIRDS_FROM && roll < 45) {
            // baixo (maioria) = tem que deslizar; alto = só não pular
            return new Obstacle(Obstacle.Type.BIRD, x, GROUND_Y - (random.nextInt(3) > 0 ? BIRD_LOW : BIRD_HIGH));
        }
        Obstacle.Type[] pines = {Obstacle.Type.PINE_SMALL, Obstacle.Type.PINE_TALL, Obstacle.Type.PINE_DOUBLE};
        return new Obstacle(pines[random.nextInt(pines.length)], x, GROUND_Y);
    }

    private static double ramp(double v, double from, double to) {
        return Math.max(0, Math.min(1, (v - from) / (to - from)));
    }

    // 0 = dia, 1 = pôr do sol, 2 = noite (valores entre = transição)
    public double getSkyPhase() {
        if (mode == Mode.STORY) {
            double p = distance / STORY_LENGTH;
            return ramp(p, 0.30, 0.40) + ramp(p, 0.62, 0.72);
        }
        double s = (distance / 10) % 1500;
        return ramp(s, 400, 500) + ramp(s, 900, 1000) - 2 * ramp(s, 1400, 1500);
    }

    // Posição na tela dele (fim do modo história).
    public int getBoyfriendX() {
        return (int) (STORY_LENGTH + PLAYER_X + Player.RUN_W + 2 - distance);
    }

    public GameState getState() { return state; }
    public Mode getMode() { return mode; }
    public int getMenuIndex() { return menuIndex; }
    public Player getPlayer() { return player; }
    public List<Obstacle> getObstacles() { return Collections.unmodifiableList(obstacles); }
    public double getDistance() { return distance; }
    public double getStoryProgress() { return Math.min(1, distance / STORY_LENGTH); }
    public int getLives() { return lives; }
    public int getHearts() { return hearts; }
    public int getScore() { return (int) (distance / 10) + hearts * HEART_POINTS; }
    public int getHighScore() { return highScore; }
    public boolean isBlinking() { return invincible > 0 && (invincible / 5) % 2 == 0; }
    public int getShake() { return shake; }
    public int getLifeGained() { return lifeGained; }
    public int getTick() { return tick; }
    public int getEndTick() { return endTick; }
    public double getRemaining() { return STORY_LENGTH - distance; }
}
