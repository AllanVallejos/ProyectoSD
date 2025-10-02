
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class Eje1copia { 
    private static final AtomicLong sumaTotalCompartida = new AtomicLong(0);
    private static final int FILAS = 30000;
    private static final int COLUMNAS = 30000;
    private static final int VALOR_RELLENO = 10;
    private static final int HILOS = 1;
    
    public static void main(String[] args) throws InterruptedException {
        // Crear y llenar la matriz
        int[][] matriz = new int[FILAS][COLUMNAS];
        
        System.out.println("Llenando matriz de " + FILAS + "x" + COLUMNAS + "...");
        for (int i = 0; i < FILAS; i++) {
            for (int j = 0; j < COLUMNAS; j++) {
                matriz[i][j] = ThreadLocalRandom.current().nextInt(0, 256); // entre 0 y 255
                //matriz[i][j] = VALOR_RELLENO;
            }
        }
        
        Thread[] hilos = new Thread[HILOS];
        int filasPorHilo = FILAS / HILOS;
        
        System.out.println("Iniciando " + HILOS + " hilos...");
        long tiempoInicio = System.currentTimeMillis();
        
        // Crear y lanzar los hilos
        for (int i = 0; i < HILOS; i++) {
            int filaInicio = i * filasPorHilo;
            int filaTermina = (i == HILOS - 1) ? FILAS : (i + 1) * filasPorHilo;
            
            hilos[i] = new SumadorHilo(matriz, filaInicio, filaTermina, i + 1);
            hilos[i].start();
        }
        
        // Esperar a que todos los hilos terminen
        for (int i = 0; i < HILOS; i++) {
            hilos[i].join();
        }
        
        long tiempoFin = System.currentTimeMillis();
        
        // Mostrar el resultado
        System.out.println("\n==================================================");
        System.out.println("RESULTADOS FINALES");
        System.out.println("==================================================");
        System.out.println("Suma total: " + sumaTotalCompartida.get());
        System.out.println("Tiempo de ejecucion: " + (tiempoFin - tiempoInicio) + " ms");
        System.out.println("Matriz procesada: " + FILAS + "x" + COLUMNAS + " elementos");
    }
    
    static class SumadorHilo extends Thread {
        private final int[][] matriz;
        private final int filaInicio;
        private final int filaTermina;
        private final int idHilo;
        
        public SumadorHilo(int[][] matriz, int filaInicio, int filaTermina, int idHilo) {
            this.matriz = matriz;
            this.filaInicio = filaInicio;
            this.filaTermina = filaTermina;
            this.idHilo = idHilo;
        }
        
        @Override
        public void run() {
            long sumaParcial = 0;
            int filasProcessed = filaTermina - filaInicio;
            
            System.out.println("Hilo " + idHilo + " iniciado - Procesando filas " + 
                             filaInicio + " a " + (filaTermina - 1) + 
                             " (" + filasProcessed + " filas)");
            
            for (int i = filaInicio; i < filaTermina; i++) {
                for (int j = 0; j < COLUMNAS; j++) {
                    sumaParcial += matriz[i][j];
                }
            }
            
            sumaTotalCompartida.addAndGet(sumaParcial);
        }
    }
}