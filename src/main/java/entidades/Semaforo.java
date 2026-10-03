package entidades; // Objetos que existen en el mundo.

import utilidades.Constantes; // Duraciones de cada luz.

/**
 * Semáforo de la ciudad: ciclo verde, amarillo y rojo. Solo expone su estado, su posición y el sentido del tráfico que
 * controla; decidir si frenar es tarea de cada vehículo.
 */
public final class Semaforo {

    /** Luz encendida en este momento. */
    public enum Estado { VERDE, AMARILLO, ROJO }

    private final float x; // Posición del poste en el mundo.
    private final float z;
    private final float sentidoX; // Dirección (vector unitario) del tráfico que debe obedecerlo.
    private final float sentidoZ;
    private Estado estado = Estado.VERDE;
    private float tiempoEnEstado = 0; // Segundos transcurridos con la luz actual.

    public Semaforo(float x, float z, float sentidoX, float sentidoZ) {
        this.x = x;
        this.z = z;
        this.sentidoX = sentidoX;
        this.sentidoZ = sentidoZ;
    }

    public float getX() { return x; }

    public float getZ() { return z; }

    public float getSentidoX() { return sentidoX; }

    public float getSentidoZ() { return sentidoZ; }

    public Estado getEstado() { return estado; }

    /** Vuelve a verde desde el principio del ciclo. */
    public void reiniciar() {
        estado = Estado.VERDE;
        tiempoEnEstado = 0;
    }

    /** Máquina de estados: 10 s verde, 3 s amarillo, 10 s rojo y vuelta a empezar. */
    public void actualizar(float deltaTime) {
        tiempoEnEstado += deltaTime;
        if (estado == Estado.VERDE && tiempoEnEstado > Constantes.SEMAFORO_DURACION_VERDE) { estado = Estado.AMARILLO; tiempoEnEstado = 0; }
        else if (estado == Estado.AMARILLO && tiempoEnEstado > Constantes.SEMAFORO_DURACION_AMARILLO) { estado = Estado.ROJO; tiempoEnEstado = 0; }
        else if (estado == Estado.ROJO && tiempoEnEstado > Constantes.SEMAFORO_DURACION_ROJO) { estado = Estado.VERDE; tiempoEnEstado = 0; }
    }
}
