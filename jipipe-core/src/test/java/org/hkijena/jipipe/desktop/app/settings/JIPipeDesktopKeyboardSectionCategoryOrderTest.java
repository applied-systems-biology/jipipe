/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the keyboard section's category display order (Task 9): the fixed
 * spec order first, then any remaining categories alphabetically, with the
 * actions inside each category keeping the registry's (category, name) order
 * that {@link JIPipeShortcutAction} lists arrive in.
 */
class JIPipeDesktopKeyboardSectionCategoryOrderTest {

    private static JIPipeShortcutAction action(String name, String category) {
        return JIPipeShortcutAction.builder("test." + category + "." + name, name, category).build();
    }

    @Test
    void fixedCategoriesComeFirstThenRemainingAlphabetically() {
        // The input is in the registry's all() order: category (alpha), then
        // name. The section must re-bucket into CATEGORY_ORDER (the spec
        // categories) and append the plugin categories alphabetically.
        List<JIPipeShortcutAction> all = List.of(
                action("q", "Alpha extras"),          // plugin category
                action("c", "Creation"),
                action("x", "Dock panels"),
                action("e", "Editor"),
                action("g", "Graph editor tools"),
                action("i", "Input/output"),
                action("l", "Layers"),
                action("a", "Menus"),
                action("b", "Menus"),
                action("n", "Navigation"),
                action("p", "Plugin tools"),          // plugin category
                action("z", "Running"),
                action("s", "Selection")
        );
        List<JIPipeShortcutAction> ordered = JIPipeDesktopKeyboardSection.categoryOrdered(all);
        List<String> expected = List.of(
                "g", // Graph editor tools (spec order 1)
                "s", // Selection (2)
                "c", // Creation (3)
                "i", // Input/output (4)
                "l", // Layers (5)
                "z", // Running (6)
                "n", // Navigation (7)
                "e", // Editor (8)
                "a", "b", // Menus (9), name order preserved
                "x", // Dock panels (10)
                "q", // then remaining categories alphabetically: Alpha extras
                "p"  // ...then Plugin tools
        );
        List<String> actual = ordered.stream().map(JIPipeShortcutAction::name).toList();
        assertEquals(expected, actual);
    }

    @Test
    void unknownFixedCategoriesAreSimplySkipped() {
        // A spec category with no actions must not produce an empty group, and
        // the (category, name) input order is preserved within a category.
        List<JIPipeShortcutAction> all = List.of(
                action("tool", "Graph editor tools"),
                action("only", "Selection")
        );
        List<JIPipeShortcutAction> ordered = JIPipeDesktopKeyboardSection.categoryOrdered(all);
        List<String> actual = ordered.stream().map(JIPipeShortcutAction::name).toList();
        assertEquals(List.of("tool", "only"), actual);
    }
}
