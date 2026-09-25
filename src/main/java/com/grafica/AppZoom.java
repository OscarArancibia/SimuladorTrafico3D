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
public class AppZoom {
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

    //Diferencia de Zoom
    private int uZoomLocation;

    //Zoom Actual
    private float zoom = 1.0f;
    private static final float ZOOM_MIN = 0.25f;
    
    // CORRECCIÓN: Nombre cambiado de ZOOM_MAN a ZOOM_MAX y valor ampliado a 3.0f
    private static final float ZOOM_MAX = 3.0f; 
    private static final float VELOCIDAD_ZOOM = 1.5f;

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

        //Cierra la ventana al presionar escape 
        GLFW.glfwSetKeyCallback(window,(w,key,scancode,action,mods)->{
            if(key==GLFW.GLFW_KEY_ESCAPE && action==GLFW.GLFW_PRESS){
                GLFW.glfwSetWindowShouldClose(w,true);
            }
        });

        // CORRECCIÓN: Se agregó la variable xoffset que exige la firma del método
        GLFW.glfwSetScrollCallback(window,(w, xoffset, yoffset)->{
            zoom +=(float) yoffset*0.10f;
            zoom = clamp(zoom, ZOOM_MIN, ZOOM_MAX);
        });
        
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

    private float clamp(float value, float min, float max){
        return Math.max(min, Math.min(max, value));
    }

    //=========================================================00
    //Paso 2: Crear el shader y el triangulo
    //=========================================================00

    private void crearShader() {   
        //VertexSrc = codigo GLSL del vertex shader
        String vertexSrc= """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                uniform float uZoom;
                void main(){
                vec3 pos = vec3(aPos.xy*uZoom, aPos.z);
                gl_Position = vec4(pos, 1.0);}
                """;

        /**
         * fragmentSrc = codigo GLSL del framework shader
         *FragColor= color de salida del pixel. vector de 4 float r,g,b,a
         *Se ejecuta por cada pixel que se dibuja en la pantalla
        */
        String fragmentSrc= """
                #version 330 core
                out vec4 fragColor;
                void main(){
                fragColor = vec4(1.0f, 0.5f, 0.2f, 1.0f);
                }
                """;
                
        //Creamos el vertex shader en la GPU y obtenemos su referencia
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        //Asignamos el codigo del vertex shader a la GPU
        GL20.glShaderSource(vertexShader, vertexSrc);
        //Compilamos el vertex shader en la GPU
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

         //Creamos el fragment shader en la GPU y obtenemos su referencia
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        //Asignamos el codigo del fragment shader a la GPU
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        //Compilamos el fragment shader en la GPU
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

        uZoomLocation = GL20.glGetUniformLocation(programa, "uZoom");
        if (uZoomLocation==-1){
            throw new RuntimeException("Error al encontrar uniform uZoom");
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

    private void procesarInput(float deltaTime){
        float paso = VELOCIDAD_ZOOM *deltaTime;

        if(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_ADD)==GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_EQUAL)==GLFW.GLFW_PRESS){
                zoom+=paso;
        }

        if(GLFW.glfwGetKey(window, GLFW.GLFW_KEY_KP_SUBTRACT)==GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_MINUS)==GLFW.GLFW_PRESS){
                zoom-=paso;
        }
        
        // CORRECCIÓN: Aplicamos clamp también aquí para que el teclado respete los límites
        zoom = clamp(zoom, ZOOM_MIN, ZOOM_MAX);
    }


    /**
    * Bucle principal: en cada iteracion dibujamos un frame
    */
    private void loop() {

        float ultimoTiempo = (float)GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            //MOvimiento desacoplado de FPS usando deltaTime
            float tiempoActual = (float)GLFW.glfwGetTime();
            float deltaTiempo = tiempoActual - ultimoTiempo;
            ultimoTiempo = tiempoActual;
            
            procesarInput(deltaTiempo);

            //Color con el que vamos a limpiar la pantalla 
            GL11.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
            //Limpiamos el buffer de color (rellenamos con el color anterior)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            
            //Activar nuestro programa de shaders
            GL20.glUseProgram(programa);
            
            // CORRECCIÓN: Le enviamos a la GPU el valor del zoom calculado
            GL20.glUniform1f(uZoomLocation, zoom);
            
            //Activamos el VAO que contiene el triangulo que queremos dibujar
            GL30.glBindVertexArray(vao);
            //Orden del dibujo: modo, empezar en el vertice 0, dibujar 6 vertices (porque es un cuadrado)
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
        // CORRECCIÓN: Instanciamos AppZoom en lugar de App
        new AppZoom().run();
    }
}