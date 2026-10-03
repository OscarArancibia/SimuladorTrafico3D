package entidades; // Objetos que existen en el mundo.

import graficos.Renderizador; // Primitivas para dibujar el modelo.
import utilidades.Constantes; // Velocidades y límite de la parcela.
import utilidades.MapaCiudad; // Conversión de fila/columna del parque a coordenadas.

/**
 * Perro o gato que camina dentro de su parque: avanza hacia delante y gira hacia el centro si se aleja o choca con un árbol o el banco.
 */
public class Mascota {

    /** Especie: determina velocidad y modelo. */
    public enum Tipo { PERRO, GATO }

    private final Tipo tipo;
    private final float centroX; // Centro X del parque donde vive.
    private final float centroZ; // Centro Z del parque donde vive.
    private float x; // Posición X actual.
    private float z; // Posición Z actual.
    private float angulo; // Orientación en radianes.

    /** Crea una mascota en el parque de la celda (fila, columna), desplazada en Z para no nacer dentro del banco. */
    public Mascota(Tipo tipo, int fila, int columna, float anguloInicial, float desvioInicialZ) {
        this.tipo = tipo;
        this.centroX = MapaCiudad.centro(columna); // Convierte la columna del parque a coordenada X.
        this.centroZ = MapaCiudad.centro(fila); // Convierte la fila del parque a coordenada Z.
        this.x = centroX; // Empieza alineado con el centro del parque.
        this.z = centroZ + desvioInicialZ; // Se desplaza para no nacer dentro del banco.
        this.angulo = anguloInicial;
    }

    /** Hace caminar al animal hacia delante y lo gira hacia el centro del parque si se aleja demasiado. */
    public void actualizar(float deltaTime) {
        float velocidad = Constantes.VELOCIDAD_PERRO; // Los perros caminan a 1,2 unidades por segundo.
        if (tipo == Tipo.GATO) { // Los gatos son más lentos.
            velocidad = Constantes.VELOCIDAD_GATO; // Velocidad de los gatos.
        }
        float siguienteX = x - (float) Math.sin(angulo) * velocidad * deltaTime; // Propone la nueva X según el frente (-sen).
        float siguienteZ = z - (float) Math.cos(angulo) * velocidad * deltaTime; // Propone la nueva Z según el frente (-cos).
        float desvioX = siguienteX - centroX; // Distancia horizontal propuesta al centro del parque.
        float desvioZ = siguienteZ - centroZ; // Distancia en profundidad propuesta al centro del parque.
        if (Math.abs(desvioX) > Constantes.LIMITE_PARCELA_MASCOTA || Math.abs(desvioZ) > Constantes.LIMITE_PARCELA_MASCOTA) { // Comprueba si llegaría al borde de su parcela.
            float haciaCentro = (float) Math.atan2(desvioX, desvioZ); // Ángulo cuyo frente apunta al centro del parque.
            angulo = haciaCentro + ((float) Math.random() - 0.5f) * 1.2f; // Gira hacia dentro con una variación aleatoria.
        } else if (chocaConObstaculoParque(desvioX, desvioZ)) { // Comprueba si tocaría un árbol o el banco.
            angulo += 1.57f + (float) Math.random() * 3.14f; // No avanza: gira entre 90 y 270 grados.
        } else { // El camino está libre.
            x = siguienteX; // Acepta el avance en X.
            z = siguienteZ; // Acepta el avance en Z.
        }
    }

    /** Indica si una posición relativa al centro del parque toca un tronco o el banco. */
    private boolean chocaConObstaculoParque(float desvioX, float desvioZ) {
        float[] troncos = {-2.5f, 2.5f}; // Los árboles están en las cuatro esquinas a ±2,5 del centro.
        for (float troncoX : troncos) { // Recorre las dos columnas de árboles.
            for (float troncoZ : troncos) { // Recorre las dos filas de árboles.
                float separacionX = desvioX - troncoX; // Distancia horizontal al tronco.
                float separacionZ = desvioZ - troncoZ; // Distancia en profundidad al tronco.
                if (separacionX * separacionX + separacionZ * separacionZ < 0.75f * 0.75f) { // Radio del tronco más medio cuerpo del animal.
                    return true; // Hay un árbol en el camino.
                }
            }
        }
        return Math.abs(desvioX) < 2.1f && desvioZ > -1.0f && desvioZ < 1.35f; // Rectángulo del banco con margen para el animal.
    }

    /** Dibuja la mascota según su especie. */
    public void dibujar(Renderizador r, float tiempoAnimacion) {
        if (tipo == Tipo.PERRO) { // Los dos primeros son perros.
            dibujarPerro(r, x, z, angulo, tiempoAnimacion);
        } else { // Los dos últimos son gatos.
            dibujarGato(r, x, z, angulo, tiempoAnimacion);
        }
    }

    /** Transforma una pieza del espacio local de la mascota (frente = -Z) al mundo y la dibuja sobre el césped. */
    private static void piezaMascota(Renderizador rend, float x, float z, float angulo, float escala, float lx, float ly, float lz, float sx, float sy, float sz, float r, float g, float b) {
        float coseno = (float) Math.cos(angulo); // Coseno de la orientación del animal.
        float seno = (float) Math.sin(angulo); // Seno de la misma orientación.
        float mundoX = x + (coseno * lx + seno * lz) * escala; // Gira y escala la posición local en X.
        float mundoZ = z + (-seno * lx + coseno * lz) * escala; // Gira y escala la posición local en Z.
        rend.cajaGirada(mundoX, 0.37f + ly * escala, mundoZ, sx * escala, sy * escala, sz * escala, r, g, b, angulo); // Césped en Y=0,37.
    }

    /** Dibuja un perro marrón que camina: cuerpo, cabeza, hocico, cola y cuatro patas alternadas. */
    private static void dibujarPerro(Renderizador rend, float x, float z, float angulo, float tiempo) {
        float balanceo = (float) Math.sin(tiempo * 8) * 0.12f; // Desplazamiento adelante/atrás de las patas.
        float cola = (float) Math.sin(tiempo * 10) * 0.12f; // Movimiento lateral de la cola.
        piezaMascota(rend, x, z, angulo, 1, 0, 1.0f, 0, 0.5f, 0.45f, 1.1f, 0.45f, 0.28f, 0.12f); // Cuerpo horizontal.
        piezaMascota(rend, x, z, angulo, 1, 0, 1.25f, -0.75f, 0.36f, 0.36f, 0.4f, 0.40f, 0.24f, 0.10f); // Cabeza sobresaliente.
        piezaMascota(rend, x, z, angulo, 1, 0, 1.17f, -1.0f, 0.2f, 0.16f, 0.2f, 0.15f, 0.10f, 0.06f); // Hocico oscuro.
        piezaMascota(rend, x, z, angulo, 1, cola, 1.2f, 0.6f, 0.1f, 0.1f, 0.4f, 0.35f, 0.20f, 0.08f); // Cola que se mueve.
        piezaMascota(rend, x, z, angulo, 1, -0.17f, 0.4f, -0.4f + balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata delantera izquierda.
        piezaMascota(rend, x, z, angulo, 1, 0.17f, 0.4f, -0.4f - balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata delantera derecha (en contrafase).
        piezaMascota(rend, x, z, angulo, 1, -0.17f, 0.4f, 0.4f - balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata trasera izquierda (en contrafase).
        piezaMascota(rend, x, z, angulo, 1, 0.17f, 0.4f, 0.4f + balanceo, 0.12f, 0.8f, 0.12f, 0.35f, 0.20f, 0.08f); // Pata trasera derecha.
    }

    /** Dibuja un gato naranja y blanco, 30% más pequeño que el perro, con orejas puntiagudas y cola erguida. */
    private static void dibujarGato(Renderizador rend, float x, float z, float angulo, float tiempo) {
        float escala = 0.7f; // El gato mide el 70 % del perro.
        float balanceo = (float) Math.sin(tiempo * 9) * 0.12f; // Desplazamiento adelante/atrás de las patas.
        float cola = (float) Math.sin(tiempo * 6) * 0.1f; // Balanceo lateral de la punta de la cola.
        piezaMascota(rend, x, z, angulo, escala, 0, 1.0f, 0, 0.45f, 0.4f, 1.0f, 0.92f, 0.52f, 0.14f); // Cuerpo naranja.
        piezaMascota(rend, x, z, angulo, escala, 0, 1.25f, -0.7f, 0.38f, 0.34f, 0.34f, 0.95f, 0.58f, 0.18f); // Cabeza redondeada.
        piezaMascota(rend, x, z, angulo, escala, -0.12f, 1.52f, -0.72f, 0.1f, 0.18f, 0.1f, 0.85f, 0.45f, 0.10f); // Oreja izquierda puntiaguda.
        piezaMascota(rend, x, z, angulo, escala, 0.12f, 1.52f, -0.72f, 0.1f, 0.18f, 0.1f, 0.85f, 0.45f, 0.10f); // Oreja derecha puntiaguda.
        piezaMascota(rend, x, z, angulo, escala, cola, 1.45f, 0.55f, 0.08f, 0.7f, 0.08f, 0.92f, 0.52f, 0.14f); // Cola erguida que se mece.
        piezaMascota(rend, x, z, angulo, escala, -0.15f, 0.35f, -0.35f + balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata delantera izquierda blanca.
        piezaMascota(rend, x, z, angulo, escala, 0.15f, 0.35f, -0.35f - balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata delantera derecha (en contrafase).
        piezaMascota(rend, x, z, angulo, escala, -0.15f, 0.35f, 0.35f - balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata trasera izquierda (en contrafase).
        piezaMascota(rend, x, z, angulo, escala, 0.15f, 0.35f, 0.35f + balanceo, 0.1f, 0.7f, 0.1f, 0.95f, 0.95f, 0.92f); // Pata trasera derecha.
    }
}
