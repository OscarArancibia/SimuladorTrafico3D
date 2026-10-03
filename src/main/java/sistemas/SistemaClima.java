package sistemas; // Lógicas invisibles del juego: clima, hora del día, IA, colisiones.

import graficos.Renderizador; // Primitivas para dibujar sol, nubes y lluvia.
import utilidades.Constantes; // Tiempos, velocidades y límites del clima.

/**
 * PASO 2 DE LA REFACTORIZACIÓN: reloj del día, ciclo despejado/lluvia, gotas y nubes.
 * Es puramente visual: no afecta a la jugabilidad. La posición del jugador se recibe por parámetro
 * para que la lluvia caiga a su alrededor; la clase no conoce al vehículo.
 */
public class SistemaClima {

    private final float[] gotaX = new float[Constantes.MAX_GOTAS];
    private final float[] gotaY = new float[Constantes.MAX_GOTAS];
    private final float[] gotaZ = new float[Constantes.MAX_GOTAS];
    private final float[] gotaVel = new float[Constantes.MAX_GOTAS]; // Velocidad de caída propia de cada gota.
    private final float[] gotaLargo = new float[Constantes.MAX_GOTAS]; // Largo propio de cada gota.
    private final float[] gotaSalpica = new float[Constantes.MAX_GOTAS]; // Segundos restantes del chapoteo al tocar el suelo (0 = cayendo).
    private final float[] nubeX = new float[Constantes.MAX_NUBES];
    private final float[] nubeZ = new float[Constantes.MAX_NUBES];
    private float timerCicloClima = Constantes.CICLO_DESPEJADO; // Segundos hasta el próximo cambio de clima.
    private boolean estaLloviendo = false; // 60 s despejado, 15 s de lluvia, y se repite.
    private boolean surazoActivo = false; // Tormenta de viento extremo: oscurece el día, trae lluvia en diagonal y limita la velocidad.
    private float intensidadTormenta = 0; // 0 = nada, 1 = tormenta completa; sube y baja con suavidad al activar o desactivar el surazo.
    private boolean reiniciarGotasPendiente = false; // Al empezar el surazo las gotas deben nacer arriba, no todas en el suelo.
    private float limiteVelocidadViento = Float.MAX_VALUE; // Tope de velocidad que impone el viento (sin tope = MAX_VALUE).
    private float tiempoDia = Constantes.HORA_INICIAL; // Hora del juego (0 a 24); empieza a las 8 AM. 24 horas = 120 s reales.
    private boolean noche; // true entre las 18:00 y las 06:00.
    private float factorNoche; // 0 = pleno día, 1 = plena noche; valores intermedios en atardeceres y amaneceres.

    public SistemaClima() {
        aplicarCicloDia(); // Calcula noche y factorNoche a partir de la hora inicial.
    }

    // ==================== ESTADO ====================

    public boolean isNoche() { return noche; }

    public float getFactorNoche() { return factorNoche; }

    /** true si llueve por el ciclo normal o porque el surazo trae su propia lluvia. */
    public boolean isLloviendo() { return estaLloviendo || surazoActivo; }

    public float getHora() { return tiempoDia; }

    /** Fija la hora del juego (la tecla N salta a una hora diurna o nocturna) y recalcula la iluminación. */
    public void setHora(float hora) {
        tiempoDia = hora;
        aplicarCicloDia();
    }

    // ==================== CICLO DE VIDA ====================

    /** Reparte la lluvia inicial alrededor del jugador y las nubes por todo el mapa. */
    public void iniciar(float jugadorX, float jugadorZ) {
        for (int i = 0; i < Constantes.MAX_GOTAS; i++) { // Lluvia inicial alrededor del jugador, a alturas repartidas.
            reiniciarGota(i, 0.0f, jugadorX, jugadorZ);
        }
        for (int i = 0; i < Constantes.MAX_NUBES; i++) { // Nubes repartidas por todo el mapa.
            nubeX[i] = (float) (Math.random() * Constantes.RANGO_APARICION_NUBES - Constantes.LIMITE_NUBES);
            nubeZ[i] = (float) (Math.random() * Constantes.RANGO_APARICION_NUBES - Constantes.LIMITE_NUBES);
        }
    }

    /** Tecla U: activa o desactiva el surazo. Al activarlo, si no llovía, empieza a llover. */
    public void alternarSurazo() {
        surazoActivo = !surazoActivo;
        if (surazoActivo && !estaLloviendo) {
            reiniciarGotasPendiente = true; // Las gotas renacen arriba y desfasadas en la próxima actualización.
        }
    }

    /** Qué tan oscuro y tormentoso está el ambiente: 0 normal, 1 tormenta completa (el shader y el cielo lo usan). */
    public float getIntensidadTormenta() { return intensidadTormenta; }

    /**
     * Con el surazo, el viento frena al auto: devuelve la velocidad máxima limitada a 40 km/h. El tope baja poco a poco desde
     * la velocidad que llevaba el auto, para que no haya un frenazo; sin surazo devuelve la velocidad máxima sin cambios.
     */
    public float limitarVelocidad(float velocidadMaxima, float velocidadActual, float deltaTime) {
        if (!surazoActivo) {
            limiteVelocidadViento = Float.MAX_VALUE;
            return velocidadMaxima;
        }
        if (limiteVelocidadViento == Float.MAX_VALUE) { // Recién empieza: parte de la velocidad con la que iba.
            limiteVelocidadViento = Math.max(velocidadActual, Constantes.VELOCIDAD_MAXIMA_SURAZO);
        }
        limiteVelocidadViento = Math.max(Constantes.VELOCIDAD_MAXIMA_SURAZO, limiteVelocidadViento - Constantes.DESACELERACION_SURAZO * deltaTime);
        return Math.min(velocidadMaxima, limiteVelocidadViento);
    }

    public boolean isSurazoActivo() { return surazoActivo; }

    /** Fuerza del viento hacia +X en u/s: la del surazo si está activo y 0 si no. El clima la dicta; el auto y la lluvia la sufren. */
    public float getFuerzaVientoX() { return surazoActivo ? Constantes.FUERZA_VIENTO_X : 0.0f; }

    /** Vuelve a empezar despejado y a las 8 AM, sin surazo. */
    public void reiniciar() {
        surazoActivo = false;
        intensidadTormenta = 0;
        reiniciarGotasPendiente = false;
        limiteVelocidadViento = Float.MAX_VALUE;
        timerCicloClima = Constantes.CICLO_DESPEJADO;
        estaLloviendo = false; // El clima vuelve a empezar despejado.
        tiempoDia = Constantes.HORA_INICIAL; // El día vuelve a empezar a las 8 AM.
        aplicarCicloDia();
    }

    // ==================== ACTUALIZACIÓN ====================

    /** Avanza el reloj del día: 2 minutos reales = 24 horas del juego (se congela en pausa porque el juego no llama aquí). */
    public void avanzarHora(float deltaTime) {
        tiempoDia += (Constantes.HORAS_POR_DIA / Constantes.DURACION_DIA_REAL) * deltaTime;
        if (tiempoDia >= Constantes.HORAS_POR_DIA) tiempoDia -= Constantes.HORAS_POR_DIA;
        aplicarCicloDia();
    }

    /** Ciclo despejado/lluvia, caída de las gotas y viento de las nubes. */
    public void actualizar(float deltaTime, float jugadorX, float jugadorZ) {
        float paso = deltaTime / Constantes.TRANSICION_TORMENTA; // La tormenta entra y sale con suavidad.
        float objetivoTormenta = surazoActivo ? 1.0f : 0.0f;
        intensidadTormenta += Math.max(-paso, Math.min(paso, objetivoTormenta - intensidadTormenta));
        if (reiniciarGotasPendiente) { // Empieza el surazo: las gotas nacen arriba y desfasadas, alrededor del jugador.
            for (int i = 0; i < Constantes.MAX_GOTAS; i++) {
                reiniciarGota(i, Constantes.ALTURA_REINICIO_GOTA, jugadorX, jugadorZ);
            }
            reiniciarGotasPendiente = false;
        }
        timerCicloClima -= deltaTime; // Ciclo de clima.
        if (timerCicloClima <= 0) {
            estaLloviendo = !estaLloviendo;
            timerCicloClima = estaLloviendo ? Constantes.CICLO_LLUVIA : Constantes.CICLO_DESPEJADO; // 15 s de lluvia, 60 s despejado.
            if (estaLloviendo) { // Al empezar, las gotas nacen arriba y desfasadas, alrededor del jugador.
                for (int i = 0; i < Constantes.MAX_GOTAS; i++) {
                    reiniciarGota(i, Constantes.ALTURA_REINICIO_GOTA, jugadorX, jugadorZ); // Todas arriba y desfasadas: no caen en bloque.
                }
            }
        }
        if (estaLloviendo || surazoActivo) { // Lluvia (la del ciclo o la del surazo): cae y reaparece arriba cerca del jugador.
            for (int i = 0; i < Constantes.MAX_GOTAS; i++) {
                if (gotaSalpica[i] > 0) { // Chapoteando en el suelo: al terminar renace arriba.
                    gotaSalpica[i] -= deltaTime;
                    if (gotaSalpica[i] <= 0) reiniciarGota(i, Constantes.ALTURA_REINICIO_GOTA, jugadorX, jugadorZ);
                } else {
                    gotaY[i] -= gotaVel[i] * deltaTime;
                    if (surazoActivo) { // La lluvia cae en diagonal, más rápido hacia el lado que el propio auto.
                        gotaX[i] += getFuerzaVientoX() * Constantes.FACTOR_LLUVIA_VIENTO * deltaTime;
                        if (gotaX[i] > jugadorX + Constantes.RADIO_LLUVIA_VIENTO) { // Se salió del radio de visión: reaparece a barlovento.
                            gotaX[i] = jugadorX - Constantes.RADIO_LLUVIA_VIENTO;
                        }
                    }
                    if (gotaY[i] <= 0) { // Toca el suelo: queda una salpicadura 0,2 s.
                        gotaY[i] = 0;
                        gotaSalpica[i] = Constantes.DURACION_SALPICADURA;
                    }
                }
            }
        }
        for (int i = 0; i < Constantes.MAX_NUBES; i++) { // Viento: las nubes avanzan en X y dan la vuelta al salir del mapa.
            nubeX[i] += Constantes.VELOCIDAD_NUBES * deltaTime;
            if (nubeX[i] > Constantes.LIMITE_NUBES) nubeX[i] = -Constantes.LIMITE_NUBES;
        }
    }

    /** Coloca una gota arriba, cerca del jugador, con velocidad y largo propios; alturaMin = 0 la reparte por todo el aire. */
    private void reiniciarGota(int i, float alturaMin, float jugadorX, float jugadorZ) {
        gotaX[i] = jugadorX + (float) (Math.random() - 0.5) * Constantes.DISPERSION_GOTAS;
        gotaZ[i] = jugadorZ + (float) (Math.random() - 0.5) * Constantes.DISPERSION_GOTAS;
        gotaY[i] = alturaMin + (float) Math.random() * (Constantes.ALTURA_MAX_GOTA - alturaMin); // Hasta unas 40 unidades de altura.
        gotaVel[i] = Constantes.VEL_GOTA_MIN + (float) Math.random() * Constantes.VEL_GOTA_VARIACION; // 32 a 50 u/s.
        gotaLargo[i] = Constantes.LARGO_GOTA_MIN + (float) Math.random() * Constantes.LARGO_GOTA_VARIACION; // 0,8 a 1,7.
        gotaSalpica[i] = 0;
    }

    /** Calcula si es de noche y el factor de transición (0 día, 1 noche). */
    private void aplicarCicloDia() {
        noche = (tiempoDia < Constantes.HORA_AMANECER || tiempoDia >= Constantes.HORA_ATARDECER); // Noche entre las 18:00 y las 06:00.
        if (tiempoDia >= Constantes.HORA_NOCHE_CERRADA_INICIO || tiempoDia < Constantes.HORA_NOCHE_CERRADA_FIN) { // Noche cerrada.
            factorNoche = 1.0f;
        } else if (tiempoDia >= Constantes.HORA_INICIO_ATARDECER) { // Atardecer 17:00-19:00: sube de 0 a 1.
            factorNoche = (tiempoDia - Constantes.HORA_INICIO_ATARDECER) / Constantes.DURACION_TRANSICION;
        } else if (tiempoDia < Constantes.HORA_FIN_AMANECER) { // Amanecer 05:00-07:00: baja de 1 a 0.
            factorNoche = (Constantes.HORA_FIN_AMANECER - tiempoDia) / Constantes.DURACION_TRANSICION;
        } else { // Día pleno.
            factorNoche = 0.0f;
        }
    }

    // ==================== CIELO Y DIBUJO ====================

    /** Interpolación lineal entre a y b según t (0 a 1). */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Cielo: mezcla día/noche según la hora; con lluvia se vuelve gris y se apaga con la noche. */
    public float[] colorCielo() {
        float r = lerp(0.4f, 0.05f, factorNoche); // Día (0.4, 0.7, 1.0) a noche (0.05, 0.05, 0.1).
        float g = lerp(0.7f, 0.05f, factorNoche);
        float b = lerp(1.0f, 0.10f, factorNoche);
        if (estaLloviendo || intensidadTormenta > 0) { // Cielo cubierto: acerca el color a un gris que también oscurece de noche.
            float brillo = 1.0f - 0.8f * factorNoche;
            float cubierto = estaLloviendo ? 0.7f : 0.7f * intensidadTormenta; // La lluvia del surazo cubre el cielo poco a poco.
            r = lerp(r, 0.30f * brillo, cubierto);
            g = lerp(g, 0.34f * brillo, cubierto);
            b = lerp(b, 0.40f * brillo, cubierto);
        }
        if (intensidadTormenta > 0) { // Surazo: cielo de tormenta, oscuro aunque sea de día.
            float brillo = 1.0f - 0.8f * factorNoche;
            r = lerp(r, 0.11f * brillo, 0.85f * intensidadTormenta);
            g = lerp(g, 0.13f * brillo, 0.85f * intensidadTormenta);
            b = lerp(b, 0.17f * brillo, 0.85f * intensidadTormenta);
        }
        return new float[] {r, g, b};
    }

    /** Dibuja sol o luna, nubes y lluvia. El llamador activa antes uEmision para que los colores salgan directos. */
    public void dibujar(Renderizador r, float jugadorX, float jugadorZ, float tiempoAnimacion) {
        float anguloSol = (tiempoDia - 6.0f) / 12.0f * (float) Math.PI; // 6:00 = 0 (horizonte), 12:00 = π/2 (cenit), 18:00 = π.
        float alturaSol = (float) Math.sin(anguloSol) * 80.0f; // Semicircunferencia: máxima al mediodía.
        float horizontalSol = -(float) Math.cos(anguloSol) * 150.0f; // Recorre el cielo de lado a lado.
        float despejado = 1.0f - intensidadTormenta; // El sol y la luna se esconden tras las nubes de la tormenta.
        if (despejado < 0.02f) {
            // Tormenta completa: no se ve ni el sol ni la luna.
        } else if (alturaSol > 0) { // De día: sol amarillo.
            r.cajaGirada(jugadorX + horizontalSol, alturaSol, jugadorZ - 100.0f, 12.0f * despejado, 12.0f * despejado, 12.0f * despejado, 1.0f, 0.9f, 0.2f, tiempoAnimacion * 0.1f);
        } else { // De noche: luna blanca en la posición opuesta.
            r.cajaGirada(jugadorX - horizontalSol, -alturaSol, jugadorZ - 100.0f, 9.0f * despejado, 9.0f * despejado, 9.0f * despejado, 0.92f, 0.94f, 1.0f, tiempoAnimacion * 0.05f);
        }
        for (int i = 0; i < Constantes.MAX_NUBES; i++) { // Nubes blancas planas y anchas.
            float tonoNube = (1.0f - 0.7f * factorNoche) * (1.0f - 0.55f * intensidadTormenta); // Las nubes se oscurecen de noche y con la tormenta.
            r.caja(nubeX[i], 40.0f, nubeZ[i], 8.0f, 1.0f, 6.0f, tonoNube, tonoNube, tonoNube);
        }
        if (estaLloviendo || surazoActivo) { // Las gotas solo se dibujan durante la lluvia.
            for (int i = 0; i < Constantes.MAX_GOTAS; i++) { // Gotas finas (el shader no tiene transparencia) y salpicaduras en el suelo.
                if (gotaSalpica[i] > 0) { // Cuadrito plano que se ensancha al tocar el suelo.
                    float ancho = 0.2f + (Constantes.DURACION_SALPICADURA - gotaSalpica[i]) * 4.0f;
                    r.caja(gotaX[i], 0.35f, gotaZ[i], ancho, 0.04f, ancho, 0.75f, 0.85f, 1.0f);
                } else {
                    if (surazoActivo) { // Gota en diagonal: la estela queda inclinada hacia atrás del movimiento (-X) en el extremo de arriba.
                        float inclinacion = (float) Math.atan2(getFuerzaVientoX() * Constantes.FACTOR_LLUVIA_VIENTO, gotaVel[i]); // Velocidad lateral contra velocidad de caída.
                        float centroX = gotaX[i] - (float) Math.sin(inclinacion) * gotaLargo[i] / 2; // El extremo de abajo (la cabeza de la gota) queda en (gotaX, gotaY).
                        float centroY = gotaY[i] + (float) Math.cos(inclinacion) * gotaLargo[i] / 2;
                        r.cajaInclinada(centroX, centroY, gotaZ[i], 0.05f, gotaLargo[i], 0.05f, 0.70f, 0.80f, 0.95f, -(float) Math.PI / 2, inclinacion);
                    } else {
                        r.caja(gotaX[i], gotaY[i] + gotaLargo[i] / 2, gotaZ[i], 0.05f, gotaLargo[i], 0.05f, 0.70f, 0.80f, 0.95f);
                    }
                }
            }
        }
    }
}
