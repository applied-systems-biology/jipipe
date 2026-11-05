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

package org.hkijena.jipipe.desktop.commons.dialogs;

/**
 * Presents a {@link JIPipeDesktopDialog} to the user and returns it once the
 * user has decided (modern dialog API, #1341).
 * <p>
 * The production implementation is {@link ModalPresenter}, which shows the
 * dialog modally. Tests install a no-op presenter so that dialogs can be
 * constructed and driven without ever being shown.
 */
interface DialogPresenter {
    /**
     * Shows the given dialog and returns it once the user has decided.
     *
     * @param dialog the dialog to show
     * @return the same dialog instance, after the user has decided
     */
    JIPipeDesktopDialog show(JIPipeDesktopDialog dialog);
}
