package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el modelo.

/**
 * Clase padre de todo lo que circula por las calles: auto del jugador, bots de tráfico, patrulla y bus.
 * Un vehículo tiene posición sobre el plano (X, Z), orientación en radianes (cero apunta hacia -Z) y sabe dibujarse.
 */
public abstract class Vehiculo {

    protected float x; // Posición horizontal.
    protected float z; // Posición en profundidad.
    protected float angulo = 0; // Orientación en radianes; cero apunta hacia -Z.

    public float getX() { return x; }

    public float getZ() { return z; }

    public float getAngulo() { return angulo; }

    /** Distancia al cuadrado entre el vehículo y un punto (sin calcular raíz). */
    public float distanciaCuadradaA(float px, float pz) {
        float dx = px - x;
        float dz = pz - z;
        return dx * dx + dz * dz;
    }

    /** Ancho de la sombra bajo el vehículo (un poco más que el modelo); un auto normal por defecto. */
    public float getAnchoSombra() { return 2.1f; }

    /** Largo de la sombra bajo el vehículo (un poco más que el modelo); un auto normal por defecto. */
    public float getLargoSombra() { return 2.8f; }

    /** Distancia del centro del vehículo a sus faros delanteros; un auto normal por defecto. */
    public float getDistanciaFaros() { return 1.36f; }

    /** Separación lateral de los faros respecto al centro; un auto normal por defecto. */
    public float getSeparacionFaros() { return 0.55f; }

    /** Dibuja el modelo del vehículo; tiempoAnimacion alimenta vibraciones y sirenas. */
    public abstract void dibujar(Renderizador r, float tiempoAnimacion);
}
