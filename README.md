# Welt-Entdecker (Android, OpenStreetMap)

Kinderfreundliche Karten-App als Alternative zu Google Earth – mit Karten von OpenStreetMap.
**Kein Login, keine Werbung, kein Tracking, kein Street View.**

## Funktionen

- Vollbild-Karte mit OpenStreetMap-Kacheln (osmdroid, Mapnik)
- **3D-Globus**: Zoomt man weit genug raus (Zoom < 5), erscheint eine Erdkugel im Weltraum – ranzoomen oder + drücken holt die Karte zurück
- Große, einfache Knöpfe: **+ / −** (Zoomen) und **„Wo bin ich?"** (Standort wird nur lokal angezeigt)
- Zoomen auch per Zwei-Finger-Geste
- Ortssuche (Nominatim/OpenStreetMap) plus Kinder-Chips: 🦁 Zoo, 🌳 Spielplatz, 🏰 Schloss, 🎢 Freizeitpark, 🏙️ Hochhaus, 🏛️ Akropolis
- Bunte Marker für gefundene Orte („Du bist hier!"-Punkt für den eigenen Standort)
- Info-Dialog mit Datenschutz-Hinweisen

## Datenschutz

- Es werden **keine Daten gesammelt** – kein Login, keine Werbung, kein Analytics.
- `INTERNET` wird nur für OSM-Kacheln (tile.openstreetmap.org) und die Nominatim-Ortssuche verwendet.
- Standort (`ACCESS_FINE/COARSE_LOCATION`) wird nur auf Knopfdruck lokal auf der Karte angezeigt, nie gespeichert oder übertragen.
- Der Kachel-Cache liegt im privaten App-Speicher (`cacheDir`), daher ist **keine Speicher-Permission** nötig.

## Projektstruktur

```
.github/workflows/build-apk.yaml   # GitHub Actions: baut app-debug.apk bei Push auf main/master
app/
  build.gradle.kts                # AGP 8.5.2, Kotlin 2.0.21, Compose BOM 2024.06.00, osmdroid 6.1.20, minSdk 26 / target 34
  src/main/AndroidManifest.xml    # INTERNET, ACCESS_NETWORK_STATE, FINE/COARSE_LOCATION
  src/main/java/com/momro/weltentdecker/
    MainActivity.kt               # osmdroid initialisieren + Compose-Theme
    OsmDroidConfig.kt             # User-Agent + privater Kachel-Cache (cacheDir)
    ui/MapScreen.kt               # Karte + Suche + Chips + Standort-Knopf + Dialoge
    ui/theme/Theme.kt             # freundliches Material-3-Farbschema
    data/NominatimRepository.kt   # Ortssuche via Nominatim (OpenStreetMap)
    data/LocationHelper.kt        # einmaliger Standort via LocationManager
    ui/GlobeView.kt               # 3D-Globus (Bitmap-Rendering der Erdkugel)
  src/main/res/drawable-nodpi/earth.jpg  # Globus-Textur: NASA Blue Marble (Public Domain), via three.js examples
```

## Lokal bauen

```bash
gradle wrapper --gradle-version 8.7   # optional: erzeugt gradlew + gradle-wrapper.jar zum Committen
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Hinweis: Ist `gradlew` nicht committet, erzeugen die CI-Workflows den Wrapper vor dem Build automatisch.

## CI: APK bauen

- **GitHub Actions**: `.github/workflows/build-apk.yaml` baut bei jedem Push auf
  `main`/`master` (und manuell via *Run workflow*) die APK und lädt sie als Artifact hoch.
  Bei Tags `v*` wird zusätzlich ein GitHub-Release mit der Debug-APK erstellt.

## Karten- & Suchdaten

- Karten © [OpenStreetMap-Mitwirkende](https://www.openstreetmap.org/copyright) (ODbL)
- Kacheln: openstreetmap.org (Tile-Usage-Policy wird eingehalten, User-Agent ist in der App gesetzt)
- Ortssuche: [Nominatim](https://nominatim.openstreetmap.org/) (nur bei Nutzereingabe, max. 1 Anfrage/s)
- Globus-Textur: NASA Blue Marble (Public Domain), bezogen aus den [three.js-Beispielen](https://github.com/mrdoob/three.js/tree/dev/examples/textures/planets)
