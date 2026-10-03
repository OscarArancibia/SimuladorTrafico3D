package entidades; // Objetos que existen en el mundo.

import core.Input; // Teclas mantenidas para conducir.
import graficos.Renderizador; // Primitivas para dibujar el modelo.
import sistemas.GestorColisiones; // Comprueba que la posición propuesta sea transitable.
import utilidades.Constantes; // Velocidades, resistencias y posición inicial.
import static org.lwjgl.glfw.GLFW.*; // Teclas y reloj para el parpadeo de los guiñadores.

/**
 * El auto que conduce el jugador: aceleración, freno, giro, choque contra edificios y bordes, y modelo con luces.
 * Los efectos de las píldoras (velocidad máxima y aceleración) los ajusta el juego mediante los setters.
 */
public class AutoJugador extends Vehiculo {

    /** Aspecto del auto: normal, o uno de los modelos que desbloquean los códigos secretos. */
    public enum Modelo { NORMAL, TANQUE, POLI }

    private float velocidadMaxima = Constantes.VELOCIDAD_MAXIMA_NORMAL; // Tope de avance en unidades por segundo (60 km/h); los power-ups lo modifican.
    private float aceleracionMotor = Constantes.ACELERACION_NORMAL; // Aceleración del motor en u/s²; debe superar velocidadMaxima * 0,7 para alcanzar el tope.
    private float velocidadImpacto = 0; // Velocidad con la que el auto chocó contra un edificio o el borde en este cuadro (0 si no hubo choque).
    private boolean jugadorFrenando = false; // true mientras se pulsa freno (S, flecha abajo o Espacio): enciende las luces rojas.
    private int direccionGiro = 0; // -1 = girando a la izquierda, 1 = a la derecha, 0 = recto (activa los guiñadores).
    private boolean derrapando = false; // true si gira rápido o frena avanzando rápido: deja marcas en el asfalto.
    private float velocidad = 0; // Unidades por segundo; un valor negativo significa reversa.
    private boolean modoDios = false; // Código FICCT: no choca con edificios y el juego no le quita vidas.
    private Modelo modelo = Modelo.NORMAL; // Los códigos secretos TAN/TAK y POLI cambian el modelo 3D.
    private boolean capoLevantado = false; // Lo decide el SistemaDano según el daño acumulado; solo afecta al dibujo.

    public AutoJugador() {
        x = Constantes.JUGADOR_X_INICIAL; // Posición horizontal inicial: centro de una calle.
        z = Constantes.JUGADOR_Z_INICIAL; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
        angulo = 0; // Orienta el frente hacia -Z.
    }

    // ==================== ESTADO ====================

    public float getVelocidad() { return velocidad; }

    public float getVelocidadMaxima() { return velocidadMaxima; }

    public float getVelocidadImpacto() { return velocidadImpacto; }

    public boolean isFrenando() { return jugadorFrenando; }

    public int getDireccionGiro() { return direccionGiro; }

    public boolean isDerrapando() { return derrapando; }

    public boolean isModoDios() { return modoDios; }

    public void setModoDios(boolean modoDios) { this.modoDios = modoDios; }

    public Modelo getModelo() { return modelo; }

    /** El tanque es más ancho y largo que el auto; la patrulla mide lo mismo. */
    @Override
    public float getAnchoSombra() { return (modelo == Modelo.TANQUE) ? 2.8f : super.getAnchoSombra(); }

    /** El tanque es más largo: sus faros salen desde el frente del casco. */
    @Override
    public float getDistanciaFaros() { return (modelo == Modelo.TANQUE) ? 1.9f : super.getDistanciaFaros(); }

    @Override
    public float getLargoSombra() { return (modelo == Modelo.TANQUE) ? 3.5f : super.getLargoSombra(); }

    public void setModelo(Modelo modelo) { this.modelo = modelo; }

    /** Indica si el capó se dibuja levantado (daño nivel 3 o más); el juego lo consulta al SistemaDano antes de dibujar. */
    public void setCapoLevantado(boolean capoLevantado) { this.capoLevantado = capoLevantado; }

    public void setVelocidadMaxima(float velocidadMaxima) { this.velocidadMaxima = velocidadMaxima; }

    public void setAceleracionMotor(float aceleracionMotor) { this.aceleracionMotor = aceleracionMotor; }

    /** Detiene el auto. */
    public void detener() { velocidad = 0; }

    /** Coloca el auto en una posición corregida (por ejemplo, devuelto a la previa al rozar un bot). */
    public void setPosicion(float nuevoX, float nuevoZ) {
        x = nuevoX;
        z = nuevoZ;
    }

    /** Devuelve el auto al punto de inicio con su orientación original, sin tocar la velocidad. */
    public void reaparecer() {
        x = Constantes.JUGADOR_X_INICIAL; // Vuelve al punto de inicio en X.
        z = Constantes.JUGADOR_Z_INICIAL; // Vuelve al punto de inicio en Z.
        angulo = 0; // Recupera la orientación inicial.
    }

    /** Coloca nuevamente el auto en su punto de partida, detenido, con los valores normales de motor y sin efectos de códigos secretos. */
    public void reiniciar() {
        velocidadMaxima = Constantes.VELOCIDAD_MAXIMA_NORMAL;
        aceleracionMotor = Constantes.ACELERACION_NORMAL;
        derrapando = false;
        modoDios = false; // Reiniciar quita los efectos de los códigos secretos.
        modelo = Modelo.NORMAL;
        reaparecer();
        velocidad = 0; // Detiene cualquier movimiento previo.
    }

    // ==================== MOVIMIENTO POR CUADRO ====================

    /** Actualiza la conducción; deltaTime contiene los segundos transcurridos entre cuadros. */
    public void actualizar(float deltaTime, Input input) {
        velocidadImpacto = 0; // Se recalcula en cada cuadro.
        float acelerador = 0; // Sin teclas pulsadas no se aplica aceleración del motor.

        if (input.pulsada(GLFW_KEY_W) || input.pulsada(GLFW_KEY_UP)) { // Acepta W o flecha arriba para avanzar.
            acelerador += 1; // Solicita aceleración hacia delante.
        }

        if (input.pulsada(GLFW_KEY_S) || input.pulsada(GLFW_KEY_DOWN)) { // Acepta S o flecha abajo para retroceder.
            acelerador -= 1; // Primero reduce la velocidad positiva y después entra en reversa.
        }

        jugadorFrenando = input.pulsada(GLFW_KEY_S) || input.pulsada(GLFW_KEY_DOWN) || input.pulsada(GLFW_KEY_SPACE); // Teclas de freno/reversa.
        velocidad += acelerador * aceleracionMotor * deltaTime; // Integra la aceleración del motor (con la resistencia de 0,7 alcanza el tope).
        float resistencia = Constantes.RESISTENCIA_RODADO; // Define la pérdida de velocidad normal al rodar.

        if (input.pulsada(GLFW_KEY_SPACE)) { // Detecta si el usuario mantiene presionado el freno.
            resistencia = Constantes.RESISTENCIA_FRENO; // Aumenta la pérdida de velocidad para detenerse rápidamente.
        }

        float factorFrenado = (float) Math.exp(-resistencia * deltaTime); // Calcula la fracción de velocidad conservada.
        velocidad *= factorFrenado; // Aplica resistencia de forma proporcional al tiempo transcurrido.
        velocidad = Math.max(Constantes.REVERSA_MAXIMA, Math.min(velocidadMaxima, velocidad)); // Limita la reversa a -6 y el avance al tope actual.
        float direccion = 0; // Sin dirección presionada el volante permanece recto.

        if (input.pulsada(GLFW_KEY_A) || input.pulsada(GLFW_KEY_LEFT)) { // Comprueba el giro hacia la izquierda.
            direccion += 1; // Selecciona el sentido positivo de rotación.
        }

        if (input.pulsada(GLFW_KEY_D) || input.pulsada(GLFW_KEY_RIGHT)) { // Comprueba el giro hacia la derecha.
            direccion -= 1; // Selecciona el sentido negativo de rotación.
        }

        direccionGiro = (int) -direccion; // direccion vale +1 a la izquierda y -1 a la derecha; direccionGiro usa el signo opuesto.
        angulo += direccion * velocidad * Constantes.FACTOR_GIRO * deltaTime; // Gira según la velocidad; en reversa invierte el giro.
        float frenteX = -(float) Math.sin(angulo); // Obtiene la componente X del frente del vehículo.
        float frenteZ = -(float) Math.cos(angulo); // Obtiene la componente Z; con ángulo cero vale -1.
        float siguienteX = x + frenteX * velocidad * deltaTime; // Propone la nueva posición X.
        float siguienteZ = z + frenteZ * velocidad * deltaTime; // Propone la nueva posición Z.

        boolean transitable = modoDios ? GestorColisiones.dentroDelMapa(siguienteX, siguienteZ) // Modo dios: atraviesa edificios, pero no sale del mapa.
                : GestorColisiones.puedeCircular(siguienteX, siguienteZ);
        if (transitable) { // Comprueba la posición antes de mover el auto.
            x = siguienteX; // Acepta el desplazamiento horizontal.
            z = siguienteZ; // Acepta el desplazamiento en profundidad.
        } else { // La posición propuesta invadiría una manzana o saldría del mapa.
            velocidadImpacto = Math.abs(velocidad); // Guarda la velocidad del impacto antes de detener el auto.
            velocidad = 0; // Detiene el auto conservando su última posición válida.
        }

        // El derrape ocurre si giras a más del 60 % de la velocidad máxima, o si frenas (S o Espacio) avanzando a más del 40 %.
        // Se calcula con la velocidad final del cuadro: un auto detenido por un choque no derrapa.
        derrapando = (Math.abs(velocidad) > Constantes.FRACCION_DERRAPE_GIRO * velocidadMaxima && direccionGiro != 0)
                || (velocidad > Constantes.FRACCION_DERRAPE_FRENO * velocidadMaxima && jugadorFrenando);
    }

    // ==================== DIBUJO ====================

    /** Dibuja el modelo activo: auto normal, tanque o patrulla de policía. */
    @Override
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        switch (modelo) {
            case TANQUE:
                dibujarTanque(r);
                break;
            case POLI: // Reutiliza el modelo de la patrulla de la IA, con las luces de freno y guiñadores del jugador.
                PatrullaPolicia.dibujarModelo(r, x, z, angulo, tiempoAnimacion * 2.5f);
                dibujarLucesDeControl(r, 1.36f, 0.82f, 1.28f);
                if (capoLevantado) dibujarCapoLevantado(r, 0.92f, 0.92f, 0.95f); // Capó del color blanco de la patrulla.
                break;
            default:
                dibujarAutoNormal(r);
                break;
        }
    }

    /** Construye el auto con cajas: carrocería roja, cabina, ruedas, luces de freno, guiñadores y faros. */
    private void dibujarAutoNormal(Renderizador r) {
        pieza(r, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.95f, 0.24f, 0.12f); // Dibuja la carrocería roja.
        pieza(r, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.22f, 0.65f, 0.78f); // Dibuja la cabina azulada.
        float[] ladosRuedas = {-0.88f, 0.88f}; // Ubica ruedas a izquierda y derecha del auto.
        float[] ejesRuedas = {-0.82f, 0.82f}; // Ubica las ruedas delanteras y traseras.

        for (float lado : ladosRuedas) { // Selecciona uno de los dos lados del vehículo.
            for (float eje : ejesRuedas) { // Selecciona el eje delantero o trasero.
                pieza(r, lado, 0.38f, eje, 0.24f, 0.58f, 0.6f, 0.055f, 0.065f, 0.08f); // Dibuja una rueda oscura.
            }
        }

        dibujarLucesDeControl(r, 1.36f, 0.82f, 1.28f); // Luces de freno y guiñadores.
        float[] ladosFaros = {-0.55f, 0.55f}; // Define la separación lateral de las luces.
        for (float lado : ladosFaros) { // Repite el dibujo para ambos lados.
            pieza(r, lado, 0.68f, -1.32f, 0.38f, 0.2f, 0.07f, 1, 0.95f, 0.65f); // Dibuja un faro delantero claro.
            pieza(r, lado, 0.68f, 1.32f, 0.35f, 0.17f, 0.07f, 0.85f, 0.05f, 0.05f); // Dibuja una luz trasera roja.
        }
        if (capoLevantado) dibujarCapoLevantado(r, 0.95f, 0.24f, 0.12f); // Capó del color rojo de la carrocería.
    }

    /**
     * Capó levantado por el daño: el motor queda a la vista sobre el frente de la carrocería y la placa del capó se gira
     * hacia arriba ANGULO_CAPO_LEVANTADO grados, con la bisagra en la base del parabrisas (el equivalente en cajas de
     * glRotatef(-30, 1, 0, 0): OpenGL 3.3 core no tiene matrices de pila). Sirve para la carrocería del auto y de la patrulla.
     */
    private void dibujarCapoLevantado(Renderizador r, float cr, float cg, float cb) {
        pieza(r, 0, 1.0f, -0.9f, 1.0f, 0.15f, 0.6f, 0.18f, 0.18f, 0.20f); // Bloque del motor, oscuro, sobre la carrocería.
        float inclinacion = (float) Math.toRadians(Constantes.ANGULO_CAPO_LEVANTADO);
        float largo = 0.8f; // Largo del capó, desde el parabrisas hasta el parachoques.
        float bisagraY = 0.93f; // Altura de la bisagra: la parte superior de la carrocería.
        float bisagraZ = -0.5f; // Base del parabrisas.
        float centroY = bisagraY + (float) Math.sin(inclinacion) * largo / 2; // El centro de la placa queda a media longitud de la bisagra.
        float centroZ = bisagraZ - (float) Math.cos(inclinacion) * largo / 2;
        piezaInclinada(r, 0, centroY, centroZ, 1.65f, 0.06f, largo, cr, cg, cb, inclinacion);
    }

    /** Como pieza(), pero con la caja inclinada alrededor de su eje X (el frente del auto es -Z). */
    private void piezaInclinada(Renderizador r, float lx, float ly, float lz, float sx, float sy, float sz, float cr, float cg, float cb, float inclinacion) {
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        r.cajaInclinada(x + coseno * lx + seno * lz, ly, z - seno * lx + coseno * lz, sx, sy, sz, cr, cg, cb, angulo, inclinacion);
    }

    /**
     * Tanque del código TANK: dos orugas, casco verde oscuro, torreta con escotilla y un cañón largo hacia delante (-Z).
     * El cañón va por debajo de la altura de los ojos de la cámara en primera persona (1,4) para no taparla.
     */
    private void dibujarTanque(Renderizador r) {
        pieza(r, -1.05f, 0.45f, 0, 0.45f, 0.9f, 3.4f, 0.10f, 0.11f, 0.10f); // Oruga izquierda.
        pieza(r, 1.05f, 0.45f, 0, 0.45f, 0.9f, 3.4f, 0.10f, 0.11f, 0.10f); // Oruga derecha.
        pieza(r, 0, 0.75f, 0, 1.7f, 0.5f, 3.0f, 0.18f, 0.28f, 0.14f); // Casco verde oscuro, ancho y bajo.
        pieza(r, 0, 0.70f, -1.6f, 1.7f, 0.35f, 0.4f, 0.20f, 0.31f, 0.16f); // Frente del casco, un poco más claro.
        pieza(r, 0, 1.25f, 0.35f, 1.2f, 0.5f, 1.3f, 0.16f, 0.25f, 0.12f); // Torreta.
        pieza(r, 0.3f, 1.55f, 0.5f, 0.4f, 0.1f, 0.4f, 0.12f, 0.20f, 0.10f); // Escotilla de la torreta.
        pieza(r, 0, 1.15f, -1.45f, 0.2f, 0.2f, 2.3f, 0.10f, 0.15f, 0.08f); // Cañón largo hacia delante.
        pieza(r, 0, 1.15f, -2.65f, 0.3f, 0.3f, 0.2f, 0.06f, 0.08f, 0.05f); // Boca del cañón.
        dibujarLucesDeControl(r, 1.6f, 0.95f, 1.6f); // Luces de freno y guiñadores en la parte trasera y delantera del casco.
    }

    /** Luces de freno (rojo puro) y guiñadores naranjas parpadeantes, para cualquier modelo; las medidas dependen del largo y ancho. */
    private void dibujarLucesDeControl(Renderizador r, float zFreno, float ladoGuinador, float zGuinador) {
        if (jugadorFrenando) { // Luces de freno: rojo puro y más grandes que las traseras normales.
            r.entero("uEmision", 1);
            pieza(r, -0.55f, 0.72f, zFreno, 0.45f, 0.25f, 0.08f, 1.0f, 0.0f, 0.0f); // Luz de freno izquierda.
            pieza(r, 0.55f, 0.72f, zFreno, 0.45f, 0.25f, 0.08f, 1.0f, 0.0f, 0.0f); // Luz de freno derecha.
            r.entero("uEmision", 0);
        }
        boolean luzParpadea = ((int) (glfwGetTime() * 8)) % 2 == 0; // Parpadeo: 4 encendidos por segundo.
        if (direccionGiro != 0 && luzParpadea) { // Guiñadores naranjas en las dos esquinas del lado hacia el que se gira.
            float lado = direccionGiro * ladoGuinador; // -x = izquierda, +x = derecha (el auto mira hacia -Z).
            r.entero("uEmision", 1); // Brillan aunque sea de noche.
            pieza(r, lado, 0.68f, -zGuinador, 0.25f, 0.2f, 0.25f, 1.0f, 0.5f, 0.0f); // Guiñador delantero.
            pieza(r, lado, 0.68f, zGuinador, 0.25f, 0.2f, 0.25f, 1.0f, 0.5f, 0.0f); // Guiñador trasero.
            r.entero("uEmision", 0); // Restablece el material normal.
        }
    }

    /** Transforma una pieza del espacio local del auto al espacio de la ciudad. */
    private void pieza(Renderizador r, float lx, float ly, float lz, float sx, float sy, float sz, float cr, float cg, float cb) {
        float coseno = (float) Math.cos(angulo); // Calcula el coseno de la orientación del auto.
        float seno = (float) Math.sin(angulo); // Calcula el seno de la misma orientación.
        float mundoX = x + coseno * lx + seno * lz; // Gira la posición local y suma la posición X del auto.
        float mundoZ = z - seno * lx + coseno * lz; // Gira la posición local y suma la posición Z del auto.
        r.cajaGirada(mundoX, ly, mundoZ, sx, sy, sz, cr, cg, cb, angulo); // Dibuja la pieza con la orientación del vehículo.
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    public void dibujarIndicadorMinimapa(Renderizador r) {
        r.cajaGirada(x, 25, z, 4.4f, 0.1f, 6.4f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = x + frenteX * 4; // Desplaza la punta cuatro unidades hacia delante en X.
        float puntaZ = z + frenteZ * 4; // Desplaza la punta cuatro unidades hacia delante en Z.
        r.caja(puntaX, 26, puntaZ, 1.8f, 0.1f, 1.8f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }
}
