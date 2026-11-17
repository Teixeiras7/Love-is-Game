package view;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Carrega as folhas de sprites da pasta arte/ (fundo magenta) e recorta cada quadro sozinho:
// tira o fundo, acha cada boneco, junta pedacinhos soltos (fios de cabelo, brilhos) e ordena em linhas.
final class Arte {
    private Arte() { }

    static BufferedImage load(String name) {
        try {
            BufferedImage src = ImageIO.read(new File("arte", name));
            if (src == null) throw new IllegalStateException("Imagem inválida: arte/" + name);
            BufferedImage im = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return im;
        } catch (IOException e) {
            throw new IllegalStateException("Não achei arte/" + name + " (rode pelo executar.bat)", e);
        }
    }

    private static boolean isMagenta(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return Math.min(r, b) - g > 90 && Math.abs(r - b) < 90;
    }

    // Fundo magenta vira transparente; a franja rosada da borda também.
    static BufferedImage keyMagenta(BufferedImage im) {
        int w = im.getWidth(), h = im.getHeight();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (isMagenta(im.getRGB(x, y))) im.setRGB(x, y, 0);
        for (int pass = 0; pass < 2; pass++) {
            List<int[]> clear = new ArrayList<>();
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    int rgb = im.getRGB(x, y);
                    if ((rgb >>> 24) == 0) continue;
                    int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                    boolean edge = (im.getRGB(x - 1, y) >>> 24) == 0 || (im.getRGB(x + 1, y) >>> 24) == 0
                        || (im.getRGB(x, y - 1) >>> 24) == 0 || (im.getRGB(x, y + 1) >>> 24) == 0;
                    if (edge && Math.min(r, b) - g > 45) clear.add(new int[] {x, y});
                }
            }
            for (int[] p : clear) im.setRGB(p[0], p[1], 0);
        }
        return im;
    }

    record Frame(BufferedImage img, double anchorX) { }

    // Recorta a folha em linhas de quadros. Pedaços a menos de `merge` px viram um só quadro.
    static List<List<Frame>> slice(BufferedImage sheet, int merge) {
        BufferedImage im = keyMagenta(sheet);
        int w = im.getWidth(), h = im.getHeight();
        int[] label = new int[w * h];
        List<int[]> boxes = new ArrayList<>(); // minX, minY, maxX, maxY, area
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < w * h; i++) {
            if (label[i] != 0 || (im.getRGB(i % w, i / w) >>> 24) == 0) continue;
            int[] box = {w, h, 0, 0, 0};
            boxes.add(box);
            label[i] = boxes.size();
            stack.push(i);
            while (!stack.isEmpty()) {
                int p = stack.pop(), px = p % w, py = p / w;
                box[0] = Math.min(box[0], px); box[1] = Math.min(box[1], py);
                box[2] = Math.max(box[2], px); box[3] = Math.max(box[3], py);
                box[4]++;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = px + dx, ny = py + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int n = ny * w + nx;
                        if (label[n] == 0 && (im.getRGB(nx, ny) >>> 24) != 0) {
                            label[n] = boxes.size();
                            stack.push(n);
                        }
                    }
                }
            }
        }

        List<int[]> objs = new ArrayList<>();
        for (int[] b : boxes) if (b[4] >= 15) objs.add(b.clone());
        boolean changed = true;
        while (changed) { // junta caixas próximas
            changed = false;
            outer:
            for (int i = 0; i < objs.size(); i++) {
                for (int j = i + 1; j < objs.size(); j++) {
                    int[] a = objs.get(i), b = objs.get(j);
                    if (a[0] - merge <= b[2] && b[0] - merge <= a[2] && a[1] - merge <= b[3] && b[1] - merge <= a[3]) {
                        a[0] = Math.min(a[0], b[0]); a[1] = Math.min(a[1], b[1]);
                        a[2] = Math.max(a[2], b[2]); a[3] = Math.max(a[3], b[3]);
                        a[4] += b[4];
                        objs.remove(j);
                        changed = true;
                        break outer;
                    }
                }
            }
        }
        int biggest = objs.stream().mapToInt(o -> o[4]).max().orElse(0);
        objs.removeIf(o -> o[4] < biggest * 0.04); // brilhinhos soltos

        objs.sort(Comparator.comparingDouble(o -> (o[1] + o[3]) / 2.0));
        List<List<int[]>> rowBoxes = new ArrayList<>();
        for (int[] o : objs) {
            double cy = (o[1] + o[3]) / 2.0;
            List<int[]> last = rowBoxes.isEmpty() ? null : rowBoxes.get(rowBoxes.size() - 1);
            if (last != null) {
                int[] f = last.get(0);
                if (Math.abs(cy - (f[1] + f[3]) / 2.0) < (f[3] - f[1]) * 0.5) {
                    last.add(o);
                    continue;
                }
            }
            List<int[]> row = new ArrayList<>();
            row.add(o);
            rowBoxes.add(row);
        }

        List<List<Frame>> rows = new ArrayList<>();
        for (List<int[]> rb : rowBoxes) {
            rb.sort(Comparator.comparingInt(o -> o[0]));
            List<Frame> row = new ArrayList<>();
            for (int[] o : rb) {
                BufferedImage f = im.getSubimage(o[0], o[1], o[2] - o[0] + 1, o[3] - o[1] + 1);
                BufferedImage copy = new BufferedImage(f.getWidth(), f.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = copy.createGraphics();
                g.drawImage(f, 0, 0, null);
                g.dispose();
                row.add(new Frame(copy, bodyCenter(copy)));
            }
            rows.add(row);
        }
        return rows;
    }

    // Para folhas em grade certinha onde um quadro encosta no outro (retratos).
    static List<Frame> grid(BufferedImage sheet, int cols, int rows) {
        BufferedImage im = keyMagenta(sheet);
        int cw = im.getWidth() / cols, ch = im.getHeight() / rows;
        List<Frame> out = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BufferedImage cell = trim(im.getSubimage(c * cw, r * ch, cw, ch));
                BufferedImage copy = new BufferedImage(cell.getWidth(), cell.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = copy.createGraphics();
                g.drawImage(cell, 0, 0, null);
                g.dispose();
                out.add(new Frame(copy, copy.getWidth() / 2.0));
            }
        }
        return out;
    }

    // Centro horizontal da parte de cima (cabeça/tronco): evita o boneco "tremer" quando a perna estica.
    private static double bodyCenter(BufferedImage im) {
        long sum = 0, n = 0;
        int limit = Math.max(1, im.getHeight() * 35 / 100);
        for (int y = 0; y < limit; y++)
            for (int x = 0; x < im.getWidth(); x++)
                if ((im.getRGB(x, y) >>> 24) != 0) { sum += x; n++; }
        return n == 0 ? im.getWidth() / 2.0 : sum / (double) n;
    }

    // Reduz com média de área (melhor qualidade pra pixel art reduzida).
    static BufferedImage resize(BufferedImage src, int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        Image scaled = src.getScaledInstance(w, h, w < src.getWidth() ? Image.SCALE_AREA_AVERAGING : Image.SCALE_REPLICATE);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return out;
    }

    static BufferedImage flip(BufferedImage src) {
        int w = src.getWidth();
        BufferedImage out = new BufferedImage(w, src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, w, 0, -w, src.getHeight(), null);
        g.dispose();
        return out;
    }

    // Separa a paisagem em faixas (dia / pôr do sol / noite) pelas linhas brancas entre elas.
    static List<BufferedImage> bands(BufferedImage im) {
        int w = im.getWidth(), h = im.getHeight();
        List<BufferedImage> out = new ArrayList<>();
        int start = -1;
        for (int y = 0; y <= h; y++) {
            boolean separator = y == h || isWhiteRow(im, y);
            if (!separator && start < 0) start = y;
            if (separator && start >= 0) {
                if (y - start > 40) out.add(im.getSubimage(0, start, w, y - start));
                start = -1;
            }
        }
        return out;
    }

    private static boolean isWhiteRow(BufferedImage im, int y) {
        int white = 0;
        for (int x = 0; x < im.getWidth(); x += 4) {
            int rgb = im.getRGB(x, y);
            if (((rgb >> 16) & 255) > 225 && ((rgb >> 8) & 255) > 225 && (rgb & 255) > 225) white++;
        }
        return white > im.getWidth() / 4 * 0.85;
    }

    // Corta só a área com pixels (usado no chão).
    static BufferedImage trim(BufferedImage im) {
        int minX = im.getWidth(), minY = im.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++)
                if ((im.getRGB(x, y) >>> 24) != 0) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
        return im.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
