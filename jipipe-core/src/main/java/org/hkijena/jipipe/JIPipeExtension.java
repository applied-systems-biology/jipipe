/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 *
 */

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * Interface shared between all JIPipe extensions
 */
public interface JIPipeExtension extends JIPipeDependency {

    /**
     * Returns true if the extension can be activated.
     * This is a hard check that is applied during the JIPipe registration
     * Avoid to use this method if possible (e.g. by depending on an ImageJ update site), as the extension will not be loaded at all
     * @param report the report associated with this extension
     * @param progressInfo allows to write information into the startup log
     * @return if the extension can be activated
     */
    default boolean canActivate(JIPipeIssueReport report, JIPipeProgressInfo progressInfo) {
        return true;
    }

    /**
     * Returns true if the extension is activated.
     * Utilizes the extension ID (default)
     *
     * @return if the extension is active
     */
    default boolean isActivated() {
        if (isCoreExtension())
            return true;
        return JIPipe.getInstance().getExtensionRegistry().getActivatedExtensions().contains(getDependencyId());
    }

    /**
     * Returns true if the extension is scheduled to be activated upon the next restart
     *
     * @return if the extension is scheduled to be activated
     */
    default boolean isScheduledForActivation() {
        return JIPipe.getInstance().getExtensionRegistry().getScheduledActivateExtensions().contains(getDependencyId());
    }

    /**
     * Returns true if the extension is scheduled to be deactivated upon the next restart
     *
     * @return if the extension is scheduled to be deactivated
     */
    default boolean isScheduledForDeactivation() {
        return JIPipe.getInstance().getExtensionRegistry().getScheduledDeactivateExtensions().contains(getDependencyId());
    }

    /**
     * Returns whether this extension is a core extension and thus cannot be disabled by users.
     * Defaults to false.
     * Please be sure if you really want to mark an extension as core extension.
     *
     * @return if the extension cannot be disabled
     */
    default boolean isCoreExtension() {
        return false;
    }

    /**
     * Returns whether this extension is in beta testing.
     * If true, users should expect significant changes in how the extension and its nodes work.
     *
     * @return if the extension is in beta
     */
    default boolean isBeta() {
        return false;
    }
}
