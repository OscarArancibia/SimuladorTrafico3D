package sistemas; // Lógicas invisibles del juego: clima, IA, colisiones, daño, bocina.

import graficos.GestorParticulas; // Dibuja la onda de la bocina.
import utilidades.Constantes; // Cadencia de las ondas.

/**
 * Bocina del jugador: mientras se mantiene pulsada asusta a los bots cercanos y lanza una onda visual cada 0,5 s.
 * Solo coordina; el susto lo aplica el ControladorTrafico y el anillo lo dibuja el GestorParticulas.
 */
public class SistemaBocina {

    private final GestorParticulas particulas;
    private final ControladorTrafico trafico;
    private float timerOnda = 0; // Segundos hasta la próxima onda mientras suena la bocina.

    public SistemaBocina(GestorParticulas particulas, ControladorTrafico trafico) {
        this.particulas = particulas;
        this.trafico = trafico;
    }

    /** Se llama cada cuadro; (x, z) es la posición del jugador y botsActivos la cantidad de bots que se procesan. */
    public void actualizar(float deltaTime, boolean sonando, float x, float z, int botsActivos) {
        if (!sonando) {
            timerOnda = 0; // Al soltar la tecla, la próxima pulsación lanza onda de inmediato.
            return;
        }
        trafico.asustarCercanos(x, z, botsActivos); // Mientras suena, los bots cercanos siguen asustados.
        timerOnda -= deltaTime;
        if (timerOnda <= 0) {
            particulas.generarOndaBocina(x, z);
            timerOnda = Constantes.INTERVALO_ONDA_BOCINA;
        }
    }
}
