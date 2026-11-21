// Genera, con la librería IRremoteESP8266, la señal exacta de cada estado
// (encendido, modo, temperatura, ventilador, oscilación) de cada protocolo de aire.
// Uso: ./gen PROTOCOLO MODELO   -> imprime JSON
#include "IRac.h"
#include "IRutils.h"
#include <cstdio>
#include <map>
#include <string>
#include <vector>
std::vector<int32_t> g_cap; uint32_t g_freq;

int main(int argc, char** argv) {
  decode_type_t proto = strToDecodeType(argv[1]);
  int16_t model = argc > 2 && argv[2][0] != '-' ? IRac::strToModel(argv[2], -1) : -1;
  if (proto == decode_type_t::UNKNOWN || !IRac::isProtocolSupported(proto)) { fprintf(stderr, "no soportado %s\n", argv[1]); return 1; }
  if (argc > 2 && argv[2][0] != '-' && model < 0) { fprintf(stderr, "modelo desconocido %s\n", argv[2]); return 1; }
  IRac ac(0);
  const stdAc::opmode_t modes[] = {stdAc::opmode_t::kCool, stdAc::opmode_t::kHeat, stdAc::opmode_t::kDry, stdAc::opmode_t::kFan, stdAc::opmode_t::kAuto};
  const stdAc::fanspeed_t fans[] = {stdAc::fanspeed_t::kAuto, stdAc::fanspeed_t::kLow, stdAc::fanspeed_t::kMedium, stdAc::fanspeed_t::kHigh};
  const stdAc::swingv_t swings[] = {stdAc::swingv_t::kOff, stdAc::swingv_t::kAuto};
  std::map<std::string, int> uniq; std::vector<std::string> sigs; std::vector<int> idx;
  std::map<int32_t, int> alpha; std::vector<int32_t> alphaList; uint32_t freq = 38000;
  for (int p = 0; p < 3; p++) for (int m = 0; m < 5; m++) for (int t = 16; t <= 30; t++)
  for (int f = 0; f < 4; f++) for (int sw = 0; sw < 2; sw++) {
    stdAc::state_t s;
    IRac::initState(&s, proto, model, p != 1, modes[m], t, true, fans[f], swings[sw], stdAc::swingh_t::kOff,
        false, false, false, true, false, false, false, -1, -1);
    // p=0: encendido sin cambiar (prev igual); p=1: apagar; p=2: encender desde apagado
    stdAc::state_t prev = s; if (p != 0) prev.power = !s.power;
    g_cap.clear();
    if (!ac.sendAc(s, &prev)) { fprintf(stderr, "fallo al enviar\n"); return 1; }
    freq = g_freq ? g_freq : 38000;
    while (!g_cap.empty() && g_cap.back() < 0) g_cap.pop_back();
    std::string enc;
    for (int32_t v : g_cap) {
      auto it = alpha.find(v);
      if (it == alpha.end()) { int n = alphaList.size(); alpha[v] = n; alphaList.push_back(v); it = alpha.find(v); }
      if (it->second > 88) { fprintf(stderr, "demasiados tiempos distintos\n"); return 1; }
      enc += (char)('#' + it->second);  // caracteres imprimibles desde '#'
    }
    auto u = uniq.find(enc);
    if (u == uniq.end()) { int n = sigs.size(); uniq[enc] = n; sigs.push_back(enc); idx.push_back(n); }
    else idx.push_back(u->second);
  }
  printf("{\"f\":%u,\"a\":[", freq);
  for (size_t i = 0; i < alphaList.size(); i++) printf("%s%d", i ? "," : "", alphaList[i]);
  printf("],\"s\":[");
  for (size_t i = 0; i < sigs.size(); i++) {
    printf("%s\"", i ? "," : "");
    for (char c : sigs[i]) { if (c == '"' || c == '\\') putchar('\\'); putchar(c); }
    putchar('"');
  }
  printf("],\"i\":[");
  for (size_t i = 0; i < idx.size(); i++) printf("%s%d", i ? "," : "", idx[i]);
  printf("]}\n");
  return 0;
}
