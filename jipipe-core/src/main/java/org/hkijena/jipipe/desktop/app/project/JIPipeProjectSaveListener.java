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

package org.hkijena.jipipe.desktop.app.project;

/**
 * The completion signal of an automated project save (the save_project operation,
 * issue #1379). The save flows hand the listener the real outcome: the non-modal
 * JIPX save reports when its background run finished or was interrupted, the
 * synchronous legacy save reports before it returns. The listener is invoked exactly
 * once, on the event dispatch thread.
 */
public interface JIPipeProjectSaveListener {

    /**
     * Called when the save run finished.
     *
     * @param failure null when the project was written and re-verified; otherwise the
     *                error. {@code stage} tells whether the write itself failed
     *                ("write": the existing file was not changed) or the write
     *                succeeded but the re-check load of the saved file failed
     *                ("recheck": the file WAS replaced)
     * @param stage   the failure stage ("write" or "recheck"), only meaningful when
     *                failure is non-null
     */
    void onSaveFinished(Throwable failure, String stage);

    /**
     * Called when the save run was interrupted (cancelled) before completion.
     */
    void onSaveCancelled();
}
