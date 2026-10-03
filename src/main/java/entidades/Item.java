package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el coleccionable.
import utilidades.Constantes; // Radios de recogida.

/**
 * PASO 3 DE LA REFACTORIZACIÓN: coleccionable del mapa (moneda o píldora de poder).
 * Conoce su posición, su radio de recogida y cómo dibujarse; el efecto de recogerlo lo decide el juego.
 */
public class Item {

    /** Clases de coleccionable. */
    public enum Tipo {
        MONEDA, // Suma una moneda; cubo amarillo sobre una marca pintada en la calle.
        PILDORA_INMUNIDAD, // Morada: inmunidad y traspasar vehículos.
        PILDORA_VELOCIDAD, // Naranja: nitro de 120 km/h.
        PILDORA_REPARACION // Verde: repara el auto y lo deja como nuevo.
    }

    private final Tipo tipo;
    private final float radioRecogidaCuad; // Distancia de recogida al cuadrado (se compara sin calcular raíz).
    private float x; // Posición X sobre la calle.
    private float z; // Posición Z sobre la calle.

    public Item(Tipo tipo, float x, float z) {
        this.tipo = tipo;
        this.x = x;
        this.z = z;
        this.radioRecogidaCuad = (tipo == Tipo.MONEDA) ? Constantes.RADIO_RECOGIDA_MONEDA_CUAD : Constantes.RADIO_RECOGIDA_PILDORA_CUAD;
    }

    public Tipo getTipo() { return tipo; }

    public float getX() { return x; }

    public float getZ() { return z; }

    /** Mueve el coleccionable a otro punto de la ciudad. */
    public void colocarEn(float nuevoX, float nuevoZ) {
        x = nuevoX;
        z = nuevoZ;
    }

    /** Indica si un vehículo en (px, pz) está lo bastante cerca para recogerlo. */
    public boolean estaAlAlcance(float px, float pz) {
        float dx = px - x; // Separación horizontal.
        float dz = pz - z; // Separación en profundidad.
        return dx * dx + dz * dz < radioRecogidaCuad; // Distancia al cuadrado, sin calcular raíz.
    }

    /** Dibuja el coleccionable; en el minimapa las píldoras se agrandan para verse con claridad. */
    public void dibujar(Renderizador r, float tiempoAnimacion, boolean vistaMapa) {
        if (tipo == Tipo.MONEDA) {
            r.caja(x, 0.05f, z, 6.0f, 0.1f, 6.0f, 1.0f, 0.8f, 0.0f); // Marca cuadrada amarilla pintada sobre la calle.
            r.cajaGirada(x, 2.5f + (float) Math.sin(tiempoAnimacion * 4.0f) * 0.4f, z, 2.0f, 2.0f, 2.0f, 1.0f, 0.8f, 0.0f, tiempoAnimacion * 2); // Cubo amarillo sólido que gira y sube y baja.
            return;
        }
        float lado = vistaMapa ? 7.0f : 2.2f; // Más grandes: en el minimapa deben verse con claridad.
        float flote = 1.6f + (float) Math.sin(tiempoAnimacion * 4.0f) * 0.4f; // Flotación suave.
        if (tipo == Tipo.PILDORA_INMUNIDAD) {
            r.cajaGirada(x, flote, z, lado, lado, lado, 0.65f, 0.15f, 0.90f, tiempoAnimacion * 2); // Morada (inmunidad).
        } else if (tipo == Tipo.PILDORA_VELOCIDAD) {
            r.cajaGirada(x, flote, z, lado, lado, lado, 1.0f, 0.5f, 0.0f, tiempoAnimacion * 2); // Naranja (velocidad).
        } else {
            r.cajaGirada(x, flote, z, lado, lado, lado, 0.10f, 0.90f, 0.20f, tiempoAnimacion * 2); // Verde (reparación).
        }
    }
}
