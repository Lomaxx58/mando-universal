# Mando Universal (Android)

App de mando universal por infrarrojos para televisiones, pensada para configurar teles
(por ejemplo, ponerlas en modo tienda) sin sacar el mando original.

Funciona en móviles Android **con emisor de infrarrojos** (casi todos los Xiaomi, Redmi y POCO,
algunos Honor, Huawei y Vivo). Los Samsung actuales y los iPhone no lo tienen.

## Cómo abrirlo

1. Instala **Android Studio** (versión reciente).
2. *File → Open* y elige la carpeta `MandoUniversal`.
3. Si Android Studio avisa de que falta el *Gradle wrapper*, acepta que lo genere
   (o en la terminal de Android Studio: `gradle wrapper`).
4. Conecta el móvil con la depuración USB activada y pulsa ▶ *Run*.

> El emulador no tiene emisor de infrarrojos: para probar de verdad hace falta un móvil real.

## Qué hace

- **Elegir marca**: Samsung, LG, Sony, Philips (modernas y antiguas), Panasonic y TCL (Roku TV).
  Cada juego de códigos tiene un botón *Probar* que envía el encendido para comprobar que la tele responde.
- **Mando**: encendido, fuente, silencio, cruceta con OK, atrás, inicio, menú, salir,
  volumen, canales y teclado numérico. Volumen, canales y flechas se repiten al mantener pulsado.
- **Modo tienda**: pasos orientativos para cada marca, con un mini mando en la misma pantalla.
  Sony tiene un botón especial que abre el modo tienda directamente; LG, uno de demo automática.
- Recuerda la última marca usada.

## Estructura

```
app/src/main/java/com/marcos/mandouniversal/
├── MainActivity.kt          Navegación entre pantallas
├── RemoteController.kt      Envía señales en segundo plano y guarda la marca
├── ir/
│   ├── IrCodec.kt           Protocolos IR (NEC, Sony, RC5, RC6, Panasonic, RCA)
│   └── IrTransmitter.kt     Acceso al emisor del móvil (ConsumerIrManager)
├── data/
│   └── Brands.kt            Códigos de cada marca y pasos de modo tienda
└── ui/                      Pantallas (Compose)
```

## Añadir una marca

Los códigos vienen de la base de datos libre **IRDB** (github.com/probonopd/irdb), carpeta `codes/<Marca>/TV/`.
Cada línea del CSV es `función,protocolo,dispositivo,subdispositivo,código`, que es justo lo que
recibe `IrCommand`. Para añadir una marca, copia uno de los bloques de `Brands.kt`, cambia los
números y añádela a la lista `all`. Si usa un protocolo nuevo, hay que añadir su codificador en `IrCodec.kt`.

## Pendiente / limitaciones

- **Hisense** no está en IRDB; habría que capturar sus códigos con un receptor IR.
- Los TCL con Google TV pueden usar códigos distintos a los de TCL Roku TV.
- Las rutas de menú del modo tienda son orientativas y cambian según el año del modelo.
- Soporte para emisores IR por USB-C (para móviles sin emisor) como siguiente paso.

## Tests

`app/src/test/.../IrCodecTest.kt` comprueba que todos los códigos generan señales válidas.
Ejecútalos con clic derecho → *Run tests*.

## Conseguir el APK sin instalar nada (GitHub)

Cada vez que se sube un cambio a `main`, GitHub compila la app (pestaña *Actions*, unos 5 minutos)
y publica **MandoUniversal.apk** en *Releases → Última versión*. Abre esa página desde el móvil
Android, descarga el APK e instálalo (acepta "instalar apps de origen desconocido").
