package sistemas; // Lógicas invisibles del juego: clima, IA, colisiones, daño.

import graficos.GestorParticulas; // Dispara la explosión.
import utilidades.Constantes; // Niveles de daño.

/**
 * Daño progresivo del auto del jugador: cada choque contra la estructura (edificios y bordes) lo daña un poco más.
 * Niveles: 1 rasguño, 2 humo blanco, 3 humo negro y capó levantado, 4 explosión (se pierde una vida y el daño vuelve a 0).
 * Solo guarda el nivel y lo explica con consultas; los gráficos las leen para decidir qué dibujar.
 */
public class SistemaDano {

    private int nivelDano = 0; // 0 = intacto; al llegar a NIVEL_DANO_EXPLOSION explota.
    private final GestorParticulas particulas; // Genera la explosión.
    private final Runnable alPerderVida; // Resta una vida global (la lleva el juego).

    public SistemaDano(GestorParticulas particulas, Runnable alPerderVida) {
        this.particulas = particulas;
        this.alPerderVida = alPerderVida;
    }

    /** Registra un choque en (x, y, z); al llegar al nivel de explosión, explota, resta una vida y vuelve a 0. */
    public void recibirChoque(float x, float y, float z) {
        nivelDano++;
        if (nivelDano == Constantes.NIVEL_DANO_EXPLOSION) {
            particulas.generarExplosion(x, y, z);
            alPerderVida.run();
            nivelDano = 0;
        }
    }

    public int getNivelDano() { return nivelDano; }

    /** Humo del motor: 0 = ninguno, 1 = blanco, 2 = negro. */
    public int getTipoHumo() {
        if (nivelDano >= Constantes.NIVEL_DANO_HUMO_NEGRO) return 2;
        if (nivelDano >= Constantes.NIVEL_DANO_HUMO_BLANCO) return 1;
        return 0;
    }

    /** true si el capó debe dibujarse levantado. */
    public boolean isCapoLevantado() {
        return nivelDano >= Constantes.NIVEL_DANO_CAPO;
    }

    /** El auto queda reparado (nueva partida o auto nuevo tras perder una vida). */
    public void reiniciar() {
        nivelDano = 0;
    }
}
