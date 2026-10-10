# Juego — ciudad 3D con Java 21 y LWJGL 3.3.3

Juego de conducción en una ciudad de 21×21 celdas dibujada solo con cajas (OpenGL 3.3 core). Se recogen monedas
para subir de nivel (3 niveles, 20 monedas para ganar), se esquiva el tráfico y al atropellar a un peatón aparece una
patrulla que persigue al jugador. Hay ciclo de día y noche, lluvia, faros, minimapa y HUD.

## Cómo ejecutar

```
mvn compile exec:java
```

La clase principal es `Main`. Con `-Ddemo.frames=60` arranca, dibuja 60 cuadros y sale
(sirve como prueba de humo).

## Controles

| Tecla | Acción |
|---|---|
| Espacio / Enter | Iniciar el juego; después, pausar y reanudar |
| W A S D / flechas | Acelerar, girar, retroceder |
| Espacio (jugando) | Freno |
| Espacio o Shift izquierdo (mantener) | Bullet Time: el mundo avanza al 30 % de la velocidad (el cronómetro del récord sigue en tiempo real) |
| U | Surazo: tormenta de viento (oscurece el día, lluvia en diagonal, cámara agitada y velocidad limitada a 40 km/h); `R` lo apaga |
| H (mantener) | Bocina: asusta a los bots a menos de 15 unidades y lanza una onda cada 0,5 s |
| C | Cicla la cámara: tercera persona → helicóptero (30 u sobre el auto) → capó → vista frontal (12 u delante del auto) |
| E | Atajo: capó / tercera persona |
| N / F | Saltar entre día y noche / encender o apagar los faros |
| M | Mostrar u ocultar el minimapa |
| R | Reiniciar la partida |
| Esc | Salir |

## Efectos visuales

- **Sombras base** bajo el auto, la patrulla, el bus y los bots; se dibujan con la iluminación normal (cambian de día a noche).
- **Ciclo de día y noche:** 24 horas de juego en 120 s, con amanecer y atardecer graduales; el cielo se interpola y los faros del jugador, el bus y los bots se encienden de noche. `F` apaga los del jugador y `N` salta entre día y noche.
- **Campo visual dinámico:** a más de 20 u/s o con nitro la cámara abre el campo visual de 55° a 70° y vuelve al frenar.

## Píldoras

Cada 30 s aparecen tres cubos flotantes en puntos distintos de la calle; al tomar uno desaparecen los tres (si nadie
los toma, se esfuman a los 20 s).

| Color | Efecto |
|---|---|
| Morada | Inmunidad durante 20 s: atraviesa vehículos |
| Naranja | Nitro: hasta 120 km/h durante 20 s |
| Verde | Repara el auto: quita el daño, el humo y el capó levantado |

## Daño progresivo

Cada choque fuerte contra la estructura (edificios y bordes del mapa, impacto de más de 3 u/s) daña el auto del
jugador. El daño se ve en el auto y se reinicia al explotar, al perder una vida por otra causa y al reiniciar con `R`.

| Choque | Efecto |
|---|---|
| 1.º | Rasguño (solo chispas) |
| 2.º | Humo blanco del motor |
| 3.º | Humo negro y capó levantado |
| 4.º | Explosión y se pierde una vida |

El modo dios no sufre daño.

## Marcas de derrape

Al girar a más del 60 % de la velocidad máxima, o al frenar (S o Espacio) avanzando a más del 40 %, las ruedas traseras
dejan marcas oscuras en el asfalto. Se generan según la distancia recorrida, duran 5 s y se estrechan al desaparecer.
Se dibujan con la iluminación normal, así que de noche se ven más oscuras que la calle.

## Códigos secretos

Se escriben en cualquier momento (también en el inicio y en pausa), con o sin mayúsculas ni acentos, sin interferir
con las teclas de conducción. Escribir el mismo código otra vez lo desactiva, y reiniciar con `R` quita todos los efectos.

| Código | Efecto |
|---|---|
| `FICCT` | Modo Dios: vidas infinitas y el auto atraviesa edificios, vehículos y peatones-patrulla (no sale del mapa) |
| `TAK` | El auto del jugador se dibuja como un tanque |
| `POLI` | El auto del jugador se dibuja como la patrulla |

Al ganar se guarda el mejor tiempo en `~/.juego3d/records.properties` y se muestra en la barra de título.

## Arquitectura

Paquetes bajo `src/main/java`:

| Paquete | Clases | Responsabilidad |
|---|---|---|
| (raíz) | `Main` | Punto de entrada y bucle principal |
| `core` | `Ventana`, `Input`, `CamaraManager`, `Juego` | GLFW/OpenGL, teclado, vistas de cámara y orquestación de la partida (reglas, niveles, vidas, orden de actualización y de dibujo) |
| `entidades` | `Vehiculo` (padre), `AutoJugador`, `BotTrafico`, `PatrullaPolicia`, `BusUrbano`, `Item`, `Peaton`, `Mascota`, `Semaforo` | Objetos del mundo: estado, movimiento y modelo de cada uno; el bot decide solo si frena ante el `Semaforo` (que únicamente expone luz, posición y sentido) |
| `sistemas` | `ControladorTrafico`, `SistemaClima`, `GestorColisiones` | Lógicas invisibles: radares anti-choque, reloj del día y lluvia, intersecciones |
| `graficos` | `Renderizador`, `Iluminacion`, `EscenaCiudad`, `GestorParticulas` | OpenGL: cajas y shaders, luces, ciudad y decoración, chispas y rastro de nitro |
| `ui` | `PantallaHUD`, `MenuManager` | Minimapa, velocímetro, vidas y monedas; estados de inicio, pausa, Game Over y victoria |
| `utilidades` | `Constantes`, `MapaCiudad`, `GuardadoDatos` | Valores fijos del juego, plano de la ciudad y récord en disco |

Los shaders están en `src/main/resources/shaders/` (`ciudad.vert`, `ciudad.frag`).

Flujo de un cuadro (`Main` → `Juego`): `actualizar(dt)` mueve al jugador, el mundo y el tráfico y resuelve las reglas;
`dibujarFrame()` dibuja la escena principal, el minimapa (segundo pase desde arriba) y los paneles.
