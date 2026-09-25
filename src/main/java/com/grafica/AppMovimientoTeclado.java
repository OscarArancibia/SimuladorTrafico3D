package com.grafica;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Este programa hace 3 cosas
 * 1. Abrir una ventana usando la biblioteca de GLFW
 * 2. Definir un triangulo y usas shaders
 * 3. En un bucle infinito dibujar el triangulo en la ventana
 * 
 * CONCEPTOS CLAVES:
 * -OpenGL: Conjunto de funciones para dibujar en 2D y 3D
 * -GLFW: Biblioteca para crear ventanas y manejar eventos de teclado y mouse no crea la ventana
 * -GPU: Unidad de procesamiento gráfico, es un procesador especializado en dibujar gráficos
 * -Shader: codigo que dice como trnasformar vertice y q color dar a cada pixel, se escribe en lenguaje gls
 * VAO/VBO: Formas de guardar en la GPU los datos de los vectores
 */
public class App{
    //referencia a nuestra ventana
    private long window;

    //Nuestro programa de shader ¿: es el vertex shader y el fragment shader
    /*La gpu usa este programa para dibujar cada frame */
    private int programa;

    //VAO = Vertex Array Object, es un objeto que guarda la referencia a los VBOs y a los atributos de los vertices
    private int vao;

    /*VBO= Vertex Buffer Object: es un bloque de memoria en la GPU donde guardamos
      las posiciones de los vértices */
    private int vbo;

    private static final int ANCHO = 800;
    private static final int ALTO = 600;

    /*
        Punto de entrada: inicializamos todo, ejecutamos el bucle del dibujo
        y al salir limpiamos la memoria
    */
    public void run(){
        init();
        loop();
        cleanup();
    }

    
    //1ro = Inicializamos GLFW y creamos la ventana
    //GLFM (Graphics Library Framework) es una biblioteca que nos permite c
    // rear ventanas y manejar eventos de teclado y mouse
    //-Crea y gestiona la ventana del sistema
    //-Gestiona eventos de teclado y mouse
    private void init() {
        if(!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        //Reestable todas las opciones por defecto
        GLFW.glfwDefaultWindowHints();

        //Ocultamos la ventana hasta que este lista
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        //Permitimos que la ventana se pueda redimensionar
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        //La version de openGL que vamos a usar es la 3.3
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        //EVITAR COMPATIVILIDAD CON VERSIONES ANTERIORES DE OPENGL, USAMOS EL PERFIL CORE
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        //CONFIGURACION PARA MACOS, SI NO SE PONE NO FUNCIONA
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        //creamos la ventana con el ancho y alto especificado, el titulo "Mi ventana" y 
        // sin monitor ni ventana compartida
        //este metodo devuelve un puntero a la ventana, que es un long
        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Mi ventana", 0, 0);

        //Decir que todas las ordenes de OpenGL se van a ejecutar en esta ventana
        GLFW.glfwMakeContextCurrent(window);

        //Sincronizamos el refresco de la ventana con la frecuencia de refresco del monitor
        //Evitamos el "tearing" imagen cortada en dos, porque la GPU dibuja a una velocidad diferente a la del monitor
        GLFW.glfwSwapInterval(1);

        //hacer visible la ventana
        GLFW.glfwShowWindow(window);

        //Carga en java las funciones de OpenGL
        //LWJGL necesita que este paso para enlazar las llamadas de Java con la GPU
        GL.createCapabilities();

        crearShader();
        crearTriangulo();
    }   

    //=========================================================00
    //Paso 2: Crear el shader y el triangulo
    //=========================================================00
    //¿Porque existen 2 shader?
    //El gpu dibuja en 2 etapas
    //1. Vertex shader: transforma los vertices de 3D a 2D y decide donde dibujar cada pixel
    //   y se ejcuta por cada vertice
    //2. Fragment shader: decide el color de cada pixel y se ejecuta por cada pixel
    // Para imlpementar se usa el lenguaje GLSL (OpenGL Shading Language) que es un lenguaje parecido a C
    // se usa para programar la GPU y decirle como dibujar cada pixel
    //El programa se pone en un string para que se compile en la gpu

    private void crearShader() {   
        //VertexSrc = codigo GLSL del vertex shader
        //Recibe la posicion de cada vertice(aPos) y le asigna a gl_Position(salida estandar)
        //"layout {location=0} in vec3 aPos;" = entrada en el canal 0.3 float x,y,z
        String vertexSrc= "#version 330 core\n"+
                          "layout (location = 0) in vec3 aPos;\n"+
                          "void main(){gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0);}\n";


        /**
         * fragmentSrc = codigo GLSL del framework shader
         *FragColor= color de salida del pixel. vector de 4 float r,g,b,a
         *Se ejecuta por cada pixel que se dibuja en la pantalla
        */
        String fragmentSrc= "#version 330 core\n" +
                            "out vec4 fragColor;\n" + // <-- AGREGAR ESTA LÍNEA EXACTAMENTE AQUÍ
                             "void main()\n" +
                            "{\n" +
                            "   fragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);\n" +
                             "}\n";
        //Creamos el vertex shader en la GPU y obtenemos su referencia
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        //Asignamos el codigo del vertex shader a la GPU
        GL20.glShaderSource(vertexShader, vertexSrc);
        //Compilamos el vertex shader en la GPU
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

         //Creamos el vertex shader en la GPU y obtenemos su referencia
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        //Asignamos el codigo del vertex shader a la GPU
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        //Compilamos el vertex shader en la GPU
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        
        //Creamos el programa de shaders en la GPU y obtenemos su referencia
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        if(GL20.glGetProgrami(programa,GL20.GL_LINK_STATUS)==GL11.GL_FALSE){
            throw new RuntimeException("Error al enlazar el programa de shaders: " + GL20.glGetProgramInfoLog(programa));
        }

        //borramos los shaders de la GPU, ya que ya estan enlazados en el programa y no los necesitamos
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        //Comprobamos si hubo errores en la compilacion del shader
        if (GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            //Si hubo errores, obtenemos el log de errores y lo imprimimos
             throw new RuntimeException(tipo +" Error al compilar el shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }
    //===========================================================
    //Paso 3 crear el triaungulo y guardarlo en la GPU
    //===========================================================
    //UN triangulo son 3 puntos 
    //En opengl las coordenadas van de -1 a 1 en x,y,z, el centro de la pantalla es (0,0,0) 

    private void crearTriangulo() {
        float[] vertices = {
    // Primer triángulo (mitad derecha del cuadrado)
                        0.5f,  0.5f, 0.0f,  // Arriba derecha
                        0.5f, -0.5f, 0.0f,  // Abajo derecha
                       -0.5f,  0.5f, 0.0f,  // Arriba izquierda

    // Segundo triángulo (mitad izquierda del cuadrado)
                        0.5f, -0.5f, 0.0f,  // Abajo derecha
                       -0.5f, -0.5f, 0.0f,  // Abajo izquierda
                       -0.5f,  0.5f, 0.0f   // Arriba izquierda
};

        //Objeto que guarda la configuracion de los vertices, que VBOs usar y como interpretar los datos
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        
        //Creamos un VBO en la GPU y obtenemos su referencia
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        //OPenGL necesita que los datos de los vertices esten en un FloatBuffer para enviarlos a la GPU
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        //Enviar los datos al vbo 
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3*Float.BYTES,0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
  
    }


    /**
    * Bucle principal: en cada iteracion dibujamos un frame
    * Mientras no se cierre la ventana:
    * 1. Limpiamos la pantalla
    * 2. Decimos usar este programa de shaders y este vao para dibujar
    * 3. Ordenamos a la GPU dibujar los 3 puntos
    * 4. Intercambiamos el buffer de dibujo con el buffer de visualizacion
    * 5. Procesamos los eventos de teclado y mouse
    */
    private void loop() {
        while (!GLFW.glfwWindowShouldClose(window)) {
            //Color con el que vamos a limpiar la pantalla 
            GL11.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
            //Limpiamos el buffer de color (rellenamos con el color anterior)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            //Activar nuestro programa de shaders
            GL20.glUseProgram(programa);
            //Activamosel VAO que contiene el triangulo que queremos dibujar
            GL30.glBindVertexArray(vao);
            //Orden del dibujo: modo, empezar en el vertice 0, dibujar 3 vertices
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
            //Doble buffer: intercambiamos el buffer de dibujo con el buffer de visualizacion
            GLFW.glfwSwapBuffers(window);
            //Procesamos los eventos de teclado y mouse
            GLFW.glfwPollEvents();
        }
        
    }

    private void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate(); 
    }





    public static void main( String[] args )
    {
        new App().run();
    }
}
