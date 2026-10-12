#!/usr/bin/env python3
"""
Reference photos for the catalogue, from Wikimedia Commons (or iNaturalist), with their credits.

For each animal: the lead image of its French Wikipedia article (or an override in photo_overrides.json),
its licence and author from Commons, a 500 px rendition saved as WebP in the app's assets, and one line in
core's catalog/photos.json. When Commons has no free image, or its image server asks to wait for minutes,
the species' iNaturalist photos are used instead. Only free licences pass (CC0, public domain, CC BY,
CC BY-SA); the app shows the author and licence on each card and lists them all in the credits.

    python3 -I tools/fetch_photos.py            # what is missing
    python3 -I tools/fetch_photos.py --redo ID  # one animal again (after editing the overrides)
    python3 -I tools/fetch_photos.py --recommons  # iNaturalist photos replaced by Commons' (waits as long as asked)

Wikimedia asks for a descriptive User-Agent and few requests at a time: one request at a time here,
Retry-After honoured, metadata asked 50 files per call.
"""
import argparse
import html
import http.client
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request
import urllib.error

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = [os.path.join(ROOT, "android/core/src/main/resources/catalog", f) for f in ("animals.json", "plants.json")]
CREDITS = os.path.join(ROOT, "android/core/src/main/resources/catalog/photos.json")
PHOTOS = os.path.join(ROOT, "android/app/src/main/assets/photos")
OVERRIDES = os.path.join(ROOT, "tools/photo_overrides.json")
CACHE = os.path.join(ROOT, "tools/out/photo_cache.json")

UA = "SafariNatureBuild/1.0 (children's nature app; https://github.com/jdelaub/dsmp-demo) python-urllib"
WIDTH = 500          # a standard Commons thumbnail step
MAX_SIDE = 560
QUALITY = 74
INAT_FREE = {"cc0": "CC0", "cc-by": "CC BY", "cc-by-sa": "CC BY-SA"}
# "Attribution" is Commons' own free licence that only asks for the author's name (e.g. Entomart's insects).
FREE = re.compile(r"^(cc0|public domain|pd|attribution|cc by(-sa)? [0-9.]+( [a-z]+)?|cc by(-sa)?)$", re.I)


class Throttled(Exception):
    """The server asks to come back later than we are willing to wait."""


def get(url, binary=False, tries=40, patience=None):
    for attempt in range(tries):
        req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept-Encoding": "identity"})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                data = r.read()
                time.sleep(0.8)
                return data if binary else json.loads(data.decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            if e.code in (429, 500, 502, 503, 504):
                wait = int(e.headers.get("Retry-After") or 0) or min(60, 5 * (attempt + 1))
                if patience is not None and wait > patience:
                    raise Throttled(url)
                print(f"  {e.code}, waiting {wait + 1}s", file=sys.stderr)
                time.sleep(wait + 1)
                continue
            raise
        except (urllib.error.URLError, TimeoutError, ConnectionError, http.client.HTTPException) as e:
            print(f"  network: {e}, retrying", file=sys.stderr)
            time.sleep(5 * (attempt + 1))
    raise RuntimeError(f"gave up on {url}")


def lead_files(titles, lang="fr"):
    """The Commons file of each article's lead image, 50 articles per request (redirects followed)."""
    out = {}
    for i in range(0, len(titles), 50):
        chunk = titles[i:i + 50]
        q = urllib.parse.urlencode({
            "action": "query", "format": "json", "formatversion": "2", "prop": "pageimages",
            "piprop": "name", "redirects": "1", "titles": "|".join(chunk),
        })
        r = get(f"https://{lang}.wikipedia.org/w/api.php?" + q) or {}
        qq = r.get("query", {})
        back = {}
        for n in qq.get("normalized", []):
            back[n["to"]] = back.get(n["from"], n["from"])
        for n in qq.get("redirects", []):
            back[n["to"]] = back.get(n["from"], n["from"])
        for pg in qq.get("pages", []):
            name = pg.get("pageimage")
            if not name or name.lower().endswith((".svg", ".gif", ".tif", ".tiff")):
                continue
            t = pg["title"]
            while t in back and back[t] != t:
                t = back[t]
            out[t] = name
            out[pg["title"]] = name
    return out


def commons_info(files):
    """Licence, author and a WIDTH px URL for up to 50 Commons files."""
    titles = "|".join("File:" + f for f in files)
    q = urllib.parse.urlencode({
        "action": "query", "format": "json", "formatversion": "2", "prop": "imageinfo",
        "iiprop": "url|extmetadata", "iiurlwidth": str(WIDTH), "titles": titles,
        "iiextmetadatafilter": "Artist|LicenseShortName|LicenseUrl|Credit",
    })
    r = get("https://commons.wikimedia.org/w/api.php?" + q)
    out = {}
    norm = {n["to"]: n["from"] for n in r.get("query", {}).get("normalized", [])}
    for p in r.get("query", {}).get("pages", []):
        ii = (p.get("imageinfo") or [None])[0]
        if not ii:
            continue
        md = ii.get("extmetadata", {})
        artist = html.unescape(re.sub(r"<[^>]+>", "", md.get("Artist", {}).get("value", ""))).strip()
        artist = re.sub(r"\s+", " ", artist)
        # Commons' guess for old uploads: "No machine-readable author provided. X assumed (based on copyright claims)."
        artist = re.sub(r"^No machine-readable author provided\.\s*(.+?) assumed \(based on copyright claims\)\.?$", r"\1", artist)
        artist = artist or "Auteur inconnu"
        name = norm.get(p["title"], p["title"]).removeprefix("File:")
        out[name] = {
            "file": name,
            "author": artist[:120],
            "license": md.get("LicenseShortName", {}).get("value", "").strip(),
            "licenseUrl": md.get("LicenseUrl", {}).get("value", "").strip(),
            "source": ii.get("descriptionurl", ""),
            "url": ii.get("thumburl") or ii.get("url"),
        }
    return out


def inaturalist(a):
    """A freely licensed iNaturalist photo of the species (500 px), or None: one of the species' curated photos
    first, else the most-liked research-grade photo of a living, wild animal (no traps, no fish market)."""
    r = get("https://api.inaturalist.org/v1/taxa?" + urllib.parse.urlencode({"q": a["latin"], "per_page": 10})) or {}
    results = r.get("results", [])
    want = a["latin"].lower().replace(" kl. ", " ")
    taxon = next((t for t in results if t.get("name", "").lower() == want), None) or \
        next((t for t in results if t.get("rank") in ("species", "hybrid", "subspecies")), None)
    if not taxon:
        return None
    time.sleep(1)
    detail = (get(f"https://api.inaturalist.org/v1/taxa/{taxon['id']}") or {}).get("results", [{}])[0]
    photos = [tp.get("photo", {}) for tp in detail.get("taxon_photos", [])]
    time.sleep(1)
    obs = get("https://api.inaturalist.org/v1/observations?" + urllib.parse.urlencode({
        "taxon_id": taxon["id"], "photo_license": ",".join(INAT_FREE), "quality_grade": "research",
        "photos": "true", "captive": "false", "term_id": 17, "term_value_id": 18,  # annotation: Alive
        "order_by": "votes", "per_page": 10,
    })) or {}
    photos += [ph for o in obs.get("results", []) for ph in o.get("photos", [])[:1]]
    for ph in photos:
        lic = INAT_FREE.get(ph.get("license_code") or "")
        url = ph.get("medium_url") or (ph.get("url") or "").replace("/square.", "/medium.")
        if not lic or not url:
            continue
        attribution = ph.get("attribution", "")
        m = re.match(r"^(?:\(c\)\s*)?(.+?),\s*(?:some|no) rights reserved", attribution)
        return {
            "file": f"iNaturalist {ph['id']}",
            "author": (m.group(1) if m else attribution)[:120] or "Auteur inconnu",
            "license": lic,
            "licenseUrl": "",
            "source": f"https://www.inaturalist.org/photos/{ph['id']}",
            "url": url,
        }
    return None


PLANT_GROUPS = {"tree", "shrub", "fruit", "flower", "seaweed", "fern", "crop"}
# Titles that say "not the living plant as a child meets it".
NOT_A_PLANT_PHOTO = re.compile(
    r"landscape|landschaft|paysage|panorama|seed|zaden|samen|graine|herbari|pollen|micro|section|coupe|illustration|"
    r"k[öo]e?hler|thom[ée]|flora_?von|plate|tafel|drawing|dessin|stamp|timbre|map|carte|distribution|wood_?grain|"
    r"insect|abeille|biene|bee_|butterfly|papillon|honey|market|march[ée]|dish|recipe|cuisine|"
    r"\bbuds?\b|knospe|bourgeon|seedling|germin|keim|holz|cross.?section|querschnitt|bark|rinde|écorce|basket|panier", re.I)


def looks_drawn(data):
    """A botanical plate or a specimen on paper: most of the picture is pale and colourless."""
    im = Image.open(io.BytesIO(data)).convert("RGB").resize((64, 64))
    pale = sum(1 for r, g, b in im.getdata() if min(r, g, b) > 200 and max(r, g, b) - min(r, g, b) < 30)
    return pale / 4096 > 0.4


def commons_search(a, avoid=()):
    """The best Commons photo of a plant: Quality Images first, then any photo; ranked by title. (meta, data) or None."""
    latin = a["latin"].replace(" × ", " ").lower()
    genus_species = " ".join(latin.split()[:2])
    for extra in (" incategory:Quality_images", " filetype:bitmap"):
        r = get("https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({
            "action": "query", "format": "json", "formatversion": "2", "list": "search", "srnamespace": "6",
            "srlimit": "12", "srsearch": a["latin"] + extra})) or {}
        titles = [x["title"].removeprefix("File:") for x in r.get("query", {}).get("search", [])]

        def score(t):
            low = t.lower().replace("_", " ")
            return (4 if genus_species in low else 0) - (10 if NOT_A_PLANT_PHOTO.search(t) else 0) \
                - (10 if low.endswith((".svg", ".gif", ".tif", ".tiff", ".pdf", ".djvu")) else 0)

        avoid_norm = {x.replace(" ", "_") for x in avoid}
        ranked = [t for t in sorted(titles, key=score, reverse=True) if score(t) >= 0 and t.replace(" ", "_") not in avoid_norm][:4]
        if not ranked:
            continue
        info = commons_info(ranked)
        for t in ranked:
            meta = info.get(t)
            if not meta or not meta.get("url") or not FREE.match(meta["license"]):
                continue
            data = get(meta["url"], binary=True, tries=200)
            if data and not looks_drawn(data):
                return meta, data
    return None


def save_webp(data, path):
    im = Image.open(io.BytesIO(data))
    im = im.convert("RGB")
    im.thumbnail((MAX_SIDE, MAX_SIDE), Image.LANCZOS)
    im.save(path, "WEBP", quality=QUALITY, method=6)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--redo", nargs="*", default=[])
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--recommons", action="store_true")
    ap.add_argument("--first", nargs="*", default=[], help="with --recommons: these animals first")
    ap.add_argument("--reinat", action="store_true", help="pick the iNaturalist photos again (curated, alive, wild)")
    ap.add_argument("--replants", nargs="*", help="plants: replace drawings, iNaturalist photos and these ids by Commons photos")
    args = ap.parse_args()

    animals = [a for f in CATALOG if os.path.exists(f) for a in json.load(open(f, encoding="utf-8"))]  # animals and plants
    overrides = json.load(open(OVERRIDES, encoding="utf-8")) if os.path.exists(OVERRIDES) else {}
    os.makedirs(PHOTOS, exist_ok=True)
    os.makedirs(os.path.dirname(CACHE), exist_ok=True)
    cache = json.load(open(CACHE, encoding="utf-8")) if os.path.exists(CACHE) else {}
    credits = {c["id"]: c for c in json.load(open(CREDITS, encoding="utf-8"))} if os.path.exists(CREDITS) else {}

    for rid in args.redo:
        cache.pop(rid, None)
        credits.pop(rid, None)
        p = os.path.join(PHOTOS, rid + ".webp")
        if os.path.exists(p):
            os.remove(p)

    if args.reinat:
        return reinat(animals, credits)
    if args.replants is not None:
        return replants(animals, credits, set(args.replants))
    if args.recommons:
        return recommons(animals, cache, credits, overrides, args.first)

    todo = [a for a in animals if a["id"] not in credits or not os.path.exists(os.path.join(PHOTOS, a["id"] + ".webp"))]
    if args.limit:
        todo = todo[: args.limit]
    print(f"{len(todo)} photos to fetch", file=sys.stderr)

    # 1. Which Commons file stands for each animal: the override, else the lead image of its French article,
    #    else of the article under its Latin name, else of the English article.
    need = [a for a in todo if a["id"] not in cache or cache[a["id"]] is None]
    for a in need:
        if a["id"] in overrides:
            cache[a["id"]] = overrides[a["id"]]
    for key, lang in (("wiki", "fr"), ("latin", "fr"), ("en", "en")):
        left = [a for a in need if not cache.get(a["id"])]
        if not left:
            break
        found = lead_files(sorted({a[key] for a in left}), lang)
        for a in left:
            cache[a["id"]] = found.get(a[key]) or found.get(a[key][:1].upper() + a[key][1:])
            if cache[a["id"]]:
                print(f"{a['id']}: {cache[a['id']]} ({key})", file=sys.stderr)
        json.dump(cache, open(CACHE, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    # 2. Licences and URLs, 50 at a time.
    wanted = sorted({cache[a["id"]] for a in todo if cache.get(a["id"])})
    info = {}
    for i in range(0, len(wanted), 50):
        info.update(commons_info(wanted[i:i + 50]))

    # 3. The pictures: Commons first; iNaturalist when Commons has nothing free or makes us wait for minutes.
    missing = []
    commons_open = True
    for a in todo:
        f = cache.get(a["id"])
        meta = info.get(f) if f else None
        data = None
        if meta and meta.get("url") and FREE.match(meta["license"]) and commons_open:
            try:
                data = get(meta["url"], binary=True, patience=30)
            except Throttled:
                print("  Commons images throttled: iNaturalist from now on", file=sys.stderr)
                commons_open = False
        if data and a["group"] in PLANT_GROUPS and looks_drawn(data):
            data = None
        if not data and a["group"] in PLANT_GROUPS:
            found = commons_search(a)
            if found:
                meta, data = found
        if not data:
            meta = inaturalist(a)
            data = get(meta["url"], binary=True) if meta else None
        if not data:
            missing.append((a["id"], "no free image"))
            continue
        save_webp(data, os.path.join(PHOTOS, a["id"] + ".webp"))
        credits[a["id"]] = {"id": a["id"], "file": meta["file"], "author": meta["author"],
                            "license": meta["license"], "licenseUrl": meta["licenseUrl"], "source": meta["source"]}
        json.dump(sorted(credits.values(), key=lambda c: c["id"]), open(CREDITS, "w", encoding="utf-8"),
                  ensure_ascii=False, indent=1)
        print(f"ok {a['id']} ({meta['license']})", file=sys.stderr)

    json.dump(sorted(credits.values(), key=lambda c: c["id"]), open(CREDITS, "w", encoding="utf-8"),
              ensure_ascii=False, indent=1)
    for rid, why in missing:
        print(f"MISSING {rid}: {why}")
    print(f"{len(credits)} photos with credits, {len(missing)} missing", file=sys.stderr)


def replants(animals, credits, also):
    """Plants whose picture is a drawing, a stranger's observation, or named here: a Commons photo instead."""
    done = 0
    for a in animals:
        if a["group"] not in PLANT_GROUPS:
            continue
        path = os.path.join(PHOTOS, a["id"] + ".webp")
        current = open(path, "rb").read() if os.path.exists(path) else None
        if not (a["id"] in also or current is None or "inaturalist" in credits.get(a["id"], {}).get("source", "")
                or looks_drawn(current)):
            continue
        # Named ones: their current picture was looked at and turned down, so the next best one.
        found = commons_search(a, avoid={credits.get(a["id"], {}).get("file", "")} if a["id"] in also else ())
        if not found:
            print(f"keep {a['id']}", file=sys.stderr)
            continue
        meta, data = found
        save_webp(data, path)
        credits[a["id"]] = {"id": a["id"], "file": meta["file"], "author": meta["author"],
                            "license": meta["license"], "licenseUrl": meta["licenseUrl"], "source": meta["source"]}
        json.dump(sorted(credits.values(), key=lambda c: c["id"]), open(CREDITS, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        done += 1
        print(f"ok {a['id']} ({meta['license']}) {meta['file']}", file=sys.stderr)
    print(f"replants done: {done}", file=sys.stderr)


def reinat(animals, credits):
    """iNaturalist photos chosen again with today's rules (see inaturalist())."""
    for a in animals:
        if "inaturalist" not in credits.get(a["id"], {}).get("source", ""):
            continue
        meta = inaturalist(a)
        if not meta or meta["source"] == credits[a["id"]]["source"]:
            continue
        data = get(meta["url"], binary=True)
        if not data:
            continue
        save_webp(data, os.path.join(PHOTOS, a["id"] + ".webp"))
        credits[a["id"]] = {"id": a["id"], **{k: meta[k] for k in ("file", "author", "license", "licenseUrl", "source")}}
        json.dump(sorted(credits.values(), key=lambda c: c["id"]), open(CREDITS, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        print(f"ok {a['id']} ({meta['license']})", file=sys.stderr)
    print("reinat done", file=sys.stderr)


def recommons(animals, cache, credits, overrides, first=()):
    """Commons' lead images are chosen by people for Wikipedia: better than a stranger's observation. Swap them in."""
    ids = [a["id"] for a in animals if "inaturalist" in credits.get(a["id"], {}).get("source", "")]
    ids.sort(key=lambda i: (i not in first, list(first).index(i) if i in first else 0))
    files = {i: overrides.get(i) or cache.get(i) for i in ids}
    files = {i: f for i, f in files.items() if f}
    info = {}
    wanted = sorted(set(files.values()))
    for i in range(0, len(wanted), 50):
        info.update(commons_info(wanted[i:i + 50]))
    done = 0
    for i, f in files.items():
        meta = info.get(f)
        if not meta or not meta.get("url") or not FREE.match(meta["license"]):
            print(f"keep {i}: {meta and meta['license']!r}", file=sys.stderr)
            continue
        data = get(meta["url"], binary=True, tries=200)
        if not data:
            continue
        save_webp(data, os.path.join(PHOTOS, i + ".webp"))
        credits[i] = {"id": i, "file": meta["file"], "author": meta["author"],
                      "license": meta["license"], "licenseUrl": meta["licenseUrl"], "source": meta["source"]}
        json.dump(sorted(credits.values(), key=lambda c: c["id"]), open(CREDITS, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        done += 1
        print(f"ok {i} ({meta['license']})", file=sys.stderr)
    print(f"{done} photos from Commons now, {len(ids) - done} still from iNaturalist", file=sys.stderr)


if __name__ == "__main__":
    main()
