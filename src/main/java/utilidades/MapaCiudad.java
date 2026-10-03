package utilidades; // Herramientas globales.

/**
 * Plano de la ciudad: matriz de celdas, tamaño del mapa y conversión de índices a coordenadas.
 * Coordenadas: X = izquierda/derecha; Y = altura; Z = profundidad.
 */
public final class MapaCiudad {

    private MapaCiudad() { } // Clase de solo datos: no se instancia.

    public static final float LIMITE = 105.0f; // Distancia del origen a cada borde: el mapa mide 210 unidades (21 celdas x 10).
    public static final float CELDA = 10; // Ancho y profundidad de cada celda del mapa.

    // 0 = calle, 1 = edificio, 2 = parque, 3 = fuente, 4 = policía, 5 = bomberos, 6 = parada de bus, 7 = gasolinera, 8 = acera vacía.
    // Mapa de 21x21: calles en las filas/columnas 1, 4, 7, 10, 13, 16 y 19 (la 1 y la 19 forman el anillo perimetral); el centro es el índice 10.
    // La fuente está en (10, 10); la policía, los bomberos y la gasolinera están en (6, 5), (12, 8) y (15, 11); el resto son edificios y parques.
    public static final int[][] MAPA = {
        { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 0, 6, 1, 0, 1, 1, 0, 1, 1, 0, 2, 2, 0, 1, 1, 0, 1, 6, 0, 8 },
        { 8, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 2, 2, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 2, 2, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 6, 1, 0, 4, 1, 0, 1, 1, 0, 2, 2, 0, 1, 1, 0, 1, 6, 0, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 0, 1, 1, 0, 2, 2, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 1, 1, 0, 2, 2, 0, 5, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 0, 6, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 2, 2, 0, 1, 6, 0, 8 },
        { 8, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 7, 1, 0, 2, 2, 0, 1, 1, 0, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 8 },
        { 8, 0, 6, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 6, 0, 8 },
        { 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8 },
        { 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8 }
    };

    /** Convierte el índice de una fila o columna al centro de su celda. */
    public static float centro(int indice) {
        return -LIMITE + CELDA * (indice + 0.5f); // Parte del borde negativo y avanza hasta la mitad de la celda.
    }
}
