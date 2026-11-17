package view;

import model.Falas;
import model.GameModel;
import model.GameState;
import model.Obstacle;
import model.Player;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class GameView extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int W = GameModel.WIDTH, H = GameModel.HEIGHT, GROUND = GameModel.GROUND_Y;

    private static final Color PINK = new Color(255, 110, 170), YELLOW = new Color(255, 225, 90);
    private static final Color SHADOW = new Color(30, 15, 40, 220), PANEL = new Color(25, 15, 45, 215);
    private static final Color LABEL = new Color(205, 195, 225);
    private static final Font TITLE = new Font(Font.DIALOG, Font.BOLD, 28);
    private static final Font BIG = new Font(Font.DIALOG, Font.BOLD, 14);
    private static final Font TALK = new Font(Font.DIALOG, Font.BOLD, 11);
    private static final Font SMALL = new Font(Font.DIALOG, Font.BOLD, 10);

    // Tinta do chão no pôr do sol e à noite (o céu já vem pronto na paisagem).
    private static final Color[] GROUND_TINT = {null, new Color(255, 110, 60, 70), new Color(14, 18, 64, 150)};

    // Sprite já reduzido para pixels de tela; w, h e ax (âncora horizontal) em unidades do jogo.
    private record Sprite(BufferedImage img, double w, double h, double ax) { }

    private final GameModel model;
    private final int scale;
    private int tick;

    private final Sprite[] herRun, herJump, herSlide, himWave, himOpen, hugJump, hugSway, hugCheek;
    private final Sprite[] frontIdle, frontTalk, frontShy, frontHeart, portraits, bird;
    private final Sprite pineSmall, pineTall, pineDouble, heart, lifeHeart, herIcon, himIcon;
    private final List<BufferedImage> sky;
    private final BufferedImage[] ground = new BufferedImage[3];
    private final double groundScale, groundSurface;

    public GameView(GameModel model) {
        this.model = model;
        int screenH = Toolkit.getDefaultToolkit().getScreenSize().height;
        scale = Math.max(2, Math.min(4, (screenH - 120) / H));
        setPreferredSize(new Dimension(W * scale, H * scale));
        setFocusable(true);

        // ---- ela: correndo / pulando / carrinho na mesma escala ----
        List<List<Arte.Frame>> ela = Arte.slice(Arte.load("ela.png"), 20);
        double k = Player.RUN_H / medianHeight(ela.get(0));
        herRun = sprites(ela.get(0), k, false, true);
        herJump = sprites(ela.get(1), k, false, true);
        herSlide = sprites(ela.get(2), k, false, false);
        BufferedImage stand = ela.get(3).get(0).img();
        BufferedImage herHead = stand.getSubimage(0, 0, stand.getWidth(), stand.getHeight() * 28 / 100);
        herIcon = sprite(new Arte.Frame(herHead, 0), 14.0 / herHead.getHeight(), false, false);

        // ---- ele (virado pra esquerda, esperando ela) ----
        List<List<Arte.Frame>> ele = Arte.slice(Arte.load("ele.png"), 20);
        double kh = 62.0 / medianHeight(ele.get(0));
        himWave = sprites(ele.get(1), kh, true, true);
        himOpen = sprites(ele.get(2), kh, true, true);

        // ---- abraço do final ----
        List<List<Arte.Frame>> abraco = Arte.slice(Arte.load("abraco.png"), 6);
        hugJump = sprites(abraco.get(0).subList(3, 5), 62.0 / medianHeight(abraco.get(1)), false, false);
        hugSway = sprites(abraco.get(1), 62.0 / medianHeight(abraco.get(1)), false, false);
        hugCheek = sprites(abraco.get(2), 62.0 / medianHeight(abraco.get(2)), false, false);

        // ---- cutscene: ele de frente + retratos ----
        List<List<Arte.Frame>> frente = Arte.slice(Arte.load("ele-frente.png"), 20);
        double kf = 62.0 / medianHeight(frente.get(0));
        frontIdle = sprites(frente.get(0), kf, false, true);
        frontTalk = sprites(frente.get(1), kf, false, true);
        frontShy = sprites(frente.get(2).subList(0, 2), kf, false, true);
        frontHeart = sprites(frente.get(2).subList(2, 4), kf, false, true);
        List<Arte.Frame> faces = Arte.grid(Arte.load("ele-retrato.png"), 3, 2);
        portraits = sprites(faces, 66.0 / medianHeight(faces), false, false);
        BufferedImage face = faces.get(0).img();
        BufferedImage himHead = face.getSubimage(0, 0, face.getWidth(), face.getHeight() * 62 / 100);
        himIcon = sprite(new Arte.Frame(himHead, 0), 14.0 / himHead.getHeight(), false, false);

        // ---- obstáculos ----
        List<List<Arte.Frame>> obs = Arte.slice(Arte.load("obstaculos.png"), 20);
        double kp = 46.0 / obs.get(0).get(1).img().getHeight();
        pineSmall = sprite(obs.get(0).get(0), kp, false, false);
        pineTall = sprite(obs.get(0).get(1), kp, false, false);
        pineDouble = sprite(obs.get(0).get(2), kp, false, false);
        bird = sprites(obs.get(1), 24.0 / obs.get(1).get(0).img().getWidth(), false, false);
        heart = sprite(obs.get(2).get(0), 13.0 / obs.get(2).get(0).img().getWidth(), false, false);
        lifeHeart = sprite(obs.get(2).get(0), 17.0 / obs.get(2).get(0).img().getWidth(), false, false);

        // ---- cenário ----
        sky = Arte.bands(Arte.load("paisagem.png"));
        BufferedImage chao = Arte.trim(Arte.keyMagenta(Arte.load("chao.png")));
        groundSurface = surfaceRow(chao);
        groundScale = (H - GROUND + 2) / (chao.getHeight() - groundSurface);
        for (int i = 0; i < 3; i++) ground[i] = tint(chao, GROUND_TINT[i]);
    }

    public Image icon() { return herIcon.img(); }
    int scale() { return scale; }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.scale(scale, scale);
        render(g);
        g.dispose();
    }

    void render(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        tick = model.getTick();
        g.setColor(Color.BLACK);
        g.fillRect(-4, -4, W + 8, H + 8);
        if (model.getShake() > 0) g.translate(tick % 3 - 1, (tick / 2) % 3 - 1);

        double phase = model.getSkyPhase();
        drawSky(g, phase);
        drawGround(g, phase);

        GameState state = model.getState();
        if (state == GameState.INTRO) {
            drawIntro(g);
            return;
        }
        drawObstacles(g);
        if (state == GameState.STORY_END) {
            drawHug(g);
        } else {
            drawBoyfriend(g);
            drawPlayer(g);
        }

        switch (state) {
            case MENU -> drawMenu(g);
            case OPTIONS -> drawOptions(g);
            case PLAYING -> drawHud(g);
            case PAUSED -> {
                drawHud(g);
                drawPause(g);
            }
            case GAME_OVER -> {
                drawHud(g);
                drawGameOver(g);
            }
            case STORY_END -> drawStoryEnd(g);
            default -> { }
        }
    }

    // ---------------- cenário ----------------

    // Dia / pôr do sol / noite: troca suave misturando as duas faixas vizinhas.
    private void drawSky(Graphics2D g, double phase) {
        int i = Math.min(1, (int) phase);
        double f = phase - i;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double k = (GROUND + 10.0) / sky.get(0).getHeight(); // mesma escala nas 3 faixas = alinhadas na mistura
        drawStrip(g, sky.get(i), -2, k, 0.25, 1f);
        if (f > 0) drawStrip(g, sky.get(i + 1), -2, k, 0.25, (float) f);
    }

    private void drawGround(Graphics2D g, double phase) {
        int i = Math.min(1, (int) phase);
        double f = phase - i;
        double top = GROUND - groundSurface * groundScale;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        drawStrip(g, ground[i], top, groundScale, 1.0, 1f);
        if (f > 0) drawStrip(g, ground[i + 1], top, groundScale, 1.0, (float) f);
    }

    // Faixa repetida em espelho (emenda sem costura), rolando com a corrida. k = unidades por pixel.
    private void drawStrip(Graphics2D g, BufferedImage img, double top, double k, double parallax, float alpha) {
        double w = img.getWidth() * k;
        double x0 = -((model.getDistance() * parallax) % (2 * w));
        Composite old = g.getComposite();
        if (alpha < 1) g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        for (int t = 0; x0 + t * w < W; t++) {
            double x = x0 + t * w;
            if (x + w < 0) continue;
            boolean flip = t % 2 == 1;
            AffineTransform at = new AffineTransform();
            at.translate(flip ? x + w + 0.3 : x - 0.3, top); // 0.3 = sobreposição pra não aparecer fresta
            at.scale((flip ? -1 : 1) * (k + 0.6 / img.getWidth()), k);
            g.drawImage(img, at, null);
        }
        g.setComposite(old);
    }

    // ---------------- personagens e obstáculos ----------------

    private void drawObstacles(Graphics2D g) {
        for (Obstacle o : model.getObstacles()) {
            switch (o.getType()) {
                case PINE_SMALL -> draw(g, pineSmall, o.centerX(), GROUND + 1);
                case PINE_TALL -> draw(g, pineTall, o.centerX(), GROUND + 1);
                case PINE_DOUBLE -> draw(g, pineDouble, o.centerX(), GROUND + 1);
                case BIRD -> {
                    Sprite s = bird[(tick / 6) % bird.length];
                    draw(g, s, o.centerX(), o.getY() + o.getHeight() / 2.0 + s.h() / 2);
                }
                case HEART -> {
                    double bob = Math.sin(tick * 0.12 + o.getX() * 0.05) * 2;
                    draw(g, heart, o.centerX(), o.bottom() + 2 + bob);
                }
                case LIFE -> drawLifeHeart(g, o.centerX(), o.getY() + o.getHeight() / 2.0 + Math.sin(tick * 0.15) * 3);
            }
        }
    }

    // Coração que devolve vida: brilho dourado pulsando + estrelinhas girando.
    private void drawLifeHeart(Graphics2D g, double cx, double cy) {
        float r = 13 + (float) Math.sin(tick * 0.2) * 2;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), r,
            new float[] {0f, 1f}, new Color[] {new Color(255, 236, 140, 200), new Color(255, 200, 80, 0)}));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        draw(g, lifeHeart, cx, cy + lifeHeart.h() / 2);
        g.setColor(new Color(255, 250, 210));
        for (int i = 0; i < 4; i++) {
            double ang = tick * 0.06 + i * Math.PI / 2, sx = cx + Math.cos(ang) * 13, sy = cy + Math.sin(ang) * 11;
            double s = 1.2 + 0.8 * Math.abs(Math.sin(tick * 0.2 + i));
            g.fill(new Rectangle2D.Double(sx - s, sy - 0.4, s * 2, 0.8));
            g.fill(new Rectangle2D.Double(sx - 0.4, sy - s, 0.8, s * 2));
        }
    }

    private void drawPlayer(Graphics2D g) {
        Player p = model.getPlayer();
        if (model.isBlinking()) return;
        Sprite s;
        if (p.isSliding()) s = herSlide[(tick / 8) % herSlide.length];
        else if (!p.isOnGround()) s = herJump[p.getVy() < -2.5 ? 0 : p.getVy() > 2.5 ? 2 : 1];
        else s = herRun[(tick / 4) % herRun.length];

        if (p.isOnGround()) { // poeirinha nos pés
            int n = p.isSliding() ? 6 : 2;
            double fx = p.isSliding() ? p.getX() + p.getWidth() - 4 : p.getX() + 2;
            for (int i = 0; i < n; i++) {
                int age = (tick + i * 5) % 16;
                g.setColor(new Color(240, 228, 200, Math.max(0, 200 - age * 12)));
                double r = 1.5 + age * 0.15;
                g.fill(new Ellipse2D.Double(fx - age * 1.2, GROUND - 2 - age / 4.0, r * 2, r * 2));
            }
        }
        draw(g, s, p.centerX(), p.bottom() + 1);
    }

    // Ele esperando no fim do modo história: acenando, e abre os braços quando ela chega perto.
    private void drawBoyfriend(Graphics2D g) {
        if (model.getMode() != GameModel.Mode.STORY || model.getState() == GameState.MENU) return;
        double cx = model.getBoyfriendX() + Player.RUN_W / 2.0;
        if (cx > W + 40) return;
        double rem = model.getRemaining();
        Sprite s = rem < 180
            ? himOpen[Math.min(himOpen.length - 1, (int) ((180 - rem) / 40))]
            : himWave[(tick / 8) % himWave.length];
        draw(g, s, cx, GROUND + 1);
    }

    private void drawHug(Graphics2D g) {
        int e = model.getEndTick();
        Sprite s = e < 16 ? hugJump[e / 8] : e < 160 ? hugSway[((e - 16) / 10) % hugSway.length]
            : hugCheek[((e - 160) / 24) % hugCheek.length];
        double cx = (model.getPlayer().centerX() + model.getBoyfriendX() + Player.RUN_W / 2.0) / 2;
        draw(g, s, cx, GROUND + 1);
    }

    // ---------------- cutscene ----------------

    private void drawIntro(Graphics2D g) {
        Falas.Fala fala = model.getFala();
        boolean typing = model.isTyping();
        Sprite body = switch (fala.rosto()) {
            case APAIXONADO -> frontHeart[(tick / 20) % frontHeart.length];
            case TIMIDO -> frontShy[(tick / 20) % frontShy.length];
            default -> typing ? frontTalk[(tick / 8) % frontTalk.length] : frontIdle[tick % 160 < 8 ? 1 : 0];
        };
        draw(g, body, 250, GROUND + 1);

        int bx = 12, by = 8, bw = W - 24, bh = 98;
        g.setColor(PANEL);
        g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setColor(PINK);
        g.drawRoundRect(bx, by, bw, bh, 10, 10);

        int face = switch (fala.rosto()) {
            case SORRINDO, TIMIDO -> 0;
            case PISCANDO -> 2;
            case RINDO -> 3;
            case APAIXONADO -> 5;
        };
        if (typing && face == 0 && (tick / 5) % 2 == 0) face = 1; // boca mexendo enquanto fala
        Sprite portrait = portraits[face];
        draw(g, portrait, bx + 6 + portrait.w() / 2, by + bh - 4);

        int tx = bx + (int) portrait.w() + 14;
        text(g, "Seu amor", tx, by + 18, SMALL, PINK);
        FontMetrics fm = g.getFontMetrics(TALK);
        int left = model.getVisibleChars(), y = by + 34;
        for (String line : wrap(fala.texto(), fm, bx + bw - tx - 10)) {
            if (left <= 0) break;
            text(g, line.substring(0, Math.min(line.length(), left)), tx, y, TALK, Color.WHITE);
            left -= line.length() + 1;
            y += 14;
        }
        if (!typing && (tick / 20) % 2 == 0) text(g, "ENTER  >", bx + bw - 52, by + bh - 7, SMALL, YELLOW);
        text(g, "ESC pular", 8, H - 6, SMALL, Color.WHITE);
    }

    private static List<String> wrap(String s, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(test) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        lines.add(line.toString());
        return lines;
    }

    // ---------------- telas ----------------

    private void drawHud(Graphics2D g) {
        for (int i = 0; i < GameModel.START_LIVES; i++) {
            drawFaded(g, heart, 12 + i * 16, 19, i < model.getLives() ? 1f : 0.25f);
        }
        if (model.getLifeGained() > 0) {
            text(g, "+1 VIDA", 12 + GameModel.START_LIVES * 16, 16 - (60 - model.getLifeGained()) / 6, SMALL, PINK);
        }
        if (model.getMode() == GameModel.Mode.FREE) {
            String s = String.format("RECORDE %05d   %05d", model.getHighScore(), model.getScore());
            text(g, s, W - 6 - g.getFontMetrics(SMALL).stringWidth(s), 14, SMALL, Color.WHITE);
            return;
        }
        int bx = 130, bw = 124;
        g.setColor(SHADOW);
        g.fillRoundRect(bx, 8, bw, 5, 4, 4);
        g.setColor(PINK);
        g.fillRoundRect(bx, 8, (int) (bw * model.getStoryProgress()), 5, 4, 4);
        draw(g, himIcon, bx + bw + 4, 18);
        draw(g, herIcon, bx + bw * model.getStoryProgress(), 18);
        draw(g, heart, W - 36, 19);
        text(g, "x" + model.getHearts(), W - 26, 15, SMALL, Color.WHITE);
    }

    private void drawMenu(Graphics2D g) {
        textCentered(g, "LOVE IS GAME", 46, TITLE, PINK);
        textCentered(g, "~ corra até o seu amor ~", 62, SMALL, Color.WHITE);

        String[] items = new String[GameModel.MENU_SIZE];
        items[GameModel.MENU_STORY] = "MODO HISTÓRIA";
        items[GameModel.MENU_FREE] = "MODO LIVRE";
        items[GameModel.MENU_OPTIONS] = "OPÇÕES";
        items[GameModel.MENU_EXIT] = "SAIR";
        g.setColor(PANEL);
        g.fillRoundRect(W / 2 - 80, 74, 160, 82, 10, 10);
        g.setColor(PINK);
        g.drawRoundRect(W / 2 - 80, 74, 160, 82, 10, 10);
        for (int i = 0; i < items.length; i++) {
            boolean sel = i == model.getMenuIndex();
            int y = 92 + i * 18;
            textCentered(g, items[i], y, BIG, sel ? YELLOW : LABEL);
            if (sel && (tick / 15) % 2 == 0) draw(g, heart, W / 2 - 66, y + 1);
        }
        textCentered(g, "RECORDE  " + String.format("%05d", model.getHighScore()), 172, SMALL, YELLOW);
    }

    private void drawOptions(Graphics2D g) {
        g.setColor(PANEL);
        g.fillRoundRect(22, 12, W - 44, 192, 10, 10);
        g.setColor(PINK);
        g.drawRoundRect(22, 12, W - 44, 192, 10, 10);
        textCentered(g, "OPÇÕES", 32, BIG, PINK);

        int x = 36;
        text(g, "CONTROLES", x, 50, SMALL, YELLOW);
        text(g, "Pular:  CIMA  /  W  /  ESPAÇO", x, 63, SMALL, Color.WHITE);
        text(g, "Deslizar (carrinho):  BAIXO  /  S  /  SHIFT", x, 75, SMALL, Color.WHITE);
        text(g, "Pausar:  ESC  ou  P      (na pausa, Q volta ao menu)", x, 87, SMALL, Color.WHITE);
        text(g, "Dica: segure pular = pulo alto; baixo no ar = cai rápido", x, 99, SMALL, LABEL);

        text(g, "MODO HISTÓRIA", x, 117, SMALL, YELLOW);
        text(g, "Uma corrida com fim, do dia até a noite, até chegar", x, 129, SMALL, Color.WHITE);
        text(g, "em quem está te esperando. Você tem 3 vidas, e o", x, 141, SMALL, Color.WHITE);
        text(g, "coração brilhante devolve 1 vida perdida.", x, 151, SMALL, Color.WHITE);

        text(g, "MODO LIVRE", x, 166, SMALL, YELLOW);
        text(g, "Corrida infinita: quanto mais longe, mais pontos.", x, 177, SMALL, Color.WHITE);
        text(g, "Cada coração vale +50. Tente bater o recorde!", x, 187, SMALL, Color.WHITE);

        if ((tick / 20) % 2 == 0) textCentered(g, "ENTER ou ESC para voltar", 198, SMALL, PINK);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, W, H);
        textCentered(g, "PAUSADO", 100, TITLE, Color.WHITE);
        textCentered(g, "ESC continuar    Q menu", 120, SMALL, Color.WHITE);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(PANEL);
        g.fillRoundRect(W / 2 - 90, 60, 180, 86, 10, 10);
        g.setColor(PINK);
        g.drawRoundRect(W / 2 - 90, 60, 180, 86, 10, 10);
        textCentered(g, "FIM DE JOGO", 86, TITLE.deriveFont(22f), PINK);
        if (model.getMode() == GameModel.Mode.FREE) {
            textCentered(g, String.format("PONTOS %05d", model.getScore()), 106, BIG, Color.WHITE);
            textCentered(g, String.format("RECORDE %05d", model.getHighScore()), 120, SMALL, YELLOW);
        } else {
            textCentered(g, "Quase lá! Tenta de novo!", 108, BIG, Color.WHITE);
        }
        if ((tick / 20) % 2 == 0) textCentered(g, "ENTER recomeçar    ESC menu", 138, SMALL, Color.WHITE);
    }

    private void drawStoryEnd(Graphics2D g) {
        int end = model.getEndTick();
        double cx = (model.getPlayer().centerX() + model.getBoyfriendX() + Player.RUN_W / 2.0) / 2;
        for (int k = 0; k < 6; k++) { // coraçõezinhos subindo
            int t = end - 20 - k * 18;
            if (t < 0) continue;
            t %= 108;
            double x = cx + Math.sin((t + k * 30) * 0.08) * 10;
            drawFaded(g, heart, x, GROUND - 66 - t / 2.0, 1f - t / 108f);
        }
        if (end > 40) {
            textCentered(g, "VOCÊ CHEGOU ATÉ MIM!", 56, TITLE.deriveFont(22f), PINK);
            textCentered(g, "Corações coletados: " + model.getHearts(), 74, BIG, Color.WHITE);
        }
        if (end > 100 && (tick / 30) % 2 == 0) textCentered(g, "ENTER para voltar ao menu", 205, SMALL, Color.WHITE);
    }

    // ---------------- utilitários ----------------

    // Desenha com a âncora em (cx, bottom), com precisão de pixel de tela.
    private void draw(Graphics2D g, Sprite s, double cx, double bottom) {
        AffineTransform at = new AffineTransform();
        at.translate(Math.round((cx - s.ax()) * scale) / (double) scale, Math.round((bottom - s.h()) * scale) / (double) scale);
        at.scale(1.0 / scale, 1.0 / scale);
        g.drawImage(s.img(), at, null);
    }

    private void drawFaded(Graphics2D g, Sprite s, double cx, double bottom, float alpha) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        draw(g, s, cx, bottom);
        g.setComposite(old);
    }

    private Sprite[] sprites(List<Arte.Frame> frames, double k, boolean flip, boolean bodyAnchor) {
        Sprite[] out = new Sprite[frames.size()];
        for (int i = 0; i < out.length; i++) out[i] = sprite(frames.get(i), k, flip, bodyAnchor);
        return out;
    }

    // k = unidades do jogo por pixel da imagem original.
    private Sprite sprite(Arte.Frame f, double k, boolean flip, boolean bodyAnchor) {
        BufferedImage src = flip ? Arte.flip(f.img()) : f.img();
        double w = src.getWidth() * k, h = src.getHeight() * k;
        double ax = bodyAnchor ? f.anchorX() * k : w / 2;
        if (flip && bodyAnchor) ax = w - ax;
        return new Sprite(Arte.resize(src, (int) Math.round(w * scale), (int) Math.round(h * scale)), w, h, ax);
    }

    private static double medianHeight(List<Arte.Frame> frames) {
        int[] hs = frames.stream().mapToInt(f -> f.img().getHeight()).sorted().toArray();
        return hs[hs.length / 2];
    }

    // Primeira linha em que a grama já é contínua = onde os pés pisam.
    private static double surfaceRow(BufferedImage im) {
        for (int y = 0; y < im.getHeight(); y++) {
            int solid = 0;
            for (int x = 0; x < im.getWidth(); x++) if ((im.getRGB(x, y) >>> 24) != 0) solid++;
            if (solid > im.getWidth() * 0.95) return y;
        }
        return 0;
    }

    private static BufferedImage tint(BufferedImage src, Color c) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        if (c != null) {
            g.setComposite(AlphaComposite.SrcAtop); // pinta só onde já tem pixel
            g.setColor(c);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
        }
        g.dispose();
        return out;
    }

    private static void text(Graphics2D g, String s, int x, int y, Font f, Color c) {
        g.setFont(f);
        g.setColor(SHADOW);
        g.drawString(s, x + 1, y + 1);
        g.setColor(c);
        g.drawString(s, x, y);
    }

    private static void textCentered(Graphics2D g, String s, int y, Font f, Color c) {
        text(g, s, (W - g.getFontMetrics(f).stringWidth(s)) / 2, y, f, c);
    }
}
