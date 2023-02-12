
![Banner](/docs/banner.png)

## Features:
 - View instantaneous power and consumption
 - View details of the current trip
 - Visualizes consumption, speed and state of charge along the trip
 - Visualizes and saves charge curves during a trip

## Planned:
 - Estimate range based on the current trip's consumption
 - Save and export trip data for further analysis 
 - More performance oriented real time drive stats
 
## Installing the app on a car
Currently the app is not available in the public Play Store. This is due to strict requirements by Google what kind of apps are allowed to be installed on cars. As of now the App is only available via internal test tracks which are limited to 100 users each. Currently there are no free internal test slots in any of the existing forks.

Once the app has reached a more finished state I will try to reach out to Polestar directly to potentially make it available through them. Just like ABRP or the Vivaldi browser which also do not comply with Google's restrictions.

## Support the development of the app:
Any form of support and feedback is very welcome! If you like the app and want to buy me a beer, feel free (but never obliged! This is a hobby for me) to use this link: https://paypal.me/Ixam

Other than that, contributions to the app are always welcome. Be it in form of code contributions, constructive feedback, bug reports or translations.

### Bug reports and feature requests:
If you have any bugs to report or want du suggest a new feature, please have a look at the GitHub issues. Don't forget to have a look at the existing issues to not create duplicates. Let me know if you support any of the existing feature request so I can adjust my priorities accordingly.

### Translations:
If you are using the app and miss the translation into your language, feel free to create a translation yourself and contribute on the development of the app. The default strings used in the app can be found here: 

[English strings](https://github.com/Ixam97/CarStatsViewer/blob/active_development/automotive/src/main/res/values/strings.xml)

Values containing `translatable="false"` shall not be translated.

## Contributors:
- Dario Bosshard (Consumption and charge curve diagrams)
- Dutch translation: DoubleYouEl
- Swedish translation: Robin Hellström, jschlyter
- Norwegian translation: Oddvarr
- FreshDave29
- rdu

## Links

Original impulse cam from here: [CarGearViewerKotlin](https://github.com/android/car-samples/tree/main/car-lib/CarGearViewerKotlin)

Discussion in the german Polestar forums: [Polestar Club](https://polestar.fans/t/car-stats-viewer-0-22-x/14653)

Discussion in the international Polestar forums: [Polestar Forum](https://www.polestar-forum.com/threads/car-stats-viewer-a-better-range-assistant.10261/)

---

## Changelog [DE]:

### 0.22.1 (02.02.2023)
- Falsche Lokalisierung für Norwegisch behoben.

### 0.22.0 ()
 - Tripzusammenfassung für den aktuellen Trip hinzugefügt. (Um einen Trip-Reset durchzuführen kann es erforderlich sein, die App neu zu installieren, da Änderungen im Manifest vorgnommen wurden)
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
