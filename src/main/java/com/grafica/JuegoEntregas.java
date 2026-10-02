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
    private boolean enInicio = true; // Cuando es true el juego espera en la pantalla de inicio.
    private boolean tituloInicioPuesto = false; // Evita reescribir el título de inicio en cada cuadro.
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
        {0, -30}, {30, 0}, {-30, 0}, {60, -30}, {-60, 30}, {30, 60}, {-30, -60}, {60, 30}, {-60, -30}, {0, -60}, {90, -60}, {-90, 60}, {0, 90}, {0, -90}
    };
    private int puntoMoneda = 0; // Índice del lugar donde está la moneda ahora.
    private float monedaX = PUNTOS_MONEDA[0][0]; // Posición X actual de la moneda.
    private float monedaZ = PUNTOS_MONEDA[0][1]; // Posición Z actual de la moneda.
    private static final float LIMITE_HUD = 107; // Mitad del rango que el shader del minimapa convierte a pantalla (LIMITE 105 + 2).
    private int monedasRecolectadas = 0; // Cuenta las monedas que ha recogido el jugador.

    // ==================== PÍLDORAS (POWER-UPS) ====================
    private float pildoraAzulX = 30.0f, pildoraAzulZ = -30.0f; // Píldora azul (inmunidad).
    private float pildoraNaranjaX = 0.0f, pildoraNaranjaZ = -30.0f; // Píldora naranja (velocidad); se reubican al aparecer.
    private boolean pildorasVisibles = false; // Las dos píldoras aparecen y desaparecen juntas.
    private float timerSpawnPildoras = 30.0f; // Segundos hasta la próxima aparición.
    private float timerVidaPildoras = 0.0f; // Segundos que le quedan a las píldoras visibles antes de desaparecer.
    private float timerFantasma = 0.0f; // Segundos restantes en que el jugador traspasa a los bots (píldora morada).
    private float timerVelocidad = 0.0f; // Segundos restantes del efecto de velocidad.
    private static final float VELOCIDAD_PILDORA = 120 / 3.6f; // 120 km/h en unidades por segundo.

    // ==================== PATRULLA PERSEGUIDORA ====================
    private float poliX = 0f, poliZ = 0f, poliAngulo = 0f; // Posición y orientación (radianes) de la patrulla perseguidora.
    private boolean poliPersiguiendo = false; // Cuando es true la patrulla existe y persigue al jugador.
    private float timerPoli = 0f; // Segundos restantes de la persecución.
    private float poliDestinoX = 0f, poliDestinoZ = 0f; // Próximo punto al que se dirige la patrulla (siempre sobre una calle).
    private boolean poliTieneDestino = false; // false obliga a elegir un nuevo destino.
    private static final float VELOCIDAD_POLI = 15.0f; // Velocidad de la patrulla: menor que los 16,7 u/s (60 km/h) del jugador, así que puede escapar.

    // ==================== NIVELES Y VICTORIA ====================
    private static final int MAX_BOTS = 15; // Tamaño de todos los arreglos de bots (los "bots ocultos" se procesan solo si están activos).
    private static final int MONEDAS_VICTORIA = 20; // Monedas necesarias para ganar.
    private int nivel = 1; // Nivel actual (1 a 3).
    private boolean victoria = false; // Cuando es true el juego se congela y se muestra la V.
    private int botsActivos = 5; // Cantidad de bots que se mueven, dibujan y pueden chocar.
    private float radioColision = 5.0f; // Umbral de choque que quita vida, en distancia al cuadrado (5.0 = radio de 2,24 unidades).
    private float velBots = 10.0f; // Velocidad global de los bots; 10 equivale a la velocidad base de cada bot.
    private float timerMensaje = 0.0f; // Segundos restantes del aviso de subida de nivel.

    // ==================== VIDAS Y GAME OVER ====================
    private static final int VIDAS_MAXIMAS = 3; // Choques permitidos antes del Game Over.
    private int choques = 0; // Choques contra otros autos acumulados en la partida.
    private boolean gameOver = false; // Cuando es true el juego se congela y se muestra la X roja.
    private float invulnerable = 0; // Segundos restantes de gracia tras reaparecer, para no perder dos vidas seguidas.
    private final float spawnX; // Coordenada X del punto de inicio del jugador.
    private final float spawnZ; // Coordenada Z del punto de inicio del jugador.
    private final float spawnAngulo; // Orientación inicial del jugador.

    /** Guarda el punto de inicio heredado de Vehiculo para poder reaparecer ahí. */
    public JuegoEntregas() {
        spawnX = autoX; // Copia la X inicial del auto.
        spawnZ = autoZ; // Copia la Z inicial del auto.
        spawnAngulo = angulo; // Copia la orientación inicial.
    }

    // ==================== TRÁFICO AUTÓNOMO ====================
    private static final float[][][] RUTAS = { // Cada ruta es un circuito cerrado de esquinas (X, Z) que siempre cae sobre calles.
        {{-30, -30}, {30, -30}, {30, 30}, {-30, 30}}, // Bot 0 (patrulla): cuadrado central por las calles -30 y 30.
        {{-60, -60}, {60, -60}, {60, 60}, {-60, 60}}, // Bot 1 (camión de bomberos): vuelta completa por las calles del borde.
        {{30, -60}, {30, -30}, {60, -30}, {60, -60}}, // Bot 2 (ambulancia): circuito pequeño en el sector noreste.
        {{-60, 0}, {-30, 0}, {-30, 30}, {-60, 30}}, // Bot 3 (civil): circuito en el sector oeste.
        {{30, 0}, {60, 0}, {60, 60}, {30, 60}}, // Bot 4 (civil): circuito en el sector sureste.
        {{-60, -30}, {-30, -30}, {-30, 0}, {-60, 0}}, // Bot 5 (civil).
        {{30, 30}, {60, 30}, {60, 60}, {30, 60}}, // Bot 6 (civil).
        {{-60, 30}, {-30, 30}, {-30, 60}, {-60, 60}}, // Bot 7 (civil).
        {{0, -60}, {30, -60}, {30, -30}, {0, -30}}, // Bot 8 (civil).
        {{-30, -60}, {0, -60}, {0, -30}, {-30, -30}}, // Bot 9 (civil).
        {{0, 0}, {30, 0}, {30, 30}, {0, 30}}, // Bot 10 (civil).
        {{-30, 0}, {0, 0}, {0, 30}, {-30, 30}}, // Bot 11 (civil).
        {{-60, -60}, {-30, -60}, {-30, -30}, {-60, -30}}, // Bot 12 (civil).
        {{0, 30}, {30, 30}, {30, 60}, {0, 60}}, // Bot 13 (civil).
        {{0, 60}, {-30, 60}, {-30, 30}, {0, 30}} // Bot 14 (civil): nace lejos del punto de inicio del jugador.
    };
    private static final float LIMITE_RUTA_BUS = 90.0f; // Eje del anillo de asfalto por el que da la vuelta el bus (celdas 1 y 19).
    private static final float DESVIO_BUS = 2.5f; // Desplazamiento hacia el carril derecho (hacia dentro del anillo).
    private static final float CARRIL_BUS = LIMITE_RUTA_BUS - DESVIO_BUS; // Límite real de la ruta: 87,5 unidades.
    private float busX = -CARRIL_BUS; // Posición X del bus (esquina superior izquierda del anillo, ya en su carril).
    private float busZ = -CARRIL_BUS; // Posición Z del bus.
    private float busAngulo = -(float) Math.PI / 2; // Orientación en radianes: mirando hacia +X.
    private int estadoBus = 0; // 0 = hacia +X, 1 = hacia +Z, 2 = hacia -X, 3 = hacia -Z.
    private static final float DESVIO_CARRIL = 2.5f; // Distancia lateral al eje de la calle: los bots circulan por su carril derecho.
    private final float[] botX = {-30, 60, 30, -60, 30, -60, 30, -60, 0, -30, 0, -30, -60, 0, 0}; // Posición X inicial de cada bot: justo en una esquina de su ruta.
    private final float[] botZ = {-30, 60, -60, 0, 0, -30, 30, 30, -60, -60, 0, 0, -60, 30, 60}; // Posición Z inicial de cada bot.
    private final float[] botAngulo = new float[MAX_BOTS]; // Orientación en radianes; se recalcula en cada cuadro.
    private final float[] botVelocidad = {7, 9, 6, 5, 8, 6, 7, 5, 8, 6, 7, 5, 9, 6, 8}; // Unidades por segundo de cada bot.
    private final float[] botChoque = new float[MAX_BOTS]; // Segundos restantes del choque de cada bot: primero espera, luego retrocede.
    private final int[] botObjetivo = {1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}; // Índice de la esquina hacia la que avanza cada bot.
    private final float[] botXInicial = botX.clone(); // Copia de las posiciones X originales para reiniciar.
    private final float[] botZInicial = botZ.clone(); // Copia de las posiciones Z originales.
    private final int[] botObjetivoInicial = botObjetivo.clone(); // Copia de las esquinas objetivo originales.
    private final float[][] botColor = {{0.95f, 0.80f, 0.15f}, {0.20f, 0.70f, 0.30f}, {0.60f, 0.30f, 0.85f}, {0.15f, 0.40f, 0.85f}, {0.90f, 0.90f, 0.92f},
        {0.85f, 0.25f, 0.20f}, {0.95f, 0.55f, 0.15f}, {0.30f, 0.75f, 0.80f}, {0.55f, 0.55f, 0.60f}, {0.80f, 0.30f, 0.60f},
        {0.25f, 0.30f, 0.65f}, {0.60f, 0.80f, 0.25f}, {0.40f, 0.25f, 0.20f}, {0.95f, 0.85f, 0.40f}, {0.20f, 0.20f, 0.25f}}; // Del bot 3 en adelante son civiles.

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
    private static final int[][] PARCELAS_MASCOTAS = {{5, 11}, {12, 6}, {14, 15}, {11, 5}}; // Celdas de parque (tipo 2) de {fila, columna}: 2 perros y 2 gatos.
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
        if (key == GLFW_KEY_SPACE || key == GLFW_KEY_ENTER) { // Espacio o Enter: iniciar o pausar.
            if (enInicio) {
                enInicio = false; // Comienza el juego; actualizarTitulo() restaura el título normal.
            } else if (!gameOver && !victoria) {
                enPausa = !enPausa; // Alterna la pausa.
                if (enPausa) { // El título solo se fija al pausar porque actualizar() está detenido.
                    glfwSetWindowTitle(ventana, "JUEGO PAUSADO");
                }
            }
        }
    }
    

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        monedasRecolectadas = 0; // Vuelve a contar las monedas desde cero.
        nivel = 1; victoria = false; botsActivos = 5; radioColision = 5.0f; velBots = 10.0f; timerMensaje = 0; // Vuelve al nivel 1.
        choques = 0; // Restaura las tres vidas.
        gameOver = false; // Sale de la pantalla de Game Over.
        enPausa = false; // Quita la pausa si estaba activa.
        pildorasVisibles = false; timerSpawnPildoras = 30.0f; timerVelocidad = 0; timerFantasma = 0; // Cancela los power-ups.
        poliPersiguiendo = false; poliTieneDestino = false; timerPoli = 0; // Retira la patrulla perseguidora.
        velocidadMaxima = 60 / 3.6f; aceleracionMotor = 14;
        invulnerable = 2.0f; // Dos segundos de gracia por si un bot queda cerca del punto de inicio.
        for (int bot = 0; bot < MAX_BOTS; bot++) { // Devuelve los 15 bots (activos y ocultos) a su estado original.
            botX[bot] = botXInicial[bot];
            botZ[bot] = botZInicial[bot];
            botAngulo[bot] = 0;
            botObjetivo[bot] = botObjetivoInicial[bot]; // Esquina objetivo original (1 en casi todos; 3 en el bot 1).
            botChoque[bot] = 0; // Cancela choques pendientes.
        }
        java.util.Arrays.fill(peatonAplastado, 0); // Los peatones aplastados vuelven a la normalidad.
        busX = -CARRIL_BUS; // Devuelve el bus a la esquina superior izquierda del anillo.
        busZ = -CARRIL_BUS;
        busAngulo = -(float) Math.PI / 2;
        estadoBus = 0;
        autoX = spawnX; // Coloca al jugador en su punto de inicio.
        autoZ = spawnZ; // Misma corrección en profundidad.
        angulo = spawnAngulo; // Recupera la orientación inicial.
        velocidad = 0; // Detiene el auto.
    }

    /** Coloca las dos píldoras en puntos de calle distintos entre sí, de la moneda y del punto de inicio. */
    private void reubicarPildoras() {
        int a, b;
        do { a = (int) (Math.random() * PUNTOS_MONEDA.length); } while (a == puntoMoneda);
        do { b = (int) (Math.random() * PUNTOS_MONEDA.length); } while (b == puntoMoneda || b == a);
        pildoraAzulX = PUNTOS_MONEDA[a][0]; pildoraAzulZ = PUNTOS_MONEDA[a][1];
        pildoraNaranjaX = PUNTOS_MONEDA[b][0]; pildoraNaranjaZ = PUNTOS_MONEDA[b][1];
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
        if (enInicio) { // En la pantalla de inicio nada se mueve.
            if (!tituloInicioPuesto) { // Fija el título una sola vez.
                glfwSetWindowTitle(ventana, "Presiona ESPACIO o ENTER para iniciar");
                tituloInicioPuesto = true;
            }
            return;
        }
        if (enPausa || gameOver || victoria) { // En pausa, Game Over o victoria nada se mueve ni se actualiza.
            return;
        }
        if (timerMensaje > 0) timerMensaje -= deltaTime; // Descuenta el aviso de nivel (se congela en pausa).
        if (invulnerable > 0) { // Descuenta la gracia posterior a un choque.
            invulnerable -= deltaTime;
        }
        if (!pildorasVisibles) { // Cuenta atrás para que aparezcan las píldoras.
            timerSpawnPildoras -= deltaTime;
            if (timerSpawnPildoras <= 0) {
                reubicarPildoras(); // Elige dos puntos de calle distintos.
                pildorasVisibles = true;
                timerVidaPildoras = 20.0f; // Permanecen 20 s en el mapa.
                timerSpawnPildoras = 30.0f;
            }
        } else { // Visibles: cuenta atrás para desaparecer si nadie las recoge.
            timerVidaPildoras -= deltaTime;
            if (timerVidaPildoras <= 0) {
                pildorasVisibles = false;
            }
        }
        if (timerFantasma > 0) timerFantasma -= deltaTime; // Descuenta el efecto de traspasar autos.
        if (timerVelocidad > 0) { // Efecto de velocidad activo.
            timerVelocidad -= deltaTime;
            velocidadMaxima = VELOCIDAD_PILDORA; // Tope de 120 km/h.
            aceleracionMotor = 30; // Con 14 no llegaría: el equilibrio es aceleración / 0,7.
        } else { // Valores normales.
            velocidadMaxima = 60 / 3.6f;
            aceleracionMotor = 14;
        }
        float autoXPrevio = autoX; // Guarda la posición X del jugador antes de moverlo.
        float autoZPrevio = autoZ; // Guarda la posición Z del jugador antes de moverlo.
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        actualizarBus(deltaTime); // Da la vuelta al anillo perimetral por su carril derecho.
        if (poliPersiguiendo) { // IA de la patrulla: persigue al jugador hasta que expire el tiempo o lo embista.
            timerPoli -= deltaTime;
            if (timerPoli <= 0) {
                poliPersiguiendo = false;
            } else {
                avanzarPatrulla(deltaTime); // Persigue siguiendo las calles (no atraviesa edificios).
                float dx = autoX - poliX; // Vector hacia el jugador tras avanzar.
                float dz = autoZ - poliZ;
                if (timerFantasma <= 0 && invulnerable <= 0 && dx * dx + dz * dz < radioColision) { // Embestida: cuesta una vida.
                    perderVida();
                    poliPersiguiendo = false;
                }
            }
        }
        if (gameOver) { // La patrulla acabó con las vidas: nada más debe resolverse en este cuadro.
            return;
        }
        actualizarBots(deltaTime); // Mueve el tráfico autónomo, incluso cuando el juego ya terminó.
        resolverChoqueBots(autoXPrevio, autoZPrevio); // Detiene al jugador y al bot si se tocan.
        if (gameOver) { // Evita cobrar monedas o ganar en el mismo cuadro del Game Over.
            return;
        }
        float dx = autoX - monedaX; // Separación horizontal entre el auto y la moneda (autoX viene de Vehiculo).
        float dz = autoZ - monedaZ; // Separación en profundidad entre el auto y la moneda.
        float distCuadrada = dx * dx + dz * dz; // Distancia al cuadrado, sin calcular raíz.
        if (distCuadrada < 25.0f) { // Radio de recolección de 5 unidades: cubre toda la marca de 6x6.
            monedasRecolectadas++; // Suma una moneda al contador.
            if (monedasRecolectadas == 5 && nivel == 1) { // Sube al nivel 2.
                nivel = 2; botsActivos = 10; radioColision = 7.0f; velBots = 15.0f; timerMensaje = 3.0f; invulnerable = Math.max(invulnerable, 2.0f);
            } else if (monedasRecolectadas == 10 && nivel == 2) { // Sube al nivel 3.
                nivel = 3; botsActivos = 15; radioColision = 9.0f; velBots = 20.0f; timerMensaje = 3.0f; invulnerable = Math.max(invulnerable, 2.0f);
            } else if (monedasRecolectadas == MONEDAS_VICTORIA) { // Fin del juego.
                victoria = true;
                glfwSetWindowTitle(ventana, "¡VICTORIA! Recogiste las 20 monedas. Presiona [R] para jugar otra vez.");
            }
            reubicarMoneda(); // Mueve la moneda a otro punto de la ciudad.
        }
        if (pildorasVisibles) { // Recogida de píldoras: al tomar una desaparecen ambas.
            float distAzul = (autoX - pildoraAzulX) * (autoX - pildoraAzulX) + (autoZ - pildoraAzulZ) * (autoZ - pildoraAzulZ);
            float distNaranja = (autoX - pildoraNaranjaX) * (autoX - pildoraNaranjaX) + (autoZ - pildoraNaranjaZ) * (autoZ - pildoraNaranjaZ);
            if (distAzul < 9.0f) { // Inmunidad: invulnerable ya impide perder vidas.
                invulnerable = 20.0f;
                timerFantasma = 20.0f; // También permite traspasar a los bots.
                pildorasVisibles = false;
            } else if (distNaranja < 9.0f) { // Velocidad.
                timerVelocidad = 20.0f;
                pildorasVisibles = false;
            }
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
    @Override // Envía al shader los faros de bots y bus además de los del auto principal.
    protected void prepararLuces() {
        super.prepararLuces(); // Conserva farolas y faros del auto principal.
        enviarFarosVehiculo(0, busX, busZ, busAngulo, 4.6f, 0.9f); // El bus ocupa el índice 0.
        for (int bot = 0; bot < botsActivos; bot++) { // Un foco doble por cada bot activo (índices 1 a botsActivos).
            float delante = (bot == 1) ? 2.4f : (bot == 2) ? 2.1f : 1.36f; // Bomberos y ambulancia son más largos.
            float lado = (bot == 1) ? 0.6f : 0.55f; // Separación lateral de los faros.
            enviarFarosVehiculo(bot + 1, botX[bot], botZ[bot], botAngulo[bot], delante, lado);
        }
        entero("uVehN", noche ? botsActivos + 1 : 0); // De noche se activan todos; de día ninguno.
    }

    @Override // Amplía los indicadores de día/noche y faros de Iluminacion.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa"; // Muestra la tecla que alterna el minimapa.
        mensaje += " | Nivel: " + nivel + " | Monedas: " + monedasRecolectadas + " | Vidas: " + (VIDAS_MAXIMAS - choques); // Solo nivel, monedas y vidas.
        mensaje += " | [Espacio/Enter] Pausar [R] Reiniciar"; // Controles vigentes.
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
        if (pildorasVisibles) { // Cubos giratorios que flotan: morada = inmunidad, naranja = velocidad.
            float lado = vistaMapa ? 7.0f : 2.2f; // Más grandes: en el minimapa deben verse con claridad.
            float flote = 1.6f + (float) Math.sin(tiempoAnimacion * 3.0f) * 0.2f;
            cajaGirada(pildoraAzulX, flote, pildoraAzulZ, lado, lado, lado, 0.65f, 0.15f, 0.90f, tiempoAnimacion * 2); // Morada (inmunidad).
            cajaGirada(pildoraNaranjaX, flote, pildoraNaranjaZ, lado, lado, lado, 1.0f, 0.5f, 0.0f, tiempoAnimacion * 2); // Naranja (velocidad).
        }
        if (poliPersiguiendo) { // Patrulla perseguidora con sirenas que alternan 10 veces por segundo.
            dibujarAutoPolicia(poliX, poliZ, poliAngulo, tiempoAnimacion * 2.5f); // dibujarAutoPolicia usa (int)(tiempo * 4) % 2.
        }
        dibujarBus(busX, busZ, busAngulo, tiempoAnimacion); // Dibuja el bus del anillo, también en el minimapa.
        for (int bot = 0; bot < botsActivos; bot++) { // Dibuja cada auto autónomo, también en el minimapa.
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
                if (!poliPersiguiendo) { // Un atropello llama a la patrulla.
                    poliPersiguiendo = true;
                    timerPoli = 30.0f;
                    float candidatoX = Math.max(-90.0f, Math.min(90.0f, autoX + 40.0f)); // Punto a 40 unidades, dentro del mapa.
                    float calleX = Math.round(candidatoX / 30.0f) * 30.0f; // Calle vertical más cercana (las calles están cada 30 unidades: -90 a 90).
                    float calleZ = Math.round(Math.max(-90.0f, Math.min(90.0f, autoZ)) / 30.0f) * 30.0f; // Calle horizontal más cercana.
                    if (Math.abs(candidatoX - calleX) <= Math.abs(autoZ - calleZ)) { // Está más cerca de una calle vertical:
                        poliX = calleX; poliZ = Math.max(-90.0f, Math.min(90.0f, autoZ)); // se centra en ella y conserva su Z.
                    } else { // Está más cerca de una calle horizontal:
                        poliX = candidatoX; poliZ = calleZ; // se centra en ella y conserva su X.
                    }
                    poliTieneDestino = false; // Elige ruta desde cero.
                    poliAngulo = (float) Math.atan2(-(autoX - poliX), -(autoZ - poliZ)); // Nace mirando al jugador.
                }
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
    /** Máquina de estados del bus: recorre el anillo en sentido horario a 15 unidades por segundo. */
    private void actualizarBus(float deltaTime) {
        float paso = 15.0f * deltaTime; // Distancia que avanza en este cuadro.
        if (estadoBus == 0) { // Tramo superior: hacia +X.
            busX += paso;
            if (busX >= CARRIL_BUS) { busX = CARRIL_BUS; busAngulo = (float) Math.PI; estadoBus = 1; } // Gira hacia +Z.
        } else if (estadoBus == 1) { // Tramo derecho: hacia +Z.
            busZ += paso;
            if (busZ >= CARRIL_BUS) { busZ = CARRIL_BUS; busAngulo = (float) Math.PI / 2; estadoBus = 2; } // Gira hacia -X.
        } else if (estadoBus == 2) { // Tramo inferior: hacia -X.
            busX -= paso;
            if (busX <= -CARRIL_BUS) { busX = -CARRIL_BUS; busAngulo = 0; estadoBus = 3; } // Gira hacia -Z.
        } else { // Tramo izquierdo: hacia -Z.
            busZ -= paso;
            if (busZ <= -CARRIL_BUS) { busZ = -CARRIL_BUS; busAngulo = -(float) Math.PI / 2; estadoBus = 0; } // Gira hacia +X.
        }
    }

    /** Dibuja el bus: carrocería celeste larga, techo blanco, ventanas laterales, parabrisas, faros y ruedas. */
    private void dibujarBus(float x, float z, float angulo, float tiempo) {
        float vibracion = (float) Math.sin(tiempo * 20) * 0.015f; // Pequeña vibración del motor.
        piezaVehiculo(x, z, angulo, 0, 1.7f + vibracion, 0, 2.6f, 2.2f, 9.0f, 0.20f, 0.65f, 0.85f); // Carrocería celeste (mucho más larga y alta que un auto).
        piezaVehiculo(x, z, angulo, 0, 2.85f + vibracion, 0, 2.4f, 0.1f, 8.8f, 0.95f, 0.95f, 0.97f); // Techo blanco.
        for (int ventana = 0; ventana < 5; ventana++) { // Cinco ventanas por lado.
            float lz = -3.0f + ventana * 1.5f; // Posición a lo largo del bus.
            piezaVehiculo(x, z, angulo, -1.31f, 2.05f + vibracion, lz, 0.06f, 0.8f, 1.1f, 0.12f, 0.20f, 0.30f); // Ventana izquierda.
            piezaVehiculo(x, z, angulo, 1.31f, 2.05f + vibracion, lz, 0.06f, 0.8f, 1.1f, 0.12f, 0.20f, 0.30f); // Ventana derecha.
        }
        piezaVehiculo(x, z, angulo, 0, 2.05f + vibracion, -4.52f, 2.2f, 0.9f, 0.06f, 0.12f, 0.20f, 0.30f); // Parabrisas frontal.
        piezaVehiculo(x, z, angulo, -0.9f, 0.95f, -4.52f, 0.4f, 0.25f, 0.06f, 1.0f, 0.95f, 0.65f); // Faro izquierdo.
        piezaVehiculo(x, z, angulo, 0.9f, 0.95f, -4.52f, 0.4f, 0.25f, 0.06f, 1.0f, 0.95f, 0.65f); // Faro derecho.
        for (int eje = 0; eje < 2; eje++) { // Dos ejes de ruedas.
            float lz = (eje == 0) ? -2.8f : 2.8f; // Eje delantero y trasero.
            piezaVehiculo(x, z, angulo, -1.35f, 0.45f, lz, 0.3f, 0.9f, 1.0f, 0.06f, 0.07f, 0.08f); // Rueda izquierda.
            piezaVehiculo(x, z, angulo, 1.35f, 0.45f, lz, 0.3f, 0.9f, 1.0f, 0.06f, 0.07f, 0.08f); // Rueda derecha.
        }
    }

    /** Dibuja una parada de bus sobre la acera: techo plano, dos postes, vidrio trasero y asiento (frente = lado de la calle). */
    private void dibujarParadaBus(float x, float z, float angulo) {
        piezaVehiculo(x, z, angulo, 0, 2.7f, 0, 3.6f, 0.15f, 1.8f, 0.30f, 0.34f, 0.40f); // Techo plano.
        piezaVehiculo(x, z, angulo, -1.7f, 1.5f, -0.75f, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f); // Poste delantero izquierdo (la acera está en Y = 0,3).
        piezaVehiculo(x, z, angulo, 1.7f, 1.5f, -0.75f, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f); // Poste delantero derecho.
        piezaVehiculo(x, z, angulo, 0, 1.5f, 0.8f, 3.4f, 1.9f, 0.05f, 0.60f, 0.85f, 0.90f); // Vidrio trasero celeste.
        piezaVehiculo(x, z, angulo, 0, 0.85f, 0.45f, 2.4f, 0.1f, 0.4f, 0.55f, 0.35f, 0.18f); // Asiento de madera.
        piezaVehiculo(x, z, angulo, -1.0f, 0.55f, 0.45f, 0.1f, 0.5f, 0.3f, 0.40f, 0.42f, 0.45f); // Pata izquierda del asiento.
        piezaVehiculo(x, z, angulo, 1.0f, 0.55f, 0.45f, 0.1f, 0.5f, 0.3f, 0.40f, 0.42f, 0.45f); // Pata derecha del asiento.
    }

    private void actualizarBots(float deltaTime) {
        for (int bot = 0; bot < botsActivos; bot++) { // Recorre solo los autos autónomos activos.
            if (botChoque[bot] > 0) { // Comprueba si el bot está en medio de un choque.
                botChoque[bot] -= deltaTime; // Consume el tiempo del choque.
                if (botChoque[bot] < 1.1f) { // Tras una breve espera detenido, empieza a retroceder.
                    botX[bot] += (float) Math.sin(botAngulo[bot]) * 3 * deltaTime; // Retrocede en X (opuesto al frente -sen).
                    botZ[bot] += (float) Math.cos(botAngulo[bot]) * 3 * deltaTime; // Retrocede en Z (opuesto al frente -cos).
                }
                continue; // Mientras dura el choque no sigue su ruta.
            }
            float dBusX = busX - botX[bot]; // Radar del bus.
            float dBusZ = busZ - botZ[bot];
            boolean puedeAvanzar = dBusX * dBusX + dBusZ * dBusZ >= 100.0f; // Frena si el bus está a menos de 10 unidades.
            if (puedeAvanzar && poliPersiguiendo) { // Radar de la patrulla.
                float dPoliX = poliX - botX[bot];
                float dPoliZ = poliZ - botZ[bot];
                puedeAvanzar = dPoliX * dPoliX + dPoliZ * dPoliZ >= 100.0f;
            }
            for (int j = 0; puedeAvanzar && j < bot; j++) { // Anti-deadlock: solo cede el paso a bots de índice menor.
                float dBotX = botX[j] - botX[bot];
                float dBotZ = botZ[j] - botZ[bot];
                if (dBotX * dBotX + dBotZ * dBotZ < 35.0f) {
                    puedeAvanzar = false;
                }
            }
            if (!puedeAvanzar) { // Se queda quieto este cuadro (equivale a envolver el movimiento en if (puedeAvanzar)).
                continue;
            }
            int anterior = (botObjetivo[bot] + RUTAS[bot].length - 1) % RUTAS[bot].length; // Esquina de la que viene el bot.
            float tramoX = RUTAS[bot][botObjetivo[bot]][0] - RUTAS[bot][anterior][0]; // Dirección X del tramo actual.
            float tramoZ = RUTAS[bot][botObjetivo[bot]][1] - RUTAS[bot][anterior][1]; // Dirección Z del tramo actual.
            float largoTramo = (float) Math.sqrt(tramoX * tramoX + tramoZ * tramoZ); // Largo del tramo para normalizar.
            float derechaX = -tramoZ / largoTramo; // Componente X del vector hacia la derecha del avance.
            float derechaZ = tramoX / largoTramo; // Componente Z del vector hacia la derecha del avance.
            float objetivoX = RUTAS[bot][botObjetivo[bot]][0] + derechaX * DESVIO_CARRIL; // Esquina objetivo desplazada al carril derecho.
            float objetivoZ = RUTAS[bot][botObjetivo[bot]][1] + derechaZ * DESVIO_CARRIL; // Misma corrección en Z.
            float diferenciaX = objetivoX - botX[bot]; // Calcula la separación horizontal al objetivo.
            float diferenciaZ = objetivoZ - botZ[bot]; // Calcula la separación en profundidad al objetivo.
            float distancia = (float) Math.sqrt(diferenciaX * diferenciaX + diferenciaZ * diferenciaZ); // Mide la distancia restante.
            float paso = botVelocidad[bot] * velBots / 10.0f * deltaTime; // Distancia que avanza en este cuadro.
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

    /** Mueve la patrulla por la cuadrícula de calles (cada 30 unidades, de -90 a 90) en dirección al jugador. */
    private void avanzarPatrulla(float deltaTime) {
        float calleX = Math.round(Math.max(-90.0f, Math.min(90.0f, poliX)) / 30.0f) * 30.0f; // Calle vertical más cercana.
        float calleZ = Math.round(Math.max(-90.0f, Math.min(90.0f, poliZ)) / 30.0f) * 30.0f; // Calle horizontal más cercana.
        boolean enCalleH = Math.abs(poliZ - calleZ) < 4.5f; // Dentro de la franja de una calle horizontal (ancho útil de ±4,5).
        boolean enCalleV = Math.abs(poliX - calleX) < 4.5f; // Dentro de la franja de una calle vertical.
        boolean alineado = (enCalleH && Math.abs(autoZ - calleZ) < 4.5f) || (enCalleV && Math.abs(autoX - calleX) < 4.5f); // Jugador en la misma calle.
        if (alineado) { // Misma calle: recta hasta el jugador (queda dentro de la franja, sin tocar manzanas).
            poliDestinoX = autoX;
            poliDestinoZ = autoZ;
            poliTieneDestino = true;
        } else if (!poliTieneDestino) { // Elige el siguiente cruce.
            float nodoX = calleX, nodoZ = calleZ; // Cruce más cercano.
            if ((poliX - nodoX) * (poliX - nodoX) + (poliZ - nodoZ) * (poliZ - nodoZ) > 0.25f) { // Aún no está en un cruce: va primero a él.
                poliDestinoX = nodoX;
                poliDestinoZ = nodoZ;
            } else { // Está en un cruce: avanza un tramo por el eje más alejado del jugador.
                float difX = autoX - nodoX;
                float difZ = autoZ - nodoZ;
                float sigX = Math.max(-90.0f, Math.min(90.0f, nodoX + 30.0f * Math.signum(difX)));
                float sigZ = Math.max(-90.0f, Math.min(90.0f, nodoZ + 30.0f * Math.signum(difZ)));
                if (Math.abs(difX) >= Math.abs(difZ) && sigX != nodoX) { // Eje X.
                    poliDestinoX = sigX; poliDestinoZ = nodoZ;
                } else if (sigZ != nodoZ) { // Eje Z.
                    poliDestinoX = nodoX; poliDestinoZ = sigZ;
                } else { // Sin movimiento posible en Z: usa X.
                    poliDestinoX = sigX; poliDestinoZ = nodoZ;
                }
            }
            poliTieneDestino = true;
        }
        float difDestX = poliDestinoX - poliX;
        float difDestZ = poliDestinoZ - poliZ;
        float distancia = (float) Math.sqrt(difDestX * difDestX + difDestZ * difDestZ);
        float paso = VELOCIDAD_POLI * deltaTime;
        if (distancia <= paso) { // Llegó: se coloca en el punto y elige otro destino en el siguiente cuadro.
            poliX = poliDestinoX;
            poliZ = poliDestinoZ;
            poliTieneDestino = false;
        } else {
            poliAngulo = (float) Math.atan2(-difDestX, -difDestZ); // Frente = (-sen, -cos).
            poliX -= (float) Math.sin(poliAngulo) * paso;
            poliZ -= (float) Math.cos(poliAngulo) * paso;
        }
    }

    /** Resta una vida: Game Over si no quedan, o reaparición con dos segundos de gracia. */
    private void perderVida() {
        choques++;
        velocidad = 0;
        if (choques >= VIDAS_MAXIMAS) {
            gameOver = true;
            glfwSetWindowTitle(ventana, "¡GAME OVER! Presiona [R] para reiniciar.");
        } else {
            autoX = spawnX; autoZ = spawnZ; angulo = spawnAngulo;
            invulnerable = Math.max(invulnerable, 2.0f);
        }
    }

    /** Si el jugador toca a un bot, ambos se detienen y el bot espera un instante y retrocede. */
    private void resolverChoqueBots(float previoX, float previoZ) {
        if (timerFantasma > 0) { // Con la píldora morada el jugador atraviesa a los bots sin bloquearlos ni dañarse.
            return;
        }
        float radioChoque = 2.0f; // Radio de bloqueo físico (menor que el de choque: sqrt(5) ≈ 2,24).
        for (int bot = 0; bot < botsActivos; bot++) { // Revisa solo los bots activos.
            float diferenciaX = autoX - botX[bot]; // Separación horizontal actual entre jugador y bot.
            float diferenciaZ = autoZ - botZ[bot]; // Separación en profundidad actual.
            float distanciaCuadrada = diferenciaX * diferenciaX + diferenciaZ * diferenciaZ; // Distancia al cuadrado, sin raíz.
            if (invulnerable <= 0 && distanciaCuadrada < radioColision) { // Choque que cuesta una vida (distancia al cuadrado según el nivel).
                choques++; // Registra el choque.
                velocidad = 0; // Detiene al jugador.
                if (choques >= VIDAS_MAXIMAS) { // Sin vidas: fin de la partida.
                    gameOver = true; // Congela el juego y activa la X roja.
                    glfwSetWindowTitle(ventana, "¡GAME OVER! Presiona [R] para reiniciar."); // actualizar() ya no refresca el título.
                } else { // Aún quedan vidas: reaparece conservando las monedas.
                    autoX = spawnX; // Vuelve al punto de inicio en X.
                    autoZ = spawnZ; // Vuelve al punto de inicio en Z.
                    angulo = spawnAngulo; // Recupera la orientación inicial.
                    invulnerable = Math.max(invulnerable, 2.0f); // Dos segundos sin perder otra vida si un bot pasa por el spawn.
                }
                return; // Un solo choque por cuadro.
            }
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
                    case 6 -> { // Parada de bus: se orienta hacia la calle contigua.
                        float anguloParada = 0; // Frente hacia -Z por defecto.
                        float desvioParadaX = 0; // Desplazamiento hacia el borde de la acera en X.
                        float desvioParadaZ = 0; // Desplazamiento hacia el borde de la acera en Z.
                        if (columna + 1 < MAPA[fila].length && MAPA[fila][columna + 1] == 0) { // Calle al este.
                            anguloParada = -(float) Math.PI / 2; desvioParadaX = 3.3f;
                        } else if (columna > 0 && MAPA[fila][columna - 1] == 0) { // Calle al oeste.
                            anguloParada = (float) Math.PI / 2; desvioParadaX = -3.3f;
                        } else if (fila + 1 < MAPA.length && MAPA[fila + 1][columna] == 0) { // Calle al sur.
                            anguloParada = (float) Math.PI; desvioParadaZ = 3.3f;
                        } else if (fila > 0 && MAPA[fila - 1][columna] == 0) { // Calle al norte.
                            desvioParadaZ = -3.3f;
                        }
                        dibujarParadaBus(x + desvioParadaX, z + desvioParadaZ, anguloParada);
                        float paradaX = x + desvioParadaX; // Centro de la parada en X.
                        float paradaZ = z + desvioParadaZ; // Centro de la parada en Z.
                        float senoParada = (float) Math.sin(anguloParada); // Seno de la orientación de la parada.
                        float cosenoParada = (float) Math.cos(anguloParada); // Coseno de la misma orientación.
                        for (int pasajero = 0; pasajero < 2; pasajero++) { // Dos pasajeros esperando bajo el techo.
                            float lateral = (pasajero == 0) ? -0.7f : 0.7f; // Offset a izquierda o derecha dentro de la parada.
                            float profundidad = -0.1f; // Un poco por delante del asiento, bajo el techo.
                            float pasajeroX = paradaX + cosenoParada * lateral + senoParada * profundidad; // Convierte el offset local a mundo (X).
                            float pasajeroZ = paradaZ - senoParada * lateral + cosenoParada * profundidad; // Convierte el offset local a mundo (Z).
                            float mirada = anguloParada + (float) Math.sin(tiempoAnimacion * 1.3f + pasajero * 2.0f) * 0.5f; // Mira hacia la calle y gira de lado a lado, desfasado entre ambos.
                            if (pasajero == 0) { // Primer pasajero: chaqueta verde.
                                dibujarPeaton(pasajeroX, pasajeroZ, mirada, 0, 0.30f, 0.60f, 0.40f, false); // Tiempo 0: piernas quietas.
                            } else { // Segundo pasajero: chaqueta naranja.
                                dibujarPeaton(pasajeroX, pasajeroZ, mirada, 0, 0.90f, 0.45f, 0.15f, false);
                            }
                        }
                    }
                    case 7 -> { // Techo flotante con surtidores (la gasolinera pasa del id 6 al 7).
                        dibujarGasolinera(x, z); // Dibuja la estructura de la gasolinera.
                        dibujarCargaCombustible(x, z); // Añade el auto estacionado y la persona que carga combustible.
                    }
                    case 0 -> { // Calle: puede llevar un montículo de basura junto a la acera.
                        boolean tramoHorizontal = fila % 3 == 1 && columna % 3 != 1; // Calle que corre a lo largo de X entre dos cruces.
                        boolean tramoVertical = columna % 3 == 1 && fila % 3 != 1; // Calle que corre a lo largo de Z entre dos cruces.
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
                if (tipo == 0 && fila >= 7 && fila <= 13 && fila % 3 == 1 && columna >= 7 && columna <= 13 && columna % 3 == 1) { // Intersección interior: calle cuya fila y columna son 7, 10 o 13.
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
        dibujarHUDJuego(); // Vidas, monedas y Game Over.
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
        int err = glGetError(); // Consulta el error pendiente de OpenGL.
        if (err != GL_NO_ERROR) {
            System.err.println("Error OpenGL ignorado para estabilidad: " + err); // Solo informa en consola.
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
            float proporcion = Math.min(Math.abs(velocidad) / velocidadMaxima, 1f); // Se adapta al tope actual (60 o 120 km/h).
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
        if (enInicio) { // Pantalla de inicio: panel oscuro con un triángulo de Play hecho con columnas escalonadas.
            glClear(GL_DEPTH_BUFFER_BIT); // Descarta la profundidad para que el panel quede encima.
            cajaHUD(0.28f, 0.25f, 0.44f, 0.50f, 0.04f, 0.04f, 0.07f, 6); // Panel oscuro (opaco: el shader no usa transparencia).
            for (int columna = 0; columna < 4; columna++) { // Cuatro columnas de altura decreciente hacia la derecha.
                float altura = 0.28f - columna * 0.07f; // 0,28; 0,21; 0,14; 0,07.
                cajaHUD(0.42f + columna * 0.045f, 0.50f - altura / 2, 0.045f, altura, 0.1f, 1.0f, 0.2f, 7); // Verde brillante centrado en vertical.
            }
        } else if (enPausa) { // Pausa: panel oscuro con las dos barras del icono.
            glClear(GL_DEPTH_BUFFER_BIT);
            cajaHUD(0.35f, 0.32f, 0.30f, 0.36f, 0.04f, 0.04f, 0.07f, 6); // Panel oscuro central.
            cajaHUD(0.43f, 0.42f, 0.04f, 0.16f, 1, 1, 1, 7); // Primera barra blanca vertical.
            cajaHUD(0.53f, 0.42f, 0.04f, 0.16f, 1, 1, 1, 7); // Segunda barra blanca vertical.
        }
        entero("uMapa", 0); // Recupera la proyección en perspectiva para el siguiente cuadro.
    }

    /** Dibuja en 2D las vidas (arriba izquierda), las monedas (debajo) y la X roja de Game Over. */
    private void dibujarHUDJuego() {
        glClear(GL_DEPTH_BUFFER_BIT); // Descarta la profundidad de la escena para que el HUD quede encima.
        entero("uMapa", 1); // Proyección ortográfica con colores directos.
        float lado = 0.035f; // Ancho de cada icono como fracción de la pantalla.
        float alt = lado * ancho / alto; // Altura equivalente para que el icono sea cuadrado en píxeles.
        for (int vida = 0; vida < VIDAS_MAXIMAS; vida++) { // Una cajita por vida.
            float fx = 0.02f + vida * (lado + 0.01f); // Separa las cajitas horizontalmente.
            if (vida < VIDAS_MAXIMAS - choques) { // Vida disponible: roja.
                cajaHUD(fx, 0.98f - alt, lado, alt, 0.90f, 0.12f, 0.15f, 5);
            } else { // Vida perdida: gris.
                cajaHUD(fx, 0.98f - alt, lado, alt, 0.35f, 0.35f, 0.38f, 5);
            }
        }
        float filaY = 0.98f - alt * 2.4f; // Segunda fila, debajo de las vidas.
        cajaHUD(0.02f, filaY, lado, alt, 1.0f, 0.85f, 0.10f, 5); // Icono amarillo de moneda.
        for (int moneda = 0; moneda < Math.min(monedasRecolectadas, 25); moneda++) { // Una cajita pequeña por moneda (máximo 25 visibles).
            cajaHUD(0.02f + lado + 0.01f + moneda * 0.009f, filaY + alt * 0.2f, 0.006f, alt * 0.6f, 1.0f, 0.85f, 0.10f, 5);
        }
        if (gameOver) { // Sub-pantalla central con borde rojo y las letras "G" y "O".
            int cuadro = (int) (Math.min(ancho, alto) * 0.6f); // Lado del viewport cuadrado: así las unidades del mundo son cuadradas en píxeles.
            int centroX = ancho / 2; // Centro horizontal de la ventana en píxeles.
            int centroY = alto / 2; // Centro vertical de la ventana en píxeles.
            float k = LIMITE_HUD / 67.0f; // Escala: el panel se diseñó para un rango de ±67 y el rango actual es ±LIMITE_HUD.
            glViewport(centroX - cuadro / 2, centroY - cuadro / 2, cuadro, cuadro); // Dentro de este viewport el mundo va de -LIMITE_HUD a LIMITE_HUD.
            caja(0, 6, 0, 126 * k, 0.1f, 76 * k, 0.90f, 0.05f, 0.05f); // Borde rojo, ligeramente más grande que el panel.
            caja(0, 7, 0, 120 * k, 0.1f, 70 * k, 0.08f, 0.08f, 0.08f); // Panel gris muy oscuro (y mayor = más cerca de la cámara).
            float rojo = 1.0f, verde = 0.1f, azul = 0.1f; // Rojo brillante de las letras.
            float letraX = -22 * k; // Centro de la letra G (izquierda del centro).
            caja(letraX, 8, -15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // G: barra superior.
            caja(letraX - 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, rojo, verde, azul); // G: barra izquierda.
            caja(letraX, 8, 15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // G: barra inferior.
            caja(letraX + 9 * k, 8, 7.5f * k, 6 * k, 0.1f, 15 * k, rojo, verde, azul); // G: barra derecha-inferior.
            caja(letraX + 4.5f * k, 8, 0, 12 * k, 0.1f, 6 * k, rojo, verde, azul); // G: palito central.
            letraX = 22 * k; // Centro de la letra O (derecha del centro).
            caja(letraX, 8, -15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // O: arriba.
            caja(letraX, 8, 15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // O: abajo.
            caja(letraX - 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, rojo, verde, azul); // O: izquierda.
            caja(letraX + 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, rojo, verde, azul); // O: derecha.
            glViewport(0, 0, ancho, alto); // Recupera la ventana completa.
        }
        if (!gameOver && (timerMensaje > 0 || victoria)) { // Panel central: aviso de nivel o pantalla de victoria.
            int cuadro = (int) (Math.min(ancho, alto) * 0.6f); // Lado del viewport cuadrado.
            float k = LIMITE_HUD / 67.0f; // Misma escala que el panel de Game Over.
            glViewport(ancho / 2 - cuadro / 2, alto / 2 - cuadro / 2, cuadro, cuadro);
            if (victoria) { // Panel verde con una V dorada formada por escalones de cajas.
                caja(0, 6, 0, 126 * k, 0.1f, 76 * k, 1.0f, 0.85f, 0.10f); // Borde dorado.
                caja(0, 7, 0, 120 * k, 0.1f, 70 * k, 0.05f, 0.45f, 0.15f); // Panel verde oscuro.
                for (int i = 0; i < 6; i++) { // Seis escalones: bajan hacia el centro por ambos lados.
                    float px = (27 - i * 5.4f) * k; // Distancia horizontal al centro en este escalón.
                    float pz = (-22.5f + i * 9) * k; // Altura del escalón (z negativo = arriba).
                    caja(-px, 8, pz, 9 * k, 0.1f, 9 * k, 1.0f, 0.95f, 0.30f); // Brazo izquierdo de la V.
                    caja(px, 8, pz, 9 * k, 0.1f, 9 * k, 1.0f, 0.95f, 0.30f); // Brazo derecho de la V.
                }
            } else { // Aviso temporal de subida de nivel: letra L y número.
                caja(0, 6, 0, 126 * k, 0.1f, 76 * k, 1.0f, 0.85f, 0.10f); // Borde amarillo.
                caja(0, 7, 0, 120 * k, 0.1f, 70 * k, 0.08f, 0.08f, 0.08f); // Panel oscuro.
                float r = 1.0f, g = 0.95f, b = 0.15f; // Amarillo brillante.
                float lx = -22 * k; // Centro de la letra L.
                caja(lx - 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, r, g, b); // L: barra vertical.
                caja(lx, 8, 15 * k, 24 * k, 0.1f, 6 * k, r, g, b); // L: barra inferior.
                float nx = 22 * k; // Centro del dígito.
                if (nivel == 1) { // Dígito 1: barra vertical con base.
                    caja(nx, 8, 0, 6 * k, 0.1f, 36 * k, r, g, b);
                    caja(nx, 8, 15 * k, 16 * k, 0.1f, 6 * k, r, g, b);
                } else if (nivel == 2) { // Dígito 2: forma de Z curva.
                    caja(nx, 8, -15 * k, 24 * k, 0.1f, 6 * k, r, g, b); // Arriba.
                    caja(nx + 9 * k, 8, -7.5f * k, 6 * k, 0.1f, 15 * k, r, g, b); // Derecha superior.
                    caja(nx, 8, 0, 24 * k, 0.1f, 6 * k, r, g, b); // Centro.
                    caja(nx - 9 * k, 8, 7.5f * k, 6 * k, 0.1f, 15 * k, r, g, b); // Izquierda inferior.
                    caja(nx, 8, 15 * k, 24 * k, 0.1f, 6 * k, r, g, b); // Abajo.
                } else { // Dígito 3: tres barras y lado derecho.
                    caja(nx, 8, -15 * k, 24 * k, 0.1f, 6 * k, r, g, b);
                    caja(nx, 8, 0, 24 * k, 0.1f, 6 * k, r, g, b);
                    caja(nx, 8, 15 * k, 24 * k, 0.1f, 6 * k, r, g, b);
                    caja(nx + 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, r, g, b);
                }
            }
            glViewport(0, 0, ancho, alto); // Recupera la ventana completa.
        }
        entero("uMapa", 0); // Recupera la perspectiva para el siguiente cuadro.
    }

    /** Dibuja una señal de tránsito: poste gris delgado y panel verde plano en la parte superior (2 cajas). */
    private void dibujarSenal(float x, float z, float angulo) {
        cajaGirada(x, 1.5f, z, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f, angulo); // Poste vertical gris sobre la acera.
        cajaGirada(x - (float) Math.sin(angulo) * 0.1f, 2.45f, z - (float) Math.cos(angulo) * 0.1f, 1.2f, 0.6f, 0.06f, 0.10f, 0.55f, 0.25f, angulo); // Panel verde plano, un poco delante del poste.
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 4.4f, 0.1f, 6.4f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 4; // Desplaza la punta dos unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 4; // Desplaza la punta dos unidades hacia delante en Z.
        caja(puntaX, 26, puntaZ, 1.8f, 0.1f, 1.8f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        JuegoEntregas aplicacion = new JuegoEntregas(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}