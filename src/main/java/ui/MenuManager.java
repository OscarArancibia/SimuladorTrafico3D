package ui; // Interfaz y HUD.

/**
 * Estados de la partida: pantalla de inicio, jugando, pausa, Game Over y victoria.
 * Centraliza las transiciones para que el resto del juego solo pregunte "¿estoy jugando?".
 */
public class MenuManager {

    /** Estado actual de la partida. */
    public enum Estado { INICIO, JUGANDO, PAUSA, GAME_OVER, VICTORIA }

    public static final String TITULO_INICIO = "Presiona ESPACIO o ENTER para iniciar";
    public static final String TITULO_PAUSA = "JUEGO PAUSADO";
    public static final String TITULO_GAME_OVER = "¡GAME OVER! Presiona [R] para reiniciar.";

    private Estado estado = Estado.INICIO; // El juego espera en la pantalla de inicio hasta pulsar Espacio o Enter.
    private boolean tituloInicioPuesto = false; // Evita reescribir el título de inicio en cada cuadro.

    public Estado getEstado() { return estado; }

    public boolean isInicio() { return estado == Estado.INICIO; }

    public boolean isPausa() { return estado == Estado.PAUSA; }

    public boolean isGameOver() { return estado == Estado.GAME_OVER; }

    public boolean isVictoria() { return estado == Estado.VICTORIA; }

    /** true cuando nada debe moverse ni actualizarse: pausa, Game Over o victoria (el inicio se trata aparte). */
    public boolean isCongelado() { return estado == Estado.PAUSA || estado == Estado.GAME_OVER || estado == Estado.VICTORIA; }

    /** true solo la primera vez que se pregunta durante el inicio: el llamador debe fijar entonces el título de inicio. */
    public boolean consumirTituloInicio() {
        if (tituloInicioPuesto) {
            return false;
        }
        tituloInicioPuesto = true;
        return true;
    }

    /**
     * Espacio o Enter: en el inicio comienza el juego; jugando alterna la pausa; en Game Over o victoria no hace nada.
     * @return true si el juego quedó en pausa (el llamador debe fijar el título de pausa)
     */
    public boolean alternarInicioPausa() {
        if (estado == Estado.INICIO) {
            estado = Estado.JUGANDO; // Comienza el juego; el título normal se restaura en el siguiente cuadro.
            return false;
        }
        if (estado == Estado.JUGANDO) {
            estado = Estado.PAUSA;
            return true;
        }
        if (estado == Estado.PAUSA) {
            estado = Estado.JUGANDO;
        }
        return false;
    }

    /** Tecla R: quita la pausa, el Game Over o la victoria; en la pantalla de inicio sigue esperando. */
    public void reiniciar() {
        if (estado != Estado.INICIO) {
            estado = Estado.JUGANDO;
        }
    }

    /** Se acabaron las vidas: congela el juego y muestra la X roja. */
    public void terminarPorGameOver() {
        estado = Estado.GAME_OVER;
    }

    /** Se recogieron todas las monedas: congela el juego y muestra la V dorada. */
    public void terminarPorVictoria() {
        estado = Estado.VICTORIA;
    }
}
