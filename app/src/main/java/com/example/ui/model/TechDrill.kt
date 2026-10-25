package com.example.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class TechDrill(
    val id: String,
    val title: String,
    val stroke: String, // "Kraul", "Delfin", "Rücken", "Brust", "Allgemein"
    val description: List<String>,
)

object StandardTechDrills {
    val DEFAULT_DRILLS = listOf(
        TechDrill(
            id = "kraul_faust",
            title = "Faust-Schwimmen",
            stroke = "Kraul",
            description = listOf(
                "Schwimme Kraul mit geschlossenen Fäusten.",
                "Fördert das Gefühl für den Unterarm als Abdruckfläche.",
                "Fokus: Ellbogen hochhalten und mit dem gesamten Unterarm Wasser fassen.",
                "Variante: 25m Faust, 25m mit offenen Händen (spürbarer Unterschied!)."
            )
        ),
        TechDrill(
            id = "kraul_abschlag",
            title = "Abschlag-Übung",
            stroke = "Kraul",
            description = listOf(
                "Der führende Arm bleibt vorne gestreckt liegen.",
                "Erst wenn der ziehende Arm vorne abschlägt, beginnt der nächste Zug.",
                "Fördert Körperschwerpunkt, Gleitphase und hohe Wasserlage.",
                "Tipp: Auf gleichmäßige Körperrotation um die Längsachse achten."
            )
        ),
        TechDrill(
            id = "kraul_einarmig",
            title = "Kraul einarmig",
            stroke = "Kraul",
            description = listOf(
                "Ein Arm bleibt vorne gestreckt liegen oder am Körper angelegt.",
                "Der aktive Arm zieht sauber durch und führt die Überwasserphase aus.",
                "Atmung zur Seite des passiven oder aktiven Arms variieren.",
                "Fokus: Saubere Rotation und stabiler Rumpf."
            )
        ),
        TechDrill(
            id = "delfin_ruecken_kicks",
            title = "Delfin-Kicks in Rückenlage",
            stroke = "Delfin",
            description = listOf(
                "In Rückenlage in Stromlinienform (Streamline) abstoßen.",
                "Rhythmische Beinschläge aus der Hüfte und Rumpf ausführen.",
                "Fokus: Gleichmäßiger Druck nach oben und unten.",
                "Wichtig: Knie nicht zu stark beugen."
            )
        ),
        TechDrill(
            id = "brust_zwei_kicks",
            title = "Brust mit 2 Kicks pro Zug",
            stroke = "Brust",
            description = listOf(
                "Ein regulärer Armzug, gefolgt von zwei aufeinanderfolgenden Beinschlägen.",
                "Der erste Kick erfolgt beim Armzug, der zweite während der langen Gleitphase.",
                "Fördert die Gleiteffizienz und Ausdauer des Beinschlags.",
                "Achte auf gestreckte Fußspitzen in der Gleitphase."
            )
        ),
        TechDrill(
            id = "ruecken_flasche",
            title = "Rücken mit Wasserflasche auf Stirn",
            stroke = "Rücken",
            description = listOf(
                "Lege einen leeren Becher oder Schwamm auf die Stirn.",
                "Schwimme Rücken, ohne dass der Gegenstand herunterfällt.",
                "Trainiert eine ruhige, stabile Kopfhaltung im Rückenstil.",
                "Fokus: Kopf bleibt fixiert, der Körper rotiert darunter."
            )
        ),
        TechDrill(
            id = "brust_scheibenwischer",
            title = "Scheibenwischer (Sculling)",
            stroke = "Brust",
            description = listOf(
                "In Bauchlage mit leicht erhobenem Kopf die Hände vor der Brust auswärts und einwärts führen.",
                "Wasser spüren und Auftrieb durch kontinuierliche Achter-Bewegungen erzeugen.",
                "Schult das Gefühl für das Wasserfassen (Catch) in der ersten Zugphase."
            )
        ),
        TechDrill(
            id = "allgemein_streamline",
            title = "Streamline-Gleiten mit Beinen",
            stroke = "Allgemein",
            description = listOf(
                "Nach dem Abstoß von der Wand Hände übereinanderlegen und Arme hinter den Ohren fixieren.",
                "Maximales Ausgleiten ohne Zappeln, danach sanfter Kraul- oder Delfinkick.",
                "Schult den geringsten Wasserwiderstand beim Abstoß und Wenden."
            )
        )
    )
}
