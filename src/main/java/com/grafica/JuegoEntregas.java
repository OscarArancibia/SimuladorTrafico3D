package com.grafica; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
- CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
- Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
- Hereda Iluminacion, que hereda clase2, que a su vez hereda clase1.
- Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 */
public class JuegoEntregas extends Iluminacion {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private boolean enPausa = false; // Cuando es true el juego se congela y el título muestra los controles.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    
    // MODIFICACIÓN: Las coordenadas de los destinos se ajustan al nuevo mapa de 11x11
    // Las calles cruzan por X/Z en -30, 0 y 30 garantizando que caen sobre asfalto.
    private static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle.
        {-30.0f, -30.0f}, // Primera entrega: Sector Noroeste.
        {  0.0f,  30.0f}, // Segunda entrega: Sector Sur Centro.
        { 30.0f, -30.0f}  // Tercera entrega: Sector Noreste.
    };

    // ==================== MONEDA COLECCIONABLE ====================
    private static final float[][] PUNTOS_MONEDA = { // Posibles lugares de la moneda: todos sobre calles y lejos de las paradas de entrega.
        {0, -30}, {30, 0}, {-30, 0}, {60, -30}, {-60, 30}, {30, 60}, {-30, -60}, {60, 30}, {-60, -30}, {0, -60}
    };
    private int puntoMoneda = 0; // Índice del lugar donde está la moneda ahora.
    private float monedaX = PUNTOS_MONEDA[0][0]; // Posición X actual de la moneda.
    private float monedaZ = PUNTOS_MONEDA[0][1]; // Posición Z actual de la moneda.
    private static final float LIMITE_HUD = 67; // Mitad del rango que el shader del minimapa convierte a pantalla (LIMITE 65 + 2).
    private int monedasRecolectadas = 0; // Cuenta las monedas que ha recogido el jugador.

    // ==================== TRÁFICO AUTÓNOMO ====================
    private static final float[][][] RUTAS = { // Cada ruta es un circuito cerrado de esquinas (X, Z) que siempre cae sobre calles.
        {{-30, -30}, {30, -30}, {30, 30}, {-30, 30}}, // Bot 0 (patrulla): cuadrado central por las calles -30 y 30.
        {{-60, -60}, {60, -60}, {60, 60}, {-60, 60}}, // Bot 1 (camión de bomberos): vuelta completa por las calles del borde.
        {{30, -60}, {30, -30}, {60, -30}, {60, -60}}, // Bot 2 (ambulancia): circuito pequeño en el sector noreste.
        {{-60, 0}, {-30, 0}, {-30, 30}, {-60, 30}}, // Bot 3 (civil): circuito en el sector oeste.
        {{30, 0}, {60, 0}, {60, 60}, {30, 60}} // Bot 4 (civil): circuito en el sector sureste.
    };
    private final float[] botX = {-30, 60, 30, -60, 30}; // Posición X inicial de cada bot: justo en una esquina de su ruta.
    private final float[] botZ = {-30, 60, -60, 0, 0}; // Posición Z inicial de cada bot.
    private final float[] botAngulo = {0, 0, 0, 0, 0}; // Orientación en radianes; se recalcula en cada cuadro.
    private final float[] botVelocidad = {7, 9, 6, 5, 8}; // Unidades por segundo de cada bot.
    private final float[] botChoque = new float[5]; // Segundos restantes del choque de cada bot: primero espera, luego retrocede.
    private final int[] botObjetivo = {1, 3, 1, 1, 1}; // Índice de la esquina hacia la que avanza cada bot.
    private final float[][] botColor = {{0.95f, 0.80f, 0.15f}, {0.20f, 0.70f, 0.30f}, {0.60f, 0.30f, 0.85f}, {0.15f, 0.40f, 0.85f}, {0.90f, 0.90f, 0.92f}}; // Los dos últimos son los civiles: azul y blanco.

    // ==================== PEATONES EN LAS ACERAS ====================
    private final float[] peatonX = {-30, 30, 0, -30, -30, 30, 0, 0, 30, -30}; // Posición X inicial de cada peatón (en el centro de su recorrido).
    private final float[] peatonZ = {-35.7f, -24.3f, 24.3f, 35.7f, -5.7f, 5.7f, -54.3f, 54.3f, -35.7f, 24.3f}; // Posición Z fija: acera pegada a una calle, a ambos lados de z = -30, 0 y 30 y en los bordes.
    private final float[] peatonAngulo = {-1.57f, 1.57f, -1.57f, 1.57f, -1.57f, 1.57f, -1.57f, 1.57f, -1.57f, 1.57f}; // -1,57 camina hacia +X y 1,57 hacia -X.
    private final float[] peatonOrigenX = {-30, 30, 0, -30, -30, 30, 0, 0, 30, -30}; // Centro del recorrido, sobre el eje de una calle vertical: van 14 unidades a cada lado, así que la cruzan.
    private final float[] peatonAplastado = new float[10]; // Temporizador por peatón: 0 = normal; mayor que 0 = aplastado (segundos transcurridos).
    private final float[][] peatonColor = {
        {0.20f, 0.65f, 0.30f}, {0.90f, 0.80f, 0.20f}, {0.50f, 0.50f, 0.52f}, {0.60f, 0.30f, 0.70f}, // Verde, amarillo, gris y morado.
        {0.90f, 0.45f, 0.15f}, {0.15f, 0.60f, 0.75f}, {0.85f, 0.35f, 0.55f}, {0.45f, 0.30f, 0.20f}, // Naranja, turquesa, rosa y café.
        {0.95f, 0.95f, 0.95f}, {0.30f, 0.30f, 0.70f} // Blanco y azul oscuro.
    };

    // ==================== MASCOTAS EN LOS PARQUES ====================
    private static final int[][] PARCELAS_MASCOTAS = {{1, 7}, {8, 2}, {10, 11}, {7, 1}}; // Celdas de parque (tipo 2) de {fila, columna}: 2 perros y 2 gatos.
    private final float[] animalCentroX = new float[4]; // Centro X del parque donde vive cada animal.
    private final float[] animalCentroZ = new float[4]; // Centro Z del parque donde vive cada animal.
    private final float[] animalX = new float[4]; // Posición X actual: índices 0-1 perros, 2-3 gatos.
    private final float[] animalZ = new float[4]; // Posición Z actual de cada animal.
    private final float[] animalAngulo = {0.5f, 2.2f, 4.0f, 5.5f}; // Orientación inicial distinta para cada animal.
    private float tiempoAnimacion = 0; // Reloj propio de las mascotas: sigue corriendo aunque el juego termine.

    { // Bloque de inicialización: coloca cada animal en el centro de su parque.
        for (int animal = 0; animal < PARCELAS_MASCOTAS.length; animal++) { // Recorre los cuatro animales.
            animalCentroX[animal] = centro(PARCELAS_MASCOTAS[animal][1]); // Convierte la columna del parque a coordenada X.
            animalCentroZ[animal] = centro(PARCELAS_MASCOTAS[animal][0]); // Convierte la fila del parque a coordenada Z.
            animalX[animal] = animalCentroX[animal]; // Empieza alineado con el centro del parque.
            animalZ[animal] = animalCentroZ[animal] + (animal % 2 == 0 ? -1.8f : 1.8f); // Se desplaza para no nacer dentro del banco.
        }
    }

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por Iluminacion.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
        if (key == GLFW_KEY_P) { // Comprueba si se pulsó la tecla de pausa.
            enPausa = !enPausa; // Alterna entre pausa y juego.
            if (enPausa) { // Al pausar, el título pasa a ser la ayuda de controles.
                glfwSetWindowTitle(ventana, "PAUSA (P: continuar) - Controles: WASD/flechas mover, Espacio freno, C camara, F luces, N dia/noche, M mapa, R reiniciar, ESC salir");
            } // Al reanudar, actualizarTitulo() de Vehiculo restaura el título normal en el siguiente cuadro.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        monedasRecolectadas = 0; // Vuelve a contar las monedas desde cero.
    }

    /** Mueve la moneda a un punto distinto de la lista, siempre sobre una calle. */
    private void reubicarMoneda() {
        int nuevoPunto = puntoMoneda; // Parte del punto actual para asegurarse de cambiarlo.
        while (nuevoPunto == puntoMoneda) { // Repite hasta obtener un punto diferente.
            nuevoPunto = (int) (Math.random() * PUNTOS_MONEDA.length); // Elige un índice al azar.
        }
        puntoMoneda = nuevoPunto; // Guarda el nuevo punto.
        monedaX = PUNTOS_MONEDA[nuevoPunto][0]; // Actualiza la posición X de la moneda.
        monedaZ = PUNTOS_MONEDA[nuevoPunto][1]; // Actualiza la posición Z de la moneda.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        if (enPausa) { // Si el juego está en pausa nada se mueve ni se actualiza (tampoco el título).
            return;
        }
        float autoXPrevio = autoX; // Guarda la posición X del jugador antes de moverlo.
        float autoZPrevio = autoZ; // Guarda la posición Z del jugador antes de moverlo.
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        actualizarBots(deltaTime); // Mueve el tráfico autónomo, incluso cuando el juego ya terminó.
        resolverChoqueBots(autoXPrevio, autoZPrevio); // Detiene al jugador y al bot si se tocan.
        float dx = autoX - monedaX; // Separación horizontal entre el auto y la moneda (autoX viene de Vehiculo).
        float dz = autoZ - monedaZ; // Separación en profundidad entre el auto y la moneda.
        float distCuadrada = dx * dx + dz * dz; // Distancia al cuadrado, sin calcular raíz.
        if (distCuadrada < 25.0f) { // Radio de recolección de 5 unidades: cubre toda la marca de 6x6.
            monedasRecolectadas++; // Suma una moneda al contador.
            reubicarMoneda(); // Mueve la moneda a otro punto de la ciudad.
        }
        actualizarMascotas(deltaTime); // Mueve perros y gatos dentro de sus parques.
        actualizarPeatones(deltaTime); // Hace patrullar a los peatones por las aceras.
        if (entregas >= DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de Iluminacion.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | "; // Muestra la tecla que alterna el minimapa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar las tres paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: jugar otra vez"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/3"; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en la marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        mensaje += " | Monedas: " + monedasRecolectadas; // Muestra cuántas monedas lleva el jugador.
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        // La marca pequeña de la parada (dibujarDestino) ya no se dibuja; las entregas siguen funcionando igual.
        dibujarMoneda(monedaX, monedaZ, tiempoAnimacion * 2, tiempoAnimacion); // Dibuja la moneda: marca amarilla y cubo flotante giratorio.
        for (int bot = 0; bot < RUTAS.length; bot++) { // Dibuja cada auto autónomo, también en el minimapa.
            if (bot == 0) { // El primer bot es la patrulla de policía.
                dibujarAutoPolicia(botX[bot], botZ[bot], botAngulo[bot], tiempoAnimacion);
            } else if (bot == 1) { // El segundo bot es el camión de bomberos.
                dibujarCamionBomberos(botX[bot], botZ[bot], botAngulo[bot], tiempoAnimacion);
            } else if (bot == 2) { // El tercer bot es la ambulancia.
                dibujarAmbulancia(botX[bot], botZ[bot], botAngulo[bot], tiempoAnimacion);
            } else { // Los bots 3 y 4 son autos civiles normales.
                dibujarBot(botX[bot], botZ[bot], botAngulo[bot], botColor[bot][0], botColor[bot][1], botColor[bot][2]);
            }
        }
        if (!vistaMapa) { // Las mascotas son demasiado pequeñas para el minimapa.
            for (int peaton = 0; peaton < peatonX.length; peaton++) { // Dibuja los cuatro peatones con ropa de colores distintos.
                dibujarPeaton(peatonX[peaton], peatonZ[peaton], peatonAngulo[peaton], tiempoAnimacion, peatonColor[peaton][0], peatonColor[peaton][1], peatonColor[peaton][2], peatonAplastado[peaton] > 0);
            }
            for (int animal = 0; animal < 4; animal++) { // Recorre los cuatro animales.
                if (animal < 2) { // Los dos primeros son perros.
                    dibujarPerro(animalX[animal], animalZ[animal], animalAngulo[animal], tiempoAnimacion);
                } else { // Los dos últimos son gatos.
                    dibujarGato(animalX[animal], animalZ[animal], animalAngulo[animal], tiempoAnimacion);
                }
            }
        }
    }

    /** Hace caminar a los peatones de ida y vuelta: invierten su ángulo al alejarse 6 unidades de su origen. */
    private void actualizarPeatones(float deltaTime) {
        for (int peaton = 0; peaton < peatonX.length; peaton++) { // Recorre todos los peatones.
            float diferenciaX = autoX - peatonX[peaton]; // Separación horizontal entre el auto del jugador y el peatón.
            float diferenciaZ = autoZ - peatonZ[peaton]; // Separación en profundidad entre ambos.
            if (peatonAplastado[peaton] == 0 && diferenciaX * diferenciaX + diferenciaZ * diferenciaZ < 9.0f) { // Radio de colisión de 3 unidades.
                peatonAplastado[peaton] = 0.01f; // Inicia el aplastamiento.
            }
            if (peatonAplastado[peaton] > 0) { // Comprueba si el peatón está aplastado.
                peatonAplastado[peaton] += deltaTime; // Cuenta los segundos que lleva aplastado.
                if (peatonAplastado[peaton] > 3.0f) { // Pasados 3 segundos...
                    peatonAplastado[peaton] = 0; // ...se infla y vuelve a su forma normal.
                }
                continue; // Mientras está aplastado no se mueve.
            }
            peatonX[peaton] -= (float) Math.sin(peatonAngulo[peaton]) * 1.0f * deltaTime; // Avanza a 1 unidad por segundo según el frente (-sen).
            peatonZ[peaton] -= (float) Math.cos(peatonAngulo[peaton]) * 1.0f * deltaTime; // Avanza en Z según el frente (-cos); es casi cero.
            if (Math.abs(peatonX[peaton] - peatonOrigenX[peaton]) > 14.0f) { // Comprueba si llegó al extremo de su recorrido.
                peatonAngulo[peaton] += (float) Math.PI; // Da media vuelta para regresar.
                peatonX[peaton] = peatonOrigenX[peaton] + Math.signum(peatonX[peaton] - peatonOrigenX[peaton]) * 14.0f; // Lo deja justo en el extremo para no quedar fuera.
            }
        }
    }

    /** Dibuja una persona normal (sin aplastar); mantiene la firma que usan el personal de las estaciones y la gasolinera. */
    private void dibujarPeaton(float x, float z, float angulo, float tiempo, float r, float g, float b) {
        dibujarPeaton(x, z, angulo, tiempo, r, g, b, false); // Delega en la versión completa.
    }

    /** Dibuja una persona de 6 partes que camina; si estaAplastado, su altura se reduce al 10 % contra la acera. */
    private void dibujarPeaton(float x, float z, float angulo, float tiempo, float r, float g, float b, boolean estaAplastado) {
        float escalaY = 1.0f; // Factor de altura: 1 = normal.
        if (estaAplastado) { // Comprueba si el peatón fue atropellado.
            escalaY = 0.1f; // Reduce alturas y posiciones verticales al 10 %.
        }
        float seno = (float) Math.sin(angulo); // Seno de la orientación (el ángulo está en radianes, como en el resto del proyecto).
        float coseno = (float) Math.cos(angulo); // Coseno de la misma orientación.
        float frenteX = -seno; // Componente X del vector hacia donde mira la persona.
        float frenteZ = -coseno; // Componente Z del vector hacia donde mira la persona.
        float lateralX = coseno; // Componente X del eje que apunta a su derecha.
        float lateralZ = -seno; // Componente Z del eje que apunta a su derecha.
        float balanceo = (float) Math.sin(tiempo * 5.0f) * 0.12f; // Oscilación corta: así las extremidades siguen tocando el torso.
        float desvioX = frenteX * balanceo; // Balanceo descompuesto en X, siguiendo el frente.
        float desvioZ = frenteZ * balanceo; // Balanceo descompuesto en Z, siguiendo el frente.
        // Las alturas se escalan respecto a la acera (Y = 0,3): si se multiplicara la Y absoluta, el cuerpo quedaría hundido bajo el suelo.
        cajaGirada(x - lateralX * 0.1f + desvioX, 0.3f + 0.4f * escalaY, z - lateralZ * 0.1f + desvioZ, 0.17f, 0.8f * escalaY, 0.2f, 0.25f, 0.27f, 0.32f, angulo); // Pierna izquierda (suma el balanceo), pantalón gris azulado.
        cajaGirada(x + lateralX * 0.1f - desvioX, 0.3f + 0.4f * escalaY, z + lateralZ * 0.1f - desvioZ, 0.17f, 0.8f * escalaY, 0.2f, 0.25f, 0.27f, 0.32f, angulo); // Pierna derecha (resta el balanceo).
        cajaGirada(x, 0.3f + 1.125f * escalaY, z, 0.4f, 0.65f * escalaY, 0.24f, r, g, b, angulo); // Torso con el color de la ropa, justo sobre las piernas.
        cajaGirada(x - lateralX * 0.26f - desvioX * 0.8f, 0.3f + 1.14f * escalaY, z - lateralZ * 0.26f - desvioZ * 0.8f, 0.11f, 0.62f * escalaY, 0.14f, r, g, b, angulo); // Brazo izquierdo (resta: opuesto a la pierna izquierda).
        cajaGirada(x + lateralX * 0.26f + desvioX * 0.8f, 0.3f + 1.14f * escalaY, z + lateralZ * 0.26f + desvioZ * 0.8f, 0.11f, 0.62f * escalaY, 0.14f, r, g, b, angulo); // Brazo derecho (suma: opuesto a la pierna derecha).
        cajaGirada(x, 0.3f + 1.59f * escalaY, z, 0.28f, 0.28f * escalaY, 0.28f, 0.90f, 0.70f, 0.50f, angulo); // Cabeza color piel sobre el torso.
    }

    /** Hace caminar a cada animal hacia delante y lo gira hacia el centro del parque si se aleja demasiado. */
    private void actualizarMascotas(float deltaTime) {
        tiempoAnimacion += deltaTime; // Avanza el reloj de la animación de patas.
        for (int animal = 0; animal < 4; animal++) { // Recorre los cuatro animales.
            float velocidad = 1.2f; // Los perros caminan a 1,2 unidades por segundo.
            if (animal >= 2) { // Los gatos son más lentos.
                velocidad = 0.9f; // Velocidad de los gatos.
            }
            float siguienteX = animalX[animal] - (float) Math.sin(animalAngulo[animal]) * velocidad * deltaTime; // Propone la nueva X según el frente (-sen).
            float siguienteZ = animalZ[animal] - (float) Math.cos(animalAngulo[animal]) * velocidad * deltaTime; // Propone la nueva Z según el frente (-cos).
            float desvioX = siguienteX - animalCentroX[animal]; // Distancia horizontal propuesta al centro del parque.
            float desvioZ = siguienteZ - animalCentroZ[animal]; // Distancia en profundidad propuesta al centro del parque.
            if (Math.abs(desvioX) > 3 || Math.abs(desvioZ) > 3) { // Comprueba si llegaría al borde de su parcela.
                float haciaCentro = (float) Math.atan2(desvioX, desvioZ); // Ángulo cuyo frente apunta al centro del parque.
                animalAngulo[animal] = haciaCentro + ((float) Math.random() - 0.5f) * 1.2f; // Gira hacia dentro con una variación aleatoria.
            } else if (chocaConObstaculoParque(desvioX, desvioZ)) { // Comprueba si tocaría un árbol o el banco.
                animalAngulo[animal] += 1.57f + (float) Math.random() * 3.14f; // No avanza: gira entre 90 y 270 grados.
            } else { // El camino está libre.
                animalX[animal] = siguienteX; // Acepta el avance en X.
                animalZ[animal] = siguienteZ; // Acepta el avance en Z.
            }
        }
    }

    /** Indica si una posición relativa al centro del parque toca un tronco o el banco. */
    private boolean chocaConObstaculoParque(float desvioX, float desvioZ) {
        float[] troncos = {-2.5f, 2.5f}; // Los árboles están en las cuatro esquinas a ±2,5 del centro.
        for (float troncoX : troncos) { // Recorre las dos columnas de árboles.
            for (float troncoZ : troncos) { // Recorre las dos filas de árboles.
                float separacionX = desvioX - troncoX; // Distancia horizontal al tronco.
                float separacionZ = desvioZ - troncoZ; // Distancia en profundidad al tronco.
                if (separacionX * separacionX + separacionZ * separacionZ < 0.75f * 0.75f) { // Radio del tronco más medio cuerpo del animal.
                    return true; // Hay un árbol en el camino.
                }
            }
        }
        return Math.abs(desvioX) < 2.1f && desvioZ > -1.0f && desvioZ < 1.35f; // Rectángulo del banco con margen para el animal.
    }

    /** Transforma una pieza del espacio local de la mascota (frente = -Z) al mundo y la dibuja sobre el césped. */
    private void piezaMascota(float x, float z, float angulo, float escala, float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación del animal.
        float seno = (float) Math.sin(angulo); // Seno de la misma orientación.
        float mundoX = x + (coseno * lx + seno * lz) * escala; // Gira y escala la posición local en X.
        float mundoZ = z + (-seno * lx + coseno * lz) * escala; // Gira y escala la posición local en Z.
        cajaGirada(mundoX, 0.37f + ly * escala, mundoZ, sx * escala, sy * escala, sz * escala, r, g, b, angulo); // Césped en Y=0,37.
    }

    /** Dibuja un perro marrón que camina: cuerpo, cabeza, hocico, cola y cuatro patas alternadas. */
    private void dibujarPerro(float x, float z, float angulo, float tiempo) {
        float balanceo = (float) Math.sin(tiempo * 8) * 0.12f; // Desplazamiento adelante/atrás de las patas.
        float cola = (float) Math.sin(tiempo * 10) * 0.12f; // Movimiento lateral de la cola.
        piezaMascota(x, z, angulo, 1, 0, 1.0f, 0, 0.5f, 0.45f, 1.1f, 0.45f, 0.28f, 0.12f); // Cuerpo horizontal.
        piezaMascota(x, z, angulo, 1, 0, 1.25f, -0.75f, 0.36f, 0.36f, 0.4f, 0.40f, 0.24f, 0.10f); // Cabeza sobresaliente.
        piezaMascota(x, z, angulo, 1, 0, 1.17f, -1.0f, 0.2f, 0.16f, 0.2f, 0.15f, 0.10f, 0.06f); // Hocico oscuro.
        piezaMascota(x, z, angulo, 1, cola, 1.2f, 0.6f, 0.1f, 0.1f, 0.4f, 0.35f, 0.20f, 0.08f); // Cola que se mueve.
        piezaMascota(x, z, angulo, 1, -0.17f, 0.4f, -0.4f + balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata delantera izquierda.
        piezaMascota(x, z, angulo, 1, 0.17f, 0.4f, -0.4f - balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata delantera derecha (en contrafase).
        piezaMascota(x, z, angulo, 1, -0.17f, 0.4f, 0.4f - balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata trasera izquierda (en contrafase).
        piezaMascota(x, z, angulo, 1, 0.17f, 0.4f, 0.4f + balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata trasera derecha.
    }

    /** Dibuja un gato naranja y blanco, 30% más pequeño que el perro, con orejas puntiagudas y cola erguida. */
    private void dibujarGato(float x, float z, float angulo, float tiempo) {
        float escala = 0.7f; // El gato mide el 70 % del perro.
        float balanceo = (float) Math.sin(tiempo * 9) * 0.12f; // Desplazamiento adelante/atrás de las patas.
        float cola = (float) Math.sin(tiempo * 6) * 0.1f; // Balanceo lateral de la punta de la cola.
        piezaMascota(x, z, angulo, escala, 0, 1.0f, 0, 0.45f, 0.4f, 1.0f, 0.92f, 0.52f, 0.14f); // Cuerpo naranja.
        piezaMascota(x, z, angulo, escala, 0, 1.25f, -0.7f, 0.38f, 0.34f, 0.34f, 0.95f, 0.58f, 0.18f); // Cabeza redondeada.
        piezaMascota(x, z, angulo, escala, -0.12f, 1.52f, -0.72f, 0.1f, 0.18f, 0.1f, 0.85f, 0.45f, 0.10f); // Oreja izquierda puntiaguda.
        piezaMascota(x, z, angulo, escala, 0.12f, 1.52f, -0.72f, 0.1f, 0.18f, 0.1f, 0.85f, 0.45f, 0.10f); // Oreja derecha puntiaguda.
        piezaMascota(x, z, angulo, escala, cola, 1.45f, 0.55f, 0.08f, 0.7f, 0.08f, 0.92f, 0.52f, 0.14f); // Cola erguida que se mece.
        piezaMascota(x, z, angulo, escala, -0.15f, 0.35f, -0.35f + balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata delantera izquierda blanca.
        piezaMascota(x, z, angulo, escala, 0.15f, 0.35f, -0.35f - balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata delantera derecha (en contrafase).
        piezaMascota(x, z, angulo, escala, -0.15f, 0.35f, 0.35f - balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata trasera izquierda (en contrafase).
        piezaMascota(x, z, angulo, escala, 0.15f, 0.35f, 0.35f + balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata trasera derecha.
    }

    /** Auto estacionado junto al surtidor izquierdo con una persona que carga combustible (manguera y pantalla parpadeante). */
    private void dibujarCargaCombustible(float x, float z) {
        dibujarBot(x - 1.2f, z + 1.9f, (float) Math.PI / 2, 0.85f, 0.15f, 0.15f); // Auto rojo estacionado de lado, bajo el techo.
        caja(x - 1.2f, 0.8f, z + 0.66f, 0.06f, 0.06f, 0.83f, 0.05f, 0.05f, 0.05f); // Manguera negra del surtidor al costado del auto.
        boolean pantallaEncendida = (int) (tiempoAnimacion * 2) % 2 == 0; // Alterna el indicador del surtidor dos veces por segundo.
        entero("uEmision", pantallaEncendida ? 1 : 0); // La pantalla brilla en las fases pares y se apaga en las impares.
        caja(x - 1.2f, 1.5f, z + 0.27f, 0.4f, 0.25f, 0.04f, 0.2f, 1, 0.3f); // Pantalla verde del surtidor que parpadea mientras carga.
        entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
        float balanceo = (float) Math.sin(tiempoAnimacion * 1.5f) * 0.15f; // Pequeño giro del cuerpo mientras espera.
        dibujarPeaton(x - 0.3f, z + 0.75f, (float) Math.PI / 2 + balanceo, 0, 0.9f, 0.5f, 0.1f); // Persona de pie junto a la manguera, mirando al auto.
    }

    /** Dibuja un hidrante rojo sobre la acera: cuerpo, tapa más ancha y válvula gris (3 cajas). */
    private void dibujarHidrante(float x, float z) {
        caja(x, 0.65f, z, 0.4f, 0.7f, 0.4f, 0.80f, 0.10f, 0.08f); // Cuerpo rojo (la acera está en Y=0,3).
        caja(x, 1.1f, z, 0.55f, 0.2f, 0.55f, 0.65f, 0.06f, 0.05f); // Tapa roja oscura, un poco más ancha.
        caja(x + 0.28f, 0.8f, z, 0.2f, 0.2f, 0.2f, 0.60f, 0.62f, 0.65f); // Válvula gris para la manguera, a un lado.
    }

    /** Dibuja el buzón azul clásico de correos de EE. UU.: patas, cuerpo, tapa escalonada, ranura y calcomanía blanca. */
    private void dibujarBuzon(float x, float z) {
        float[] lados = {-0.25f, 0.25f}; // Posiciones de las patas a cada lado del centro.
        for (float pataX : lados) { // Recorre las patas izquierda y derecha.
            for (float pataZ : lados) { // Recorre las patas delantera y trasera.
                caja(x + pataX, 0.4f, z + pataZ * 0.7f, 0.12f, 0.2f, 0.12f, 0.08f, 0.24f, 0.65f); // Pata azul corta en cada esquina de la base.
            }
        }
        caja(x, 1.0f, z, 0.7f, 1.0f, 0.55f, 0.10f, 0.30f, 0.80f); // Cuerpo principal azul, alto y robusto, sobre las patas.
        caja(x, 1.625f, z, 0.6f, 0.25f, 0.45f, 0.10f, 0.30f, 0.80f); // Tapa más angosta y menos profunda: efecto de techo curvo escalonado.
        caja(x, 1.35f, z + 0.28f, 0.4f, 0.05f, 0.04f, 0.10f, 0.10f, 0.12f); // Ranura gris oscura para las cartas en la parte frontal superior.
        caja(x + 0.355f, 1.0f, z, 0.02f, 0.3f, 0.3f, 0.95f, 0.95f, 0.95f); // Calcomanía blanca casi plana pegada al lateral (águila).
    }

    /** Dibuja un montículo de basura sobre el asfalto con una bolsa negra, una caja de cartón y una lata. */
    private void dibujarBasura(float x, float z) {
        caja(x, 0.3f, z, 0.8f, 0.6f, 0.7f, 0.05f, 0.05f, 0.06f); // Bolsa negra de basura.
        caja(x + 0.6f, 0.2f, z + 0.25f, 0.5f, 0.4f, 0.5f, 0.55f, 0.38f, 0.20f); // Caja de cartón marrón al lado.
        caja(x - 0.4f, 0.12f, z - 0.45f, 0.2f, 0.24f, 0.2f, 0.60f, 0.62f, 0.65f); // Lata gris tirada delante.
    }

    /** Avanza cada bot hacia la siguiente esquina de su ruta y gira 90 grados al llegar. */
    private void actualizarBots(float deltaTime) {
        for (int bot = 0; bot < RUTAS.length; bot++) { // Recorre los tres autos autónomos.
            if (botChoque[bot] > 0) { // Comprueba si el bot está en medio de un choque.
                botChoque[bot] -= deltaTime; // Consume el tiempo del choque.
                if (botChoque[bot] < 1.1f) { // Tras una breve espera detenido, empieza a retroceder.
                    botX[bot] += (float) Math.sin(botAngulo[bot]) * 3 * deltaTime; // Retrocede en X (opuesto al frente -sen).
                    botZ[bot] += (float) Math.cos(botAngulo[bot]) * 3 * deltaTime; // Retrocede en Z (opuesto al frente -cos).
                }
                continue; // Mientras dura el choque no sigue su ruta.
            }
            float objetivoX = RUTAS[bot][botObjetivo[bot]][0]; // Lee la X de la esquina objetivo.
            float objetivoZ = RUTAS[bot][botObjetivo[bot]][1]; // Lee la Z de la esquina objetivo.
            float diferenciaX = objetivoX - botX[bot]; // Calcula la separación horizontal al objetivo.
            float diferenciaZ = objetivoZ - botZ[bot]; // Calcula la separación en profundidad al objetivo.
            float distancia = (float) Math.sqrt(diferenciaX * diferenciaX + diferenciaZ * diferenciaZ); // Mide la distancia restante.
            float paso = botVelocidad[bot] * deltaTime; // Distancia que avanza en este cuadro.
            if (distancia <= paso) { // Comprueba si alcanza la esquina en este cuadro.
                botX[bot] = objetivoX; // Se coloca exactamente sobre la esquina.
                botZ[bot] = objetivoZ; // Evita que el error se acumule y se salga del asfalto.
                botObjetivo[bot] = (botObjetivo[bot] + 1) % RUTAS[bot].length; // Pasa a la siguiente esquina y cierra el circuito.
            } else { // Todavía no llega: avanza en línea recta.
                botX[bot] += diferenciaX / distancia * paso; // Avanza en X siguiendo la dirección normalizada.
                botZ[bot] += diferenciaZ / distancia * paso; // Avanza en Z siguiendo la dirección normalizada.
                botAngulo[bot] = (float) Math.atan2(-diferenciaX, -diferenciaZ); // Orienta el frente (-sen, -cos) hacia el movimiento.
            }
        }
    }

    /** Si el jugador toca a un bot, ambos se detienen y el bot espera un instante y retrocede. */
    private void resolverChoqueBots(float previoX, float previoZ) {
        float radioChoque = 3.0f; // Suma aproximada de los radios de los dos autos.
        for (int bot = 0; bot < RUTAS.length; bot++) { // Revisa cada bot.
            float diferenciaX = autoX - botX[bot]; // Separación horizontal actual entre jugador y bot.
            float diferenciaZ = autoZ - botZ[bot]; // Separación en profundidad actual.
            float distanciaCuadrada = diferenciaX * diferenciaX + diferenciaZ * diferenciaZ; // Distancia al cuadrado, sin raíz.
            if (distanciaCuadrada < radioChoque * radioChoque) { // Comprueba si los autos se tocan.
                velocidad = 0; // El auto del jugador se detiene.
                if (botChoque[bot] <= 0) { // Evita reiniciar el choque si ya está en curso.
                    botChoque[bot] = 1.5f; // Inicia el choque: 0,4 s detenido y luego 1,1 s de retroceso.
                }
                float previaX = previoX - botX[bot]; // Separación X que había antes de mover al jugador.
                float previaZ = previoZ - botZ[bot]; // Separación Z que había antes de mover al jugador.
                if (distanciaCuadrada < previaX * previaX + previaZ * previaZ) { // Solo bloquea si el jugador se estaba acercando.
                    autoX = previoX; // Devuelve al jugador a su posición anterior para que no traspase al bot.
                    autoZ = previoZ; // Misma corrección en profundidad.
                }
            }
        }
    }

    /** Transforma una pieza del espacio local de un vehículo (frente = -Z) al mundo y la dibuja sobre el asfalto. */
    private void piezaVehiculo(float x, float z, float angulo, float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación del vehículo.
        float seno = (float) Math.sin(angulo); // Seno de la misma orientación.
        cajaGirada(x + coseno * lx + seno * lz, ly, z - seno * lx + coseno * lz, sx, sy, sz, r, g, b, angulo); // ly es la altura absoluta sobre el asfalto.
    }

    /** Dibuja la patrulla: carrocería blanca con franja azul oscura y sirenas roja y azul que alternan. */
    private void dibujarAutoPolicia(float x, float z, float angulo, float tiempo) {
        piezaVehiculo(x, z, angulo, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.92f, 0.92f, 0.95f); // Carrocería blanca.
        piezaVehiculo(x, z, angulo, 0, 0.5f, 0, 1.7f, 0.2f, 2.65f, 0.08f, 0.12f, 0.35f); // Franja azul oscura a lo largo de la carrocería.
        piezaVehiculo(x, z, angulo, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.15f, 0.20f, 0.30f); // Cabina oscura.
        piezaVehiculo(x, z, angulo, -0.88f, 0.38f, 0, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f); // Ruedas del lado izquierdo.
        piezaVehiculo(x, z, angulo, 0.88f, 0.38f, 0, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f); // Ruedas del lado derecho.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla.
        entero("uEmision", rojoEncendido ? 1 : 0); // La roja brilla en las fases pares.
        piezaVehiculo(x, z, angulo, -0.4f, 1.5f, 0.12f, 0.45f, 0.18f, 0.35f, 0.9f, 0.1f, 0.1f); // Sirena roja sobre el techo.
        entero("uEmision", rojoEncendido ? 0 : 1); // La azul brilla cuando la roja se apaga.
        piezaVehiculo(x, z, angulo, 0.4f, 1.5f, 0.12f, 0.45f, 0.18f, 0.35f, 0.1f, 0.25f, 0.95f); // Sirena azul sobre el techo.
        entero("uEmision", 0); // Devuelve la iluminación normal.
    }

    /** Dibuja el camión de bomberos: chasis largo rojo, cabina, escalera gris y sirena. */
    private void dibujarCamionBomberos(float x, float z, float angulo, float tiempo) {
        piezaVehiculo(x, z, angulo, 0, 0.9f, 0.5f, 1.9f, 0.9f, 3.2f, 0.80f, 0.10f, 0.08f); // Caja trasera roja.
        piezaVehiculo(x, z, angulo, 0, 1.0f, -1.7f, 1.9f, 1.1f, 1.3f, 0.85f, 0.12f, 0.10f); // Cabina delantera roja, más alta.
        piezaVehiculo(x, z, angulo, 0, 1.2f, -1.9f, 1.6f, 0.45f, 0.1f, 0.25f, 0.3f, 0.35f); // Parabrisas oscuro.
        piezaVehiculo(x, z, angulo, 0, 1.52f, 0.5f, 0.5f, 0.15f, 3f, 0.70f, 0.72f, 0.75f); // Escalera gris sobre el techo de la caja.
        piezaVehiculo(x, z, angulo, -1.0f, 0.45f, 0, 0.3f, 0.9f, 3.6f, 0.06f, 0.07f, 0.08f); // Ruedas del lado izquierdo.
        piezaVehiculo(x, z, angulo, 1.0f, 0.45f, 0, 0.3f, 0.9f, 3.6f, 0.06f, 0.07f, 0.08f); // Ruedas del lado derecho.
        boolean encendida = (int) (tiempo * 4) % 2 == 0; // Hace parpadear la sirena.
        entero("uEmision", encendida ? 1 : 0); // La sirena brilla en las fases pares.
        piezaVehiculo(x, z, angulo, 0, 1.7f, -1.7f, 1.0f, 0.2f, 0.3f, 1, 0.2f, 0.1f); // Sirena roja sobre la cabina.
        entero("uEmision", 0); // Devuelve la iluminación normal.
    }

    /** Dibuja la ambulancia: caja blanca con cruces rojas a los lados y sirenas roja y azul. */
    private void dibujarAmbulancia(float x, float z, float angulo, float tiempo) {
        piezaVehiculo(x, z, angulo, 0, 1.0f, 0.4f, 1.9f, 1.2f, 2.8f, 0.95f, 0.95f, 0.97f); // Caja blanca del compartimiento médico.
        piezaVehiculo(x, z, angulo, 0, 0.85f, -1.5f, 1.8f, 0.8f, 1.1f, 0.90f, 0.90f, 0.93f); // Cabina blanca más baja al frente.
        piezaVehiculo(x, z, angulo, 0, 1.0f, -2.0f, 1.5f, 0.4f, 0.1f, 0.2f, 0.28f, 0.35f); // Parabrisas oscuro.
        piezaVehiculo(x, z, angulo, -0.97f, 1.1f, 0.4f, 0.04f, 0.2f, 0.8f, 0.85f, 0.1f, 0.1f); // Cruz roja izquierda: brazo horizontal.
        piezaVehiculo(x, z, angulo, -0.97f, 1.1f, 0.4f, 0.04f, 0.8f, 0.2f, 0.85f, 0.1f, 0.1f); // Cruz roja izquierda: brazo vertical.
        piezaVehiculo(x, z, angulo, 0.97f, 1.1f, 0.4f, 0.04f, 0.2f, 0.8f, 0.85f, 0.1f, 0.1f); // Cruz roja derecha: brazo horizontal.
        piezaVehiculo(x, z, angulo, 0.97f, 1.1f, 0.4f, 0.04f, 0.8f, 0.2f, 0.85f, 0.1f, 0.1f); // Cruz roja derecha: brazo vertical.
        piezaVehiculo(x, z, angulo, -0.9f, 0.4f, 0, 0.26f, 0.6f, 3.2f, 0.06f, 0.07f, 0.08f); // Ruedas del lado izquierdo.
        piezaVehiculo(x, z, angulo, 0.9f, 0.4f, 0, 0.26f, 0.6f, 3.2f, 0.06f, 0.07f, 0.08f); // Ruedas del lado derecho.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla.
        entero("uEmision", rojoEncendido ? 1 : 0); // La roja brilla en las fases pares.
        piezaVehiculo(x, z, angulo, -0.5f, 1.7f, -1.4f, 0.5f, 0.2f, 0.3f, 0.9f, 0.1f, 0.1f); // Sirena roja sobre la cabina.
        entero("uEmision", rojoEncendido ? 0 : 1); // La azul brilla cuando la roja se apaga.
        piezaVehiculo(x, z, angulo, 0.5f, 1.7f, -1.4f, 0.5f, 0.2f, 0.3f, 0.1f, 0.25f, 0.95f); // Sirena azul sobre la cabina.
        entero("uEmision", 0); // Devuelve la iluminación normal.
    }

    /** Dibuja un auto simplificado de color propio con 5 llamadas a cajaGirada(). */
    private void dibujarBot(float x, float z, float angulo, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación del bot.
        float seno = (float) Math.sin(angulo); // Seno de la misma orientación.
        cajaGirada(x, 0.65f, z, 1.65f, 0.55f, 2.6f, r, g, b, angulo); // Carrocería del color del bot.
        cajaGirada(x + seno * 0.12f, 1.12f, z + coseno * 0.12f, 1.3f, 0.55f, 1.25f, 0.20f, 0.25f, 0.30f, angulo); // Cabina oscura, un poco hacia atrás.
        cajaGirada(x + coseno * -0.88f, 0.38f, z - seno * -0.88f, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f, angulo); // Fila de ruedas del lado izquierdo.
        cajaGirada(x + coseno * 0.88f, 0.38f, z - seno * 0.88f, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f, angulo); // Fila de ruedas del lado derecho.
        cajaGirada(x - seno * 1.32f, 0.68f, z - coseno * 1.32f, 1.2f, 0.2f, 0.07f, 1, 0.95f, 0.65f, angulo); // Franja de faros en el frente.
    }

    /** Dibuja un coleccionable: marca amarilla plana sobre el asfalto y un cubo amarillo que gira y flota encima. */
    private void dibujarMoneda(float x, float z, float angulo, float tiempo) {
        caja(x, 0.05f, z, 6.0f, 0.1f, 6.0f, 1.0f, 0.8f, 0.0f); // Marca cuadrada amarilla pintada sobre la calle.
        cajaGirada(x, 2.5f + (float) Math.sin(tiempo * 3.0f) * 0.5f, z, 2.0f, 2.0f, 2.0f, 1.0f, 0.8f, 0.0f, angulo); // Cubo amarillo sólido que gira y sube y baja.
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
        if (!vistaMapa) { // Evita añadir una baliza tridimensional al mapa pequeño.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempo); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                switch (tipo) { // Elige la decoración según el tipo de parcela.
                    case 1 -> { // Edificio normal: la caja principal ya la dibuja AppCiudad.
                        float altura = 5 + (fila * 3 + columna * 7) % 9; // Recupera la misma altura calculada en AppCiudad.
                        dibujarVentanas(x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                    }
                    case 2 -> dibujarParque(x, z); // Parque con árboles y un banco.
                    case 3 -> dibujarFuente(x, z); // Fuente central de piedra con agua.
                    case 4 -> { // Comisaría con sirenas en el techo.
                        dibujarPolicia(x, z); // Dibuja el edificio.
                        dibujarPeaton(x + 3, z + 4.4f, (float) Math.PI, 0, 0.1f, 0.2f, 0.8f); // Policía de azul inmóvil frente a la entrada (tiempo 0: piernas quietas).
                        dibujarPeaton(x - 3, z + 4.4f, (float) Math.PI, 0, 0.05f, 0.12f, 0.55f); // Segundo policía, de azul más oscuro, también estático.
                    }
                    case 5 -> { // Estación roja con portones de garaje.
                        dibujarBomberos(x, z); // Dibuja el edificio.
                        dibujarPeaton(x + 4.2f, z + 4.4f, (float) Math.PI, 0, 0.8f, 0.1f, 0.1f); // Bombero de rojo inmóvil en la esquina de la acera frontal.
                        dibujarPeaton(x - 4.2f, z + 4.4f, (float) Math.PI, 0, 0.9f, 0.5f, 0.1f); // Segundo bombero con chaqueta naranja, también estático.
                    }
                    case 6 -> { // Techo flotante con surtidores.
                        dibujarGasolinera(x, z); // Dibuja la estructura de la gasolinera.
                        dibujarCargaCombustible(x, z); // Añade el auto estacionado y la persona que carga combustible.
                    }
                    case 0 -> { // Calle: puede llevar un montículo de basura junto a la acera.
                        boolean tramoHorizontal = fila % 3 == 0 && columna % 3 != 0; // Calle que corre a lo largo de X entre dos cruces.
                        boolean tramoVertical = columna % 3 == 0 && fila % 3 != 0; // Calle que corre a lo largo de Z entre dos cruces.
                        boolean hayBasura = (fila * 7 + columna * 13) % 23 == 5; // Selección fija (~7 % de los tramos) que no cambia entre cuadros.
                        if (hayBasura && tramoHorizontal) { // Basura en el borde sur de una calle horizontal.
                            dibujarBasura(x, z + 4);
                        } else if (hayBasura && tramoVertical) { // Basura en el borde este de una calle vertical.
                            dibujarBasura(x + 4, z);
                        }
                    }
                    default -> { } // Los demás tipos ya tienen su propio método de decoración.
                }
                if (tipo == 1 || tipo == 2) { // Solo las parcelas de edificio o parque llevan mobiliario urbano en su acera.
                    int lado = 0; // 0 = sin calle a los lados; 1 = calle al este; -1 = calle al oeste.
                    if (columna + 1 < MAPA[fila].length && MAPA[fila][columna + 1] == 0) { // Comprueba si hay calle al este.
                        lado = 1; // Usa el borde este de la acera.
                    } else if (columna > 0 && MAPA[fila][columna - 1] == 0) { // Si no, comprueba si hay calle al oeste.
                        lado = -1; // Usa el borde oeste de la acera.
                    }
                    int azar = (fila * 37 + columna * 53) % 100; // Número fijo por parcela (0-99): evita que cambie en cada cuadro como haría Math.random().
                    float desvioZ = 1.5f; // Desplazamiento a lo largo de la acera, lejos de las farolas y semáforos.
                    if (azar % 2 != 0) { // Alterna el lado según la parcela.
                        desvioZ = -1.5f; // Usa la mitad norte de la acera.
                    }
                    if (lado != 0 && azar < 15) { // Aproximadamente el 15 % de las parcelas con calle tienen hidrante.
                        dibujarHidrante(x + lado * 4.4f, z + desvioZ); // Lo pega al borde de la acera junto a la calle.
                    } else if (lado != 0 && azar < 30) { // Otro 15 % tiene buzón.
                        dibujarBuzon(x + lado * 4.4f, z + desvioZ); // Lo coloca en el mismo borde de la acera.
                    } else if (tipo == 1 && lado != 0 && azar % 10 == 3) { // Cerca del 10 % de los edificios restantes llevan una señal.
                        dibujarSenal(x + lado * 4.4f, z + desvioZ * 1.9f, (float) Math.PI / 2); // Panel orientado hacia la calle, lejos de farolas e hidrantes.
                    }
                }
                if (tipo == 0 && fila >= 3 && fila <= 9 && fila % 3 == 0 && columna >= 3 && columna <= 9 && columna % 3 == 0) { // Intersección: calle cuya fila y columna son 3, 6 o 9.
                    dibujarCruce(x, z); // Dibuja los 4 pasos de cebra y los 4 semáforos del cruce.
                }
            }
        }
    }

    /** Construye una fuente de piedra con agua azul y un surtidor animado (5 cajas). */
    private void dibujarFuente(float x, float z) {
        caja(x, 0.82f, z, 7, 0.9f, 7, 0.55f, 0.55f, 0.58f); // Base de piedra que forma el borde de la fuente.
        if (noche) { // De noche el agua brilla suavemente, como las ventanas.
            entero("uEmision", 1); // Evita que la iluminación oscurezca el agua.
        }
        caja(x, 1.3f, z, 5.8f, 0.1f, 5.8f, 0.15f, 0.45f, 0.85f); // Agua azul que sobresale un poco del borde.
        entero("uEmision", 0); // Restablece el material normal.
        caja(x, 2.0f, z, 1, 1.4f, 1, 0.50f, 0.50f, 0.53f); // Columna central de piedra.
        caja(x, 2.8f, z, 2.2f, 0.25f, 2.2f, 0.58f, 0.58f, 0.60f); // Taza superior de la fuente.
        float alturaChorro = 1.2f + (float) Math.sin(tiempo * 3) * 0.25f; // Hace subir y bajar el surtidor con el tiempo.
        entero("uEmision", 1); // El chorro se ve brillante de día y de noche.
        caja(x, 2.93f + alturaChorro / 2, z, 0.3f, alturaChorro, 0.3f, 0.45f, 0.75f, 1); // Surtidor de agua celeste.
        entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
    }

    /** Construye la comisaría: edificio bajo y ancho con pórtico, placa y sirenas que alternan. */
    private void dibujarPolicia(float x, float z) {
        caja(x, 2.0f, z, 8.4f, 3.4f, 7.4f, 0.28f, 0.30f, 0.38f); // Cuerpo bajo gris azulado (unos dos pisos) sobre la acera.
        caja(x, 3.85f, z, 8.8f, 0.3f, 7.8f, 0.16f, 0.18f, 0.25f); // Cubierta ligeramente más ancha y oscura.
        caja(x, 1.3f, z + 3.75f, 1.8f, 2, 0.12f, 0.08f, 0.12f, 0.25f); // Puerta de entrada azul oscuro en la fachada sur.
        caja(x, 2.65f, z + 4.3f, 3.2f, 0.2f, 1.2f, 0.10f, 0.15f, 0.40f); // Toldo o pórtico que sobresale sobre la entrada.
        caja(x, 3.15f, z + 3.76f, 1.2f, 0.6f, 0.08f, 0.95f, 0.80f, 0.15f); // Placa amarilla que simula el escudo en la fachada.
        caja(x - 2.8f, 1.9f, z + 3.72f, 1.4f, 1, 0.08f, 0.55f, 0.70f, 0.85f); // Ventana izquierda de la fachada.
        caja(x + 2.8f, 1.9f, z + 3.72f, 1.4f, 1, 0.08f, 0.55f, 0.70f, 0.85f); // Ventana derecha de la fachada.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla cuatro veces por segundo.
        entero("uEmision", rojoEncendido ? 1 : 0); // La sirena roja brilla en las fases pares.
        caja(x - 1.2f, 4.3f, z, 1, 0.6f, 1, 0.9f, 0.1f, 0.1f); // Sirena roja sobre el techo.
        entero("uEmision", rojoEncendido ? 0 : 1); // La sirena azul brilla cuando la roja se apaga.
        caja(x + 1.2f, 4.3f, z, 1, 0.6f, 1, 0.1f, 0.25f, 0.95f); // Sirena azul sobre el techo.
        entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
    }

    /** Construye la estación de bomberos: edificio bajo y ancho con dos portones, techo saliente y torre. */
    private void dibujarBomberos(float x, float z) {
        caja(x, 2.1f, z, 8.8f, 3.6f, 7.4f, 0.80f, 0.12f, 0.10f); // Cuerpo bajo rojo llamativo (unos dos pisos).
        caja(x, 4.05f, z, 9.6f, 0.3f, 8.2f, 0.45f, 0.06f, 0.06f); // Techo más ancho que sobresale de las paredes.
        caja(x - 2.2f, 1.5f, z + 3.75f, 3.4f, 2.4f, 0.12f, 0.18f, 0.18f, 0.20f); // Primer portón gris oscuro del garaje.
        caja(x + 2.2f, 1.5f, z + 3.75f, 3.4f, 2.4f, 0.12f, 0.18f, 0.18f, 0.20f); // Segundo portón para otro camión.
        caja(x, 3.2f, z + 3.72f, 8, 0.35f, 0.06f, 0.90f, 0.90f, 0.88f); // Franja blanca sobre los portones.
        caja(x + 3.3f, 5.6f, z - 2.4f, 1.4f, 2.8f, 1.4f, 0.60f, 0.10f, 0.10f); // Torre pequeña de secado de mangueras.
        caja(x + 3.3f, 7.1f, z - 2.4f, 1.7f, 0.2f, 1.7f, 0.30f, 0.05f, 0.05f); // Remate oscuro de la torre.
    }

    /** Construye la gasolinera: techo flotante sobre dos columnas con dos surtidores (6 cajas). */
    private void dibujarGasolinera(float x, float z) {
        caja(x, 4.2f, z, 8, 0.35f, 6, 0.92f, 0.92f, 0.94f); // Techo plano flotante blanco.
        caja(x, 4.45f, z, 8.2f, 0.15f, 6.2f, 0.90f, 0.70f, 0.10f); // Banda amarilla sobre el borde del techo.
        caja(x - 3, 2.25f, z, 0.3f, 3.9f, 0.3f, 0.35f, 0.37f, 0.40f); // Columna fina izquierda.
        caja(x + 3, 2.25f, z, 0.3f, 3.9f, 0.3f, 0.35f, 0.37f, 0.40f); // Columna fina derecha.
        caja(x - 1.2f, 1.1f, z, 0.7f, 1.6f, 0.5f, 0.85f, 0.15f, 0.10f); // Surtidor rojo izquierdo.
        caja(x + 1.2f, 1.1f, z, 0.7f, 1.6f, 0.5f, 0.15f, 0.55f, 0.20f); // Surtidor verde derecho.
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
        }
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada norte.
                caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada sur.
                caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada oeste.
                caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada este.
            }
        }
        entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Dibuja en una intersección 4 pasos de cebra (uno por entrada) y 4 semáforos en sus esquinas. */
    private void dibujarCruce(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) { // Siete franjas por cada paso de cebra.
            caja(x + desplazamiento, 0.045f, z - 7.5f, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Entrada norte: se cruza a lo largo de X.
            caja(x + desplazamiento, 0.045f, z + 7.5f, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Entrada sur: se cruza a lo largo de X.
            caja(x - 7.5f, 0.045f, z + desplazamiento, 2, 0.04f, 0.45f, 0.85f, 0.87f, 0.83f); // Entrada oeste: se cruza a lo largo de Z.
            caja(x + 7.5f, 0.045f, z + desplazamiento, 2, 0.04f, 0.45f, 0.85f, 0.87f, 0.83f); // Entrada este: se cruza a lo largo de Z.
        }

        int faseNorteSur = (int) (tiempo % 12); // Ciclo de 12 s para el tráfico que circula en dirección Z.
        int faseEsteOeste = (int) ((tiempo + 6) % 12); // El tráfico en X va desfasado medio ciclo respecto al de Z.
        for (int signoX = -1; signoX <= 1; signoX += 2) { // Elige la esquina oeste (-1) o este (+1).
            for (int signoZ = -1; signoZ <= 1; signoZ += 2) { // Elige la esquina norte (-1) o sur (+1).
                float postesX = x + signoX * 5.6f; // Coloca el poste sobre la acera de la manzana diagonal.
                float postesZ = z + signoZ * 5.6f; // Coloca el poste en la misma esquina, en profundidad.
                caja(postesX, 1.7f, postesZ, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
                caja(postesX, 3.1f, postesZ, 0.5f, 1.2f, 0.5f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa con una cara hacia cada calle.
                for (int eje = 0; eje < 2; eje++) { // Eje 0: cara que mira a Z; eje 1: cara que mira a X.
                    int fase = faseNorteSur; // Parte de la fase del tráfico en Z.
                    if (eje == 1) { // Comprueba si se dibuja la cara orientada a X.
                        fase = faseEsteOeste; // Usa la fase desfasada del otro sentido.
                    }
                    for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
                        boolean encendida = false; // Parte de una bombilla apagada.
                        if (indice == 0) { // Selecciona la bombilla roja.
                            encendida = fase < 5; // Mantiene rojo durante los primeros cinco segundos.
                        } else if (indice == 1) { // Selecciona la bombilla amarilla.
                            encendida = fase >= 10; // Mantiene amarillo en los últimos dos segundos del ciclo.
                        } else { // Selecciona la bombilla verde.
                            encendida = fase >= 5 && fase < 10; // Mantiene verde durante los cinco segundos intermedios.
                        }
                        float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
                        entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
                        if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                            brillo = 1; // Usa intensidad completa para su color.
                            entero("uEmision", 1); // Hace que la bombilla se vea encendida.
                        }
                        float rojo = 0; // Componente roja inicialmente ausente.
                        float verde = 0; // Componente verde inicialmente ausente.
                        if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                            rojo = brillo; // Añade rojo con la intensidad elegida.
                        }
                        if (indice > 0) { // Amarillo y verde necesitan componente verde.
                            verde = brillo; // Añade verde; rojo más verde produce amarillo.
                        }
                        float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
                        if (eje == 0) { // Cara que mira hacia el centro del cruce a lo largo de Z.
                            caja(postesX, altura, postesZ - signoZ * 0.27f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Bombilla en la cara norte o sur.
                        } else { // Cara que mira hacia el centro del cruce a lo largo de X.
                            caja(postesX - signoX * 0.27f, altura, postesZ, 0.06f, 0.25f, 0.28f, rojo, verde, 0.02f); // Bombilla en la cara oeste o este.
                        }
                    }
                }
            }
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (mostrarMapa) { // Comprueba si el usuario no ocultó el minimapa con M.
            dibujarMinimapa(); // Dibuja el segundo pase desde arriba.
        }
        dibujarHUD(velocidad, faros); // El panel de velocidad y luces se dibuja siempre, encima de todo.
    }

    /** Dibuja la misma ciudad desde arriba en un recuadro de la esquina superior derecha. */
    private void dibujarMinimapa() {
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja una caja plana en el panel 2D: posición y tamaño como fracciones (0 a 1) del viewport, con origen abajo a la izquierda. */
    private void cajaHUD(float fx, float fy, float fw, float fh, float r, float g, float b, float nivel) {
        float mundoX = (fx + fw / 2) * 2 * LIMITE_HUD - LIMITE_HUD; // Convierte el centro horizontal al rango del shader (-67 a 67).
        float mundoZ = LIMITE_HUD - (fy + fh / 2) * 2 * LIMITE_HUD; // El shader invierte Z: la parte superior de la pantalla es -Z.
        caja(mundoX, nivel, mundoZ, fw * 2 * LIMITE_HUD, 0.1f, fh * 2 * LIMITE_HUD, r, g, b); // La altura (nivel) decide qué caja queda por encima.
    }

    /** Dibuja el panel inferior izquierdo con barra de velocidad e indicadores de luces; en pausa añade el icono de pausa. */
    private void dibujarHUD(float velocidad, boolean luces) {
        int anchoPanel = Math.min(240, ancho / 2); // Ancho del panel, limitado en ventanas pequeñas.
        int altoPanel = Math.min(100, alto / 4); // Alto del panel, limitado en ventanas pequeñas.
        int margen = Math.min(18, alto / 20); // Separación respecto a los bordes de la ventana.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para borrar solo el panel.
        glScissor(margen - 3, margen - 3, anchoPanel + 6, altoPanel + 6); // Panel más un borde de tres píxeles.
        glClearColor(0.55f, 0.58f, 0.62f, 1); // Color gris claro del marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Pinta el marco y reinicia la profundidad.
        glScissor(margen, margen, anchoPanel, altoPanel); // Reduce el recorte al interior del panel.
        glClearColor(0.12f, 0.12f, 0.14f, 1); // Gris oscuro del fondo del panel.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Pinta el fondo del panel.
        glViewport(margen, margen, anchoPanel, altoPanel); // Redirige la proyección 2D al panel.
        entero("uMapa", 1); // Reutiliza la proyección ortográfica del minimapa y los colores directos sin iluminación.
        try { // Garantiza que el estado de dibujo se restaure aunque algo falle.
            cajaHUD(0.05f, 0.50f, 0.90f, 0.30f, 0.30f, 0.30f, 0.32f, 1); // Marco gris de la barra de velocidad.
            float proporcion = Math.min(Math.abs(velocidad) / 16f, 1f); // 16 es la velocidad máxima de avance.
            float rojo = 0.15f; // Color verde por defecto cuando se avanza.
            float verde = 0.85f; // Componente verde del color de avance.
            if (velocidad < 0) { // En reversa la barra se vuelve roja.
                rojo = 0.90f; // Componente roja de la reversa.
                verde = 0.15f; // Componente verde de la reversa.
            }
            cajaHUD(0.06f, 0.53f, 0.88f * proporcion, 0.24f, rojo, verde, 0.15f, 2); // Barra cuyo ancho depende de |velocidad|.
            if (luces) { // Indicador de los faros del auto.
                cajaHUD(0.05f, 0.12f, 0.12f, 0.28f, 1.0f, 0.9f, 0.1f, 2); // Amarillo brillante: faros encendidos.
            } else {
                cajaHUD(0.05f, 0.12f, 0.12f, 0.28f, 0.22f, 0.22f, 0.24f, 2); // Gris oscuro: faros apagados.
            }
            if (noche) { // Segundo indicador: ambiente nocturno o diurno.
                cajaHUD(0.22f, 0.12f, 0.12f, 0.28f, 0.15f, 0.20f, 0.60f, 2); // Azul oscuro: es de noche.
            } else {
                cajaHUD(0.22f, 0.12f, 0.12f, 0.28f, 0.60f, 0.85f, 1.0f, 2); // Celeste: es de día.
            }
        } finally { // Siempre devuelve OpenGL a la configuración normal.
            glDisable(GL_SCISSOR_TEST); // Permite dibujar en toda la ventana.
            glViewport(0, 0, ancho, alto); // Recupera el área completa.
        }
        if (enPausa) { // En pausa se dibuja el icono ⏸ en el centro de la pantalla.
            glClear(GL_DEPTH_BUFFER_BIT); // Descarta la profundidad de la escena para que las barras queden encima.
            cajaHUD(0.45f, 0.42f, 0.03f, 0.16f, 1, 1, 1, 5); // Primera barra blanca vertical.
            cajaHUD(0.52f, 0.42f, 0.03f, 0.16f, 1, 1, 1, 5); // Segunda barra blanca vertical.
        }
        entero("uMapa", 0); // Recupera la proyección en perspectiva para el siguiente cuadro.
    }

    /** Dibuja una señal de tránsito: poste gris delgado y panel verde plano en la parte superior (2 cajas). */
    private void dibujarSenal(float x, float z, float angulo) {
        cajaGirada(x, 1.5f, z, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f, angulo); // Poste vertical gris sobre la acera.
        cajaGirada(x - (float) Math.sin(angulo) * 0.1f, 2.45f, z - (float) Math.cos(angulo) * 0.1f, 1.2f, 0.6f, 0.06f, 0.10f, 0.55f, 0.25f, angulo); // Panel verde plano, un poco delante del poste.
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 2.2f, 0.1f, 3.2f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2; // Desplaza la punta dos unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2; // Desplaza la punta dos unidades hacia delante en Z.
        caja(puntaX, 26, puntaZ, 0.9f, 0.1f, 0.9f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        JuegoEntregas aplicacion = new JuegoEntregas(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}