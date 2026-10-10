package fr.godox.figlekt.core

/** The layouts, how the FIGlet characters are fitted and smushed together. */

internal fun setLayout(option: Option, horizontalLayout: Layout?, verticalLayout: Layout?): Option {
    if ((horizontalLayout == null || horizontalLayout == Layout.DEFAULT)
            && (verticalLayout == null || verticalLayout == Layout.DEFAULT)) {
        return option
    }
    val newOption = option.copy()
    setHorizontalLayout(newOption, horizontalLayout)
    setVerticalLayout(newOption, verticalLayout)
    return newOption
}

internal fun setHorizontalLayout(option: Option, layout: Layout?) {
    if (layout == null || layout == Layout.DEFAULT) {
        return
    }
    val rule = option.rule
    when (layout) {
        Layout.FULL -> rule.setHorizontal(Layout.FULL, false, false, false, false, false, false)
        Layout.FITTED -> rule.setHorizontal(Layout.FITTED, false, false, false, false, false, false)
        Layout.SMUSH_U -> rule.setHorizontal(Layout.SMUSH_U, false, false, false, false, false, false)
        Layout.SMUSH_R -> rule.setHorizontal(Layout.SMUSH_R, true, true, true, true, true, true)
        else -> {}
    }
}

internal fun setVerticalLayout(option: Option, layout: Layout?) {
    if (layout == null || layout == Layout.DEFAULT) {
        return
    }
    val rule = option.rule
    when (layout) {
        Layout.FULL -> rule.setVertical(Layout.FULL, false, false, false, false, false)
        Layout.FITTED -> rule.setVertical(Layout.FITTED, false, false, false, false, false)
        Layout.SMUSH_U -> rule.setVertical(Layout.SMUSH_U, false, false, false, false, false)
        Layout.SMUSH_R -> rule.setVertical(Layout.SMUSH_R, true, true, true, true, true)
        else -> {}
    }
}

internal fun getSmushRule(oldLayout: Int, newLayout: Int?): Rule {
    val rules = HashMap<String, Int>()
    var layout = newLayout ?: oldLayout
    for (rule in RuleEnum.values()) {
        val key = rule.key
        val code = rule.code
        val value = rule.value
        if (layout >= code) {
            layout -= code
            if (!rules.containsKey(key)) {
                rules[key] = value
            }
        } else if (RuleEnum.HORIZONTAL_LAYOUT_SMUSH.key != key
                && RuleEnum.VERTICAL_LAYOUT_SMUSH.key != key) {
            rules[key] = 0
        }
    }
    val rule = Rule(
            Layout.get(rules[RuleEnum.HORIZONTAL_LAYOUT_SMUSH.key]),
            rules[RuleEnum.HORIZONTAL_1.key] == 1,
            rules[RuleEnum.HORIZONTAL_2.key] == 1,
            rules[RuleEnum.HORIZONTAL_3.key] == 1,
            rules[RuleEnum.HORIZONTAL_4.key] == 1,
            rules[RuleEnum.HORIZONTAL_5.key] == 1,
            rules[RuleEnum.HORIZONTAL_6.key] == 1,
            Layout.get(rules[RuleEnum.VERTICAL_LAYOUT_SMUSH.key]),
            rules[RuleEnum.VERTICAL_1.key] == 1,
            rules[RuleEnum.VERTICAL_2.key] == 1,
            rules[RuleEnum.VERTICAL_3.key] == 1,
            rules[RuleEnum.VERTICAL_4.key] == 1,
            rules[RuleEnum.VERTICAL_5.key] == 1
    )
    if (rule.horizontalLayout == null) {
        if (oldLayout == 0) {
            rule.horizontalLayout = Layout.FITTED
        } else if (oldLayout == -1) {
            rule.horizontalLayout = Layout.FULL
        } else if (rule.horizontal1
                || rule.horizontal2
                || rule.horizontal3
                || rule.horizontal4
                || rule.horizontal5
                || rule.horizontal6) {
            rule.horizontalLayout = Layout.SMUSH_R
        } else {
            rule.horizontalLayout = Layout.SMUSH_U
        }
    } else if ((rule.horizontalLayout == Layout.SMUSH_U
                    && rule.horizontal1)
            || rule.horizontal2
            || rule.horizontal3
            || rule.horizontal4
            || rule.horizontal5
            || rule.horizontal6) {
        rule.horizontalLayout = Layout.SMUSH_R
    }
    if (rule.verticalLayout == null) {
        if (rule.vertical1
                || rule.vertical2
                || rule.vertical3
                || rule.vertical4
                || rule.vertical5) {
            rule.verticalLayout = Layout.SMUSH_R
        } else {
            rule.verticalLayout = Layout.FULL
        }
    } else if ((rule.verticalLayout == Layout.SMUSH_U
                    && rule.vertical1)
            || rule.vertical2
            || rule.vertical3
            || rule.vertical4
            || rule.vertical5) {
        rule.verticalLayout = Layout.SMUSH_R
    }
    return rule
}
