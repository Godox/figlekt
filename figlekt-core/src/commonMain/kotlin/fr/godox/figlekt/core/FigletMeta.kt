package fr.godox.figlekt.core

/** The parsed content of a FIGlet font file. */
internal class Meta(
    val option: Option,
    val figletMap: Map<Int, Array<String>>,
)

/** The options of a FIGlet font file. */
internal class Option(
    val baseline: Int,
    val hardBlank: String,
    val height: Int,
    val oldLayout: Int,
    val printDirection: Int,
    val fullLayout: Int?,
    val codeTagCount: Int?,
    val rule: Rule,
) {

    fun copy(): Option {
        return Option(baseline, hardBlank, height, oldLayout, printDirection, fullLayout, codeTagCount, rule.copy())
    }
}

/** The smushing rules of a FIGlet font file. */
internal class Rule(
    var horizontalLayout: Layout?,
    var horizontal1: Boolean,
    var horizontal2: Boolean,
    var horizontal3: Boolean,
    var horizontal4: Boolean,
    var horizontal5: Boolean,
    var horizontal6: Boolean,
    var verticalLayout: Layout?,
    var vertical1: Boolean,
    var vertical2: Boolean,
    var vertical3: Boolean,
    var vertical4: Boolean,
    var vertical5: Boolean,
) {

    fun copy(): Rule {
        return Rule(horizontalLayout, horizontal1, horizontal2, horizontal3, horizontal4, horizontal5, horizontal6,
                verticalLayout, vertical1, vertical2, vertical3, vertical4, vertical5)
    }

    fun setHorizontal(horizontalLayout: Layout, horizontal1: Boolean, horizontal2: Boolean, horizontal3: Boolean,
                      horizontal4: Boolean, horizontal5: Boolean, horizontal6: Boolean) {
        this.horizontalLayout = horizontalLayout
        this.horizontal1 = horizontal1
        this.horizontal2 = horizontal2
        this.horizontal3 = horizontal3
        this.horizontal4 = horizontal4
        this.horizontal5 = horizontal5
        this.horizontal6 = horizontal6
    }

    fun setVertical(verticalLayout: Layout, vertical1: Boolean, vertical2: Boolean, vertical3: Boolean,
                    vertical4: Boolean, vertical5: Boolean) {
        this.verticalLayout = verticalLayout
        this.vertical1 = vertical1
        this.vertical2 = vertical2
        this.vertical3 = vertical3
        this.vertical4 = vertical4
        this.vertical5 = vertical5
    }
}

/** The smushing rule codes, as stored in the FIGlet font file header. */
internal enum class RuleEnum(val key: String, val code: Int, val value: Int) {
    VERTICAL_LAYOUT_SMUSH("vLayout", 16384, Layout.SMUSH_U.code),
    VERTICAL_LAYOUT_FITTED("vLayout", 8192, Layout.FITTED.code),
    VERTICAL_5("v5", 4096, 1),
    VERTICAL_4("v4", 2048, 1),
    VERTICAL_3("v3", 1024, 1),
    VERTICAL_2("v2", 512, 1),
    VERTICAL_1("v1", 256, 1),
    HORIZONTAL_LAYOUT_SMUSH("hLayout", 128, Layout.SMUSH_U.code),
    HORIZONTAL_LAYOUT_FITTED("hLayout", 64, Layout.FITTED.code),
    HORIZONTAL_6("h6", 32, 1),
    HORIZONTAL_5("h5", 16, 1),
    HORIZONTAL_4("h4", 8, 1),
    HORIZONTAL_3("h3", 4, 1),
    HORIZONTAL_2("h2", 2, 1),
    HORIZONTAL_1("h1", 1, 1);
}