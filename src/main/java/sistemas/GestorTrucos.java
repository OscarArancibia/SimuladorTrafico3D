package sistemas; // Lógicas invisibles del juego: clima, IA, colisiones, códigos secretos.

import java.text.Normalizer; // Quita los acentos: "POLICÍA" también vale.

/**
 * Códigos secretos: recibe las letras que escribe el jugador, guarda las últimas diez en un buffer y avisa cuando
 * lo escrito termina en una de las palabras clave. No sabe qué hace cada truco: el juego decide los efectos.
 */
public class GestorTrucos {

    /** Trucos disponibles. */
    public enum Truco {
        MODO_DIOS("FICCT"), // Vidas infinitas y el jugador no colisiona con nada.
        TANQUE("TAK"), // El auto del jugador se dibuja como un tanque.
        POLICIA("POLI"); // El auto del jugador se dibuja como la patrulla.

        private final String palabra;

        Truco(String palabra) {
            this.palabra = palabra;
        }

        public String getPalabra() { return palabra; }
    }

    private static final int TAMANO_BUFFER = 10; // Solo se recuerdan las últimas diez letras.

    private String buffer = ""; // Últimas letras escritas, en mayúsculas y sin acentos.

    /** Letras recordadas hasta ahora (para pruebas y depuración). */
    public String getBuffer() { return buffer; }

    /**
     * Registra una letra escrita.
     * @return el truco que acaba de completarse (el buffer se limpia), o null si todavía no se completó ninguno
     */
    public Truco escribir(char letra) {
        String normalizada = Normalizer.normalize(String.valueOf(Character.toUpperCase(letra)), Normalizer.Form.NFD);
        for (int i = 0; i < normalizada.length(); i++) {
            char c = normalizada.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) { // Descarta las marcas de acento.
                buffer += c;
            }
        }
        if (buffer.length() > TAMANO_BUFFER) { // Mantiene el buffer en un tamaño máximo de 10 caracteres.
            buffer = buffer.substring(buffer.length() - TAMANO_BUFFER);
        }
        for (Truco truco : Truco.values()) { // Revisa trucos.
            if (buffer.endsWith(truco.getPalabra())) {
                buffer = ""; // Limpiar tras activar.
                return truco;
            }
        }
        return null;
    }
}
