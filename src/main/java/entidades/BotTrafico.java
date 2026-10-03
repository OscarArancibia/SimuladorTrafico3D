package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el modelo.
import utilidades.Constantes; // Velocidades y tiempos de choque.

/**
 * PASO 3 DE LA REFACTORIZACIÓN: un auto autónomo que recorre un circuito cerrado de esquinas por su carril derecho.
 * Conoce su estado y su movimiento; decidir si puede avanzar (radares) es tarea del controlador de tráfico.
 */
public final class BotTrafico extends Vehiculo {

    /** Clase de vehículo: determina el modelo y la posición de sus luces. */
    public enum Tipo { POLICIA, BOMBEROS, AMBULANCIA, CIVIL }

    private final int indice; // Posición del bot en la flota.
    private final Tipo tipo; // Los tres primeros bots son vehículos de emergencia; el resto son civiles.
    private final float[][] ruta; // Circuito cerrado de esquinas (X, Z) que siempre cae sobre calles.
    private final float velocidadBase; // Unidades por segundo del bot cuando la velocidad global vale 10.
    private final float[] color; // Color RGB de la carrocería (solo los civiles lo usan).
    private final float xInicial; // Posición X de partida, para reiniciar.
    private final float zInicial; // Posición Z de partida.
    private final int objetivoInicial; // Esquina objetivo de partida.

    private int objetivo; // Índice de la esquina hacia la que avanza.
    private boolean frenando = false; // true cuando el bot está detenido: enciende sus luces de freno.
    private float tiempoChoque = 0; // Segundos restantes del choque: primero espera, luego retrocede.
    private float timerSusto = 0; // Segundos restantes del susto por la bocina: mientras dura, el bot acelera.
    private float velocidadActual = 0; // Velocidad (u/s) que realmente aplica: baja y sube gradualmente por el semáforo.
    private boolean esperandoSemaforo = false; // true mientras una luz no verde lo obliga a frenar o a esperar.
    private boolean recuperandoVelocidad = false; // true desde que frenó por el semáforo hasta volver a su velocidad normal.
    private boolean frenadoPorSemaforo = false; // true mientras el semáforo es lo que limita su velocidad (enciende las luces de freno).
    private int decisionAmarillo = 0; // Decidida una sola vez al empezar el amarillo: 1 = parar, -1 = cruzar, 0 = sin decidir.
    private float distanciaAlPare = 0; // Metros que le faltan hasta la línea de pare del semáforo (solo con esperandoSemaforo).

    public BotTrafico(int indice, float[][] ruta, float xInicial, float zInicial, int objetivoInicial, float velocidadBase, float[] color) {
        this.indice = indice;
        this.tipo = (indice == 0) ? Tipo.POLICIA : (indice == 1) ? Tipo.BOMBEROS : (indice == 2) ? Tipo.AMBULANCIA : Tipo.CIVIL;
        this.ruta = ruta;
        this.xInicial = xInicial;
        this.zInicial = zInicial;
        this.objetivoInicial = objetivoInicial;
        this.velocidadBase = velocidadBase;
        this.color = color;
        reiniciar();
    }

    // ==================== ESTADO ====================

    public int getIndice() { return indice; }

    public Tipo getTipo() { return tipo; }

    public float[] getColor() { return color; }

    public boolean isFrenando() { return frenando; }

    public void setFrenando(boolean frenando) { this.frenando = frenando; }

    public boolean estaEnChoque() { return tiempoChoque > 0; }

    public boolean isEsperandoSemaforo() { return esperandoSemaforo; }

    /** true mientras el semáforo reduce o mantiene detenido al bot: el controlador enciende sus luces de freno. */
    public boolean isFrenadoPorSemaforo() { return frenadoPorSemaforo; }

    /**
     * Decide si debe frenar ante el semáforo. Frena si la luz no está en verde, está a menos de
     * DISTANCIA_DETECCION_SEMAFORO (distancia euclidiana), regula su mismo sentido de marcha, queda por delante y todavía
     * no pasó la línea de pare. En amarillo sigue de largo si ya no alcanza a detenerse con un frenado cómodo.
     */
    public void evaluarSemaforo(Semaforo semaforo) {
        esperandoSemaforo = false;
        if (semaforo.getEstado() == Semaforo.Estado.VERDE) {
            decisionAmarillo = 0; // Con la luz verde se olvida la decisión del ciclo anterior.
            return;
        }
        float dx = semaforo.getX() - x;
        float dz = semaforo.getZ() - z;
        float distancia = (float) Math.sqrt(dx * dx + dz * dz); // Distancia euclidiana bot-semáforo.
        if (distancia >= Constantes.DISTANCIA_DETECCION_SEMAFORO) {
            return;
        }
        float frenteX = -(float) Math.sin(angulo); // Frente del bot (-sen, -cos).
        float frenteZ = -(float) Math.cos(angulo);
        if (frenteX * semaforo.getSentidoX() + frenteZ * semaforo.getSentidoZ() < 0.9f) { // El semáforo regula otro sentido de marcha.
            return;
        }
        float adelante = dx * frenteX + dz * frenteZ; // Cuánto falta hasta el semáforo siguiendo su avance.
        if (adelante <= 0) { // Ya lo dejó atrás.
            return;
        }
        float aLaLinea = adelante - Constantes.DISTANCIA_PARE_SEMAFORO; // Falta hasta la línea de pare (negativo: ya la pasó).
        if (aLaLinea < -Constantes.TOLERANCIA_LINEA_PARE) { // Ya está dentro del cruce: no frena en seco en medio.
            return;
        }
        if (semaforo.getEstado() == Semaforo.Estado.AMARILLO) {
            if (decisionAmarillo == 0) { // Primer cuadro con amarillo a la vista: decide una sola vez.
                float distanciaFrenado = velocidadActual * velocidadActual / (2.0f * Constantes.DECELERACION_SEMAFORO);
                decisionAmarillo = (velocidadActual > 0.5f && aLaLinea < distanciaFrenado) ? -1 : 1; // No alcanza a parar con comodidad: cruza.
            }
            if (decisionAmarillo < 0) {
                return;
            }
        }
        distanciaAlPare = Math.max(0, aLaLinea);
        esperandoSemaforo = true;
    }

    /**
     * Velocidad que realmente aplica este cuadro. Sin semáforo es exactamente la deseada. Con semáforo baja sin pasar de
     * FRENADO_MAXIMO_SEMAFORO por segundo, siguiendo la curva que lo deja en 0 justo en la línea de pare, y al ponerse
     * verde sube a ACELERACION_SEMAFORO por segundo hasta recuperar su velocidad normal.
     */
    private float velocidadConSemaforo(float velocidadDeseada, float deltaTime) {
        if (esperandoSemaforo) {
            float velocidadSegura = (float) Math.sqrt(2.0f * Constantes.DECELERACION_SEMAFORO * distanciaAlPare); // v² = 2·a·d: llega a 0 en la línea.
            float objetivo = Math.min(velocidadDeseada, velocidadSegura);
            if (objetivo < velocidadActual) {
                velocidadActual = Math.max(objetivo, velocidadActual - Constantes.FRENADO_MAXIMO_SEMAFORO * deltaTime);
            } else {
                velocidadActual = Math.min(objetivo, velocidadActual + Constantes.ACELERACION_SEMAFORO * deltaTime);
            }
            recuperandoVelocidad = true;
            frenadoPorSemaforo = objetivo < velocidadDeseada;
        } else if (recuperandoVelocidad && velocidadActual < velocidadDeseada) {
            frenadoPorSemaforo = false;
            velocidadActual = Math.min(velocidadDeseada, velocidadActual + Constantes.ACELERACION_SEMAFORO * deltaTime);
        } else {
            frenadoPorSemaforo = false;
            recuperandoVelocidad = false;
            velocidadActual = velocidadDeseada;
        }
        return velocidadActual;
    }

    /** Devuelve el bot a su esquina de partida, sin choques ni luces de freno. */
    public void reiniciar() {
        x = xInicial;
        z = zInicial;
        angulo = 0;
        objetivo = objetivoInicial; // Esquina objetivo original (1 en casi todos; 3 en el bot 1).
        tiempoChoque = 0; // Cancela choques pendientes.
        timerSusto = 0; // Se calma.
        velocidadActual = 0; // La toma de la velocidad normal en el primer cuadro libre.
        esperandoSemaforo = false;
        recuperandoVelocidad = false;
        frenadoPorSemaforo = false;
        decisionAmarillo = 0;
        frenando = false; // Apaga las luces de freno.
    }

    // ==================== SOMBRA Y LUCES SEGÚN EL TAMAÑO DEL VEHÍCULO ====================

    /** Los bomberos y la ambulancia son más largos y anchos que un auto. */
    @Override
    public float getAnchoSombra() { return (tipo == Tipo.BOMBEROS) ? 2.4f : (tipo == Tipo.AMBULANCIA) ? 2.2f : 2.1f; }

    @Override
    public float getLargoSombra() { return (tipo == Tipo.BOMBEROS) ? 4.6f : (tipo == Tipo.AMBULANCIA) ? 4.0f : 2.8f; }

    /** Distancia del centro a los faros delanteros: bomberos y ambulancia son más largos. */
    public float getDistanciaFaros() { return (tipo == Tipo.BOMBEROS) ? 2.4f : (tipo == Tipo.AMBULANCIA) ? 2.1f : 1.36f; }

    /** Separación lateral de los faros. */
    public float getSeparacionFaros() { return (tipo == Tipo.BOMBEROS) ? 0.6f : 0.55f; }

    /** Distancia al borde trasero, donde van las luces de freno (frente = -Z). */
    public float getTraseraFreno() { return (tipo == Tipo.BOMBEROS) ? 2.15f : (tipo == Tipo.AMBULANCIA) ? 1.85f : 1.36f; }

    /** Separación lateral de las luces de freno. */
    public float getLadoFreno() { return (tipo == Tipo.BOMBEROS || tipo == Tipo.AMBULANCIA) ? 0.7f : 0.55f; }

    /** Altura de las luces de freno. */
    public float getAlturaFreno() { return (tipo == Tipo.BOMBEROS || tipo == Tipo.AMBULANCIA) ? 0.9f : 0.72f; }

    // ==================== CHOQUE Y MOVIMIENTO ====================

    /** La bocina del jugador lo asusta: acelera a VELOCIDAD_BOT_ASUSTADO y se va calmando durante DURACION_SUSTO. */
    public void asustar() {
        timerSusto = Constantes.DURACION_SUSTO;
    }

    public boolean estaAsustado() { return timerSusto > 0; }

    /** Descuenta el susto; debe llamarse cada cuadro. */
    public void actualizarSusto(float deltaTime) {
        if (timerSusto > 0) {
            timerSusto = Math.max(0, timerSusto - deltaTime);
        }
    }

    /** Inicia un choque (0,4 s detenido y luego 1,1 s de retroceso) salvo que ya haya uno en curso. */
    public void iniciarChoque() {
        if (tiempoChoque <= 0) { // Evita reiniciar el choque si ya está en curso.
            tiempoChoque = Constantes.DURACION_CHOQUE_BOT;
        }
    }

    /**
     * Si el bot está en medio de un choque lo avanza (detenido, luego retrocede) y devuelve true;
     * mientras dura el choque no sigue su ruta ni consulta los radares.
     */
    public boolean actualizarChoque(float deltaTime) {
        if (tiempoChoque <= 0) { // Comprueba si el bot está en medio de un choque.
            return false;
        }
        frenando = true; // Detenido tras el choque: luces de freno encendidas.
        tiempoChoque -= deltaTime; // Consume el tiempo del choque.
        if (tiempoChoque < Constantes.UMBRAL_RETROCESO_BOT) { // Tras una breve espera detenido, empieza a retroceder.
            x += (float) Math.sin(angulo) * Constantes.VELOCIDAD_RETROCESO_BOT * deltaTime; // Retrocede en X (opuesto al frente -sen).
            z += (float) Math.cos(angulo) * Constantes.VELOCIDAD_RETROCESO_BOT * deltaTime; // Retrocede en Z (opuesto al frente -cos).
        }
        return true;
    }

    /** Avanza hacia la esquina objetivo de su ruta por el carril derecho y gira 90 grados al llegar. */
    public void avanzarPorRuta(float deltaTime, float velocidadGlobal) {
        int anterior = (objetivo + ruta.length - 1) % ruta.length; // Esquina de la que viene el bot.
        float tramoX = ruta[objetivo][0] - ruta[anterior][0]; // Dirección X del tramo actual.
        float tramoZ = ruta[objetivo][1] - ruta[anterior][1]; // Dirección Z del tramo actual.
        float largoTramo = (float) Math.sqrt(tramoX * tramoX + tramoZ * tramoZ); // Largo del tramo para normalizar.
        float derechaX = -tramoZ / largoTramo; // Componente X del vector hacia la derecha del avance.
        float derechaZ = tramoX / largoTramo; // Componente Z del vector hacia la derecha del avance.
        float objetivoX = ruta[objetivo][0] + derechaX * Constantes.DESVIO_CARRIL; // Esquina objetivo desplazada al carril derecho.
        float objetivoZ = ruta[objetivo][1] + derechaZ * Constantes.DESVIO_CARRIL; // Misma corrección en Z.
        float diferenciaX = objetivoX - x; // Calcula la separación horizontal al objetivo.
        float diferenciaZ = objetivoZ - z; // Calcula la separación en profundidad al objetivo.
        float distancia = (float) Math.sqrt(diferenciaX * diferenciaX + diferenciaZ * diferenciaZ); // Mide la distancia restante.
        float velocidadNormal = velocidadBase * velocidadGlobal / Constantes.VEL_BOTS_BASE; // Velocidad habitual del bot.
        float susto = timerSusto / Constantes.DURACION_SUSTO; // 1 recién asustado, 0 calmado.
        float velocidad = velocidadNormal + (Math.max(velocidadNormal, Constantes.VELOCIDAD_BOT_ASUSTADO) - velocidadNormal) * susto; // Vuelve a la normalidad poco a poco.
        velocidad = velocidadConSemaforo(velocidad, deltaTime); // El semáforo puede reducirla poco a poco hasta detener el bot.
        float paso = velocidad * deltaTime; // Distancia que avanza en este cuadro.
        if (distancia <= paso) { // Comprueba si alcanza la esquina en este cuadro.
            x = objetivoX; // Se coloca exactamente sobre la esquina.
            z = objetivoZ; // Evita que el error se acumule y se salga del asfalto.
            objetivo = (objetivo + 1) % ruta.length; // Pasa a la siguiente esquina y cierra el circuito.
        } else { // Todavía no llega: avanza en línea recta.
            x += diferenciaX / distancia * paso; // Avanza en X siguiendo la dirección normalizada.
            z += diferenciaZ / distancia * paso; // Avanza en Z siguiendo la dirección normalizada.
            angulo = (float) Math.atan2(-diferenciaX, -diferenciaZ); // Orienta el frente (-sen, -cos) hacia el movimiento.
        }
    }

    // ==================== DIBUJO ====================

    /** Dibuja el vehículo según su tipo y, si frena, las luces rojas traseras. */
    @Override
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        switch (tipo) {
            case POLICIA: // El primer bot es la patrulla de policía.
                PatrullaPolicia.dibujarModelo(r, x, z, angulo, tiempoAnimacion);
                break;
            case BOMBEROS: // El segundo bot es el camión de bomberos.
                dibujarCamionBomberos(r, x, z, angulo, tiempoAnimacion);
                break;
            case AMBULANCIA: // El tercer bot es la ambulancia.
                dibujarAmbulancia(r, x, z, angulo, tiempoAnimacion);
                break;
            default: // Del cuarto en adelante son autos civiles.
                dibujarAutoCivil(r, x, z, angulo, color[0], color[1], color[2]);
                break;
        }
        if (frenando) { // Luces de freno rojas en la parte trasera, según el tamaño de cada vehículo.
            r.entero("uEmision", 1);
            r.piezaVehiculo(x, z, angulo, -getLadoFreno(), getAlturaFreno(), getTraseraFreno(), 0.4f, 0.22f, 0.08f, 1.0f, 0.0f, 0.0f);
            r.piezaVehiculo(x, z, angulo, getLadoFreno(), getAlturaFreno(), getTraseraFreno(), 0.4f, 0.22f, 0.08f, 1.0f, 0.0f, 0.0f);
            r.entero("uEmision", 0);
        }
    }

    /** Dibuja el camión de bomberos: chasis largo rojo, cabina, escalera gris y sirena. */
    private static void dibujarCamionBomberos(Renderizador r, float x, float z, float angulo, float tiempo) {
        r.piezaVehiculo(x, z, angulo, 0, 0.9f, 0.5f, 1.9f, 0.9f, 3.2f, 0.80f, 0.10f, 0.08f); // Caja trasera roja.
        r.piezaVehiculo(x, z, angulo, 0, 1.0f, -1.7f, 1.9f, 1.1f, 1.3f, 0.85f, 0.12f, 0.10f); // Cabina delantera roja, más alta.
        r.piezaVehiculo(x, z, angulo, 0, 1.2f, -1.9f, 1.6f, 0.45f, 0.1f, 0.25f, 0.3f, 0.35f); // Parabrisas oscuro.
        r.piezaVehiculo(x, z, angulo, 0, 1.52f, 0.5f, 0.5f, 0.15f, 3f, 0.70f, 0.72f, 0.75f); // Escalera gris sobre el techo de la caja.
        r.piezaVehiculo(x, z, angulo, -1.0f, 0.45f, 0, 0.3f, 0.9f, 3.6f, 0.06f, 0.07f, 0.08f); // Ruedas del lado izquierdo.
        r.piezaVehiculo(x, z, angulo, 1.0f, 0.45f, 0, 0.3f, 0.9f, 3.6f, 0.06f, 0.07f, 0.08f); // Ruedas del lado derecho.
        boolean encendida = (int) (tiempo * 4) % 2 == 0; // Hace parpadear la sirena.
        r.entero("uEmision", encendida ? 1 : 0); // La sirena brilla en las fases pares.
        r.piezaVehiculo(x, z, angulo, 0, 1.7f, -1.7f, 1.0f, 0.2f, 0.3f, 1, 0.2f, 0.1f); // Sirena roja sobre la cabina.
        r.entero("uEmision", 0); // Devuelve la iluminación normal.
    }

    /** Dibuja la ambulancia: caja blanca con cruces rojas a los lados y sirenas roja y azul. */
    private static void dibujarAmbulancia(Renderizador r, float x, float z, float angulo, float tiempo) {
        r.piezaVehiculo(x, z, angulo, 0, 1.0f, 0.4f, 1.9f, 1.2f, 2.8f, 0.95f, 0.95f, 0.97f); // Caja blanca del compartimiento médico.
        r.piezaVehiculo(x, z, angulo, 0, 0.85f, -1.5f, 1.8f, 0.8f, 1.1f, 0.90f, 0.90f, 0.93f); // Cabina blanca más baja al frente.
        r.piezaVehiculo(x, z, angulo, 0, 1.0f, -2.0f, 1.5f, 0.4f, 0.1f, 0.2f, 0.28f, 0.35f); // Parabrisas oscuro.
        r.piezaVehiculo(x, z, angulo, -0.97f, 1.1f, 0.4f, 0.04f, 0.2f, 0.8f, 0.85f, 0.1f, 0.1f); // Cruz roja izquierda: brazo horizontal.
        r.piezaVehiculo(x, z, angulo, -0.97f, 1.1f, 0.4f, 0.04f, 0.8f, 0.2f, 0.85f, 0.1f, 0.1f); // Cruz roja izquierda: brazo vertical.
        r.piezaVehiculo(x, z, angulo, 0.97f, 1.1f, 0.4f, 0.04f, 0.2f, 0.8f, 0.85f, 0.1f, 0.1f); // Cruz roja derecha: brazo horizontal.
        r.piezaVehiculo(x, z, angulo, 0.97f, 1.1f, 0.4f, 0.04f, 0.8f, 0.2f, 0.85f, 0.1f, 0.1f); // Cruz roja derecha: brazo vertical.
        r.piezaVehiculo(x, z, angulo, -0.9f, 0.4f, 0, 0.26f, 0.6f, 3.2f, 0.06f, 0.07f, 0.08f); // Ruedas del lado izquierdo.
        r.piezaVehiculo(x, z, angulo, 0.9f, 0.4f, 0, 0.26f, 0.6f, 3.2f, 0.06f, 0.07f, 0.08f); // Ruedas del lado derecho.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla.
        r.entero("uEmision", rojoEncendido ? 1 : 0); // La roja brilla en las fases pares.
        r.piezaVehiculo(x, z, angulo, -0.5f, 1.7f, -1.4f, 0.5f, 0.2f, 0.3f, 0.9f, 0.1f, 0.1f); // Sirena roja sobre la cabina.
        r.entero("uEmision", rojoEncendido ? 0 : 1); // La azul brilla cuando la roja se apaga.
        r.piezaVehiculo(x, z, angulo, 0.5f, 1.7f, -1.4f, 0.5f, 0.2f, 0.3f, 0.1f, 0.25f, 0.95f); // Sirena azul sobre la cabina.
        r.entero("uEmision", 0); // Devuelve la iluminación normal.
    }

    /** Dibuja un auto simplificado de color propio con 5 llamadas a cajaGirada(); también lo usa la decoración de la gasolinera. */
    public static void dibujarAutoCivil(Renderizador rend, float x, float z, float angulo, float cr, float cg, float cb) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación del bot.
        float seno = (float) Math.sin(angulo); // Seno de la misma orientación.
        rend.cajaGirada(x, 0.65f, z, 1.65f, 0.55f, 2.6f, cr, cg, cb, angulo); // Carrocería del color del bot.
        rend.cajaGirada(x + seno * 0.12f, 1.12f, z + coseno * 0.12f, 1.3f, 0.55f, 1.25f, 0.20f, 0.25f, 0.30f, angulo); // Cabina oscura, un poco hacia atrás.
        rend.cajaGirada(x + coseno * -0.88f, 0.38f, z - seno * -0.88f, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f, angulo); // Fila de ruedas del lado izquierdo.
        rend.cajaGirada(x + coseno * 0.88f, 0.38f, z - seno * 0.88f, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f, angulo); // Fila de ruedas del lado derecho.
        rend.cajaGirada(x - seno * 1.32f, 0.68f, z - coseno * 1.32f, 1.2f, 0.2f, 0.07f, 1, 0.95f, 0.65f, angulo); // Franja de faros en el frente.
    }
}
