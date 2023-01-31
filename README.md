AAOS Car Stats Viewer
===========================================
Reads data from the VHAL of AAOS to provide more detailed car stats than the regular trip computer.

Currently being tested on Polestar 2

Inspired by https://github.com/android/car-samples/tree/main/car-lib/CarGearViewerKotlin

## Changelog [DE]:

### 0.22.0 (29.01.2023)
 - Tripzusammenfassung für den aktuellen Trip hinzugefügt. (Um einen Trip-Reset während der Fahrt durchzuführen kann es erforderlich sein, die App neu zu installieren, da Änderungen im Manifest vorgnommen wurden)
 - Stabilitätsverbresserungen bei der Abfrage und Verarbeitung von Fahrzeugdaten
 - Schwedische Übersetzung hinzugefügt
 - Norwegische Übersetzung hinzugefügt

### 0.21.2
 - Zurücksetzen der Ladekurve beim erneuten Betreten des Fahrzeugs wurde behoben

### 0.21.1
 - Hardcoded Text durch variablen String ersetzt

### 0.21.0 (25.01.2023)
 - Optische Differenzierung der Ladekurvenansicht
 - Mehrere Ladekurven in einem Trip gespeichert und können in den Einstellungen betrachtet werden
 - Die Verbrauchsanzeige in der Hauptansicht ist fest auf 10 km eingestellt
 - Die Verbrauchsanzeige in den Einstellungen kann mit einer Zoom-Geste vergrößert und seitlich verschoben werden.
 - Die Verbrauchsanzeige in den Einstellungen beinhaltet nun Markierungen fürs Parken und Laden
 - Grundstein für Tripzusammenfassung gelegt (Work in Progrss, momentan nur zum Zurücksetzen des Trips)
 - Niederländische Übersetzung hinzugefügt

### 0.20.0 (18.01.2023)
 - Wake Lock wieder entfernt.
 - Debug Logging für Zündungsstatus wieder entfernt
 - Der Energiezähler erfasst nur noch Werte, wenn das Fahrzug nicht in Fahrstufe P ist
 - Trips werden auch nach vollständigem Beenden der App fortgesetzt
 - Es kann nun durch den Verbrauchsplot gescrollt werden

### 0.19.2 (16.01.2023)
 - Wake Lock hinzugefügt

### 0.19.1 (16.01.2023)
 - Zusätzliches Debug-Logging für den Zündungsstatus
 - Zeiterfassung korrigiert
 - Untermenüs werden während der Fahrt deaktiviert

### 0.19.0 (15.01.2023)
 - Die Ladekurve wird solange angezeigt, bis sie manuell geschlossen wird, nachdem das Ladekabel abgezogen wurde
 - Die Lade- und Verbrauchskurven sind in ihrem jemweiligen Einstellungsmenü sichtbar
 - Die Ladekurve ist nun (versuchsweise) blau eingefärbt 

### 0.18.2 (14.01.2023)
 - UI-Updates optimiert

### 0.18.1 (13.01.2023)
 - Aktualisierungsrate der Hauptansicht reduziert

### 0.18.0
 - Neues Icon
 - Optische verbesserungen am Diagramm
 - Neue Diagrammoptionen (vorerst beschränkt auf das Verbrauchsdiagramm)
 - Allgemeine Stabilitätsverbresserung, insbesondere bei der Rundung und Zeiterfassung

### 0.17 (11.01.2023)
 - Diagrammachsen aufgehellt
 - Versuchsweise hellgrauer statt grüner Graph für die Geschwindigkeit
 - Min- und Max-Grenzen für die Diagrammskalierung (WIP)
 - Farbe und Grenzwerte für Balkenanzeigen angepasst
 - Fahrzeit sollte nun korrekt funktionieren

### 0.16 (10.01.2023)
 - Labels der Diagramme hervorgehoben
 - 50km bei Diagrammen durch "Trip" ersetzt (Zeigt die gesamte Strecke seit dem letzten Reset in 10km-Schritten an)
 - Zwischen den Distanzen kann durch ein Tippen auf das Diagramm durchgeschaltet werden
 - Kleinere UI-Anpassungen
 - Fahrzeit hinzugefügt
 - Mittelwertberechnung in den Diagrammen korrigiert
 - Allgemeine Stabilitätsverbessrungen

### 0.15 (08.01.2023)
- Ladekurve hinzugefügt.
- Experimentelles Layout erweitert
- Allgemeine Stabilitätsverbesserungen

### 0.14 (07.01.2023)
- Experimentelle Messwertansicht hinzugefügt

### 0.13 (06.01.2023)
- Achsenbeschriftung im Verbrauchsdiagramm mit sinnvollerer Einteilung
- Diagrammaktualisierung stabilisiert
- Service für die Fahrdatenerfassung ist jetzt (hoffentlich) stabiler und es sollten keine Werte mehr einfrieren
- Diagramme werden bei größeren Distanzen geglättet

### 0.12 (05.01.2023)
- Diagramm für die Geschwindigkeit hinzugefügt.
- Leichte Anpassungen am Logging.

### 0.11 (04.01.2023)
- Debug-Log hinzugefügt

### 0.10 (04.01.2023)
- Verbrauchsdiagramm verbessert

### 0.9 (02.01.2023)
- Experimentelle Verbrauchsanzeige hinzugefügt
- Layout für bessere Leserlichkeit angepasst

### 0.8 (31.12.2022)
- (Hoffentlich) Fix für einfrierende Werte bei Aktivierung der Kamera

### 0.7 (30.12.2022)
- Einstellungsmenü hinzugefügt
- Datenerfassung in Service ausgelagert

### 0.6 (24.12.2022)
- Benachrichtigung mit den aktuellen Fahrdaten
- Kein Neustart der App mehr erforderlich, wenn die Berechtigungen erteilt wurden

### 0.5 (21.12.2022)
- Deutsche Lokalisierung und Styling-Anpassungen
