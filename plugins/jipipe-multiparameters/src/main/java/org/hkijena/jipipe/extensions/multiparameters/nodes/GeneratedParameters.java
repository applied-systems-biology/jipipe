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

package org.hkijena.jipipe.extensions.multiparameters.nodes;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.*;
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
        super(true, JIPipe.getParameterTypes().getRegisteredParameters().values());
        this.parent = parent;
    }

    public GeneratedParameters(GeneratedParameters other) {
        super(other);
    }

    @SetJIPipeDocumentation(name = "Import", description = "Imports a parameter from another graph node")
    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/graph-compartment.png")
    public void uiImportParameterFromGraph(JIPipeWorkbench workbench) {
        if (parent == null)
            return;

        JIPipeParameterTree globalTree = parent.getParentGraph().getParameterTree(false, null);

        List<Object> importedParameters = ParameterTreeUI.showPickerDialog(workbench.getWindow(), globalTree, "Import parameter");
        for (Object importedParameter : importedParameters) {
            if (importedParameter instanceof JIPipeParameterAccess) {
                JIPipeParameterTree.Node node = globalTree.getSourceNode(((JIPipeParameterAccess) importedParameter).getSource());
                importParameter(node, (JIPipeParameterAccess) importedParameter);
            } else if (importedParameter instanceof JIPipeParameterTree.Node) {
                for (JIPipeParameterAccess access : ((JIPipeParameterTree.Node) importedParameter).getParameters().values()) {
                    JIPipeParameterTree.Node node = globalTree.getSourceNode(access.getSource());
                    importParameter(node, access);
                }
            }
        }
        emitParameterStructureChangedEvent();
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
