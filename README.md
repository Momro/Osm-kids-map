# Welt-Entdecker (Android, OpenStreetMap)

Kinderfreundliche Karten-App als Alternative zu Google Earth – mit Karten von OpenStreetMap.
**Kein Login, keine Werbung, kein Tracking, kein Street View.**

## Funktionen

- Vollbild-Karte mit **OpenTopoMap**-Kacheln (osmdroid) – topografische Landkarte mit Bergen, Tälern und Höhenlinien statt Stadt-Fokus
- **3D-Globus**: Die App startet auf einer Erdkugel im Weltraum (Höhenmodell mit Höhenfarben: Ozean blau, Tiefland grün, Berge braun/weiß); sie bleibt bis Zoom 7 sichtbar. Per Wischen seitlich drehbar (keine Eigenrotation), Tippen oder + drücken holt die Landkarte hervor
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
  src/main/res/drawable-nodpi/earth.png  # Globus-Textur: NASA-Höhenmodell (Public Domain) mit Höhenfarben, via three-globe examples
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
  `main`/`master` (und manuell via *Run workflow*) die APKs und lädt sie als Artifacts hoch.
  Bei Tags `v*` wird zusätzlich ein GitHub-Release erstellt.

## Signierung & Updates (wichtig)

Damit sich neue Versionen **ohne Deinstallieren** über die alte installieren lassen, muss jede
APK mit dem **gleichen** Key signiert sein. Debug-APKs aus der CI bekamen bisher von jedem
frischen Runner einen neuen Debug-Key (deshalb ging Updates nur nach Deinstallation).

Abhilfe: Die CI baut eine **signierte Release-APK** (`app-release.apk` im Release) mit einem
eigenen Keystore, das als GitHub-Secret hinterlegt ist. Einmalig anlegen (lokal mit JDK):

```bash
keytool -genkeypair -v -keystore welt-entdecker.p12 -storetype PKCS12 -alias weltentdecker \
  -keyalg RSA -keysize 2048 -validity 10000 -storepass MEIN_PASSWORT -keypass MEIN_PASSWORT \
  -dname "CN=Welt-Entdecker, OU=Apps, O=Momro, C=DE"
base64 -w0 welt-entdecker.p12 | gh secret set KEYSTORE_BASE64 --repo Momro/Osm-kids-map   # macOS: base64 -i
printf '%s' 'MEIN_PASSWORT' | gh secret set KEYSTORE_PASSWORD --repo Momro/Osm-kids-map
printf '%s' 'weltentdecker' | gh secret set KEY_ALIAS --repo Momro/Osm-kids-map
printf '%s' 'MEIN_PASSWORT' | gh secret set KEY_PASSWORD --repo Momro/Osm-kids-map
```

Solange das Secret fehlt, fällt die CI auf die Debug-APK zurück. **Hinweis:** Beim Wechsel von
einer alten Debug-APK (v1.3.0 und älter) auf die erste signierte Version muss die App **einmalig**
deinstalliert werden – danach funktionieren Updates ohne Deinstallation.

## Karten- & Suchdaten

- Karten © [OpenStreetMap-Mitwirkende](https://www.openstreetmap.org/copyright) (ODbL)
- Kacheln: [OpenTopoMap](https://opentopomap.org/) (© OpenTopoMap, CC-BY-SA; Tile-Usage-Policy wird eingehalten, User-Agent ist in der App gesetzt)
- Ortssuche: [Nominatim](https://nominatim.openstreetmap.org/) (nur bei Nutzereingabe, max. 1 Anfrage/s)
- Globus-Textur: NASA-Höhenmodell (Public Domain) mit Höhenfarben, bezogen aus den [three-globe-Beispielen](https://github.com/vasturiano/three-globe/tree/master/example/img)
