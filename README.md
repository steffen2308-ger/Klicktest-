# Klicktest – Mauszeiger über anderen Apps

Native Android-App (ab Android 7.0), die einen Mauszeiger über allen Apps anzeigt. Ein kleines Touchpad steuert ihn, und an der Zeigerspitze werden echte Klicks in der App darunter ausgelöst. Zeiger und Touchpad lassen sich bis fast unsichtbar ausblenden.

Technisch ist das ein Android-Bedienungshilfe-Dienst (`AccessibilityService`) mit Gestenausführung. Klicktest liest keine Bildschirminhalte und hat keine Internet-Berechtigung.

## APK ohne Android Studio

Unter **Actions → Android APK** den neuesten erfolgreichen Lauf öffnen. Bei **Artifacts** `klicktest-debug-apk` herunterladen, ZIP entpacken und `app-debug.apk` installieren. Nach jedem Push auf `main` baut GitHub automatisch; alternativ **Run workflow**. Die APK ist mit einem öffentlichen Entwicklungsschlüssel signiert (`app/debug.keystore`, Passwort `android`), damit spätere Testversionen sich darüber installieren lassen.

## Einrichten

1. Klicktest öffnen und **Bedienungshilfe einschalten** antippen.
2. In der Liste **Klicktest-Mauszeiger** wählen und einschalten.
3. **Android 13 und neuer:** Ist der Schalter ausgegraut bzw. erscheint „Eingeschränkte Einstellung“, in Klicktest **App-Info öffnen** antippen, oben rechts **⋮ → Eingeschränkte Einstellungen zulassen** wählen und Schritt 2 wiederholen. Android sperrt Bedienungshilfen von Apps außerhalb des Play Stores standardmäßig.

## Bedienung

- **Wischen** auf dem Touchpad bewegt den Zeiger. Langsame Bewegungen sind fein, schnelle werden beschleunigt.
- **Tippen** klickt an der Zeigerspitze; der Zeiger leuchtet dabei kurz auf.
- **Halten** (½ Sekunde) löst einen langen Klick aus.
- **Mit zwei Fingern wischen** scrollt an der Zeigerposition.
- Die **blaue Leiste** oben am Touchpad ziehen verschiebt das Touchpad; die Position wird gespeichert.
- **✕** blendet Zeiger und Touchpad aus. Wieder einblenden in der App oder über die Bedienungshilfe-Taste von Android (Android 8+).
- Liegt der Zeiger über dem Touchpad selbst, wird das Touchpad für den Klick kurz durchlässig, sodass die App darunter getroffen wird.

## Einstellungen

In der App per Schieberegler:

- **Sichtbarkeit Mauszeiger** und **Sichtbarkeit Touchpad**: 5–100 %.
- **Zeiger-Geschwindigkeit**: 0,5× bis 4,0×.

Änderungen wirken sofort, auch während der Zeiger angezeigt wird.

## Grenzen

- Einige Bereiche erlauben keine Overlays oder eingeblendeten Klicks, etwa der Sperrbildschirm, Systemdialoge zur Berechtigungsvergabe und manche Banking-Apps.
- Mehrere Zeiger oder Tastatureingaben gibt es noch nicht.
