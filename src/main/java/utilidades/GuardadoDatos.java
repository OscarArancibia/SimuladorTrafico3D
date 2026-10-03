package utilidades; // Herramientas globales.

import java.io.IOException; // Fallos de lectura o escritura del archivo de récords.
import java.io.InputStream; // Lee el archivo.
import java.io.OutputStream; // Escribe el archivo.
import java.nio.file.Files; // Operaciones sobre archivos.
import java.nio.file.Path; // Ubicación del archivo de récords.
import java.nio.file.Paths; // Construye la ubicación por defecto.
import java.util.Properties; // Formato clave=valor del archivo.

/**
 * Persistencia del récord: guarda en disco el mejor tiempo con el que se ganó la partida (recoger todas las monedas).
 * Si el archivo no existe o no se puede leer o escribir, el juego sigue funcionando sin récord.
 */
public class GuardadoDatos {

    private static final String CLAVE_MEJOR_TIEMPO = "mejorTiempoVictoria"; // Segundos del mejor tiempo de victoria.

    private final Path archivo; // Archivo donde se guarda el récord.

    public GuardadoDatos(Path archivo) {
        this.archivo = archivo;
    }

    /** Archivo por defecto: ~/.juego3d/records.properties, fuera del proyecto. */
    public static GuardadoDatos porDefecto() {
        return new GuardadoDatos(Paths.get(System.getProperty("user.home"), ".juego3d", "records.properties"));
    }

    /** Devuelve el mejor tiempo guardado en segundos, o -1 si todavía no hay récord. */
    public float cargarMejorTiempo() {
        if (!Files.isRegularFile(archivo)) {
            return -1;
        }
        Properties datos = new Properties();
        try (InputStream in = Files.newInputStream(archivo)) {
            datos.load(in);
            return Float.parseFloat(datos.getProperty(CLAVE_MEJOR_TIEMPO, "-1"));
        } catch (IOException | NumberFormatException e) {
            System.err.println("No se pudo leer el récord (" + archivo + "): " + e.getMessage());
            return -1;
        }
    }

    /**
     * Guarda el tiempo si es el primero o mejora al anterior.
     * @return true si el tiempo es un nuevo récord (y quedó guardado)
     */
    public boolean guardarSiEsRecord(float segundos) {
        float mejor = cargarMejorTiempo();
        if (mejor >= 0 && segundos >= mejor) {
            return false;
        }
        Properties datos = new Properties();
        datos.setProperty(CLAVE_MEJOR_TIEMPO, Float.toString(segundos));
        try {
            Files.createDirectories(archivo.getParent());
            try (OutputStream out = Files.newOutputStream(archivo)) {
                datos.store(out, "Records de JuegoEntregas");
            }
            return true;
        } catch (IOException e) {
            System.err.println("No se pudo guardar el récord (" + archivo + "): " + e.getMessage());
            return false;
        }
    }

    /** Da formato mm:ss a una cantidad de segundos. */
    public static String formatearTiempo(float segundos) {
        int total = Math.round(segundos);
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
