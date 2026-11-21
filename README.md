# Mando Universal (Android)

App de mando universal por infrarrojos, al estilo del "Smart Remote" de Vivo: televisiones,
aires acondicionados, TV box, proyectores, barras de sonido, ventiladores y más.

Funciona en móviles Android **con emisor de infrarrojos** (Xiaomi, Redmi, POCO, Vivo y algunos
Honor y Huawei). Los Samsung actuales y los iPhone no lo tienen.

## Descargar el APK

**https://github.com/Lomaxx58/mando-universal/releases/download/ultima/MandoUniversal.apk**

Ábrelo desde el móvil Android, descárgalo e instálalo (acepta "instalar apps de origen desconocido").
Cada vez que se sube un cambio a `main`, GitHub recompila la app y ese enlace pasa a dar la versión nueva.

## Cómo se usa

1. **Menú principal**: tus mandos guardados y los tipos de aparato.
2. Elige el tipo y la **marca** (con buscador y las más comunes arriba).
3. **Búsqueda del código**: pulsa el botón de encendido y di si el aparato ha respondido;
   si no, pasa al siguiente código de la marca. Queda guardado en *Mis mandos*.
4. **Mando**: flecha ← para elegir otra marca (y otra vez ← para volver al menú),
   marca en el centro y **⋮** con:
   - Modo tienda (solo TV), con los pasos por marca y un mini mando al lado
   - Probar otro código
   - Todos los botones del código
   - Información de la tele / del mando (protocolo, frecuencia, teclas, Smart TV, etc.)
   - Consejos, cambiar nombre y eliminar
5. **Aire acondicionado**: los **mandos completos** (los primeros que se prueban en cada marca)
   guardan el estado y lo enseñan en pantalla: encendido, modo, grados, ventilador y oscilación.
   Cada pulsación envía la orden entera, igual que el mando original, así que el aire queda
   como indica la pantalla. Los demás códigos son de teclas sueltas.

## Base de datos

Los códigos vienen de **Flipper-IRDB** (github.com/Lucaslhm/Flipper-IRDB, licencia CC0),
convertidos con `tools/build_db.py` a `app/src/main/assets/db/*.json`:

| Tipo | Marcas | Códigos |
|---|---|---|
| Televisión | 122 | 391 |
| Aire acondicionado | 96 | 158 |
| TV Box / Decodificador | 59 | 88 |
| Proyector | 54 | 122 |
| Barra de sonido / Audio | 99 | 261 |
| Ventilador | 92 | 152 |
| Purificador / Calefactor | 157 | 231 |

Los **mandos completos de aire** (57 protocolos de 66 marcas: Daikin, Mitsubishi, Fujitsu, LG,
Samsung, Panasonic, Gree, Midea, Haier, Hisense, Toshiba...) se generan con la librería
[IRremoteESP8266](https://github.com/crankyoldgit/IRremoteESP8266) (LGPL-2.1): `tools/acgen/gen.cpp`
calcula la señal exacta de cada combinación de encendido, modo, grados, ventilador y oscilación, y
`tools/acgen/package.py` las guarda en `assets/db/acp` junto con las capacidades de cada protocolo.

Para actualizar la base de datos de teclas: `git clone https://github.com/Lucaslhm/Flipper-IRDB` y
`python3 tools/build_db.py Flipper-IRDB app/src/main/assets/db`.

## Estructura

```
app/src/main/java/com/marcos/mandouniversal/
├── MainActivity.kt          Navegación entre pantallas
├── RemoteController.kt      Envía las teclas en segundo plano
├── ir/IrCodec.kt            Protocolos (NEC, Samsung, Sony, RC5/RC6, Kaseikyo, RCA, Pioneer) y señales raw
├── data/                    Base de datos, mandos guardados y guías de modo tienda
└── ui/                      Pantallas (Compose)
```

Los protocolos interpretan dirección y comando igual que el firmware de Flipper Zero, que es
quien capturó los códigos. El test `IrCodecTest` compara las señales con las que genera el
propio firmware de Flipper y comprueba que todas las teclas de la base de datos se pueden emitir.

## Abrirlo en Android Studio

*File → Open* → esta carpeta. Si avisa de que falta el *Gradle wrapper*, acepta que lo genere.
El emulador no tiene infrarrojos: para probar de verdad hace falta un móvil real.
