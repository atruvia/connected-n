[![Build Status](https://github.com/fiduciagad/connected-n/actions/workflows/maven.yml/badge.svg)](https://github.com/fiduciagad/connected-n/actions/workflows/maven.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

## Connected n
Eine Umgebung zur Ausführung von Spielen, bei welchem es das Ziel ist, Steine in einer vertikalen, horizontalen oder diagonalen Reihe zu platzieren. Das Spiel wird von dem hier bereitgestellten Server ausgeführt. Die Spieler kommunizieren mit dem Server unter Verwendung eines einfachen textbasierten Protokolls über UDP.

Das Spielprinzip basiert auf [4 Gewinnt](https://de.wikipedia.org/wiki/Vier_gewinnt) bzw. [connect four](https://en.wikipedia.org/wiki/Connect_Four). Die Brettgröße sowie die Anzahl verbundener Steine kann konfiguriert werden. 

Die Idee für diese Coding Challenge wurde schamlos von Jan Ernsting (Mäxchen), welcher nach eigenen Angaben schamlos von Nicolas Botzet und Steven Collins (Poker) kopierte, kopiert. :-D

### Registrierung

Um sich beim 4Gewinnt-Server anzumelden, ist eine Registrierung nötig:

```
REGISTER;yourname
```

yourname darf aus beliebigen UTF-8 Zeichen bestehen aber maximal 30 Zeichen lang sein. 
Der Server antwortet im ok-Fall mit 
```
WELCOME;yourname
```
bzw. mit entsprechender Fehlermeldung, falls der Name nicht den Vorgaben entspricht (bereits von andererem Bot genutzt, zu lang, etc.)

### Ablauf des Turniers
Solange mindestens zwei Spieler registriert sind, wird Saison für Saion gespielt.
Innerhalb einer Saison spielt jeder Spieler, Partie für Partie, gegen alle anderen Spieler (=Spieltag).
Hierbei gibt es Hin- und Rückrunde, so dass jeder Spieler einmal Heimrecht (=erster Zug) hat.
Alle Partien eines Spieltags finden parallel statt. 

### Teilnahme an einer Saison
Wenn der Server eine neue Saison startet, schickt er eine Benachrichtung an alle
registrierten Spieler:

```
NEW SEASON;58ca8b44
```

Daraufhin muss man dem Server signalisieren, dass man teilnehmen möchte:

```
JOIN;58ca8b44
```

Der Teil ```58ca8b44``` ist ein variabler Token, der je nach Saison variiert.

### Einen Zug machen
Der Server schickt eine Nachricht an den Spieler, wenn ein neue Partie beginnt:
```
NEW GAME;gegner_name
```
Bist du selbst am Zug, wird der Server dich auffordern:
```
YOURTURN;758f30a5
```
Der Teil ```758f30a5``` ist wieder ein variabler Token.
Daraufhin kannst du mit
```
INSERT;spalte;758f30a5
```
Einen Zug ansagen. Hierbei ist ```spalte``` ein Wert zwischen ```0``` und ```6```.

Der Server informiert beide Spieler über alle gemachten Züge via:
```
TOKEN INSERTED;spieler_name;spalte
```

### Ende einer Partie
Eine Partie kann
- verloren werden (ungültiger Zug)
- gewonnen werden (vier Steine in einer vertikalen, horizontalen oder diagonalen Reihe)
- unentschieden enden (Spielbrett gefüllt aber kein Spieler hat vier Steine in Reihe)

Über das Ende jeder Partie informiert der Server beide Spieler via:
```
RESULT;ergebnis;spieler_name;grund
```
Das ```ergebnis``` hat die Ausprägungen LOSE|WIN|DRAW

### Gründe, warum eine Partie beendet wurde
Der Wert ```grund``` kann u.a folgende Ausprägungen haben: 
- CONNECTED_LINE (WIN)
- COLUMN_IS_FULL (LOSE)
- ILLEGAL_COLUMN_ANNOUNCED (LOSE)
- Fehlertext der Exception (Protokollfehler, Timeout, etc.) (LOSE)

### Timeouts
Antwortet ein Spieler ungültig oder nicht innerhalb von 250ms verliert er die Partie. Antwortet ein Spieler 10x in Folge nicht rechtzeitig, wird er deregistriert und muss sich daher erneut registrieren. 

### Umgebungsvariablen
Folgende Umgebungsvariablen können gesetzt werden
- PORT: Port auf welchem der Server lauscht (default 4446)
- TIMEOUT: Maximale Zeit in Millisekunden auf welche der Server auf eine Antwort des Clients wartet (default 250)
- DELAY: Wartezeit in Millisekunden zwischen Serverstart und erster Saison in welcher sich bereits Clients registrieren können (default 0)
- MINPLAYERS: Minimale Anzahl an Spielern welche angemeldet sein müssen, damit eine Saison gestartet wird (default 1)

## Setup des Servers
Um das Docker-Image mit dem Connected4-Server zu bauen, muss einmal 
```
sh build.sh
```

ausgeführt werden.
Anschließend kann mit 
```
cd docker
docker-compose up 
```
der Server gestartet werden.
