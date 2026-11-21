#!/usr/bin/env python3
"""
Convierte la base de datos Flipper-IRDB (CC0, github.com/Lucaslhm/Flipper-IRDB)
en ficheros JSON compactos por categoría para la app.

Uso:  python3 tools/build_db.py /ruta/a/Flipper-IRDB app/src/main/assets/db

Formato de salida (un fichero por categoría):
{
  "brands": [
    {"n": "Samsung", "r": [            # mandos de esa marca, del más común al menos
        {"m": "BN59-01179A",           # modelo de mando / nombre del fichero
         "s": [                         # señales
            ["Power", "POWER", "Samsung32", 7, 2],          # protocolo: etiqueta, tecla, protocolo, dirección, comando
            ["Cool 23", "COOL_23", "RAW", 38000, "9000 4500 ..."]  # raw: frecuencia y tiempos en µs
         ]}
    ]}
  ]
}
"""
import json
import os
import re
import sys
from collections import Counter, defaultdict

CATEGORIES = {
    "tv": ["TVs", "Monitors"],
    "ac": ["ACs"],
    "tvbox": ["Cable_Boxes", "Streaming_Devices", "DVB-T", "TV_Tuner"],
    "projector": ["Projectors"],
    "audio": ["SoundBars", "Audio_and_Video_Receivers", "Speakers"],
    "fan": ["Fans"],
    "home": ["Air_Purifiers", "Heaters", "Humidifiers", "LED_Lighting"],
}

SUPPORTED = {"NEC", "NECext", "NEC42", "NEC42ext", "Samsung32", "RC5", "RC5X", "RC6",
             "SIRC", "SIRC15", "SIRC20", "Kaseikyo", "RCA", "Pioneer"}

# Tecla estándar -> nombres posibles (ya normalizados), por orden de preferencia.
KEYS = {
    "POWER": ["power", "pwr", "tv_pwr", "tv_power", "power_toggle", "on_off", "onoff", "on/off", "standby", "power_on_off", "pwr_toggle"],
    "POWER_ON": ["power_on", "on", "pwr_on"],
    "POWER_OFF": ["power_off", "off", "pwr_off"],
    "MUTE": ["mute", "vol_mute", "volume_mute", "mute_toggle"],
    "VOL_UP": ["vol_up", "vol+", "volume_up", "volume+", "vol_plus", "volup", "v+"],
    "VOL_DOWN": ["vol_dn", "vol_down", "vol-", "volume_down", "volume-", "vol_minus", "vol_dwn", "voldown", "v-"],
    "CH_UP": ["ch_next", "ch_up", "ch+", "channel_up", "channel+", "prog+", "p+", "chan_up", "pr+"],
    "CH_DOWN": ["ch_prev", "ch_down", "ch-", "channel_down", "channel-", "prog-", "p-", "chan_down", "pr-"],
    "UP": ["up", "up_arrow", "cursor_up", "arrow_up", "nav_up"],
    "DOWN": ["down", "down_arrow", "cursor_down", "arrow_down", "dwn", "dn", "nav_down"],
    "LEFT": ["left", "left_arrow", "cursor_left", "arrow_left", "nav_left"],
    "RIGHT": ["right", "right_arrow", "cursor_right", "arrow_right", "nav_right"],
    "OK": ["ok", "select", "enter", "center", "cursor_ok", "confirm"],
    "BACK": ["back", "return", "ret", "go_back", "esc", "escape"],
    "HOME": ["home", "smart_hub", "smart", "my_apps"],
    "MENU": ["menu", "settings", "setup", "setting", "setting_menu"],
    "EXIT": ["exit"],
    "INFO": ["info", "display", "status", "information"],
    "SOURCE": ["source", "input", "sources", "tv/av", "tv_av", "av", "input_select", "src"],
    "GUIDE": ["guide", "epg", "tv_guide"],
    "PLAY": ["play"], "PAUSE": ["pause"], "PLAY_PAUSE": ["play_pause", "play/pause", "play_pa", "playpause"],
    "STOP": ["stop"], "REW": ["rewind", "rew", "rwd", "fast_back", "fast_ba", "backward", "reverse"],
    "FF": ["fast_forward", "fastforward", "ff", "fwd", "forward", "fast_fwd", "fast_fo"],
    "RED": ["red"], "GREEN": ["green"], "YELLOW": ["yellow"], "BLUE": ["blue"],
    # Aire acondicionado / ventiladores
    "TEMP_UP": ["temp+", "temp_up", "temperature_up", "temp_plus", "t+"],
    "TEMP_DOWN": ["temp-", "temp_down", "temperature_down", "temp_minus", "t-"],
    "MODE": ["mode"],
    "FAN": ["fan", "fan_speed", "speed", "fanspeed"],
    "SWING": ["swing", "oscillate", "oscillation", "osc", "rotate"],
    "TIMER": ["timer"],
    "SLEEP": ["sleep"],
}
for d in range(10):
    KEYS[f"D{d}"] = [str(d), f"num_{d}", f"key_{d}", f"digit_{d}", f"btn_{d}", f"_{d}"]

NAME_TO_KEY = {}
for key, names in KEYS.items():
    for prio, n in enumerate(names):
        NAME_TO_KEY.setdefault(n, (key, prio))

AC_STATE = re.compile(r"^(cool|heat|dry|auto|fan)_?(\d{2})(?:_?c)?$")


def norm(name: str) -> str:
    n = name.strip().lower()
    n = re.sub(r"\s+", "_", n)
    n = re.sub(r"_+", "_", n)
    return n


def pretty(name: str) -> str:
    s = name.replace("_", " ").strip()
    return s[:1].upper() + s[1:] if s else s


def le_int(hexbytes: str) -> int:
    b = [int(x, 16) for x in hexbytes.split()]
    return sum(v << (8 * i) for i, v in enumerate(b))


def parse_ir(path):
    signals, cur = [], {}

    def flush():
        if cur.get("name") and cur.get("type"):
            signals.append(dict(cur))

    with open(path, encoding="utf-8", errors="ignore") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#"):
                if line.startswith("#"):
                    flush(); cur.clear()
                continue
            if ":" not in line:
                continue
            k, v = line.split(":", 1)
            k, v = k.strip(), v.strip()
            if k == "name":
                flush(); cur.clear()
            cur[k] = v
    flush()
    return signals


def convert_signal(sig):
    name = sig["name"].strip()
    if sig["type"].startswith("parsed"):
        proto = sig.get("protocol", "")
        if proto not in SUPPORTED:
            return None
        try:
            addr, cmd = le_int(sig["address"]), le_int(sig["command"])
        except (KeyError, ValueError):
            return None
        return [name, proto, addr, cmd]
    if sig["type"].startswith("raw"):
        try:
            freq = int(float(sig.get("frequency", "38000")))
            data = [abs(int(x)) for x in sig["data"].split()]
        except (KeyError, ValueError):
            return None
        if len(data) < 4 or sum(data) > 1_900_000 or not 20000 <= freq <= 60000:
            return None
        return [name, "RAW", freq, " ".join(map(str, data))]
    return None


def assign_keys(signals):
    """Asigna teclas estándar evitando duplicados (gana el nombre de mayor prioridad)."""
    best = {}
    for i, s in enumerate(signals):
        n = norm(s[0])
        if n in NAME_TO_KEY:
            key, prio = NAME_TO_KEY[n]
        else:
            m = AC_STATE.match(n)
            if not m:
                continue
            key, prio = f"{m.group(1).upper()}_{m.group(2)}", 0
        if key not in best or prio < best[key][1]:
            best[key] = (i, prio)
    keys = [""] * len(signals)
    for key, (i, _) in best.items():
        keys[i] = key
    return keys


def model_name(path, brand):
    stem = os.path.splitext(os.path.basename(path))[0]
    stem = re.sub(rf"^{re.escape(brand)}[_ -]?", "", stem, flags=re.I)
    return stem.replace("_", " ").strip() or "Mando"


def build(src, out):
    os.makedirs(out, exist_ok=True)
    summary = {}
    for cat, dirs in CATEGORIES.items():
        brands = defaultdict(list)
        for d in dirs:
            base = os.path.join(src, d)
            if not os.path.isdir(base):
                continue
            for brand in sorted(os.listdir(base)):
                bdir = os.path.join(base, brand)
                if not os.path.isdir(bdir):
                    continue
                pretty_brand = brand.replace("_", " ").strip()
                for root, _, files in os.walk(bdir):
                    for fn in sorted(files):
                        if not fn.lower().endswith(".ir"):
                            continue
                        path = os.path.join(root, fn)
                        sigs = [c for c in (convert_signal(s) for s in parse_ir(path)) if c]
                        # quitar señales repetidas con el mismo nombre
                        seen, uniq = set(), []
                        for s in sigs:
                            if norm(s[0]) in seen:
                                continue
                            seen.add(norm(s[0])); uniq.append(s)
                        if len(uniq) < 1:
                            continue
                        keys = assign_keys(uniq)
                        full = [[s[0], k] + s[1:] for s, k in zip(uniq, keys)]
                        brands[pretty_brand.lower()].append({
                            "brand": pretty_brand,
                            "m": model_name(path, brand),
                            "s": full,
                        })

        out_brands = []
        for _, remotes in sorted(brands.items()):
            # quitar mandos idénticos
            seen, uniq = set(), []
            for r in remotes:
                sig = json.dumps(sorted(tuple(map(str, s[2:])) for s in r["s"]))
                if sig in seen:
                    continue
                seen.add(sig); uniq.append(r)

            # ordenar: primero los mandos cuyo código de encendido comparten más ficheros
            def power_sig(r):
                for s in r["s"]:
                    if s[1] in ("POWER", "POWER_OFF", "POWER_ON"):
                        return json.dumps(s[2:])
                return None
            counts = Counter(p for p in map(power_sig, uniq) if p)
            uniq.sort(key=lambda r: (-(counts.get(power_sig(r), 0)), -len(r["s"]), r["m"]))
            name = uniq[0]["brand"]
            out_brands.append({"n": name, "r": [{"m": r["m"], "s": r["s"]} for r in uniq]})

        out_brands.sort(key=lambda b: b["n"].lower())
        path = os.path.join(out, f"{cat}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump({"brands": out_brands}, f, ensure_ascii=False, separators=(",", ":"))
        summary[cat] = (len(out_brands), sum(len(b["r"]) for b in out_brands), os.path.getsize(path))
    return summary


if __name__ == "__main__":
    s = build(sys.argv[1], sys.argv[2])
    for cat, (b, r, size) in s.items():
        print(f"{cat:10s} marcas={b:4d} mandos={r:4d} {size/1024:8.0f} KB")
