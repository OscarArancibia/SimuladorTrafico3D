package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el modelo.
import utilidades.Constantes; // Velocidad, duración y geometría de las calles.

/**
 * PASO 3 DE LA REFACTORIZACIÓN: patrulla que aparece al atropellar a un peatón y persigue al jugador por las calles.
 * La posición del jugador se recibe por parámetro. Las consecuencias de la embestida (perder una vida) las decide el juego.
 */
public class PatrullaPolicia extends Vehiculo {

    private boolean persiguiendo = false; // Cuando es true la patrulla existe y persigue al jugador.
    private float timer = 0f; // Segundos restantes de la persecución.
    private float destinoX = 0f; // Próximo punto al que se dirige la patrulla (siempre sobre una calle).
    private float destinoZ = 0f;
    private boolean tieneDestino = false; // false obliga a elegir un nuevo destino.

    public boolean isPersiguiendo() { return persiguiendo; }

    /** Llama a la patrulla: nace sobre una calle a 40 unidades del jugador, mirándolo. Si ya persigue, no hace nada. */
    public void llamar(float jugadorX, float jugadorZ) {
        if (persiguiendo) {
            return;
        }
        persiguiendo = true;
        timer = Constantes.DURACION_PERSECUCION;
        float candidatoX = Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, jugadorX + Constantes.DISTANCIA_APARICION_POLI)); // Punto a 40 unidades, dentro del mapa.
        float calleX = Math.round(candidatoX / Constantes.ESPACIADO_CALLES) * Constantes.ESPACIADO_CALLES; // Calle vertical más cercana (las calles están cada 30 unidades: -90 a 90).
        float calleZ = Math.round(Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, jugadorZ)) / Constantes.ESPACIADO_CALLES) * Constantes.ESPACIADO_CALLES; // Calle horizontal más cercana.
        if (Math.abs(candidatoX - calleX) <= Math.abs(jugadorZ - calleZ)) { // Está más cerca de una calle vertical:
            x = calleX; z = Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, jugadorZ)); // se centra en ella y conserva su Z.
        } else { // Está más cerca de una calle horizontal:
            x = candidatoX; z = calleZ; // se centra en ella y conserva su X.
        }
        tieneDestino = false; // Elige ruta desde cero.
        angulo = (float) Math.atan2(-(jugadorX - x), -(jugadorZ - z)); // Nace mirando al jugador.
    }

    /** Termina la persecución (tiempo agotado o embestida consumada). */
    public void detener() {
        persiguiendo = false;
    }

    /** Retira la patrulla y borra su ruta. */
    public void reiniciar() {
        persiguiendo = false;
        tieneDestino = false;
        timer = 0;
    }

    /** Descuenta el tiempo de persecución y, si sigue activa, avanza hacia el jugador. */
    public void actualizar(float deltaTime, float jugadorX, float jugadorZ) {
        timer -= deltaTime;
        if (timer <= 0) {
            persiguiendo = false;
            return;
        }
        avanzar(deltaTime, jugadorX, jugadorZ); // Persigue siguiendo las calles (no atraviesa edificios).
    }

    /** Indica si la patrulla está a menos de radioCuad (distancia al cuadrado) del jugador. */
    public boolean estaCercaDe(float jugadorX, float jugadorZ, float radioCuad) {
        float dx = jugadorX - x; // Vector hacia el jugador.
        float dz = jugadorZ - z;
        return dx * dx + dz * dz < radioCuad;
    }

    /** Mueve la patrulla por la cuadrícula de calles (cada 30 unidades, de -90 a 90) en dirección al jugador. */
    private void avanzar(float deltaTime, float jugadorX, float jugadorZ) {
        float calleX = Math.round(Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, x)) / Constantes.ESPACIADO_CALLES) * Constantes.ESPACIADO_CALLES; // Calle vertical más cercana.
        float calleZ = Math.round(Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, z)) / Constantes.ESPACIADO_CALLES) * Constantes.ESPACIADO_CALLES; // Calle horizontal más cercana.
        boolean enCalleH = Math.abs(z - calleZ) < Constantes.SEMIANCHO_CALLE; // Dentro de la franja de una calle horizontal (ancho útil de ±4,5).
        boolean enCalleV = Math.abs(x - calleX) < Constantes.SEMIANCHO_CALLE; // Dentro de la franja de una calle vertical.
        boolean alineado = (enCalleH && Math.abs(jugadorZ - calleZ) < Constantes.SEMIANCHO_CALLE) || (enCalleV && Math.abs(jugadorX - calleX) < Constantes.SEMIANCHO_CALLE); // Jugador en la misma calle.
        if (alineado) { // Misma calle: recta hasta el jugador (queda dentro de la franja, sin tocar manzanas).
            destinoX = jugadorX;
            destinoZ = jugadorZ;
            tieneDestino = true;
        } else if (!tieneDestino) { // Elige el siguiente cruce.
            float nodoX = calleX, nodoZ = calleZ; // Cruce más cercano.
            if ((x - nodoX) * (x - nodoX) + (z - nodoZ) * (z - nodoZ) > Constantes.RADIO_NODO_POLI_CUAD) { // Aún no está en un cruce: va primero a él.
                destinoX = nodoX;
                destinoZ = nodoZ;
            } else { // Está en un cruce: avanza un tramo por el eje más alejado del jugador.
                float difX = jugadorX - nodoX;
                float difZ = jugadorZ - nodoZ;
                float sigX = Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, nodoX + Constantes.ESPACIADO_CALLES * Math.signum(difX)));
                float sigZ = Math.max(-Constantes.LIMITE_CALLES, Math.min(Constantes.LIMITE_CALLES, nodoZ + Constantes.ESPACIADO_CALLES * Math.signum(difZ)));
                if (Math.abs(difX) >= Math.abs(difZ) && sigX != nodoX) { // Eje X.
                    destinoX = sigX; destinoZ = nodoZ;
                } else if (sigZ != nodoZ) { // Eje Z.
                    destinoX = nodoX; destinoZ = sigZ;
                } else { // Sin movimiento posible en Z: usa X.
                    destinoX = sigX; destinoZ = nodoZ;
                }
            }
            tieneDestino = true;
        }
        float difDestX = destinoX - x;
        float difDestZ = destinoZ - z;
        float distancia = (float) Math.sqrt(difDestX * difDestX + difDestZ * difDestZ);
        float paso = Constantes.VELOCIDAD_POLI * deltaTime;
        if (distancia <= paso) { // Llegó: se coloca en el punto y elige otro destino en el siguiente cuadro.
            x = destinoX;
            z = destinoZ;
            tieneDestino = false;
        } else {
            angulo = (float) Math.atan2(-difDestX, -difDestZ); // Frente = (-sen, -cos).
            x -= (float) Math.sin(angulo) * paso;
            z -= (float) Math.cos(angulo) * paso;
        }
    }

    // ==================== DIBUJO ====================

    /** Dibuja la patrulla perseguidora con sirenas que alternan 10 veces por segundo. */
    @Override
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        dibujarModelo(r, x, z, angulo, tiempoAnimacion * 2.5f); // dibujarModelo usa (int)(tiempo * 4) % 2.
    }

    /** Dibuja el modelo de patrulla: carrocería blanca con franja azul oscura y sirenas roja y azul que alternan. También lo usa el bot 0 del tráfico. */
    public static void dibujarModelo(Renderizador r, float x, float z, float angulo, float tiempo) {
        r.piezaVehiculo(x, z, angulo, 0, 0.65f, 0, 1.65f, 0.55f, 2.6f, 0.92f, 0.92f, 0.95f); // Carrocería blanca.
        r.piezaVehiculo(x, z, angulo, 0, 0.5f, 0, 1.7f, 0.2f, 2.65f, 0.08f, 0.12f, 0.35f); // Franja azul oscura a lo largo de la carrocería.
        r.piezaVehiculo(x, z, angulo, 0, 1.12f, 0.12f, 1.3f, 0.55f, 1.25f, 0.15f, 0.20f, 0.30f); // Cabina oscura.
        r.piezaVehiculo(x, z, angulo, -0.88f, 0.38f, 0, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f); // Ruedas del lado izquierdo.
        r.piezaVehiculo(x, z, angulo, 0.88f, 0.38f, 0, 0.24f, 0.58f, 1.9f, 0.06f, 0.07f, 0.08f); // Ruedas del lado derecho.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla.
        r.entero("uEmision", rojoEncendido ? 1 : 0); // La roja brilla en las fases pares.
        r.piezaVehiculo(x, z, angulo, -0.4f, 1.5f, 0.12f, 0.45f, 0.18f, 0.35f, 0.9f, 0.1f, 0.1f); // Sirena roja sobre el techo.
        r.entero("uEmision", rojoEncendido ? 0 : 1); // La azul brilla cuando la roja se apaga.
        r.piezaVehiculo(x, z, angulo, 0.4f, 1.5f, 0.12f, 0.45f, 0.18f, 0.35f, 0.1f, 0.25f, 0.95f); // Sirena azul sobre el techo.
        r.entero("uEmision", 0); // Devuelve la iluminación normal.
    }
}
