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

package org.hkijena.jipipe.api.compartments.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.colors.OptionalColorParameter;

import java.awt.*;

/**
 * A graph compartment output
 * Transfers data 1:1 from input to output
 */
@SetJIPipeDocumentation(name = "Compartment output", description = "Output of a compartment")
@ConfigureJIPipeNode()
public class JIPipeProjectCompartmentOutput extends IOInterfaceAlgorithm {

    private String outputSlotName;
    private boolean showInProjectOverview = true;
    private OptionalColorParameter projectOverviewColor = new OptionalColorParameter(Color.WHITE, false);

    /**
     * Creates a new instance.
     * Please do not use this constructor manually, but instead use {@link JIPipeGraphNode}'s newInstance() method
     *
     * @param info The algorithm info
     */
    public JIPipeProjectCompartmentOutput(JIPipeNodeInfo info) {
        super(info);

    }

    public JIPipeProjectCompartmentOutput(JIPipeProjectCompartmentOutput other) {
        super(other);
        this.outputSlotName = other.outputSlotName;
        this.showInProjectOverview = other.showInProjectOverview;
        this.projectOverviewColor = new OptionalColorParameter(other.projectOverviewColor);
    }

    @Override
    @JIPipeParameter(value = "jipipe:node:name", uiOrder = -9999, pinned = true, functional = false, hidden = true)
    @SetJIPipeDocumentation(name = "Name", description = "Custom algorithm name.")
    public String getName() {
        return outputSlotName;
    }

    @JIPipeParameter(value = "jipipe:compartment:output-slot-name", hidden = true)
    public String getOutputSlotName() {
        return outputSlotName;
    }

    @JIPipeParameter("jipipe:compartment:output-slot-name")
    public void setOutputSlotName(String outputSlotName) {
        this.outputSlotName = outputSlotName;
    }

    @SetJIPipeDocumentation(name = "Show in project overview", description = "If enabled, show this output in the project overview. " +
            "Please note that this setting has no effect if 'Show in project overview' on the compartment itself.")
    @JIPipeParameter("show-in-project-overview")
    public boolean isShowInProjectOverview() {
        return showInProjectOverview;
    }

    @JIPipeParameter("show-in-project-overview")
    public void setShowInProjectOverview(boolean showInProjectOverview) {
        this.showInProjectOverview = showInProjectOverview;
    }

    @SetJIPipeDocumentation(name = "Project overview color", description = "Allows to mark outputs of this compartment with a color in the project overview.")
    @JIPipeParameter("project-overview-color")
    public OptionalColorParameter getProjectOverviewColor() {
        return projectOverviewColor;
    }

    @JIPipeParameter("project-overview-color")
    public void setProjectOverviewColor(OptionalColorParameter projectOverviewColor) {
        this.projectOverviewColor = projectOverviewColor;
    }
}
