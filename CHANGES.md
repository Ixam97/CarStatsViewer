# Changelog [DE]:

## 0.28.1
 - Beginnend mit OTA P4.2.4 ist der Polestar 4 vollständig mit Car Stats Viewer kompatibel.
 - Das Autostart-Verhalten wurde verbessert.
 - Debugging- und Logging-Funktionen verbessert.

## 0.28.0
 - (Play Edition) Tripverlauf wieder eingefügt, allerdings mit begrenztem Informationsinhalt.
 - Komplette überarbeitung des User Interface für die Einstellungen und Trip Details. Diese nutzen nun horizontale Displays wie im Polestar 4 besser aus.
 - Kartenansicht zu den Trip-Details hinzugefügt (abhängig von der Implementierung im jeweiligen Track).
 - BITTE BEACHTEN: Polestar 3, Polestar 4 und Volvo EX30 liefern keine Leistungsdaten. Nicht alle App-Funktionen funktionieren wie gewohnt, bis dies von den Herstelletn behoben wurde!
 - Bekannte Fehler: Das Diagram in der Hauptansicht zeigt unter Umständen keine aktuellen Daten an. Dies wird mit der Überarbeitung dieser Ansicht in einem zukünftigen Update behoben. Die Diagramme in der Tripzusammenfassung sind davon nicht betroffen.

## 0.27.3
- Zusätzliche Fartbeginn-Bedingungen hinzugefügt (z.B. für den Volvo EX30).

## 0.27.2
- Fehlerbehandlung und Logging beim Wechsel von Trips hinzugefügt.

## 0.27.1
- (Play Edition) Das Legacy Dashboard und der Tripverlauf wurden aufgrund von Einschränkungen durch Google-Richtlinien vorübergehend deaktiviert.
- (Play Edition) Abstürze bei der Erstellung von Changelogs werden abgefangen.
- (Play Edition) Fehler behoben, bei dem das interagieren mit Benachrichtigungen zu Abstürzen führt.
- Abstürze beim Speichern von Tripdaten behoben.
- Absturzmeldungen verbessert.

## 0.27.0
- Implementierung des Automotive App Host für einen potentiellen öffentlichen Release im Play Store (nicht in klassischen Builds enthalten).
- Implementierung von Firebase Crashlytics. Kann in den Einstellungen deaktivert werden.
- Die Art und Weise, wie Changelogs generiert werden, wurde überarbeitet.
- Die Ladekurve wird in der Tripzusammenfassung nicht mehr bei 160 kW abgeschnitten.
- Der Datenbank-Upload läuft nun in einem eigenen Dienst. Es muss nicht länger der Tripverlauf geöffnet bleiben. Eine Benachrichtigung informiert über den Fortschritt.
- Der Datenbank-Upload enthält nun Ladekurven.
- Der Webhook füllt nun das Feld \"charged_soc\" am Ende eines Ladevorgangs aus.
- Datenbankzugriffe reduziert durch das verwenden der Debug-Einstellung bei der Speicherung von Logs.
- Debug-Logs werden nach 28 Tagen ein Mal täglich gelöscht.
- Fehler beim Anwenden des experimentellen Farbschemas behoben.

## 0.26.2
- Verbesserung der Diagramm-Steuerelemente in der Tripzusammenfassung
- Defektes Layout nach Polestar OTA P3.0.3 behoben
- Einige Layoutprobleme im Volvo Infotainment wurden behoben
        
## 0.26.1
- Fix für Android 12

## 0.26.0
- Experimentelles Farbschema hinzugefügt.
- Vereinfachte Auswahl der Sekundärachse.
- Der Datenbankupload wurde in Abschnitte unterteilt, um falsche Fehlermedungen zu vermeiden und den Fortschritt anzeigen zu können.

## 0.25.2
- Erweiterung der Webhook-API.
- Option für eine Handy-Erinnerung beim verlassen des Fahrzeugs hinzugefügt.
- Optimierungen bei der Lade- und Rendergeschwindigkeit der Statistikdiagramme.
- Fehler behoben, bei dem zum Teil falsche Ladevorgänge angezeigt wurden.
- Fehler behoben, der verhinderte, dass Meilen als Distanzeinheit verwendet werden.

## 0.25.1 (27.07.2023)
- Anpassungen an der UI, insbesondere in der Tripzusammenfassung.
- Die Weitergabe des Standortes kann nun für jede API einzeln festgelegt werden.
- Fahrzeugkonfiguration für MY24 hinzugefügt.
- Kleinere Bugfixes.

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
