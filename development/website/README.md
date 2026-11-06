# Website-Entwicklung (nicht auf den Server hochladen)

Dieser Ordner enthält ausschließlich lokale Tests und Design-Demos. Produktive Dateien liegen weiterhin unter backend_server.

- demos/: Header-Demo als HTML, CSS und JavaScript.
- tests/: Backend- und Frontendtests; nutzt die installierten Pakete aus backend_server/node_modules.
- run-tests.js: startet alle Tests.
- preview.js: lokaler Vorschau-Webserver, nur auf 127.0.0.1 erreichbar; liefert demos und produktive public-Dateien aus. Keine echten Backend-/Loginfunktionen.

Im Ordner backend_server:

```bash
npm test
npm run preview
```

Demo: http://127.0.0.1:8766/header-demo.html
Offizielle Website-Vorschau: http://127.0.0.1:8766/main_website.html

Auf dem Server sind keine Tests oder Vorschau-Dateien erforderlich. npm start bleibt unverändert. npm test und npm run preview sind lokale Entwicklungsbefehle und benötigen diesen Ordner.

Falls bereits hochgeladen: Auf dem Server die drei alten public/header-demo.*-Dateien und die *.test.js-Dateien aus public und website_backend gezielt entfernen. Nur neu hochladen entfernt alte Dateien nicht. Produktive .js-Dateien ohne .test.js behalten.
