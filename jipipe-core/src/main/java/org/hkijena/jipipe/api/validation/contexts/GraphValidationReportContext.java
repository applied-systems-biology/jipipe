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

package org.hkijena.jipipe.api.validation.contexts;

import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class GraphValidationReportContext extends JIPipeValidationReportContext {

    private final JIPipeGraph graph;

    public GraphValidationReportContext(JIPipeGraph graph) {
        this.graph = graph;
    }

    public GraphValidationReportContext(JIPipeValidationReportContext parent, JIPipeGraph graph) {
        super(parent);
        this.graph = graph;
    }

    @Override
    public String renderName() {
        return "Graph";
    }

    @Override
    public Icon renderIcon() {
        return UIUtils.getIconFromResources("actions/distribute-graph-directed.png");
    }
}
