package core; // Motor base.

import graficos.Renderizador; // Envía la posición de la cámara al shader.
import utilidades.Constantes; // Campo visual normal y alto.

/**
 * Control de vistas: tercera persona detrás del auto, helicóptero, primera persona sobre el capó y vista frontal.
 * También genera la sacudida de cámara que acompaña a los choques.
 */
public class CamaraManager {

    private int estadoCamara = 0; // 0: tercera persona, 1: primera persona (capó).
    private boolean helicoptero = false; // true: cámara sobre el auto, mirando hacia abajo.
    private boolean camaraFrontal = false; // true: cámara delante del auto, mirándolo de frente.
    private float timerTemblor = 0.0f; // Segundos restantes de la sacudida de cámara (solo efecto visual).
    private float fovActual = Constantes.FOV_NORMAL; // Campo visual vertical actual, en grados.
    private float intensidadViento = 0; // 0 = sin viento, 1 = tormenta completa (la dicta el clima).
    private float faseViento = 0; // Reloj de las oscilaciones del viento, en segundos.

    /** Tecla C: recorre tercera persona → helicóptero → capó → vista frontal → tercera persona (módulo 4). */
    public void ciclarModo() {
        int actual = camaraFrontal ? 3 : (estadoCamara == 1 ? 2 : (helicoptero ? 1 : 0)); // Modo en que estamos ahora.
        int siguiente = (actual + 1) % 4; // Avanza al siguiente y vuelve al primero.
        helicoptero = siguiente == 1;
        estadoCamara = siguiente == 2 ? 1 : 0;
        camaraFrontal = siguiente == 3;
    }

    /** Tecla E: alterna primera y tercera persona y sale de la vista frontal si estaba activa. */
    public void alternarPrimeraPersona() {
        estadoCamara = (estadoCamara + 1) % 2; // Alterna 0 ↔ 1.
        helicoptero = false; // Sale del helicóptero si estaba activo.
        camaraFrontal = false; // Sale de la vista frontal si estaba activa.
    }

    public boolean isFrontal() { return camaraFrontal; }

    public boolean isHelicoptero() { return helicoptero; }

    public int getEstado() { return estadoCamara; }

    /** Inicia una sacudida de cámara de la duración indicada. */
    public void temblar(float segundos) {
        timerTemblor = segundos;
    }

    /** Campo visual vertical actual, en grados. */
    public float getFov() { return fovActual; }

    /**
     * Acerca suavemente el campo visual a su objetivo: se abre a FOV_ALTO a gran velocidad o con nitro y vuelve a
     * FOV_NORMAL al frenar (interpolación lineal: fovActual += (objetivo - fovActual) * 2 * deltaTime).
     */
    public void actualizarFov(float deltaTime, boolean velocidadAlta) {
        float objetivo = velocidadAlta ? Constantes.FOV_ALTO : Constantes.FOV_NORMAL;
        fovActual += (objetivo - fovActual) * Math.min(1.0f, Constantes.VELOCIDAD_CAMBIO_FOV * deltaTime);
    }

    /** Actualiza el viento que agita la cámara; debe llamarse cada cuadro con el tiempo real y la intensidad del clima. */
    public void actualizarViento(float deltaTime, float intensidad) {
        intensidadViento = intensidad;
        faseViento = (faseViento + deltaTime) % 1000.0f; // El módulo evita perder precisión tras horas de juego.
    }

    /**
     * Desplazamiento (x, y, z) que el viento le da a la cámara: un temblor suave y continuo, hecho con senos de distintas
     * frecuencias, que sube y baja en ráfagas lentas. Es {0, 0, 0} cuando no hay viento.
     */
    public float[] oscilacionViento() {
        float rafaga = 0.6f + 0.4f * (float) Math.sin(faseViento * 0.9f); // Ráfagas de unos 7 s.
        float a = Constantes.AMPLITUD_VIENTO_CAMARA * intensidadViento * rafaga;
        return new float[] {
            a * (0.6f * (float) Math.sin(faseViento * 7.3f) + 0.4f * (float) Math.sin(faseViento * 13.1f)),
            a * 0.5f * (float) Math.sin(faseViento * 9.7f + 1.0f),
            a * 0.8f * (float) Math.sin(faseViento * 5.1f + 2.0f)
        };
    }

    /** Descuenta la sacudida; debe llamarse en cada cuadro, incluso en pausa o Game Over, para que termine. */
    public void actualizarTemblor(float deltaTime) {
        if (timerTemblor > 0) timerTemblor -= deltaTime;
    }

    /** Coloca la cámara según el modo activo y la posición del auto (autoX, autoZ) orientado según angulo. */
    public void configurar(Renderizador r, float autoX, float autoZ, float angulo, float aspecto) {
        r.decimal("uFov", fovActual);

        float offsetX = 0.0f, offsetY = 0.0f, offsetZ = 0.0f; // Desplazamiento aleatorio del temblor (común a todas las vistas).
        if (timerTemblor > 0) {
            float intensidad = timerTemblor * 3.0f; // Se reduce gradualmente hasta cero.
            offsetX = (float) (Math.random() - 0.5) * intensidad;
            offsetY = (float) (Math.random() - 0.5) * intensidad;
            offsetZ = (float) (Math.random() - 0.5) * intensidad;
        }
        float[] viento = oscilacionViento(); // El surazo agita la cámara además de los choques.
        offsetX += viento[0];
        offsetY += viento[1];
        offsetZ += viento[2];
        if (helicoptero) { // Helicóptero: 30 unidades sobre el auto; el desvío de 0,1 en Z evita que "mirar hacia abajo" coincida con el eje vertical de la cámara.
            r.vector("uOjo", autoX + offsetX, Constantes.ALTURA_HELICOPTERO + offsetY, autoZ + 0.1f + offsetZ);
            r.vector("uObjetivo", autoX + offsetX * 2, 0, autoZ + offsetZ * 2);
            r.decimal("uAspecto", aspecto);
            return;
        }
        if (estadoCamara == 1) { // Primera persona: cámara en el capó, delante de la cabina para no ver su interior.
            float frenteX = -(float) Math.sin(angulo); // Frente del auto (-sen, -cos).
            float frenteZ = -(float) Math.cos(angulo);
            float posX = autoX + frenteX * 0.9f; // 0,9 unidades hacia delante del centro.
            float posZ = autoZ + frenteZ * 0.9f;
            r.vector("uOjo", posX + offsetX, 1.4f + offsetY, posZ + offsetZ); // Altura de los ojos, sobre el capó.
            r.vector("uObjetivo", posX + frenteX * 10.0f + offsetX * 2, 1.2f + offsetY * 2, posZ + frenteZ * 10.0f + offsetZ * 2); // Mira 10 unidades al frente, un poco hacia la calle.
            r.decimal("uAspecto", aspecto);
            return;
        }
        if (camaraFrontal) { // Frontal: 12 unidades por delante del auto, un poco elevada, mirando hacia atrás a la carrocería.
            float frenteX = -(float) Math.sin(angulo); // Frente del auto (-sen, -cos).
            float frenteZ = -(float) Math.cos(angulo);
            r.vector("uOjo", autoX + frenteX * 12.0f + offsetX, 3.0f + offsetY, autoZ + frenteZ * 12.0f + offsetZ);
            r.vector("uObjetivo", autoX + offsetX * 2, 1.0f + offsetY * 2, autoZ + offsetZ * 2);
            r.decimal("uAspecto", aspecto);
            return;
        }
        float camaraX = autoX + (float) Math.sin(angulo) * 12; // Coloca la cámara 12 unidades detrás en X.
        float camaraZ = autoZ + (float) Math.cos(angulo) * 12; // Coloca la cámara 12 unidades detrás en Z.
        r.vector("uOjo", camaraX + offsetX, 9 + offsetY, camaraZ + offsetZ); // Envía la posición de la cámara, a 9 unidades de altura.
        r.vector("uObjetivo", autoX + offsetX * 2, 0.8f + offsetY * 2, autoZ + offsetZ * 2); // Orienta la cámara hacia la carrocería (el objetivo tiembla el doble para que se note el giro).
        r.decimal("uAspecto", aspecto); // Mantiene las proporciones al redimensionar la ventana.
    }
}
