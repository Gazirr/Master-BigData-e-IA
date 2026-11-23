import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import javax.imageio.ImageIO;

public class knn_iris_java {

    static class Muestra {
        final double[] features; // [sepal_length, sepal_width, petal_length, petal_width]
        final String species;

        Muestra(double[] features, String species) {
            this.features = features;
            this.species = species;
        }
    }

    static class Vecino {
        final double distancia;
        final String species;

        Vecino(double distancia, String species) {
            this.distancia = distancia;
            this.species = species;
        }
    }

    static class EscaladorZScore {
        final double[] medias;
        final double[] desviaciones;

        EscaladorZScore(double[] medias, double[] desviaciones) {
            this.medias = medias;
            this.desviaciones = desviaciones;
        }

        static EscaladorZScore ajustar(List<Muestra> trainData) {
            int n = trainData.size();
            int d = trainData.get(0).features.length;
            double[] medias = new double[d];
            double[] desvs = new double[d];

            for (int j = 0; j < d; j++) {
                double suma = 0.0;
                for (Muestra m : trainData) {
                    suma += m.features[j];
                }
                medias[j] = suma / n;
            }

            // Desviacion tipica muestral (n - 1), consistente con R/Julia/Pandas
            for (int j = 0; j < d; j++) {
                double sumaSq = 0.0;
                for (Muestra m : trainData) {
                    double diff = m.features[j] - medias[j];
                    sumaSq += diff * diff;
                }
                double sd = Math.sqrt(sumaSq / Math.max(1, n - 1));
                desvs[j] = (sd == 0.0) ? 1.0 : sd;
            }

            return new EscaladorZScore(medias, desvs);
        }

        List<Muestra> transformar(List<Muestra> datos) {
            List<Muestra> salida = new ArrayList<>(datos.size());
            for (Muestra m : datos) {
                double[] esc = new double[m.features.length];
                for (int j = 0; j < m.features.length; j++) {
                    esc[j] = (m.features[j] - medias[j]) / desviaciones[j];
                }
                salida.add(new Muestra(esc, m.species));
            }
            return salida;
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        long tInicioNs = System.nanoTime();

        final long SEED = 42L;
        final double TRAIN_RATIO = 0.80;
        final int K = 5;

        // 1. Entorno y trazabilidad
        System.out.println("=== 1. Entorno y trazabilidad ===");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        guardarInfoSesion(SEED, TRAIN_RATIO, K);

        // 2. Carga y exploracion del dataset Iris (150 filas, 4 variables, 3 clases)
        List<Muestra> iris = cargarIrisIntegrado();
        System.out.println("\n=== 2. Carga y exploracion inicial del dataset Iris ===");
        System.out.println("Filas: " + iris.size() + " | Variables numericas: 4 | Clases: 3");
        int nulos = contarValoresInvalidos(iris);
        System.out.println("Valores NaN/infinitos: " + nulos);
        Map<String, Integer> distTotal = contarClases(iris);
        System.out.println("Distribucion total: " + distTotal);

        // 3. Particion estratificada 80/20 (semilla 42)
        Random rng = new Random(SEED);
        Map<String, List<Muestra>> porClase = new LinkedHashMap<>();
        for (Muestra m : iris) {
            porClase.computeIfAbsent(m.species, k -> new ArrayList<>()).add(m);
        }

        List<Muestra> trainRaw = new ArrayList<>();
        List<Muestra> testRaw = new ArrayList<>();

        for (Map.Entry<String, List<Muestra>> entry : porClase.entrySet()) {
            List<Muestra> grupo = new ArrayList<>(entry.getValue());
            Collections.shuffle(grupo, rng);
            int nTrainClase = (int) Math.round(grupo.size() * TRAIN_RATIO);
            for (int i = 0; i < grupo.size(); i++) {
                if (i < nTrainClase) {
                    trainRaw.add(grupo.get(i));
                } else {
                    testRaw.add(grupo.get(i));
                }
            }
        }

        Collections.shuffle(trainRaw, rng);
        Collections.shuffle(testRaw, rng);

        System.out.println("\n=== 3. Particion estratificada (80/20, seed=" + SEED + ") y escalado ===");
        System.out.println("Train: " + trainRaw.size() + " | Test: " + testRaw.size());
        System.out.println("Distribucion train: " + contarClases(trainRaw));
        System.out.println("Distribucion test:  " + contarClases(testRaw));

        // Estandarizacion ajustada EXCLUSIVAMENTE en train (sin data leakage)
        EscaladorZScore scaler = EscaladorZScore.ajustar(trainRaw);
        List<Muestra> trainScaled = scaler.transformar(trainRaw);
        List<Muestra> testScaled = scaler.transformar(testRaw);

        double[] mediaTrainEsc = mediaColumnas(trainScaled);
        double[] stdTrainEsc = desvColumnas(trainScaled);
        System.out.printf("Media train escalado: [%.4f, %.4f, %.4f, %.4f]%n",
                mediaTrainEsc[0], mediaTrainEsc[1], mediaTrainEsc[2], mediaTrainEsc[3]);
        System.out.printf("Std train escalado:   [%.4f, %.4f, %.4f, %.4f]%n",
                stdTrainEsc[0], stdTrainEsc[1], stdTrainEsc[2], stdTrainEsc[3]);

        // 4. Entrenamiento y evaluacion KNN (K = 5)
        List<String> clases = new ArrayList<>(new TreeSet<>(distTotal.keySet()));
        int numClases = clases.size();
        Map<String, Integer> claseToIdx = new LinkedHashMap<>();
        for (int i = 0; i < numClases; i++) {
            claseToIdx.put(clases.get(i), i);
        }

        List<String> yPred = new ArrayList<>(testScaled.size());
        for (Muestra mTest : testScaled) {
            yPred.add(predecirKNN(mTest, trainScaled, K));
        }

        int aciertos = 0;
        for (int i = 0; i < testScaled.size(); i++) {
            if (yPred.get(i).equals(testScaled.get(i).species)) {
                aciertos++;
            }
        }
        double accTest = (double) aciertos / testScaled.size();

        System.out.println("\n=== 4. Evaluacion KNN (K = " + K + ") en conjunto de prueba ===");
        System.out.printf("Accuracy en test: %.2f%% (%d/%d)%n", accTest * 100.0, aciertos, testScaled.size());

        // Matriz de confusion: filas = Clase Predicha, columnas = Clase Real
        int[][] matCounts = new int[numClases][numClases];
        for (int i = 0; i < testScaled.size(); i++) {
            int idxPred = claseToIdx.get(yPred.get(i));
            int idxReal = claseToIdx.get(testScaled.get(i).species);
            matCounts[idxPred][idxReal]++;
        }

        System.out.println("\n=== Matriz de confusion (filas = Predicha, columnas = Real) ===");
        imprimirMatrizConfusion(clases, matCounts);

        System.out.println("\n=== Metricas por clase ===");
        System.out.printf("%-12s %8s %10s %10s %10s%n", "Clase", "Soporte", "Precision", "Recall", "F1");
        for (String c : clases) {
            int tp = 0, fp = 0, fn = 0;
            for (int i = 0; i < testScaled.size(); i++) {
                String real = testScaled.get(i).species;
                String pred = yPred.get(i);
                if (pred.equals(c) && real.equals(c)) tp++;
                else if (pred.equals(c) && !real.equals(c)) fp++;
                else if (!pred.equals(c) && real.equals(c)) fn++;
            }
            int soporte = tp + fn;
            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            double f1 = (precision + recall) > 0 ? 2.0 * precision * recall / (precision + recall) : 0.0;
            System.out.printf("%-12s %8d %10.4f %10.4f %10.4f%n", c, soporte, precision, recall, f1);
        }

        // 5. Visualizacion de la matriz de confusion
        String rutaImg = "matriz_confusion_java.png";
        guardarGraficoMatrizConfusion(clases, matCounts, accTest, K, rutaImg);

        long tFinNs = System.nanoTime();
        double duracionMs = (tFinNs - tInicioNs) / 1_000_000.0;
        System.out.println("\n=== 5. Visualizacion y cierre ===");
        System.out.println("Grafico guardado en: " + new File(rutaImg).getAbsolutePath());
        System.out.printf("Tiempo total de ejecucion: %.2f ms%n", duracionMs);
    }

    private static String predecirKNN(Muestra objetivo, List<Muestra> train, int k) {
        List<Vecino> vecinos = new ArrayList<>(train.size());
        for (Muestra m : train) {
            double dist = distanciaEuclidea(objetivo.features, m.features);
            vecinos.add(new Vecino(dist, m.species));
        }
        vecinos.sort(Comparator.comparingDouble(v -> v.distancia));

        int limite = Math.min(k, vecinos.size());
        Map<String, Integer> votos = new LinkedHashMap<>();
        Map<String, Double> sumaDist = new LinkedHashMap<>();

        for (int i = 0; i < limite; i++) {
            Vecino v = vecinos.get(i);
            votos.put(v.species, votos.getOrDefault(v.species, 0) + 1);
            sumaDist.put(v.species, sumaDist.getOrDefault(v.species, 0.0) + v.distancia);
        }

        String mejorClase = null;
        int maxVotos = -1;
        double menorDistMedia = Double.POSITIVE_INFINITY;

        for (Map.Entry<String, Integer> entry : votos.entrySet()) {
            String especie = entry.getKey();
            int count = entry.getValue();
            double distMedia = sumaDist.get(especie) / count;
            if (count > maxVotos || (count == maxVotos && distMedia < menorDistMedia)) {
                maxVotos = count;
                menorDistMedia = distMedia;
                mejorClase = especie;
            }
        }
        return mejorClase;
    }

    private static double distanciaEuclidea(double[] a, double[] b) {
        double suma = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            suma += diff * diff;
        }
        return Math.sqrt(suma);
    }

    private static int contarValoresInvalidos(List<Muestra> datos) {
        int invalidos = 0;
        for (Muestra m : datos) {
            for (double v : m.features) {
                if (Double.isNaN(v) || Double.isInfinite(v)) {
                    invalidos++;
                }
            }
        }
        return invalidos;
    }

    private static Map<String, Integer> contarClases(List<Muestra> datos) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Muestra m : datos) {
            counts.put(m.species, counts.getOrDefault(m.species, 0) + 1);
        }
        return counts;
    }

    private static double[] mediaColumnas(List<Muestra> datos) {
        int d = datos.get(0).features.length;
        double[] res = new double[d];
        for (int j = 0; j < d; j++) {
            double s = 0.0;
            for (Muestra m : datos) s += m.features[j];
            res[j] = s / datos.size();
        }
        return res;
    }

    private static double[] desvColumnas(List<Muestra> datos) {
        int d = datos.get(0).features.length;
        double[] medias = mediaColumnas(datos);
        double[] res = new double[d];
        for (int j = 0; j < d; j++) {
            double s = 0.0;
            for (Muestra m : datos) {
                double diff = m.features[j] - medias[j];
                s += diff * diff;
            }
            res[j] = Math.sqrt(s / Math.max(1, datos.size() - 1));
        }
        return res;
    }

    private static void imprimirMatrizConfusion(List<String> clases, int[][] matCounts) {
        System.out.printf("%-14s", "Pred \\ Real");
        for (String c : clases) {
            System.out.printf("%12s", c);
        }
        System.out.println();
        for (int i = 0; i < clases.size(); i++) {
            System.out.printf("%-14s", clases.get(i));
            for (int j = 0; j < clases.size(); j++) {
                System.out.printf("%12d", matCounts[i][j]);
            }
            System.out.println();
        }
    }

    private static void guardarInfoSesion(long seed, double trainRatio, int k) {
        try (PrintWriter out = new PrintWriter(new FileWriter("session_info_java.txt"))) {
            out.println("Java version: " + System.getProperty("java.version"));
            out.println("Java vendor: " + System.getProperty("java.vendor"));
            out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            out.println("Seed: " + seed);
            out.println("Split: " + (int) Math.round(trainRatio * 100) + "/" + (int) Math.round((1 - trainRatio) * 100) + " (stratified)");
            out.println("Scaler: Z-score fit on train only");
            out.println("Model: KNN (K=" + k + ", Euclidean distance)");
        } catch (IOException e) {
            System.err.println("Aviso al guardar session_info_java.txt: " + e.getMessage());
        }
    }

    private static void guardarGraficoMatrizConfusion(List<String> clases, int[][] matCounts, double acc, int k, String ruta) {
        int ancho = 720;
        int alto = 560;
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, ancho, alto);

        int n = clases.size();
        int left = 160;
        int top = 95;
        int gridSize = 360;
        int cellSize = gridSize / n;

        int maxVal = 1;
        for (int[] fila : matCounts) {
            for (int v : fila) {
                if (v > maxVal) maxVal = v;
            }
        }

        // Titulo
        g.setColor(new Color(25, 25, 25));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String titulo = String.format("Matriz de Confusion KNN en Java (K = %d | Accuracy: %.2f%%)", k, acc * 100.0);
        FontMetrics fmTitle = g.getFontMetrics();
        g.drawString(titulo, (ancho - fmTitle.stringWidth(titulo)) / 2, 45);

        // Celdas del heatmap
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        for (int i = 0; i < n; i++) {       // fila = Clase Predicha
            for (int j = 0; j < n; j++) {   // col  = Clase Real
                int val = matCounts[i][j];
                float ratio = (float) val / maxVal;
                int r = Math.round(240 - ratio * (240 - 20));
                int gr = Math.round(246 - ratio * (246 - 85));
                int b = Math.round(252 - ratio * (252 - 155));

                int x = left + j * cellSize;
                int y = top + i * cellSize;

                g.setColor(new Color(r, gr, b));
                g.fillRect(x, y, cellSize, cellSize);

                g.setColor(new Color(180, 190, 200));
                g.setStroke(new BasicStroke(1.2f));
                g.drawRect(x, y, cellSize, cellSize);

                String txt = String.valueOf(val);
                g.setColor(ratio > 0.55f ? Color.WHITE : Color.BLACK);
                FontMetrics fmVal = g.getFontMetrics();
                int tx = x + (cellSize - fmVal.stringWidth(txt)) / 2;
                int ty = y + (cellSize + fmVal.getAscent() - fmVal.getDescent()) / 2;
                g.drawString(txt, tx, ty);
            }
        }

        // Etiquetas de ejes (clases)
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(35, 35, 35));
        for (int idx = 0; idx < n; idx++) {
            String label = clases.get(idx);
            FontMetrics fm = g.getFontMetrics();

            // Eje X: Clase Real
            int cx = left + idx * cellSize + (cellSize - fm.stringWidth(label)) / 2;
            g.drawString(label, cx, top + gridSize + 24);

            // Eje Y: Clase Predicha
            int cy = top + idx * cellSize + (cellSize + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(label, left - fm.stringWidth(label) - 14, cy);
        }

        // Titulos de ejes
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        String xTitle = "Clase Real";
        FontMetrics fmX = g.getFontMetrics();
        g.drawString(xTitle, left + (gridSize - fmX.stringWidth(xTitle)) / 2, top + gridSize + 58);

        Graphics2D gRot = (Graphics2D) g.create();
        String yTitle = "Clase Predicha";
        FontMetrics fmY = gRot.getFontMetrics();
        int yCenter = top + gridSize / 2;
        gRot.translate(38, yCenter + fmY.stringWidth(yTitle) / 2);
        gRot.rotate(-Math.PI / 2);
        gRot.drawString(yTitle, 0, 0);
        gRot.dispose();

        // Barra de color lateral
        int barX = left + gridSize + 35;
        int barY = top;
        int barW = 22;
        int barH = gridSize;
        for (int py = 0; py < barH; py++) {
            float ratio = 1.0f - ((float) py / Math.max(1, barH - 1));
            int r = Math.round(240 - ratio * (240 - 20));
            int gr = Math.round(246 - ratio * (246 - 85));
            int b = Math.round(252 - ratio * (252 - 155));
            g.setColor(new Color(r, gr, b));
            g.drawLine(barX, barY + py, barX + barW, barY + py);
        }
        g.setColor(new Color(140, 140, 140));
        g.drawRect(barX, barY, barW, barH);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(40, 40, 40));
        g.drawString(String.valueOf(maxVal), barX + barW + 8, barY + 10);
        g.drawString("0", barX + barW + 8, barY + barH);

        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("Muestras", barX - 8, barY - 12);

        g.dispose();

        try {
            ImageIO.write(img, "png", new File(ruta));
        } catch (IOException e) {
            System.err.println("Error al guardar " + ruta + ": " + e.getMessage());
        }
    }

    private static List<Muestra> cargarIrisIntegrado() {
        double[][] raw = {
            {5.1, 3.5, 1.4, 0.2}, {4.9, 3.0, 1.4, 0.2}, {4.7, 3.2, 1.3, 0.2}, {4.6, 3.1, 1.5, 0.2}, {5.0, 3.6, 1.4, 0.2},
            {5.4, 3.9, 1.7, 0.4}, {4.6, 3.4, 1.4, 0.3}, {5.0, 3.4, 1.5, 0.2}, {4.4, 2.9, 1.4, 0.2}, {4.9, 3.1, 1.5, 0.1},
            {5.4, 3.7, 1.5, 0.2}, {4.8, 3.4, 1.6, 0.2}, {4.8, 3.0, 1.4, 0.1}, {4.3, 3.0, 1.1, 0.1}, {5.8, 4.0, 1.2, 0.2},
            {5.7, 4.4, 1.5, 0.4}, {5.4, 3.9, 1.3, 0.4}, {5.1, 3.5, 1.4, 0.3}, {5.7, 3.8, 1.7, 0.3}, {5.1, 3.8, 1.5, 0.3},
            {5.4, 3.4, 1.7, 0.2}, {5.1, 3.7, 1.5, 0.4}, {4.6, 3.6, 1.0, 0.2}, {5.1, 3.3, 1.7, 0.5}, {4.8, 3.4, 1.9, 0.2},
            {5.0, 3.0, 1.6, 0.2}, {5.0, 3.4, 1.6, 0.4}, {5.2, 3.5, 1.5, 0.2}, {5.2, 3.4, 1.4, 0.2}, {4.7, 3.2, 1.6, 0.2},
            {4.8, 3.1, 1.6, 0.2}, {5.4, 3.4, 1.5, 0.4}, {5.2, 4.1, 1.5, 0.1}, {5.5, 4.2, 1.4, 0.2}, {4.9, 3.1, 1.5, 0.2},
            {5.0, 3.2, 1.2, 0.2}, {5.5, 3.5, 1.3, 0.2}, {4.9, 3.6, 1.4, 0.1}, {4.4, 3.0, 1.3, 0.2}, {5.1, 3.4, 1.5, 0.2},
            {5.0, 3.5, 1.3, 0.3}, {4.5, 2.3, 1.3, 0.3}, {4.4, 3.2, 1.3, 0.2}, {5.0, 3.5, 1.6, 0.6}, {5.1, 3.8, 1.9, 0.4},
            {4.8, 3.0, 1.4, 0.3}, {5.1, 3.8, 1.6, 0.2}, {4.6, 3.2, 1.4, 0.2}, {5.3, 3.7, 1.5, 0.2}, {5.0, 3.3, 1.4, 0.2},

            {7.0, 3.2, 4.7, 1.4}, {6.4, 3.2, 4.5, 1.5}, {6.9, 3.1, 4.9, 1.5}, {5.5, 2.3, 4.0, 1.3}, {6.5, 2.8, 4.6, 1.5},
            {5.7, 2.8, 4.5, 1.3}, {6.3, 3.3, 4.7, 1.6}, {4.9, 2.4, 3.3, 1.0}, {6.6, 2.9, 4.6, 1.3}, {5.2, 2.7, 3.9, 1.4},
            {5.0, 2.0, 3.5, 1.0}, {5.9, 3.0, 4.2, 1.5}, {6.0, 2.2, 4.0, 1.0}, {6.1, 2.9, 4.7, 1.4}, {5.6, 2.9, 3.6, 1.3},
            {6.7, 3.1, 4.4, 1.4}, {5.6, 3.0, 4.5, 1.5}, {5.8, 2.7, 4.1, 1.0}, {6.2, 2.2, 4.5, 1.5}, {5.6, 2.5, 3.9, 1.1},
            {5.9, 3.2, 4.8, 1.8}, {6.1, 2.8, 4.0, 1.3}, {6.3, 2.5, 4.9, 1.5}, {6.1, 2.8, 4.7, 1.2}, {6.4, 2.9, 4.3, 1.3},
            {6.6, 3.0, 4.4, 1.4}, {6.8, 2.8, 4.8, 1.4}, {6.7, 3.0, 5.0, 1.7}, {6.0, 2.9, 4.5, 1.5}, {5.7, 2.6, 3.5, 1.0},
            {5.5, 2.4, 3.8, 1.1}, {5.5, 2.4, 3.7, 1.0}, {5.8, 2.7, 3.9, 1.2}, {6.0, 2.7, 5.1, 1.6}, {5.4, 3.0, 4.5, 1.5},
            {6.0, 3.4, 4.5, 1.6}, {6.7, 3.1, 4.7, 1.5}, {6.3, 2.3, 4.4, 1.3}, {5.6, 3.0, 4.1, 1.3}, {5.5, 2.5, 4.0, 1.3},
            {5.5, 2.6, 4.4, 1.2}, {6.1, 3.0, 4.6, 1.4}, {5.8, 2.6, 4.0, 1.2}, {5.0, 2.3, 3.3, 1.0}, {5.6, 2.7, 4.2, 1.3},
            {5.7, 3.0, 4.2, 1.2}, {5.7, 2.9, 4.2, 1.3}, {6.2, 2.9, 4.3, 1.3}, {5.1, 2.5, 3.0, 1.1}, {5.7, 2.8, 4.1, 1.3},

            {6.3, 3.3, 6.0, 2.5}, {5.8, 2.7, 5.1, 1.9}, {7.1, 3.0, 5.9, 2.1}, {6.3, 2.9, 5.6, 1.8}, {6.5, 3.0, 5.8, 2.2},
            {7.6, 3.0, 6.6, 2.1}, {4.9, 2.5, 4.5, 1.7}, {7.3, 2.9, 6.3, 1.8}, {6.7, 2.5, 5.8, 1.8}, {7.2, 3.6, 6.1, 2.5},
            {6.5, 3.2, 5.1, 2.0}, {6.4, 2.7, 5.3, 1.9}, {6.8, 3.0, 5.5, 2.1}, {5.7, 2.5, 5.0, 2.0}, {5.8, 2.8, 5.1, 2.4},
            {6.4, 3.2, 5.3, 2.3}, {6.5, 3.0, 5.5, 1.8}, {7.7, 3.8, 6.7, 2.2}, {7.7, 2.6, 6.9, 2.3}, {6.0, 2.2, 5.0, 1.5},
            {6.9, 3.2, 5.7, 2.3}, {5.6, 2.8, 4.9, 2.0}, {7.7, 2.8, 6.7, 2.0}, {6.3, 2.7, 4.9, 1.8}, {6.7, 3.3, 5.7, 2.1},
            {7.2, 3.2, 6.0, 1.8}, {6.2, 2.8, 4.8, 1.8}, {6.1, 3.0, 4.9, 1.8}, {6.4, 2.8, 5.6, 2.1}, {7.2, 3.0, 5.8, 1.6},
            {7.4, 2.8, 6.1, 1.9}, {7.9, 3.8, 6.4, 2.0}, {6.4, 2.8, 5.6, 2.2}, {6.3, 2.8, 5.1, 1.5}, {6.1, 2.6, 5.6, 1.4},
            {7.7, 3.0, 6.1, 2.3}, {6.3, 3.4, 5.6, 2.4}, {6.4, 3.1, 5.5, 1.8}, {6.0, 3.0, 4.8, 1.8}, {6.9, 3.1, 5.4, 2.1},
            {6.7, 3.1, 5.6, 2.4}, {6.9, 3.1, 5.1, 2.3}, {5.8, 2.7, 5.1, 1.9}, {6.8, 3.2, 5.9, 2.3}, {6.7, 3.3, 5.7, 2.5},
            {6.7, 3.0, 5.2, 2.3}, {6.3, 2.5, 5.0, 1.9}, {6.5, 3.0, 5.2, 2.0}, {6.2, 3.4, 5.4, 2.3}, {5.9, 3.0, 5.1, 1.8}
        };

        List<Muestra> lista = new ArrayList<>(150);
        for (int i = 0; i < raw.length; i++) {
            String especie;
            if (i < 50) especie = "setosa";
            else if (i < 100) especie = "versicolor";
            else especie = "virginica";
            lista.add(new Muestra(raw[i], especie));
        }
        return lista;
    }
}