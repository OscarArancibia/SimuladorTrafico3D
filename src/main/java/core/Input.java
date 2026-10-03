package core; // Motor base.

import utilidades.Constantes; // Escala del Bullet Time.
import static org.lwjgl.glfw.GLFW.*; // Funciones y constantes de teclado.

/**
 * Manejo de eventos de teclado: avisa una sola vez por pulsación y permite consultar teclas mantenidas.
 */
public class Input {

    /** Quien recibe las pulsaciones puntuales (una por cada vez que se presiona una tecla) y el texto escrito. */
    public interface OyenteTecla {
        void alPresionar(int tecla);

        /** Letra escrita (ya con mayúsculas y teclado del usuario aplicados); no interfiere con las teclas de conducción. */
        default void alEscribirCaracter(char letra) { }
    }

    private final long ventana; // Ventana cuyo teclado se consulta.

    /** Entrada simulada, sin ventana ni callback: las subclases sobrescriben pulsada() (pruebas automáticas). */
    protected Input() {
        this.ventana = 0;
    }

    /** Registra el callback: responde una sola vez al presionar, no a las repeticiones de la tecla. */
    public Input(long ventana, OyenteTecla oyente) {
        this.ventana = ventana;
        glfwSetKeyCallback(ventana, (ventanaEvento, key, scancode, action, mods) -> { // Registra la función que recibe eventos de teclado.
            if (action == GLFW_PRESS) {
                oyente.alPresionar(key); // Entrega la tecla al juego.
            }
        });
        glfwSetCharCallback(ventana, (ventanaEvento, codepoint) -> { // Registra la función que recibe el texto escrito (para los códigos secretos).
            if (Character.isBmpCodePoint(codepoint)) { // Solo caracteres del plano básico: las letras de los códigos.
                oyente.alEscribirCaracter((char) codepoint);
            }
        });
    }

    /** Bullet Time: con Espacio o Shift izquierdo pulsado el tiempo corre al 30 % (ESCALA_BULLET_TIME); si no, a velocidad normal. */
    public float escalaTiempo() {
        return (pulsada(GLFW_KEY_SPACE) || pulsada(GLFW_KEY_LEFT_SHIFT)) ? Constantes.ESCALA_BULLET_TIME : 1.0f;
    }

    /** Bocina: true mientras se mantiene pulsada la tecla H (vuelve a false al soltarla). */
    public boolean bocinaPulsada() {
        return pulsada(GLFW_KEY_H);
    }

    /** Consulta si una tecla sigue presionada durante el cuadro actual. */
    public boolean pulsada(int tecla) {
        return glfwGetKey(ventana, tecla) == GLFW_PRESS; // Devuelve true mientras el usuario mantiene la tecla.
    }
}
