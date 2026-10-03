package ui; // Interfaz y HUD.

import graficos.Renderizador; // Dibuja las cajas del panel.
import utilidades.Constantes; // Escala del minimapa y del panel de Game Over.
import static org.lwjgl.opengl.GL33.*; // Viewport, recorte y buffers para los recuadros.

/**
 * Interfaz en pantalla: minimapa, velocímetro con indicadores de luces, vidas y monedas, y los paneles
 * de inicio, pausa, Game Over, victoria y aviso de nivel. Todo se dibuja con las mismas cajas del juego,
 * en un viewport propio y con colores directos (sin iluminación).
 */
public class PantallaHUD {

    /** Lo que la pantalla necesita saber del juego para dibujarse. */
    public record Datos(int ancho, int alto, float velocidad, float velocidadMaxima, boolean faros, boolean noche,
                        int choques, int monedas, int nivel, float timerMensaje) { }

    /** Escena que el minimapa vuelve a dibujar desde arriba. */
    public interface Escena {
        /** Dibuja la ciudad completa; vistaMapa omite los detalles pequeños y la baliza flotante. */
        void dibujar(boolean vistaMapa);

        /** Dibuja la marca del jugador por encima de los edificios del minimapa. */
        void dibujarIndicadorJugador();
    }

    private final Renderizador rend; // Dibuja las cajas de los paneles.

    public PantallaHUD(Renderizador renderizador) {
        this.rend = renderizador;
    }

    /** Compone el texto de la barra de título: controles, velocidad, luces, hora, nivel, monedas y vidas. */
    public static String componerTitulo(float velocidad, boolean noche, boolean faros, float hora, int nivel, int monedas, int vidasRestantes) {
        int kilometrosPorHora = Math.round(Math.abs(velocidad) * 3.6f); // Convierte m/s a km/h y redondea.
        String titulo = Constantes.NOMBRE_JUEGO; // Nombre de la aplicación.
        titulo += " | WASD/flechas: conducir | Espacio: freno"; // Añade los controles del vehículo.
        titulo += " | C: camara | R: reiniciar | "; // Añade los controles de cámara y reinicio.
        titulo += kilometrosPorHora + " km/h"; // Añade la magnitud de la velocidad.
        titulo += " | N: " + (noche ? "noche" : "dia") + " | F: faros " + (faros ? "ON" : "OFF"); // Indicadores de iluminación.
        titulo += String.format(" | Hora: %02d:%02d", (int) hora, (int) ((hora % 1.0f) * 60)); // Hora del juego.
        titulo += " | M: mapa"; // Muestra la tecla que alterna el minimapa.
        titulo += " | Nivel: " + nivel + " | Monedas: " + monedas + " | Vidas: " + vidasRestantes; // Solo nivel, monedas y vidas.
        titulo += " | [Espacio/Enter] Pausar [R] Reiniciar"; // Controles vigentes.
        return titulo;
    }

    // ==================== MINIMAPA: SEGUNDO PASE DE DIBUJO ====================

    /** Dibuja la misma ciudad desde arriba en un recuadro de la esquina superior derecha. */
    public void dibujarMinimapa(int ancho, int alto, Escena escena) {
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
        rend.entero("uMapa", 1); // Selecciona la proyección ortográfica del shader de clase1.
        try { // Asegura que el estado de dibujo se restaure incluso si el segundo pase falla.
            escena.dibujar(true); // Dibuja otra vez la misma ciudad, ahora vista desde arriba y sin detalles pequeños ni baliza flotante.
            escena.dibujarIndicadorJugador(); // Resalta la posición y el frente del jugador en el mapa.
        } finally { // El siguiente cuadro debe volver a la configuración de pantalla completa.
            rend.entero("uMapa", 0); // Recupera la proyección en perspectiva.
            glDisable(GL_SCISSOR_TEST); // Permite que la próxima limpieza abarque toda la pantalla.
            glViewport(0, 0, ancho, alto); // Recupera el área de dibujo de la ventana completa.
        }
        int err = glGetError(); // Consulta el error pendiente de OpenGL.
        if (err != GL_NO_ERROR) {
            System.err.println("Error OpenGL ignorado para estabilidad: " + err); // Solo informa en consola.
        }
    }

    // ==================== PANEL DE VELOCIDAD, LUCES E INICIO/PAUSA ====================

    /** Dibuja una caja plana en el panel 2D: posición y tamaño como fracciones (0 a 1) del viewport, con origen abajo a la izquierda. */
    private void cajaHUD(float fx, float fy, float fw, float fh, float cr, float cg, float cb, float nivel) {
        float mundoX = (fx + fw / 2) * 2 * Constantes.LIMITE_HUD - Constantes.LIMITE_HUD; // Convierte el centro horizontal al rango del shader (-107 a 107).
        float mundoZ = Constantes.LIMITE_HUD - (fy + fh / 2) * 2 * Constantes.LIMITE_HUD; // El shader invierte Z: la parte superior de la pantalla es -Z.
        rend.caja(mundoX, nivel, mundoZ, fw * 2 * Constantes.LIMITE_HUD, 0.1f, fh * 2 * Constantes.LIMITE_HUD, cr, cg, cb); // La altura (nivel) decide qué caja queda por encima.
    }

    /** Dibuja el panel inferior izquierdo con barra de velocidad e indicadores de luces; en inicio o pausa añade su icono. */
    public void dibujarPanelVelocidad(Datos d, MenuManager menu) {
        int ancho = d.ancho();
        int alto = d.alto();
        float velocidad = d.velocidad();
        boolean luces = d.faros();
        int anchoPanel = Math.min(240, ancho / 2); // Ancho del panel, limitado en ventanas pequeñas.
        int altoPanel = Math.min(100, alto / 4); // Alto del panel, limitado en ventanas pequeñas.
        int margen = Math.min(18, alto / 20); // Separación respecto a los bordes de la ventana.
        glEnable(GL_SCISSOR_TEST); // Activa el recorte para borrar solo el panel.
        glScissor(margen - 3, margen - 3, anchoPanel + 6, altoPanel + 6); // Panel más un borde de tres píxeles.
        glClearColor(0.55f, 0.58f, 0.62f, 1); // Color gris claro del marco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Pinta el marco y reinicia la profundidad.
        glScissor(margen, margen, anchoPanel, altoPanel); // Reduce el recorte al interior del panel.
        glClearColor(0.12f, 0.12f, 0.14f, 1); // Gris oscuro del fondo del panel.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Pinta el fondo del panel.
        glViewport(margen, margen, anchoPanel, altoPanel); // Redirige la proyección 2D al panel.
        rend.entero("uMapa", 1); // Reutiliza la proyección ortográfica del minimapa y los colores directos sin iluminación.
        try { // Garantiza que el estado de dibujo se restaure aunque algo falle.
            cajaHUD(0.05f, 0.50f, 0.90f, 0.30f, 0.30f, 0.30f, 0.32f, 1); // Marco gris de la barra de velocidad.
            float proporcion = Math.min(Math.abs(velocidad) / d.velocidadMaxima(), 1f); // Se adapta al tope actual (60 o 120 km/h).
            float rojo = 0.15f; // Color verde por defecto cuando se avanza.
            float verde = 0.85f; // Componente verde del color de avance.
            if (velocidad < 0) { // En reversa la barra se vuelve roja.
                rojo = 0.90f; // Componente roja de la reversa.
                verde = 0.15f; // Componente verde de la reversa.
            }
            cajaHUD(0.06f, 0.53f, 0.88f * proporcion, 0.24f, rojo, verde, 0.15f, 2); // Barra cuyo ancho depende de |velocidad|.
            if (luces) { // Indicador de los faros del auto.
                cajaHUD(0.05f, 0.12f, 0.12f, 0.28f, 1.0f, 0.9f, 0.1f, 2); // Amarillo brillante: faros encendidos.
            } else {
                cajaHUD(0.05f, 0.12f, 0.12f, 0.28f, 0.22f, 0.22f, 0.24f, 2); // Gris oscuro: faros apagados.
            }
            if (d.noche()) { // Segundo indicador: ambiente nocturno o diurno.
                cajaHUD(0.22f, 0.12f, 0.12f, 0.28f, 0.15f, 0.20f, 0.60f, 2); // Azul oscuro: es de noche.
            } else {
                cajaHUD(0.22f, 0.12f, 0.12f, 0.28f, 0.60f, 0.85f, 1.0f, 2); // Celeste: es de día.
            }
        } finally { // Siempre devuelve OpenGL a la configuración normal.
            glDisable(GL_SCISSOR_TEST); // Permite dibujar en toda la ventana.
            glViewport(0, 0, ancho, alto); // Recupera el área completa.
        }
        if (menu.isInicio()) { // Pantalla de inicio: panel oscuro con un triángulo de Play hecho con columnas escalonadas.
            glClear(GL_DEPTH_BUFFER_BIT); // Descarta la profundidad para que el panel quede encima.
            cajaHUD(0.28f, 0.25f, 0.44f, 0.50f, 0.04f, 0.04f, 0.07f, 6); // Panel oscuro (opaco: el shader no usa transparencia).
            for (int columna = 0; columna < 4; columna++) { // Cuatro columnas de altura decreciente hacia la derecha.
                float altura = 0.28f - columna * 0.07f; // 0,28; 0,21; 0,14; 0,07.
                cajaHUD(0.42f + columna * 0.045f, 0.50f - altura / 2, 0.045f, altura, 0.1f, 1.0f, 0.2f, 7); // Verde brillante centrado en vertical.
            }
        } else if (menu.isPausa()) { // Pausa: panel oscuro con las dos barras del icono.
            glClear(GL_DEPTH_BUFFER_BIT);
            cajaHUD(0.35f, 0.32f, 0.30f, 0.36f, 0.04f, 0.04f, 0.07f, 6); // Panel oscuro central.
            cajaHUD(0.43f, 0.42f, 0.04f, 0.16f, 1, 1, 1, 7); // Primera barra blanca vertical.
            cajaHUD(0.53f, 0.42f, 0.04f, 0.16f, 1, 1, 1, 7); // Segunda barra blanca vertical.
        }
        rend.entero("uMapa", 0); // Recupera la proyección en perspectiva para el siguiente cuadro.
    }

    // ==================== VIDAS, MONEDAS Y PANTALLAS DE FIN ====================

    /** Dibuja en 2D las vidas (arriba izquierda), las monedas (debajo), la X roja de Game Over, la V de victoria y el aviso de nivel. */
    public void dibujarHUDJuego(Datos d, MenuManager menu) {
        int ancho = d.ancho();
        int alto = d.alto();
        int choques = d.choques();
        int monedasRecolectadas = d.monedas();
        int nivel = d.nivel();
        float timerMensaje = d.timerMensaje();
        glClear(GL_DEPTH_BUFFER_BIT); // Descarta la profundidad de la escena para que el HUD quede encima.
        rend.entero("uMapa", 1); // Proyección ortográfica con colores directos.
        float lado = 0.035f; // Ancho de cada icono como fracción de la pantalla.
        float alt = lado * ancho / alto; // Altura equivalente para que el icono sea cuadrado en píxeles.
        for (int vida = 0; vida < Constantes.VIDAS_MAXIMAS; vida++) { // Una cajita por vida.
            float fx = 0.02f + vida * (lado + 0.01f); // Separa las cajitas horizontalmente.
            if (vida < Constantes.VIDAS_MAXIMAS - choques) { // Vida disponible: roja.
                cajaHUD(fx, 0.98f - alt, lado, alt, 0.90f, 0.12f, 0.15f, 5);
            } else { // Vida perdida: gris.
                cajaHUD(fx, 0.98f - alt, lado, alt, 0.35f, 0.35f, 0.38f, 5);
            }
        }
        float filaY = 0.98f - alt * 2.4f; // Segunda fila, debajo de las vidas.
        cajaHUD(0.02f, filaY, lado, alt, 1.0f, 0.85f, 0.10f, 5); // Icono amarillo de moneda.
        for (int icono = 0; icono < Math.min(monedasRecolectadas, 25); icono++) { // Una cajita pequeña por moneda (máximo 25 visibles).
            cajaHUD(0.02f + lado + 0.01f + icono * 0.009f, filaY + alt * 0.2f, 0.006f, alt * 0.6f, 1.0f, 0.85f, 0.10f, 5);
        }
        if (menu.isGameOver()) { // Sub-pantalla central con borde rojo y las letras "G" y "O".
            int cuadro = (int) (Math.min(ancho, alto) * 0.6f); // Lado del viewport cuadrado: así las unidades del mundo son cuadradas en píxeles.
            int centroX = ancho / 2; // Centro horizontal de la ventana en píxeles.
            int centroY = alto / 2; // Centro vertical de la ventana en píxeles.r
            float k = Constantes.LIMITE_HUD / Constantes.ESCALA_BASE_HUD; // Escala: el panel se diseñó para un rango de ±67 y el rango actual es ±LIMITE_HUD.
            glViewport(centroX - cuadro / 2, centroY - cuadro / 2, cuadro, cuadro); // Dentro de este viewport el mundo va de -LIMITE_HUD a LIMITE_HUD.
            rend.caja(0, 6, 0, 126 * k, 0.1f, 76 * k, 0.90f, 0.05f, 0.05f); // Borde rojo, ligeramente más grande que el panel.
            rend.caja(0, 7, 0, 120 * k, 0.1f, 70 * k, 0.08f, 0.08f, 0.08f); // Panel gris muy oscuro (y mayor = más cerca de la cámara).
            float rojo = 1.0f, verde = 0.1f, azul = 0.1f; // Rojo brillante de las letras.
            float letraX = -22 * k; // Centro de la letra G (izquierda del centro).
            rend.caja(letraX, 8, -15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // G: barra superior.
            rend.caja(letraX - 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, rojo, verde, azul); // G: barra izquierda.
            rend.caja(letraX, 8, 15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // G: barra inferior.
            rend.caja(letraX + 9 * k, 8, 7.5f * k, 6 * k, 0.1f, 15 * k, rojo, verde, azul); // G: barra derecha-inferior.
            rend.caja(letraX + 4.5f * k, 8, 0, 12 * k, 0.1f, 6 * k, rojo, verde, azul); // G: palito central.
            letraX = 22 * k; // Centro de la letra O (derecha del centro).
            rend.caja(letraX, 8, -15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // O: arriba.
            rend.caja(letraX, 8, 15 * k, 24 * k, 0.1f, 6 * k, rojo, verde, azul); // O: abajo.
            rend.caja(letraX - 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, rojo, verde, azul); // O: izquierda.
            rend.caja(letraX + 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, rojo, verde, azul); // O: derecha.
            glViewport(0, 0, ancho, alto); // Recupera la ventana completa.
        }
        if (!menu.isGameOver() && (timerMensaje > 0 || menu.isVictoria())) { // Panel central: aviso de nivel o pantalla de victoria.
            int cuadro = (int) (Math.min(ancho, alto) * 0.6f); // Lado del viewport cuadrado.
            float k = Constantes.LIMITE_HUD / Constantes.ESCALA_BASE_HUD; // Misma escala que el panel de Game Over.
            glViewport(ancho / 2 - cuadro / 2, alto / 2 - cuadro / 2, cuadro, cuadro);
            if (menu.isVictoria()) { // Panel verde con una V dorada formada por escalones de cajas.
                rend.caja(0, 6, 0, 126 * k, 0.1f, 76 * k, 1.0f, 0.85f, 0.10f); // Borde dorado.
                rend.caja(0, 7, 0, 120 * k, 0.1f, 70 * k, 0.05f, 0.45f, 0.15f); // Panel verde oscuro.
                for (int i = 0; i < 6; i++) { // Seis escalones: bajan hacia el centro por ambos lados.
                    float px = (27 - i * 5.4f) * k; // Distancia horizontal al centro en este escalón.
                    float pz = (-22.5f + i * 9) * k; // Altura del escalón (z negativo = arriba).
                    rend.caja(-px, 8, pz, 9 * k, 0.1f, 9 * k, 1.0f, 0.95f, 0.30f); // Brazo izquierdo de la V.
                    rend.caja(px, 8, pz, 9 * k, 0.1f, 9 * k, 1.0f, 0.95f, 0.30f); // Brazo derecho de la V.
                }
            } else { // Aviso temporal de subida de nivel: letra L y número.
                rend.caja(0, 6, 0, 126 * k, 0.1f, 76 * k, 1.0f, 0.85f, 0.10f); // Borde amarillo.
                rend.caja(0, 7, 0, 120 * k, 0.1f, 70 * k, 0.08f, 0.08f, 0.08f); // Panel oscuro.
                float r = 1.0f, g = 0.95f, b = 0.15f; // Amarillo brillante.
                float lx = -22 * k; // Centro de la letra L.
                rend.caja(lx - 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, r, g, b); // L: barra vertical.
                rend.caja(lx, 8, 15 * k, 24 * k, 0.1f, 6 * k, r, g, b); // L: barra inferior.
                float nx = 22 * k; // Centro del dígito.
                if (nivel == 1) { // Dígito 1: barra vertical con base.
                    rend.caja(nx, 8, 0, 6 * k, 0.1f, 36 * k, r, g, b);
                    rend.caja(nx, 8, 15 * k, 16 * k, 0.1f, 6 * k, r, g, b);
                } else if (nivel == 2) { // Dígito 2: forma de Z curva.
                    rend.caja(nx, 8, -15 * k, 24 * k, 0.1f, 6 * k, r, g, b); // Arriba.
                    rend.caja(nx + 9 * k, 8, -7.5f * k, 6 * k, 0.1f, 15 * k, r, g, b); // Derecha superior.
                    rend.caja(nx, 8, 0, 24 * k, 0.1f, 6 * k, r, g, b); // Centro.
                    rend.caja(nx - 9 * k, 8, 7.5f * k, 6 * k, 0.1f, 15 * k, r, g, b); // Izquierda inferior.
                    rend.caja(nx, 8, 15 * k, 24 * k, 0.1f, 6 * k, r, g, b); // Abajo.
                } else { // Dígito 3: tres barras y lado derecho.
                    rend.caja(nx, 8, -15 * k, 24 * k, 0.1f, 6 * k, r, g, b);
                    rend.caja(nx, 8, 0, 24 * k, 0.1f, 6 * k, r, g, b);
                    rend.caja(nx, 8, 15 * k, 24 * k, 0.1f, 6 * k, r, g, b);
                    rend.caja(nx + 9 * k, 8, 0, 6 * k, 0.1f, 36 * k, r, g, b);
                }
            }
            glViewport(0, 0, ancho, alto); // Recupera la ventana completa.
        }
        rend.entero("uMapa", 0); // Recupera la perspectiva para el siguiente cuadro.
    }
}
