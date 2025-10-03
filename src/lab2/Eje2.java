
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Eje2 {
    private static final int DISTANCIA_TOTAL = 400;
    private static final int COMPETIDORES_POR_PISTA = 4;
    private static final int NUM_PISTAS = 5;
    private static final int TOTAL_COMPETIDORES = 20;
    private static final int DISTANCIA_POR_COMPETIDOR = 100;
    
    // Variables compartidas
    private static final boolean[] testigoEnPista = new boolean[NUM_PISTAS]; // true si hay testigo en esa pista
    private static final int[] posicionActualPista = new int[NUM_PISTAS]; // posición actual en cada pista
    private static final boolean[] carreraTerminada = new boolean[NUM_PISTAS]; // true si la pista ya terminó
    private static final Lock lockSalida = new ReentrantLock();
    private static final int[] posicionFinal = new int[NUM_PISTAS]; // posición final de cada pista
    private static final CountDownLatch inicioCarrera = new CountDownLatch(1);
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== CARRERA DE RELEVO ===");
        System.out.println("Distancia total: " + DISTANCIA_TOTAL + " metros");
        System.out.println("Numero de pistas: " + NUM_PISTAS);
        System.out.println("Competidores por pista: " + COMPETIDORES_POR_PISTA);
        System.out.println("Distancia por competidor: " + DISTANCIA_POR_COMPETIDOR + " metros\n");
        
        // Inicializar variables
        for (int i = 0; i < NUM_PISTAS; i++) {
            testigoEnPista[i] = false;
            posicionActualPista[i] = 0;
            carreraTerminada[i] = false;
            posicionFinal[i] = 0;
        }
        
        // Crear y configurar competidores
        Thread[] competidores = new Thread[TOTAL_COMPETIDORES];
        int idCompetidor = 1;
        
        System.out.println("=== CONFIGURACION INICIAL ===");
        
        for (int pista = 0; pista < NUM_PISTAS; pista++) {
            for (int posicion = 0; posicion < COMPETIDORES_POR_PISTA; posicion++) {
                int posicionInicial = posicion * DISTANCIA_POR_COMPETIDOR;
                boolean esInicial = (posicion == 0); // Solo el primero de cada pista tiene testigo inicialmente
                
                competidores[idCompetidor - 1] = new Competidor(idCompetidor, pista, posicion, posicionInicial, esInicial);
                
                System.out.println("Competidor " + idCompetidor + " asignado a pista " + (pista + 1) + 
                                 " en posicion " + posicion + 
                                 (esInicial ? " (CON TESTIGO INICIAL)" : " (ESPERANDO TESTIGO)"));
                
                idCompetidor++;
            }
        }
        
        System.out.println("\n=== INICIANDO CARRERA ===");
        
        // Iniciar todos los hilos
        for (Thread competidor : competidores) {
            competidor.start();
        }
        
        // Dar inicio a la carrera
        Thread.sleep(1000); // Pequeña pausa para que todos estén listos
        inicioCarrera.countDown(); // ¡Que comience la carrera!
        
        // Esperar a que todos terminen
        for (Thread competidor : competidores) {
            competidor.join();
        }
        
        // Mostrar resultados finales
        mostrarResultadosFinales();
    }
    
    private static void mostrarResultadosFinales() {
        System.out.println("\n==================================================");
        System.out.println("RESULTADOS FINALES DE LA CARRERA");
        System.out.println("==================================================");
        
        // Ordenar pistas por posición final (de mayor a menor)
        Integer[] pistasOrdenadas = new Integer[NUM_PISTAS];
        for (int i = 0; i < NUM_PISTAS; i++) {
            pistasOrdenadas[i] = i;
        }
        
        // Ordenamiento simple por posición final
        for (int i = 0; i < NUM_PISTAS - 1; i++) {
            for (int j = i + 1; j < NUM_PISTAS; j++) {
                if (posicionFinal[pistasOrdenadas[i]] < posicionFinal[pistasOrdenadas[j]]) {
                    Integer temp = pistasOrdenadas[i];
                    pistasOrdenadas[i] = pistasOrdenadas[j];
                    pistasOrdenadas[j] = temp;
                }
            }
        }
        
        System.out.println("CLASIFICACION FINAL:");
        for (int i = 0; i < NUM_PISTAS; i++) {
            int pista = pistasOrdenadas[i];
            String estado = carreraTerminada[pista] ? "COMPLETO LA CARRERA" : "NO COMPLETO";
            System.out.println((i + 1) + " lugar - Pista " + (pista + 1) + 
                             ": " + posicionFinal[pista] + " metros - " + estado);
        }
    }
    
    static class Competidor extends Thread {
        private final int id;
        private final int pista;
        private final int numeroEnPista; // 0, 1, 2, 3
        private final int posicionInicial;
        private boolean tieneTestigo;
        private final Random random = new Random();
        
        public Competidor(int id, int pista, int numeroEnPista, int posicionInicial, boolean tieneTestigo) {
            this.id = id;
            this.pista = pista;
            this.numeroEnPista = numeroEnPista;
            this.posicionInicial = posicionInicial;
            this.tieneTestigo = tieneTestigo;
        }
        
        @Override
        public void run() {
            try {
                // Esperar a que inicie la carrera
                inicioCarrera.await();
                
                if (tieneTestigo) {
                    // Si es el primer competidor, empieza inmediatamente
                    testigoEnPista[pista] = true;
                    correr();
                } else {
                    // Esperar a recibir el testigo
                    esperarTestigo();
                    correr();
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        private void esperarTestigo() throws InterruptedException {
            // Esperar hasta que el testigo llegue a mi posición
            while (!carreraTerminada[pista]) {
                synchronized (this) {
                    if (posicionActualPista[pista] >= posicionInicial && testigoEnPista[pista] && 
                        !tieneTestigo && numeroEnPista > 0) {
                        
                        // Verificar que el competidor anterior haya llegado a mi posición
                        if (posicionActualPista[pista] >= posicionInicial) {
                            tieneTestigo = true;
                            
                            lockSalida.lock();
                            try {
                                System.out.println("CAMBIO DE TESTIGO: Competidor " + id + 
                                                 " (Pista " + (pista + 1) + ") recibe el testigo en posicion " + 
                                                 posicionActualPista[pista] + " metros");
                            } finally {
                                lockSalida.unlock();
                            }
                            break;
                        }
                    }
                    Thread.sleep(10); // Pequeña pausa para no consumir demasiado CPU
                }
            }
        }
        
        private void correr() throws InterruptedException {
            int posicionLocal = posicionActualPista[pista];
            int metaPersonal = posicionInicial + DISTANCIA_POR_COMPETIDOR;
            if (numeroEnPista == COMPETIDORES_POR_PISTA - 1) {
                metaPersonal = DISTANCIA_TOTAL;
            }           
            while (posicionLocal < metaPersonal && !carreraTerminada[pista]) {
                int pasos = random.nextInt(2) + 1; // 1 o 2 pasos
                posicionLocal += pasos;                
                synchronized (this) {
                    if (posicionLocal > posicionActualPista[pista]) {
                        posicionActualPista[pista] = posicionLocal;
                        posicionFinal[pista] = posicionLocal;
                    }
                }               
                if (posicionLocal >= DISTANCIA_TOTAL) {
                    lockSalida.lock();
                    try {
                        if (!carreraTerminada[pista]) {
                            carreraTerminada[pista] = true;
                            System.out.println("META! Competidor " + id + " (Pista " + (pista + 1) + 
                                             ") cruzo la meta en " + posicionLocal + " metros");
                        }
                    } finally {
                        lockSalida.unlock();
                    }
                    break;
                }               
                if (posicionLocal >= metaPersonal && numeroEnPista < COMPETIDORES_POR_PISTA - 1) {
                    lockSalida.lock();
                    try {
                        System.out.println("Competidor " + id + " (Pista " + (pista + 1) + 
                                         ") termino su tramo en " + posicionLocal + 
                                         " metros. Pasando testigo...");
                    } finally {
                        lockSalida.unlock();
                    }
                    break;
                }
                Thread.sleep(random.nextInt(50) + 25);
            }
        }
    }
}