package graficos; // Capa gráfica: aquí vive todo el OpenGL de bajo nivel.

import utilidades.Constantes; // Límite del minimapa que se inyecta en el shader.
import java.io.IOException; // Error al leer un shader del classpath.
import java.io.InputStream; // Lee el archivo del shader.
import java.nio.charset.StandardCharsets; // Los shaders están en UTF-8.
import java.util.Arrays; // Rellena la tabla de ubicaciones de los arreglos GLSL.
import java.util.HashMap; // Guarda las ubicaciones de las variables del shader.
import java.util.Map; // Define el tipo del diccionario de ubicaciones.
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * PASO 1 DE LA REFACTORIZACIÓN: primitivas geométricas y manejo de shaders.
 * Dibuja cajas (un cubo unitario escalado, girado y trasladado) y envía uniforms al programa GLSL.
 * No conoce el juego: solo recibe el código de los shaders y las órdenes de dibujo.
 */
public class Renderizador {

    private int programa; // Identificador del programa que une ambos shaders.
    private int vao; // Identificador del VAO: describe cómo leer los vértices.
    private int vbo; // Identificador del VBO: contiene los vértices en la GPU.
    private static final int MAX_ELEMENTOS_ARREGLO = 64; // Tamaño máximo de un arreglo GLSL (el mayor es uLuces[49]).
    private static final int SIN_BUSCAR = Integer.MIN_VALUE; // Marca las posiciones de arreglo cuya ubicación aún no se consultó.
    private final Map<String, Integer> uniforms = new HashMap<>(); // Evita buscar repetidamente el mismo uniform.
    private final Map<String, int[]> uniformsIndexados = new HashMap<>(); // Ubicación de cada elemento de un arreglo GLSL, por nombre base.

    /** Lee un shader de resources (por ejemplo "/shaders/ciudad.vert") y sustituye @LIMITE_HUD@ por el límite del minimapa. */
    public static String cargarShader(String ruta) {
        try (InputStream in = Renderizador.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró el shader " + ruta);
            }
            String codigo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return codigo.replace("@LIMITE_HUD@", Float.toString(Constantes.LIMITE_HUD)); // 107.0
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el shader " + ruta, e);
        }
    }

    /** Compila los shaders y sube el cubo compartido a la GPU; requiere un contexto OpenGL activo. */
    public void iniciar(String codigoVertices, String codigoFragmentos) {
        crearPrograma(codigoVertices, codigoFragmentos); // Compila y enlaza los shaders que transforman y colorean los vértices.
        crearCubo(); // Guarda en la GPU el cubo que servirá para todos los objetos.
    }

    /** Activa el programa y el cubo; se llama una vez al inicio de cada pasada de dibujo. */
    public void usar() {
        glUseProgram(programa); // Activa los shaders.
        glBindVertexArray(vao); // Selecciona los atributos del cubo compartido.
    }

    /** Libera únicamente los recursos que llegaron a crearse. */
    public void liberar() {
        if (programa != 0) { // Comprueba si existe un programa en la GPU.
            glDeleteProgram(programa); // Libera el programa de shaders.
        }
        if (vbo != 0) { // Comprueba si se reservó el buffer de vértices.
            glDeleteBuffers(vbo); // Libera los datos de geometría de la GPU.
        }
        if (vao != 0) { // Comprueba si se creó la descripción de atributos.
            glDeleteVertexArrays(vao); // Libera la configuración del cubo.
        }
    }

    // ==================== DIBUJAR CAJAS Y ENVIAR UNIFORMS ====================

    /** Dibuja una caja sin giro: posición XYZ, tamaño XYZ y color RGB entre 0 y 1. */
    public void caja(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b) {
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0); // Reutiliza el dibujo general con un ángulo de cero.
    }

    /** Dibuja el cubo unitario con escala, giro alrededor de Y, posición y color. */
    public void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo) {
        vector("uPos", x, y, z); // Envía la posición del centro de la caja en el mundo.
        vector("uEscala", sx, sy, sz); // Envía el ancho, alto y profundidad de la caja.
        vector("uColor", r, g, b); // Envía las intensidades roja, verde y azul del material.
        decimal("uGiro", angulo); // Envía la orientación en radianes.
        glDrawArrays(GL_TRIANGLES, 0, 36); // Dibuja 12 triángulos: dos por cada una de las seis caras.
    }

    /**
     * Dibuja una caja inclinada alrededor de su eje X (como un capó levantado): con una inclinación positiva en radianes
     * su frente (-Z) sube. La inclinación se aplica antes del giro alrededor de Y y se restablece a 0 al terminar.
     */
    public void cajaInclinada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo, float inclinacion) {
        decimal("uInclinacion", inclinacion);
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, angulo);
        decimal("uInclinacion", 0); // Las cajas siguientes se dibujan sin inclinar.
    }

    /** Transforma una pieza del espacio local de un vehículo (frente = -Z) al mundo y la dibuja sobre el asfalto. */
    public void piezaVehiculo(float x, float z, float angulo, float lx, float ly, float lz, float sx, float sy, float sz, float cr, float cg, float cb) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación del vehículo.
        float seno = (float) Math.sin(angulo); // Seno de la misma orientación.
        cajaGirada(x + coseno * lx + seno * lz, ly, z - seno * lx + coseno * lz, sx, sy, sz, cr, cg, cb, angulo); // ly es la altura absoluta sobre el asfalto.
    }

    /**
     * Sombra base (blob shadow): un rectángulo casi plano, casi negro, en el suelo bajo un vehículo en (x, z) orientado
     * según angulo. Gira con el vehículo gracias al giro de cajaGirada (OpenGL 3.3 core no tiene glPushMatrix ni glRotatef).
     * Va por encima de la pintura de la calle para no quedar tapada por las cebras ni parpadear (z-fighting) con el asfalto.
     */
    public void dibujarSombraBase(float x, float z, float angulo, float ancho, float largo) {
        cajaGirada(x, Constantes.ALTURA_SOMBRA, z, ancho, Constantes.GROSOR_SOMBRA, largo,
                Constantes.COLOR_SOMBRA, Constantes.COLOR_SOMBRA, Constantes.COLOR_SOMBRA, angulo);
    }

    /** Busca un uniform una vez y guarda su ubicación para los siguientes dibujos. */
    public int uniform(String nombre) {
        if (!uniforms.containsKey(nombre)) { // Revisa si esta variable todavía no fue localizada.
            int ubicacion = glGetUniformLocation(programa, nombre); // Pregunta a OpenGL por la dirección del uniform.
            uniforms.put(nombre, ubicacion); // Guarda la respuesta; -1 significa que el shader no usa esa variable.
        }
        return uniforms.get(nombre); // Recupera la ubicación guardada; OpenGL ignora envíos a -1.
    }

    /**
     * Ubicación del elemento indice de un arreglo GLSL (por ejemplo uLuces[3]). El nombre completo se arma una sola vez
     * por elemento; en los cuadros siguientes solo se consulta la tabla, sin crear ningún String.
     */
    public int uniform(String nombreBase, int indice) {
        int[] ubicaciones = uniformsIndexados.get(nombreBase);
        if (ubicaciones == null) { // Primera vez que se usa este arreglo.
            ubicaciones = new int[MAX_ELEMENTOS_ARREGLO];
            Arrays.fill(ubicaciones, SIN_BUSCAR);
            uniformsIndexados.put(nombreBase, ubicaciones);
        }
        if (ubicaciones[indice] == SIN_BUSCAR) { // Este elemento todavía no fue localizado.
            ubicaciones[indice] = glGetUniformLocation(programa, nombreBase + "[" + indice + "]");
        }
        return ubicaciones[indice];
    }

    /** Envía tres números reales al elemento indice de un arreglo de vec3 del shader. */
    public void vector(String nombreBase, int indice, float x, float y, float z) {
        glUniform3f(uniform(nombreBase, indice), x, y, z);
    }

    /** Envía tres números reales a una variable vec3 del shader. */
    public void vector(String nombre, float x, float y, float z) {
        glUniform3f(uniform(nombre), x, y, z); // Escribe las tres componentes en el programa activo.
    }

    /** Envía un número real a una variable float del shader. */
    public void decimal(String nombre, float valor) {
        glUniform1f(uniform(nombre), valor); // Actualiza el escalar identificado por su nombre.
    }

    /** Envía un número entero a una variable int del shader. */
    public void entero(String nombre, int valor) {
        glUniform1i(uniform(nombre), valor); // Actualiza un entero, usado también para interruptores 0/1.
    }

    // ==================== SHADERS ====================

    /** Compila un shader y comunica el mensaje del controlador si hay un error GLSL. */
    private int compilar(int tipo, String fuente) {
        int shader = glCreateShader(tipo); // Reserva un shader del tipo vértice o fragmento.
        glShaderSource(shader, fuente); // Entrega a OpenGL el texto GLSL.
        glCompileShader(shader); // Solicita la compilación para esta GPU.
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) { // Consulta si la compilación falló.
            String error = glGetShaderInfoLog(shader); // Recupera el detalle del error antes de borrar el objeto.
            glDeleteShader(shader); // Libera el shader que no pudo compilarse.
            throw new IllegalStateException(error); // Detiene la aplicación con el mensaje de diagnóstico.
        }
        return shader; // Devuelve el identificador del shader compilado.
    }

    /** Une los shaders en un programa ejecutable y libera los objetos intermedios. */
    private void crearPrograma(String codigoVertices, String codigoFragmentos) {
        int shaderVertices = compilar(GL_VERTEX_SHADER, codigoVertices); // Compila las transformaciones geométricas.
        int shaderFragmentos = 0; // Permite saber si el segundo shader llegó a crearse.
        try { // Garantiza que ambos objetos intermedios se liberen incluso ante un fallo.
            shaderFragmentos = compilar(GL_FRAGMENT_SHADER, codigoFragmentos); // Compila el color de la etapa actual.
            programa = glCreateProgram(); // Crea el contenedor que enlazará ambos shaders.
            glAttachShader(programa, shaderVertices); // Adjunta el shader que calcula posiciones.
            glAttachShader(programa, shaderFragmentos); // Adjunta el shader que calcula colores.
            glLinkProgram(programa); // Enlaza las entradas y salidas de ambos shaders.
            if (glGetProgrami(programa, GL_LINK_STATUS) == GL_FALSE) { // Comprueba que el enlace haya sido válido.
                throw new IllegalStateException(glGetProgramInfoLog(programa)); // Muestra incompatibilidades del programa.
            }
        } finally { // Ya no hacen falta objetos shader separados después del enlace.
            glDeleteShader(shaderVertices); // Libera el objeto del shader de vértices.
            if (shaderFragmentos != 0) { // Comprueba que el segundo objeto exista.
                glDeleteShader(shaderFragmentos); // Libera el objeto del shader de fragmentos.
            }
        }
    }

    // ==================== GEOMETRÍA DEL CUBO: VAO Y VBO ====================

    /** Crea un cubo explícito, con posición XYZ y normal XYZ por vértice, como en los ejemplos. */
    private void crearCubo() {
        float[] vertices = { // Cada línea describe un vértice: X, Y, Z, normalX, normalY, normalZ.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina inferior izquierda, normal hacia +Z.
            0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina inferior derecha.
            0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina superior derecha; termina el primer triángulo.
            0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: repite la esquina para el segundo triángulo.
            -0.5f, 0.5f, 0.5f, 0, 0, 1, // Cara frontal: esquina superior izquierda.
            -0.5f, -0.5f, 0.5f, 0, 0, 1, // Cara frontal: cierra el segundo triángulo.

            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: primer vértice, normal hacia -Z.
            -0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: segundo vértice.
            0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: termina el primer triángulo.
            0.5f, 0.5f, -0.5f, 0, 0, -1, // Cara trasera: inicia el segundo triángulo.
            0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: esquina inferior derecha.
            -0.5f, -0.5f, -0.5f, 0, 0, -1, // Cara trasera: cierra la cara.

            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda: primer vértice, normal hacia -X.
            -0.5f, 0.5f, -0.5f, -1, 0, 0, // Cara izquierda: segundo vértice.
            -0.5f, -0.5f, -0.5f, -1, 0, 0, // Cara izquierda: termina el primer triángulo.
            -0.5f, -0.5f, -0.5f, -1, 0, 0, // Cara izquierda: inicia el segundo triángulo.
            -0.5f, -0.5f, 0.5f, -1, 0, 0, // Cara izquierda: esquina inferior frontal.
            -0.5f, 0.5f, 0.5f, -1, 0, 0, // Cara izquierda: cierra la cara.

            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha: primer vértice, normal hacia +X.
            0.5f, -0.5f, 0.5f, 1, 0, 0, // Cara derecha: segundo vértice.
            0.5f, -0.5f, -0.5f, 1, 0, 0, // Cara derecha: termina el primer triángulo.
            0.5f, -0.5f, -0.5f, 1, 0, 0, // Cara derecha: inicia el segundo triángulo.
            0.5f, 0.5f, -0.5f, 1, 0, 0, // Cara derecha: esquina superior trasera.
            0.5f, 0.5f, 0.5f, 1, 0, 0, // Cara derecha: cierra la cara.

            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: primer vértice, normal hacia +Y.
            -0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: segundo vértice.
            0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: termina el primer triángulo.
            0.5f, 0.5f, 0.5f, 0, 1, 0, // Cara superior: inicia el segundo triángulo.
            0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: esquina trasera derecha.
            -0.5f, 0.5f, -0.5f, 0, 1, 0, // Cara superior: cierra la cara.

            -0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior: primer vértice, normal hacia -Y.
            0.5f, -0.5f, -0.5f, 0, -1, 0, // Cara inferior: segundo vértice.
            0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: termina el primer triángulo.
            0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: inicia el segundo triángulo.
            -0.5f, -0.5f, 0.5f, 0, -1, 0, // Cara inferior: esquina frontal izquierda.
            -0.5f, -0.5f, -0.5f, 0, -1, 0 // Cara inferior: cierra los 36 vértices del cubo.
        };
        vao = glGenVertexArrays(); // Reserva el objeto que recordará la disposición de los atributos.
        glBindVertexArray(vao); // Activa ese VAO para configurarlo.
        vbo = glGenBuffers(); // Reserva el buffer que almacenará los vértices.
        glBindBuffer(GL_ARRAY_BUFFER, vbo); // Selecciona el VBO como destino de datos de vértices.
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW); // Copia los datos a la GPU; el cubo no cambiará.
        int bytesPorVertice = 6 * Float.BYTES; // Cada vértice contiene seis float de cuatro bytes.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, bytesPorVertice, 0L); // Describe XYZ desde el primer byte del vértice.
        glEnableVertexAttribArray(0); // Habilita la posición que recibe aPos.
        glVertexAttribPointer(1, 3, GL_FLOAT, false, bytesPorVertice, 3L * Float.BYTES); // Describe la normal después de XYZ.
        glEnableVertexAttribArray(1); // Habilita la normal que recibe aNormal.
        glBindBuffer(GL_ARRAY_BUFFER, 0); // Deja de seleccionar el VBO una vez configurado.
        glBindVertexArray(0); // Finaliza la configuración del VAO.
    }
}
