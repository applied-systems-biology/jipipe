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

package org.hkijena.jipipe.plugins.publish.rocrate;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.publish.JIPipeDesktopPublisherAssistant;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.publish.conditions.SavedProjectAssistantCondition;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ROCratePublisherAssistant extends JIPipeDesktopPublisherAssistant {
    public ROCratePublisherAssistant(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench);
        addAssistantCondition(new SavedProjectAssistantCondition(this));
    }

    @Override
    public String getAssistantTitle() {
        return "Publish project as RO-Crate";
    }

    @Override
    public HTMLText getAssistantDescription() {
        return new HTMLText("This tool will guide you through the process of publishing your JIPipe project as RO-Crate with CWL. This will produce a *.zip file that contains the projects and all inputs together with all necessary instructions to run the project.");
    }

    @Override
    public List<BufferedImage> getAssistantLogos() {
        return List.of(JIPipe.RESOURCES.getVariantResourceAsImage("logos/ro-crate.png"),
                JIPipe.RESOURCES.getVariantResourceAsImage("logos/cwl.png"));
    }
}
