package com.grafica; // Reúne la versión final con las otras tres lecciones.

import static org.lwjgl.glfw.GLFW.*; // Permite consultar la tecla que controla el minimapa.
import static org.lwjgl.opengl.GL33.*; // Permite cambiar viewport, recorte y buffers de dibujo.

/**
- CLASE 4: CIUDAD TERMINADA, ENTREGAS Y MINIMAPA.
- Orden de lectura: estado, controles, entregas, decoración y segundo pase de dibujo.
- Hereda Iluminacion, que hereda clase2, que a su vez hereda clase1.
- Los semáforos son decorativos; el ejemplo no incluye tráfico autónomo.
 */
public class JuegoEntregas extends Iluminacion {

    // ==================== 1. ESTADO DEL JUEGO ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Acumula los segundos de la partida hasta completar el recorrido.
    
    // MODIFICACIÓN: Las coordenadas de los destinos se ajustan al nuevo mapa de 11x11
    // Las calles cruzan por X/Z en -30, 0 y 30 garantizando que caen sobre asfalto.
    private static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle.
        {-30.0f, -30.0f}, // Primera entrega: Sector Noroeste.
        {  0.0f,  30.0f}, // Segunda entrega: Sector Sur Centro.
        { 30.0f, -30.0f}  // Tercera entrega: Sector Noreste.
    };

    // ==================== 2. CONTROLES Y REINICIO ====================

    /** Añade el interruptor del minimapa a los controles anteriores. */
    @Override // Amplía las teclas definidas por Iluminacion.
    protected void tecla(int key) {
        super.tecla(key); // Conserva salida, cámara, reinicio y luces.
        if (key == GLFW_KEY_M) { // Comprueba si se pulsó la tecla del mapa.
            mostrarMapa = !mostrarMapa; // Alterna entre mostrar y ocultar la vista superior.
        }
    }

    /** Reinicia el vehículo y el progreso de las entregas. */
    @Override // Amplía el reinicio definido en clase2.
    protected void reiniciar() {
        super.reiniciar(); // Restaura posición, velocidad y orientación del vehículo.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
    }

    // ==================== 3. REGLAS DE LAS ENTREGAS ====================

    /** Actualiza el vehículo y comprueba si llegó y frenó en el destino activo. */
    @Override // Añade el objetivo del juego al movimiento heredado.
    protected void actualizar(float deltaTime) {
        super.actualizar(deltaTime); // Procesa aceleración, giro, colisiones y título de la ventana.
        if (entregas >= DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = autoX - DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = autoZ - DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < 3 * 3; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(velocidad) < 1; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Compone el progreso que se añade al título de la ventana. */
    @Override // Amplía los indicadores de día/noche y faros de Iluminacion.
    protected String estadoExtra() {
        String mensaje = super.estadoExtra(); // Recupera los indicadores de iluminación.
        mensaje += " | M: mapa | "; // Muestra la tecla que alterna el minimapa.
        if (entregas == DESTINOS.length) { // Selecciona el texto de victoria al completar las tres paradas.
            mensaje += "GANASTE en " + (int) tiempo + " s! R: jugar otra vez"; // Muestra tiempo final y opción de reinicio.
        } else { // Durante el recorrido muestra progreso e instrucciones.
            mensaje += "Entregas " + entregas + "/3"; // Indica cuántas paradas se completaron.
            mensaje += " | Frena en la marca dorada | " + (int) tiempo + " s"; // Explica la condición de entrega y el tiempo.
        }
        return mensaje; // Entrega el texto a actualizarTitulo() de clase2.
    }

    // ==================== 4. ESCENA FINAL Y DESTINO ====================

    /** Añade decoración y señal del destino a la escena iluminada. */
    @Override // Amplía el dibujo acumulado de las tres etapas anteriores.
    protected void escena() {
        super.escena(); // Dibuja la ciudad, el auto y las farolas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            decorarCiudad(); // Añade árboles, bancos, ventanas y señalización urbana.
        }
        if (entregas < DESTINOS.length) { // Dibuja un objetivo únicamente mientras queden entregas.
            dibujarDestino(); // Coloca la marca dorada en la parada activa.
        }
    }

    /** Marca la próxima parada con una plataforma y una baliza flotante. */
    private void dibujarDestino() {
        float x = DESTINOS[entregas][0]; // Lee el X de la próxima entrega.
        float z = DESTINOS[entregas][1]; // Lee el Z de la próxima entrega.
        entero("uEmision", 1); // Hace que el objetivo sea visible incluso de noche.
        caja(x, 0.06f, z, 5, 0.08f, 5, 1, 0.72f, 0.12f); // Dibuja una marca dorada sobre el asfalto.
        if (!vistaMapa) { // Evita añadir una baliza tridimensional al mapa pequeño.
            float alturaBaliza = 3.5f + (float) Math.sin(tiempo * 2) * 0.3f; // Hace oscilar la baliza suavemente.
            cajaGirada(x, alturaBaliza, z, 0.8f, 0.8f, 0.8f, 1, 0.8f, 0.15f, tiempo); // Dibuja el cubo giratorio del objetivo.
        }
        entero("uEmision", 0); // Devuelve a los siguientes objetos su iluminación normal.
    }

    // ==================== 5. DECORACIÓN DE LAS MANZANAS ====================

    /** Decide qué decoración corresponde a cada tipo de parcela. */
    private void decorarCiudad() {
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre las filas del mapa.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre las columnas de esa fila.
                float x = centro(columna); // Obtiene el centro horizontal de la parcela.
                float z = centro(fila); // Obtiene el centro de la parcela en profundidad.
                int tipo = MAPA[fila][columna]; // Lee el contenido de la celda.
                switch (tipo) { // Elige la decoración según el tipo de parcela.
                    case 1 -> { // Edificio normal: la caja principal ya la dibuja AppCiudad.
                        float altura = 5 + (fila * 3 + columna * 7) % 9; // Recupera la misma altura calculada en AppCiudad.
                        dibujarVentanas(x, z, altura); // Coloca ventanas en sus cuatro fachadas.
                    }
                    case 2 -> dibujarParque(x, z); // Parque con árboles y un banco.
                    case 3 -> dibujarFuente(x, z); // Fuente central de piedra con agua.
                    case 4 -> dibujarPolicia(x, z); // Comisaría con sirenas en el techo.
                    case 5 -> dibujarBomberos(x, z); // Estación roja con portón de garaje.
                    case 6 -> dibujarGasolinera(x, z); // Techo flotante con surtidores.
                    default -> { } // Las calles (0) no llevan decoración de parcela.
                }
                if (tipo == 0 && fila >= 3 && fila <= 9 && fila % 3 == 0 && columna >= 3 && columna <= 9 && columna % 3 == 0) { // Intersección: calle cuya fila y columna son 3, 6 o 9.
                    dibujarCruce(x, z); // Dibuja los 4 pasos de cebra y los 4 semáforos del cruce.
                }
            }
        }
    }

    /** Construye una fuente de piedra con agua azul y un surtidor animado (5 cajas). */
    private void dibujarFuente(float x, float z) {
        caja(x, 0.82f, z, 7, 0.9f, 7, 0.55f, 0.55f, 0.58f); // Base de piedra que forma el borde de la fuente.
        if (noche) { // De noche el agua brilla suavemente, como las ventanas.
            entero("uEmision", 1); // Evita que la iluminación oscurezca el agua.
        }
        caja(x, 1.3f, z, 5.8f, 0.1f, 5.8f, 0.15f, 0.45f, 0.85f); // Agua azul que sobresale un poco del borde.
        entero("uEmision", 0); // Restablece el material normal.
        caja(x, 2.0f, z, 1, 1.4f, 1, 0.50f, 0.50f, 0.53f); // Columna central de piedra.
        caja(x, 2.8f, z, 2.2f, 0.25f, 2.2f, 0.58f, 0.58f, 0.60f); // Taza superior de la fuente.
        float alturaChorro = 1.2f + (float) Math.sin(tiempo * 3) * 0.25f; // Hace subir y bajar el surtidor con el tiempo.
        entero("uEmision", 1); // El chorro se ve brillante de día y de noche.
        caja(x, 2.93f + alturaChorro / 2, z, 0.3f, alturaChorro, 0.3f, 0.45f, 0.75f, 1); // Surtidor de agua celeste.
        entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
    }

    /** Construye la comisaría: edificio bajo y ancho con pórtico, placa y sirenas que alternan. */
    private void dibujarPolicia(float x, float z) {
        caja(x, 2.0f, z, 8.4f, 3.4f, 7.4f, 0.28f, 0.30f, 0.38f); // Cuerpo bajo gris azulado (unos dos pisos) sobre la acera.
        caja(x, 3.85f, z, 8.8f, 0.3f, 7.8f, 0.16f, 0.18f, 0.25f); // Cubierta ligeramente más ancha y oscura.
        caja(x, 1.3f, z + 3.75f, 1.8f, 2, 0.12f, 0.08f, 0.12f, 0.25f); // Puerta de entrada azul oscuro en la fachada sur.
        caja(x, 2.65f, z + 4.3f, 3.2f, 0.2f, 1.2f, 0.10f, 0.15f, 0.40f); // Toldo o pórtico que sobresale sobre la entrada.
        caja(x, 3.15f, z + 3.76f, 1.2f, 0.6f, 0.08f, 0.95f, 0.80f, 0.15f); // Placa amarilla que simula el escudo en la fachada.
        caja(x - 2.8f, 1.9f, z + 3.72f, 1.4f, 1, 0.08f, 0.55f, 0.70f, 0.85f); // Ventana izquierda de la fachada.
        caja(x + 2.8f, 1.9f, z + 3.72f, 1.4f, 1, 0.08f, 0.55f, 0.70f, 0.85f); // Ventana derecha de la fachada.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla cuatro veces por segundo.
        entero("uEmision", rojoEncendido ? 1 : 0); // La sirena roja brilla en las fases pares.
        caja(x - 1.2f, 4.3f, z, 1, 0.6f, 1, 0.9f, 0.1f, 0.1f); // Sirena roja sobre el techo.
        entero("uEmision", rojoEncendido ? 0 : 1); // La sirena azul brilla cuando la roja se apaga.
        caja(x + 1.2f, 4.3f, z, 1, 0.6f, 1, 0.1f, 0.25f, 0.95f); // Sirena azul sobre el techo.
        entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
    }

    /** Construye la estación de bomberos: edificio bajo y ancho con dos portones, techo saliente y torre. */
    private void dibujarBomberos(float x, float z) {
        caja(x, 2.1f, z, 8.8f, 3.6f, 7.4f, 0.80f, 0.12f, 0.10f); // Cuerpo bajo rojo llamativo (unos dos pisos).
        caja(x, 4.05f, z, 9.6f, 0.3f, 8.2f, 0.45f, 0.06f, 0.06f); // Techo más ancho que sobresale de las paredes.
        caja(x - 2.2f, 1.5f, z + 3.75f, 3.4f, 2.4f, 0.12f, 0.18f, 0.18f, 0.20f); // Primer portón gris oscuro del garaje.
        caja(x + 2.2f, 1.5f, z + 3.75f, 3.4f, 2.4f, 0.12f, 0.18f, 0.18f, 0.20f); // Segundo portón para otro camión.
        caja(x, 3.2f, z + 3.72f, 8, 0.35f, 0.06f, 0.90f, 0.90f, 0.88f); // Franja blanca sobre los portones.
        caja(x + 3.3f, 5.6f, z - 2.4f, 1.4f, 2.8f, 1.4f, 0.60f, 0.10f, 0.10f); // Torre pequeña de secado de mangueras.
        caja(x + 3.3f, 7.1f, z - 2.4f, 1.7f, 0.2f, 1.7f, 0.30f, 0.05f, 0.05f); // Remate oscuro de la torre.
    }

    /** Construye la gasolinera: techo flotante sobre dos columnas con dos surtidores (6 cajas). */
    private void dibujarGasolinera(float x, float z) {
        caja(x, 4.2f, z, 8, 0.35f, 6, 0.92f, 0.92f, 0.94f); // Techo plano flotante blanco.
        caja(x, 4.45f, z, 8.2f, 0.15f, 6.2f, 0.90f, 0.70f, 0.10f); // Banda amarilla sobre el borde del techo.
        caja(x - 3, 2.25f, z, 0.3f, 3.9f, 0.3f, 0.35f, 0.37f, 0.40f); // Columna fina izquierda.
        caja(x + 3, 2.25f, z, 0.3f, 3.9f, 0.3f, 0.35f, 0.37f, 0.40f); // Columna fina derecha.
        caja(x - 1.2f, 1.1f, z, 0.7f, 1.6f, 0.5f, 0.85f, 0.15f, 0.10f); // Surtidor rojo izquierdo.
        caja(x + 1.2f, 1.1f, z, 0.7f, 1.6f, 0.5f, 0.15f, 0.55f, 0.20f); // Surtidor verde derecho.
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
        }
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada norte.
                caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada sur.
                caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada oeste.
                caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada este.
            }
        }
        entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Dibuja en una intersección 4 pasos de cebra (uno por entrada) y 4 semáforos en sus esquinas. */
    private void dibujarCruce(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) { // Siete franjas por cada paso de cebra.
            caja(x + desplazamiento, 0.045f, z - 7.5f, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Entrada norte: se cruza a lo largo de X.
            caja(x + desplazamiento, 0.045f, z + 7.5f, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Entrada sur: se cruza a lo largo de X.
            caja(x - 7.5f, 0.045f, z + desplazamiento, 2, 0.04f, 0.45f, 0.85f, 0.87f, 0.83f); // Entrada oeste: se cruza a lo largo de Z.
            caja(x + 7.5f, 0.045f, z + desplazamiento, 2, 0.04f, 0.45f, 0.85f, 0.87f, 0.83f); // Entrada este: se cruza a lo largo de Z.
        }

        int faseNorteSur = (int) (tiempo % 12); // Ciclo de 12 s para el tráfico que circula en dirección Z.
        int faseEsteOeste = (int) ((tiempo + 6) % 12); // El tráfico en X va desfasado medio ciclo respecto al de Z.
        for (int signoX = -1; signoX <= 1; signoX += 2) { // Elige la esquina oeste (-1) o este (+1).
            for (int signoZ = -1; signoZ <= 1; signoZ += 2) { // Elige la esquina norte (-1) o sur (+1).
                float postesX = x + signoX * 5.6f; // Coloca el poste sobre la acera de la manzana diagonal.
                float postesZ = z + signoZ * 5.6f; // Coloca el poste en la misma esquina, en profundidad.
                caja(postesX, 1.7f, postesZ, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
                caja(postesX, 3.1f, postesZ, 0.5f, 1.2f, 0.5f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa con una cara hacia cada calle.
                for (int eje = 0; eje < 2; eje++) { // Eje 0: cara que mira a Z; eje 1: cara que mira a X.
                    int fase = faseNorteSur; // Parte de la fase del tráfico en Z.
                    if (eje == 1) { // Comprueba si se dibuja la cara orientada a X.
                        fase = faseEsteOeste; // Usa la fase desfasada del otro sentido.
                    }
                    for (int indice = 0; indice < 3; indice++) { // Recorre rojo arriba, amarillo al centro y verde abajo.
                        boolean encendida = false; // Parte de una bombilla apagada.
                        if (indice == 0) { // Selecciona la bombilla roja.
                            encendida = fase < 5; // Mantiene rojo durante los primeros cinco segundos.
                        } else if (indice == 1) { // Selecciona la bombilla amarilla.
                            encendida = fase >= 10; // Mantiene amarillo en los últimos dos segundos del ciclo.
                        } else { // Selecciona la bombilla verde.
                            encendida = fase >= 5 && fase < 10; // Mantiene verde durante los cinco segundos intermedios.
                        }
                        float brillo = 0.15f; // Conserva un color tenue cuando la bombilla está apagada.
                        entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
                        if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                            brillo = 1; // Usa intensidad completa para su color.
                            entero("uEmision", 1); // Hace que la bombilla se vea encendida.
                        }
                        float rojo = 0; // Componente roja inicialmente ausente.
                        float verde = 0; // Componente verde inicialmente ausente.
                        if (indice < 2) { // Rojo y amarillo necesitan componente roja.
                            rojo = brillo; // Añade rojo con la intensidad elegida.
                        }
                        if (indice > 0) { // Amarillo y verde necesitan componente verde.
                            verde = brillo; // Añade verde; rojo más verde produce amarillo.
                        }
                        float altura = 3.45f - indice * 0.35f; // Separa verticalmente las tres bombillas.
                        if (eje == 0) { // Cara que mira hacia el centro del cruce a lo largo de Z.
                            caja(postesX, altura, postesZ - signoZ * 0.27f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Bombilla en la cara norte o sur.
                        } else { // Cara que mira hacia el centro del cruce a lo largo de X.
                            caja(postesX - signoX * 0.27f, altura, postesZ, 0.06f, 0.25f, 0.28f, rojo, verde, 0.02f); // Bombilla en la cara oeste o este.
                        }
                    }
                }
            }
        }
        entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    // ==================== 6. MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la escena principal y después la misma ciudad desde arriba, en un recuadro. */
    @Override // Amplía el cuadro completo definido en clase1.
    protected void dibujarFrame() {
        super.dibujarFrame(); // Dibuja primero la vista normal de la ciudad.
        if (!mostrarMapa) { // Comprueba si el usuario ocultó el minimapa con M.
            return; // Conserva únicamente la imagen principal.
        }
        int dimensionMenor = Math.min(ancho, alto); // Busca la dimensión que limita el espacio disponible.
        int lado = Math.min(260, dimensionMenor / 3); // Limita el mapa a 260 píxeles y a un tercio de la ventana.
        int margen = Math.min(18, dimensionMenor / 20); // Calcula una separación adaptable respecto a los bordes.
        int x = ancho - lado - margen; // Ubica el recuadro cerca del borde derecho.
        int y = alto - lado - margen; // Ubica el recuadro arriba; OpenGL mide Y desde abajo.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para no borrar el resto de la escena.
        glScissor(x - 3, y - 3, lado + 6, lado + 6); // Selecciona el mapa más un borde de tres píxeles.
        glClearColor(0.8f, 0.87f, 0.94f, 1); // Define un color claro para el marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra solo el recuadro exterior gracias al scissor.
        glScissor(x, y, lado, lado); // Reduce el recorte al interior del minimapa.
        glClearColor(0.06f, 0.10f, 0.15f, 1); // Define el fondo oscuro del mapa.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Limpia color y profundidad dentro del mapa.
        glViewport(x, y, lado, lado); // Redirige la proyección al recuadro cuadrado.
        entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        vistaMapa = true; // Indica a escena() que omita decoración pequeña y baliza flotante.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena(); // Dibuja otra vez la misma ciudad, ahora vista desde arriba.
            dibujarIndicadorAuto(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            vistaMapa = false; // Reactiva los detalles de la escena principal.
            entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        if (glGetError() != GL_NO_ERROR) { // Comprueba que el pase del mapa no haya generado errores OpenGL.
            throw new IllegalStateException("Error OpenGL en minimapa"); // Expone el error en la consola.
        }
    }

    /** Dibuja una marca cian y una punta blanca por encima de los edificios del minimapa. */
    private void dibujarIndicadorAuto() {
        cajaGirada(autoX, 25, autoZ, 2.2f, 0.1f, 3.2f, 0.1f, 1, 1, angulo); // Marca la posición con un rectángulo cian orientado.
        float frenteX = -(float) Math.sin(angulo); // Calcula la dirección frontal en el eje X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula la dirección frontal en el eje Z.
        float puntaX = autoX + frenteX * 2; // Desplaza la punta dos unidades hacia delante en X.
        float puntaZ = autoZ + frenteZ * 2; // Desplaza la punta dos unidades hacia delante en Z.
        caja(puntaX, 26, puntaZ, 0.9f, 0.1f, 0.9f, 1, 1, 1); // Dibuja la punta blanca encima del indicador cian.
    }

    /** Punto de entrada del proyecto final. */
    public static void main(String[] args) {
        JuegoEntregas aplicacion = new JuegoEntregas(); // Crea la versión con todas las etapas acumuladas.
        aplicacion.run(); // Inicia el ciclo de vida completo de la aplicación.
    }
}