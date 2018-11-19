## 4 Gewinnt

### Registrierung

Um sich beim 4Gewinnt-Server anzumelden, ist eine Registrierung nötig:

```
REGISTER;yourname
```

### Ablauf des Turniers
Solange mindestens zwei Spieler registriert sind, wird Saison für Saion gespielt.
Innerhalb einer Sasion spielt jeder Spieler, Spiel für Spiel, gegen alle anderen Spieler (=Spieltag).
Hierbei gibt es Hin- und Rückrunde, so dass jeder Spieler einmal Heimrecht (=erster Zug) hat.
Die Spiele eines Spieltags finden parallel statt.

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
Der Server schickt eine Nachricht an den Spieler, wenn ein neues Spiel beginnt:
```
NEW GAME
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

### Ende eines Spiels
Ein Spiel kann
- verloren werden (ungültiger Zug)
- gewonnen werden (vier Steine in einer vertikalen, horizontalen oder diagonalen Reihe)
- unentschieden enden (Spielbrett gefüllt aber kein Spieler hat vier Steine in Reihe)

Über das Ende jedes Spiels informiert der Server beide Spieler via:
```
RESULT;ergebnis;spieler_name;grund
```
Das ```ergebnis``` hat die Ausprägungen LOSE|WIN|DRAW

### Timeouts
Antwortet ein Spieler ungültig oder nicht innerhalb von 250ms verliert er das Spiel
