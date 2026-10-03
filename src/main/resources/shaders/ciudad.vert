#version 330 core // Selecciona GLSL 3.30 para el contexto OpenGL 3.3.
layout (location = 0) in vec3 aPos; // Lee la posición local del vértice desde el VBO.
layout (location = 1) in vec3 aNormal; // Lee la normal de la cara desde el mismo VBO.
uniform vec3 uPos; // Recibe el centro de la caja en la ciudad.
uniform vec3 uEscala; // Recibe el tamaño de la caja en cada eje.
uniform vec3 uOjo; // Recibe la posición de la cámara.
uniform vec3 uObjetivo; // Recibe el punto que observa la cámara.
uniform float uGiro; // Recibe el giro del objeto alrededor de Y.
uniform float uInclinacion; // Recibe la inclinación del objeto alrededor de su eje X (0 = sin inclinar); la usa el capó levantado.
uniform float uAspecto; // Recibe la relación ancho/alto de la imagen.
uniform float uFov; // Recibe el campo visual vertical en grados (lo cambia CamaraManager con la velocidad).
uniform int uMapa; // Selecciona perspectiva (0) o vista superior ortográfica (1).
out vec3 vMundo; // Envía la posición mundial al shader de fragmentos.
out vec3 vNormal; // Envía la normal transformada para la iluminación de Iluminacion.

void main() { // OpenGL ejecuta este bloque una vez por vértice.
    float coseno = cos(uGiro); // Calcula el coseno del giro del objeto.
    float seno = sin(uGiro); // Calcula el seno del mismo giro.
    mat3 giro = mat3( // Construye la matriz de rotación; GLSL recibe sus columnas.
        coseno, 0.0, -seno, // Primera columna: dirección del eje X rotado.
        0.0, 1.0, 0.0, // Segunda columna: Y permanece vertical.
        seno, 0.0, coseno // Tercera columna: dirección del eje Z rotado.
    ); // Completa la matriz de tres filas y tres columnas.
    float ci = cos(uInclinacion); // Calcula el coseno de la inclinación.
    float si = sin(uInclinacion); // Calcula el seno de la misma inclinación.
    mat3 inclinacion = mat3( // Rotación alrededor de X: con un ángulo positivo el frente (-Z) del objeto sube.
        1.0, 0.0, 0.0, // Primera columna: el eje X no cambia.
        0.0, ci, si, // Segunda columna: dirección del eje Y inclinado.
        0.0, -si, ci // Tercera columna: dirección del eje Z inclinado.
    ); // Con inclinación 0 es la matriz identidad: nada cambia.
    vMundo = giro * (inclinacion * (aPos * uEscala)) + uPos; // Escala, inclina, gira y traslada el vértice al mundo.
    vNormal = normalize(giro * (inclinacion * (aNormal / uEscala))); // Corrige la normal con la inversa transpuesta de escala, inclinación y giro.

    if (uMapa == 1) { // Esta rama se usa al dibujar el minimapa en JuegoEntregas.
        float pantallaX = vMundo.x / @LIMITE_HUD@; // Ajusta el ancho del mundo (LIMITE 105 + 2) al intervalo visible -1 a 1.
        float pantallaY = -vMundo.z / @LIMITE_HUD@; // Coloca el norte (-Z) en la parte superior del mapa.
        float profundidad = -vMundo.y / 100.0; // Hace que los objetos más altos se vean por encima.
        gl_Position = vec4(pantallaX, pantallaY, profundidad, 1.0); // Proyecta sin reducir objetos lejanos.
    } else { // La escena principal utiliza una cámara con perspectiva.
        vec3 frente = normalize(uObjetivo - uOjo); // Calcula la dirección hacia la que mira la cámara.
        vec3 derecha = normalize(cross(frente, vec3(0.0, 1.0, 0.0))); // Obtiene el eje horizontal de la cámara.
        vec3 arriba = cross(derecha, frente); // Obtiene el eje vertical perpendicular a los otros dos.
        vec3 diferencia = vMundo - uOjo; // Traslada el origen del mundo hasta la cámara.
        float vistaX = dot(diferencia, derecha); // Mide cuánto está el vértice a la derecha de la cámara.
        float vistaY = dot(diferencia, arriba); // Mide cuánto está el vértice encima de la cámara.
        float vistaZ = -dot(diferencia, frente); // Usa Z negativa delante de la cámara, como espera OpenGL.
        float fov = (uFov > 0.0) ? uFov : 55.0; // Si nadie envió el campo visual, usa el normal de 55 grados.
        float factor = (fov == 55.0) ? 1.0 / tan(radians(55.0) * 0.5) // Con el campo visual normal usa la constante de siempre: la imagen no cambia ni en un píxel.
                : 1.0 / tan(radians(fov) * 0.5); // Con el campo visual abierto por la velocidad, lo calcula con el valor recibido.
        float cerca = 0.1; // Define la distancia mínima visible.
        float lejos = 500.0; // Define la distancia máxima visible.
        float clipX = vistaX * factor / uAspecto; // Corrige la coordenada horizontal según el ancho de la pantalla.
        float clipY = vistaY * factor; // Aplica el campo visual a la coordenada vertical.
        float clipZ = (lejos + cerca) / (cerca - lejos) * vistaZ; // Calcula el término de profundidad dependiente de Z.
        clipZ += 2.0 * lejos * cerca / (cerca - lejos); // Añade el término constante de profundidad.
        gl_Position = vec4(clipX, clipY, clipZ, -vistaZ); // OpenGL dividirá XYZ por W para producir perspectiva.
    }
}
