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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;

public class CreateSpotDetectorNode extends JIPipeSimpleIteratingAlgorithm {

    private final CreateSpotDetectorNodeInfo nodeInfo;

    public CreateSpotDetectorNode(JIPipeNodeInfo info) {
        super(info);
        this.nodeInfo = (CreateSpotDetectorNodeInfo) info;
    }

    public CreateSpotDetectorNode(CreateSpotDetectorNode other) {
        super(other);
        this.nodeInfo = other.nodeInfo;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
