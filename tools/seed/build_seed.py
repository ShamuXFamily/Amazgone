#!/usr/bin/env python3
"""Builds the offline seed catalog bundled in compose resources.

Downloads the full DummyJSON catalog and top CheapShark deals, stores the *raw API JSON*
(so the app reuses its normal DTO mappers) plus one thumbnail per product for first-launch
offline browsing. Run from repo root:  python3 tools/seed/build_seed.py
"""
import json, os, sys, urllib.request

OUT = "shared/src/commonMain/composeResources/files/seed"
UA = "Amazgone/1.0 (Kotlin Multiplatform demo shop)"
DUMMY_URL = "https://dummyjson.com/products?limit=0"
DEALS_URL = ("https://www.cheapshark.com/api/1.0/deals?pageSize=60&sortBy=Deal%20Rating"
             "&onSale=1&steamRating=75&AAA=0")

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read()

def main():
    os.makedirs(f"{OUT}/thumbs", exist_ok=True)
    dummy = json.loads(get(DUMMY_URL))
    deals = json.loads(get(DEALS_URL))
    with open(f"{OUT}/catalog.json", "w") as f:
        json.dump({"dummyjson": dummy, "cheapshark": deals}, f, separators=(",", ":"))
    thumbs = [(f"dummyjson_{p['id']}.webp", p["thumbnail"]) for p in dummy["products"]]
    thumbs += [(f"cheapshark_{d['gameID']}.jpg", d["thumb"]) for d in {d["gameID"]: d for d in deals}.values()]
    failed = 0
    for name, url in thumbs:
        path = f"{OUT}/thumbs/{name}"
        if os.path.exists(path):
            continue
        try:
            with open(path, "wb") as f:
                f.write(get(url))
        except Exception as e:  # keep going; the app falls back to a placeholder icon
            failed += 1
            print(f"thumb failed {name}: {e}", file=sys.stderr)
    print(f"products={len(dummy['products'])} deals={len(deals)} thumbs={len(thumbs) - failed}/{len(thumbs)}")

if __name__ == "__main__":
    main()
