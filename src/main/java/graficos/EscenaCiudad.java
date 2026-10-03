package graficos; // Capa gráfica: escena de la ciudad.

import entidades.BotTrafico; // Modelo del auto civil estacionado en la gasolinera.
import entidades.Peaton; // Modelo de persona para policías, bomberos y pasajeros.
import entidades.Semaforo; // Posición y luz encendida del semáforo.
import static utilidades.MapaCiudad.MAPA; // Plano de la ciudad.
import static utilidades.MapaCiudad.centro; // Índice de celda a coordenada.

/**
 * Dibujo estático de la ciudad: asfalto, aceras, edificios y marcas de calle (base) y toda la decoración urbana
 * (ventanas, parques, fuente, comisaría, bomberos, paradas de bus, gasolinera, cruces con semáforos, mobiliario).
 * Lee el plano de MapaCiudad; los efectos animados reciben el estado del mundo antes de cada dibujo.
 */
public class EscenaCiudad {

    private final Renderizador r; // Dibuja las cajas de todos los elementos.
    private boolean noche; // Las ventanas y el agua brillan de noche (se actualiza en cada dibujo de decoración).
    private float tiempo; // Reloj de la partida: sirenas, chorro de la fuente y fases de los semáforos.
    private float tiempoAnimacion; // Reloj de animación: pasajeros de la parada y persona de la gasolinera.

    public EscenaCiudad(Renderizador renderizador) {
        this.r = renderizador;
    }

    // ==================== BASE DE LA CIUDAD ====================

    public void dibujarBase() {
        r.caja(0, -0.25f, 0, 194.0f, 0.5f, 194.0f, 0.18f, 0.18f, 0.19f);
        // Base de asfalto gris oscuro que cubre todo el mapa; cara superior en Y=0.
        for (int fila = 0; fila < MAPA.length; fila++) { // Recorre el mapa de norte a sur.
            for (int columna = 0; columna < MAPA[fila].length; columna++) { // Recorre cada fila de izquierda a derecha.
                float x = centro(columna); // Convierte la columna a posición X.
                float z = centro(fila); // Convierte la fila a posición Z.
                int tipo = MAPA[fila][columna]; // Lee si la celda es calle, edificio o parque.
                if (tipo == 0) { // Selecciona las celdas transitables.
                    dibujarMarcasCalle(fila, columna, x, z); // Añade líneas amarillas entre intersecciones.
                } else { // Las demás celdas representan manzanas completas.
                    boolean esBordeFila = (fila == 0 || fila == MAPA.length - 1); // Celda de la fila exterior (norte o sur).
                    boolean esBordeColumna = (columna == 0 || columna == MAPA[fila].length - 1); // Celda de la columna exterior (oeste o este).
                    if (esBordeFila && esBordeColumna) { // Esquina absoluta del mapa: solo un cuadrado de 2x2 pegado al anillo de calle.
                        float desvioX = (columna == 0) ? 4.0f : -4.0f; // Hacia el interior del mapa en X.
                        float desvioZ = (fila == 0) ? 4.0f : -4.0f; // Hacia el interior del mapa en Z.
                        r.caja(x + desvioX, 0.15f, z + desvioZ, 2, 0.3f, 2, 0.60f, 0.64f, 0.66f); // Acera de esquina de 2x2.
                    } else if (fila == 0) { // Borde superior: cordón de 2 unidades pegado a la calle.
                        r.caja(x, 0.15f, z + 4.0f, 10, 0.3f, 2, 0.60f, 0.64f, 0.66f);
                    } else if (fila == MAPA.length - 1) { // Borde inferior.
                        r.caja(x, 0.15f, z - 4.0f, 10, 0.3f, 2, 0.60f, 0.64f, 0.66f);
                    } else if (columna == 0) { // Borde izquierdo.
                        r.caja(x + 4.0f, 0.15f, z, 2, 0.3f, 10, 0.60f, 0.64f, 0.66f);
                    } else if (columna == MAPA[fila].length - 1) { // Borde derecho.
                        r.caja(x - 4.0f, 0.15f, z, 2, 0.3f, 10, 0.60f, 0.64f, 0.66f);
                    } else { // Acera interior: bloque completo de 10x10.
                        r.caja(x, 0.15f, z, 10, 0.3f, 10, 0.60f, 0.64f, 0.66f); // Dibuja la acera elevada sobre el asfalto.
                    }
                    if (tipo == 1 && !esBordeFila && !esBordeColumna) { // Selecciona una manzana ocupada por un edificio (nunca en el cordón exterior).
                        float altura = 5 + (fila * 3 + columna * 7) % 9; // Varía la altura de forma reproducible entre 5 y 13.
                        float rojo = 0.28f + columna * 0.045f; // Varía el tono rojo según la columna.
                        float azul = 0.48f + fila * 0.025f; // Varía el tono azul según la fila.
                        r.caja(x, altura / 2 + 0.3f, z, 7, altura, 7, rojo, 0.40f, azul); // Coloca la base del edificio sobre la acera.
                        r.caja(x, altura + 0.45f, z, 7.3f, 0.3f, 7.3f, 0.20f, 0.26f, 0.32f); // Añade una cubierta más ancha y oscura.
                    } else if (tipo == 2 || tipo == 3) { // Los tipos 2 (parque) y 3 (fuente) llevan césped; 4, 5 y 6 solo acera.
                        r.caja(x, 0.32f, z, 9, 0.1f, 9, 0.20f, 0.45f, 0.28f); // Cubre la parcela con césped verde.
                    }
                }
            }
        }
    }

    /** Dibuja las líneas discontinuas de las calles dejando los cruces despejados. */
    private void dibujarMarcasCalle(int fila, int columna, float x, float z) {
        if (fila % 3 == 1 && columna % 3 != 1) { // Tramo horizontal: fila de calle (1, 4, 7, 10, 13, 16, 19) entre dos cruces.
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) { // Coloca tres marcas en la celda.
                r.caja(x + desplazamiento, 0.025f, z, 1.6f, 0.03f, 0.13f, 1, 0.84f, 0.35f); // Dibuja una línea alargada en X.
            }
        }
        if (columna % 3 == 1 && fila % 3 != 1) { // Tramo vertical: columna de calle (1, 4, 7, 10, 13, 16, 19) entre dos cruces.
            for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento += 3) { // Repite las marcas sobre ese tramo.
                r.caja(x, 0.025f, z + desplazamiento, 0.13f, 0.03f, 1.6f, 1, 0.84f, 0.35f); // Dibuja una línea alargada en Z.
            }
        }
    }

    // ==================== DECORACIÓN DE LAS MANZANAS ====================

    /** Dibuja ventanas, parques, estaciones, cruces y mobiliario urbano con los relojes y la hora actuales. */
    public void dibujarDecoracion(boolean noche, float tiempo, float tiempoAnimacion, Semaforo semaforo) {
        this.noche = noche;
        this.tiempo = tiempo;
        this.tiempoAnimacion = tiempoAnimacion;
        decorarCiudad();
        dibujarSemaforo(semaforo);
    }

    /**
     * Semáforo independiente sobre la acera: poste delgado oscuro de 6 unidades, caja principal arriba y tres luces
     * (rojo arriba, amarillo al centro, verde abajo) en las caras que miran a +Z y +X. Solo la luz del estado
     * actual está encendida; las demás quedan en gris apagado.
     */
    private void dibujarSemaforo(Semaforo semaforo) {
        float x = semaforo.getX();
        float z = semaforo.getZ();
        r.caja(x, 3.3f, z, 0.2f, 6.0f, 0.2f, 0.10f, 0.10f, 0.12f); // Poste delgado oscuro (la acera está en Y = 0,3).
        r.caja(x, 5.6f, z, 0.7f, 2.0f, 0.7f, 0.08f, 0.09f, 0.10f); // Caja principal del semáforo.
        for (int cara = 0; cara < 2; cara++) { // Cara 0 mira a +Z (tráfico que viene del sur); cara 1 mira a +X (hacia la calle).
            float lx = (cara == 0) ? x : x + 0.40f; // Las luces sobresalen un poco de la caja.
            float lz = (cara == 0) ? z + 0.40f : z;
            float ancho = (cara == 0) ? 0.4f : 0.15f;
            float fondo = (cara == 0) ? 0.15f : 0.4f;
            for (int luz = 0; luz < 3; luz++) { // 0 = rojo (arriba), 1 = amarillo (centro), 2 = verde (abajo).
                Semaforo.Estado estadoDeLaLuz = (luz == 0) ? Semaforo.Estado.ROJO : (luz == 1) ? Semaforo.Estado.AMARILLO : Semaforo.Estado.VERDE; // Estado en el que esta luz se enciende.
                float altura = 6.2f - luz * 0.6f;
                boolean encendida = (semaforo.getEstado() == estadoDeLaLuz);
                if (encendida) {
                    r.entero("uEmision", 1); // Brilla aunque sea de noche.
                    if (estadoDeLaLuz == Semaforo.Estado.VERDE) r.caja(lx, altura, lz, ancho, 0.4f, fondo, 0.0f, 1.0f, 0.0f); // Verde.
                    else if (estadoDeLaLuz == Semaforo.Estado.AMARILLO) r.caja(lx, altura, lz, ancho, 0.4f, fondo, 1.0f, 0.9f, 0.0f); // Amarillo.
                    else r.caja(lx, altura, lz, ancho, 0.4f, fondo, 1.0f, 0.0f, 0.0f); // Rojo.
                    r.entero("uEmision", 0);
                } else {
                    r.caja(lx, altura, lz, ancho, 0.4f, fondo, 0.2f, 0.2f, 0.2f); // Luz apagada.
                }
            }
        }
    }

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
                    case 4 -> { // Comisaría con sirenas en el techo.
                        dibujarPolicia(x, z); // Dibuja el edificio.
                        Peaton.dibujarModelo(r, x + 3, z + 4.4f, (float) Math.PI, 0, 0.1f, 0.2f, 0.8f); // Policía de azul inmóvil frente a la entrada (tiempo 0: piernas quietas).
                        Peaton.dibujarModelo(r, x - 3, z + 4.4f, (float) Math.PI, 0, 0.05f, 0.12f, 0.55f); // Segundo policía, de azul más oscuro, también estático.
                    }
                    case 5 -> { // Estación roja con portones de garaje.
                        dibujarBomberos(x, z); // Dibuja el edificio.
                        Peaton.dibujarModelo(r, x + 4.2f, z + 4.4f, (float) Math.PI, 0, 0.8f, 0.1f, 0.1f); // Bombero de rojo inmóvil en la esquina de la acera frontal.
                        Peaton.dibujarModelo(r, x - 4.2f, z + 4.4f, (float) Math.PI, 0, 0.9f, 0.5f, 0.1f); // Segundo bombero con chaqueta naranja, también estático.
                    }
                    case 6 -> { // Parada de bus: se orienta hacia la calle contigua.
                        float anguloParada = 0; // Frente hacia -Z por defecto.
                        float desvioParadaX = 0; // Desplazamiento hacia el borde de la acera en X.
                        float desvioParadaZ = 0; // Desplazamiento hacia el borde de la acera en Z.
                        if (columna + 1 < MAPA[fila].length && MAPA[fila][columna + 1] == 0) { // Calle al este.
                            anguloParada = -(float) Math.PI / 2; desvioParadaX = 3.3f;
                        } else if (columna > 0 && MAPA[fila][columna - 1] == 0) { // Calle al oeste.
                            anguloParada = (float) Math.PI / 2; desvioParadaX = -3.3f;
                        } else if (fila + 1 < MAPA.length && MAPA[fila + 1][columna] == 0) { // Calle al sur.
                            anguloParada = (float) Math.PI; desvioParadaZ = 3.3f;
                        } else if (fila > 0 && MAPA[fila - 1][columna] == 0) { // Calle al norte.
                            desvioParadaZ = -3.3f;
                        }
                        dibujarParadaBus(x + desvioParadaX, z + desvioParadaZ, anguloParada);
                        float paradaX = x + desvioParadaX; // Centro de la parada en X.
                        float paradaZ = z + desvioParadaZ; // Centro de la parada en Z.
                        float senoParada = (float) Math.sin(anguloParada); // Seno de la orientación de la parada.
                        float cosenoParada = (float) Math.cos(anguloParada); // Coseno de la misma orientación.
                        for (int pasajero = 0; pasajero < 2; pasajero++) { // Dos pasajeros esperando bajo el techo.
                            float lateral = (pasajero == 0) ? -0.7f : 0.7f; // Offset a izquierda o derecha dentro de la parada.
                            float profundidad = -0.1f; // Un poco por delante del asiento, bajo el techo.
                            float pasajeroX = paradaX + cosenoParada * lateral + senoParada * profundidad; // Convierte el offset local a mundo (X).
                            float pasajeroZ = paradaZ - senoParada * lateral + cosenoParada * profundidad; // Convierte el offset local a mundo (Z).
                            float mirada = anguloParada + (float) Math.sin(tiempoAnimacion * 1.3f + pasajero * 2.0f) * 0.5f; // Mira hacia la calle y gira de lado a lado, desfasado entre ambos.
                            if (pasajero == 0) { // Primer pasajero: chaqueta verde.
                                Peaton.dibujarModelo(r, pasajeroX, pasajeroZ, mirada, 0, 0.30f, 0.60f, 0.40f, false); // Tiempo 0: piernas quietas.
                            } else { // Segundo pasajero: chaqueta naranja.
                                Peaton.dibujarModelo(r, pasajeroX, pasajeroZ, mirada, 0, 0.90f, 0.45f, 0.15f, false);
                            }
                        }
                    }
                    case 7 -> { // Techo flotante con surtidores (la gasolinera pasa del id 6 al 7).
                        dibujarGasolinera(x, z); // Dibuja la estructura de la gasolinera.
                        dibujarCargaCombustible(x, z); // Añade el auto estacionado y la persona que carga combustible.
                    }
                    case 0 -> { // Calle: puede llevar un montículo de basura junto a la acera.
                        boolean tramoHorizontal = fila % 3 == 1 && columna % 3 != 1; // Calle que corre a lo largo de X entre dos cruces.
                        boolean tramoVertical = columna % 3 == 1 && fila % 3 != 1; // Calle que corre a lo largo de Z entre dos cruces.
                        boolean hayBasura = (fila * 7 + columna * 13) % 23 == 5; // Selección fija (~7 % de los tramos) que no cambia entre cuadros.
                        if (hayBasura && tramoHorizontal) { // Basura en el borde sur de una calle horizontal.
                            dibujarBasura(x, z + 4);
                        } else if (hayBasura && tramoVertical) { // Basura en el borde este de una calle vertical.
                            dibujarBasura(x + 4, z);
                        }
                    }
                    default -> { } // Los demás tipos ya tienen su propio método de decoración.
                }
                if (tipo == 1 || tipo == 2) { // Solo las parcelas de edificio o parque llevan mobiliario urbano en su acera.
                    int lado = 0; // 0 = sin calle a los lados; 1 = calle al este; -1 = calle al oeste.
                    if (columna + 1 < MAPA[fila].length && MAPA[fila][columna + 1] == 0) { // Comprueba si hay calle al este.
                        lado = 1; // Usa el borde este de la acera.
                    } else if (columna > 0 && MAPA[fila][columna - 1] == 0) { // Si no, comprueba si hay calle al oeste.
                        lado = -1; // Usa el borde oeste de la acera.
                    }
                    int azar = (fila * 37 + columna * 53) % 100; // Número fijo por parcela (0-99): evita que cambie en cada cuadro como haría Math.random().
                    float desvioZ = 1.5f; // Desplazamiento a lo largo de la acera, lejos de las farolas y semáforos.
                    if (azar % 2 != 0) { // Alterna el lado según la parcela.
                        desvioZ = -1.5f; // Usa la mitad norte de la acera.
                    }
                    if (lado != 0 && azar < 15) { // Aproximadamente el 15 % de las parcelas con calle tienen hidrante.
                        dibujarHidrante(x + lado * 4.4f, z + desvioZ); // Lo pega al borde de la acera junto a la calle.
                    } else if (lado != 0 && azar < 30) { // Otro 15 % tiene buzón.
                        dibujarBuzon(x + lado * 4.4f, z + desvioZ); // Lo coloca en el mismo borde de la acera.
                    } else if (tipo == 1 && lado != 0 && azar % 10 == 3) { // Cerca del 10 % de los edificios restantes llevan una señal.
                        dibujarSenal(x + lado * 4.4f, z + desvioZ * 1.9f, (float) Math.PI / 2); // Panel orientado hacia la calle, lejos de farolas e hidrantes.
                    }
                }
                if (tipo == 0 && fila >= 7 && fila <= 13 && fila % 3 == 1 && columna >= 7 && columna <= 13 && columna % 3 == 1) { // Intersección interior: calle cuya fila y columna son 7, 10 o 13.
                    dibujarCruce(x, z); // Dibuja los 4 pasos de cebra y los 4 semáforos del cruce.
                }
            }
        }
    }

    /** Construye una fuente de piedra con agua azul y un surtidor animado (5 cajas). */
    private void dibujarFuente(float x, float z) {
        r.caja(x, 0.82f, z, 7, 0.9f, 7, 0.55f, 0.55f, 0.58f); // Base de piedra que forma el borde de la fuente.
        if (noche) { // De noche el agua brilla suavemente, como las ventanas.
            r.entero("uEmision", 1); // Evita que la iluminación oscurezca el agua.
        }
        r.caja(x, 1.3f, z, 5.8f, 0.1f, 5.8f, 0.15f, 0.45f, 0.85f); // Agua azul que sobresale un poco del borde.
        r.entero("uEmision", 0); // Restablece el material normal.
        r.caja(x, 2.0f, z, 1, 1.4f, 1, 0.50f, 0.50f, 0.53f); // Columna central de piedra.
        r.caja(x, 2.8f, z, 2.2f, 0.25f, 2.2f, 0.58f, 0.58f, 0.60f); // Taza superior de la fuente.
        float alturaChorro = 1.2f + (float) Math.sin(tiempo * 3) * 0.25f; // Hace subir y bajar el surtidor con el tiempo.
        r.entero("uEmision", 1); // El chorro se ve brillante de día y de noche.
        r.caja(x, 2.93f + alturaChorro / 2, z, 0.3f, alturaChorro, 0.3f, 0.45f, 0.75f, 1); // Surtidor de agua celeste.
        r.entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
    }

    /** Construye la comisaría: edificio bajo y ancho con pórtico, placa y sirenas que alternan. */
    private void dibujarPolicia(float x, float z) {
        r.caja(x, 2.0f, z, 8.4f, 3.4f, 7.4f, 0.28f, 0.30f, 0.38f); // Cuerpo bajo gris azulado (unos dos pisos) sobre la acera.
        r.caja(x, 3.85f, z, 8.8f, 0.3f, 7.8f, 0.16f, 0.18f, 0.25f); // Cubierta ligeramente más ancha y oscura.
        r.caja(x, 1.3f, z + 3.75f, 1.8f, 2, 0.12f, 0.08f, 0.12f, 0.25f); // Puerta de entrada azul oscuro en la fachada sur.
        r.caja(x, 2.65f, z + 4.3f, 3.2f, 0.2f, 1.2f, 0.10f, 0.15f, 0.40f); // Toldo o pórtico que sobresale sobre la entrada.
        r.caja(x, 3.15f, z + 3.76f, 1.2f, 0.6f, 0.08f, 0.95f, 0.80f, 0.15f); // Placa amarilla que simula el escudo en la fachada.
        r.caja(x - 2.8f, 1.9f, z + 3.72f, 1.4f, 1, 0.08f, 0.55f, 0.70f, 0.85f); // Ventana izquierda de la fachada.
        r.caja(x + 2.8f, 1.9f, z + 3.72f, 1.4f, 1, 0.08f, 0.55f, 0.70f, 0.85f); // Ventana derecha de la fachada.
        boolean rojoEncendido = (int) (tiempo * 4) % 2 == 0; // Alterna cuál sirena brilla cuatro veces por segundo.
        r.entero("uEmision", rojoEncendido ? 1 : 0); // La sirena roja brilla en las fases pares.
        r.caja(x - 1.2f, 4.3f, z, 1, 0.6f, 1, 0.9f, 0.1f, 0.1f); // Sirena roja sobre el techo.
        r.entero("uEmision", rojoEncendido ? 0 : 1); // La sirena azul brilla cuando la roja se apaga.
        r.caja(x + 1.2f, 4.3f, z, 1, 0.6f, 1, 0.1f, 0.25f, 0.95f); // Sirena azul sobre el techo.
        r.entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
    }

    /** Construye la estación de bomberos: edificio bajo y ancho con dos portones, techo saliente y torre. */
    private void dibujarBomberos(float x, float z) {
        r.caja(x, 2.1f, z, 8.8f, 3.6f, 7.4f, 0.80f, 0.12f, 0.10f); // Cuerpo bajo rojo llamativo (unos dos pisos).
        r.caja(x, 4.05f, z, 9.6f, 0.3f, 8.2f, 0.45f, 0.06f, 0.06f); // Techo más ancho que sobresale de las paredes.
        r.caja(x - 2.2f, 1.5f, z + 3.75f, 3.4f, 2.4f, 0.12f, 0.18f, 0.18f, 0.20f); // Primer portón gris oscuro del garaje.
        r.caja(x + 2.2f, 1.5f, z + 3.75f, 3.4f, 2.4f, 0.12f, 0.18f, 0.18f, 0.20f); // Segundo portón para otro camión.
        r.caja(x, 3.2f, z + 3.72f, 8, 0.35f, 0.06f, 0.90f, 0.90f, 0.88f); // Franja blanca sobre los portones.
        r.caja(x + 3.3f, 5.6f, z - 2.4f, 1.4f, 2.8f, 1.4f, 0.60f, 0.10f, 0.10f); // Torre pequeña de secado de mangueras.
        r.caja(x + 3.3f, 7.1f, z - 2.4f, 1.7f, 0.2f, 1.7f, 0.30f, 0.05f, 0.05f); // Remate oscuro de la torre.
    }

    /** Construye la gasolinera: techo flotante sobre dos columnas con dos surtidores (6 cajas). */
    private void dibujarGasolinera(float x, float z) {
        r.caja(x, 4.2f, z, 8, 0.35f, 6, 0.92f, 0.92f, 0.94f); // Techo plano flotante blanco.
        r.caja(x, 4.45f, z, 8.2f, 0.15f, 6.2f, 0.90f, 0.70f, 0.10f); // Banda amarilla sobre el borde del techo.
        r.caja(x - 3, 2.25f, z, 0.3f, 3.9f, 0.3f, 0.35f, 0.37f, 0.40f); // Columna fina izquierda.
        r.caja(x + 3, 2.25f, z, 0.3f, 3.9f, 0.3f, 0.35f, 0.37f, 0.40f); // Columna fina derecha.
        r.caja(x - 1.2f, 1.1f, z, 0.7f, 1.6f, 0.5f, 0.85f, 0.15f, 0.10f); // Surtidor rojo izquierdo.
        r.caja(x + 1.2f, 1.1f, z, 0.7f, 1.6f, 0.5f, 0.15f, 0.55f, 0.20f); // Surtidor verde derecho.
    }

    /** Construye cuatro árboles y un banco utilizando cajas. */
    private void dibujarParque(float x, float z) {
        float[] posiciones = {-2.5f, 2.5f}; // Define desplazamientos respecto al centro de la parcela.
        for (float desplazamientoX : posiciones) { // Selecciona el lado izquierdo o derecho del parque.
            for (float desplazamientoZ : posiciones) { // Selecciona el lado delantero o trasero.
                float arbolX = x + desplazamientoX; // Convierte el desplazamiento local en coordenada X mundial.
                float arbolZ = z + desplazamientoZ; // Convierte el desplazamiento local en coordenada Z mundial.
                r.caja(arbolX, 1.2f, arbolZ, 0.35f, 2, 0.35f, 0.38f, 0.22f, 0.12f); // Dibuja el tronco marrón.
                r.caja(arbolX, 2.7f, arbolZ, 2, 2.3f, 2, 0.12f, 0.42f, 0.23f); // Dibuja la copa verde del árbol.
            }
        }
        r.caja(x, 0.65f, z, 3, 0.25f, 0.8f, 0.55f, 0.30f, 0.13f); // Dibuja el asiento de madera del banco.
        r.caja(x, 0.4f, z, 2, 0.6f, 0.35f, 0.22f, 0.24f, 0.24f); // Dibuja el soporte oscuro del banco.
        r.caja(x, 1, z + 0.35f, 3, 0.7f, 0.15f, 0.55f, 0.30f, 0.13f); // Dibuja el respaldo detrás del asiento.
    }

    /** Distribuye ventanas por pisos en las cuatro paredes del edificio. */
    private void dibujarVentanas(float x, float z, float altura) {
        if (noche) { // Las ventanas simulan habitaciones encendidas en el ambiente nocturno.
            r.entero("uEmision", 1); // Permite ver el color de las ventanas sin depender de farolas.
        }
        for (float y = 1.7f; y < altura; y += 2) { // Recorre los pisos separados por dos unidades de altura.
            for (float desplazamiento = -2; desplazamiento <= 2; desplazamiento += 2) { // Coloca tres ventanas por fachada.
                r.caja(x + desplazamiento, y, z - 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada norte.
                r.caja(x + desplazamiento, y, z + 3.51f, 0.8f, 0.9f, 0.04f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada sur.
                r.caja(x - 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada oeste.
                r.caja(x + 3.51f, y, z + desplazamiento, 0.04f, 0.9f, 0.8f, 0.95f, 0.75f, 0.38f); // Ventana de la fachada este.
            }
        }
        r.entero("uEmision", 0); // Restablece la iluminación normal de los demás elementos.
    }

    /** Dibuja en una intersección 4 pasos de cebra (uno por entrada) y 4 semáforos en sus esquinas. */
    private void dibujarCruce(float x, float z) {
        for (int desplazamiento = -3; desplazamiento <= 3; desplazamiento++) { // Siete franjas por cada paso de cebra.
            r.caja(x + desplazamiento, 0.045f, z - 7.5f, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Entrada norte: se cruza a lo largo de X.
            r.caja(x + desplazamiento, 0.045f, z + 7.5f, 0.45f, 0.04f, 2, 0.85f, 0.87f, 0.83f); // Entrada sur: se cruza a lo largo de X.
            r.caja(x - 7.5f, 0.045f, z + desplazamiento, 2, 0.04f, 0.45f, 0.85f, 0.87f, 0.83f); // Entrada oeste: se cruza a lo largo de Z.
            r.caja(x + 7.5f, 0.045f, z + desplazamiento, 2, 0.04f, 0.45f, 0.85f, 0.87f, 0.83f); // Entrada este: se cruza a lo largo de Z.
        }

        int faseNorteSur = (int) (tiempo % 12); // Ciclo de 12 s para el tráfico que circula en dirección Z.
        int faseEsteOeste = (int) ((tiempo + 6) % 12); // El tráfico en X va desfasado medio ciclo respecto al de Z.
        for (int signoX = -1; signoX <= 1; signoX += 2) { // Elige la esquina oeste (-1) o este (+1).
            for (int signoZ = -1; signoZ <= 1; signoZ += 2) { // Elige la esquina norte (-1) o sur (+1).
                float postesX = x + signoX * 5.6f; // Coloca el poste sobre la acera de la manzana diagonal.
                float postesZ = z + signoZ * 5.6f; // Coloca el poste en la misma esquina, en profundidad.
                r.caja(postesX, 1.7f, postesZ, 0.18f, 2.8f, 0.18f, 0.18f, 0.20f, 0.22f); // Dibuja el poste sobre la acera.
                r.caja(postesX, 3.1f, postesZ, 0.5f, 1.2f, 0.5f, 0.08f, 0.10f, 0.12f); // Dibuja la carcasa con una cara hacia cada calle.
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
                        r.entero("uEmision", 0); // Configura inicialmente una superficie sin emisión.
                        if (encendida) { // Comprueba si esta bombilla corresponde a la fase activa.
                            brillo = 1; // Usa intensidad completa para su color.
                            r.entero("uEmision", 1); // Hace que la bombilla se vea encendida.
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
                            r.caja(postesX, altura, postesZ - signoZ * 0.27f, 0.28f, 0.25f, 0.06f, rojo, verde, 0.02f); // Bombilla en la cara norte o sur.
                        } else { // Cara que mira hacia el centro del cruce a lo largo de X.
                            r.caja(postesX - signoX * 0.27f, altura, postesZ, 0.06f, 0.25f, 0.28f, rojo, verde, 0.02f); // Bombilla en la cara oeste o este.
                        }
                    }
                }
            }
        }
        r.entero("uEmision", 0); // Evita que el siguiente objeto herede la emisión del semáforo.
    }

    /** Dibuja una señal de tránsito: poste gris delgado y panel verde plano en la parte superior (2 cajas). */
    private void dibujarSenal(float x, float z, float angulo) {
        r.cajaGirada(x, 1.5f, z, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f, angulo); // Poste vertical gris sobre la acera.
        r.cajaGirada(x - (float) Math.sin(angulo) * 0.1f, 2.45f, z - (float) Math.cos(angulo) * 0.1f, 1.2f, 0.6f, 0.06f, 0.10f, 0.55f, 0.25f, angulo); // Panel verde plano, un poco delante del poste.
    }

    /** Dibuja un hidrante rojo sobre la acera: cuerpo, tapa más ancha y válvula gris (3 cajas). */
    private void dibujarHidrante(float x, float z) {
        r.caja(x, 0.65f, z, 0.4f, 0.7f, 0.4f, 0.80f, 0.10f, 0.08f); // Cuerpo rojo (la acera está en Y=0,3).
        r.caja(x, 1.1f, z, 0.55f, 0.2f, 0.55f, 0.65f, 0.06f, 0.05f); // Tapa roja oscura, un poco más ancha.
        r.caja(x + 0.28f, 0.8f, z, 0.2f, 0.2f, 0.2f, 0.60f, 0.62f, 0.65f); // Válvula gris para la manguera, a un lado.
    }

    /** Dibuja el buzón azul clásico de correos de EE. UU.: patas, cuerpo, tapa escalonada, ranura y calcomanía blanca. */
    private void dibujarBuzon(float x, float z) {
        float[] lados = {-0.25f, 0.25f}; // Posiciones de las patas a cada lado del centro.
        for (float pataX : lados) { // Recorre las patas izquierda y derecha.
            for (float pataZ : lados) { // Recorre las patas delantera y trasera.
                r.caja(x + pataX, 0.4f, z + pataZ * 0.7f, 0.12f, 0.2f, 0.12f, 0.08f, 0.24f, 0.65f); // Pata azul corta en cada esquina de la base.
            }
        }
        r.caja(x, 1.0f, z, 0.7f, 1.0f, 0.55f, 0.10f, 0.30f, 0.80f); // Cuerpo principal azul, alto y robusto, sobre las patas.
        r.caja(x, 1.625f, z, 0.6f, 0.25f, 0.45f, 0.10f, 0.30f, 0.80f); // Tapa más angosta y menos profunda: efecto de techo curvo escalonado.
        r.caja(x, 1.35f, z + 0.28f, 0.4f, 0.05f, 0.04f, 0.10f, 0.10f, 0.12f); // Ranura gris oscura para las cartas en la parte frontal superior.
        r.caja(x + 0.355f, 1.0f, z, 0.02f, 0.3f, 0.3f, 0.95f, 0.95f, 0.95f); // Calcomanía blanca casi plana pegada al lateral (águila).
    }

    /** Dibuja un montículo de basura sobre el asfalto con una bolsa negra, una caja de cartón y una lata. */
    private void dibujarBasura(float x, float z) {
        r.caja(x, 0.3f, z, 0.8f, 0.6f, 0.7f, 0.05f, 0.05f, 0.06f); // Bolsa negra de basura.
        r.caja(x + 0.6f, 0.2f, z + 0.25f, 0.5f, 0.4f, 0.5f, 0.55f, 0.38f, 0.20f); // Caja de cartón marrón al lado.
        r.caja(x - 0.4f, 0.12f, z - 0.45f, 0.2f, 0.24f, 0.2f, 0.60f, 0.62f, 0.65f); // Lata gris tirada delante.
    }

    /** Dibuja una parada de bus sobre la acera: techo plano, dos postes, vidrio trasero y asiento (frente = lado de la calle). */
    private void dibujarParadaBus(float x, float z, float angulo) {
        r.piezaVehiculo(x, z, angulo, 0, 2.7f, 0, 3.6f, 0.15f, 1.8f, 0.30f, 0.34f, 0.40f); // Techo plano.
        r.piezaVehiculo(x, z, angulo, -1.7f, 1.5f, -0.75f, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f); // Poste delantero izquierdo (la acera está en Y = 0,3).
        r.piezaVehiculo(x, z, angulo, 1.7f, 1.5f, -0.75f, 0.12f, 2.4f, 0.12f, 0.55f, 0.57f, 0.60f); // Poste delantero derecho.
        r.piezaVehiculo(x, z, angulo, 0, 1.5f, 0.8f, 3.4f, 1.9f, 0.05f, 0.60f, 0.85f, 0.90f); // Vidrio trasero celeste.
        r.piezaVehiculo(x, z, angulo, 0, 0.85f, 0.45f, 2.4f, 0.1f, 0.4f, 0.55f, 0.35f, 0.18f); // Asiento de madera.
        r.piezaVehiculo(x, z, angulo, -1.0f, 0.55f, 0.45f, 0.1f, 0.5f, 0.3f, 0.40f, 0.42f, 0.45f); // Pata izquierda del asiento.
        r.piezaVehiculo(x, z, angulo, 1.0f, 0.55f, 0.45f, 0.1f, 0.5f, 0.3f, 0.40f, 0.42f, 0.45f); // Pata derecha del asiento.
    }

    /** Auto estacionado junto al surtidor izquierdo con una persona que carga combustible (manguera y pantalla parpadeante). */
    private void dibujarCargaCombustible(float x, float z) {
        BotTrafico.dibujarAutoCivil(r, x - 1.2f, z + 1.9f, (float) Math.PI / 2, 0.85f, 0.15f, 0.15f); // Auto rojo estacionado de lado, bajo el techo.
        r.caja(x - 1.2f, 0.8f, z + 0.66f, 0.06f, 0.06f, 0.83f, 0.05f, 0.05f, 0.05f); // Manguera negra del surtidor al costado del auto.
        boolean pantallaEncendida = (int) (tiempoAnimacion * 2) % 2 == 0; // Alterna el indicador del surtidor dos veces por segundo.
        r.entero("uEmision", pantallaEncendida ? 1 : 0); // La pantalla brilla en las fases pares y se apaga en las impares.
        r.caja(x - 1.2f, 1.5f, z + 0.27f, 0.4f, 0.25f, 0.04f, 0.2f, 1, 0.3f); // Pantalla verde del surtidor que parpadea mientras carga.
        r.entero("uEmision", 0); // Devuelve la iluminación normal a los siguientes objetos.
        float balanceo = (float) Math.sin(tiempoAnimacion * 1.5f) * 0.15f; // Pequeño giro del cuerpo mientras espera.
        Peaton.dibujarModelo(r, x - 0.3f, z + 0.75f, (float) Math.PI / 2 + balanceo, 0, 0.9f, 0.5f, 0.1f); // Persona de pie junto a la manguera, mirando al auto.
    }
}
