import core.Juego; // Reglas y orquestación de la partida.
import core.Ventana; // GLFW y OpenGL.
import utilidades.Constantes; // Nombre del juego.

/**
 * Punto de entrada y bucle principal: crea la ventana y el juego, y repite eventos, actualización, dibujo y presentación
 * hasta que se cierre la ventana, liberando los recursos aunque algo falle.
 */
public class Main {

    public static void main(String[] args) {
        Ventana ventana = new Ventana();
        Juego juego = null;
        try { // Protege el arranque y el ciclo para poder liberar recursos si fallan.
            ventana.iniciar(Constantes.NOMBRE_JUEGO); // Crea la ventana y el contexto OpenGL.
            juego = new Juego(ventana); // Compila los shaders y crea todas las entidades.
            bucle(ventana, juego); // Actualiza y dibuja hasta que se cierre la ventana.
        } finally { // Este bloque se ejecuta tanto al salir normalmente como al fallar.
            if (juego != null) {
                juego.liberar(); // Libera la GPU.
            }
            ventana.destruir(); // Libera los callbacks y la ventana.
        }
    }

    /** Mantiene el ciclo continuo de eventos, actualización, dibujo y presentación. */
    private static void bucle(Ventana ventana, Juego juego) {
        double tiempoAnterior = ventana.tiempo(); // Guarda el instante inicial para calcular el tiempo entre cuadros.
        int cuadros = 0; // Cuenta los cuadros dibujados para la comprobación automática opcional.
        int maxCuadros = Integer.getInteger("demo.frames", 0); // Cero permite jugar; otro valor limita el arranque de prueba.

        while (!ventana.debeCerrar()) { // Repite mientras no se haya solicitado cerrar.
            ventana.procesarEventos(); // Procesa teclado, redimensionamiento y botón de cierre.
            double tiempoActual = ventana.tiempo(); // Consulta el tiempo actual en segundos.
            float deltaTime = (float) Math.min(tiempoActual - tiempoAnterior, 0.05); // Evita saltos de más de 50 ms.
            tiempoAnterior = tiempoActual; // Conserva el instante de este cuadro para la próxima vuelta.
            juego.actualizar(deltaTime); // Actualiza el mundo y las reglas.
            ventana.actualizarTamano(); // Obtiene los píxeles reales, también en pantallas Retina.
            if (ventana.getAncho() > 0 && ventana.getAlto() > 0) { // Evita dibujar y dividir por cero cuando la ventana está minimizada.
                juego.dibujarFrame(); // Genera la imagen de la ciudad para este cuadro.
            }
            ventana.intercambiarBuffers(); // Muestra la imagen terminada intercambiando los buffers.
            cuadros++; // Registra que se completó una vuelta del ciclo.
            if (maxCuadros > 0 && cuadros >= maxCuadros) { // Detecta el final de un arranque automático limitado.
                ventana.pedirCierre(); // Solicita salir por el mismo camino que un cierre normal.
            }
        }
    }
}
