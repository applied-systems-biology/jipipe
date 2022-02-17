package org.hkijena.jipipe.extensions.forms.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;

import javax.swing.*;

/**
 * {@link FormData} that is put into a {@link org.hkijena.jipipe.ui.components.FormPanel}
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
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        FormPanel formPanel = new FormPanel(new MarkdownDocument("This is a preview of the form."),
                FormPanel.WITH_DOCUMENTATION | FormPanel.WITH_SCROLLING | FormPanel.DOCUMENTATION_BELOW);
        ParameterFormData duplicate = (ParameterFormData) duplicate(progressInfo);
        if (isShowName()) {
            formPanel.addToForm(duplicate.getEditor(workbench), new JLabel(getName()), description.toMarkdown());
        } else {
            formPanel.addWideToForm(duplicate.getEditor(workbench), description.toMarkdown());
        }
        formPanel.addVerticalGlue();
        JFrame frame = new JFrame("Preview: " + JIPipeDataInfo.getInstance(getClass()).getName());
        frame.setContentPane(formPanel);
        frame.setSize(640, 480);
        frame.setLocationRelativeTo(workbench.getWindow());
        frame.setVisible(true);
    }

    @JIPipeDocumentation(name = "Name", description = "Name of the form element. Hidden if 'Show name' is disabled.")
    @JIPipeParameter(value = "form:name", uiOrder = -100)
    public String getName() {
        return name;
    }

    @JIPipeParameter("form:name")
    public void setName(String name) {
        this.name = name;
    }

    @JIPipeDocumentation(name = "Description", description = "Description of the element displayed to the user.")
    @JIPipeParameter(value = "form:description", uiOrder = -80)
    public HTMLText getDescription() {
        return description;
    }

    @JIPipeParameter("form:description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @JIPipeDocumentation(name = "Show name", description = "If enabled, the name of the form element is shown next to it.")
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
