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

package org.hkijena.jipipe.api.cache;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;

import java.util.UUID;

public class JIPipeCacheClearOutdatedRun extends AbstractJIPipeRunnable {
    private final JIPipeCache cache;

    public JIPipeCacheClearOutdatedRun(JIPipeCache cache) {
        this.cache = cache;
    }

    @Override
    public String getTaskLabel() {
        return "Clear outdated cache items";
    }

    @Override
    public void run() {
        cache.clearOutdated(getProgressInfo());
        getProgressInfo().log(cache.toString());
    }

    public JIPipeCache getCache() {
        return cache;
    }
}
