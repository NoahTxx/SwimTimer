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
        // === KRAUL (20 Übungen) ===
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
                "Aktiver Arm zieht sauber durch und führt eine hohe Überwasserphase aus.",
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
        TechDrill(
            id = "kraul_finger_paddles",
            title = "Finger-Paddles Catch-Drill",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Nutze kleine Finger-Paddles (nur an den Fingern fixiert).",
                "Achte auf spürbaren Druckaufbau direkt beim Eintauchen.",
                "Vermeide ein Abknicken der Handgelenke nach oben."
            ),
            goal = "Sensibilisiert das späte und gezielte Wasserfassen im vorderen Drittel."
        ),
        TechDrill(
            id = "kraul_segel",
            title = "Segel-Kraul (Pullbuoy an den Knöcheln)",
            stroke = "Kraul",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Klemme die Pullbuoy zwischen deine Fußgelenke statt Oberschenkel.",
                "Schwimme Kraul ohne Beinschlag.",
                "Aktiviere Bauch- und Gesäßmuskeln für eine ebene Wasserlage."
            ),
            goal = "Erzwingt maximale Rumpfspannung und verhindert ein Absinken der Beine ohne Kick."
        ),
        TechDrill(
            id = "kraul_polo",
            title = "Wasserpolo-Kraul (Head-Up)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Schwimme Kraul mit dem Kopf komplett über der Wasseroberfläche.",
                "Der Blick bleibt starr nach vorne gerichtet.",
                "Erfordert eine höhere Frequenz und kräftigen Beinschlag."
            ),
            goal = "Stärkt Schulter- und Rückenmuskulatur und erhöht den Abdruckdruck."
        ),
        TechDrill(
            id = "kraul_zipper",
            title = "Reißverschluss-Drill (Zipper Drill)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Rückholphase",
            tips = listOf(
                "Streife mit dem Daumen während der Überwasserphase an der Körperseite entlang.",
                "Vom Oberschenkel über die Hüfte bis zur Achselhöhle führen.",
                "Erhält den Ellbogen als höchsten Punkt der Bewegung."
            ),
            goal = "Korrigiert ein zu breites Ausgreifen der Arme während der Überwasserphase."
        ),
        TechDrill(
            id = "kraul_3_3_3",
            title = "3-3-3 Wechselzug (Links, Rechts, Beidseitig)",
            stroke = "Kraul",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Schwimme 3 Züge nur links, 3 Züge nur rechts, dann 3 reguläre Kraulzüge.",
                "Beine schlagen durchgehend im Rhythmus mit.",
                "Achte auf gleichmäßige Gleitweiten auf beiden Seiten."
            ),
            goal = "Deckt Asymmetrien im Armzug und der Atmung auf und gleicht sie aus."
        ),
        TechDrill(
            id = "kraul_tennisball",
            title = "Kraul mit Tennisbällen in den Händen",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Halte in jeder Hand locker einen Tennisball umschlossen.",
                "Schwimme Kraul mit Fokus auf die Hebelwirkung des Unterarms.",
                "Nutze den gesamten Unterarm als Paddelfläche."
            ),
            goal = "Eliminiert das Anklammern mit den Fingern und verlagert den Druck auf den Unterarm."
        ),
        TechDrill(
            id = "kraul_beine_faust_board",
            title = "Kraulbeine mit Fäusten am Schwimmbrett",
            stroke = "Kraul",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Greife das Kickboard vorne nur mit geschlossenen Fäusten.",
                "Halte das Brett flach auf der Wasseroberfläche.",
                "Kick aus der Hüfte mit minimalem Kniewinkel."
            ),
            goal = "Erhöht die Beinstärke ohne Stützkraft der Finger am Brett."
        ),
        TechDrill(
            id = "kraul_hypoxie",
            title = "Atem-Pyramide (3-5-7-9 Hypoxie)",
            stroke = "Kraul",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmische Steuerung",
            tips = listOf(
                "Schwimme 25m mit 3er-Atmung, 25m mit 5er-Atmung, 25m mit 7er-Atmung.",
                "Halte Wasserlage und Zugfrequenz trotz reduzierter Atmung stabil.",
                "Konzentriere dich auf ruhiges, stetiges Ausatmen unter Wasser."
            ),
            goal = "Schult CO2-Toleranz, Lungenvolumen und mentale Kontrolle unter Belastung."
        ),
        TechDrill(
            id = "kraul_vertikalbeine",
            title = "Vertikaler Kraulbeinschlag im tiefen Wasser",
            stroke = "Kraul",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Halte dich im tiefen Wasser senkrecht aufrecht.",
                "Führe Kraulbeinschlag aus (Arme auf der Brust gekreuzt).",
                "Halte den Kopf für 30-45 Sekunden komplett über Wasser."
            ),
            goal = "Entwickelt enorme Beinkraft und kontinuierliche Kickfrequenz."
        ),
        TechDrill(
            id = "kraul_verzögertes_wasserfassen",
            title = "Verzögertes Wasserfassen (Delayed Catch)",
            stroke = "Kraul",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Nach dem Eintauchen die Hand kurz bewusst 1 Sekunde vorne stehen lassen.",
                "Erst dann den Ellbogen anstellen und explosiv nach hinten durchziehen.",
                "Sorgt für maximale Gleitstrecke pro Zug."
            ),
            goal = "Verhindert Hektik nach dem Eintauchen und vergrößert die Zuglänge."
        ),

        // === RÜCKEN (15 Übungen) ===
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
        TechDrill(
            id = "ruecken_brett_oberschenkel",
            title = "Rücken mit Brett auf den Oberschenkeln",
            stroke = "Rücken",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Lege ein Schwimmbrett flach auf deine Oberschenkel.",
                "Schwimme Rückenbeine, ohne dass die Knie das Brett nach oben stoßen.",
                "Die Fußspitzen kicken knapp unter der Oberfläche."
            ),
            goal = "Verhindert das schädliche 'Radfahren' mit zu stark angewinkelten Knien."
        ),
        TechDrill(
            id = "ruecken_faust",
            title = "Rücken-Zug mit Fäusten",
            stroke = "Rücken",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Ballen beide Hände zu Fäusten.",
                "Achte beim Unterwasserzug auf die Beugung des Ellbogens unter Wasser.",
                "Drücke das Wasser kraftvoll mit der Unterarmfläche ab."
            ),
            goal = "Schult den gebeugten Ellbogen beim Rückenzug unter Wasser."
        ),
        TechDrill(
            id = "ruecken_6_kick_switch",
            title = "Rücken 6-Kick-Switch in Seitenlage",
            stroke = "Rücken",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Schwimme Rückenbeine in 45°-Seitenlage (1 Schulter schaut aus dem Wasser).",
                "Nach 6 Beinschlägen 1 kräftigen Armzug machen und auf die andere Seite rotieren.",
                "Kopf bleibt starr zur Decke gerichtet."
            ),
            goal = "Stabilisiert die Rumpf-Rotation in der Rückenlage."
        ),
        TechDrill(
            id = "ruecken_deckenblick",
            title = "Rücken Deckenblick & Kinn-Fixierung",
            stroke = "Rücken",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Fixiere einen festen Punkt an der Schwimmhallendecke.",
                "Halte das Kinn leicht angehoben (1 Apfel passt zwischen Kinn und Brust).",
                "Die Ohren liegen leicht unter der Wasseroberfläche."
            ),
            goal = "Optimiere die Hydrodynamik und verhindert das Absinken der Hüfte."
        ),
        TechDrill(
            id = "ruecken_schnorchel",
            title = "Rücken mit Mittelschnorchel",
            stroke = "Rücken",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Verwende einen Frontschnorchel in Rückenlage (oder umgekehrt am Hinterkopf).",
                "Ermöglicht kontinuierliches Atmen ohne Wellen im Gesicht.",
                "Fokussiere dich rein auf die Arm- und Hüftbewegung."
            ),
            goal = "Ermöglicht ruhige Fokusstunden auf die saubere Schulterrotation."
        ),
        TechDrill(
            id = "ruecken_einarmig_streamline",
            title = "Rücken Einarmig mit Stützarm in Streamline",
            stroke = "Rücken",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Ein Arm bleibt über dem Kopf in Streamline gestreckt.",
                "Der andere Arm führt kontinuierlich Züge aus.",
                "Wechsle die Seiten nach jeweils 25 Metern."
            ),
            goal = "Gleichzeitiges Training von Streckung auf der einen und Abdruck auf der anderen Seite."
        ),
        TechDrill(
            id = "ruecken_vertikalbeine",
            title = "Vertikaler Rückenbeinschlag",
            stroke = "Rücken",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Halte dich im tiefen Becken senkrecht aufrecht.",
                "Führe Rücken-Kickbewegungen nach vorne und hinten aus.",
                "Hände aus dem Wasser halten (gekreuzt auf der Brust)."
            ),
            goal = "Stärkt Hüftbeuger und Oberschenkel für mehr Vortrieb auf dem Rücken."
        ),
        TechDrill(
            id = "ruecken_doppelschlag",
            title = "Rücken Doppelschlag-Impuls (Double Bump Kick)",
            stroke = "Rücken",
            focusArea = "Beine (Kick)",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Achte bei jedem Armeintauchen auf einen besonders betonten Kick der gleichen Seite.",
                "Ein zweiter kleinerer Kick erfolgt beim finalen Handabdruck.",
                "Synchronisiert die Beine perfekt mit der Rumpfrotation."
            ),
            goal = "Harmonisiert den Rythmus zwischen Beinschlag und Schulterrotation."
        ),

        // === BRUST (15 Übungen) ===
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
        TechDrill(
            id = "brust_kraulbeine",
            title = "Brust-Armzug mit Kraulbeinschlag",
            stroke = "Brust",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Führe den Brustarmzug aus, während die Beine durchgehend Kraulbeinschlag machen.",
                "Hält die Hüfte konstant hoch an der Wasseroberfläche.",
                "Konzentration voll auf den schnellen Zug und das Nach-Vorne-Schießen der Hände."
            ),
            goal = "Isoliert die Oberkörperbewegung und verhindert das Absinken der Hüfte."
        ),
        TechDrill(
            id = "brust_kinn_ball",
            title = "Brust mit Ball unter dem Kinn",
            stroke = "Brust",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Klemme einen kleinen Tennisball oder Schwamm zwischen Kinn und Brustbein.",
                "Schwimme Brust, ohne den Ball zu verlieren.",
                "Verhindert übermäßiges Kopf-In-Den-Nacken-Legen beim Atmen."
            ),
            goal = "Korrigiert die Kopfhaltung für eine hydrodynamisch neutrale Nackenposition."
        ),
        TechDrill(
            id = "brust_1_pull_2_kick_vertikal",
            title = "Brust 1-Pull-2-Kick Vertikal-Gleiten",
            stroke = "Brust",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "1 Armzug machen, 1. Beinschlag ausführen, kurz verharren, 2. Beinschlag abtauchend ausführen.",
                "Tauche mit dem Kopf leicht ab und nutze die Welle nach vorne.",
                "Maximale Kniebeugungskontrolle."
            ),
            goal = "Fördert die moderne Wellen-Brusttechnik mit Abwärtsbewegung des Oberkörpers."
        ),
        TechDrill(
            id = "brust_pullbuoy_arme",
            title = "Brust-Armzug mit Pullbuoy (Arm-Isolation)",
            stroke = "Brust",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Schwimme Brustzug mit Pullbuoy zwischen den Oberschenkeln (ohne Beinschlag).",
                "Achte auf das Schließen der Ellbogen vor der Brust.",
                "Schnelles Strecken der Hände nach vorne."
            ),
            goal = "Erhöht die Kraft im Brustarmzug und schult den schnellen Innen- und Wieder-Vor-Zug."
        ),
        TechDrill(
            id = "brust_schmaler_zug",
            title = "Schmaler Brust-Armzug (Narrow Pull)",
            stroke = "Brust",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Öffne die Hände beim Außenzug nur maximal schulterbreit.",
                "Gehe direkt in den kraftvollen Innenzug über.",
                "Hände bleiben immer vor den Schultern."
            ),
            goal = "Verhindert ein zu breites Durchreißen der Arme nach hinten."
        ),
        TechDrill(
            id = "brust_streamline_countdown",
            title = "Brust Streamline-Countdown",
            stroke = "Brust",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Nach dem Händestrecken und Beinschlag in Streamline verharren.",
                "Zähle auf jeder Bahn lautlos '1-2-3' vor dem nächsten Zug.",
                "Versuche die Bahnanzahl der Züge um 1-2 zu verringern."
            ),
            goal = "Maximiert den Streckungseffekt und den Gleit-Wirkungsgrad."
        ),
        TechDrill(
            id = "brust_unterwasser_delfinkick",
            title = "Brust-Abstoß mit getimtem Delfinkick",
            stroke = "Brust",
            focusArea = "Wenden / Starts",
            specificFocus = "Unterwasser-Kick",
            tips = listOf(
                "Nach Abstoß in langer Streamline verharren.",
                "Genau 1 kräftigen Delfinkick ausführen, BEVOR der große Unterwasser-Armzug beginnt.",
                "Einhalten der FINA/World Aquatics Wettkampfregeln."
            ),
            goal = "Perfektioniert das legale und maximale Tempo beim Tauchzug nach Start und Wende."
        ),
        TechDrill(
            id = "brust_hand_druck",
            title = "Brust Hand-Druck-Drill (Hands In Front Pressure)",
            stroke = "Brust",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Drücke beim Händestrecken die Handflächen kurz gegeneinander.",
                "Erst danach wieder flach parallel nach vorne schießen lassen.",
                "Hält die Oberkörperstreckung kompakt."
            ),
            goal = "Garantiert die vollständige Armstreckung am Ende jedes Zugzyklus."
        ),

        // === DELFIN / SCHMETTERLING (14 Übungen) ===
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
        TechDrill(
            id = "delfin_kraulbeine",
            title = "Delfin-Armzug mit Kraulbeinschlag",
            stroke = "Delfin",
            focusArea = "Arme (Zug)",
            specificFocus = "Rückholphase",
            tips = listOf(
                "Führe Schmetterling-Armzüge aus, aber schlage kontinuierlich Kraulbeine.",
                "Liefert stetigen Auftrieb von den Beinen.",
                "Erleichtert das flache Herüberholen der Arme über Wasser."
            ),
            goal = "Ermöglicht das Einüben des flachen Armschwungs ohne Beinschlagfrequenz-Stress."
        ),
        TechDrill(
            id = "delfin_vertikalbeine",
            title = "Vertikaler Delfinbeinschlag (Vertical Fly Kick)",
            stroke = "Delfin",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Halte dich senkrecht im tiefen Wasser mit Delfin-Rumpfkicks.",
                "Arme vor der Brust kreuzen.",
                "Bewegung muss aus der Brust/Hüfte kommen, nicht nur aus den Knien."
            ),
            goal = "Baut enorme Rumpf- und Bauchmuskelkraft für den Unterwasser-Kick auf."
        ),
        TechDrill(
            id = "delfin_unterwasser",
            title = "Delfin Unterwasser-Armzug (ohne Überwasserphase)",
            stroke = "Delfin",
            focusArea = "Arme (Zug)",
            specificFocus = "Druckphase",
            tips = listOf(
                "Unter Wasser ins Schlüsselloch-Muster nach hinten durchziehen.",
                "Hände nach dem Druckende unter Bauch und Brust wieder nach vorne schieben.",
                "Kein Ausheben der Arme über Wasser."
            ),
            goal = "Trainiert rein den Unterwasserzug und die S-Kurve ohne Schulterbelastung."
        ),
        TechDrill(
            id = "delfin_schnorchel",
            title = "Delfin mit Frontschnorchel",
            stroke = "Delfin",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Schwimme Delfin mit angewendetem Zentral-Schnorchel.",
                "Der Kopf bleibt völlig ruhig nach unten gerichtet.",
                "Achte auf das Rhythmusgefühl der Hüftwelle."
            ),
            goal = "Verhindert das Überstrecken des Nackens und stabilisiert die Wasserlage."
        ),
        TechDrill(
            id = "delfin_2_2_2",
            title = "2-2-2 Delfin (2x links, 2x rechts, 2x beide)",
            stroke = "Delfin",
            focusArea = "Atmung & Koordination",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "2 Armzüge nur links, 2 Armzüge nur rechts, 2 Armzüge mit beiden Armen.",
                "Achte beim Einarmzug auf das flache Überholen des aktiven Arms.",
                "Beinschlag läuft ununterbrochen im 2er-Takt."
            ),
            goal = "Stufenweiser Aufbau des Rhythmus bei reduzierter Kraftbelastung."
        ),
        TechDrill(
            id = "delfin_faust",
            title = "Delfin Faust-Schwimmen",
            stroke = "Delfin",
            focusArea = "Arme (Zug)",
            specificFocus = "Wasserfassen / Catch",
            tips = listOf(
                "Ballen beide Hände zu Fäusten.",
                "Achte auf den frühen Anstellwinkel des Unterarms.",
                "Nutze den Druck des Unterarms für den Auftrieb."
            ),
            goal = "Zwingt zur Nutzung des gesamten Unterarms für den Delfin-Druckaufbau."
        ),
        TechDrill(
            id = "delfin_pullbuoy",
            title = "Delfin mit Pullbuoy (Rumpf-Isolation)",
            stroke = "Delfin",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Klemme eine Pullbuoy fest zwischen die Oberschenkel.",
                "Schwimme Delfin-Arme ohne Beinschlag.",
                "Körperwelle entsteht rein durch Impulse aus dem Brustkorb."
            ),
            goal = "Isoliert den Armzug und schult den Brustkorb-Impuls für die Körperwelle."
        ),
        TechDrill(
            id = "delfin_rueckenlage_kicks",
            title = "Delfinkicks in Rückenlage (Streamline Back Kicks)",
            stroke = "Delfin",
            focusArea = "Beine (Kick)",
            specificFocus = "Unterwasser-Kick",
            tips = listOf(
                "In Rückenlage Arme in Streamline-Position halten.",
                "Führe kraftvolle Rumpfwelle aus.",
                "Achte darauf, dass die Knie nicht aus dem Wasser ragen."
            ),
            goal = "Schult den Kick nach oben und unten in vollkommener Körperstreckung."
        ),

        // === WENDEN & STARTS (10 Übungen) ===
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
            specificFocus = "Rhythmus / Timing",
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
        ),
        TechDrill(
            id = "wende_kraul_knie_schwamm",
            title = "Kraul-Rolle mit Schwamm zwischen den Knien",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Streamline",
            tips = listOf(
                "Klemme einen kleinen Schwamm oder Tennisball zwischen die Knie.",
                "Führe die Kraul-Rollwende an der Wand aus, ohne den Gegenstand zu verlieren.",
                "Erzwingt eine kompakte Beinhaltung beim Rollen."
            ),
            goal = "Verhindert ein Breittreten der Beine während der Rollwende."
        ),
        TechDrill(
            id = "wende_kraul_5m_anlauf",
            title = "Kraul-Wende Anlauf aus 5m (ohne Atmung)",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Starte 5m vor der Wand mit vollem Kraul-Tempo.",
                "Kein Atmen mehr auf den letzten 2 Armzügen vor der Rolle.",
                "Direktes Hineindrehen in die Rolle ohne Verzögerung."
            ),
            goal = "Beseitigt das Abbremsen und Zögern kurz vor der Beckenwand."
        ),
        TechDrill(
            id = "wende_ruecken_faehnchen",
            title = "Rücken-Wende Fähnchen-Countdown & Timing",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Zähle die Armzüge ab den 5m-Rücken-Fähnchen bis zur Rollwende (meist 4-5 Züge).",
                "Bei der letzten Drehung auf den Bauch genau 1 Kraulzug ausführen und sofort rollen.",
                "Kein Gleiten in Bauchlage!"
            ),
            goal = "Sichert exaktes Timing und Regelkonformität bei der Rücken-Rollwende."
        ),
        TechDrill(
            id = "wende_brust_schmetterling_pivot",
            title = "Brust/Schmetterling 2-Hand-Anschlag & Hüft-Pivot",
            stroke = "Wenden / Starts",
            focusArea = "Wasserlage & Rumpf",
            specificFocus = "Rhythmus / Timing",
            tips = listOf(
                "Simultaner Anschlag beider Hände auf gleicher Höhe an der Wand.",
                "Beine schnell unter den Körper ziehen (Pivot-Drehung).",
                "Einer der Arme löst sich eng am Körper vorbei nach vorne."
            ),
            goal = "Maximiert die Wendedrehgeschwindigkeit bei gleichzeitiger Einhaltung der Regeln."
        ),
        TechDrill(
            id = "start_block_reaktion",
            title = "Startblock-Reaktionssprung & Katapult",
            stroke = "Wenden / Starts",
            focusArea = "Beine (Kick)",
            specificFocus = "Kick-Frequenz",
            tips = listOf(
                "Einnahme der 'Fertig'-Haltung mit Schwerpunkt leicht nach vorne verlagert.",
                "Bei Signal explosives Ziehen mit den Händen am Block nach hinten.",
                "Kräftiger Beinabdruck nach vorne-oben."
            ),
            goal = "Minimiert die Reaktionszeit und maximiert den Abdruck-Impuls beim Start."
        ),
        TechDrill(
            id = "start_streamline_delfinwelle",
            title = "Streamline Unterwasser-Delfinwelle nach Abstoß",
            stroke = "Wenden / Starts",
            focusArea = "Beine (Kick)",
            specificFocus = "Unterwasser-Kick",
            tips = listOf(
                "Halte nach Start oder Wende vollkommene Streamline-Position.",
                "Führe 4-6 rhythmische Unterwasser-Delfinkicks aus, bevor du auftauchst.",
                "Erster Armzug bricht die Wasseroberfläche."
            ),
            goal = "Nutzt die höchste Geschwindigkeit der Unterwasserphase optimal aus."
        )
    )
}
