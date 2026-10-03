package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el modelo.
import utilidades.Constantes; // Velocidad y carril del anillo.

/**
 * Bus que da la vuelta al anillo perimetral de asfalto por su carril derecho, en sentido horario.
 */
public final class BusUrbano extends Vehiculo {

    private int estado; // 0 = hacia +X, 1 = hacia +Z, 2 = hacia -X, 3 = hacia -Z.

    public BusUrbano() {
        reiniciar();
    }

    /** Devuelve el bus a la esquina superior izquierda del anillo, ya en su carril y mirando hacia +X. */
    public void reiniciar() {
        x = -Constantes.CARRIL_BUS; // Posición X del bus (esquina superior izquierda del anillo, ya en su carril).
        z = -Constantes.CARRIL_BUS; // Posición Z del bus.
        angulo = -(float) Math.PI / 2; // Orientación en radianes: mirando hacia +X.
        estado = 0;
    }

    @Override
    public float getAnchoSombra() { return 3.1f; }

    @Override
    public float getLargoSombra() { return 9.2f; }

    /** Distancia del centro del bus a sus faros delanteros. */
    public float getDistanciaFaros() { return 4.6f; }

    /** Separación lateral de los faros del bus. */
    public float getSeparacionFaros() { return 0.9f; }

    /** Máquina de estados del bus: recorre el anillo en sentido horario a 15 unidades por segundo. */
    public void actualizar(float deltaTime) {
        float paso = Constantes.VELOCIDAD_BUS * deltaTime; // Distancia que avanza en este cuadro.
        if (estado == 0) { // Tramo superior: hacia +X.
            x += paso;
            if (x >= Constantes.CARRIL_BUS) { x = Constantes.CARRIL_BUS; angulo = (float) Math.PI; estado = 1; } // Gira hacia +Z.
        } else if (estado == 1) { // Tramo derecho: hacia +Z.
            z += paso;
            if (z >= Constantes.CARRIL_BUS) { z = Constantes.CARRIL_BUS; angulo = (float) Math.PI / 2; estado = 2; } // Gira hacia -X.
        } else if (estado == 2) { // Tramo inferior: hacia -X.
            x -= paso;
            if (x <= -Constantes.CARRIL_BUS) { x = -Constantes.CARRIL_BUS; angulo = 0; estado = 3; } // Gira hacia -Z.
        } else { // Tramo izquierdo: hacia -Z.
            z -= paso;
            if (z <= -Constantes.CARRIL_BUS) { z = -Constantes.CARRIL_BUS; angulo = -(float) Math.PI / 2; estado = 0; } // Gira hacia +X.
        }
    }

    /** Dibuja el bus: carrocería celeste larga, techo blanco, ventanas laterales, parabrisas, faros y ruedas. */
    @Override
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        float tiempo = tiempoAnimacion;
        float vibracion = (float) Math.sin(tiempo * 20) * 0.015f; // Pequeña vibración del motor.
        r.piezaVehiculo(x, z, angulo, 0, 1.7f + vibracion, 0, 2.6f, 2.2f, 9.0f, 0.20f, 0.65f, 0.85f); // Carrocería celeste (mucho más larga y alta que un auto).
        r.piezaVehiculo(x, z, angulo, 0, 2.85f + vibracion, 0, 2.4f, 0.1f, 8.8f, 0.95f, 0.95f, 0.97f); // Techo blanco.
        for (int ventana = 0; ventana < 5; ventana++) { // Cinco ventanas por lado.
            float lz = -3.0f + ventana * 1.5f; // Posición a lo largo del bus.
            r.piezaVehiculo(x, z, angulo, -1.31f, 2.05f + vibracion, lz, 0.06f, 0.8f, 1.1f, 0.12f, 0.20f, 0.30f); // Ventana izquierda.
            r.piezaVehiculo(x, z, angulo, 1.31f, 2.05f + vibracion, lz, 0.06f, 0.8f, 1.1f, 0.12f, 0.20f, 0.30f); // Ventana derecha.
        }
        r.piezaVehiculo(x, z, angulo, 0, 2.05f + vibracion, -4.52f, 2.2f, 0.9f, 0.06f, 0.12f, 0.20f, 0.30f); // Parabrisas frontal.
        r.piezaVehiculo(x, z, angulo, -0.9f, 0.95f, -4.52f, 0.4f, 0.25f, 0.06f, 1.0f, 0.95f, 0.65f); // Faro izquierdo.
        r.piezaVehiculo(x, z, angulo, 0.9f, 0.95f, -4.52f, 0.4f, 0.25f, 0.06f, 1.0f, 0.95f, 0.65f); // Faro derecho.
        for (int eje = 0; eje < 2; eje++) { // Dos ejes de ruedas.
            float lz = (eje == 0) ? -2.8f : 2.8f; // Eje delantero y trasero.
            r.piezaVehiculo(x, z, angulo, -1.35f, 0.45f, lz, 0.3f, 0.9f, 1.0f, 0.06f, 0.07f, 0.08f); // Rueda izquierda.
            r.piezaVehiculo(x, z, angulo, 1.35f, 0.45f, lz, 0.3f, 0.9f, 1.0f, 0.06f, 0.07f, 0.08f); // Rueda derecha.
        }
    }
}
