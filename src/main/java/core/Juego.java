package core; // Motor base.

import entidades.AutoJugador; // Auto que conduce el jugador.
import entidades.BotTrafico; // Autos autónomos (luces).
import entidades.BusUrbano; // Bus del anillo perimetral.
import entidades.Item; // Monedas y píldoras.
import entidades.Mascota; // Perros y gatos de los parques.
import entidades.PatrullaPolicia; // Patrulla perseguidora.
import entidades.Vehiculo; // Medidas de la sombra de cada vehículo.
import entidades.Peaton; // Personas de las aceras.
import graficos.EscenaCiudad; // Ciudad y decoración.
import graficos.GestorParticulas; // Chispas y rastro de nitro.
import graficos.Iluminacion; // Farolas y faros.
import graficos.Renderizador; // Cajas y shaders.
import sistemas.ControladorTrafico; // Radares y choques del tráfico.
import sistemas.GestorColisiones; // Contacto con el bus.
import sistemas.GestorTrucos; // Códigos secretos.
import sistemas.SistemaBocina; // Bocina del jugador.
import sistemas.SistemaDano; // Daño progresivo del auto.
import sistemas.SistemaClima; // Hora del día, lluvia y nubes.
import ui.MenuManager; // Estados inicio, pausa, Game Over y victoria.
import ui.PantallaHUD; // Minimapa, velocímetro, vidas y monedas.
import utilidades.Constantes; // Valores fijos del juego.
import utilidades.GuardadoDatos; // Récord de mejor tiempo.
import static org.lwjgl.glfw.GLFW.*; // Teclas.
import static org.lwjgl.opengl.GL33.*; // Viewport y buffers del cuadro principal.

/**
 * Orquestador de la partida: reúne los subsistemas, aplica las reglas (niveles, vidas, monedas, píldoras, entregas)
 * y decide el orden de actualización y de dibujo. No contiene geometría ni física: eso vive en cada entidad y sistema.
 */
public final class Juego implements Input.OyenteTecla, PantallaHUD.Escena {

    // ==================== SUBSISTEMAS ====================
    private final Ventana ventana;
    private final Renderizador renderizador = new Renderizador(); // Cajas, shaders y uniforms.
    private final Input input; // Teclado.
    private final CamaraManager camara = new CamaraManager(); // Tercera persona, helicóptero, capó y vista frontal.
    private final Iluminacion iluminacion = new Iluminacion(); // Farolas, faros del jugador y de los bots.
    private final EscenaCiudad ciudad = new EscenaCiudad(renderizador); // Asfalto, edificios y decoración.
    private final SistemaClima clima = new SistemaClima(); // Hora del día, ciclo despejado/lluvia, gotas y nubes.
    private final GestorParticulas particulas = new GestorParticulas(); // Chispas de colisión y rastro de nitro.
    private final SistemaDano sistemaDano = new SistemaDano(particulas, this::perderVida); // Daño progresivo: al explotar resta una vida.
    private final MenuManager menu = new MenuManager(); // Estados de la partida.
    private final PantallaHUD hud = new PantallaHUD(renderizador); // Minimapa y paneles.
    private final GuardadoDatos guardado = GuardadoDatos.porDefecto(); // Récord de mejor tiempo.
    private final GestorTrucos trucos = new GestorTrucos(); // Códigos secretos escritos con el teclado.

    // ==================== ENTIDADES ====================
    private final AutoJugador jugador = new AutoJugador();
    private final BusUrbano bus = new BusUrbano();
    private final PatrullaPolicia patrulla = new PatrullaPolicia(); // Aparece al atropellar a un peatón.
    private final ControladorTrafico trafico = new ControladorTrafico(); // Flota de 15 bots, radares y choque con el jugador.
    private final SistemaBocina bocina = new SistemaBocina(particulas, trafico); // Tecla H: asusta a los bots cercanos y lanza una onda.
    private final Item moneda = new Item(Item.Tipo.MONEDA, Constantes.PUNTOS_ITEMS[0][0], Constantes.PUNTOS_ITEMS[0][1]);
    private final Item pildoraAzul = new Item(Item.Tipo.PILDORA_INMUNIDAD, 30.0f, -30.0f); // Se reubica al aparecer.
    private final Item pildoraNaranja = new Item(Item.Tipo.PILDORA_VELOCIDAD, 0.0f, -30.0f); // Se reubica al aparecer.
    private final Item pildoraVerde = new Item(Item.Tipo.PILDORA_REPARACION, 60.0f, -30.0f); // Repara el auto; se reubica al aparecer.
    private final Peaton[] peatones = new Peaton[Constantes.TOTAL_PEATONES];
    private final Mascota[] mascotas = new Mascota[Constantes.TOTAL_ANIMALES];

    // ==================== ESTADO DE LAS REGLAS ====================
    private boolean mostrarMapa = true; // Muestra el minimapa desde el inicio.
    private int entregas = 0; // Cuenta las entregas completadas; también identifica el siguiente destino.
    private float tiempo = 0; // Segundos de la partida hasta completar el recorrido (semáforos, fuente y sirenas).
    private float tiempoPartida = 0; // Segundos jugados desde el inicio o el último reinicio, para el récord.
    private float tiempoAnimacion = 0; // Reloj de las mascotas y de las animaciones de dibujo.
    private int puntoMoneda = 0; // Índice (en Constantes.PUNTOS_ITEMS) del lugar donde está la moneda ahora.
    private int monedasRecolectadas = 0; // Cuenta las monedas que ha recogido el jugador.
    private boolean pildorasVisibles = false; // Las tres píldoras aparecen y desaparecen juntas.
    private float timerSpawnPildoras = Constantes.SPAWN_PILDORAS; // Segundos hasta la próxima aparición.
    private float timerVidaPildoras = 0.0f; // Segundos que le quedan a las píldoras visibles antes de desaparecer.
    private float timerFantasma = 0.0f; // Segundos restantes en que el jugador traspasa a los bots (píldora morada).
    private float timerVelocidad = 0.0f; // Segundos restantes del efecto de velocidad.
    private int nivel = Constantes.NIVEL_INICIAL; // Nivel actual (1 a 3).
    private int botsActivos = Constantes.BOTS_NIVEL_1; // Cantidad de bots que se mueven, dibujan y pueden chocar.
    private float radioColision = Constantes.RADIO_COLISION_NIVEL_1; // Umbral de choque que quita vida, en distancia al cuadrado.
    private float velBots = Constantes.VEL_BOTS_NIVEL_1; // Velocidad global de los bots; 10 equivale a la velocidad base de cada bot.
    private float timerMensaje = 0.0f; // Segundos restantes del aviso de subida de nivel.
    private int choques = 0; // Choques acumulados en la partida.
    private float invulnerable = 0; // Segundos restantes de gracia tras reaparecer, para no perder dos vidas seguidas.
    private float escalaTiempo = 1.0f; // Velocidad del tiempo del juego: 1 normal, 0,3 en Bullet Time.

    /** Crea el juego: requiere que la ventana ya tenga un contexto OpenGL activo. */
    public Juego(Ventana ventana) {
        this.ventana = ventana;
        renderizador.iniciar(Renderizador.cargarShader("/shaders/ciudad.vert"), Renderizador.cargarShader("/shaders/ciudad.frag"));
        input = new Input(ventana.getHandle(), this);
        for (int i = 0; i < Constantes.TOTAL_PEATONES; i++) { // Los diez peatones con su recorrido y ropa de colores distintos.
            peatones[i] = new Peaton(Constantes.PEATON_X_INICIAL[i], Constantes.PEATON_Z[i], Constantes.PEATON_ANGULO_INICIAL[i],
                    Constantes.PEATON_ORIGEN_X[i], Constantes.PEATON_COLORES[i]);
        }
        for (int i = 0; i < Constantes.TOTAL_ANIMALES; i++) { // Cada animal en el centro de su parque.
            Mascota.Tipo tipo = (i < Constantes.TOTAL_PERROS) ? Mascota.Tipo.PERRO : Mascota.Tipo.GATO;
            mascotas[i] = new Mascota(tipo, Constantes.PARCELAS_MASCOTAS[i][0], Constantes.PARCELAS_MASCOTAS[i][1],
                    Constantes.ANIMAL_ANGULO_INICIAL[i], i % 2 == 0 ? -1.8f : 1.8f);
        }
        aplicarCicloDia(); // Fija la iluminación inicial según la hora (8 AM: día, faros apagados).
        clima.iniciar(jugador.getX(), jugador.getZ()); // Lluvia inicial alrededor del jugador y nubes repartidas por el mapa.
    }

    /** Libera los recursos de la GPU. */
    public void liberar() {
        renderizador.liberar();
    }

    // ==================== TECLADO Y REINICIO ====================

    /** Atiende las acciones que deben ocurrir una sola vez por pulsación. */
    @Override
    public void alPresionar(int tecla) {
        if (tecla == GLFW_KEY_ESCAPE) { // ESC solicita terminar el ciclo principal.
            ventana.pedirCierre();
        }
        if (tecla == GLFW_KEY_C) { // Cicla las cámaras: tercera persona, helicóptero, capó y vista frontal.
            camara.ciclarModo();
        }
        if (tecla == GLFW_KEY_E) { // Alterna primera y tercera persona.
            camara.alternarPrimeraPersona();
        }
        if (tecla == GLFW_KEY_R) { // Comienza de nuevo.
            reiniciar();
        }
        if (tecla == GLFW_KEY_F) { // Interruptor de los focos del auto.
            iluminacion.alternarFaros();
        }
        if (tecla == GLFW_KEY_N) { // N salta a una hora diurna (8:00) o nocturna (21:00) para que el ciclo no anule el cambio.
            clima.setHora(clima.isNoche() ? Constantes.HORA_INICIAL : Constantes.HORA_NOCHE_TECLA);
            aplicarCicloDia();
        }
        if (tecla == GLFW_KEY_U) { // Surazo: tormenta de viento que oscurece el día, trae lluvia en diagonal y limita la velocidad.
            clima.alternarSurazo();
            System.out.println(clima.isSurazoActivo() ? "SURAZO ACTIVADO" : "SURAZO DESACTIVADO");
        }
        if (tecla == GLFW_KEY_M) { // Alterna entre mostrar y ocultar el minimapa.
            mostrarMapa = !mostrarMapa;
        }
        if (tecla == GLFW_KEY_SPACE || tecla == GLFW_KEY_ENTER) { // Espacio o Enter: iniciar o pausar.
            if (menu.alternarInicioPausa()) { // El título solo se fija al pausar porque actualizar() está detenido.
                ventana.setTitulo(MenuManager.TITULO_PAUSA);
            }
        }
    }

    /** Letras escritas en cualquier momento (también en el inicio y en pausa): FICCT, TANK y POLICIA; escribir de nuevo el código lo desactiva. */
    @Override
    public void alEscribirCaracter(char letra) {
        GestorTrucos.Truco truco = trucos.escribir(letra);
        if (truco == null) {
            return;
        }
        switch (truco) {
            case MODO_DIOS: {
                boolean activar = !jugador.isModoDios();
                jugador.setModoDios(activar);
                if (!activar && !GestorColisiones.puedeCircular(jugador.getX(), jugador.getZ())) { // Si estaba dentro de un edificio quedaría atrapado: vuelve al inicio.
                    jugador.reaparecer();
                    jugador.detener();
                }
                System.out.println(activar ? "TRUCO ACTIVADO: Modo FICCT (Dios)" : "TRUCO DESACTIVADO: Modo FICCT (Dios)");
                break;
            }
            case TANQUE:
                alternarModelo(AutoJugador.Modelo.TANQUE, "Tanque");
                break;
            case POLICIA:
                alternarModelo(AutoJugador.Modelo.POLI, "Policia");
                break;
        }
    }

    /** Cambia al modelo indicado, o vuelve al auto normal si ya era ese. */
    private void alternarModelo(AutoJugador.Modelo modelo, String nombre) {
        boolean activar = jugador.getModelo() != modelo;
        jugador.setModelo(activar ? modelo : AutoJugador.Modelo.NORMAL);
        System.out.println(activar ? "TRUCO ACTIVADO: " + nombre : "TRUCO DESACTIVADO: " + nombre);
    }

    /** true si el jugador atraviesa a los demás vehículos: con la píldora morada o con el modo dios. */
    private boolean atraviesaVehiculos() {
        return timerFantasma > 0 || jugador.isModoDios();
    }

    /** Reinicia el vehículo y el progreso de la partida. */
    private void reiniciar() {
        jugador.reiniciar(); // Restaura posición, velocidad, orientación y valores normales del motor.
        entregas = 0; // Vuelve a seleccionar la primera parada.
        tiempo = 0; // Reinicia el cronómetro de la partida.
        tiempoPartida = 0; // Reinicia el cronómetro del récord.
        monedasRecolectadas = 0; // Vuelve a contar las monedas desde cero.
        nivel = Constantes.NIVEL_INICIAL; botsActivos = Constantes.BOTS_NIVEL_1; radioColision = Constantes.RADIO_COLISION_NIVEL_1; velBots = Constantes.VEL_BOTS_NIVEL_1; timerMensaje = 0; // Vuelve al nivel 1.
        choques = 0; // Restaura las tres vidas.
        menu.reiniciar(); // Sale de la pausa, del Game Over o de la victoria.
        pildorasVisibles = false; timerSpawnPildoras = Constantes.SPAWN_PILDORAS; timerVelocidad = 0; timerFantasma = 0; // Cancela los power-ups.
        particulas.reiniciar(); // Borra las chispas y el rastro de nitro.
        sistemaDano.reiniciar(); // El auto vuelve a estar intacto.
        clima.reiniciar(); aplicarCicloDia(); // El clima vuelve a empezar despejado y el día a las 8 AM.
        patrulla.reiniciar(); // Retira la patrulla perseguidora.
        invulnerable = Constantes.GRACIA_REAPARICION; // Dos segundos de gracia por si un bot queda cerca del punto de inicio.
        trafico.reiniciar(); // Devuelve los 15 bots (activos y ocultos) a su estado original.
        for (Peaton peaton : peatones) { // Los peatones aplastados vuelven a la normalidad.
            peaton.restaurar();
        }
        bus.reiniciar(); // Devuelve el bus a la esquina superior izquierda del anillo.
    }

    /** Copia la noche y el factor de transición del clima a la iluminación; los faros siguen al ciclo. */
    private void aplicarCicloDia() {
        iluminacion.sincronizarConClima(clima.isNoche(), clima.getFactorNoche());
    }

    // ==================== ITEMS ====================

    /** Coloca las tres píldoras en puntos de calle distintos entre sí y de la moneda. */
    private void reubicarPildoras() {
        int a, b;
        do { a = (int) (Math.random() * Constantes.PUNTOS_ITEMS.length); } while (a == puntoMoneda);
        do { b = (int) (Math.random() * Constantes.PUNTOS_ITEMS.length); } while (b == puntoMoneda || b == a);
        pildoraAzul.colocarEn(Constantes.PUNTOS_ITEMS[a][0], Constantes.PUNTOS_ITEMS[a][1]);
        pildoraNaranja.colocarEn(Constantes.PUNTOS_ITEMS[b][0], Constantes.PUNTOS_ITEMS[b][1]);
        int c = (b + 1) % Constantes.PUNTOS_ITEMS.length; // La verde ocupa el siguiente punto libre (sin sorteo extra).
        while (c == puntoMoneda || c == a || c == b) {
            c = (c + 1) % Constantes.PUNTOS_ITEMS.length;
        }
        pildoraVerde.colocarEn(Constantes.PUNTOS_ITEMS[c][0], Constantes.PUNTOS_ITEMS[c][1]);
    }

    /** Mueve la moneda a un punto distinto de la lista, siempre sobre una calle. */
    private void reubicarMoneda() {
        int nuevoPunto = puntoMoneda; // Parte del punto actual para asegurarse de cambiarlo.
        while (nuevoPunto == puntoMoneda) { // Repite hasta obtener un punto diferente.
            nuevoPunto = (int) (Math.random() * Constantes.PUNTOS_ITEMS.length); // Elige un índice al azar.
        }
        puntoMoneda = nuevoPunto; // Guarda el nuevo punto.
        moneda.colocarEn(Constantes.PUNTOS_ITEMS[nuevoPunto][0], Constantes.PUNTOS_ITEMS[nuevoPunto][1]); // Actualiza la posición de la moneda.
    }

    // ==================== ACTUALIZACIÓN POR CUADRO ====================

    /** Velocidad actual del tiempo del juego: 1 normal, 0,3 en Bullet Time. */
    public float getEscalaTiempo() { return escalaTiempo; }

    /**
     * Avanza un cuadro de juego: conducción, mundo, colisiones, ítems y entregas, en este orden.
     * Recibe el tiempo real transcurrido; el mundo avanza con ese tiempo escalado por el Bullet Time, mientras que
     * el temblor de cámara y el cronómetro del récord siguen en tiempo real.
     */
    public void actualizar(float deltaTimeReal) {
        escalaTiempo = input.escalaTiempo(); // Espacio o Shift pulsado: cámara lenta.
        float deltaTime = deltaTimeReal * escalaTiempo; // Tiempo del mundo: lo usan todos los módulos de abajo.
        camara.actualizarFov(deltaTimeReal, Math.abs(jugador.getVelocidad()) > Constantes.VELOCIDAD_FOV_ALTO || timerVelocidad > 0); // Campo visual más abierto a gran velocidad o con nitro (efecto de cámara: tiempo real).
        camara.actualizarViento(deltaTimeReal, clima.getIntensidadTormenta()); // El surazo agita la cámara (efecto de cámara: tiempo real).
        camara.actualizarTemblor(deltaTimeReal); // Va antes de los return para que el temblor termine aunque haya Game Over o pausa; es un efecto de cámara, en tiempo real.
        if (menu.isInicio()) { // En la pantalla de inicio nada se mueve.
            if (menu.consumirTituloInicio()) { // Fija el título una sola vez.
                ventana.setTitulo(MenuManager.TITULO_INICIO);
            }
            return;
        }
        if (menu.isCongelado()) { // En pausa, Game Over o victoria nada se mueve ni se actualiza.
            return;
        }
        clima.avanzarHora(deltaTime); // 2 minutos reales = 24 horas del juego (se congela en pausa).
        aplicarCicloDia(); // Sincroniza noche, factorNoche y faros con la hora.
        tiempoPartida += deltaTimeReal; // Cronómetro del récord: tiempo real, para que la cámara lenta no lo falsee.
        if (timerMensaje > 0) timerMensaje -= deltaTime; // Descuenta el aviso de nivel (se congela en pausa).
        if (invulnerable > 0) { // Descuenta la gracia posterior a un choque.
            invulnerable -= deltaTime;
        }
        if (!pildorasVisibles) { // Cuenta atrás para que aparezcan las píldoras.
            timerSpawnPildoras -= deltaTime;
            if (timerSpawnPildoras <= 0) {
                reubicarPildoras(); // Elige dos puntos de calle distintos.
                pildorasVisibles = true;
                timerVidaPildoras = Constantes.VIDA_PILDORAS; // Permanecen 20 s en el mapa.
                timerSpawnPildoras = Constantes.SPAWN_PILDORAS;
            }
        } else { // Visibles: cuenta atrás para desaparecer si nadie las recoge.
            timerVidaPildoras -= deltaTime;
            if (timerVidaPildoras <= 0) {
                pildorasVisibles = false;
            }
        }
        if (timerFantasma > 0) timerFantasma -= deltaTime; // Descuenta el efecto de traspasar autos.
        if (timerVelocidad > 0) { // Efecto de velocidad activo.
            timerVelocidad -= deltaTime;
            jugador.setVelocidadMaxima(Constantes.VELOCIDAD_PILDORA); // Tope de 120 km/h.
            jugador.setAceleracionMotor(Constantes.ACELERACION_PILDORA); // Con 14 no llegaría: el equilibrio es aceleración / 0,7.
        } else { // Valores normales.
            jugador.setVelocidadMaxima(Constantes.VELOCIDAD_MAXIMA_NORMAL);
            jugador.setAceleracionMotor(Constantes.ACELERACION_NORMAL);
        }
        jugador.setVelocidadMaxima(clima.limitarVelocidad(jugador.getVelocidadMaxima(), Math.abs(jugador.getVelocidad()), deltaTime)); // Con el surazo el viento no deja pasar de 40 km/h.
        float autoXPrevio = jugador.getX(); // Guarda la posición X del jugador antes de moverlo.
        float autoZPrevio = jugador.getZ(); // Guarda la posición Z del jugador antes de moverlo.
        jugador.actualizar(deltaTime, input); // Procesa aceleración, giro y colisiones con la ciudad.
        actualizarTitulo(); // Muestra los controles, la velocidad y el progreso.
        if (jugador.getVelocidadImpacto() > Constantes.VELOCIDAD_IMPACTO_CHISPAS) { // Chocó contra un edificio o el borde a velocidad apreciable (evita chispas continuas al empujar la pared).
            particulas.generarChispas(jugador.getX(), jugador.getZ(), jugador.getAngulo()); // Las chispas nacen en el parachoques delantero.
            if (!jugador.isModoDios()) { // El modo dios no sufre daño.
                sistemaDano.recibirChoque(jugador.getX(), 1.0f, jugador.getZ()); // Choque contra la estructura: sube el daño; al cuarto explota.
            }
        }
        particulas.actualizar(deltaTime, timerVelocidad > 0, jugador.getX(), jugador.getZ(), jugador.getAngulo()); // Chispas, desvanecimiento y rastro de nitro.
        particulas.actualizarHuellas(deltaTime, jugador.isDerrapando(), jugador.getX(), jugador.getZ(), jugador.getAngulo()); // Marcas de derrape en el asfalto.
        particulas.actualizarHumo(deltaTime, jugador.getX(), 0f, jugador.getZ(), jugador.getAngulo(), sistemaDano.getTipoHumo()); // Humo del motor: blanco o negro según el daño.
        clima.actualizar(deltaTime, jugador.getX(), jugador.getZ()); // Ciclo de clima, lluvia y nubes.
        bus.actualizar(deltaTime); // Da la vuelta al anillo perimetral por su carril derecho.
        if (patrulla.isPersiguiendo()) { // IA de la patrulla: persigue al jugador hasta que expire el tiempo o lo embista.
            patrulla.actualizar(deltaTime, jugador.getX(), jugador.getZ());
            if (patrulla.isPersiguiendo() && !atraviesaVehiculos() && invulnerable <= 0 && patrulla.estaCercaDe(jugador.getX(), jugador.getZ(), radioColision)) { // Embestida: cuesta una vida.
                perderVida();
                patrulla.detener();
            }
        }
        if (menu.isGameOver()) { // La patrulla acabó con las vidas: nada más debe resolverse en este cuadro.
            return;
        }
        bocina.actualizar(deltaTime, input.bocinaPulsada(), jugador.getX(), jugador.getZ(), botsActivos); // La bocina asusta a los bots antes de moverlos.
        trafico.actualizar(deltaTime, botsActivos, velBots, bus.getX(), bus.getZ(), patrulla.isPersiguiendo(), patrulla.getX(), patrulla.getZ()); // Mueve el tráfico autónomo.
        if (!atraviesaVehiculos()) { // Con la píldora morada o el modo dios el jugador atraviesa a los bots sin bloquearlos ni dañarse.
            ControladorTrafico.ResultadoColision choque = trafico.resolverChoqueJugador(botsActivos, jugador.getX(), jugador.getZ(), autoXPrevio, autoZPrevio, radioColision, invulnerable <= 0);
            jugador.setPosicion(choque.jugadorX, choque.jugadorZ); // Posición corregida: devuelta a la previa si se acercaba a un bot.
            if (choque.detenerJugador) jugador.detener(); // Roce con un bot: el auto del jugador se detiene.
            if (choque.choqueFatal) perderVida(); // Choque que cuesta una vida: Game Over o reaparición con gracia.
        }
        resolverChoqueBus(); // El bus también es un obstáculo sólido.
        if (menu.isGameOver()) { // Evita cobrar monedas o ganar en el mismo cuadro del Game Over.
            return;
        }
        if (moneda.estaAlAlcance(jugador.getX(), jugador.getZ())) { // Radio de recolección de 5 unidades: cubre toda la marca de 6x6.
            monedasRecolectadas++; // Suma una moneda al contador.
            if (monedasRecolectadas == Constantes.MONEDAS_NIVEL_2 && nivel == Constantes.NIVEL_INICIAL) { // Sube al nivel 2.
                subirDeNivel(Constantes.NIVEL_2, Constantes.BOTS_NIVEL_2, Constantes.RADIO_COLISION_NIVEL_2, Constantes.VEL_BOTS_NIVEL_2);
            } else if (monedasRecolectadas == Constantes.MONEDAS_NIVEL_3 && nivel == Constantes.NIVEL_2) { // Sube al nivel 3.
                subirDeNivel(Constantes.NIVEL_3, Constantes.BOTS_NIVEL_3, Constantes.RADIO_COLISION_NIVEL_3, Constantes.VEL_BOTS_NIVEL_3);
            } else if (monedasRecolectadas == Constantes.MONEDAS_VICTORIA) { // Fin del juego.
                menu.terminarPorVictoria();
                ventana.setTitulo(tituloVictoria());
            }
            reubicarMoneda(); // Mueve la moneda a otro punto de la ciudad.
        }
        if (pildorasVisibles) { // Recogida de píldoras: al tomar una desaparecen ambas.
            if (pildoraAzul.estaAlAlcance(jugador.getX(), jugador.getZ())) { // Inmunidad: invulnerable ya impide perder vidas.
                invulnerable = Constantes.DURACION_INMUNIDAD;
                timerFantasma = Constantes.DURACION_INMUNIDAD; // También permite traspasar a los bots.
                pildorasVisibles = false;
            } else if (pildoraNaranja.estaAlAlcance(jugador.getX(), jugador.getZ())) { // Velocidad.
                timerVelocidad = Constantes.DURACION_VELOCIDAD;
                pildorasVisibles = false;
            } else if (pildoraVerde.estaAlAlcance(jugador.getX(), jugador.getZ())) { // Reparación: el auto vuelve a su estado original.
                sistemaDano.reiniciar();
                pildorasVisibles = false;
            }
        }
        tiempoAnimacion += deltaTime; // Avanza el reloj de la animación de patas de las mascotas.
        for (Mascota mascota : mascotas) { // Mueve perros y gatos dentro de sus parques.
            mascota.actualizar(deltaTime);
        }
        for (Peaton peaton : peatones) { // Hace patrullar a los peatones por las aceras.
            if (peaton.actualizar(deltaTime, jugador.getX(), jugador.getZ())) { // Un atropello llama a la patrulla (si ya persigue, no hace nada).
                patrulla.llamar(jugador.getX(), jugador.getZ());
            }
        }
        if (entregas >= Constantes.DESTINOS.length) { // Comprueba si ya se completaron todas las paradas.
            return; // Conserva el tiempo final y evita leer fuera del arreglo.
        }
        tiempo += deltaTime; // Suma los segundos de este cuadro al cronómetro.
        float distanciaX = jugador.getX() - Constantes.DESTINOS[entregas][0]; // Calcula la separación horizontal al destino activo.
        float distanciaZ = jugador.getZ() - Constantes.DESTINOS[entregas][1]; // Calcula la separación en profundidad al destino.
        float distanciaCuadrada = distanciaX * distanciaX + distanciaZ * distanciaZ; // Mide cercanía sin calcular raíz cuadrada.
        boolean estaCerca = distanciaCuadrada < Constantes.RADIO_LLEGADA_ENTREGA_CUAD; // Acepta un radio de llegada de tres unidades.
        boolean estaFrenando = Math.abs(jugador.getVelocidad()) < Constantes.VELOCIDAD_PARADA_ENTREGA; // Exige circular a menos de una unidad por segundo.
        if (estaCerca && estaFrenando) { // Solo completa la entrega si ambas condiciones se cumplen.
            entregas++; // Selecciona la siguiente parada o completa el juego.
        }
    }

    /** Aplica los parámetros del nuevo nivel: más bots, choque más amplio y tráfico más rápido, con aviso y gracia. */
    private void subirDeNivel(int nuevoNivel, int bots, float radio, float velocidadBots) {
        nivel = nuevoNivel;
        botsActivos = bots;
        radioColision = radio;
        velBots = velocidadBots;
        timerMensaje = Constantes.DURACION_MENSAJE_NIVEL;
        invulnerable = Math.max(invulnerable, Constantes.GRACIA_REAPARICION);
    }

    /** Contacto del jugador con el bus: sin gracia cuesta una vida; con gracia el bus lo expulsa como una pared. */
    private void resolverChoqueBus() {
        if (atraviesaVehiculos()) { // Con la píldora morada o el modo dios también atraviesa el bus.
            return;
        }
        GestorColisiones.ResultadoBus contacto = GestorColisiones.contactoConBus(bus.getX(), bus.getZ(), bus.getAngulo(), jugador.getX(), jugador.getZ(), invulnerable <= 0);
        if (contacto.contacto == GestorColisiones.Contacto.FATAL) { // Atropello: cuesta una vida y reaparece.
            perderVida();
        } else if (contacto.contacto == GestorColisiones.Contacto.EMPUJADO) { // En periodo de gracia el bus actúa como pared.
            jugador.detener();
            jugador.setPosicion(contacto.jugadorX, contacto.jugadorZ);
        }
    }

    /** Resta una vida: Game Over si no quedan, o reaparición con dos segundos de gracia. */
    private void perderVida() {
        if (jugador.isModoDios()) { // Vidas infinitas: el modo dios no pierde nunca una vida.
            return;
        }
        choques++;
        camara.temblar(Constantes.DURACION_TEMBLOR); // Sacudida de cámara.
        jugador.detener();
        if (choques >= Constantes.VIDAS_MAXIMAS) {
            menu.terminarPorGameOver();
            ventana.setTitulo(MenuManager.TITULO_GAME_OVER);
        } else {
            jugador.reaparecer();
            sistemaDano.reiniciar(); // Auto nuevo: sin daño.
            invulnerable = Math.max(invulnerable, Constantes.GRACIA_REAPARICION);
        }
    }

    /** Texto de la victoria con el tiempo de la partida; guarda el récord si es el mejor. */
    private String tituloVictoria() {
        boolean nuevoRecord = guardado.guardarSiEsRecord(tiempoPartida);
        String texto = "¡VICTORIA! Recogiste las " + Constantes.MONEDAS_VICTORIA + " monedas en " + GuardadoDatos.formatearTiempo(tiempoPartida);
        if (nuevoRecord) {
            texto += " (¡NUEVO RÉCORD!)";
        } else {
            texto += " | Récord: " + GuardadoDatos.formatearTiempo(guardado.cargarMejorTiempo());
        }
        return texto + ". Presiona [R] para jugar otra vez.";
    }

    /** Publica en la barra de título los controles, la velocidad, las luces, la hora y el progreso. */
    private void actualizarTitulo() {
        ventana.setTitulo(PantallaHUD.componerTitulo(jugador.getVelocidad(), iluminacion.isNoche(), iluminacion.isFaros(), clima.getHora(),
                nivel, monedasRecolectadas, Constantes.VIDAS_MAXIMAS - choques));
    }

    // ==================== DIBUJO ====================

    /** Prepara OpenGL, dibuja la escena principal y encima el minimapa y los paneles. */
    public void dibujarFrame() {
        int ancho = ventana.getAncho();
        int alto = ventana.getAlto();
        glViewport(0, 0, ancho, alto); // Utiliza toda el área de la ventana para la escena principal.
        float[] cielo = clima.colorCielo(); // El clima cambia el color del cielo.
        glClearColor(cielo[0], cielo[1], cielo[2], 1); // Define un fondo opaco.
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Borra la imagen y las distancias del cuadro anterior.
        renderizador.usar(); // Activa los shaders y el cubo compartido.
        renderizador.entero("uMapa", 0); // Selecciona perspectiva normal, no la proyección del minimapa.
        camara.configurar(renderizador, jugador.getX(), jugador.getZ(), jugador.getAngulo(), ventana.getAspecto()); // Posición y objetivo de la cámara.
        prepararLuces(); // Envía la iluminación al shader.
        dibujar(false); // Dibuja la ciudad y todo lo que hay en ella.
        int err = glGetError(); // Consulta el error pendiente de OpenGL.
        if (err != GL_NO_ERROR) {
            System.err.println("Error OpenGL ignorado para estabilidad: " + err); // Solo informa en consola; no cierra el juego.
        }
        if (mostrarMapa) { // Comprueba si el usuario no ocultó el minimapa con M.
            hud.dibujarMinimapa(ancho, alto, this); // Dibuja el segundo pase desde arriba.
        }
        PantallaHUD.Datos datos = new PantallaHUD.Datos(ancho, alto, jugador.getVelocidad(), jugador.getVelocidadMaxima(),
                iluminacion.isFaros(), iluminacion.isNoche(), choques, monedasRecolectadas, nivel, timerMensaje);
        hud.dibujarPanelVelocidad(datos, menu); // El panel de velocidad y luces se dibuja siempre, encima de todo.
        hud.dibujarHUDJuego(datos, menu); // Vidas, monedas y pantallas de fin.
    }

    /** Envía al shader las farolas, los faros del jugador y los de bus y bots. */
    private void prepararLuces() {
        iluminacion.prepararLuces(renderizador, jugador.getX(), jugador.getZ(), jugador.getAngulo()); // Farolas y faros del auto principal.
        iluminacion.enviarFarosVehiculo(renderizador, 0, bus.getX(), bus.getZ(), bus.getAngulo(), bus.getDistanciaFaros(), bus.getSeparacionFaros()); // El bus ocupa el índice 0.
        for (int i = 0; i < botsActivos; i++) { // Un foco doble por cada bot activo (índices 1 a botsActivos).
            BotTrafico bot = trafico.getBot(i);
            iluminacion.enviarFarosVehiculo(renderizador, i + 1, bot.getX(), bot.getZ(), bot.getAngulo(), bot.getDistanciaFaros(), bot.getSeparacionFaros());
        }
        iluminacion.enviarCantidadVehiculos(renderizador, botsActivos + 1); // De noche se activan todos; de día ninguno.
        iluminacion.enviarTormenta(renderizador, clima.getIntensidadTormenta()); // El surazo oscurece todo, también de día.
    }

    /** Dibuja la escena completa; vistaMapa omite los detalles pequeños, el clima y los animales (minimapa). */
    @Override
    public void dibujar(boolean vistaMapa) {
        jugador.setCapoLevantado(sistemaDano.isCapoLevantado()); // El capó se levanta con el daño.
        ciudad.dibujarBase(); // Asfalto, aceras, edificios y marcas de calle.
        if (!vistaMapa) { // Las marcas de derrape (con iluminación, sobre el asfalto) son demasiado finas para el minimapa.
            particulas.dibujarHuellas(renderizador);
            dibujarSombras(); // Sombras bajo los vehículos, antes de dibujar sus modelos.
        }
        jugador.dibujar(renderizador, tiempoAnimacion); // El auto del jugador.
        iluminacion.dibujarFarolas(renderizador); // Postes y bombillas.
        if (!vistaMapa) { // Los detalles pequeños solo son necesarios en la vista principal.
            ciudad.dibujarDecoracion(iluminacion.isNoche(), tiempo, tiempoAnimacion, trafico.getSemaforo()); // Árboles, bancos, ventanas y señalización urbana.
        }
        moneda.dibujar(renderizador, tiempoAnimacion, vistaMapa); // Marca amarilla y cubo flotante giratorio.
        if (!vistaMapa) { // El clima y el rastro no se dibujan en el minimapa (taparían la vista superior).
            renderizador.entero("uEmision", 1); // Colores directos, sin que la iluminación nocturna los oscurezca.
            clima.dibujar(renderizador, jugador.getX(), jugador.getZ(), tiempoAnimacion); // Sol o luna, nubes y lluvia.
            particulas.dibujar(renderizador, tiempoAnimacion); // Chispas y rastro de nitro.
            renderizador.entero("uEmision", 0); // Restablece el material normal.
        }
        if (pildorasVisibles) { // Cubos giratorios que flotan: morada = inmunidad, naranja = velocidad, verde = reparación.
            pildoraAzul.dibujar(renderizador, tiempoAnimacion, vistaMapa);
            pildoraNaranja.dibujar(renderizador, tiempoAnimacion, vistaMapa);
            pildoraVerde.dibujar(renderizador, tiempoAnimacion, vistaMapa);
        }
        if (patrulla.isPersiguiendo()) { // Patrulla perseguidora con sirenas.
            patrulla.dibujar(renderizador, tiempoAnimacion);
        }
        bus.dibujar(renderizador, tiempoAnimacion); // El bus del anillo, también en el minimapa.
        for (int i = 0; i < botsActivos; i++) { // Cada auto autónomo, también en el minimapa.
            trafico.getBot(i).dibujar(renderizador, tiempoAnimacion);
        }
        if (!vistaMapa) { // Peatones y mascotas son demasiado pequeños para el minimapa.
            for (Peaton peaton : peatones) {
                peaton.dibujar(renderizador, tiempoAnimacion);
            }
            for (Mascota mascota : mascotas) {
                mascota.dibujar(renderizador, tiempoAnimacion);
            }
        }
    }

    /** Sombra base bajo el jugador, la patrulla perseguidora, el bus y los bots activos (no se dibuja en el minimapa). */
    private void dibujarSombras() {
        dibujarSombra(jugador);
        if (patrulla.isPersiguiendo()) {
            dibujarSombra(patrulla);
        }
        dibujarSombra(bus);
        for (int i = 0; i < botsActivos; i++) {
            dibujarSombra(trafico.getBot(i));
        }
    }

    private void dibujarSombra(Vehiculo vehiculo) {
        renderizador.dibujarSombraBase(vehiculo.getX(), vehiculo.getZ(), vehiculo.getAngulo(), vehiculo.getAnchoSombra(), vehiculo.getLargoSombra());
    }

    /** Marca del jugador en el minimapa. */
    @Override
    public void dibujarIndicadorJugador() {
        jugador.dibujarIndicadorMinimapa(renderizador);
    }
}
