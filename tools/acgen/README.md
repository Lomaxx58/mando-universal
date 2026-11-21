# Generar los mandos completos de aire

```sh
git clone https://github.com/crankyoldgit/IRremoteESP8266 irr
cp -r irr/src lib && cp irr/test/IRsend_test.h lib/
# Añadir a IRsendTest::mark/space un registro en g_cap/g_freq (ver gen.cpp)
g++ -O1 -w -std=gnu++11 -DUNIT_TEST -D_IR_LOCALE_=en-AU -Ilib -c lib/*.cpp
g++ -O1 -w -std=gnu++11 -DUNIT_TEST -D_IR_LOCALE_=en-AU -Ilib gen.cpp *.o -o gen
mkdir out; while read P M N; do ./gen $P $M > out/$N.json; done < list.txt
python3 package.py out ../../app/src/main/assets/db
```

Cada fichero tiene 1800 estados: 3 (encendido / apagar / encender) × 5 modos × 15 temperaturas
(16–30 °C) × 4 velocidades × 2 (oscilación). Índice:
`((((p*5 + modo)*15 + (temp-16))*4 + ventilador)*2 + oscilación)`.
