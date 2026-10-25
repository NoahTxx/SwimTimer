package com.example.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class TechDrill(
    val id: String,
    val title: String,
    val stroke: String, // "Kraul", "Rücken", "Brust", "Delfin", "Wenden / Starts"
    val focusArea: String, // "Arme (Zug)", "Beine (Kick)", "Wasserlage & Rumpf", "Atmung & Koordination"
    val specificFocus: String? = null, // "Wasserfassen / Catch", "Druckphase", "Rückholphase", "Rhythmus / Timing", "Kick-Frequenz", "Unterwasser-Kick"
    val tips: List<String>, // "Worauf achten?"
    val goal: String, // "Warum machen wir das?"
)

object StandardTechDrills {
    val DEFAULT_DRILLS = listOf(
        // === KRAUL (10 Übungen) ===
        TechDrill(
            id = "kraul_faust",
            title = "Faust-Schwimmen",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Schwimme Kraul mit fest geschlossenen Fäusten.",
                "Halte den Ellbogen beim Wasserfassen hoch.",
                "Führe den Abdruck über die gesamte Unterarmfläche aus.",
                "Variante: 25m Faust + 25m normale Handhaltung."
            ),
            goal = "Schult den hohen Ellbogen und das Gefühl für den Unterarm als Abdruckfläche."
        ),
        TechDrill(
            id = "kraul_abschlag",
            title = "Abschlag-Schwimmen (Catch-up)",
            stroke = "Kraul",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Der führende Arm bleibt vorne gestreckt liegen.",
                "Erst wenn die Hand des ziehenden Arms vorne abschlägt, beginnt der nächste Zug.",
                "Achte auf eine aktive, gleichmäßige Längsachsen-Rotation des Körpers."
            ),
            goal = "Verbessert die Gleitphase, Stabilität im Rumpf und verhindert ein zu frühes Abziehen."
        ),
        TechDrill(
            id = "kraul_einarmig_vorn",
            title = "Einarmig Kraul (passiver Arm vorn)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Ein Arm bleibt vorne gestreckt im Wasser liegen.",
                "Der aktive Arm führt einen sauberen, vollständigen Kraulzug aus.",
                "Atme ruhig zur Seite des arbeitenden Arms.",
                "Achte auf kräftigen finalen Oberschenkel-Abdruck."
            ),
            goal = "Isoliert die Zug- und Druckphase und schult die stabile Wasserlage."
        ),
        TechDrill(
            id = "kraul_einarmig_seite",
            title = "Einarmig Kraul (passiver Arm an der Seite)",
            stroke = "Kraul",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Der passive Arm liegt flach am Oberschenkel an.",
                "Aktiver Arm zieht sauber durch und führt einehohe Überwasserphase aus.",
                "Rotation um die Längsachse intensiv spüren."
            ),
            goal = "Maximale Körperspannung und saubere Schulterrotation ohne Stützarm."
        ),
        TechDrill(
            id = "kraul_finger_drag",
            title = "Fingerspitzen-Schleifen (Finger Tip Drag)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Rückholphase",
            tips = listOf(
                "Ziehe die Fingerspitzen während der Überwasserphase leicht über die Wasseroberfläche.",
                "Halte den Ellbogen konstant als höchsten Punkt nah am Körper.",
                "Führe die Hand entspannt nach vorne."
            ),
            goal = "Erzwingt eine kompakte, kraftsparende Überwasserphase mit hohem Ellbogen."
        ),
        TechDrill(
            id = "kraul_sculling_front",
            title = "Scheibenwischer / Sculling (Front Catch)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "In Bauchlage Arme nach vorne strecken und kleine 8er-Bewegungen mit den Handflächen machen.",
                "Handgelenke leicht anwinkeln und den Druck auf das Wasser spüren.",
                "Mit oder ohne Beinschlag / Pullbuoy ausführen."
            ),
            goal = "Schult das feinfühlige Wasserfassen in der allerersten Zugphase."
        ),
        TechDrill(
            id = "kraul_6_kick_switch",
            title = "6-Kick-Switch (Seitenlage mit Wechsel)",
            stroke = "Kraul",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "6 Kraulbeinschläge in 90°-Seitenlage ausführen (unterer Arm gestreckt vorn).",
                "Nach 6 Kicks mit 1 dynamischen Armzug auf die andere Seite rotieren.",
                "Kopf schaut nach unten, nur zum Atmen kurz rotieren."
            ),
            goal = "Trainiert die optimale Seitenlage und den explosiven Hüftwechsel."
        ),
        TechDrill(
            id = "kraul_3er_zug_ellbogen",
            title = "3er-Zug mit Fokus auf hohem Ellbogen",
            stroke = "Kraul",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Konsequenter 3er-Atmenzug (links – rechts abwechselnd).",
                "Achte bei der Atmung darauf, den Kopf nicht zu hoch zu heben (1 Brillen-Glas im Wasser).",
                "Der Ellbogen führt die Bewegung nach oben an."
            ),
            goal = "Fördert symmetrische Atemtechnik, Nackenentspannung und ausgeglichene Wasserlage."
        ),
        TechDrill(
            id = "kraul_beine_seitenlage",
            title = "Kraulbeine in Seitenlage",
            stroke = "Kraul",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Schwimme Kraulbeine komplett auf der Seite (1 Arm vorn, 1 Arm am Körper).",
                "Kick erfolgt gleichmäßig aus der Hüfte nach vorn und hinten.",
                "Achte auf gestreckte Fußgelenke."
            ),
            goal = "Stärkt die Kern-Muskulatur und schult den wirksamen Beinschlag in Längsachsen-Rotation."
        ),
        TechDrill(
            id = "kraul_doggy_paddle",
            title = "Entenpaddeln (Doggy Paddle)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Arme ziehen unter Wasser nach hinten, werden aber unter Wasser wieder nach vorn geschoben.",
                "Keine Überwasserphase!",
                "Fokus voll auf das Anwinkeln des Unterarms (Early Vertical Forearm)."
            ),
            goal = "Entwickelt das Gefühl für das frühe vertikale Anstellen des Unterarms."
        ),

        // === RÜCKEN (7 Übungen) ===
        TechDrill(
            id = "ruecken_doppelarm",
            title = "Rücken-Doppelarm mit langem Gleiten",
            stroke = "Rücken",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Beide Arme führen gleichzeitig die Überwasserphase und den Unterwasserzug aus.",
                "Eintauchen mit dem kleinen Finger zuerst außerhalb der Schulterlinie.",
                "Nach dem Abdruck an den Oberschenkeln 2-3 Sekunden ausgleiten."
            ),
            goal = "Schult den kraftvollen Endabdruck und symmetrischen Krafteinsatz."
        ),
        TechDrill(
            id = "ruecken_schulter_touch",
            title = "Schulter-Touch-Rücken",
            stroke = "Rücken",
            focusArea = "Arme (Zug)",
            specificFocus = "Rückholphase",
            tips = listOf(
                "Beim Auftauchen der Hand kurz die eigene Schulter berühren.",
                "Erst danach den Arm senkrecht nach oben strecken und eintauchen.",
                "Kopf bleibt absolut ruhig im Wasser liegen."
            ),
            goal = "Verhindert ein seitliches Wegklappen des Arms bei der Rückholphase."
        ),
        TechDrill(
            id = "ruecken_becher",
            title = "Rücken mit Becher auf der Stirn",
            stroke = "Rücken",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Setze einen leeren Plastikbecher oder Schwamm mitten auf die Stirn.",
                "Schwimme Rücken, ohne dass der Gegenstand herunterfällt.",
                "Der Kopf bleibt stabil, der Körper rotiert darunter."
            ),
            goal = "Fixiert die ruhige Kopfhaltung als Dreh- und Angelpunkt im Rückenstil."
        ),
        TechDrill(
            id = "ruecken_streamline_kicks",
            title = "Rückenbeine in Stromlinienform (Streamline)",
            stroke = "Rücken",
            focusArea = "Beine (Kick)",
            specificFocus = "Unterwasser-Kick",
            tips = listOf(
                "Arme über dem Kopf in Streamline-Form Hände übereinanderlegen.",
                "Knie bleiben knapp unter der Wasseroberfläche.",
                "Kick erfolgt aus der Hüfte mit lockerem Fußgelenk nach oben."
            ),
            goal = "Erhöht die Beinstärke und die optimale Körperstreckung."
        ),
        TechDrill(
            id = "ruecken_einarmig",
            title = "Rücken einarmig mit Körperrotation",
            stroke = "Rücken",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "1 Arm liegt am Oberschenkel, der andere führt 4 Züge aus, dann Wechsel.",
                "Rotationswinkel der arbeitenden Schulter bis ca. 45° erzielen.",
                "Gleite mit hoher Wasserlage."
            ),
            goal = "Maximale Rumpfrotation und gezielte Schulter-Mobilisierung."
        ),
        TechDrill(
            id = "ruecken_ecken_pause",
            title = "Ecken-Schwimmen Rücken (Pause bei 12 Uhr)",
            stroke = "Rücken",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmische Steuerung",
            tips = listOf(
                "Stoppe den Rückholarm senkrecht zur Decke (12-Uhr-Position) für 2 Kicks.",
                "Danach zügig eintauchen und durchziehen."
            ),
            goal = "Bewusste Steuerung der Bewegung und Gleichgewichtskontrolle."
        ),
        TechDrill(
            id = "ruecken_daumen_kleinfinger",
            title = "Daumen raus, kleiner Finger rein",
            stroke = "Rücken",
            focusArea = "Arme (Zug)",
            specificFocus = "Rückholphase",
            tips = listOf(
                "Hand verlässt das Wasser mit dem Daumen zuerst.",
                "Arm dreht in der Luft, sodass der kleine Finger zuerst eintaucht."
            ),
            goal = "Perfektioniert die biomechanisch korrekte Handdrehung im Rückenarmzug."
        ),

        // === BRUST (7 Übungen) ===
        TechDrill(
            id = "brust_zwei_kicks",
            title = "2-Beinschläge-1-Armzug Brust",
            stroke = "Brust",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Ein regulärer Brustarmzug, danach zwei aufeinanderfolgende Beinschläge.",
                "Der zweite Kick erfolgt während der vollständig gestreckten Gleitphase.",
                "Körper bildet eine lange, waagerechte Linie im Wasser."
            ),
            goal = "Schult die lange, effiziente Gleitphase und die Beinschlag-Dominanz."
        ),
        TechDrill(
            id = "brust_delfinbeine",
            title = "Brust-Armzug mit Delfinbeinen",
            stroke = "Brust",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Kombiniere den engen Brust-Armzug mit einem flüssigen Delfinbeinschlag.",
                "Der Kick hilft, die Schultern beim Atmen sanft anzuheben.",
                "Fokus auf die schnelle Handzusammenführung vor der Brust."
            ),
            goal = "Fördert das schnelle Hineinschießen der Hände nach vorne und entlastet die Knie."
        ),
        TechDrill(
            id = "brust_glide_count",
            title = "Brust mit langem Gleiten (Glide Count)",
            stroke = "Brust",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Nach jedem Beinschlag gedanklich 1 - 2 - 3 zählen.",
                "Hände, Kopf und Beine bleiben in perfekter Streamline.",
                "Zähle die Züge pro Bahn und versuche sie zu reduzieren."
            ),
            goal = "Maximale Gleiteffizienz und Reduktion des Wasserwiderstands."
        ),
        TechDrill(
            id = "brust_sculling",
            title = "Brust-Scheibenwischer (Sculling Front)",
            stroke = "Brust",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Aus der Streckung die Hände schräg nach außen drücken (Außenzug), dann schnell nach innen führen.",
                "Der Ellbogen bleibt hoch und geht nicht hinter die Schulterlinie."
            ),
            goal = "Präzises Wasserfassen ohne Verlust des Vorwärtstriebs."
        ),
        TechDrill(
            id = "brust_ruecken_kicks",
            title = "Brustbeine in Rückenlage",
            stroke = "Brust",
            focusArea = "Beine (Kick)",
            specificFocus = "Fußgelenk-Flexibilität",
            tips = listOf(
                "In Rückenlage Brustbeine ausführen (Arme am Körper oder gestreckt).",
                "Die Knie dürfen beim Fersenzug NICHT aus dem Wasser schauen!",
                "Füße nach außen drehen und kreisförmig nach hinten zusammenziehen."
            ),
            goal = "Garantiert die korrekte Hüft- und Kniebeugung ohne Bremswirkung."
        ),
        TechDrill(
            id = "brust_fersen_touch",
            title = "Fersen-Touch-Brustbeine",
            stroke = "Brust",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Hände an das Gesäß legen.",
                "Bei jedem Beinschlag sollen die Fersen ganz kurz die Fingerspitzen berühren.",
                "Danach kraftvoll nach außen-hinten schließen."
            ),
            goal = "Sichert den vollen Bewegungsumfang des Brustbeinschlags."
        ),
        TechDrill(
            id = "brust_tauchzug",
            title = "Unterwasser-Tauchzug (Pull-out)",
            stroke = "Brust",
            focusArea = "Wenden / Starts",
            specificFocus = "Streamline",
            tips = listOf(
                "Nach Abstoß: Streamline-Gleiten -> 1 kleiner Delfinkick -> 1 langer Armzug bis an die Oberschenkel -> Gleiten -> Hände eng am Körper nach vorn schieben + 1 Brustkick -> Auftauchen."
            ),
            goal = "Perfektioniert den regelgerechten und schnellen Unterwasser-Tauchzug nach Start und Wende."
        ),

        // === DELFIN (6 Übungen) ===
        TechDrill(
            id = "delfin_biondi",
            title = "Biondi-Drill (Delfin)",
            stroke = "Delfin",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "1 Armzug Delfin mit Atmung nach vorn.",
                "Danach 3-4 kräftige Delfinkicks unter Wasser / an der Oberfläche in Gleitlage.",
                "Wiederhole den einzelnen sauberen Zug."
            ),
            goal = "Vermeidet Übermüdung und schult den kraftvollen Einzelschlag mit flüssigem Übergang."
        ),
        TechDrill(
            id = "delfin_1_1_2",
            title = "1-1-2 Delfin (1x links, 1x rechts, 2x beide)",
            stroke = "Delfin",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "1 Armzug nur links, 1 Armzug nur rechts, dann 2 vollständige Delfinzüge.",
                "Der Beinschlag läuft durchgehend im 2er-Rhythmus pro Armzug weiter."
            ),
            goal = "Vereinfacht die Atemkoordination und stabilisiert den Wellenrhythmus."
        ),
        TechDrill(
            id = "delfin_wellen_seitenlage",
            title = "Delfin-Wellen in Seitenlage",
            stroke = "Delfin",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Arme am Körper oder 1 Arm gestreckt.",
                "Führe die Rumpfwelle seitlich aus (Brustkorb und Hüfte bewegen sich rhythmisch).",
                "Gleichmäßiger Impuls nach vorn und hinten."
            ),
            goal = "Stärkt die seitliche Rumpfmuskulatur und fördert die ganzheitliche Körperwelle."
        ),
        TechDrill(
            id = "delfin_kraul_armzug",
            title = "Delfinbeine mit Kraul-Armzug",
            stroke = "Delfin",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Schwimme Kraul-Arme, aber mache pro Armzug exakt 2 Delfinbeinschläge.",
                "1. Kick beim Hände-Eintauchen, 2. Kick beim Druckende."
            ),
            goal = "Schult das exakte Timing der zwei Beinschläge pro Armzugzyklus."
        ),
        TechDrill(
            id = "delfin_streamline_underwater",
            title = "Delfinkicks unter Wasser (Streamline-Abstoß)",
            stroke = "Delfin",
            focusArea = "Beine (Kick)",
            specificFocus = "Unterwasser-Kick",
            tips = listOf(
                "Starke Streamline-Haltung nach dem Abstoß in Rücken- oder Bauchlage.",
                "6-8 Rumpfkicks unter Wasser ausführen, erst kurz vor dem Auftauchen den 1. Zug ansetzen."
            ),
            goal = "Maximiert die Wenden-Geschwindigkeit und die Unterwasser-Distanz."
        ),
        TechDrill(
            id = "delfin_ohne_atmung",
            title = "Delfin 4-6 Züge ohne Atmung",
            stroke = "Delfin",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Schwimme kurze Abschnitte (4-6 Züge) komplett ohne Kopfheben.",
                "Kopf bleibt neutral nach unten gerichtet.",
                "Achte auf flache Wasserlage."
            ),
            goal = "Sichert die flache, energieeffiziente Wasserlage ohne Störung durch die Atmung."
        ),

        // === WENDEN & STARTS (4 Übungen) ===
        TechDrill(
            id = "wende_kraul_rolle_bahn",
            title = "Kraul-Rolle mitten auf der Bahn",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Aus dem Kraulschwimmen ohne Wand eine kompakte Rolle vorwärts ausführen.",
                "Kinn fest an die Brust ziehen, Beine eng anhocken.",
                "Zügig auf dem Rücken ausgleiten und mit Kraulbeinen drehen."
            ),
            goal = "Nimmt die Angst vor der Wand und schult die schnelle Rollbewegung."
        ),
        TechDrill(
            id = "wende_streamline_abstoss",
            title = "Abstoß & Ausgleiten in Streamline (Mindestdistanz)",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Kräftiger Abstoß von der Wand mit angewinkelten Beinen.",
                "Hände übereinander, Kopf zwischen den Oberarmen fixiert.",
                "Gleiten ohne Bewegung, bis die Geschwindigkeit nachlässt."
            ),
            goal = "Findet den geringsten Wasserwiderstand nach jeder Wende."
        ),
        TechDrill(
            id = "wende_lagen_garnet",
            title = "Anschwimmen & Drehung Rücken-Brust-Wende",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Timing",
            tips = listOf(
                "Auf dem Rücken der Wand nähern, Anschlag mit der Hand an der Wand.",
                "Beine eng anziehen, über die Seite drehen und in Bauchlage kraftvoll abstoßen."
            ),
            goal = "Sichert den Regelsatz und die maximale Schnelligkeit bei der Lagen-Mittelwende."
        ),
        TechDrill(
            id = "start_gleitphase_delfin",
            title = "Startsprung-Gleitphase mit Unterwasser-Kick",
            stroke = "Wenden / Starts",
            focusArea = "Beine (Kick)",
            specificFocus = "Unterwasser-Kick",
            tips = listOf(
                "Explosiver Abstoß vom Startblock.",
                "Eintauchen durch ein 'Nadelöhr' (alle Körperteile tauchen an derselben Stelle ein).",
                "Nahtloser Übergang in schnelle Delfinkicks."
            ),
            goal = "Nützt den Startschwung optimal für die maximale Unterwasser-Geschwindigkeit."
        )
    )
}
