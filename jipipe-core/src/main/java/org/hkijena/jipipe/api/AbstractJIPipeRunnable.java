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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.run.JIPipeRunnable;

import java.util.UUID;

/**
 * Abstract base class for implementing {@link JIPipeRunnable}
 */
public abstract class AbstractJIPipeRunnable implements JIPipeRunnable {
    private final UUID uuid = UUID.randomUUID();

    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public UUID getRunUUID() {
        return uuid;
    }
}
