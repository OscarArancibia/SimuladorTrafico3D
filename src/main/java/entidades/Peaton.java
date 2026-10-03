package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el modelo.
import utilidades.Constantes; // Velocidad, radio de atropello y recorrido.

/**
 * Persona que camina de ida y vuelta por una acera. Si el auto del jugador la toca queda aplastada unos segundos.
 * Este tipo de entidad no sabe qué consecuencias tiene el atropello: avisa y el juego decide.
 */
public class Peaton {

    private float x; // Posición X actual.
    private float z; // Posición Z fija: acera pegada a una calle.
    private float angulo; // Orientación: -1,57 camina hacia +X y 1,57 hacia -X.
    private final float origenX; // Centro del recorrido, sobre el eje de una calle vertical: va 14 unidades a cada lado, así que la cruza.
    private final float[] color; // Color RGB de la ropa.
    private float aplastado = 0; // 0 = normal; mayor que 0 = aplastado (segundos transcurridos).

    public Peaton(float x, float z, float angulo, float origenX, float[] color) {
        this.x = x;
        this.z = z;
        this.angulo = angulo;
        this.origenX = origenX;
        this.color = color;
    }

    public boolean estaAplastado() { return aplastado > 0; }

    /** Los peatones aplastados vuelven a la normalidad. */
    public void restaurar() { aplastado = 0; }

    /**
     * Hace caminar al peatón y comprueba el atropello por el jugador en (jugadorX, jugadorZ).
     * @return true si el jugador lo atropelló justo en este cuadro
     */
    public boolean actualizar(float deltaTime, float jugadorX, float jugadorZ) {
        boolean atropellado = false;
        float diferenciaX = jugadorX - x; // Separación horizontal entre el auto del jugador y el peatón.
        float diferenciaZ = jugadorZ - z; // Separación en profundidad entre ambos.
        if (aplastado == 0 && diferenciaX * diferenciaX + diferenciaZ * diferenciaZ < Constantes.RADIO_ATROPELLO_CUAD) { // Radio de colisión de 3 unidades.
            aplastado = Constantes.INICIO_APLASTADO; // Inicia el aplastamiento.
            atropellado = true;
        }
        if (aplastado > 0) { // Comprueba si el peatón está aplastado.
            aplastado += deltaTime; // Cuenta los segundos que lleva aplastado.
            if (aplastado > Constantes.DURACION_APLASTADO) { // Pasados 3 segundos...
                aplastado = 0; // ...se infla y vuelve a su forma normal.
            }
            return atropellado; // Mientras está aplastado no se mueve.
        }
        x -= (float) Math.sin(angulo) * Constantes.VELOCIDAD_PEATON * deltaTime; // Avanza a 1 unidad por segundo según el frente (-sen).
        z -= (float) Math.cos(angulo) * Constantes.VELOCIDAD_PEATON * deltaTime; // Avanza en Z según el frente (-cos); es casi cero.
        if (Math.abs(x - origenX) > Constantes.RECORRIDO_PEATON) { // Comprueba si llegó al extremo de su recorrido.
            angulo += (float) Math.PI; // Da media vuelta para regresar.
            x = origenX + Math.signum(x - origenX) * Constantes.RECORRIDO_PEATON; // Lo deja justo en el extremo para no quedar fuera.
        }
        return atropellado;
    }

    /** Dibuja al peatón con la ropa de su color; aplastado si fue atropellado. */
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        dibujarModelo(r, x, z, angulo, tiempoAnimacion, color[0], color[1], color[2], aplastado > 0);
    }

    /** Dibuja una persona normal (sin aplastar); la usan también el personal de las estaciones, las paradas y la gasolinera. */
    public static void dibujarModelo(Renderizador r, float x, float z, float angulo, float tiempo, float cr, float cg, float cb) {
        dibujarModelo(r, x, z, angulo, tiempo, cr, cg, cb, false); // Delega en la versión completa.
    }

    /** Dibuja una persona de 6 partes que camina; si estaAplastado, su altura se reduce al 10 % contra la acera. */
    public static void dibujarModelo(Renderizador rend, float x, float z, float angulo, float tiempo, float cr, float cg, float cb, boolean estaAplastado) {
        float escalaY = 1.0f; // Factor de altura: 1 = normal.
        if (estaAplastado) { // Comprueba si el peatón fue atropellado.
            escalaY = Constantes.ESCALA_Y_APLASTADO; // Reduce alturas y posiciones verticales al 10 %.
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
        rend.cajaGirada(x - lateralX * 0.1f + desvioX, 0.3f + 0.4f * escalaY, z - lateralZ * 0.1f + desvioZ, 0.17f, 0.8f * escalaY, 0.2f, 0.25f, 0.27f, 0.32f, angulo); // Pierna izquierda (suma el balanceo), pantalón gris azulado.
        rend.cajaGirada(x + lateralX * 0.1f - desvioX, 0.3f + 0.4f * escalaY, z + lateralZ * 0.1f - desvioZ, 0.17f, 0.8f * escalaY, 0.2f, 0.25f, 0.27f, 0.32f, angulo); // Pierna derecha (resta el balanceo).
        rend.cajaGirada(x, 0.3f + 1.125f * escalaY, z, 0.4f, 0.65f * escalaY, 0.24f, cr, cg, cb, angulo); // Torso con el color de la ropa, justo sobre las piernas.
        rend.cajaGirada(x - lateralX * 0.26f - desvioX * 0.8f, 0.3f + 1.14f * escalaY, z - lateralZ * 0.26f - desvioZ * 0.8f, 0.11f, 0.62f * escalaY, 0.14f, cr, cg, cb, angulo); // Brazo izquierdo (resta: opuesto a la pierna izquierda).
        rend.cajaGirada(x + lateralX * 0.26f + desvioX * 0.8f, 0.3f + 1.14f * escalaY, z + lateralZ * 0.26f + desvioZ * 0.8f, 0.11f, 0.62f * escalaY, 0.14f, cr, cg, cb, angulo); // Brazo derecho (suma: opuesto a la pierna derecha).
        rend.cajaGirada(x, 0.3f + 1.59f * escalaY, z, 0.28f, 0.28f * escalaY, 0.28f, 0.90f, 0.70f, 0.50f, angulo); // Cabeza color piel sobre el torso.
    }
}
