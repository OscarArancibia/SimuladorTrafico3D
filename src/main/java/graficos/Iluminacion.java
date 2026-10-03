package graficos; // Capa gráfica: iluminación de la ciudad.

/**
 * Iluminación de la ciudad: farolas, focos del auto del jugador y focos de los vehículos autónomos.
 * Guarda el estado de las luces (noche, factor de transición, interruptor de faros) y lo envía al shader;
 * el cálculo de la luz lo hace la GPU en el shader de fragmentos (resources/shaders/ciudad.frag).
 */
public class Iluminacion {

    // ==================== ESTADO Y POSICIONES DE LAS LUCES ====================
    private boolean noche = true; // Inicia la escena con iluminación nocturna.
    private float factorNoche = 1.0f; // 0 = pleno día, 1 = plena noche; las transiciones del ciclo usan valores intermedios.
    private boolean faros = true; // Inicia los focos del auto encendidos.
    private Renderizador lucesEnviadasA; // Renderizador que ya recibió las farolas: nunca se mueven, basta enviarlas una vez.
    public static final float ALTURA_LUZ = 4.5f; // Altura de la bombilla: la usan el shader y el modelo de la farola.
    public static final int TOTAL_LUCES = 49; // Una farola en cada cruce de la cuadrícula de calles de 7x7.
    private static final float[][] LUCES = new float[TOTAL_LUCES][3]; // Cada fila contiene X, Y, Z de una bombilla.
    static { // Se ejecuta una vez al cargar la clase y rellena la cuadrícula.
        int indice = 0; // Posición lineal en el arreglo (0 a 48).
        int[] indicesMapa = {1, 4, 7, 10, 13, 16, 19}; // Filas y columnas de calle del mapa de 21x21 (la 1 y la 19 forman el anillo).
        for (int fila : indicesMapa) { // Recorre las 7 filas de calle.
            for (int col : indicesMapa) { // Recorre las 7 columnas de calle.
                float offsetX = (col == 19) ? -6.0f : 6.0f; // En el cruce del último índice se invierte el desplazamiento: la acera queda hacia dentro.
                float offsetZ = (fila == 19) ? -6.0f : 6.0f; // Misma regla en profundidad.
                LUCES[indice][0] = (col - 10) * 10.0f + offsetX; // Coordenada X: esquina de la acera, 1 unidad dentro de la manzana vecina.
                LUCES[indice][1] = ALTURA_LUZ; // Altura de la bombilla.
                LUCES[indice][2] = (fila - 10) * 10.0f + offsetZ; // Coordenada Z.
                indice++;
            }
        }
    }

    // ==================== ESTADO ====================

    public boolean isNoche() { return noche; }

    public float getFactorNoche() { return factorNoche; }

    public boolean isFaros() { return faros; }

    /** Tecla F: enciende los faros si estaban apagados y viceversa. */
    public void alternarFaros() {
        faros = !faros;
    }

    /** Copia la noche y el factor de transición del clima; los faros del jugador siguen al ciclo (la tecla F aún permite apagarlos). */
    public void sincronizarConClima(boolean nuevaNoche, float nuevoFactor) {
        boolean nocheAntes = noche;
        noche = nuevaNoche;
        factorNoche = nuevoFactor;
        if (noche != nocheAntes) {
            faros = noche;
        }
    }

    // ==================== DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad; (autoX, autoZ, angulo) describen al auto del jugador. */
    public void prepararLuces(Renderizador r, float autoX, float autoZ, float angulo) {
        r.decimal("uFactorNoche", factorNoche); // Mezcla suave entre iluminación diurna y nocturna.
        r.entero("uNoche", 0); // Inicialmente configura iluminación diurna.
        if (noche) { // Revisa el modo elegido por el usuario.
            r.entero("uNoche", 1); // Comunica al shader que debe usar iluminación nocturna.
        }
        r.entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            r.entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        r.entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        if (lucesEnviadasA != r) { // Los uniforms se conservan en el programa: las farolas solo se suben la primera vez.
            for (int indice = 0; indice < LUCES.length; indice++) { // Recorre todas las bombillas (49).
                float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
                float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
                float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
                r.vector("uLuces", indice, x, y, z); // Envía esa posición al arreglo GLSL.
            }
            lucesEnviadasA = r;
        }

        float frenteX = -(float) Math.sin(angulo); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula hacia dónde apunta el auto en Z.
        r.vector("uAuto", autoX, 0.7f, autoZ); // Envía el centro del vehículo a la altura de sus focos.
        r.vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    /** Envía al shader los focos de un vehículo autónomo (índice 0 a 15). */
    public void enviarFarosVehiculo(Renderizador r, int indice, float x, float z, float anguloVehiculo, float delante, float lado) {
        r.vector("uVehPos", indice, x, 0.68f, z); // Centro del vehículo a la altura de los faros.
        r.vector("uVehFrente", indice, -(float) Math.sin(anguloVehiculo), 0, -(float) Math.cos(anguloVehiculo)); // Frente local = -Z.
        r.vector("uVehDim", indice, delante, lado, 0); // Distancia al frente y separación lateral de los faros.
    }

    /** Indica cuántos vehículos autónomos tienen los faros encendidos: de noche todos los activos, de día ninguno. */
    public void enviarCantidadVehiculos(Renderizador r, int vehiculosActivos) {
        r.entero("uVehN", noche ? vehiculosActivos : 0);
    }

    /** Oscurece toda la iluminación con la tormenta del surazo (0 = normal, 1 = tormenta completa), también de día. */
    public void enviarTormenta(Renderizador r, float intensidad) {
        r.decimal("uTormenta", intensidad);
    }

    // ==================== MODELOS DE LAS FAROLAS ====================

    /** Dibuja postes y bombillas en las mismas posiciones usadas para calcular la luz. */
    public void dibujarFarolas(Renderizador r) {
        for (int i = 0; i < LUCES.length; i++) { // Recorre todas las luces.
            int filaLuz = i / 7; // Fila de la cuadrícula 7x7 (0 a 6); 2, 3 y 4 equivalen a las calles 7, 10 y 13.
            int columnaLuz = i % 7; // Columna de la cuadrícula 7x7.
            if (filaLuz >= 2 && filaLuz <= 4 && columnaLuz >= 2 && columnaLuz <= 4) { // Cruce interior con semáforos.
                continue; // Omite solo el poste: la luz sigue enviándose al shader desde prepararLuces().
            }
            dibujarFarola(r, LUCES[i][0], LUCES[i][2]); // Dibuja el poste exactamente donde está la fuente de luz del shader.
        }
    }

    /** Dibuja una farola: poste gris alto y bloque en la punta, centrado en la bombilla (x, ALTURA_LUZ, z). */
    private void dibujarFarola(Renderizador r, float x, float z) {
        r.caja(x, ALTURA_LUZ / 2 - 0.05f, z, 0.18f, ALTURA_LUZ - 0.1f, 0.18f, 0.20f, 0.24f, 0.28f); // Poste que termina justo bajo la bombilla.
        float rojo = 0.22f; // De día: vidrio oscuro apagado.
        float verde = 0.24f;
        float azul = 0.27f;
        if (noche) { // Solo de noche la bombilla aparenta estar encendida.
            r.entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            rojo = 1; verde = 0.83f; azul = 0.42f; // Color cálido brillante.
        }
        r.caja(x, ALTURA_LUZ, z, 0.7f, 0.35f, 0.7f, rojo, verde, azul); // Bloque de la bombilla, en la misma posición que uLuces[i].
        r.entero("uEmision", 0); // Restablece el material normal.
    }
}
