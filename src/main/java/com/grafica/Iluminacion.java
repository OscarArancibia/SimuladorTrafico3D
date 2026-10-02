package com.grafica; // Mantiene esta lección junto a las clases anteriores.

import static org.lwjgl.glfw.GLFW.*; // Incluye las constantes de las teclas N y F.

/**
 * CLASE 3: ILUMINACIÓN DE LA CIUDAD.
 * Conserva ciudad y conducción; añade sol, farolas y focos del vehículo.
 * El shader calcula iluminación local: este ejemplo todavía no proyecta sombras.
 */
public class Iluminacion extends Vehiculo {

    // ==================== 1. ESTADO Y POSICIONES DE LAS LUCES ====================
    protected boolean noche = true; // Inicia la escena con iluminación nocturna.
    protected boolean faros = true; // Inicia los focos del auto encendidos.
    protected static final float ALTURA_LUZ = 4.5f; // Altura de la bombilla: la usan el shader y el modelo de la farola.
    protected static final int TOTAL_LUCES = 49; // Una farola en cada cruce de la cuadrícula de calles de 7x7.
    protected static final float[][] LUCES = new float[TOTAL_LUCES][3]; // Cada fila contiene X, Y, Z de una bombilla.
    static { // Se ejecuta una vez al cargar la clase y rellena la cuadrícula.
        int indice = 0; // Posición lineal en el arreglo (0 a 48).
        int[] indicesMapa = {1, 4, 7, 10, 13, 16, 19}; // Filas y columnas de calle del mapa de 21x21 (la 1 y la 19 forman el anillo).
        for (int fila : indicesMapa) { // Recorre las 7 filas de calle.
            for (int col : indicesMapa) { // Recorre las 7 columnas de calle.
                float offsetX = (col == 19) ? -6.0f : 6.0f; // En el cruce del último índice se invierte el desplazamiento: la acera queda hacia dentro.
                float offsetZ = (fila == 19) ? -6.0f : 6.0f; // Misma regla en profundidad.
                LUCES[indice][0] = (col - 10) * 10.0f + offsetX; // Coordenada X: esquina de la acera, 1 unidad dentro de la manzana vecina.
                LUCES[indice][1] = ALTURA_LUZ; // Altura de la bombilla.
                LUCES[indice][2] = (fila - 10) * 10.0f + offsetZ; // Coordenada Z.
                indice++;
            }
        }
    }

    // ==================== 2. CONTROLES E INDICADORES ====================

    /** Añade los interruptores de iluminación a los controles existentes. */
    @Override // Amplía el método de teclado de clase2.
    protected void tecla(int key) {
        super.tecla(key); // Conserva ESC, C y R.
        if (key == GLFW_KEY_N) { // Comprueba la tecla de cambio de ambiente.
            noche = !noche; // Alterna entre noche y día.
            faros = noche; // Los faros del auto se encienden de noche y se apagan de día.
        }
        if (key == GLFW_KEY_F) { // Comprueba el interruptor de los focos del auto.
            faros = !faros; // Enciende los faros si estaban apagados y viceversa.
        }
    }

    /** Prepara el texto que clase2 incorpora al título de la ventana. */
    @Override // Añade información al espacio reservado para indicadores.
    protected String estadoExtra() {
        String ambiente = "dia"; // Usa día como texto inicial.
        if (noche) { // Comprueba si está seleccionado el ambiente nocturno.
            ambiente = "noche"; // Reemplaza el texto por el estado real.
        }
        String estadoFaros = "OFF"; // Usa apagado como texto inicial de los focos.
        if (faros) { // Comprueba si las luces del auto están activas.
            estadoFaros = "ON"; // Indica que los faros están encendidos.
        }
        return " | N: " + ambiente + " | F: faros " + estadoFaros; // Devuelve ambos indicadores y sus teclas.
    }

    // ==================== 3. DATOS QUE RECIBE EL SHADER ====================

    /** Actualiza los uniforms de iluminación antes de dibujar la ciudad. */
    @Override // Sustituye el método vacío de clase1.
    protected void prepararLuces() {
        entero("uNoche", 0); // Inicialmente configura iluminación diurna.
        if (noche) { // Revisa el modo elegido por el usuario.
            entero("uNoche", 1); // Comunica al shader que debe usar iluminación nocturna.
        }
        entero("uFaros", 0); // Inicialmente desactiva los conos de luz del auto.
        if (faros) { // Revisa el interruptor de los faros.
            entero("uFaros", 1); // Activa el cálculo de los dos focos en el shader.
        }
        entero("uEmision", 0); // Hace que los objetos normales reciban iluminación.

        for (int indice = 0; indice < LUCES.length; indice++) { // Recorre todas las bombillas (49).
            float x = LUCES[indice][0]; // Lee la coordenada horizontal de la bombilla.
            float y = LUCES[indice][1]; // Lee su altura sobre el suelo.
            float z = LUCES[indice][2]; // Lee su coordenada en profundidad.
            vector("uLuces[" + indice + "]", x, y, z); // Envía esa posición al arreglo GLSL.
        }

        float frenteX = -(float) Math.sin(angulo); // Calcula hacia dónde apunta el auto en X.
        float frenteZ = -(float) Math.cos(angulo); // Calcula hacia dónde apunta el auto en Z.
        vector("uAuto", autoX, 0.7f, autoZ); // Envía el centro del vehículo a la altura de sus focos.
        vector("uFrente", frenteX, 0, frenteZ); // Envía la dirección frontal del vehículo.
    }

    /** Envía al shader los focos de un vehículo autónomo (índice 0 a 15). */
    protected void enviarFarosVehiculo(int indice, float x, float z, float anguloVehiculo, float delante, float lado) {
        vector("uVehPos[" + indice + "]", x, 0.68f, z); // Centro del vehículo a la altura de los faros.
        vector("uVehFrente[" + indice + "]", -(float) Math.sin(anguloVehiculo), 0, -(float) Math.cos(anguloVehiculo)); // Frente local = -Z.
        vector("uVehDim[" + indice + "]", delante, lado, 0); // Distancia al frente y separación lateral de los faros.
    }

    // ==================== 4. MODELOS DE LAS FAROLAS ====================

    /** Dibuja postes y bombillas en las mismas posiciones usadas para calcular la luz. */
    @Override // Añade geometría a la ciudad y al auto heredados.
    protected void escena() {
        super.escena(); // Dibuja la ciudad y el vehículo de las primeras dos lecciones.
        for (int i = 0; i < LUCES.length; i++) { // Recorre todas las luces.
            int filaLuz = i / 7; // Fila de la cuadrícula 7x7 (0 a 6); 2, 3 y 4 equivalen a las calles 7, 10 y 13.
            int columnaLuz = i % 7; // Columna de la cuadrícula 7x7.
            if (filaLuz >= 2 && filaLuz <= 4 && columnaLuz >= 2 && columnaLuz <= 4) { // Cruce interior con semáforos (misma regla que decorarCiudad).
                continue; // Omite solo el poste: la luz sigue enviándose al shader desde prepararLuces().
            }
            dibujarFarola(LUCES[i][0], LUCES[i][2]); // Dibuja el poste exactamente donde está la fuente de luz del shader.
        }
    }

    /** Dibuja una farola: poste gris alto y bloque en la punta, centrado en la bombilla (x, ALTURA_LUZ, z). */
    protected void dibujarFarola(float x, float z) {
        caja(x, ALTURA_LUZ / 2 - 0.05f, z, 0.18f, ALTURA_LUZ - 0.1f, 0.18f, 0.20f, 0.24f, 0.28f); // Poste que termina justo bajo la bombilla.
        float rojo = 0.22f; // De día: vidrio oscuro apagado.
        float verde = 0.24f;
        float azul = 0.27f;
        if (noche) { // Solo de noche la bombilla aparenta estar encendida.
            entero("uEmision", 1); // Evita que la bombilla sea oscurecida por la iluminación.
            rojo = 1; verde = 0.83f; azul = 0.42f; // Color cálido brillante.
        }
        caja(x, ALTURA_LUZ, z, 0.7f, 0.35f, 0.7f, rojo, verde, azul); // Bloque de la bombilla, en la misma posición que uLuces[i].
        entero("uEmision", 0); // Restablece el material normal.
    }

    // ==================== 5. CÁLCULO DE LUZ EN LA GPU ====================

    /** Devuelve el programa GLSL que calcula el color iluminado de cada fragmento. */
    @Override // Reemplaza el shader de color plano de clase1.
    protected String fragmentShader() {
        return """
            #version 330 core // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
            in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
            in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
            uniform vec3 uColor; // Recibe el color base de la caja.
            uniform vec3 uLuces[49]; // Recibe las posiciones de las 49 farolas.
            uniform vec3 uAuto; // Recibe la posición del auto a la altura de los faros.
            uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
            uniform vec3 uVehPos[16]; // Recibe el centro de cada vehículo autónomo a la altura de sus faros.
            uniform vec3 uVehFrente[16]; // Recibe la dirección frontal de cada vehículo autónomo.
            uniform vec3 uVehDim[16]; // Recibe en x la distancia al frente y en y la separación lateral de los faros.
            uniform int uVehN; // Recibe cuántos vehículos autónomos tienen los faros encendidos.
            uniform int uNoche; // Vale 1 de noche y 0 de día.
            uniform int uFaros; // Vale 1 cuando los focos están encendidos.
            uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
            uniform int uMapa; // Vale 1 durante el dibujo del minimapa de JuegoEntregas.
            out vec4 color; // Entrega el color RGBA final al framebuffer.

            void main() { // Se ejecuta para cada fragmento visible de una caja.
                if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
                    color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
                    return; // Termina el shader sin calcular iluminación.
                }

                vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
                vec3 luz = vec3(0.48); // Define la luz ambiental diurna que llega a todas las caras.
                float intensidadSol = 0.65; // Define la fuerza de la iluminación direccional diurna.
                if (uNoche == 1) { // Ajusta el ambiente si es de noche.
                    luz = vec3(0.12, 0.16, 0.24); // Usa una luz ambiental tenue y azulada.
                    intensidadSol = 0.10; // Conserva una pequeña luz direccional nocturna.
                }
                vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // Define una fuente lejana por su dirección.
                float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe más luz si mira al sol.
                luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribución direccional al ambiente.

                if (uNoche == 1) { // Calcula la iluminación de las farolas solo de noche.
                    for (int indice = 0; indice < 49; indice++) { // Acumula el aporte de cada bombilla.
                        vec3 haciaLuz = uLuces[indice] - vMundo; // Forma el vector desde la superficie hacia la farola.
                        float distancia = length(haciaLuz); // Mide cuántas unidades separan superficie y bombilla.
                        float difusa = max(dot(normal, normalize(haciaLuz)), 0.0); // Calcula la incidencia de la luz sobre la cara.
                        float atenuacion = 1.0 + 0.12 * distancia + 0.045 * distancia * distancia; // Reduce el alcance con la distancia.
                        vec3 colorFarola = vec3(1.0, 0.73, 0.34); // Define el tono cálido de la farola.
                        luz += colorFarola * difusa * 3.0 / atenuacion; // Suma el aporte atenuado de esta bombilla.
                    }
                }

                if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
                    for (int indice = 0; indice < 2; indice++) { // Repite el cálculo para los dos faros.
                        vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Obtiene la dirección hacia el lado derecho del auto.
                        float separacion = -0.55; // Selecciona inicialmente el faro izquierdo.
                        if (indice == 1) { // Comprueba si corresponde calcular el segundo faro.
                            separacion = 0.55; // Desplaza el segundo faro al lado derecho.
                        }
                        vec3 origen = uAuto + uFrente * 1.36 + lateral * separacion; // Ubica el faro delante de la carrocería.
                        vec3 haciaSuperficie = vMundo - origen; // Forma el vector del faro al fragmento.
                        float distancia = length(haciaSuperficie); // Mide la distancia recorrida por la luz.
                        vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Inclina el foco ligeramente hacia el suelo.
                        float alineacion = dot(normalize(haciaSuperficie), eje); // Un valor cercano a 1 indica el centro del haz.
                        float cono = smoothstep(0.85, 0.97, alineacion); // Suaviza el borde entre el exterior y el interior del foco.
                        float difusa = max(dot(normal, -normalize(haciaSuperficie)), 0.0); // Mide cuánto mira la cara hacia el faro.
                        float atenuacion = 1.0 + 0.04 * distancia * distancia; // Disminuye la intensidad al alejarse.
                        vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Define una luz frontal blanca y cálida.
                        luz += colorFaro * cono * difusa * 8.0 / atenuacion; // Añade el aporte del foco a la iluminación total.
                    }
                }

                for (int v = 0; v < uVehN; v++) { // Repite el cálculo de los conos para cada vehículo autónomo.
                    vec3 lateralV = vec3(-uVehFrente[v].z, 0.0, uVehFrente[v].x); // Dirección hacia el lado derecho del vehículo.
                    for (int lado = 0; lado < 2; lado++) { // Faro izquierdo y derecho.
                        float sep = (lado == 0) ? -uVehDim[v].y : uVehDim[v].y; // Separación lateral con signo.
                        vec3 origenV = uVehPos[v] + uVehFrente[v] * uVehDim[v].x + lateralV * sep; // Ubica el faro en el frente.
                        vec3 haciaSup = vMundo - origenV; // Vector del faro al fragmento.
                        float distV = length(haciaSup); // Distancia recorrida por la luz.
                        vec3 ejeV = normalize(uVehFrente[v] + vec3(0.0, -0.10, 0.0)); // Inclina el haz hacia el suelo.
                        float conoV = smoothstep(0.85, 0.97, dot(normalize(haciaSup), ejeV)); // Suaviza el borde del haz.
                        float difusaV = max(dot(normal, -normalize(haciaSup)), 0.0); // Cuánto mira la cara hacia el faro.
                        luz += vec3(1.0, 0.94, 0.72) * conoV * difusaV * 8.0 / (1.0 + 0.04 * distV * distV); // Suma el aporte del foco.
                    }
                }

                color = vec4(uColor * luz, 1.0); // Multiplica el material por toda la luz acumulada.
            }
            """; // Finaliza el texto GLSL que Java enviará a OpenGL.
    }

    /** Punto de entrada de la tercera lección. */
    public static void main(String[] args) {
        Iluminacion aplicacion = new Iluminacion(); // Crea la etapa con iluminación.
        aplicacion.run(); // Inicia el ciclo de vida heredado de clase1.
    }
}
