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

package org.hkijena.jipipe.plugins.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;

public class SetWindowSizeTool extends JIPipeDesktopMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public SetWindowSizeTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Set window size");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/itmages-resize.png"));
        addActionListener(e -> {
            doChangeWindowSize();
        });
    }

    private void doChangeWindowSize() {
        Settings settings = new Settings();
        if (JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(), settings, MarkdownText.EMPTY, "Set window size")) {
            getDesktopWorkbench().getWindow().setSize(settings.getWidth(), settings.getHeight());
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development";
    }

    public static class Settings extends AbstractJIPipeParameterCollection {
        private int width = 1920;
        private int height = 1080;

        @SetJIPipeDocumentation(name = "Width")
        @JIPipeParameter(value = "width", uiOrder = -100)
        public int getWidth() {
            return width;
        }

        @JIPipeParameter("width")
        public void setWidth(int width) {
            this.width = width;
        }

        @SetJIPipeDocumentation(name = "Height")
        @JIPipeParameter(value = "height", uiOrder = -90)
        public int getHeight() {
            return height;
        }

        @JIPipeParameter("height")
        public void setHeight(int height) {
            this.height = height;
        }
    }
}
