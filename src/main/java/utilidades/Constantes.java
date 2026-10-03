package utilidades; // Paquete de utilidades compartidas por todo el proyecto.

/**
 * FASE 1: todos los valores fijos de la lógica del juego, agrupados por tema.
 * Cada constante conserva exactamente el valor que antes estaba escrito a mano en JuegoEntregas.
 * Las distancias que terminan en _CUAD están al cuadrado (se comparan sin calcular raíz).
 */
public final class Constantes {

    private Constantes() { } // Clase de solo constantes: no se instancia.

    // ==================== JUEGO ====================
    public static final String NOMBRE_JUEGO = "JuegoEntregas"; // Nombre que encabeza la barra de título.

    // ==================== MAPA Y CALLES ====================
    public static final float LIMITE_HUD = 107.0f; // Mitad del rango que el shader del minimapa convierte a pantalla (LIMITE 105 + 2).
    public static final float ESCALA_BASE_HUD = 67.0f; // Rango (±67) para el que se diseñó el panel de Game Over y victoria.
    public static final float LIMITE_CALLES = 90.0f; // Las calles van de -90 a 90.
    public static final float ESPACIADO_CALLES = 30.0f; // Distancia entre calles paralelas.
    public static final float SEMIANCHO_CALLE = 4.5f; // Ancho útil de una calle (±4,5) para decidir si algo está "en la calle".
    public static final float DESVIO_CARRIL = 2.5f; // Distancia lateral al eje de la calle: los bots circulan por su carril derecho.

    // ==================== VEHÍCULO DEL JUGADOR ====================
    public static final float VELOCIDAD_MAXIMA_NORMAL = 60 / 3.6f; // 60 km/h en unidades por segundo.
    public static final float ACELERACION_NORMAL = 14.0f; // Aceleración del motor en u/s².
    public static final float VELOCIDAD_PILDORA = 120 / 3.6f; // 120 km/h en unidades por segundo.
    public static final float ACELERACION_PILDORA = 30.0f; // Con 14 no llegaría a 120 km/h: el equilibrio es aceleración / 0,7.

    // ==================== AUTO DEL JUGADOR ====================
    public static final float RADIO_AUTO = 1.65f; // Radio que contiene al vehículo para las colisiones.
    public static final float JUGADOR_X_INICIAL = -30; // Posición horizontal inicial: centro de una calle.
    public static final float JUGADOR_Z_INICIAL = 30; // Posición inicial sobre el eje que recorre el fondo de la ciudad.
    public static final float REVERSA_MAXIMA = -6; // Velocidad máxima en reversa (u/s).
    public static final float RESISTENCIA_RODADO = 0.7f; // Pérdida de velocidad normal al rodar.
    public static final float RESISTENCIA_FRENO = 7.0f; // Pérdida de velocidad con el freno (Espacio) pulsado.
    public static final float FACTOR_GIRO = 0.11f; // Cuánto gira el auto por unidad de velocidad.

    // ==================== ENTREGAS ====================
    public static final float[][] DESTINOS = { // Cada fila contiene X y Z de una parada sobre la calle (las calles cruzan por X/Z en -30, 0 y 30).
        {-30.0f, -30.0f}, // Primera entrega: Sector Noroeste.
        {  0.0f,  30.0f}, // Segunda entrega: Sector Sur Centro.
        { 30.0f, -30.0f}  // Tercera entrega: Sector Noreste.
    };
    public static final float RADIO_LLEGADA_ENTREGA_CUAD = 3 * 3; // Radio de llegada de tres unidades.
    public static final float VELOCIDAD_PARADA_ENTREGA = 1.0f; // Debe circular a menos de una unidad por segundo para entregar.

    // ==================== PUNTOS DE APARICIÓN DE ITEMS ====================
    public static final float[][] PUNTOS_ITEMS = { // Posibles lugares de monedas y píldoras: todos sobre calles y lejos de las paradas de entrega.
        {0, -30}, {30, 0}, {-30, 0}, {60, -30}, {-60, 30}, {30, 60}, {-30, -60}, {60, 30}, {-60, -30}, {0, -60}, {90, -60}, {-90, 60}, {0, 90}, {0, -90}
    };

    // ==================== MONEDAS ====================
    public static final float RADIO_RECOGIDA_MONEDA_CUAD = 25.0f; // Radio de recolección de 5 unidades: cubre toda la marca de 6x6.
    public static final int MONEDAS_NIVEL_2 = 5; // Monedas que suben al nivel 2.
    public static final int MONEDAS_NIVEL_3 = 10; // Monedas que suben al nivel 3.
    public static final int MONEDAS_VICTORIA = 20; // Monedas necesarias para ganar.

    // ==================== NIVELES ====================
    public static final int NIVEL_INICIAL = 1;
    public static final int NIVEL_2 = 2;
    public static final int NIVEL_3 = 3;
    public static final int BOTS_NIVEL_1 = 5; // Bots activos por nivel.
    public static final int BOTS_NIVEL_2 = 10;
    public static final int BOTS_NIVEL_3 = 15;
    public static final float RADIO_COLISION_NIVEL_1 = 5.0f; // Umbral de choque que quita vida (distancia al cuadrado; 5.0 = radio de 2,24).
    public static final float RADIO_COLISION_NIVEL_2 = 7.0f;
    public static final float RADIO_COLISION_NIVEL_3 = 9.0f;
    public static final float VEL_BOTS_NIVEL_1 = 10.0f; // Velocidad global de los bots; 10 equivale a la velocidad base de cada bot.
    public static final float VEL_BOTS_NIVEL_2 = 15.0f;
    public static final float VEL_BOTS_NIVEL_3 = 20.0f;
    public static final float VEL_BOTS_BASE = 10.0f; // Divisor: la velocidad individual de cada bot se escala por velBots / 10.
    public static final float DURACION_MENSAJE_NIVEL = 3.0f; // Segundos del aviso de subida de nivel.

    // ==================== VIDAS Y CHOQUES ====================
    public static final int VIDAS_MAXIMAS = 3; // Choques permitidos antes del Game Over.
    public static final float GRACIA_REAPARICION = 2.0f; // Segundos sin perder vida tras reaparecer.
    public static final float DURACION_TEMBLOR = 0.3f; // Sacudida de cámara tras un choque.
    public static final float VELOCIDAD_IMPACTO_CHISPAS = 3.0f; // Velocidad mínima de impacto contra pared para generar chispas.

    // ==================== PÍLDORAS (POWER-UPS) ====================
    public static final float SPAWN_PILDORAS = 30.0f; // Segundos hasta la próxima aparición.
    public static final float VIDA_PILDORAS = 20.0f; // Segundos que permanecen en el mapa.
    public static final float DURACION_INMUNIDAD = 20.0f; // Segundos de invulnerabilidad y fantasma (píldora morada).
    public static final float DURACION_VELOCIDAD = 20.0f; // Segundos de nitro (píldora naranja).
    public static final float RADIO_RECOGIDA_PILDORA_CUAD = 9.0f; // Radio de recogida de 3 unidades.

    // ==================== PATRULLA PERSEGUIDORA ====================
    public static final float VELOCIDAD_POLI = 15.0f; // Menor que los 16,7 u/s (60 km/h) del jugador, así que puede escapar.
    public static final float DURACION_PERSECUCION = 30.0f; // Segundos que dura la persecución.
    public static final float DISTANCIA_APARICION_POLI = 40.0f; // Unidades por delante del jugador donde aparece.
    public static final float RADIO_NODO_POLI_CUAD = 0.25f; // Radio (al cuadrado) para considerar que ya llegó a un cruce.

    // ==================== BUS DEL ANILLO ====================
    public static final float VELOCIDAD_BUS = 15.0f; // Unidades por segundo.
    public static final float LIMITE_RUTA_BUS = 90.0f; // Eje del anillo de asfalto por el que da la vuelta el bus.
    public static final float DESVIO_BUS = 2.5f; // Desplazamiento hacia el carril derecho (hacia dentro del anillo).
    public static final float CARRIL_BUS = LIMITE_RUTA_BUS - DESVIO_BUS; // Límite real de la ruta: 87,5 unidades.
    public static final float SEMIANCHO_BUS = 1.5f; // Semiancho del bus (con ruedas) para colisiones.
    public static final float SEMILARGO_BUS = 4.5f; // Semilargo del bus para colisiones.

    // ==================== BOTS (TRÁFICO AUTÓNOMO) ====================
    public static final int MAX_BOTS = 15; // Tamaño de todos los arreglos de bots.
    public static final float RADAR_FRENADO_CUAD = 100.0f; // Un bot frena si el bus o la patrulla están a menos de 10 unidades.
    public static final float SEPARACION_BOTS_CUAD = 35.0f; // Distancia mínima entre bots (al cuadrado).
    public static final float RADIO_BLOQUEO_FISICO = 2.0f; // Radio de bloqueo físico jugador-bot (menor que el de choque).
    public static final float DURACION_CHOQUE_BOT = 1.5f; // 0,4 s detenido y luego 1,1 s de retroceso.
    public static final float UMBRAL_RETROCESO_BOT = 1.1f; // Bajo este valor el bot choqueado empieza a retroceder.
    public static final float VELOCIDAD_RETROCESO_BOT = 3.0f; // Unidades por segundo al retroceder.

    // ==================== DATOS DE LA FLOTA DE BOTS ====================
    public static final float[][][] RUTAS_BOTS = { // Cada ruta es un circuito cerrado de esquinas (X, Z) que siempre cae sobre calles.
        {{-30, -30}, {30, -30}, {30, 30}, {-30, 30}}, // Bot 0 (patrulla): cuadrado central por las calles -30 y 30.
        {{-60, -60}, {60, -60}, {60, 60}, {-60, 60}}, // Bot 1 (camión de bomberos): vuelta completa por las calles del borde.
        {{30, -60}, {30, -30}, {60, -30}, {60, -60}}, // Bot 2 (ambulancia): circuito pequeño en el sector noreste.
        {{-60, 0}, {-30, 0}, {-30, 30}, {-60, 30}}, // Bot 3 (civil): circuito en el sector oeste.
        {{30, 0}, {60, 0}, {60, 60}, {30, 60}}, // Bot 4 (civil): circuito en el sector sureste.
        {{-60, -30}, {-30, -30}, {-30, 0}, {-60, 0}}, // Bot 5 (civil).
        {{30, 30}, {60, 30}, {60, 60}, {30, 60}}, // Bot 6 (civil).
        {{-60, 30}, {-30, 30}, {-30, 60}, {-60, 60}}, // Bot 7 (civil).
        {{0, -60}, {30, -60}, {30, -30}, {0, -30}}, // Bot 8 (civil).
        {{-30, -60}, {0, -60}, {0, -30}, {-30, -30}}, // Bot 9 (civil).
        {{0, 0}, {30, 0}, {30, 30}, {0, 30}}, // Bot 10 (civil).
        {{-30, 0}, {0, 0}, {0, 30}, {-30, 30}}, // Bot 11 (civil).
        {{-60, -60}, {-30, -60}, {-30, -30}, {-60, -30}}, // Bot 12 (civil).
        {{0, 30}, {30, 30}, {30, 60}, {0, 60}}, // Bot 13 (civil).
        {{0, 60}, {-30, 60}, {-30, 30}, {0, 30}} // Bot 14 (civil): nace lejos del punto de inicio del jugador.
    };
    public static final float[] BOT_X_INICIAL = {-30, 60, 30, -60, 30, -60, 30, -60, 0, -30, 0, -30, -60, 0, 0}; // Posición X inicial de cada bot: justo en una esquina de su ruta.
    public static final float[] BOT_Z_INICIAL = {-30, 60, -60, 0, 0, -30, 30, 30, -60, -60, 0, 0, -60, 30, 60}; // Posición Z inicial de cada bot.
    public static final float[] BOT_VELOCIDAD = {7, 9, 6, 5, 8, 6, 7, 5, 8, 6, 7, 5, 9, 6, 8}; // Unidades por segundo de cada bot.
    public static final int[] BOT_OBJETIVO_INICIAL = {1, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}; // Índice de la esquina hacia la que avanza cada bot.
    public static final float[][] BOT_COLORES = {{0.95f, 0.80f, 0.15f}, {0.20f, 0.70f, 0.30f}, {0.60f, 0.30f, 0.85f}, {0.15f, 0.40f, 0.85f}, {0.90f, 0.90f, 0.92f},
        {0.85f, 0.25f, 0.20f}, {0.95f, 0.55f, 0.15f}, {0.30f, 0.75f, 0.80f}, {0.55f, 0.55f, 0.60f}, {0.80f, 0.30f, 0.60f},
        {0.25f, 0.30f, 0.65f}, {0.60f, 0.80f, 0.25f}, {0.40f, 0.25f, 0.20f}, {0.95f, 0.85f, 0.40f}, {0.20f, 0.20f, 0.25f}}; // Del bot 3 en adelante son civiles.

    // ==================== DATOS DE PEATONES Y MASCOTAS ====================
    public static final float[] PEATON_X_INICIAL = {-30, 30, 0, -30, -30, 30, 0, 0, 30, -30}; // Posición X inicial de cada peatón (en el centro de su recorrido).
    public static final float[] PEATON_Z = {-35.7f, -24.3f, 24.3f, 35.7f, -5.7f, 5.7f, -54.3f, 54.3f, -35.7f, 24.3f}; // Posición Z fija: acera pegada a una calle, a ambos lados de z = -30, 0 y 30 y en los bordes.
    public static final float[] PEATON_ANGULO_INICIAL = {-1.57f, 1.57f, -1.57f, 1.57f, -1.57f, 1.57f, -1.57f, 1.57f, -1.57f, 1.57f}; // -1,57 camina hacia +X y 1,57 hacia -X.
    public static final float[] PEATON_ORIGEN_X = {-30, 30, 0, -30, -30, 30, 0, 0, 30, -30}; // Centro del recorrido, sobre el eje de una calle vertical: van 14 unidades a cada lado, así que la cruzan.
    public static final float[][] PEATON_COLORES = {
        {0.20f, 0.65f, 0.30f}, {0.90f, 0.80f, 0.20f}, {0.50f, 0.50f, 0.52f}, {0.60f, 0.30f, 0.70f}, // Verde, amarillo, gris y morado.
        {0.90f, 0.45f, 0.15f}, {0.15f, 0.60f, 0.75f}, {0.85f, 0.35f, 0.55f}, {0.45f, 0.30f, 0.20f}, // Naranja, turquesa, rosa y café.
        {0.95f, 0.95f, 0.95f}, {0.30f, 0.30f, 0.70f} // Blanco y azul oscuro.
    };
    public static final int[][] PARCELAS_MASCOTAS = {{5, 11}, {12, 6}, {14, 15}, {11, 5}}; // Celdas de parque (tipo 2) de {fila, columna}: 2 perros y 2 gatos.
    public static final float[] ANIMAL_ANGULO_INICIAL = {0.5f, 2.2f, 4.0f, 5.5f}; // Orientación inicial distinta para cada animal.

    // ==================== PEATONES ====================
    public static final int TOTAL_PEATONES = 10;
    public static final float VELOCIDAD_PEATON = 1.0f; // Unidades por segundo.
    public static final float RADIO_ATROPELLO_CUAD = 9.0f; // Radio de colisión de 3 unidades.
    public static final float DURACION_APLASTADO = 3.0f; // Segundos aplastado antes de volver a la normalidad.
    public static final float INICIO_APLASTADO = 0.01f; // Valor inicial del temporizador al aplastarlo (debe ser > 0).
    public static final float RECORRIDO_PEATON = 14.0f; // Distancia máxima desde el centro de su recorrido.
    public static final float ESCALA_Y_APLASTADO = 0.1f; // Altura del peatón aplastado respecto a la normal.

    // ==================== MASCOTAS ====================
    public static final int TOTAL_ANIMALES = 4; // Índices 0-1 perros, 2-3 gatos.
    public static final int TOTAL_PERROS = 2;
    public static final float VELOCIDAD_PERRO = 1.2f;
    public static final float VELOCIDAD_GATO = 0.9f;
    public static final float LIMITE_PARCELA_MASCOTA = 3.0f; // Distancia máxima al centro del parque.

    // ==================== CLIMA ====================
    public static final int MAX_GOTAS = 220; // Gotas de lluvia.
    public static final int MAX_NUBES = 5; // Nubes que el viento mueve en X.
    public static final float CICLO_DESPEJADO = 60.0f; // Segundos despejado.
    public static final float CICLO_LLUVIA = 15.0f; // Segundos de lluvia.
    public static final float ALTURA_REINICIO_GOTA = 25.0f; // Altura mínima al renacer una gota.
    public static final float DISPERSION_GOTAS = 50.0f; // Lado del área de lluvia alrededor del jugador.
    public static final float ALTURA_MAX_GOTA = 40.0f; // Altura máxima de una gota al repartirlas en el aire.
    public static final float VEL_GOTA_MIN = 32.0f;
    public static final float VEL_GOTA_VARIACION = 18.0f; // Velocidad de caída de 32 a 50 u/s.
    public static final float LARGO_GOTA_MIN = 0.8f;
    public static final float LARGO_GOTA_VARIACION = 0.9f; // Largo de 0,8 a 1,7.
    public static final float DURACION_SALPICADURA = 0.2f; // Segundos de chapoteo al tocar el suelo.
    public static final float VELOCIDAD_NUBES = 5.0f; // Viento en X, u/s.
    public static final float LIMITE_NUBES = 100.0f; // Las nubes dan la vuelta al salir de ±100.
    public static final float RANGO_APARICION_NUBES = 200.0f; // Las nubes nacen repartidas en 200 unidades.

    // ==================== BULLET TIME ====================
    public static final float ESCALA_BULLET_TIME = 0.3f; // Con Espacio o Shift pulsado el tiempo del juego corre al 30 %.

    // ==================== SEMÁFORO ====================
    public static final float SEMAFORO_DURACION_VERDE = 10.0f; // Segundos en verde.
    public static final float SEMAFORO_DURACION_AMARILLO = 3.0f; // Segundos en amarillo.
    public static final float SEMAFORO_DURACION_ROJO = 10.0f; // Segundos en rojo.
    public static final float SEMAFORO_SENTIDO_X = 0.0f; // Sentido del tráfico que obedece el semáforo: el que viene del sur y va hacia -Z (la cara con luces mira a +Z).
    public static final float SEMAFORO_SENTIDO_Z = -1.0f;
    public static final float DISTANCIA_DETECCION_SEMAFORO = 25.0f; // Distancia euclidiana a la que un bot empieza a considerar el semáforo.
    public static final float DISTANCIA_PARE_SEMAFORO = 2.5f; // Distancia, medida a lo largo de su avance, a la que el bot deja el semáforo al detenerse (línea de pare).
    public static final float TOLERANCIA_LINEA_PARE = 1.0f; // Si el bot ya pasó la línea de pare por más de esto, se considera que cruzó y no frena.
    public static final float DECELERACION_SEMAFORO = 6.0f; // Frenado cómodo (u/s²) con el que el bot calcula cuánto antes debe empezar a frenar.
    public static final float FRENADO_MAXIMO_SEMAFORO = 30.0f; // Máxima pérdida de velocidad (u/s²) al frenar, para que no se detenga de golpe.
    public static final float ACELERACION_SEMAFORO = 10.0f; // Ganancia de velocidad (u/s²) al arrancar de nuevo con la luz verde.
    public static final float SEMAFORO_X = -35.6f; // Posición del semáforo: acera oeste de la calle donde empieza el jugador.
    public static final float SEMAFORO_Z = 21.0f;

    // ==================== HACES DE LOS FAROS ====================

    // ==================== VIENTO (SURAZO) ====================
    public static final float FUERZA_VIENTO_X = 25.0f; // Velocidad del viento hacia +X con el surazo activo (u/s).
    public static final float FACTOR_LLUVIA_VIENTO = 1.5f; // Las gotas se desplazan 1,5 veces la fuerza del viento: la lluvia viaja más rápido que el auto.
    public static final float RADIO_LLUVIA_VIENTO = 50.0f; // Una gota que se aleja más de 50 unidades a sotavento reaparece a barlovento.
    public static final float VELOCIDAD_MAXIMA_SURAZO = 40 / 3.6f; // Con el surazo el auto no pasa de 40 km/h.
    public static final float DESACELERACION_SURAZO = 10.0f; // El tope baja poco a poco (u/s²) hasta 40 km/h, sin frenazo.
    public static final float TRANSICION_TORMENTA = 1.0f; // Segundos que tarda el ambiente en oscurecerse (o aclararse) del todo.
    public static final float AMPLITUD_VIENTO_CAMARA = 0.14f; // Cuánto se agita la cámara con el viento a plena intensidad (unidades).

    // ==================== SOMBRAS BASE ====================
    public static final float ALTURA_SOMBRA = 0.068f; // Sobre la pintura de la calle (las cebras llegan a 0,065) y bajo las marcas de derrape (0,07).
    public static final float GROSOR_SOMBRA = 0.004f; // Caja casi plana.
    public static final float COLOR_SOMBRA = 0.04f; // Gris casi negro; se dibuja con iluminación, así que cambia de día y de noche.

    // ==================== BOCINA ====================
    public static final float RADIO_BOCINA = 15.0f; // Los bots a menos de esta distancia del jugador se asustan.
    public static final float VELOCIDAD_BOT_ASUSTADO = 30.0f; // Velocidad (u/s) de un bot asustado.
    public static final float DURACION_SUSTO = 3.0f; // Segundos que tarda un bot en calmarse del todo (la velocidad baja poco a poco).
    public static final float INTERVALO_ONDA_BOCINA = 0.5f; // Con la bocina mantenida sale una onda nueva cada 0,5 s.
    public static final float DURACION_ONDA_BOCINA = 0.5f; // Segundos que tarda la onda en crecer hasta RADIO_BOCINA y desaparecer.
    public static final int MAX_ONDAS_BOCINA = 4;
    public static final int SEGMENTOS_ONDA_BOCINA = 28; // Cajas que forman el anillo.

    // ==================== CÁMARA: CAMPO VISUAL ====================
    public static final float ALTURA_HELICOPTERO = 30.0f; // Altura de la cámara de helicóptero sobre el auto.
    public static final float FOV_NORMAL = 55.0f; // Campo visual vertical normal, en grados.
    public static final float FOV_ALTO = 70.0f; // A gran velocidad o con nitro se abre 15 grados para dar sensación de velocidad.
    public static final float VELOCIDAD_FOV_ALTO = 20.0f; // Por encima de esta velocidad (u/s) el campo visual se abre; solo se alcanza con nitro.
    public static final float VELOCIDAD_CAMBIO_FOV = 2.0f; // Rapidez con la que el campo visual alcanza su objetivo (1/s).

    // ==================== MARCAS DE DERRAPE ====================
    public static final float FRACCION_DERRAPE_GIRO = 0.6f; // Derrapa al girar a más del 60 % de la velocidad máxima.
    public static final float FRACCION_DERRAPE_FRENO = 0.4f; // O al frenar avanzando a más del 40 % de la velocidad máxima.
    public static final int MAX_HUELLAS = 600; // Marcas visibles como máximo (dos por cada tramo de derrape, una por rueda trasera).
    public static final float VIDA_HUELLA = 5.0f; // Segundos que dura una marca en el asfalto.
    public static final float DESVANECIMIENTO_HUELLA = 1.5f; // En los últimos segundos la marca se va estrechando hasta desaparecer.
    public static final float ESPACIADO_HUELLA = 0.45f; // Distancia recorrida entre un par de marcas y el siguiente.
    public static final float LARGO_HUELLA = 0.65f; // Largo de cada marca: un poco más que el espaciado para que se unan.
    public static final float ANCHO_HUELLA = 0.3f; // Ancho de cada marca (la rueda mide 0,24).
    public static final float ALTURA_HUELLA = 0.075f; // Por encima de la pintura de la calle (las cebras llegan a 0,065).
    public static final float COLOR_HUELLA = 0.07f; // Casi negro; se oscurece de noche porque se dibuja con iluminación.
    public static final float LADO_RUEDAS = 0.88f; // Distancia lateral de las ruedas al centro del auto.
    public static final float EJE_TRASERO = 0.82f; // Posición del eje trasero (hacia atrás, +Z local).

    // ==================== DAÑO PROGRESIVO ====================
    public static final int NIVEL_DANO_HUMO_BLANCO = 2; // Desde este nivel el motor echa humo blanco.
    public static final int NIVEL_DANO_HUMO_NEGRO = 3; // Desde este nivel el humo es negro.
    public static final int NIVEL_DANO_CAPO = 3; // Desde este nivel el capó se levanta.
    public static final int NIVEL_DANO_EXPLOSION = 4; // Al llegar a este nivel el auto explota y se pierde una vida.
    public static final float ANGULO_CAPO_LEVANTADO = 30.0f; // Grados que sube el borde delantero del capó.

    // ==================== EXPLOSIÓN ====================
    public static final int MAX_EXPLOSION = 40; // Partículas de fuego y escombros por explosión.
    public static final float FRENADO_ESCOMBROS = 6.3f; // Pérdida de velocidad de los escombros en el suelo (1/s); equivale a conservar el 90 % por cuadro a 60 FPS.
    public static final float VIDA_EXPLOSION_MIN = 0.6f; // Vida mínima de cada partícula (s).
    public static final float VIDA_EXPLOSION_VARIACION = 0.5f; // Vida extra aleatoria (s).
    public static final float VELOCIDAD_EXPLOSION = 12.0f; // Velocidad máxima de salida (u/s).
    public static final float GRAVEDAD_EXPLOSION = 15.0f; // Los escombros caen más despacio que las chispas.
    public static final float TAMANO_EXPLOSION = 0.9f; // Lado máximo de cada cubo.

    // ==================== HUMO POR DAÑO ====================
    public static final int MAX_HUMO = 15; // Partículas de humo simultáneas como máximo.
    public static final float INTERVALO_HUMO = 0.1f; // Una partícula cada 0,1 s mientras el auto está dañado.
    public static final float VIDA_HUMO = 1.0f; // Segundos de vida de cada partícula.
    public static final float ALTURA_CAPO = 1.5f; // Altura sobre el auto donde nace el humo.
    public static final float DISTANCIA_CAPO = 0.9f; // Distancia hacia delante del centro del auto (donde está el motor).
    public static final float DISPERSION_HUMO = 0.6f; // Dispersión lateral inicial (±0,3 en X y Z).
    public static final float VELOCIDAD_ASCENSO_HUMO = 2.0f; // El humo sube 2 unidades por segundo.
    public static final float ESCALA_MAX_HUMO = 1.5f; // Tamaño de la caja de humo al disiparse.

    // ==================== PARTÍCULAS ====================
    public static final int MAX_CHISPAS = 20;
    public static final int MAX_RASTRO = 12; // Partículas del rastro de nitro.
    public static final float GRAVEDAD_CHISPAS = 40.0f;
    public static final float DISPERSION_CHISPAS = 15.0f; // Velocidad horizontal ±7,5 y vertical hasta 15.
    public static final float IMPULSO_VERTICAL_CHISPAS = 5.0f; // Mínimo hacia arriba.
    public static final float ALTURA_INICIAL_CHISPAS = 1.0f;
    public static final float DISTANCIA_FRENTE_CHISPAS = 1.3f; // Nacen en el parachoques delantero.
    public static final float VIDA_CHISPA_MIN = 0.4f;
    public static final float VIDA_CHISPA_VARIACION = 0.3f;
    public static final float VIDA_RASTRO = 0.5f;
    public static final float INTERVALO_RASTRO = 0.1f; // Una partícula cada 0,1 s.
    public static final float DISTANCIA_RASTRO = 2.5f; // Distancia detrás del auto.

    // ==================== CICLO DE DÍA Y NOCHE ====================
    public static final float HORAS_POR_DIA = 24.0f;
    public static final float DURACION_DIA_REAL = 120.0f; // 24 horas de juego = 120 s reales.
    public static final float HORA_INICIAL = 8.0f; // Empieza a las 8 AM.
    public static final float HORA_NOCHE_TECLA = 21.0f; // La tecla N salta a las 21:00.
    public static final float HORA_AMANECER = 6.0f; // Noche entre las 18:00 y las 06:00.
    public static final float HORA_ATARDECER = 18.0f;
    public static final float HORA_NOCHE_CERRADA_INICIO = 19.0f; // Factor de noche = 1 desde las 19:00...
    public static final float HORA_NOCHE_CERRADA_FIN = 5.0f; // ...hasta las 05:00.
    public static final float HORA_INICIO_ATARDECER = 17.0f; // Transición 17:00-19:00.
    public static final float HORA_FIN_AMANECER = 7.0f; // Transición 05:00-07:00.
    public static final float DURACION_TRANSICION = 2.0f; // Horas que dura cada transición.
}
