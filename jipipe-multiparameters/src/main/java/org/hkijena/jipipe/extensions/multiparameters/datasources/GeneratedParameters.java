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
 */

package org.hkijena.jipipe.extensions.multiparameters.datasources;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.utils.ResourceUtils;

import java.util.List;

/**
 * Encapsulates the generation of parameters
 */
public class GeneratedParameters extends JIPipeDynamicParameterCollection {

    private JIPipeGraphNode parent;

    public GeneratedParameters(JIPipeGraphNode parent) {
        super(true, JIPipeParameterTypeRegistry.getInstance().getRegisteredParameters().values());
        this.parent = parent;
    }

    public GeneratedParameters(GeneratedParameters other) {
        super(other);
    }

    @JIPipeDocumentation(name = "Import", description = "Imports a parameter from another graph node")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/graph-compartment.png")
    public void uiImportParameterFromGraph(JIPipeWorkbench workbench) {
        if (parent == null)
            return;

        JIPipeParameterTree globalTree = parent.getGraph().getParameterTree();

        List<Object> importedParameters = ParameterTreeUI.showPickerDialog(workbench.getWindow(), globalTree, "Import parameter");
        for (Object importedParameter : importedParameters) {
            if (importedParameter instanceof JIPipeParameterAccess) {
                JIPipeParameterTree.Node node = globalTree.getSourceNode(((JIPipeParameterAccess) importedParameter).getSource());
                importParameter(node, (JIPipeParameterAccess) importedParameter);
            } else if (importedParameter instanceof JIPipeParameterTree.Node) {
                for (JIPipeParameterAccess access : ((JIPipeParameterTree.Node) importedParameter).getParameters().values()) {
                    if (access.getVisibility().isVisibleIn(JIPipeParameterVisibility.TransitiveVisible)) {
                        JIPipeParameterTree.Node node = globalTree.getSourceNode(access.getSource());
                        importParameter(node, access);
                    }
                }
            }
        }
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    private void importParameter(JIPipeParameterTree.Node node, JIPipeParameterAccess importedParameter) {
        List<String> path = node.getPath();
        path.add(importedParameter.getKey());
        path.remove(0);

        String uniqueKey = String.join("/", path);
        JIPipeMutableParameterAccess access = addParameter(uniqueKey, importedParameter.getFieldClass());
        access.setName(importedParameter.getName());
        access.setDescription(importedParameter.getDescription());
    }

    public JIPipeGraphNode getParent() {
        return parent;
    }

    public void setParent(JIPipeGraphNode parent) {
        this.parent = parent;
    }
}
