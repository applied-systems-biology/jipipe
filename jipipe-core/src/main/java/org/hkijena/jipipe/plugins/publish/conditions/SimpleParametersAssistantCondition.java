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

package org.hkijena.jipipe.plugins.publish.conditions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReference;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroup;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

public class SimpleParametersAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public SimpleParametersAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        addButton(UIUtils.createButton("Go to project overview", JIPipe.RESOURCES.getIcon16("actions/go-jump.png"), this::goToProjectOverview));
    }

    private void goToProjectOverview() {
        getDesktopProjectWorkbench().getDocumentTabPane().selectSingletonTab(JIPipeDesktopProjectWorkbench.TAB_PROJECT_OVERVIEW);
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        JIPipeParameterTree tree = getProject().getGraph().getParameterTree(false, null);

        for (GraphNodeParameterReferenceGroup group : getProject().getPipelineParameters().getExportedParameters().getParameterReferenceGroups()) {
            for (GraphNodeParameterReference reference : group.getContent()) {
                JIPipeParameterAccess access = reference.resolve(tree);
                if(access != null) {
                    if(!ParameterUtils.isSimpleType(access.getFieldClass())) {
                        return JIPipeDesktopPublisherAssistantConditionStatus.Warning;
                    }
                }
            }
        }

        for (JIPipeParameterAccess access : getProject().getMetadata().getGlobalParameters().getParameters().values()) {
            if(!ParameterUtils.isSimpleType(access.getFieldClass())) {
                return JIPipeDesktopPublisherAssistantConditionStatus.Warning;
            }
        }
        return JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return "No issues with parameters detected";
        } else {
            return "Complex parameters detected";
        }
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return new HTMLText("No issues with any parameters that might be exported into the RO-Crate were detected");
        }
        else {
            return new HTMLText("Not all parameters can be exported into the RO-Crate, as only simple (boolean/numeric/text) parameters are supported. This will not change the behavior of the pipeline.");
        }
    }
}
