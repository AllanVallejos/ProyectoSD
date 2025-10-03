package proyecto1;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;

public class MorfologiaMatematicaOptimizada {

    // Variable estática para almacenar la ruta de la imagen actual
    private static String rutaImagen = "test_image.png"; // Imagen por defecto

    // Elemento estructurante
    static class ElementoEstructurante {

        int[][] estructura;
        //int centerX, centerY;
        int width, height;
        int anclaX, anclaY;

        public ElementoEstructurante(int[][] estructura, int anchorX, int anchorY) {
            if (estructura == null || estructura.length == 0 || estructura[0].length == 0) {
                throw new IllegalArgumentException("Estructura inválida");
            }
            this.estructura = estructura;
            this.height = estructura.length;
            this.width = estructura[0].length;
            setAnchor(anchorX, anchorY);
        }

        public void setAnchor(int ax, int ay) {
            if (ax < 0 || ay < 0 || ax >= width || ay >= height) {
                throw new IllegalArgumentException(
                        "Ancla fuera de rango (" + ax + "," + ay + ") para EE de " + width + "x" + height
                );
            }
            this.anclaX = ax;
            this.anclaY = ay;
        }

        public boolean isActive(int x, int y) {
            return estructura[y][x] == 1;
        }
    }

    // Clase que representa el trabajo de cada hilo con matriz local
    static class HiloMorfologiaOptimizado extends Thread {

        private final BufferedImage imagenOriginal;
        private final int[][] matrizGlobal;
        private final int[][] matrizLocal;
        private final ElementoEstructurante elemento;
        private final int startY, endY;
        private final int width, height;
        private final boolean esErosion;
        private final CountDownLatch latch;
        private final Object lockCopia;

        public HiloMorfologiaOptimizado(BufferedImage imagenOriginal, int[][] matrizGlobal,
                ElementoEstructurante elemento, int startY, int endY,
                boolean esErosion, CountDownLatch latch, Object lockCopia) {
            this.imagenOriginal = imagenOriginal;
            this.matrizGlobal = matrizGlobal;
            this.elemento = elemento;
            this.startY = startY;
            this.endY = endY;
            this.width = imagenOriginal.getWidth();
            this.height = imagenOriginal.getHeight();
            this.esErosion = esErosion;
            this.latch = latch;
            this.lockCopia = lockCopia;

            int filasLocales = endY - startY;
            this.matrizLocal = new int[filasLocales][width];
        }

        @Override
        public void run() {
            try {
                long tiempoInicio = System.currentTimeMillis();
                System.out.println("Hilo " + Thread.currentThread().getName()
                        + " procesando filas " + startY + " a " + (endY - 1));

                procesarFilasLocalmente();

                long tiempoProcesamiento = System.currentTimeMillis() - tiempoInicio;

                copiarResultadosAMatrizGlobal();

                long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

                System.out.println("Hilo " + Thread.currentThread().getName()
                        + " completado - Procesamiento: " + tiempoProcesamiento
                        + "ms, Total: " + tiempoTotal + "ms");

            } catch (Exception e) {
                System.err.println("Error en hilo " + Thread.currentThread().getName() + ": " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }

        private void procesarFilasLocalmente() {
            for (int y = startY; y < endY; y++) {
                int filaLocal = y - startY;

                for (int x = 0; x < width; x++) {
                    int nuevoRGB;

                    if (esErosion) {
                        nuevoRGB = calcularErosionRGB(x, y);
                    } else {
                        nuevoRGB = calcularDilatacionRGB(x, y);
                    }

                    matrizLocal[filaLocal][x] = nuevoRGB;
                }
            }
        }

        private void copiarResultadosAMatrizGlobal() {
            synchronized (lockCopia) {
                for (int filaLocal = 0; filaLocal < matrizLocal.length; filaLocal++) {
                    int filaGlobal = startY + filaLocal;
                    System.arraycopy(matrizLocal[filaLocal], 0,
                            matrizGlobal[filaGlobal], 0, width);
                }
            }
        }

        private int calcularErosionRGB(int x, int y) {
            int minR = 255, minG = 255, minB = 255;
            boolean encontroPixel = false;

            for (int ey = 0; ey < elemento.height; ey++) {
                for (int ex = 0; ex < elemento.width; ex++) {
                    if (!elemento.isActive(ex, ey)) {
                        continue;
                    }

                    int px = x + ex - elemento.anclaX;
                    int py = y + ey - elemento.anclaY;

                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        int rgb = imagenOriginal.getRGB(px, py);

                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        minR = Math.min(minR, r);
                        minG = Math.min(minG, g);
                        minB = Math.min(minB, b);
                        encontroPixel = true;
                    }
                }
            }

            if (!encontroPixel) {
                return imagenOriginal.getRGB(x, y);
            }

            return (minR << 16) | (minG << 8) | minB;
        }

        private int calcularDilatacionRGB(int x, int y) {
            int maxR = 0, maxG = 0, maxB = 0;
            boolean encontroPixel = false;

            for (int ey = 0; ey < elemento.height; ey++) {
                for (int ex = 0; ex < elemento.width; ex++) {
                    if (!elemento.isActive(ex, ey)) {
                        continue;
                    }

                    int px = x + ex - elemento.anclaX;
                    int py = y + ey - elemento.anclaY;

                    if (px >= 0 && px < width && py >= 0 && py < height) {
                        int rgb = imagenOriginal.getRGB(px, py);

                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;

                        maxR = Math.max(maxR, r);
                        maxG = Math.max(maxG, g);
                        maxB = Math.max(maxB, b);
                        encontroPixel = true;
                    }
                }
            }

            if (!encontroPixel) {
                return imagenOriginal.getRGB(x, y);
            }

            return (maxR << 16) | (maxG << 8) | maxB;
        }
    }

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    /**
     * Establece la ruta de la imagen a procesar
     */
    public static void setImagen(String ruta) {
        rutaImagen = ruta;
    }

    /**
     * Método principal para procesar desde el menú
     */
    public static void procesar(String operacion, boolean paralelo) {
        try {
            // Cargar imagen
            File archivoImagen = new File(rutaImagen);
            if (!archivoImagen.exists()) {
                System.err.println("ERROR: No se encuentra el archivo: " + rutaImagen);
                return;
            }

            BufferedImage imagen = ImageIO.read(archivoImagen);
            if (imagen == null) {
                System.err.println("ERROR: No se pudo leer la imagen: " + rutaImagen);
                return;
            }

            System.out.println("Imagen cargada: " + rutaImagen);
            System.out.println("Dimensiones: " + imagen.getWidth() + "x" + imagen.getHeight());

            // Seleccionar elemento estructurante
            ElementoEstructurante elemento = seleccionarElementoEstructurante();

            // Procesar según operación
            BufferedImage resultado;
            long tiempoInicio = System.nanoTime();

            if (operacion.equalsIgnoreCase("erosion")) {
                if (paralelo) {
                    resultado = erosionParalela(imagen, elemento);
                } else {
                    resultado = erosionSecuencial(imagen, elemento);
                }
            } else if (operacion.equalsIgnoreCase("dilatacion")) {
                if (paralelo) {
                    resultado = dilatacionParalela(imagen, elemento);
                } else {
                    resultado = dilatacionSecuencial(imagen, elemento);
                }
            } else {
                System.err.println("ERROR: Operacion no válida: " + operacion);
                return;
            }

            long tiempoTotal = System.nanoTime() - tiempoInicio;

            // Guardar resultado
            String nombreSalida = generarNombreSalida(operacion, paralelo);
            File archivoSalida = new File(nombreSalida);
            ImageIO.write(resultado, "png", archivoSalida);

            System.out.println("\n=== RESULTADO ===");
            System.out.println("Operacion: " + operacion + (paralelo ? " (Paralela)" : " (Secuencial)"));
            System.out.println("Tiempo total: " + tiempoTotal / 1_000_000 + " ms");
            System.out.println("Imagen guardada en: " + nombreSalida);
            System.out.println("=================\n");

        } catch (IOException e) {
            System.err.println("ERROR al procesar imagen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Permite al usuario seleccionar el elemento estructurante
     */
    private static ElementoEstructurante seleccionarElementoEstructurante() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n--- Seleccionar Elemento Estructurante ---");
        System.out.println("1. Cruz 3x3");
        System.out.println("2. Cuadrado 3x3");
        System.out.println("3. Linea 3x1");
        System.out.println("4. Diagonal 3x3");
        System.out.println("5. Cuadrado 5x5");
        System.out.print("Selecciona (1-5, default=1): ");

        int opcion;
        try {
            String input = scanner.nextLine().trim();
            opcion = input.isEmpty() ? 1 : Integer.parseInt(input);
        } catch (NumberFormatException e) {
            opcion = 1;
        }

        switch (opcion) {
            //case 2: return crearCuadrado3x3();
            case 3:
                return crearLineaHorizontal();
            //case 4: return crearDiagonal3x3();
            //case 5: return crearCuadrado5x5();
            default:
                return crearCruz3x3();
        }
    }

    /**
     * Genera el nombre del archivo de salida
     */
    private static String generarNombreSalida(String operacion, boolean paralelo) {
        String nombreBase = rutaImagen.substring(0, rutaImagen.lastIndexOf('.'));
        String extension = rutaImagen.substring(rutaImagen.lastIndexOf('.'));
        String tipo = paralelo ? "paralela" : "secuencial";

        return nombreBase + "_" + operacion + "_" + tipo + extension;
    }

    /**
     * Aplica erosión usando hilos optimizados con matrices locales
     */
    public static BufferedImage erosionParalela(BufferedImage imagen, ElementoEstructurante elemento) {
        return aplicarMorfologiaParalela(imagen, elemento, true);
    }

    /**
     * Aplica dilatación usando hilos optimizados con matrices locales
     */
    public static BufferedImage dilatacionParalela(BufferedImage imagen, ElementoEstructurante elemento) {
        return aplicarMorfologiaParalela(imagen, elemento, false);
    }

    /**
     * Método principal que coordina los hilos optimizados
     */
    private static BufferedImage aplicarMorfologiaParalela(BufferedImage imagen,
            ElementoEstructurante elemento,
            boolean esErosion) {
        int width = imagen.getWidth();
        int height = imagen.getHeight();

        int[][] matrizResultado = new int[height][width];
        Object lockCopia = new Object();
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);

        int filasPerHilo = height / NUM_THREADS;
        int filasRestantes = height % NUM_THREADS;

        Thread[] hilos = new Thread[NUM_THREADS];

        System.out.println("=== Procesamiento Optimizado ===");
        System.out.println("Hilos: " + NUM_THREADS);
        System.out.println("Imagen: " + width + "x" + height + " pixeles");
        System.out.println("Operacion: " + (esErosion ? "Erosion" : "Dilatacion"));
        System.out.println("Filas por hilo: ~" + filasPerHilo);

        long tiempoInicio = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            int startY = i * filasPerHilo;
            int endY = (i == NUM_THREADS - 1) ? startY + filasPerHilo + filasRestantes : startY + filasPerHilo;

            hilos[i] = new HiloMorfologiaOptimizado(imagen, matrizResultado, elemento,
                    startY, endY, esErosion, latch, lockCopia);
            hilos[i].setName("" + i);
            hilos[i].start();
        }

        try {
            latch.await();

            long tiempoProcesamiento = System.currentTimeMillis() - tiempoInicio;
            System.out.println("Procesamiento paralelo completado en " + tiempoProcesamiento + " ms");

        } catch (InterruptedException e) {
            System.err.println("Error esperando hilos: " + e.getMessage());
            e.printStackTrace();
        }

        return convertirMatrizAImagen(matrizResultado, width, height, imagen.getType());
    }

    /**
     * Convierte la matriz de enteros RGB a BufferedImage
     */
    private static BufferedImage convertirMatrizAImagen(int[][] matriz, int width, int height, int tipo) {
        BufferedImage resultado = new BufferedImage(width, height, tipo);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                resultado.setRGB(x, y, matriz[y][x]);
            }
        }

        return resultado;
    }

    /**
     * Versiones secuenciales para comparación
     */
    public static BufferedImage erosionSecuencial(BufferedImage imagen, ElementoEstructurante elemento) {
        return aplicarMorfologiaSecuencial(imagen, elemento, true);
    }

    public static BufferedImage dilatacionSecuencial(BufferedImage imagen, ElementoEstructurante elemento) {
        return aplicarMorfologiaSecuencial(imagen, elemento, false);
    }

    private static BufferedImage aplicarMorfologiaSecuencial(BufferedImage imagen,
            ElementoEstructurante elemento,
            boolean esErosion) {
        int width = imagen.getWidth();
        int height = imagen.getHeight();
        BufferedImage resultado = new BufferedImage(width, height, imagen.getType());

        System.out.println("Procesamiento secuencial iniciado...");
        long tiempoInicio = System.currentTimeMillis();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int nuevoRGB;

                if (esErosion) {
                    nuevoRGB = calcularErosionRGBSecuencial(imagen, elemento, x, y);
                } else {
                    nuevoRGB = calcularDilatacionRGBSecuencial(imagen, elemento, x, y);
                }

                resultado.setRGB(x, y, nuevoRGB);
            }
        }

        long tiempoProcesamiento = System.currentTimeMillis() - tiempoInicio;
        System.out.println("Procesamiento secuencial completado en " + tiempoProcesamiento + " ms");

        return resultado;
    }

    private static int calcularErosionRGBSecuencial(BufferedImage imagen, ElementoEstructurante elemento, int x, int y) {
        int minR = 255, minG = 255, minB = 255;

        for (int ey = 0; ey < elemento.height; ey++) {
            for (int ex = 0; ex < elemento.width; ex++) {
                if (!elemento.isActive(ex, ey)) {
                    continue;
                }

                int px = x + ex - elemento.anclaX;
                int py = y + ey - elemento.anclaY;

                if (px >= 0 && px < imagen.getWidth() && py >= 0 && py < imagen.getHeight()) {
                    int rgb = imagen.getRGB(px, py);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    minR = Math.min(minR, r);
                    minG = Math.min(minG, g);
                    minB = Math.min(minB, b);
                }
            }
        }

        return (minR << 16) | (minG << 8) | minB;
    }

    private static int calcularDilatacionRGBSecuencial(BufferedImage imagen, ElementoEstructurante elemento, int x, int y) {
        int maxR = 0, maxG = 0, maxB = 0;

        for (int ey = 0; ey < elemento.height; ey++) {
            for (int ex = 0; ex < elemento.width; ex++) {
                if (!elemento.isActive(ex, ey)) {
                    continue;
                }

                int px = x + ex - elemento.anclaX;
                int py = y + ey - elemento.anclaY;

                if (px >= 0 && px < imagen.getWidth() && py >= 0 && py < imagen.getHeight()) {
                    int rgb = imagen.getRGB(px, py);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    maxR = Math.max(maxR, r);
                    maxG = Math.max(maxG, g);
                    maxB = Math.max(maxB, b);
                }
            }
        }

        return (maxR << 16) | (maxG << 8) | maxB;
    }

    // Elementos estructurantes
    public static ElementoEstructurante crearCruz3x3() {
        int[][] estructura = {
            {0, 1, 0},
            {1, 1, 1},
            {0, 1, 0}
        };
        return new ElementoEstructurante(estructura, 1, 1);
    }

    public static ElementoEstructurante crearLInvertida() {
        int[][] estructura = {
            {1, 1},
            {0, 1}
        };
        return new ElementoEstructurante(estructura, 1, 0);
    }

    public static ElementoEstructurante crearLineaHorizontal() {
        int[][] estructura = {{1, 1, 1}};
        return new ElementoEstructurante(estructura, 1, 0);
    }

    public static ElementoEstructurante crearLNormal() {
        int[][] estructura = {
            {0, 1},
            {1, 1}
        };
        return new ElementoEstructurante(estructura, 1, 1);
    }

    public static ElementoEstructurante crearLineaVertical() {
        int[][] estructura = {
            {1},
            {1}
        };
        return new ElementoEstructurante(estructura, 0, 0);
    }

    public static ElementoEstructurante crearX() {
        int[][] estructura = {
            {1, 0, 1},
            {0, 1, 0},
            {1, 0, 1}
        };
        return new ElementoEstructurante(estructura, 1, 1);
    }

}

// Clase de prueba optimizada
class TestMorfologiaOptimizada {

    public static void main(String[] args) {
        System.out.println("=== Test Morfologia Matematica Optimizada ===");
        System.out.println("Procesadores disponibles: " + Runtime.getRuntime().availableProcessors());

        Scanner scanner = new Scanner(System.in);
        int opcion = -1;

        while (opcion != 0) {
            System.out.println("\n=======================================");
            System.out.println("   Procesamiento Morfologia Matematica");
            System.out.println("=======================================");
            System.out.println("1. Erosion (Secuencial)");
            System.out.println("2. Dilatacion (Secuencial)");
            System.out.println("3. Erosion (Paralela)");
            System.out.println("4. Dilatacion (Paralela)");
            System.out.println("5. Cambiar imagen de entrada");
            System.out.println("0. Salir");
            System.out.print("Selecciona una opcion: ");

            try {
                opcion = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                opcion = -1;
            }

            switch (opcion) {
                case 1:
                    System.out.println("\nEjecutando erosion secuencial...");
                    MorfologiaMatematicaOptimizada.procesar("erosion", false);
                    break;
                case 2:
                    System.out.println("\nEjecutando dilatacion secuencial...");
                    MorfologiaMatematicaOptimizada.procesar("dilatacion", false);
                    break;
                case 3:
                    System.out.println("\nEjecutando erosion paralela...");
                    MorfologiaMatematicaOptimizada.procesar("erosion", true);
                    break;
                case 4:
                    System.out.println("\nEjecutando dilatacion paralela...");
                    MorfologiaMatematicaOptimizada.procesar("dilatacion", true);
                    break;
                case 5:
                    System.out.print("Ingresa el nombre del archivo de imagen (con extensión): ");
                    String nombre = scanner.nextLine();
                    MorfologiaMatematicaOptimizada.setImagen(nombre);
                    System.out.println("Imagen establecida en: " + nombre);
                    break;
                case 0:
                    System.out.println("Saliendo del programa...");
                    break;
                default:
                    System.out.println("Opcion invalida. Intenta nuevamente");
            }
        }

        scanner.close();
        System.out.println("\nPrograma finalizado.");
    }
}
