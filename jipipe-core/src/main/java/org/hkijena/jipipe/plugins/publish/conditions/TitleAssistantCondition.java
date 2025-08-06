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

import javax.swing.*;
import java.awt.*;

public class TitleAssistantCondition extends JIPipeDesktopPublisherAssistantCondition {
    public TitleAssistantCondition(JIPipeDesktopPublisherAssistant assistant) {
        super(assistant);
        initialize();
    }

    private void initialize() {
        addButton(UIUtils.createButton("Edit title", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editProjectDescription));
    }

    private void editProjectDescription() {
        String newTitle = JOptionPane.showInputDialog(this, "Please set the title of your project:", StringUtils.nullToEmpty(getProject().getMetadata().getName()));
        if(!StringUtils.isNullOrEmpty(newTitle)) {
            getProject().getMetadata().setName(newTitle);
            getAssistant().updateAssistant();
        }
    }

    @Override
    public JIPipeDesktopPublisherAssistantConditionStatus getStatus() {
        return StringUtils.isNullOrEmpty(getProject().getMetadata().getName()) ? JIPipeDesktopPublisherAssistantConditionStatus.Invalid : JIPipeDesktopPublisherAssistantConditionStatus.Valid;
    }

    @Override
    public String getAssistantTitle(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return "Title is available";
        } else {
            return "No title set";
        }
    }

    @Override
    public HTMLText getAssistantDescription(JIPipeDesktopPublisherAssistantConditionStatus status) {
        if (status == JIPipeDesktopPublisherAssistantConditionStatus.Valid) {
            return new HTMLText("The RO-Crate will be named according to your project's title.");
        } else {
            return new HTMLText("You are required to set the title of your project.");
        }
    }
}
