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
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantCondition;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistantConditionStatus;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopHTMLEditor;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.*;

public class DescriptionAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public DescriptionAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        addButton(UIUtils.createButton("Edit description", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editProjectDescription));
    }

    private void editProjectDescription() {
        JIPipeDesktopHTMLEditor editor = new JIPipeDesktopHTMLEditor(getDesktopProjectWorkbench(), JIPipeDesktopHTMLEditor.Mode.Full, JIPipeDesktopHTMLEditor.WITH_SCROLL_BAR);
        editor.setText(getProject().getMetadata().getDescription().getHtml());
        if (UIUtils.showConfirmDialog(this, "Edit project description", new Dimension(800, 600), editor)) {
            getProject().getMetadata().setDescription(new HTMLText(editor.getHTML()));
            getAssistant().updateAssistant();
        }
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        return StringUtils.isNullOrEmpty(getProject().getMetadata().getDescription().toPlainText()) ? JIPipeDesktopPublisherAssistantConditionStatus.Warning : JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return "Description is available";
        } else {
            return "No description set";
        }
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return new HTMLText("The project description will be stored into a README file.");
        } else {
            return new HTMLText("It is highly recommended to provide a description for the project and associated RO-Crate.");
        }
    }
}
