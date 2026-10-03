package sistemas; // Lógicas invisibles del juego: clima, IA, colisiones.

import entidades.BotTrafico; // Autos autónomos que este controlador gobierna.
import entidades.Semaforo; // Luz que los bots consultan para decidir si frenan.
import utilidades.Constantes; // Radios de radar, bloqueo y tablas de la flota.

/**
 * PASO 3B DE LA REFACTORIZACIÓN: cerebro del tráfico autónomo.
 * Crea la flota, decide con radares anti-choque si cada bot puede avanzar y resuelve el contacto jugador-bot.
 * No conoce las reglas de vidas: devuelve qué ocurrió y el juego decide las consecuencias.
 */
public class ControladorTrafico {

    /** Qué pasó cuando el jugador tocó (o no) a los bots en este cuadro. */
    public static class ResultadoColision {
        public boolean choqueFatal; // Un bot tocó al jugador sin gracia ni inmunidad: cuesta una vida.
        public boolean detenerJugador; // El jugador rozó a un bot: debe quedar con velocidad 0.
        public float jugadorX; // Posición X del jugador tras las correcciones (puede haber sido devuelta a la previa).
        public float jugadorZ; // Posición Z del jugador tras las correcciones.
    }

    private final BotTrafico[] bots = new BotTrafico[Constantes.MAX_BOTS]; // Solo los primeros botsActivos se procesan.
    private final Semaforo semaforo = new Semaforo(Constantes.SEMAFORO_X, Constantes.SEMAFORO_Z,
            Constantes.SEMAFORO_SENTIDO_X, Constantes.SEMAFORO_SENTIDO_Z); // El único semáforo de la ciudad.

    public ControladorTrafico() {
        for (int i = 0; i < Constantes.MAX_BOTS; i++) { // Crea los 15 bots con sus rutas, posiciones, velocidades y colores.
            bots[i] = new BotTrafico(i, Constantes.RUTAS_BOTS[i], Constantes.BOT_X_INICIAL[i], Constantes.BOT_Z_INICIAL[i],
                    Constantes.BOT_OBJETIVO_INICIAL[i], Constantes.BOT_VELOCIDAD[i], Constantes.BOT_COLORES[i]);
        }
    }

    public BotTrafico getBot(int indice) {
        return bots[indice];
    }

    /** La bocina del jugador asusta a los bots activos que estén a menos de RADIO_BOCINA unidades. */
    public void asustarCercanos(float jugadorX, float jugadorZ, int botsActivos) {
        float radioCuad = Constantes.RADIO_BOCINA * Constantes.RADIO_BOCINA;
        for (int i = 0; i < botsActivos; i++) {
            if (bots[i].distanciaCuadradaA(jugadorX, jugadorZ) < radioCuad) {
                bots[i].asustar();
            }
        }
    }

    /** El semáforo de la ciudad (estado y posición) para dibujarlo. */
    public Semaforo getSemaforo() { return semaforo; }

    /** Devuelve los 15 bots (activos y ocultos) a su estado original y el semáforo a verde. */
    public void reiniciar() {
        for (BotTrafico bot : bots) {
            bot.reiniciar();
        }
        semaforo.reiniciar();
    }

    /**
     * Mueve el tráfico autónomo. Cada bot frena ante el semáforo (decisión propia) o si el bus, la patrulla perseguidora o un bot de índice menor
     * están demasiado cerca; durante un choque ni siquiera consulta los radares.
     */
    public void actualizar(float deltaTime, int botsActivos, float velocidadGlobal,
                           float busX, float busZ, boolean patrullaActiva, float patrullaX, float patrullaZ) {
        semaforo.actualizar(deltaTime); // El semáforo avanza con el tiempo global de juego.
        for (int i = 0; i < botsActivos; i++) { // Recorre solo los autos autónomos activos.
            BotTrafico bot = bots[i];
            bot.actualizarSusto(deltaTime); // El susto de la bocina se va pasando.
            if (bot.actualizarChoque(deltaTime)) { // Mientras dura el choque no sigue su ruta.
                continue;
            }
            bot.evaluarSemaforo(semaforo); // El bot decide por sí mismo si debe frenar ante la luz.
            float dBusX = busX - bot.getX(); // Radar del bus.
            float dBusZ = busZ - bot.getZ();
            boolean puedeAvanzar = dBusX * dBusX + dBusZ * dBusZ >= Constantes.RADAR_FRENADO_CUAD; // Frena si el bus está a menos de 10 unidades.
            if (puedeAvanzar && patrullaActiva) { // Radar de la patrulla.
                float dPoliX = patrullaX - bot.getX();
                float dPoliZ = patrullaZ - bot.getZ();
                puedeAvanzar = dPoliX * dPoliX + dPoliZ * dPoliZ >= Constantes.RADAR_FRENADO_CUAD;
            }
            for (int j = 0; puedeAvanzar && j < i; j++) { // Anti-deadlock: solo cede el paso a bots de índice menor.
                if (bots[j].distanciaCuadradaA(bot.getX(), bot.getZ()) < Constantes.SEPARACION_BOTS_CUAD) {
                    puedeAvanzar = false;
                }
            }
            bot.setFrenando(!puedeAvanzar || bot.isFrenadoPorSemaforo()); // Luces de freno si el radar detecta un obstáculo o el semáforo lo detiene.
            if (!puedeAvanzar) { // Se queda quieto este cuadro.
                continue;
            }
            bot.avanzarPorRuta(deltaTime, velocidadGlobal);
        }
    }

    /**
     * Si el jugador toca a un bot, ambos se detienen y el bot espera un instante y retrocede.
     * Un contacto por debajo de radioChoqueCuad sin gracia es un choque fatal y corta la revisión (uno por cuadro).
     * Si el jugador se estaba acercando al bot, vuelve a su posición previa para no traspasarlo.
     */
    public ResultadoColision resolverChoqueJugador(int botsActivos, float jugadorX, float jugadorZ,
                                                   float previoX, float previoZ, float radioChoqueCuad, boolean puedeSerHerido) {
        ResultadoColision r = new ResultadoColision();
        r.jugadorX = jugadorX;
        r.jugadorZ = jugadorZ;
        float radioBloqueo = Constantes.RADIO_BLOQUEO_FISICO; // Radio de bloqueo físico (menor que el de choque: sqrt(5) ≈ 2,24).
        for (int i = 0; i < botsActivos; i++) { // Revisa solo los bots activos.
            BotTrafico bot = bots[i];
            float diferenciaX = r.jugadorX - bot.getX(); // Separación horizontal actual entre jugador y bot.
            float diferenciaZ = r.jugadorZ - bot.getZ(); // Separación en profundidad actual.
            float distanciaCuadrada = diferenciaX * diferenciaX + diferenciaZ * diferenciaZ; // Distancia al cuadrado, sin raíz.
            if (puedeSerHerido && distanciaCuadrada < radioChoqueCuad) { // Choque que cuesta una vida (distancia al cuadrado según el nivel).
                r.choqueFatal = true;
                return r; // Un solo choque por cuadro.
            }
            if (distanciaCuadrada < radioBloqueo * radioBloqueo) { // Comprueba si los autos se tocan.
                r.detenerJugador = true; // El auto del jugador se detiene.
                bot.iniciarChoque(); // Inicia el choque (0,4 s detenido y luego 1,1 s de retroceso) si no hay uno en curso.
                float previaX = previoX - bot.getX(); // Separación X que había antes de mover al jugador.
                float previaZ = previoZ - bot.getZ(); // Separación Z que había antes de mover al jugador.
                if (distanciaCuadrada < previaX * previaX + previaZ * previaZ) { // Solo bloquea si el jugador se estaba acercando.
                    r.jugadorX = previoX; // Devuelve al jugador a su posición anterior para que no traspase al bot.
                    r.jugadorZ = previoZ; // Misma corrección en profundidad.
                }
            }
        }
        return r;
    }
}
