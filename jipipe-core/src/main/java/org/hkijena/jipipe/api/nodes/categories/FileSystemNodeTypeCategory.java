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

package org.hkijena.jipipe.api.nodes.categories;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;

import javax.swing.*;

public class FileSystemNodeTypeCategory implements JIPipeNodeTypeCategory {

    @Override
    public String getId() {
        return "org.hkijena.jipipe:file-system";
    }

    @Override
    public String getName() {
        return "File system";
    }

    @Override
    public String getDescription() {
        return "Nodes that are encapsulate file system operations";
    }

    @Override
    public int getUIOrder() {
        return 10;
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("devices/drive-harddisk.png");
    }

    @Override
    public float getColorHue() {
        return 60.0f / 360.0f;
    }

    @Override
    public boolean isVisibleInPipeline() {
        return true;
    }

    @Override
    public boolean isVisibleInCompartments() {
        return false;
    }

    @Override
    public boolean userCanCreate() {
        return true;
    }

    @Override
    public boolean userCanDelete() {
        return true;
    }
}
