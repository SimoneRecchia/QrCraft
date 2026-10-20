# QrCraft — guida

← [Torna al README](../README.md)

QrCraft trasforma un testo, un link o un libro in un QR code disegnato su una mappa di Minecraft. Metti la mappa in
una cornice e qualsiasi telefono la legge dal muro.

## Installazione

* **Server (multiplayer):** copia `qrcraft-<versione>.jar` nella cartella `mods` del server Forge 1.20.1 (Forge 47.4.0
  o successivo, sempre 47). I giocatori **non** devono installare niente: entrano con il client vanilla, o con Forge
  senza QrCraft, e vedono le mappe normalmente.
* **Singleplayer:** copia il jar nella tua cartella `mods`. Il comando funziona anche in un mondo senza trucchi.

## I comandi

| Comando | Cosa ottieni |
|---|---|
| `/qr <testo>` | il QR code su una mappa, direttamente in mano |
| `/qr poster <lato> <testo>` | un QR code grande su un muro di `lato`×`lato` mappe (fino a 4×4 di default) |
| `/qr color <codice> <sfondo> <testo>` | in due dei sedici colori delle tinture |
| `/qr poster <lato> color <codice> <sfondo> <testo>` | poster e colori insieme |
| `/qr book` | il Libro e penna (o il Libro scritto) che hai in mano |
| `/qr book color <codice> <sfondo>` | il libro, a colori |
| `/qr text <testo>` | per un testo che inizia con `poster`, `color`, `book`, `text` o `help` |
| `/qr` oppure `/qr help` | l'elenco dei comandi |

I colori si scrivono come nei comandi del gioco: `black`, `white`, `red`, `blue`, `light_blue`, `yellow`, `lime`,
`green`, `cyan`, `purple`, `magenta`, `pink`, `orange`, `brown`, `gray`, `light_gray`. Il tasto Tab li completa.

### Esempi

```
/qr https://github.com/SimoneRecchia/QrCraft
/qr Benvenuto nel server! Le regole sono al cartello accanto.
/qr poster 2 https://discord.gg/il-mio-server
/qr color blue white Base segreta: 120 64 -340
/qr WIFI:T:WPA;S:CasaMia;P:password123;;
```

L'ultimo, inquadrato con il telefono, propone di collegarsi alla rete Wi-Fi.

### Testi lunghi: il libro

La chat taglia i messaggi a 256 caratteri. Per un testo più lungo scrivilo in un **Libro e penna** (o firmalo), tienilo
in mano e usa `/qr book`. Le pagine vengono unite andando a capo, e la mod sceglie da sola il poster più piccolo su
cui il codice resta ben leggibile. Se il libro è firmato, le mappe prendono il suo titolo.

### Poster

`/qr poster 3 <testo>` ti dà 9 mappe. Ognuna ha nel nome il suo numero (`QR 1/9`, `QR 2/9`…) e nella descrizione la
riga e la colonna: la riga 1 è in alto e la colonna 1 a sinistra. Metti 3×3 cornici su un muro e appendi ogni mappa
al suo posto.

## Consigli per la scansione

* Usa una **cornice luminosa**: di notte, o in una stanza buia, una cornice normale scurisce la mappa.
* Il nero su bianco è il più leggibile. Se scegli due colori troppo simili, o un codice chiaro su sfondo scuro, la mod
  ti avvisa: non tutti gli scanner lo leggono.
* Se il codice è molto fitto (testi lunghi su una mappa sola), la mod te lo dice e ti suggerisce un poster.
* Avvicina il telefono allo schermo quanto basta perché la mappa riempia l'inquadratura.

## Le mappe

* Sono **mappe piene normali**, bloccate e senza posizione: non si aggiornano con il mondo e non si possono ingrandire
  al tavolo da cartografia. Si copiano come qualsiasi mappa, al tavolo da cartografia o al banco da lavoro.
* Lo **stesso testo** con gli stessi colori e la stessa misura ti dà **la stessa mappa**: le copie si impilano e il mondo
  non si riempie di file `map_N.dat`.
* I messaggi e i nomi delle mappe sono nella lingua del tuo client (italiano o inglese).

## Per chi gestisce il server

Le impostazioni sono salvate con il mondo in `serverconfig/qrcraft-server.toml` e valgono appena salvi il file, senza
riavviare.

| Impostazione | Default | Cosa fa |
|---|---|---|
| `permissionLevel` | `0` | chi può usare `/qr`: 0 tutti, 2 solo gli operatori |
| `cooldownSeconds` | `3` | i secondi da aspettare tra due QR code; operatori e proprietario del mondo singleplayer non aspettano |
| `consumeEmptyMaps` | `false` | ogni mappa costa una mappa vuota, come al tavolo da cartografia; in creativa è gratis |
| `maxPosterSize` | `4` | il poster più grande, in mappe per lato (fino a 8) |
| `errorCorrection` | `MEDIUM` | la correzione d'errore minima: `LOW`, `MEDIUM`, `QUARTILE`, `HIGH` |
| `reuseIdenticalMaps` | `true` | ridà le stesse mappe per lo stesso testo, misura e colori |
| `fallbackLanguage` | `en_us` | la lingua per chi ha il client in una lingua non tradotta (`en_us` o `it_it`) |

Da un blocco comandi: `/execute as @p run qr https://example.com`.
