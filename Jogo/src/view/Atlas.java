package view;

import model.Player;

import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Toda a arte do jogo, já recortada e com tamanho em unidades do mundo (384x216).
// Usado pelo jogo Java (GameView) e pelo exportador da versão web (ferramentas/ExportarWeb).
final class Atlas {
    // img na resolução original; w, h e ax (âncora horizontal) em unidades do mundo.
    record Frame(BufferedImage img, double w, double h, double ax) { }

    final Map<String, List<Frame>> anims = new LinkedHashMap<>();
    final List<BufferedImage> sky;      // dia, pôr do sol, noite
    final BufferedImage ground;
    final double skyScale, groundScale, groundSurface; // unidades por pixel / linha onde se pisa

    Atlas() {
        // ---- ela: correndo / pulando / carrinho na mesma escala ----
        List<List<Arte.Frame>> ela = Arte.slice(Arte.load("ela.png"), 20);
        double k = Player.RUN_H / medianHeight(ela.get(0));
        put("herRun", ela.get(0), k, false, true);
        put("herJump", ela.get(1), k, false, true);
        put("herSlide", ela.get(2), k, false, false);
        BufferedImage stand = ela.get(3).get(0).img();
        BufferedImage herHead = stand.getSubimage(0, 0, stand.getWidth(), stand.getHeight() * 28 / 100);
        put("herIcon", List.of(new Arte.Frame(herHead, 0)), 14.0 / herHead.getHeight(), false, false);

        // ---- ele, virado pra esquerda esperando ela ----
        List<List<Arte.Frame>> ele = Arte.slice(Arte.load("ele.png"), 20);
        double kh = 62.0 / medianHeight(ele.get(0));
        put("himWave", ele.get(1), kh, true, true);
        put("himOpen", ele.get(2), kh, true, true);

        // ---- abraço do final ----
        List<List<Arte.Frame>> abraco = Arte.slice(Arte.load("abraco.png"), 6);
        put("hugJump", abraco.get(0).subList(3, 5), 62.0 / medianHeight(abraco.get(1)), false, false);
        put("hugSway", abraco.get(1), 62.0 / medianHeight(abraco.get(1)), false, false);
        put("hugCheek", abraco.get(2), 62.0 / medianHeight(abraco.get(2)), false, false);

        // ---- cutscene: ele de frente + retratos ----
        List<List<Arte.Frame>> frente = Arte.slice(Arte.load("ele-frente.png"), 20);
        double kf = 62.0 / medianHeight(frente.get(0));
        put("frontIdle", frente.get(0), kf, false, true);
        put("frontTalk", frente.get(1), kf, false, true);
        put("frontShy", frente.get(2).subList(0, 2), kf, false, true);
        put("frontHeart", frente.get(2).subList(2, 4), kf, false, true);
        List<Arte.Frame> faces = Arte.grid(Arte.load("ele-retrato.png"), 3, 2);
        put("portraits", faces, 66.0 / medianHeight(faces), false, false);
        BufferedImage face = faces.get(0).img();
        BufferedImage himHead = face.getSubimage(0, 0, face.getWidth(), face.getHeight() * 62 / 100);
        put("himIcon", List.of(new Arte.Frame(himHead, 0)), 14.0 / himHead.getHeight(), false, false);

        // ---- obstáculos ----
        List<List<Arte.Frame>> obs = Arte.slice(Arte.load("obstaculos.png"), 20);
        double kp = 46.0 / obs.get(0).get(1).img().getHeight();
        put("pineSmall", obs.get(0).subList(0, 1), kp, false, false);
        put("pineTall", obs.get(0).subList(1, 2), kp, false, false);
        put("pineDouble", obs.get(0).subList(2, 3), kp, false, false);
        put("bird", obs.get(1), 24.0 / obs.get(1).get(0).img().getWidth(), false, false);
        double kHeart = 13.0 / obs.get(2).get(0).img().getWidth();
        put("heart", obs.get(2).subList(0, 1), kHeart, false, false);
        put("lifeHeart", obs.get(2).subList(0, 1), kHeart * 17 / 13, false, false);

        // ---- cenário ----
        sky = Arte.bands(Arte.load("paisagem.png"));
        skyScale = (model.GameModel.GROUND_Y + 10.0) / sky.get(0).getHeight(); // mesma escala nas 3 faixas
        ground = Arte.trim(Arte.keyMagenta(Arte.load("chao.png")));
        groundSurface = surfaceRow(ground);
        groundScale = (model.GameModel.HEIGHT - model.GameModel.GROUND_Y + 2) / (ground.getHeight() - groundSurface);
    }

    List<Frame> get(String name) { return anims.get(name); }

    // k = unidades do mundo por pixel da imagem original.
    private void put(String name, List<Arte.Frame> frames, double k, boolean flip, boolean bodyAnchor) {
        anims.put(name, frames.stream().map(f -> {
            BufferedImage img = flip ? Arte.flip(f.img()) : f.img();
            double w = img.getWidth() * k, h = img.getHeight() * k;
            double ax = bodyAnchor ? f.anchorX() * k : w / 2;
            if (flip && bodyAnchor) ax = w - ax;
            return new Frame(img, w, h, ax);
        }).toList());
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
}
