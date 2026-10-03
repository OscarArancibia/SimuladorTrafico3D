package graficos; // Capa gráfica: efectos visuales de partículas.

import utilidades.Constantes; // Tamaños, vidas y físicas de las partículas.

/**
 * PASO 2 DE LA REFACTORIZACIÓN: chispas de colisión y rastro de nitro (solo visuales).
 * La posición del vehículo se recibe por parámetro; la clase no conoce al jugador.
 */
public class GestorParticulas {

    private final float[] chispaX = new float[Constantes.MAX_CHISPAS];
    private final float[] chispaY = new float[Constantes.MAX_CHISPAS];
    private final float[] chispaZ = new float[Constantes.MAX_CHISPAS];
    private final float[] chispaVX = new float[Constantes.MAX_CHISPAS];
    private final float[] chispaVY = new float[Constantes.MAX_CHISPAS];
    private final float[] chispaVZ = new float[Constantes.MAX_CHISPAS];
    private final float[] chispaVida = new float[Constantes.MAX_CHISPAS];
    private final float[] rastroX = new float[Constantes.MAX_RASTRO];
    private final float[] rastroZ = new float[Constantes.MAX_RASTRO];
    private final float[] rastroVida = new float[Constantes.MAX_RASTRO];
    private int indiceRastro = 0;
    private float timerSpawnRastro = 0;
    private final float[] humoX = new float[Constantes.MAX_HUMO]; // Humo negro del motor dañado.
    private final float[] humoY = new float[Constantes.MAX_HUMO];
    private final float[] humoZ = new float[Constantes.MAX_HUMO];
    private final float[] humoVida = new float[Constantes.MAX_HUMO]; // 1 al nacer, 0 al disiparse.
    private final int[] humoTipo = new int[Constantes.MAX_HUMO]; // 1 = humo blanco, 2 = humo negro.
    private final float[] expX = new float[Constantes.MAX_EXPLOSION]; // Fuego y escombros de la explosión.
    private final float[] expY = new float[Constantes.MAX_EXPLOSION];
    private final float[] expZ = new float[Constantes.MAX_EXPLOSION];
    private final float[] expVX = new float[Constantes.MAX_EXPLOSION];
    private final float[] expVY = new float[Constantes.MAX_EXPLOSION];
    private final float[] expVZ = new float[Constantes.MAX_EXPLOSION];
    private final float[] expVida = new float[Constantes.MAX_EXPLOSION]; // Segundos restantes.
    private final float[] expVidaTotal = new float[Constantes.MAX_EXPLOSION]; // Vida con la que nació, para calcular el color y el tamaño.
    private int indiceHumo = 0;
    private float timerSpawnHumo = 0;
    private final float[] huellaX = new float[Constantes.MAX_HUELLAS]; // Marcas de derrape en el asfalto (una por rueda trasera).
    private final float[] huellaZ = new float[Constantes.MAX_HUELLAS];
    private final float[] huellaAngulo = new float[Constantes.MAX_HUELLAS]; // Orientación del auto al dejar la marca.
    private final float[] huellaVida = new float[Constantes.MAX_HUELLAS]; // Segundos restantes.
    private int indiceHuella = 0;
    private final float[] ondaX = new float[Constantes.MAX_ONDAS_BOCINA]; // Ondas de la bocina: anillos que crecen en el suelo.
    private final float[] ondaZ = new float[Constantes.MAX_ONDAS_BOCINA];
    private final float[] ondaVida = new float[Constantes.MAX_ONDAS_BOCINA]; // Segundos restantes.
    private int indiceOnda = 0;
    private boolean huellaAnclada = false; // true mientras dura un derrape continuo.
    private float anclaX; // Último punto donde se dejó un par de marcas.
    private float anclaZ;

    /** Lanza todas las chispas desde el parachoques delantero del vehículo en (x, z) orientado según angulo. */
    public void generarChispas(float x, float z, float angulo) {
        float frenteX = -(float) Math.sin(angulo); // Las chispas nacen en el parachoques delantero.
        float frenteZ = -(float) Math.cos(angulo);
        for (int i = 0; i < Constantes.MAX_CHISPAS; i++) {
            chispaX[i] = x + frenteX * Constantes.DISTANCIA_FRENTE_CHISPAS;
            chispaY[i] = Constantes.ALTURA_INICIAL_CHISPAS;
            chispaZ[i] = z + frenteZ * Constantes.DISTANCIA_FRENTE_CHISPAS;
            chispaVX[i] = (float) (Math.random() - 0.5) * Constantes.DISPERSION_CHISPAS;
            chispaVY[i] = (float) (Math.random() * Constantes.DISPERSION_CHISPAS) + Constantes.IMPULSO_VERTICAL_CHISPAS; // Siempre hacia arriba.
            chispaVZ[i] = (float) (Math.random() - 0.5) * Constantes.DISPERSION_CHISPAS;
            chispaVida[i] = Constantes.VIDA_CHISPA_MIN + (float) Math.random() * Constantes.VIDA_CHISPA_VARIACION;
        }
    }

    /** Borra todas las partículas activas. */
    public void reiniciar() {
        java.util.Arrays.fill(chispaVida, 0); // Borra las chispas.
        java.util.Arrays.fill(rastroVida, 0); // Borra el rastro de nitro.
        java.util.Arrays.fill(humoVida, 0); // Borra el humo del motor dañado.
        java.util.Arrays.fill(expVida, 0); // Borra la explosión.
        java.util.Arrays.fill(huellaVida, 0); // Borra las marcas de derrape.
        java.util.Arrays.fill(ondaVida, 0); // Borra las ondas de la bocina.
        huellaAnclada = false;
    }

    /** Mueve las chispas, desvanece el rastro y, con el nitro activo, emite partículas detrás del vehículo. */
    public void actualizar(float deltaTime, boolean nitroActivo, float x, float z, float angulo) {
        for (int i = 0; i < Constantes.MAX_CHISPAS; i++) { // Física de las chispas: movimiento y gravedad.
            if (chispaVida[i] > 0) {
                chispaVida[i] -= deltaTime;
                chispaX[i] += chispaVX[i] * deltaTime;
                chispaY[i] += chispaVY[i] * deltaTime;
                chispaZ[i] += chispaVZ[i] * deltaTime;
                chispaVY[i] -= Constantes.GRAVEDAD_CHISPAS * deltaTime;
                if (chispaY[i] < 0) chispaY[i] = 0;
            }
        }
        for (int i = 0; i < Constantes.MAX_EXPLOSION; i++) { // Física de la explosión: movimiento y gravedad.
            if (expVida[i] > 0) {
                expVida[i] -= deltaTime;
                expX[i] += expVX[i] * deltaTime;
                expY[i] += expVY[i] * deltaTime;
                expZ[i] += expVZ[i] * deltaTime;
                expVY[i] -= Constantes.GRAVEDAD_EXPLOSION * deltaTime;
                if (expY[i] < 0.2f) { // Los escombros se frenan al tocar el suelo.
                    float conservado = (float) Math.exp(-Constantes.FRENADO_ESCOMBROS * deltaTime); // Fracción de velocidad que se conserva: no depende de los cuadros por segundo.
                    expY[i] = 0.2f;
                    expVY[i] = 0;
                    expVX[i] *= conservado;
                    expVZ[i] *= conservado;
                }
            }
        }
        for (int i = 0; i < Constantes.MAX_ONDAS_BOCINA; i++) { // Las ondas de la bocina se van apagando.
            if (ondaVida[i] > 0) ondaVida[i] -= deltaTime;
        }
        for (int i = 0; i < Constantes.MAX_RASTRO; i++) { // Las partículas existentes se desvanecen siempre.
            if (rastroVida[i] > 0) rastroVida[i] -= deltaTime;
        }
        if (nitroActivo) { // Nitro activo: emite una partícula cada 0,1 s detrás del auto.
            timerSpawnRastro -= deltaTime;
            if (timerSpawnRastro <= 0) {
                rastroX[indiceRastro] = x + (float) Math.sin(angulo) * Constantes.DISTANCIA_RASTRO + (float) (Math.random() - 0.5); // ±0,5 de dispersión lateral. La parte trasera es opuesta al frente (-sen, -cos).
                rastroZ[indiceRastro] = z + (float) Math.cos(angulo) * Constantes.DISTANCIA_RASTRO + (float) (Math.random() - 0.5);
                rastroVida[indiceRastro] = Constantes.VIDA_RASTRO;
                indiceRastro = (indiceRastro + 1) % Constantes.MAX_RASTRO;
                timerSpawnRastro = Constantes.INTERVALO_RASTRO; // Una partícula cada 0,1 s.
            }
        }
    }

    /**
     * Marcas de derrape: mientras el auto derrapa deja un par de marcas (una por rueda trasera) cada ESPACIADO_HUELLA
     * unidades recorridas, así la densidad no depende de los cuadros por segundo ni del Bullet Time. El auto está en
     * (autoX, autoZ) con orientación autoAngulo en radianes (el frente es -sen, -cos, por eso el eje trasero queda en +Z local).
     */
    public void actualizarHuellas(float deltaTime, boolean estaDerrapando, float autoX, float autoZ, float autoAngulo) {
        if (estaDerrapando) {
            if (!huellaAnclada) { // Empieza un derrape: primer par de marcas en el punto actual.
                huellaAnclada = true;
                anclaX = autoX;
                anclaZ = autoZ;
                agregarParDeHuellas(autoX, autoZ, autoAngulo);
            } else {
                float dx = autoX - anclaX;
                float dz = autoZ - anclaZ;
                float distancia = (float) Math.sqrt(dx * dx + dz * dz);
                if (distancia > 4.0f) { // El auto se teletransportó (reaparición): no dibujar una raya por el mapa.
                    anclaX = autoX;
                    anclaZ = autoZ;
                } else if (distancia >= Constantes.ESPACIADO_HUELLA) {
                    int pares = (int) (distancia / Constantes.ESPACIADO_HUELLA); // Si se avanzó mucho en un cuadro, rellena el tramo.
                    for (int k = 1; k <= pares; k++) {
                        agregarParDeHuellas(anclaX + dx / distancia * Constantes.ESPACIADO_HUELLA * k,
                                anclaZ + dz / distancia * Constantes.ESPACIADO_HUELLA * k, autoAngulo);
                    }
                    anclaX += dx / distancia * Constantes.ESPACIADO_HUELLA * pares;
                    anclaZ += dz / distancia * Constantes.ESPACIADO_HUELLA * pares;
                }
            }
        } else {
            huellaAnclada = false; // El derrape terminó: el próximo empieza con un par nuevo.
        }
        for (int i = 0; i < Constantes.MAX_HUELLAS; i++) { // Las marcas existentes se desvanecen siempre.
            if (huellaVida[i] > 0) {
                huellaVida[i] -= deltaTime;
            }
        }
    }

    /** Deja una marca bajo cada rueda trasera de un auto centrado en (x, z) con orientación angulo. */
    private void agregarParDeHuellas(float x, float z, float angulo) {
        float coseno = (float) Math.cos(angulo);
        float seno = (float) Math.sin(angulo);
        for (int lado = -1; lado <= 1; lado += 2) { // Rueda izquierda y derecha.
            float lx = lado * Constantes.LADO_RUEDAS; // Posición local de la rueda (mismo cambio de ejes que AutoJugador.pieza).
            huellaX[indiceHuella] = x + coseno * lx + seno * Constantes.EJE_TRASERO;
            huellaZ[indiceHuella] = z - seno * lx + coseno * Constantes.EJE_TRASERO;
            huellaAngulo[indiceHuella] = angulo;
            huellaVida[indiceHuella] = Constantes.VIDA_HUELLA;
            indiceHuella = (indiceHuella + 1) % Constantes.MAX_HUELLAS;
        }
    }

    /**
     * Dibuja las marcas de derrape: cajas casi planas, oscuras, apenas sobre la pintura de la calle, que se estrechan
     * al final de su vida. Va aparte de dibujar(): se pinta con la iluminación normal (de noche se oscurecen) y no en el minimapa.
     */
    public void dibujarHuellas(Renderizador r) {
        for (int i = 0; i < Constantes.MAX_HUELLAS; i++) {
            if (huellaVida[i] > 0) {
                float desvanecimiento = Math.min(1.0f, huellaVida[i] / Constantes.DESVANECIMIENTO_HUELLA); // 1 hasta los últimos 1,5 s, luego 0.
                r.cajaGirada(huellaX[i], Constantes.ALTURA_HUELLA, huellaZ[i], Constantes.ANCHO_HUELLA * desvanecimiento, 0.01f, Constantes.LARGO_HUELLA,
                        Constantes.COLOR_HUELLA, Constantes.COLOR_HUELLA, Constantes.COLOR_HUELLA, huellaAngulo[i]);
            }
        }
    }

    /** Onda de la bocina centrada en (x, z): un anillo plano que crece hasta RADIO_BOCINA en 0,5 s y desaparece. */
    public void generarOndaBocina(float x, float z) {
        ondaX[indiceOnda] = x;
        ondaZ[indiceOnda] = z;
        ondaVida[indiceOnda] = Constantes.DURACION_ONDA_BOCINA;
        indiceOnda = (indiceOnda + 1) % Constantes.MAX_ONDAS_BOCINA;
    }

    /** Dibuja las ondas de la bocina: anillos de cajas planas que se agrandan y se afinan hasta desaparecer. */
    private void renderizarOndasBocina(Renderizador r) {
        for (int i = 0; i < Constantes.MAX_ONDAS_BOCINA; i++) {
            if (ondaVida[i] > 0) {
                float avance = 1.0f - ondaVida[i] / Constantes.DURACION_ONDA_BOCINA; // 0 al nacer, 1 al desaparecer.
                float radio = avance * Constantes.RADIO_BOCINA;
                float largo = Math.max(0.5f, 2.0f * (float) Math.PI * radio / Constantes.SEGMENTOS_ONDA_BOCINA * 1.2f); // Un poco más que el tramo para que se unan.
                float grosor = 0.45f * (1.0f - avance); // Se afina al alejarse.
                for (int k = 0; k < Constantes.SEGMENTOS_ONDA_BOCINA; k++) {
                    float fase = 2.0f * (float) Math.PI * k / Constantes.SEGMENTOS_ONDA_BOCINA;
                    r.cajaGirada(ondaX[i] + (float) Math.cos(fase) * radio, 0.3f, ondaZ[i] + (float) Math.sin(fase) * radio,
                            grosor, 0.08f, largo, 1.0f, 0.95f, 0.6f, -fase); // Cada caja se alinea con la tangente del círculo.
                }
            }
        }
    }

    /** Explosión del auto en (x, y, z): lanza fuego y escombros en todas las direcciones, con más fuerza hacia arriba. */
    public void generarExplosion(float x, float y, float z) {
        for (int i = 0; i < Constantes.MAX_EXPLOSION; i++) {
            expX[i] = x + (float) (Math.random() - 0.5) * 0.6f;
            expY[i] = y;
            expZ[i] = z + (float) (Math.random() - 0.5) * 0.6f;
            expVX[i] = (float) (Math.random() - 0.5) * 2.0f * Constantes.VELOCIDAD_EXPLOSION;
            expVY[i] = (0.4f + (float) Math.random() * 0.6f) * Constantes.VELOCIDAD_EXPLOSION; // Siempre hacia arriba.
            expVZ[i] = (float) (Math.random() - 0.5) * 2.0f * Constantes.VELOCIDAD_EXPLOSION;
            expVidaTotal[i] = Constantes.VIDA_EXPLOSION_MIN + (float) Math.random() * Constantes.VIDA_EXPLOSION_VARIACION;
            expVida[i] = expVidaTotal[i];
        }
    }

    /**
     * Si el auto tiene daño emite una partícula de humo en el capó cada 0,1 s con una ligera dispersión (tipoHumo:
     * 0 = ninguno, 1 = blanco, 2 = negro, según el SistemaDano); las partículas vivas pierden vida y suben lentamente.
     * El auto está en (autoX, autoY, autoZ) mirando según angulo.
     */
    public void actualizarHumo(float deltaTime, float autoX, float autoY, float autoZ, float angulo, int tipoHumo) {
        if (tipoHumo > 0) {
            timerSpawnHumo -= deltaTime;
            if (timerSpawnHumo <= 0) {
                float frenteX = -(float) Math.sin(angulo); // El motor está en el frente del auto (-sen, -cos).
                float frenteZ = -(float) Math.cos(angulo);
                humoX[indiceHumo] = autoX + frenteX * Constantes.DISTANCIA_CAPO + (float) (Math.random() - 0.5) * Constantes.DISPERSION_HUMO;
                humoY[indiceHumo] = autoY + Constantes.ALTURA_CAPO;
                humoZ[indiceHumo] = autoZ + frenteZ * Constantes.DISTANCIA_CAPO + (float) (Math.random() - 0.5) * Constantes.DISPERSION_HUMO;
                humoVida[indiceHumo] = Constantes.VIDA_HUMO;
                humoTipo[indiceHumo] = tipoHumo;
                indiceHumo = (indiceHumo + 1) % Constantes.MAX_HUMO;
                timerSpawnHumo = Constantes.INTERVALO_HUMO;
            }
        }
        for (int i = 0; i < Constantes.MAX_HUMO; i++) { // Las partículas existentes se disipan siempre y suben.
            if (humoVida[i] > 0) {
                humoVida[i] -= deltaTime;
                humoY[i] += Constantes.VELOCIDAD_ASCENSO_HUMO * deltaTime;
            }
        }
    }

    /** Dibuja el humo: cajas negras que se expanden a medida que se disipan. */
    private void renderizarHumo(Renderizador r, float tiempoAnimacion) {
        for (int i = 0; i < Constantes.MAX_HUMO; i++) {
            if (humoVida[i] > 0) {
                float escala = (1.0f - humoVida[i]) * Constantes.ESCALA_MAX_HUMO; // Crece al disiparse.
                float disipado = 1.0f - humoVida[i]; // 0 al nacer, 1 al disiparse.
                float gris = (humoTipo[i] == 1) ? 0.95f - 0.35f * disipado // Humo blanco: casi blanco al nacer, gris claro al disiparse.
                        : 0.05f + 0.15f * disipado; // Humo negro: negro al nacer, gris oscuro al disiparse.
                r.cajaGirada(humoX[i], humoY[i], humoZ[i], escala, escala, escala, gris, gris, gris, tiempoAnimacion * 2.0f + i);
            }
        }
    }

    /** Dibuja la explosión: cubos de fuego que pasan de amarillo a naranja y a gris oscuro mientras se encogen. */
    private void renderizarExplosion(Renderizador r, float tiempoAnimacion) {
        for (int i = 0; i < Constantes.MAX_EXPLOSION; i++) {
            if (expVida[i] > 0) {
                float t = expVida[i] / expVidaTotal[i]; // 1 al nacer, 0 al apagarse.
                float tamano = Constantes.TAMANO_EXPLOSION * (0.3f + 0.7f * t);
                float rojo, verde, azul;
                if (t > 0.6f) { rojo = 1.0f; verde = 0.9f - 0.45f * (1.0f - t) / 0.4f; azul = 0.2f - 0.15f * (1.0f - t) / 0.4f; } // Amarillo a naranja.
                else if (t > 0.25f) { float k = (0.6f - t) / 0.35f; rojo = 1.0f - 0.8f * k; verde = 0.45f - 0.35f * k; azul = 0.05f + 0.07f * k; } // Naranja a gris oscuro.
                else { rojo = 0.2f; verde = 0.1f; azul = 0.12f; } // Escombro oscuro.
                r.cajaGirada(expX[i], expY[i], expZ[i], tamano, tamano, tamano, rojo, verde, azul, tiempoAnimacion * 6.0f + i);
            }
        }
    }

    /** Dibuja chispas, rastro, humo y explosión. El llamador activa antes uEmision para que los colores salgan directos. */
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        renderizarExplosion(r, tiempoAnimacion);
        renderizarOndasBocina(r);
        renderizarHumo(r, tiempoAnimacion);
        for (int i = 0; i < Constantes.MAX_CHISPAS; i++) { // Chispas: cubitos amarillos y blancos que se achican al morir.
            if (chispaVida[i] > 0) {
                float tamano = chispaVida[i] * 0.4f;
                float colorG = (i % 2 == 0) ? 1.0f : 0.8f; // Alterna blanco puro y amarillo.
                float colorB = (i % 2 == 0) ? 1.0f : 0.0f;
                r.cajaGirada(chispaX[i], chispaY[i], chispaZ[i], tamano, tamano, tamano, 1.0f, colorG, colorB, tiempoAnimacion * (10.0f + i));
            }
        }
        for (int i = 0; i < Constantes.MAX_RASTRO; i++) { // Rastro de nitro: cajas naranjas que se encogen con su vida.
            if (rastroVida[i] > 0) {
                float tamano = rastroVida[i] * 0.8f; // Empiezan pequeñas (0,4) y desaparecen.
                float sube = (0.5f - rastroVida[i]) * 1.5f; // El humo asciende un poco con la edad.
                r.cajaGirada(rastroX[i], 0.5f + sube, rastroZ[i], tamano, tamano, tamano, 1.0f, 0.3f + rastroVida[i], 0.05f, rastroX[i] * 3.0f); // Naranja que se oscurece; giro distinto por partícula.
            }
        }
    }
}
