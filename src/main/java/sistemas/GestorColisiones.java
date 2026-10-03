package sistemas; // Lógicas invisibles del juego: clima, IA, colisiones.

import utilidades.Constantes; // Radio del auto y dimensiones del bus.
import utilidades.MapaCiudad; // Plano de la ciudad.

/**
 * Matemáticas de intersección: círculo del auto contra manzanas y bordes del mapa, y contra el rectángulo orientado del bus.
 * Solo calcula; las consecuencias (perder vida, detener el auto) las decide el juego.
 */
public final class GestorColisiones {

    private GestorColisiones() { } // Clase de solo métodos estáticos: no se instancia.

    /** Resultado del contacto del jugador con el bus. */
    public enum Contacto { NINGUNO, FATAL, EMPUJADO }

    /** Qué ocurrió con el bus y, si fue empujado, dónde queda el jugador. */
    public static class ResultadoBus {
        public Contacto contacto = Contacto.NINGUNO;
        public float jugadorX; // Posición X del jugador tras el empuje.
        public float jugadorZ; // Posición Z del jugador tras el empuje.
    }

    /** Comprueba si el círculo del auto cabe dentro de los bordes del mapa (sin mirar las manzanas). */
    public static boolean dentroDelMapa(float x, float z) {
        float limitePermitido = MapaCiudad.LIMITE - Constantes.RADIO_AUTO; // Reserva espacio para que el auto completo quede dentro.
        return Math.abs(x) <= limitePermitido && Math.abs(z) <= limitePermitido; // Detecta salida por cualquier borde.
    }

    /** Comprueba si el círculo del auto cabe en una posición sin tocar manzanas ni bordes. */
    public static boolean puedeCircular(float x, float z) {
        if (!dentroDelMapa(x, z)) { // Detecta salida por cualquier borde.
            return false; // Rechaza la posición exterior.
        }

        for (int fila = 0; fila < MapaCiudad.MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MapaCiudad.MAPA[fila].length; columna++) { // Recorre las celdas de cada fila.
                if (MapaCiudad.MAPA[fila][columna] == 0) { // Una celda con cero representa calle.
                    continue; // Omite la calle porque no es un obstáculo.
                }

                float centroX = MapaCiudad.centro(columna); // Convierte la columna al centro X de la manzana.
                float centroZ = MapaCiudad.centro(fila); // Convierte la fila al centro Z de la manzana.
                float cercaX = Math.max(centroX - 5, Math.min(x, centroX + 5)); // Busca el X más cercano dentro de la acera.
                float cercaZ = Math.max(centroZ - 5, Math.min(z, centroZ + 5)); // Busca el Z más cercano dentro de la acera.
                float distanciaX = x - cercaX; // Calcula la separación horizontal del auto al rectángulo.
                float distanciaZ = z - cercaZ; // Calcula la separación en profundidad al rectángulo.
                float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Aplica Pitágoras sin raíz.

                if (distanciaCuadrada < Constantes.RADIO_AUTO * Constantes.RADIO_AUTO) { // Comprueba si el círculo invade la manzana.
                    return false; // Rechaza el movimiento que produciría una colisión.
                }
            }
        }

        return true; // Acepta la posición porque no se encontró ningún obstáculo.
    }

    /**
     * Contacto del jugador con el bus: rectángulo orientado (3 x 9) ensanchado por el radio del auto.
     * Sin gracia (puedeSerHerido) es fatal; con gracia el bus actúa como pared y expulsa al jugador por el lado más cercano.
     */
    public static ResultadoBus contactoConBus(float busX, float busZ, float busAngulo, float jugadorX, float jugadorZ, boolean puedeSerHerido) {
        ResultadoBus resultado = new ResultadoBus();
        resultado.jugadorX = jugadorX;
        resultado.jugadorZ = jugadorZ;
        float coseno = (float) Math.cos(busAngulo);
        float seno = (float) Math.sin(busAngulo);
        float dx = jugadorX - busX; // Posición del jugador respecto al centro del bus.
        float dz = jugadorZ - busZ;
        float lx = coseno * dx - seno * dz; // Coordenadas en el sistema local del bus (inverso de piezaVehiculo).
        float lz = seno * dx + coseno * dz;
        float limiteX = Constantes.SEMIANCHO_BUS + Constantes.RADIO_AUTO; // Semiancho del bus (con ruedas) más el radio del auto.
        float limiteZ = Constantes.SEMILARGO_BUS + Constantes.RADIO_AUTO; // Semilargo del bus más el radio del auto.
        if (Math.abs(lx) >= limiteX || Math.abs(lz) >= limiteZ) { // Sin contacto.
            return resultado;
        }
        if (puedeSerHerido) { // Atropello: cuesta una vida y reaparece.
            resultado.contacto = Contacto.FATAL;
            return resultado;
        }
        if (limiteX - Math.abs(lx) < limiteZ - Math.abs(lz)) { // En periodo de gracia: expulsa al jugador por el lado más cercano.
            lx = Math.signum(lx == 0 ? 1 : lx) * limiteX;
        } else {
            lz = Math.signum(lz == 0 ? -1 : lz) * limiteZ;
        }
        resultado.contacto = Contacto.EMPUJADO;
        resultado.jugadorX = busX + coseno * lx + seno * lz; // De vuelta a coordenadas del mundo.
        resultado.jugadorZ = busZ - seno * lx + coseno * lz;
        return resultado;
    }
}
