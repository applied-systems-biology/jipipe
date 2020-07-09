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

package org.hkijena.pipelinej.extensions.multiparameters.datasources;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.events.ParameterStructureChangedEvent;
import org.hkijena.pipelinej.api.parameters.*;
import org.hkijena.pipelinej.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.components.ParameterTreeUI;
import org.hkijena.pipelinej.utils.ResourceUtils;

import java.util.List;

/**
 * Encapsulates the generation of parameters
 */
public class GeneratedParameters extends ACAQDynamicParameterCollection {

    private ACAQGraphNode parent;

    public GeneratedParameters(ACAQGraphNode parent) {
        super(true, ACAQParameterTypeRegistry.getInstance().getRegisteredParameters().values());
        this.parent = parent;
    }

    public GeneratedParameters(GeneratedParameters other) {
        super(other);
    }

    @ACAQDocumentation(name = "Import", description = "Imports a parameter from another graph node")
    @ACAQContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/graph-compartment.png")
    public void uiImportParameterFromGraph(ACAQWorkbench workbench) {
        if (parent == null)
            return;

        ACAQParameterTree globalTree = parent.getGraph().getParameterTree();

        List<Object> importedParameters = ParameterTreeUI.showPickerDialog(workbench.getWindow(), globalTree, "Import parameter");
        for (Object importedParameter : importedParameters) {
            if (importedParameter instanceof ACAQParameterAccess) {
                ACAQParameterTree.Node node = globalTree.getSourceNode(((ACAQParameterAccess) importedParameter).getSource());
                importParameter(node, (ACAQParameterAccess) importedParameter);
            } else if (importedParameter instanceof ACAQParameterTree.Node) {
                for (ACAQParameterAccess access : ((ACAQParameterTree.Node) importedParameter).getParameters().values()) {
                    if (access.getVisibility().isVisibleIn(ACAQParameterVisibility.TransitiveVisible)) {
                        ACAQParameterTree.Node node = globalTree.getSourceNode(access.getSource());
                        importParameter(node, access);
                    }
                }
            }
        }
        getEventBus().post(new ParameterStructureChangedEvent(this));
    }

    private void importParameter(ACAQParameterTree.Node node, ACAQParameterAccess importedParameter) {
        List<String> path = node.getPath();
        path.add(importedParameter.getKey());
        path.remove(0);

        String uniqueKey = String.join("/", path);
        ACAQMutableParameterAccess access = addParameter(uniqueKey, importedParameter.getFieldClass());
        access.setName(importedParameter.getName());
        access.setDescription(importedParameter.getDescription());
    }

    public ACAQGraphNode getParent() {
        return parent;
    }

    public void setParent(ACAQGraphNode parent) {
        this.parent = parent;
    }
}
