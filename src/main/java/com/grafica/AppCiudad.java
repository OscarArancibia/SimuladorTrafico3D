package com.grafica;

import java.util.Map;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL33.*;

public class AppCiudad {
    private  long ventana;
    private  int programa;
    private  int vao;
    private  int vbo;
    private  int ancho = 1100;
    private  int alto = 760;

    //Angulo inicial de la camara de la ciudad
    private  float orbita;

    //Cambiar el angulo a la vista del mapa
    private boolean vistaMapa=false;

    //Dustancia del origen a cada borde
    private static final float LIMITE=35;

    //Ancho y profundidad de cada celda del mapa
    private static final float CELDA=10;

    private final Map<String, Integer> uniforms = new HasMap<>();
    private static final int [][] MAPA ={
        {0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0},
        {0, 2, 0, 1, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0},
        {0, 1, 0, 2, 0, 1, 0},
        {0, 0, 0, 0, 0, 0, 0}
    };

    //Organiza las tres fasesde la aplicacion y limpiar en caso ocurra un errore
    public void run(){
        GLFWErrorCallback errores = GLFWErrorCallback.createPrint(System.err);
        //Instamos el manejador de errores en GLFW
        errores.set();
        try{

        }finally{
            limpiar();
            glfwSetErrorCallback(null);
            errores.free();
        }
    }

    //configurar GLFW y OpenGL 
    public void iniciar(){
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
        ventana = GLFW.glfwCreateWindow(ancho, alto, "Ciudad | Flrchas : orbital | Esc : salir", 0, 0);

        //Decir que todas las ordenes de OpenGL se van a ejecutar en esta ventana
        GLFW.glfwMakeContextCurrent(ventana);

        //Sincronizamos el refresco de la ventana con la frecuencia de refresco del monitor
        //Evitamos el "tearing" imagen cortada en dos, porque la GPU dibuja a una velocidad diferente a la del monitor
        GLFW.glfwSwapInterval(1);

        //hacer visible la ventana
        GLFW.glfwShowWindow(ventana);

        //Carga en java las funciones de OpenGL
        //LWJGL necesita que este paso para enlazar las llamadas de Java con la GPU
        GL.createCapabilities();

        //Hace que las superficie cercanas oculten a las lejanas
        glEnable(GL_DEPTH_TEST);

        //registramos la funcion del evento del teclado para cerrar 
        glfwSetKeyCallback(ventana,(ventanaEvento, key, scancode, action, mods)->{
            if (action==GLFW_PRESS){
                tecla(key);
            }
        });

        crearPrograma();
        crearCubo();
    }

    private void loop(){
        //guardamos el instante inicial para calcular el tiempo entre cuadros
        double tiempoAnterior = glfwGetTime();
        //Cuenta los cuadros dibujados para la comprobacion
        int cuadros =0;
        //Cero permite jugar otro valor limita el arranque de prueba
        int maxCuadros = Integer.getInteger("demo.frames",0);
        //Reserva espacio para que GLFW escriba el ancho en pixels
        int[] anchoReal = new int [1];
        //Reserva espacio para que GLFW escriba  el alto en pixeles
        int[] altoReal = new int [1];

        while (!glfwWindowShouldClose(ventana)) {
            //Procesa el teclado, redimensionamiento y boton de cierre
            glfwPollEvents();
            //Consulta el tiempo actual en segundos
            double tiempoActual= glfwGetTime();
            float deltaTime = (float) Math.min(tiempoActual-tiempoAnterior, 0.05);
            tiempoAnterior = tiempoActual;
            actualizar(deltaTime);
            ancho = anchoReal[0];
            alto = altoReal[0];
            if (ancho > 0&& alto>0) {
                dibujarFrame();
            }

            glfwSwapBuffers(ventana);
            cuadros++;
            if (maxCuadros>0 && cuadros >= maxCuadros){
                glfwSetWindowShouldClose(ventana, true);
            }
        }
    }

    private void limpiar(){
        if (programa != 0){
            glDeleteProgram(programa);
        }
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (vao!= 0 ){
            glDeleteVertexArrays(vao);
        }
        if (ventana != 0) {
            Callbacks.glfwFreeCallbacks(ventana);
            glfwDestroyWindow(ventana);
        }
        glfwTerminate();
    }

    private void tecla(int key){
        if (key==GLFW_KEY_ESCAPE){
            glfwSetWindowShouldClose(ventana,true);
        }
    }

    private boolean pulsada(int key){
        return glfwGetKey(ventana, key)==GLFW_PRESS;
    }

    private void actualiza(float deltaTime){
        if (pulsada(GLFW_KEY_LEFT)){
            orbita -= deltaTime;
        }

        if (pulsada(GLFW_KEY_RIGHT)){
            orbita += deltaTime;
        }
    }

    private void configurarCamara(){
        float camaraX= (float)Math.sin(orbita)*65;
        float camaraZ= (float)Math.cos(orbita)*65;
        vector("uOjo", camaraX, 55, camaraZ);
        vector("uObjetivo", 0,0,0);
        decimal("uAspecto",(float) ancho/alto);
    }

    private void prepararLuces(){
        //enviar luces al gpu 
    }
    //=======================================
    //Creacion de la ciudad
    //=======================================

    private float centro(int indice){
        return -LIMITE+CELDA * (indice + 0.5f);

    }

    private void debujarFrame(){
        glViewport(0, 0, ancho, alto);
        glClearColor(0.12f, 0.20f, 0.30f, 1);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(programa);
        glBindVertexArray(vao);
        entero("uMapa", 0);
        configurarCamara();
        prepararLuces();
        escena();

        if (glGetError() != GL_NO_ERROR) {
            throw new IllegalAccessException("Error OpenGL al dibujar");
        }

    }

    private void dibujarMarcassCalle(int fila, int columna,float x, flloat z){
        if (fila%2 == 0 && columna%2==1){
            for (int desplazamiento = -3; desplazamiento <=3; desplazamiento+=3){
                caja(x+desplazamiento, 0.025f,z,1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }

        if (columna%2 == 0 && fila%2==1){
            for (int desplazamiento = -3; desplazamiento <=3; desplazamiento+=3){
                caja(x+desplazamiento, 0.025f,z,1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f);
            }
        }
    }

    private void escena(){
        caja(0,-0.25f, 0, 70, 0.5f, 70, 0.16f, 0.19f, 0.23f);
        for(int fila = 0; fila < MAPA.length; fila++){
            for(int columna = 0; columna < MAPA[fila].length; fila++){}
            float x = centro(columna);    
            float z = centro(fila);
            int tipo = MAPA[fila][columna];
            if (tipo == 0){
                dibujarMarcasCalle(int fila, int columna, float x, float z);
            }else{
                if (tipo == 1){

                }
            }
        }
    }
    //=============================00Dibujar cajas y enviar uniform ============
    private void caja (float x, float y, float z, float sx, float sy, float sz, float r, float g, float b){
        cajaGirada(x, y, z, sx, sy, sz, r, g, b, 0);
    }

    private void cajaGirada(float x, float y, float z, float sx, float sy, float sz, float r, float g, float b, float angulo){
        vector("uPos",x,y,z);
        vector("uEscala",sx,sy,sz);
        vector("uCOlor",r,g,b);
        decimal("uGiro", angulo);
        glDrawArrays(GL_TRIANGLES, 0, 36);
    }

    private int uniform(String nombre){
        if (!uniforms.containsKey(nombre)){
            int ubicacion = glGetUniformLocation(programa, nombre);
            uniforms.put(nombre,ubicacion);
        }
        return uniforms.get(nombre);
    }

    private void vector(String nombre, float x, float y, float z){
        glUniform3f(uniform(nombre), x, y, z);
    }

    private void decimal(String nombre, float valor){
        glUniform1f(uniform(nombre), valor);
    }

     private void entero(String nombre, int valor){
        glUniform1f(uniform(nombre), valor);
    }

    private String vertexShader(){
        return """
                #version 330 core //Seleccionamos GLSL 3.30
                layout(location=0) in vec3 aPos; //Leemos la posicion local del vertice desde el vbo
                layout(location=1) in vec3 aNormal;//Leamos la normal de la cara desde el mismo vbo
                uniform vec3 uPos; //Recibe el centro de la caja de la ciudad
                uniform vec3 uEscala;//Recibe la posicion de la camara
                uniform vec3 uOjo; //Punto que observa la camara
                uniform float uGiro; //Recibe el giro alrededor de Y
                uniform float uAspecto; //Recibe la relacion del ancho y alto de la imagen
                uniform int uMap ; // Seleccionamos la perspectiva (0,1)
                out vec3 vMundo; //La posicion mundial al shader de fragmentos
                out vec3 vNormal; //Enviamos la normal transformada para la iluminacion

                void main(){
                    float coseno = cos(uGiro); //Calculamos el coseno del giro del objeto
                    float seno = sin(uGiro);
                    mat3 giro = mat3(
                        coseno , 0.0, -seno,
                        0.0, 1.0, 0.0,
                        seno, 0.0, coseno
                    )
                    vMundo= giro * (aPos * uEscala) + uPos; //Escalar, girar y trasladar el vertice al mundo
                    vNormal = normalize(giro * (aNormal / uEscala)); Corregimos la normal con la inversa transpuesta de la escala y giro

                    if (uMapa== 1){ //Esta rama se usa al dibujar el minimapa
                        float pantallaX = vMUndo.x / 37.0;
                        float pantallaY = vMUndo.y / 37.0;
                        float profundidad = -vMUndo.y / 100.0;
                        gl_POsition = vec4(pantallaX, pantallaY, profundidad, 1.0)
                    }else{
                        vec3 frente = normalize(uObjeto - uOjo);
                        vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0)));
                        vec3 arriba = cross(derecha, frente);//Obtenemos el eje vertical
                        vec3 diferencia = vMundo -uOjo; //TRasladamos el origen del mundo hasta la camara
                        float vistaX = dot(diferencia, derecha); 
                        float vistaY = dot(diferencia, arrriba);
                        float vistaZ = dot(diferencia, frente);
                        float factor = 1.0/tan(radians(55.0));
                        float cerca = 0.1;
                        float lejos = 250.0;
                        float clipX = vistaX*factor/uAspecto;
                        float clipY = vistaY*factor ;
                        float clipZ = (lejos + cerca) / (cerca - lejos)*vistaZ;
                        clipZ += 2.0 *lejos * cerca / (cerca -lejos); // añadir el constante de profundidad
                        gl_Position = vec4(clipX, clipY, clipZ, -vistaZ);
                        }
                }
                """;
    }

    private String fragmentShader(){
        return """
                #version 330 core
                uniform vec3 uColor;
                out vec4 color;
                void main(){
                    color = vec4(uColor, 1.0);
                }
                """;
    }

}
