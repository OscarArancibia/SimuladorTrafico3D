package core; // Motor base.

import org.lwjgl.glfw.Callbacks; // Libera los callbacks de la ventana al cerrar.
import org.lwjgl.glfw.GLFWErrorCallback; // Muestra los errores de GLFW en la consola.
import org.lwjgl.opengl.GL; // Carga las funciones OpenGL disponibles en el equipo.
import static org.lwjgl.glfw.GLFW.*; // Funciones y constantes de ventanas.
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Inicialización de GLFW y OpenGL: crea la ventana, el contexto 3.3 core y mantiene el tamaño del framebuffer.
 * No conoce el juego: solo ofrece la ventana y las operaciones del ciclo (eventos, buffers, tiempo, título).
 */
public class Ventana {

    private long handle; // Identificador que GLFW asigna a la ventana.
    private int ancho = 1100; // Ancho inicial de la ventana; después contiene píxeles del framebuffer.
    private int alto = 760; // Alto inicial de la ventana; después contiene píxeles del framebuffer.
    private final int[] anchoReal = new int[1]; // Espacio para que GLFW escriba el ancho en píxeles.
    private final int[] altoReal = new int[1]; // Espacio para que GLFW escriba el alto en píxeles.
    private GLFWErrorCallback errores; // Manejador de errores de GLFW.

    /** Configura GLFW y OpenGL: contexto 3.3 core, sincronización vertical y prueba de profundidad. */
    public void iniciar(String titulo) {
        errores = GLFWErrorCallback.createPrint(System.err); // Prepara mensajes de error en consola.
        errores.set(); // Instala el manejador de errores de GLFW.
        if (!glfwInit()) { // Intenta iniciar la biblioteca que administra ventanas y entrada.
            throw new IllegalStateException("No se pudo iniciar GLFW"); // Detiene el arranque si GLFW falla.
        }
        glfwDefaultWindowHints(); // Restablece las opciones de ventana antes de personalizarlas.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3); // Solicita la versión mayor de OpenGL.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3); // Solicita OpenGL 3.3.
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); // Usa el perfil moderno basado en shaders.
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // Solicita compatibilidad con contextos modernos de macOS.
        handle = glfwCreateWindow(ancho, alto, titulo, 0, 0); // Crea una ventana normal.
        if (handle == 0) { // GLFW devuelve cero cuando no consigue crear la ventana.
            throw new IllegalStateException("No se pudo crear la ventana OpenGL"); // Informa el fallo de creación.
        }
        glfwMakeContextCurrent(handle); // Selecciona la ventana que recibirá las llamadas OpenGL.
        glfwSwapInterval(1); // Sincroniza la presentación con la pantalla para reducir cortes de imagen.
        GL.createCapabilities(); // Carga las funciones OpenGL del contexto actual.
        glEnable(GL_DEPTH_TEST); // Hace que las superficies cercanas oculten las lejanas.
    }

    public long getHandle() { return handle; }

    public int getAncho() { return ancho; }

    public int getAlto() { return alto; }

    /** Relación ancho/alto de la imagen, para no deformar la proyección. */
    public float getAspecto() { return (float) ancho / alto; }

    /** Segundos desde que arrancó GLFW. */
    public double tiempo() { return glfwGetTime(); }

    public boolean debeCerrar() { return glfwWindowShouldClose(handle); }

    /** Solicita terminar el ciclo principal. */
    public void pedirCierre() { glfwSetWindowShouldClose(handle, true); }

    /** Procesa teclado, redimensionamiento y botón de cierre. */
    public void procesarEventos() { glfwPollEvents(); }

    /** Muestra la imagen terminada intercambiando los buffers. */
    public void intercambiarBuffers() { glfwSwapBuffers(handle); }

    /** Publica el texto en la barra superior de la ventana. */
    public void setTitulo(String titulo) { glfwSetWindowTitle(handle, titulo); }

    /** Obtiene los píxeles reales del framebuffer (también en pantallas Retina) y los guarda como ancho y alto. */
    public void actualizarTamano() {
        glfwGetFramebufferSize(handle, anchoReal, altoReal);
        ancho = anchoReal[0]; // Copia el ancho actual al estado de la ventana.
        alto = altoReal[0]; // Copia el alto actual al estado de la ventana.
    }

    /** Libera los callbacks, destruye la ventana y finaliza GLFW; seguro aunque el arranque haya fallado a medias. */
    public void destruir() {
        if (handle != 0) { // Evita destruir una ventana que no pudo crearse.
            Callbacks.glfwFreeCallbacks(handle); // Libera el callback de teclado registrado.
            glfwDestroyWindow(handle); // Destruye la ventana y su contexto OpenGL.
            handle = 0;
        }
        glfwTerminate(); // Finaliza GLFW y sus recursos globales.
        glfwSetErrorCallback(null); // Desconecta el manejador antes de liberar su memoria.
        if (errores != null) {
            errores.free(); // Libera el callback de errores.
            errores = null;
        }
    }
}
