# Changelog [DE]:

## 0.25.0 (23.07.2023)
- Implementierung einer lokalen Datenbank für eine robustere und leistungsfähigere Speicherung von Trips.
- Tripverlauf hinzugefügt, um vergangene Trips anzeigen zu können.
- Ladevorgänge können nun für bis zu 5 Minuten unterbrochen werden, bevor ein neuer Ladevorgang angelegt wird, sogar, wenn das Auto bewegt wurde.
- Icon hinzugefügt, das den aktuell ausgewählten Trip-Typ symbolisiert. Tippen auf die Trip-Statistiken wechselt zum nächsten Trip-Typ.
- Einstellung hinzugefügt, welche API den Verbindungsstatus in der Hauptansicht anzeigen soll.
- Der manuelle Trip kann nun direkt aus der Hauptansicht heraus zurückgesetzt werden.
- Der Datensatz für den HTTP-Webhook wurde an die neue Datenstruktur angepasst.
- Allgemeine Bugfixes und Stabilitätsverbesserungen.

## 0.24.1 (18.05.2023)
- Höhenunterschied in der Zusammenfassung hinzugefügt (experimentell)
- Falsche Einheitem im Diagramm der Zusammenfassung behoben
- Falsche Datumsangaben der Ladekurven in der Zusammenfassung behoben
- Bedingungen für Neustart-Benachrichtigungen verbessert
- Bedingungen für Energieerfassung angepasst (experimentell)
- Französische, portugisische und finnische Übersetzungen hinzugefügt

## 0.24.0 (02.04.2023)
- Mehrere Live-Daten-APIs hinzugefügt:
  - ABRP OTA Live Data
  - HTTP Webhook mit BasicAuth
- Standorterfassung mit Höhenverlauf hinzugefügt
- Neustart-Benachrichtigung nach Update, Reboot oder Absturz hinzugefügt
- Fahrzeugkonfiguration hinzugefügt
- Einstellungsmenü umstrukturiert
- Fehler behoben, der das aktualisieren der Einheiten in der Hauptansicht verhindert hat
- Laden und Speichern des Debug-Log wurde drastisch optimiert.
- Allgemeine Bugfixes und Stabilitätsverbesserungen.

## 0.23.0 (24.02.2023)
- Datenstruktur grundlegend überarbeitet, um die Stabilität und Sklaierbarkeit zu verbessern
- Dänische Übersetzung hinzugefügt
- "Über Car Stats Viewer" hinzugefügt, (inkl. grundlegende Überarbeitung der ReadMe mit Hinweisen zur Unterstützung und Mitwirkung)
- Es können neben dem manuellen Trip mehrere, automatisch zurückgesetzte Trips ausgewählt werden
- Verschiedene optische Anpassungen an den Diagrammen
- Einzelne Werte eines Diagramms können per Doppeltipp hervorgehoben werden
- Möglichkeit zum verschicken von Debug-Logs per SMTP (experimentell!)
- Stabilisierung des Verhaltens der Ladekurve, wenn die Ausführung der App zwischenzeitlich unterbrochen wird
- Optimierung der Fahrerablenkung
- Zahlreiche weitere Bugfixes und Stabilitätsverbesserungen.

## 0.22.1 (02.02.2023)
- Falsche Lokalisierung für Norwegisch behoben.

## 0.22.0 ()
- Tripzusammenfassung für den aktuellen Trip hinzugefügt. (Um einen Trip-Reset durchzuführen kann es erforderlich sein, die App neu zu installieren, da Änderungen im Manifest vorgnommen wurden)
- Stabilitätsverbresserungen bei der Abfrage und Verarbeitung von Fahrzeugdaten
- Schwedische Übersetzung hinzugefügt
- Norwegische Übersetzung hinzugefügt

## 0.21.2
- Zurücksetzen der Ladekurve beim erneuten Betreten des Fahrzeugs wurde behoben

## 0.21.1
- Hardcoded Text durch variablen String ersetzt

## 0.21.0 (25.01.2023)
- Optische Differenzierung der Ladekurvenansicht
- Mehrere Ladekurven in einem Trip gespeichert und können in den Einstellungen betrachtet werden
- Die Verbrauchsanzeige in der Hauptansicht ist fest auf 10 km eingestellt
- Die Verbrauchsanzeige in den Einstellungen kann mit einer Zoom-Geste vergrößert und seitlich verschoben werden.
- Die Verbrauchsanzeige in den Einstellungen beinhaltet nun Markierungen fürs Parken und Laden
- Grundstein für Tripzusammenfassung gelegt (Work in Progrss, momentan nur zum Zurücksetzen des Trips)
- Niederländische Übersetzung hinzugefügt

## 0.20.0 (18.01.2023)
- Wake Lock wieder entfernt.
- Debug Logging für Zündungsstatus wieder entfernt
- Der Energiezähler erfasst nur noch Werte, wenn das Fahrzug nicht in Fahrstufe P ist
- Trips werden auch nach vollständigem Beenden der App fortgesetzt
- Es kann nun durch den Verbrauchsplot gescrollt werden

## 0.19.2 (16.01.2023)
- Wake Lock hinzugefügt

## 0.19.1 (16.01.2023)
- Zusätzliches Debug-Logging für den Zündungsstatus
- Zeiterfassung korrigiert
- Untermenüs werden während der Fahrt deaktiviert

## 0.19.0 (15.01.2023)
- Die Ladekurve wird solange angezeigt, bis sie manuell geschlossen wird, nachdem das Ladekabel abgezogen wurde
- Die Lade- und Verbrauchskurven sind in ihrem jemweiligen Einstellungsmenü sichtbar
- Die Ladekurve ist nun (versuchsweise) blau eingefärbt

## 0.18.2 (14.01.2023)
- UI-Updates optimiert

## 0.18.1 (13.01.2023)
- Aktualisierungsrate der Hauptansicht reduziert

## 0.18.0
- Neues Icon
- Optische verbesserungen am Diagramm
- Neue Diagrammoptionen (vorerst beschränkt auf das Verbrauchsdiagramm)
- Allgemeine Stabilitätsverbresserung, insbesondere bei der Rundung und Zeiterfassung

## 0.17 (11.01.2023)
- Diagrammachsen aufgehellt
- Versuchsweise hellgrauer statt grüner Graph für die Geschwindigkeit
- Min- und Max-Grenzen für die Diagrammskalierung (WIP)
- Farbe und Grenzwerte für Balkenanzeigen angepasst
- Fahrzeit sollte nun korrekt funktionieren

## 0.16 (10.01.2023)
- Labels der Diagramme hervorgehoben
- 50km bei Diagrammen durch "Trip" ersetzt (Zeigt die gesamte Strecke seit dem letzten Reset in 10km-Schritten an)
- Zwischen den Distanzen kann durch ein Tippen auf das Diagramm durchgeschaltet werden
- Kleinere UI-Anpassungen
- Fahrzeit hinzugefügt
- Mittelwertberechnung in den Diagrammen korrigiert
- Allgemeine Stabilitätsverbessrungen

## 0.15 (08.01.2023)
- Ladekurve hinzugefügt.
- Experimentelles Layout erweitert
- Allgemeine Stabilitätsverbesserungen

## 0.14 (07.01.2023)
- Experimentelle Messwertansicht hinzugefügt

## 0.13 (06.01.2023)
- Achsenbeschriftung im Verbrauchsdiagramm mit sinnvollerer Einteilung
- Diagrammaktualisierung stabilisiert
- Service für die Fahrdatenerfassung ist jetzt (hoffentlich) stabiler und es sollten keine Werte mehr einfrieren
- Diagramme werden bei größeren Distanzen geglättet

## 0.12 (05.01.2023)
- Diagramm für die Geschwindigkeit hinzugefügt.
- Leichte Anpassungen am Logging.

## 0.11 (04.01.2023)
- Debug-Log hinzugefügt

## 0.10 (04.01.2023)
- Verbrauchsdiagramm verbessert

## 0.9 (02.01.2023)
- Experimentelle Verbrauchsanzeige hinzugefügt
- Layout für bessere Leserlichkeit angepasst

## 0.8 (31.12.2022)
- (Hoffentlich) Fix für einfrierende Werte bei Aktivierung der Kamera

## 0.7 (30.12.2022)
- Einstellungsmenü hinzugefügt
- Datenerfassung in Service ausgelagert

## 0.6 (24.12.2022)
- Benachrichtigung mit den aktuellen Fahrdaten
- Kein Neustart der App mehr erforderlich, wenn die Berechtigungen erteilt wurden

## 0.5 (21.12.2022)
- Deutsche Lokalisierung und Styling-Anpassungen
