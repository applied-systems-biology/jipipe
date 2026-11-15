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

package org.hkijena.jipipe.api.shortcuts;

import javax.swing.KeyStroke;
import java.util.List;

/**
 * The effective key binding of one shortcut action at a point in time: the
 * variants currently assigned to {@code actionId} and where they come from.
 * <p>
 * A variant is an ordered list of key strokes (a chord); the variants are
 * interchangeable. An {@link Source#UNASSIGNED} binding has no variants: the
 * action can be triggered from the UI, but no key produces it.
 *
 * @param actionId the id of the bound {@link JIPipeShortcutAction}
 * @param variants the effective binding variants, empty when unassigned
 * @param source   where the variants come from
 */
public record JIPipeShortcutBinding(String actionId, List<List<KeyStroke>> variants, Source source) {
    /**
     * Where a binding's variants come from.
     */
    public enum Source {
        /**
         * The action's default variants.
         */
        DEFAULT,
        /**
         * A user profile override.
         */
        OVERRIDE,
        /**
         * The action has no binding at all (the variants are empty).
         */
        UNASSIGNED
    }

    /**
     * @return true when this binding has no key (source {@link Source#UNASSIGNED})
     */
    public boolean isUnassigned() {
        return source == Source.UNASSIGNED;
    }
}
