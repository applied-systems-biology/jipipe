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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;

public class JIPipeOutputDataSlot extends JIPipeDataSlot {

    private boolean skipCache;
    private boolean skipExport;
    private boolean skipGC;
    public JIPipeOutputDataSlot(JIPipeDataSlotInfo info, JIPipeGraphNode node) {
        super(info, node);
        if (info.getSlotType() != JIPipeSlotType.Output) {
            throw new IllegalArgumentException("Data slot info describes an output slot.");
        }
    }

    public JIPipeOutputDataSlot(JIPipeDataSlot other, boolean shallow, JIPipeProgressInfo progressInfo) {
        super(other, shallow, progressInfo);
    }

    public boolean isSkipGC() {
        return skipGC;
    }

    public void setSkipGC(boolean skipGC) {
        this.skipGC = skipGC;
    }

    public boolean isSkipCache() {
        return skipCache;
    }

    public void setSkipCache(boolean skipCache) {
        this.skipCache = skipCache;
    }

    public boolean isSkipExport() {
        return skipExport;
    }

    public void setSkipExport(boolean skipExport) {
        this.skipExport = skipExport;
    }
}
