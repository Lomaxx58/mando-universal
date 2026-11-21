#!/usr/bin/env python3
"""
Empaqueta las señales de aire acondicionado generadas con gen.cpp (IRremoteESP8266)
en assets/db/acp/<id>.json y crea assets/db/ac_protocols.json con las capacidades
de cada protocolo y qué marcas lo usan.

Uso: python3 package.py <carpeta con los .json de gen> <app/src/main/assets/db>
"""
import hashlib, json, os, sys

MODES = ["COOL", "HEAT", "DRY", "FAN", "AUTO"]
FANS = ["AUTO", "LOW", "MEDIUM", "HIGH"]

def ix(p, m, t, f, s):
    return ((((p * 5 + m) * 15 + (t - 16)) * 4 + f) * 2 + s)

# Marca -> protocolos a probar, en orden (los de más arriba son los más habituales)
BRANDS = {
    "Gree": ["Gree", "Gree-YBOFB", "Kelvinator"],
    "Tosot": ["Gree", "Gree-YBOFB"], "Sinclair": ["Gree", "Gree-YBOFB"],
    "Cooper & Hunter": ["Gree"], "Soleus Air": ["Gree", "Neoclima"], "Kelvinator": ["Kelvinator", "Gree"],
    "Amana": ["Gree"], "Ekokai": ["Gree"], "Hyundai": ["Gree", "Midea", "Coolix"],
    "Infiniton": ["Gree", "Midea", "Coolix"],
    "Midea": ["Midea", "Coolix"], "Comfee": ["Midea", "Coolix"], "Kaysun": ["Midea", "Coolix"],
    "Mundoclima": ["Midea", "Coolix"], "Johnson": ["Midea", "Coolix"], "Danby": ["Midea"],
    "Lennox": ["Midea"], "Keystone": ["Midea"], "MrCool": ["Midea"], "Pioneer": ["Midea"],
    "Trotec": ["Trotec", "Midea"], "Duux": ["Trotec"],
    "Beko": ["Coolix"], "Tokio": ["Coolix"], "Fisher": ["Coolix"], "Kastron": ["Coolix"],
    "Toshiba": ["Toshiba", "Coolix"], "Carrier": ["Carrier", "Toshiba", "Midea", "Coolix"],
    "Airwell": ["Airwell", "Coolix"], "Bosch": ["Bosch", "Coolix"],
    "Fujitsu": ["Fujitsu-ARRAH2E", "Fujitsu-ARREB1E", "Fujitsu-ARDB1"],
    "General": ["Fujitsu-ARRAH2E", "Fujitsu-ARREB1E", "Fujitsu-ARDB1"],
    "Electra": ["Electra"], "AUX": ["Electra"], "AEG": ["Electra"], "Frigidaire": ["Electra"],
    "Subtropic": ["Electra"], "Electrolux": ["Electra", "TCL"], "DeLonghi": ["Delonghi", "Electra"],
    "Hisense": ["Kelon", "Midea", "Coolix"], "Kelon": ["Kelon"],
    "TCL": ["TCL"], "Daewoo": ["TCL"], "Teknopoint": ["TCL"],
    "Haier": ["Haier-YRW02", "Haier176", "Haier160", "Haier"], "Daichi": ["Haier-YRW02"],
    "LG": ["LG", "LG-AKB74955603", "LG-AKB75215403"], "General Electric": ["LG"],
    "Daikin": ["Daikin", "Daikin2", "Daikin216", "Daikin176", "Daikin128", "Daikin152", "Daikin64"],
    "Mitsubishi": ["Mitsubishi", "MitsubishiHeavy152", "Mitsubishi136", "Mitsubishi112", "MitsubishiHeavy88"],
    "Mitsubishi Electric": ["Mitsubishi", "Mitsubishi136", "Mitsubishi112"],
    "Mitsubishi Heavy": ["MitsubishiHeavy152", "MitsubishiHeavy88"],
    "Samsung": ["Samsung"],
    "Panasonic": ["Panasonic-DKE", "Panasonic-JKE", "Panasonic-NKE", "Panasonic-RKR", "Panasonic-CKP"],
    "Hitachi": ["Hitachi", "Hitachi1", "Hitachi424"],
    "Sharp": ["Sharp"], "Whirlpool": ["Whirlpool"], "Vestel": ["Vestel"], "Argo": ["Argo"],
    "Ecoclim": ["Ecoclim"], "Corona": ["Corona"], "Neoclima": ["Neoclima"], "Goodweather": ["Goodweather"],
    "Sanyo": ["Sanyo"], "Mirage": ["Mirage"], "Alaska": ["Teco"],
}

# Nombre que se enseña al usuario para cada protocolo
LABEL = {
    "Gree-YBOFB": "Gree (mando YBOFB)", "Fujitsu-ARRAH2E": "Fujitsu (AR-RAH2E)", "Fujitsu-ARREB1E": "Fujitsu (AR-REB1E)",
    "Fujitsu-ARDB1": "Fujitsu (AR-DB1)", "LG-AKB74955603": "LG (AKB74955603)", "LG-AKB75215403": "LG (AKB75215403)",
    "Haier-YRW02": "Haier (YR-W02)", "Haier176": "Haier (176 bits)", "Haier160": "Haier (160 bits)",
    "Daikin2": "Daikin (ARC477)", "Daikin216": "Daikin (ARC433B)", "Daikin176": "Daikin (BRC4C)",
    "Daikin128": "Daikin (BRC52B)", "Daikin152": "Daikin (ARC480)", "Daikin64": "Daikin (BRC4C153)",
    "Mitsubishi136": "Mitsubishi (PAR-FA)", "Mitsubishi112": "Mitsubishi (TG-TC)",
    "MitsubishiHeavy152": "Mitsubishi Heavy (RKX)", "MitsubishiHeavy88": "Mitsubishi Heavy (RLA)",
    "Panasonic-DKE": "Panasonic (DKE / PKR)", "Panasonic-JKE": "Panasonic (JKE)", "Panasonic-NKE": "Panasonic (NKE)",
    "Panasonic-RKR": "Panasonic (RKR)", "Panasonic-CKP": "Panasonic (CKP)", "Hitachi1": "Hitachi (RAS)",
    "Hitachi424": "Hitachi (RAK)", "Teco": "Teco / Alaska",
}

def caps(d):
    sig = lambda p, m, t, f, s: d["i"][ix(p, m, t, f, s)]
    # modos reales: los que no repiten la señal de otro modo
    modes, seen = [], []
    for m in range(5):
        block = tuple(sig(0, m, t, f, s) for t in range(16, 31) for f in range(4) for s in range(2))
        if block in seen:
            continue
        seen.append(block); modes.append(m)
    temps = {}
    for m in modes:
        row = [sig(0, m, t, 0, 0) for t in range(16, 31)]
        if len(set(row)) == 1:
            temps[MODES[m]] = None
            continue
        lo = 16
        while lo < 30 and row[lo - 16] == row[lo + 1 - 16]:
            lo += 1
        hi = 30
        while hi > 16 and row[hi - 16] == row[hi - 1 - 16]:
            hi -= 1
        temps[MODES[m]] = [lo, hi]
    m0 = modes[0]
    fans, fseen = [], []
    for f in range(4):
        v = sig(0, m0, 24, f, 0)
        if v in fseen:
            continue
        fseen.append(v); fans.append(FANS[f])
    swing = any(sig(0, m0, t, 0, 0) != sig(0, m0, t, 0, 1) for t in range(16, 31))
    return {"modes": [MODES[m] for m in modes], "temps": temps, "fans": fans, "swing": swing, "freq": d["f"]}

def main(src, dst):
    out = os.path.join(dst, "acp"); os.makedirs(out, exist_ok=True)
    protos, by_hash = {}, {}
    for fn in sorted(os.listdir(src)):
        if not fn.endswith(".json"):
            continue
        pid = fn[:-5]
        raw = open(os.path.join(src, fn)).read().strip()
        d = json.loads(raw)
        h = hashlib.sha1(raw.encode()).hexdigest()
        if h in by_hash:          # mismo contenido que otro protocolo: se reutiliza
            protos[pid] = dict(protos[by_hash[h]], label=LABEL.get(pid, pid))
            continue
        by_hash[h] = pid
        with open(os.path.join(out, pid + ".json"), "w") as f:
            f.write(raw)
        protos[pid] = dict(caps(d), file=pid, label=LABEL.get(pid, pid))
    brands = {}
    for b, ids in BRANDS.items():
        seen_files, keep = set(), []
        for i in ids:
            if i not in protos or protos[i]["file"] in seen_files:
                continue
            seen_files.add(protos[i]["file"]); keep.append(i)
        if keep:
            brands[b] = keep
    with open(os.path.join(dst, "ac_protocols.json"), "w") as f:
        json.dump({"protocols": protos, "brands": brands}, f, ensure_ascii=False, separators=(",", ":"))
    print(len(protos), "protocolos,", len(by_hash), "ficheros,", len(brands), "marcas")
    for p, c in protos.items():
        print(f"{p:20s} {c['modes']} {c['temps'].get(c['modes'][0])} {c['fans']} swing={c['swing']} file={c['file']}")

main(sys.argv[1], sys.argv[2])
