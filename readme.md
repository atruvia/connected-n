## 4 Gewinnt

### Registrierung

Um sich beim 4Gewinnt-Server anzumelden, ist eine Registrierung nötig:

```
REGISTER;yourname
```

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
