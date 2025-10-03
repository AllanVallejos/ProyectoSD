package proyecto1;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class MorfologiaMatematicaOptimizada {

    // ==============================
    // ELEMENTOS ESTRUCTURANTES (Figura 4)
    // Ancla = centro geométrico
    // ==============================
    public static int[][] crearCruz3x3() {
        return new int[][]{
                {0, 1, 0},
                {1, 1, 1},
                {0, 1, 0}
        };
    }

    public static int[][] figura4.1() {
        return new int[][]{
                {1, 1, 1},
                {1, 1, 1},
                {1, 1, 1}
        };
    }

    public static int[][] crearLinea3x1() { // Horizontal
        return new int[][]{
                {1, 1, 1}
        };
    }

    // (Opcional) Rotación vertical 1x3, si la quieres exponer en el menú
    public static int[][] crearLinea1x3() {
        return new int[][]{
                {1},
                {1},
                {1}
        };
    }

    public static int[][] crearDiagonal3x3() { // Diagonal principal
        return new int[][]{
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        };
    }

    public static int[][] crearCuadrado5x5() {
        return new int[][]{
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 1, 1, 1, 1}
        };
    }

    // Selección del EE (1..5)
    public static int[][] seleccionarElementoEstructurante(int opcion) {
        switch (opcion) {
            case 1: return crearCruz3x3();
            case 2: return crearCuadrado3x3();
            case 3: return crearLinea3x1();    // si agregas 6, puedes mapear a crearLinea1x3()
            case 4: return crearDiagonal3x3();
            case 5: return crearCuadrado5x5();
            default:
                System.out.println("Opción inválida, usando cruz 3x3 por defecto.");
                return crearCruz3x3();
        }
    }

    // ==============================
    // POLÍTICA DE BORDES: Replicación
    // ==============================
    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    // Alfa: si la imagen no tiene canal alfa, usar 255
    private static int alphaDe(BufferedImage img, int x, int y) {
        int argb = img.getRGB(x, y);
        int a = (argb >>> 24) & 0xFF;
        if (!img.getColorModel().hasAlpha()) return 0xFF;
        return a;
    }

    // ==============================
    // EROSIÓN SECUENCIAL (RGB por canal, alfa preservado)
    // ==============================
    public static BufferedImage erosionSecuencial(BufferedImage imagen, int[][] ee) {
        final int width = imagen.getWidth();
        final int height = imagen.getHeight();
        // Para conservar el tipo original (y por ende alfa si existe):
        BufferedImage salida = new BufferedImage(width, height, imagen.getType());

        // altoEE = filas, anchoEE = columnas
        final int altoEE = ee.length;
        final int anchoEE = ee[0].length;
        final int cx = anchoEE / 2;  // centro en X depende del ANCHO
        final int cy = altoEE / 2;   // centro en Y depende del ALTO

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int minR = 255, minG = 255, minB = 255;
                int a = alphaDe(imagen, x, y); // conservar alfa

                for (int dy = 0; dy < altoEE; dy++) {
                    for (int dx = 0; dx < anchoEE; dx++) {
                        if (ee[dy][dx] != 1) continue;

                        // Replicación de bordes
                        int px = clamp(x + dx - cx, 0, width - 1);
                        int py = clamp(y + dy - cy, 0, height - 1);

                        int rgb = imagen.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        if (r < minR) minR = r;
                        if (g < minG) minG = g;
                        if (b < minB) minB = b;
                    }
                }

                int newRgb = (a << 24) | (minR << 16) | (minG << 8) | minB;
                salida.setRGB(x, y, newRgb);
            }
        }
        return salida;
    }

    // ==============================
    // DILATACIÓN SECUENCIAL (RGB por canal, alfa preservado)
    // ==============================
    public static BufferedImage dilatacionSecuencial(BufferedImage imagen, int[][] ee) {
        final int width = imagen.getWidth();
        final int height = imagen.getHeight();
        BufferedImage salida = new BufferedImage(width, height, imagen.getType());

        final int altoEE = ee.length;
        final int anchoEE = ee[0].length;
        final int cx = anchoEE / 2;
        final int cy = altoEE / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maxR = 0, maxG = 0, maxB = 0;
                int a = alphaDe(imagen, x, y); // conservar alfa

                for (int dy = 0; dy < altoEE; dy++) {
                    for (int dx = 0; dx < anchoEE; dx++) {
                        if (ee[dy][dx] != 1) continue;

                        int px = clamp(x + dx - cx, 0, width - 1);
                        int py = clamp(y + dy - cy, 0, height - 1);

                        int rgb = imagen.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        if (r > maxR) maxR = r;
                        if (g > maxG) maxG = g;
                        if (b > maxB) maxB = b;
                    }
                }

                int newRgb = (a << 24) | (maxR << 16) | (maxG << 8) | maxB;
                salida.setRGB(x, y, newRgb);
            }
        }
        return salida;
    }

    // ==============================
    // MAIN (demo simple)
    // Lee entrada.png, aplica erosión y dilatación y guarda resultados
    // ==============================
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Seleccione el elemento estructurante:");
        System.out.println("1. Cruz 3x3");
        System.out.println("2. Cuadrado 3x3");
        System.out.println("3. Línea 3x1");
        System.out.println("4. Diagonal 3x3");
        System.out.println("5. Cuadrado 5x5");
        System.out.print("Opción: ");

        int opcion = 1;
        try {
            opcion = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException ignored) {}

        int[][] ee = seleccionarElementoEstructurante(opcion);

        try {
            BufferedImage entrada = ImageIO.read(new File("entrada.png"));
            if (entrada == null) {
                System.err.println("No se pudo leer 'entrada.png'.");
                return;
            }

            System.out.println("Procesando erosión...");
            BufferedImage erosion = erosionSecuencial(entrada, ee);

            System.out.println("Procesando dilatación...");
            BufferedImage dilatacion = dilatacionSecuencial(entrada, ee);

            ImageIO.write(erosion, "png", new File("salida_erosion.png"));
            ImageIO.write(dilatacion, "png", new File("salida_dilatacion.png"));

            System.out.println("Listo. Archivos: salida_erosion.png, salida_dilatacion.png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
