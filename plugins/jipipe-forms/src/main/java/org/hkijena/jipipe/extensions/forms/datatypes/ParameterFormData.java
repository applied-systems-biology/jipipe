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

package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.markup.MarkdownText;

import javax.swing.*;

/**
 * {@link FormData} that is put into a {@link JIPipeDesktopFormPanel}
 */
public abstract class ParameterFormData extends FormData {
    private String name = "Form";
    private HTMLText description = new HTMLText();
    private boolean showName = true;

    public ParameterFormData() {
    }

    public ParameterFormData(ParameterFormData other) {
        super(other);
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.showName = other.showName;
    }

    @Override
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(new MarkdownText("This is a preview of the form."),
                JIPipeDesktopFormPanel.WITH_DOCUMENTATION | JIPipeDesktopFormPanel.WITH_SCROLLING | JIPipeDesktopFormPanel.DOCUMENTATION_BELOW);
        ParameterFormData duplicate = (ParameterFormData) duplicate(new JIPipeProgressInfo());
        if (isShowName()) {
            formPanel.addToForm(duplicate.getEditor(desktopWorkbench), new JLabel(getName()), description.toMarkdown());
        } else {
            formPanel.addWideToForm(duplicate.getEditor(desktopWorkbench), description.toMarkdown());
        }
        formPanel.addVerticalGlue();
        JFrame frame = new JFrame("Preview: " + JIPipeDataInfo.getInstance(getClass()).getName());
        frame.setContentPane(formPanel);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(desktopWorkbench.getWindow());
        frame.setVisible(true);
    }

    @SetJIPipeDocumentation(name = "Name", description = "Name of the form element. Hidden if 'Show name' is disabled.")
    @JIPipeParameter(value = "form:name", uiOrder = -100)
    public String getName() {
        return name;
    }

    @JIPipeParameter("form:name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Description", description = "Description of the element displayed to the user.")
    @JIPipeParameter(value = "form:description", uiOrder = -80)
    public HTMLText getDescription() {
        return description;
    }

    @JIPipeParameter("form:description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @SetJIPipeDocumentation(name = "Show name", description = "If enabled, the name of the form element is shown next to it.")
    @JIPipeParameter("form:show-name")
    public boolean isShowName() {
        return showName;
    }

    @JIPipeParameter("form:show-name")
    public void setShowName(boolean showName) {
        this.showName = showName;
    }

    @Override
    public String toString() {
        return JIPipeData.getNameOf(getClass()) + " [name=" + getName() + "]";
    }
}
