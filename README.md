![Banner](/docs/banner.png)

## Features

- View instantaneous power and consumption
- View details of the current and past trips
- Visualizes consumption, speed and state of charge along the trip
- Visualizes and saves charge curves during a trip
- Live Data API for ABRP and HTTP Webhook

## [Changes](/CHANGES.md)

## [Building and installing Car Stats Viewer](/docs/BUILD.md)


## Installing the app on a car

Currently the app is not available in the public Play Store. This is due to strict requirements by Google what kind of apps are allowed to be installed on cars. As of now the App is only available via internal test tracks which are limited to 100 users each. Here you can find a listing of all available Test Tracks: [Polestar Club](https://polestar.fans/t/carstatsviewer-informationen/15027)

There are currently ongoing talks with Polestar to find ways of bringing the app to a broader audience. Though Polestar won't release the app themselves as a publisher, progress has been made for a public release by myself. The app is currently in an Closed Alpha Test which has passed the Google Review. Read more about it in [Issue #225](https://github.com/Ixam97/CarStatsViewer/issues/225).

## Using the webhook API

The app comes with the possibility to send data to a REST API endpoint. Details on this can be found in the [API documentation](/docs/APIDOC.md).

## Support the development of the app

Any form of support and feedback is very welcome! If you like the app and want to buy me a beer, feel free (but never obliged, this is a hobby for me) to use this link: https://paypal.me/Ixam

Please let me now if you do not want to be listed in the supporters list.

**Many thanks to everyone who has supported the development of Car Stats Viewer!**

<details>
<summary><h3>Supporters</h3></summary>

* Robin Hellström
* Benjamin Stegmann
* Horst Zimmermann
* Michael Roehn
* Man8ck
* Björn Befuß
* Peter Füllhase
* Lukas Bruckenberger
* Stefan Süssenguth
* Jürgen Bereuter
* Markus Enseroth
* Jacob Frostholm
* Christoffer Gennerud
* Samuel Lodyga
* Konstantinos Theiakos
* Oliver Charlton
* Dennis Berggren
* Erik Jan Rouwenhorst
* Ahti Hinnov
* Jonas Friedemann Heuer
* David Baumann

</details>

Other than that, contributions to the app are always welcome. Be it in form of code contributions, constructive feedback, bug reports or translations.


### Bug reports and feature requests

If you have any bugs to report or want du suggest a new feature, please have a look at the GitHub issues. Don't forget to have a look at the existing issues to not create duplicates. Let me know if you support any of the existing feature request so I can adjust my priorities accordingly.


### Translations

![Lokalise](/docs/lokalise.png)

If you are using the app and miss the translation into your language, feel free to create a translation yourself and contribute on the development of the app:

[Lokalise project for Car Stats Viewer](https://app.lokalise.com/public/7279689963f1e922c08f26.64130521/)

Currently the following languages are already available:

* :gb: English
* :de: German
* :netherlands: Dutch
* :sweden: Swedish
* :norway: Norwegian
* :denmark: Danish
* :fr: French
* :portugal: Portuguese (Portugal)
* :finland: Finnish
* :brazil: Portuguese (Brazil)

<details>
<summary><h3>Translators</h3></summary>

* Emacee
* Morten Kjærgaard
* Ian Mascarenhas
* Jakob Schlyter
* Oddvar Rasmussen
* DoubleYou
* 078emil
* Dominik Brüning
* Juha Mönkkönen
* Ossi Lahtinen
* J-P
* Laurent Vitalis
* Jere Kataja
* Pedro Leite
* Michele Campeotto
* Teribot
* GD
* Joachim Appinger
* Robin Hellström
* Silver Beard
* Eric van Engelen
* Ivan F. Martinez
* Luiz Pacifico Centa
* Ricardo Blauth
* Marcelo Fornereto
* Rafael Miranda
* Patrick Pimentel
* GuidoMa
* Mário Franco

</details>


### Rules for contributing code
<details>

<summary>Please read before contributing and creating Pull Requests!</summary>


* If you want to contribute code you are very welcome to. When creating a Pull Request, make sure to use [active_development](https://github.com/Ixam97/CarStatsViewer/tree/active_development). With the exception of hotfixes I will not merge any PRs into master since that branch is used by other forks to build the app bundle for the Play Store.
* Also describe what you want to archive with your code contribution. Uncommented PRs with no context on what they do are hard to understand and review. To make it easier for me to review and test your contribution make sure to [allow edits from maintainers](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/working-with-forks/allowing-changes-to-a-pull-request-branch-created-from-a-fork).

Please also be aware that I will not just include everything. It has to fit into my goals I want to archive with this app. I may just say "I don't feel it" (yes, I know, this joke is getting old 😅). It would be best to open an issue beforehand, describing what you want to see in the app and offer your help before starting to code. This way it is possible to exchange ideas before spending hours in coding.

</details>


## Contributors

* Dario Bosshard (Consumption and charge curve diagrams)
* Klaus Rheinwald (Testing and consulting)
* Jakob Schlyter (Lokalise setup and maintenance)
* FreshDave29
* rdu
* Jannick Fahlbusch


## Links

Original impulse cam from here: [CarGearViewerKotlin](https://github.com/android/car-samples/tree/main/car-lib/CarGearViewerKotlin)

Discussion in the german Polestar forums: [Polestar Club](https://polestar.fans/t/carstatsviewer-informationen/15027)

Discussion in the international Polestar forums: [Polestar Forum](https://www.polestar-forum.com/threads/car-stats-viewer-a-better-range-assistant.10261/)
