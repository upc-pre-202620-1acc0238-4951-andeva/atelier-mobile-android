---
name: Atelier Workshop Mobile
description: App Android de staff para talleres mecánicos; el taller leído como una vía bien señalizada.
colors:
  azul-senal: "#0071EB"
  azul-profundo: "#031A6B"
  azul-suave: "#69B1FF"
  ambar-preventivo: "#F68B01"
  ambar-oscuro: "#D87900"
  ambar-suave: "#FFCD69"
  exito: "#3AC530"
  error: "#FB2C36"
  advertencia: "#F0B100"
  fondo-claro: "#F8F8FA"
  fondo-oscuro: "#262626"
  tarjeta-clara: "#FFFFFF"
  tarjeta-oscura: "#272727"
  tinta: "#262626"
  tinta-inversa: "#FFFFFF"
  tinta-secundaria-clara: "#454B55"
  tinta-secundaria-oscura: "#C6C6C6"
  borde-claro: "#C6C6C6"
  borde-oscuro: "#EBEBEB"
  tinta-azul-clara: "#005DC2"
  tinta-exito-clara: "#00702B"
  tinta-error-clara: "#B3101D"
  tinta-espera-clara: "#9A5300"
typography:
  display:
    fontFamily: "Satoshi, sans-serif"
    fontSize: "48sp"
    fontWeight: 900
    lineHeight: "48sp"
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Satoshi, sans-serif"
    fontSize: "28sp"
    fontWeight: 900
    lineHeight: "32sp"
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Satoshi, sans-serif"
    fontSize: "16sp"
    fontWeight: 700
    lineHeight: "24sp"
  body:
    fontFamily: "Satoshi, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  label:
    fontFamily: "Satoshi, sans-serif"
    fontSize: "14sp"
    fontWeight: 700
    lineHeight: "20sp"
rounded:
  placa: "6dp"
  control: "12dp"
  panel: "16dp"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
components:
  panel-parte:
    backgroundColor: "{colors.azul-senal}"
    textColor: "{colors.tinta-inversa}"
    rounded: "{rounded.panel}"
    padding: "16dp"
  panel-decision:
    backgroundColor: "{colors.ambar-preventivo}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.control}"
    padding: "12dp"
  placa:
    backgroundColor: "{colors.tarjeta-clara}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.placa}"
    padding: "2dp 8dp"
  button-primary:
    backgroundColor: "{colors.azul-senal}"
    textColor: "{colors.tinta-inversa}"
    rounded: "{rounded.control}"
    height: "48dp"
  button-on-panel:
    backgroundColor: "{colors.tinta-inversa}"
    textColor: "{colors.azul-profundo}"
    rounded: "{rounded.control}"
    height: "48dp"
  fab:
    backgroundColor: "{colors.azul-profundo}"
    textColor: "{colors.tinta-inversa}"
    rounded: "{rounded.panel}"
    height: "56dp"
---

<!-- SEED: established with the user before implementation; re-run $impeccable document once there's code to capture the actual tokens and components. -->

# Design System: Atelier Workshop Mobile

Reemplaza a `docs/design-system.md`, que describía la web en Tailwind. Este documento es solo visual y solo para la app Android (Jetpack Compose, Material 3). La verdad de producto está en [PRODUCT.md](PRODUCT.md).

Es una **semilla**: la dirección la eligió el usuario el 2026-11-16 antes de construir. La paleta, Satoshi y los dos temas son compromisos de marca y están cerrados. Las medidas exactas (escala tipográfica, alturas, tonos derivados del tema oscuro) son el punto de partida y se confirman al implementar; lo que aún no se puede afirmar va marcado como *por resolver*.

## Overview

**Creative North Star: "La vía señalizada"**

El taller se lee como una vía bien señalizada. Quien maneja no estudia una señal: la entiende de un vistazo, a distancia y en movimiento. Así revisa el dueño su taller, de pie junto al portón, con el teléfono en una mano y un cliente esperando. La señalización vial peruana ya le enseñó el código: el azul informa, el ámbar previene, el rojo prohíbe, el verde confirma. La paleta de marca coincide con ese código, así que el color no decora: dice qué clase de cosa se está mirando.

Del mundo de la señalización la app toma cuatro cosas y solo cuatro: el **color con oficio**, la **leyenda pesada** (pocas palabras, grandes, en Satoshi Black), la **densidad de señal** (un mensaje por panel) y una firma, **la placa**: cada vehículo aparece como su placa, igual que en el patio nadie dice "la orden 84" sino "la BKL". La estructura, la navegación y los controles no vienen de ese mundo: son Material 3 sin disfraz. Nada de flechas de carretera, postes, texturas reflectivas ni pictogramas viales.

El sistema rechaza dos salidas fáciles: el panel de tarjetas de indicadores con íconos en cajitas, que es lo que trae cualquier app de gestión, y el tablero "automotriz" oscuro con neón y relojes.

**Key Characteristics:**

- Una región domina cada pantalla. El panel azul es dueño del primer tercio y nada compite con él.
- Una cifra por pantalla a escala de señal; el resto se queda en tamaño de lectura.
- El ámbar significa una sola cosa: algo espera una decisión humana.
- Cada vehículo es su placa, siempre con la misma forma.
- Una sola familia tipográfica. Satoshi lleva leyendas, datos y etiquetas.
- Plano. La profundidad la dan los campos de color y el filete, no las sombras.
- Los paneles no cambian de color de noche, como una señal: cambian el fondo y la tinta.

## Colors

Paleta de marca fija, usada con el código de la señalización: pocos colores saturados en campos grandes, cada uno con un solo significado.

**Estrategia:** comprometida en las pantallas de supervisión (el azul ocupa cerca de un tercio de "Hoy") y contenida en las de trabajo (formularios y listas), donde el azul se reduce a la barra superior y al botón principal.

**Tema:** claro y oscuro, siguiendo al sistema. Claro es el de la escena principal (patio y mostrador de día); oscuro es el del cierre del día en casa y el turno largo. Ambos son esquemas completos. Sin color dinámico de Material You: la paleta es de marca.

### Primary

- **Azul Señal** (#0071EB): informa. Fondo del panel de cabecera, botón principal, destino activo y estados "en curso". Con texto blanco da 4.6:1.
- **Azul Profundo** (#031A6B): el azul que se apoya sobre el azul. FAB en tema claro, texto de botones sobre panel, tinta del destino activo.
- **Azul Suave** (#69B1FF): el azul del tema oscuro para texto, enlaces y FAB; en claro, solo fondos tenues de selección.

### Secondary

- **Ámbar Preventivo** (#F68B01): previene. Fondo de los paneles de "Esperan tu decisión" y de nada más. El texto encima es siempre Tinta (#262626, 6.2:1); el blanco sobre ámbar no pasa contraste.
- **Ámbar Oscuro** (#D87900): el ámbar presionado y el borde de una decisión pendiente dentro de una ficha.
- **Ámbar Suave** (#FFCD69): fondo tenue de una fila con decisión pendiente cuando el panel completo sería demasiado.

### Tertiary

Estados del sistema. El tono de marca va en **marcas y rellenos**; el texto usa la tinta derivada, que sí pasa contraste.

- **Éxito** (#3AC530): listo, aprobado, sincronizado. Texto en claro: #00702B.
- **Error** (#FB2C36): rechazado, falló, sin stock. Texto en claro: #B3101D.
- **Advertencia** (#F0B100): tardanza, stock mínimo, por vencer. Solo como marca de estado, nunca como panel. Texto en claro: #9A5300.

### Neutral

- **Fondo claro** (#F8F8FA) y **Tarjeta clara** (#FFFFFF): suelo y superficies en tema claro.
- **Fondo oscuro** (#262626) y **Tarjeta oscura** (#272727): suelo y superficies en tema oscuro. La separación entre ambos es mínima a propósito; la da el divisor, no el tono. Pasos tonales fijados en `Theme.kt`: barra inferior y hoja inferior usan `surfaceContainer` (#2E2E2E); el borde de separación es `outlineVariant` (blanco al 12 %). Falta verificar contraste en dispositivo.
- **Tinta** (#262626) y **Tinta inversa** (#FFFFFF): texto principal.
- **Tinta secundaria**: #454B55 en claro, #C6C6C6 en oscuro. El #C6C6C6 de marca nunca es texto sobre fondo claro (1.7:1).
- **Bordes**: #C6C6C6 en claro. En oscuro, #EBEBEB a baja opacidad para divisores; a plena fuerza solo en el filete de un panel.
- **Tinta azul clara** (#005DC2): texto y enlaces azules sobre fondo claro, porque #0071EB sobre #F8F8FA queda en 4.3:1.

### Named Rules

**La Regla del Código Vial.** Cada color dice una sola cosa: azul informa, ámbar pide una decisión, verde confirma, rojo detiene. Si un color aparece sin ese significado, sobra.

**La Regla del Ámbar Escaso.** El ámbar es solo para lo que espera a una persona: una propuesta por aprobar, una proforma sin respuesta, un descargo por revisar. Nunca un botón principal, nunca un adorno, nunca "destacado". Si en una pantalla todo es ámbar, nada lo es.

**La Regla de la Señal Nocturna.** El panel azul y los paneles ámbar tienen el mismo color en claro y en oscuro. Lo que cambia entre temas es el suelo y la tinta.

**La Regla del Tono con Tinta.** Verde, rojo y amarillo de marca no son colores de texto sobre claro. Marca con el tono, escribe con la tinta derivada.

## Typography

**Display Font:** Satoshi (con sans-serif del sistema de respaldo)
**Body Font:** Satoshi
**Label/Mono Font:** ninguna aparte. Albert Sans ExtraBold existe solo dentro del imagotipo.

**Character:** una grotesca geométrica de trazo firme. En Black se comporta como la leyenda de una señal; en Regular se lee sin esfuerzo en una fila. El contraste del sistema viene del salto de peso y de escala, no de mezclar familias.

Satoshi entra a `res/font` como OTF (`satoshi_regular`, `medium`, `bold`, `black`), convertido desde los WOFF2 de `docs/branding/satoshi/`. Pesos disponibles: 400, 500, 700 y 900; no hay 600 ni 800. El archivo trae cifras tabulares (`tnum`) y se aplican a toda la tipografía con `fontFeatureSettings`.

### Hierarchy

Mapeada a los roles de Material 3. Tamaños en `sp`, para que sigan el ajuste de fuente del sistema.

- **Display** (900, 48sp, 48sp, −0.02em) → `displaySmall`: la leyenda de señal. Una por pantalla: la cifra del parte del día, el total de una proforma.
- **Headline** (900, 28sp, 32sp, −0.01em) → `headlineMedium`: título de pantalla sin panel y cifras secundarias del panel.
- **Title L** (900, 22sp, 28sp) → `titleLarge`: la placa en tamaño grande y el título de una ficha.
- **Title** (700, 16sp, 24sp) → `titleMedium`: primera línea de una fila, título de sección.
- **Body** (400, 16sp, 24sp) → `bodyLarge`: lectura y campos de formulario.
- **Body S** (400, 14sp, 20sp) → `bodyMedium`: segunda línea de una fila, metadatos.
- **Label** (700, 14sp, 20sp) → `labelLarge`: botones, pestañas, estados.
- **Label S** (500, 12sp, 16sp, +0.01em) → `labelMedium`: destinos de la barra inferior y encabezados de columna. Nada baja de 12sp.

### Named Rules

**La Regla de la Leyenda Única.** Una sola cifra por pantalla va en Display. Dos leyendas compitiendo son dos señales en el mismo poste.

**La Regla de la Familia Única.** Todo es Satoshi. Los códigos, placas, montos y cronómetros no usan fuente mono: usan peso y cifras tabulares.

**La Regla de la Frase Corta.** El texto de un panel cabe en una mirada: cifra y hasta cuatro palabras. Lo que necesita una oración va debajo del panel, en tamaño de lectura.

## Layout

**Módulo de 8 dp.** Todo espaciado y toda altura es múltiplo de 8 (4 solo dentro de un componente). Margen de pantalla 16 dp; separación entre secciones 24 dp, con más aire encima de un título de sección que debajo.

**Orden de lectura de una pantalla:** panel de cabecera → lo que espera decisión → el resto. El panel sangra hasta el borde superior, por debajo de la barra de estado (edge-to-edge, íconos de estado claros).

**Alcance del pulgar.** Las acciones principales viven en el tercio inferior: FAB, barra de acciones fija al pie de un formulario, botones de una hoja inferior. Arriba se lee; abajo se actúa.

**Filas.** Alturas en módulos enteros: 64 dp para una línea con placa, 72 dp para dos líneas, 88 dp cuando la fila lleva una acción. Todo objetivo táctil mide 48 dp o más, con 8 dp de separación.

**Densidad.** Baja en el panel (un mensaje), media en las listas. Una pantalla de supervisión no muestra más de tres bloques antes del primer scroll.

**Tamaños de ventana.** Ancho compacto: barra de navegación inferior. Ancho expandido (tablet, plegable abierto): riel de navegación y contenido en dos paneles, lista y detalle. El panel de cabecera nunca supera un tercio del alto visible. Con el teclado abierto, la barra de acciones sube con el IME y el panel se colapsa a barra.

**Tamaño de fuente del sistema.** A escala 1.3 nada se recorta: las leyendas del panel bajan a una columna y las filas crecen en alto en lugar de truncar la placa.

## Elevation & Depth

Plano. La profundidad se construye con campos de color y con el filete, no con sombras. Una tarjeta no flota: se distingue por su fondo y un divisor de 1 dp.

Solo llevan elevación de Material 3, sin sombras a medida, las piezas que de verdad están encima: el FAB, la hoja inferior con su velo, los diálogos y el snackbar. La barra inferior se separa con un divisor, no con sombra.

No hay vidrio ni desenfoque: una señal es opaca, y a contraluz del portón la translucidez se vuelve ilegible.

### Named Rules

**La Regla de la Lámina Plana.** Si algo necesita sombra para entenderse, está mal ubicado o mal coloreado. Sombra solo para lo que tapa contenido.

## Shapes

Una familia de rectángulos de esquina redondeada, como una lámina de señal. Tres radios y nada más:

- **Placa** (6 dp): la ficha de placa y los huesos del esqueleto.
- **Control** (12 dp): botones, campos, chips, tarjetas y paneles de decisión.
- **Panel** (16 dp): el filete del panel de cabecera y el FAB.

**El filete.** El panel de cabecera lleva un borde interior blanco de 2 dp, separado 16 dp del borde de pantalla, como el filete de una señal informativa. Es el único borde decorativo del sistema y solo existe ahí.

Sin píldoras: los botones son rectángulos redondeados. **Única excepción (decisión del 2026-11-20): la barra inferior flotante y su botón de acción**, que usan forma completa (`FloatingBarShape`). Sin tarjetas dentro de tarjetas. La hoja inferior y los diálogos usan la forma estándar de Material 3.

## Components

Controles de Material 3 con el tema de Atelier. La regla del mundo es que entra por tipo, color, densidad y la placa; nunca reemplaza un control conocido. Todo vive en `core/designsystem/component/`.

### Panel de cabecera

La región que domina la pantalla. Tres tamaños, un solo componente:

- **Parte** (pantallas de supervisión, como "Hoy"): fondo Azul Señal a sangre, filete blanco, una línea de contexto (sede y fecha), la leyenda en Display y hasta tres cifras secundarias en Headline separadas por una regla blanca superior. Ocupa cerca de un tercio de la pantalla.
- **Ficha** (detalle de un objeto: una orden, un vehículo, un colaborador): la identidad del objeto —la placa en grande, o el nombre— más su estado actual. Sin filete.
- **Barra** (formularios y listas largas): la barra superior de Material 3 en Azul Señal con título y Atrás. Al hacer scroll, Parte y Ficha se colapsan a Barra.

Las cifras del panel no se animan al cambiar ni cuentan hacia arriba: cambian. Tocar una cifra secundaria lleva a su lista filtrada.

### La placa

La firma del sistema. Un vehículo se muestra siempre como su placa.

- **Forma:** campo blanco, borde Tinta de 2 dp, radio 6 dp, texto Satoshi Black en mayúsculas con su guion (`BKL-482`). Tamaño L (22sp) en paneles de decisión y fichas; tamaño M (16sp) en listas densas.
- **Constante entre temas:** una placa es un objeto físico. En oscuro sigue siendo blanca con tinta oscura.
- **Posición fija:** primera cosa de la fila, a la izquierda.
- **Solo para placas.** Números de orden, códigos de repuesto y documentos son texto normal.
- **Accesibilidad:** se anuncia como "placa B K L 4 8 2", no como una palabra.

### Panel de decisión

Lo que espera al dueño: propuestas por aprobar, proformas sin respuesta, descargos por revisar, suspensiones por validar.

- **Estilo:** fondo Ámbar Preventivo, texto Tinta, radio 12 dp, sin borde. Placa a la izquierda, qué se decide y quién lo originó al centro, el monto a la derecha.
- **Encabezado de sección:** "Esperan tu decisión · 3", siempre con el conteo.
- **Comportamiento:** toda la fila es presionable y abre la decisión en una hoja inferior con las acciones al alcance del pulgar. Al resolverse, el panel muestra su resultado (ícono y texto: "Aprobada", "Rechazada") y se retira.
- **Vacío:** si no hay nada, la sección dice "Nada espera tu decisión" en una línea. No desaparece: su ausencia también informa.

### Buttons

- **Shape:** rectángulo redondeado (12 dp), alto mínimo 48 dp.
- **Primary:** fondo Azul Señal, etiqueta blanca en Label. Uno por pantalla.
- **Sobre el panel azul:** fondo blanco, etiqueta Azul Profundo.
- **Secondary:** contorno de 1 dp en el color de borde, etiqueta Tinta. **Text:** etiqueta en tinta azul.
- **Destructivo:** etiqueta y contorno en tinta de error; relleno rojo solo en la confirmación final.
- **Pressed:** escala a 0.97 en 120 ms con ease-out, al tocar, no al soltar. **Disabled:** 38 % de opacidad, y siempre con el motivo visible cerca. **Loading:** el botón conserva su ancho, bloquea toques y muestra progreso junto a la etiqueta.
- **FAB:** uno por pantalla, extendido, con verbo y objeto. Azul Profundo en claro, Azul Suave con tinta Azul Profundo en oscuro. **No va en las raíces de pestaña**: ahí la acción principal ("Recibir vehículo") es el botón circular de la barra flotante; el FAB queda para pantallas sin barra (p. ej. "Nueva bahía").

### Chips y marcas de estado

- **Marca de estado:** ícono más texto en Label, sobre un fondo tenue del color del estado. Nunca solo color.
- **Vocabulario fijo:** En curso (azul) · Listo, Aprobada, Sincronizado (verde) · Espera decisión (ámbar) · Rechazada, Falló (rojo) · Tardanza, Stock mínimo, Suspendida (advertencia) · Pendiente de sincronizar, Sin permiso (neutro).
- **Posición fija:** la marca va siempre en el mismo lugar de la fila, al extremo derecho.
- **Chips de filtro:** los de Material 3, radio 12 dp; seleccionado en fondo azul tenue con tinta azul.

### Cards / Containers

- **Corner Style:** 12 dp.
- **Background:** Tarjeta clara u oscura sobre el fondo.
- **Shadow Strategy:** ninguna (ver Elevation).
- **Border:** divisor de 1 dp entre filas; una tarjeta aislada lleva borde de 1 dp.
- **Internal Padding:** 16 dp.
- Se prefiere la **lista con divisores** a la pila de tarjetas. Tarjeta solo cuando el contenido es un objeto que se abre.

### Inputs / Fields

- **Style:** campo delineado de Material 3, radio 12 dp, etiqueta siempre visible encima; el texto de ejemplo nunca hace de etiqueta.
- **Focus:** borde Azul Señal de 2 dp.
- **Error:** borde y mensaje en tinta de error debajo del campo, con ícono; el mensaje dice cómo corregir. Los errores del backend muestran su `message` tal cual.
- **Teclado:** el tipo correcto por campo (numérico para montos y kilometraje, mayúsculas para placa).
- La placa se escribe en un campo propio que da formato mientras se teclea.

### Navigation

- **Barra inferior flotante** (estilo Lemon, medidas de quina-webapp): una píldora de 64 dp con 3 a 5 destinos (ícono 22 dp y etiqueta Label S, siempre visibles), margen de 12 dp a los lados y abajo, borde de 1 dp y relleno opaco, **sin sombra**. Flota sobre el contenido: las listas suman su alto al final (`LocalFloatingBarInset`). El destino activo lleva un indicador `primaryContainer` que se desliza (250 ms, ease-out-expo; sin movimiento con "quitar animaciones"). A la derecha, separado por 12 dp, un botón circular de 64 dp con la acción más importante ("Recibir vehículo", solo con `operations:work_orders:create`). Para dueño y administrador: Hoy · Órdenes · Equipo · **Módulos**; "Módulos" no navega, abre la hoja de módulos. La escala de fuente se detiene en 1,15 dentro de la píldora.
- **Hoja de módulos:** hoja inferior que se arrastra para cerrar, con cuadrícula de 4 columnas de baldosas (ícono 24 dp y nombre, mínimo 80 dp) agrupadas por tarea (Taller, Caja y reportes, Repuestos) y, al pie, la cuenta (Notificaciones, Pendientes de sincronizar, Mi cuenta). Cada baldosa aparece solo con el permiso de su fuente.
  Etiquetas con el nombre de lo que contienen ("Hoy", "Órdenes", "Equipo", "Módulos"), no "Inicio" ni "Panel".
- **Activo:** indicador azul tenue, ícono y etiqueta en Azul Profundo (Azul Suave en oscuro). **Inactivo:** tinta secundaria.
- Los destinos que el usuario no puede abrir no se muestran. El gesto Atrás del sistema siempre funciona y nunca pierde un formulario sin avisar.
- **Transiciones:** entre destinos, fundido corto; al entrar a un detalle, eje compartido horizontal; todo de Material, sin curvas propias.

### Hoja inferior

Para decidir sin salir de la lista: aprobar una propuesta, revisar un descargo, elegir sede.

- Sigue al dedo 1:1 y se puede soltar o retomar a mitad de camino. Se cierra por velocidad, no solo por distancia. Resorte sin rebote (amortiguación crítica).
- Las acciones van al pie, dentro del alcance del pulgar. Rechazar pide motivo en la misma hoja.

### Carga (esqueleto)

La pantalla nunca aparece vacía para después llenarse. El método es el de boneyard: **el esqueleto se extrae del layout real**, no se dibuja aparte. boneyard-js no tiene soporte para Jetpack Compose (cubre React, Vue, Svelte, Angular y React Native), así que aquí se aplica su mismo principio de forma nativa.

1. **Room primero.** Si hay datos en caché, se pintan de inmediato y la actualización corre detrás con una barra de progreso fina bajo el panel. No hay esqueleto.
2. **Sin caché, esqueleto.** Se renderiza el mismo composable de la pantalla con un `UiState` de muestra y cada hoja de datos (texto, cifra, placa, foto) se pinta como un hueso con `Modifier.bone(loading)`. Mismas medidas, mismos márgenes: al llegar los datos nada se mueve.
3. **Lo fijo es real desde el primer cuadro.** Panel, títulos de sección, barra inferior y FAB se muestran de verdad. Solo los datos son huesos.
4. **Hueso:** rectángulo de radio 6 dp con el alto de la línea que reemplaza y entre 40 % y 80 % de su ancho. Tinta al 8 % sobre el fondo; blanco al 24 % sobre el panel azul. Late en opacidad (de 1 a 0.55, ciclo de 1.2 s). Sin barrido de brillo.
5. **Cuántos:** las filas que caben en pantalla, o el último conteo conocido.
6. **Sin parpadeo.** El esqueleto espera 150 ms antes de aparecer; si los datos llegan antes, no se muestra. El paso a contenido es un fundido de 150 ms.
7. **Nunca eterno.** Si falla, cambia al estado de error con reintento. Sin red y sin caché, dice "Sin conexión y sin datos guardados".
8. **Accesibilidad:** el contenedor anuncia una vez "Cargando órdenes"; los huesos no existen para TalkBack. Con "quitar animaciones" activo, los huesos quedan fijos.

Prohibido: pantalla en blanco, spinner a pantalla completa, esqueletos dibujados a mano que no coinciden con el layout final y contenido que salta al cargar.

### Estados de pantalla

Toda pantalla resuelve los cinco, con las palabras del taller:

- **Vacío:** una línea que dice qué habría aquí y, si aplica, la acción para crearlo. Sin ilustraciones.
- **Error:** el `message` del backend, y "Reintentar".
- **Pendiente de sincronizar:** marca neutra con ícono en el elemento, y el conteo total visible en "Más". Lo encolado se distingue de lo guardado.
- **Sin permiso:** el destino no aparece; si se llega por un enlace, una línea que lo explica.
- **Bloqueado por plan:** se dice qué plan lo incluye, sin tono de venta.

### Movimiento

- **Respuesta inmediata.** Todo lo presionable responde al tocar, no al soltar.
- **Lo frecuente no se anima.** "Hoy" se abre decenas de veces al día: sin entrada escalonada ni cifras que cuentan. La entrada animada queda para lo ocasional.
- **Curvas:** ease-out para lo que entra o responde; nunca ease-in. Menos de 300 ms, salvo hojas y diálogos. Salir es más rápido que entrar.
- **Continuidad:** lo que entra por un lado sale por el mismo lado; una hoja nace desde abajo y vuelve abajo.
- **Háptica** solo en lo que importa: confirmar una decisión, un error, completar una marcación. En el mismo cuadro que el cambio visual.
- **"Quitar animaciones":** los desplazamientos se vuelven fundidos; la respuesta de color al presionar se mantiene.

## Do's and Don'ts

### Do:

- **Do** abrir cada pantalla con una sola región dominante y una sola cifra en Display.
- **Do** mostrar cada vehículo con el componente de placa, primero en la fila y con la misma forma en toda la app.
- **Do** reservar el ámbar para lo que espera una decisión, con texto en Tinta (#262626).
- **Do** acompañar todo estado con ícono y texto, en la misma posición de la fila.
- **Do** pintar Room primero y derivar el esqueleto del layout real cuando no hay caché.
- **Do** poner la acción principal en el tercio inferior, con verbo y objeto.
- **Do** tomar colores, tipos y formas del tema; verificar cada pantalla en claro, oscuro y fuente al 1.3.
- **Do** usar los controles de Material 3 tal como son.

### Don't:

- **Don't** poner íconos en cajitas dentro de las tarjetas de indicador. (Decisión del 2026-11-20: "Hoy" vuelve a la composición anterior —taller y usuario, sede activa, recepción, seis indicadores, órdenes— con los tokens de este sistema; el indicador es la cifra, sin ícono.)
- **Don't** usar ámbar para botones principales, adornos o "destacados".
- **Don't** poner texto blanco sobre ámbar, ni texto en verde, rojo o amarillo de marca sobre fondo claro.
- **Don't** usar #C6C6C6 como texto sobre fondo claro.
- **Don't** añadir una segunda familia tipográfica ni fuente mono; Albert Sans es solo del imagotipo.
- **Don't** agregar sombras a tarjetas o paneles, ni vidrio, desenfoque o degradados.
- **Don't** usar botones píldora (salvo la barra flotante y su botón), tarjetas dentro de tarjetas ni franjas de color en el borde de una tarjeta.
- **Don't** disfrazar la app de carretera: sin flechas viales, postes, pictogramas de tránsito ni texturas reflectivas.
- **Don't** mostrar una pantalla en blanco ni un spinner a pantalla completa.
- **Don't** animar lo que se ve decenas de veces al día, ni usar rebote fuera de un gesto con impulso.
- **Don't** mostrar un dato que no venga del backend ni decidir la interfaz por nombre de rol.
