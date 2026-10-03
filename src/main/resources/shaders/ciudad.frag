#version 330 core // Selecciona la versión GLSL correspondiente a OpenGL 3.3.
in vec3 vMundo; // Recibe la posición del fragmento en la ciudad.
in vec3 vNormal; // Recibe la dirección perpendicular a la superficie.
uniform vec3 uColor; // Recibe el color base de la caja.
uniform vec3 uLuces[49]; // Recibe las posiciones de las 49 farolas.
uniform vec3 uAuto; // Recibe la posición del auto a la altura de los faros.
uniform vec3 uFrente; // Recibe la dirección hacia la que apunta el vehículo.
uniform vec3 uVehPos[16]; // Recibe el centro de cada vehículo autónomo a la altura de sus faros.
uniform vec3 uVehFrente[16]; // Recibe la dirección frontal de cada vehículo autónomo.
uniform vec3 uVehDim[16]; // Recibe en x la distancia al frente y en y la separación lateral de los faros.
uniform int uVehN; // Recibe cuántos vehículos autónomos tienen los faros encendidos.
uniform int uNoche; // Vale 1 de noche y 0 de día.
uniform float uFactorNoche; // 0 = día, 1 = noche, intermedio durante atardeceres y amaneceres.
uniform float uTormenta; // 0 = ambiente normal, 1 = tormenta del surazo: oscurece toda la iluminación aunque sea de día.
uniform int uFaros; // Vale 1 cuando los focos están encendidos.
uniform int uEmision; // Vale 1 si el objeto debe conservar su color sin oscurecerse.
uniform int uMapa; // Vale 1 durante el dibujo del minimapa de JuegoEntregas.
out vec4 color; // Entrega el color RGBA final al framebuffer.

void main() { // Se ejecuta para cada fragmento visible de una caja.
    if (uEmision == 1 || uMapa == 1) { // Bombillas y minimapa usan colores directos.
        color = vec4(uColor, 1.0); // Conserva el color base con opacidad completa.
        return; // Termina el shader sin calcular iluminación.
    }

    vec3 normal = normalize(vNormal); // Convierte la normal interpolada en un vector unitario.
    vec3 luz = vec3(0.48); // Define la luz ambiental diurna que llega a todas las caras.
    float intensidadSol = 0.65; // Define la fuerza de la iluminación direccional diurna.
    luz = mix(luz, vec3(0.12, 0.16, 0.24), uFactorNoche); // Transición suave a luz ambiental tenue y azulada.
    intensidadSol = mix(intensidadSol, 0.10, uFactorNoche); // Y a una pequeña luz direccional nocturna.
    vec3 direccionSol = normalize(vec3(0.4, 1.0, 0.3)); // Define una fuente lejana por su dirección.
    float incidenciaSol = max(dot(normal, direccionSol), 0.0); // Lambert: una cara recibe más luz si mira al sol.
    luz += vec3(intensidadSol) * incidenciaSol; // Suma la contribución direccional al ambiente.

    if (uFactorNoche > 0.01) { // Las farolas aportan luz en proporción a la noche.
        for (int indice = 0; indice < 49; indice++) { // Acumula el aporte de cada bombilla.
            vec3 haciaLuz = uLuces[indice] - vMundo; // Forma el vector desde la superficie hacia la farola.
            float distancia = length(haciaLuz); // Mide cuántas unidades separan superficie y bombilla.
            float difusa = max(dot(normal, normalize(haciaLuz)), 0.0); // Calcula la incidencia de la luz sobre la cara.
            float atenuacion = 1.0 + 0.12 * distancia + 0.045 * distancia * distancia; // Reduce el alcance con la distancia.
            vec3 colorFarola = vec3(1.0, 0.73, 0.34); // Define el tono cálido de la farola.
            luz += colorFarola * difusa * 3.0 / atenuacion * uFactorNoche; // Suma el aporte atenuado de esta bombilla.
        }
    }

    if (uFaros == 1) { // Calcula los conos únicamente si están encendidos.
        for (int indice = 0; indice < 2; indice++) { // Repite el cálculo para los dos faros.
            vec3 lateral = vec3(-uFrente.z, 0.0, uFrente.x); // Obtiene la dirección hacia el lado derecho del auto.
            float separacion = -0.55; // Selecciona inicialmente el faro izquierdo.
            if (indice == 1) { // Comprueba si corresponde calcular el segundo faro.
                separacion = 0.55; // Desplaza el segundo faro al lado derecho.
            }
            vec3 origen = uAuto + uFrente * 1.36 + lateral * separacion; // Ubica el faro delante de la carrocería.
            vec3 haciaSuperficie = vMundo - origen; // Forma el vector del faro al fragmento.
            float distancia = length(haciaSuperficie); // Mide la distancia recorrida por la luz.
            vec3 eje = normalize(uFrente + vec3(0.0, -0.10, 0.0)); // Inclina el foco ligeramente hacia el suelo.
            float alineacion = dot(normalize(haciaSuperficie), eje); // Un valor cercano a 1 indica el centro del haz.
            float cono = smoothstep(0.85, 0.97, alineacion); // Suaviza el borde entre el exterior y el interior del foco.
            float difusa = max(dot(normal, -normalize(haciaSuperficie)), 0.0); // Mide cuánto mira la cara hacia el faro.
            float atenuacion = 1.0 + 0.04 * distancia * distancia; // Disminuye la intensidad al alejarse.
            vec3 colorFaro = vec3(1.0, 0.94, 0.72); // Define una luz frontal blanca y cálida.
            luz += colorFaro * cono * difusa * 8.0 / atenuacion; // Añade el aporte del foco a la iluminación total.
        }
    }

    for (int v = 0; v < uVehN; v++) { // Repite el cálculo de los conos para cada vehículo autónomo.
        vec3 lateralV = vec3(-uVehFrente[v].z, 0.0, uVehFrente[v].x); // Dirección hacia el lado derecho del vehículo.
        for (int lado = 0; lado < 2; lado++) { // Faro izquierdo y derecho.
            float sep = (lado == 0) ? -uVehDim[v].y : uVehDim[v].y; // Separación lateral con signo.
            vec3 origenV = uVehPos[v] + uVehFrente[v] * uVehDim[v].x + lateralV * sep; // Ubica el faro en el frente.
            vec3 haciaSup = vMundo - origenV; // Vector del faro al fragmento.
            float distV = length(haciaSup); // Distancia recorrida por la luz.
            vec3 ejeV = normalize(uVehFrente[v] + vec3(0.0, -0.10, 0.0)); // Inclina el haz hacia el suelo.
            float conoV = smoothstep(0.85, 0.97, dot(normalize(haciaSup), ejeV)); // Suaviza el borde del haz.
            float difusaV = max(dot(normal, -normalize(haciaSup)), 0.0); // Cuánto mira la cara hacia el faro.
            luz += vec3(1.0, 0.94, 0.72) * conoV * difusaV * 8.0 / (1.0 + 0.04 * distV * distV); // Suma el aporte del foco.
        }
    }

    luz *= mix(vec3(1.0), vec3(0.50, 0.55, 0.65), uTormenta); // La tormenta apaga la luz y la tiñe de gris azulado; con 0 no cambia nada.
    color = vec4(uColor * luz, 1.0); // Multiplica el material por toda la luz acumulada.
}
