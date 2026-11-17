package view;

import model.Falas;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

// Gera a arte e as falas da versão web (pasta web/arte) a partir das MESMAS imagens e falas do jogo Java.
// Rode pelo exportar-web.bat sempre que mudar a arte ou o Falas.java.
public class ExportarWeb {
    private static final double MAX_PX_PER_UNIT = 6; // resolução suficiente até em celular com tela grande
    private static final double GROUND_PX_PER_UNIT = 4.5;

    public static void main(String[] args) throws Exception {
        File out = new File("web/arte");
        out.mkdirs();
        Atlas atlas = new Atlas();
        StringBuilder json = new StringBuilder("{\n  \"anims\": {\n");
        int n = 0;
        for (Map.Entry<String, List<Atlas.Frame>> e : atlas.anims.entrySet()) {
            json.append(n++ > 0 ? ",\n" : "").append("    \"").append(e.getKey()).append("\": [");
            for (int i = 0; i < e.getValue().size(); i++) {
                Atlas.Frame f = e.getValue().get(i);
                String file = e.getKey() + "-" + i + ".png";
                write(shrink(f.img(), f.w(), f.h()), new File(out, file));
                json.append(i > 0 ? ", " : "").append(String.format(java.util.Locale.ROOT,
                    "{\"f\": \"%s\", \"w\": %.3f, \"h\": %.3f, \"ax\": %.3f}", file, f.w(), f.h(), f.ax()));
            }
            json.append("]");
        }
        json.append("\n  },\n  \"sky\": [");
        for (int i = 0; i < atlas.sky.size(); i++) { // céu não tem transparência: JPEG fica ~6x menor
            writeJpeg(atlas.sky.get(i), new File(out, "ceu-" + i + ".jpg"));
            json.append(i > 0 ? ", " : "").append("\"ceu-").append(i).append(".jpg\"");
        }
        double groundShrink = Math.min(1, GROUND_PX_PER_UNIT * atlas.groundScale);
        BufferedImage chao = Arte.resize(atlas.ground, (int) Math.round(atlas.ground.getWidth() * groundShrink),
            (int) Math.round(atlas.ground.getHeight() * groundShrink));
        write(chao, new File(out, "chao.png"));
        json.append(String.format(java.util.Locale.ROOT,
            "],\n  \"skyScale\": %.5f,\n  \"ground\": {\"f\": \"chao.png\", \"scale\": %.5f, \"surface\": %.1f},\n  \"falas\": [\n",
            atlas.skyScale, atlas.groundScale / groundShrink, atlas.groundSurface * groundShrink));
        for (int i = 0; i < Falas.INTRO.size(); i++) {
            Falas.Fala f = Falas.INTRO.get(i);
            json.append(i > 0 ? ",\n" : "").append("    {\"rosto\": \"").append(f.rosto())
                .append("\", \"texto\": \"").append(escape(f.texto())).append("\"}");
        }
        json.append("\n  ]\n}\n");
        Files.writeString(new File(out, "manifest.json").toPath(), json, StandardCharsets.UTF_8);
        long bytes = 0;
        for (File f : out.listFiles()) bytes += f.length();
        System.out.printf("Exportado: %d arquivos, %.1f MB em %s%n", out.listFiles().length, bytes / 1e6, out.getPath());
    }

    // Reduz quadros muito grandes (ex.: pinheiros, retratos) pra web carregar rápido.
    private static BufferedImage shrink(BufferedImage img, double w, double h) {
        int pw = (int) Math.round(Math.min(img.getWidth(), w * MAX_PX_PER_UNIT));
        int ph = (int) Math.round(Math.min(img.getHeight(), h * MAX_PX_PER_UNIT));
        return pw == img.getWidth() && ph == img.getHeight() ? img : Arte.resize(img, pw, ph);
    }

    private static void write(BufferedImage img, File f) throws Exception {
        ImageIO.write(img, "png", f);
    }

    private static void writeJpeg(BufferedImage img, File f) throws Exception {
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(img, 0, 0, null);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.9f);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(f)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
        }
        writer.dispose();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
