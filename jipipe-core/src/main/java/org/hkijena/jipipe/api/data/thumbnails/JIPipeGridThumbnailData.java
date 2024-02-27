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

package org.hkijena.jipipe.api.data.thumbnails;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.LabelAsJIPipeHeavyData;
import org.hkijena.jipipe.api.LabelAsJIPipeHidden;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.context.JIPipeDataContext;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


@SetJIPipeDocumentation(name = "Grid thumbnail", description = "Grid thumbnail data (used internally)")
@LabelAsJIPipeHeavyData
@JIPipeDataStorageDocumentation(humanReadableDescription = "Stores a data table in the standard JIPipe format (data-table.json plus numeric slot folders)",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-data-table.schema.json")
@LabelAsJIPipeHidden
public class JIPipeGridThumbnailData implements JIPipeThumbnailData {

    private final List<JIPipeThumbnailData> children;

    public JIPipeGridThumbnailData(List<JIPipeThumbnailData> children) {
        this.children = new ArrayList<>(children);
    }

    public JIPipeGridThumbnailData(JIPipeGridThumbnailData other) {
        this.children = other.children;
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeGridThumbnailData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable();
        for (JIPipeThumbnailData child : children) {
            dataTable.addData(child, JIPipeDataContext.create("empty"), progressInfo);
        }
        dataTable.exportData(storage, name, forceName, progressInfo);
    }

    public static JIPipeGridThumbnailData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = JIPipeDataTable.importData(storage, progressInfo);
        return new JIPipeGridThumbnailData(dataTable.getAllData(JIPipeThumbnailData.class, progressInfo));
    }

    @Override
    public Component renderToComponent(int width, int height) {
        if (children.isEmpty()) {
            return new JLabel("N/A");
        } else {
            JPanel panel = new JPanel(new GridBagLayout());
            for (int i = 0; i < Math.min(9, children.size()); i++) {
                JIPipeThumbnailData thumbnailData = children.get(i);

                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = i % 3;
                constraints.gridy = i / 3;
                constraints.weightx = 1;
                constraints.weighty = 1;
                constraints.anchor = GridBagConstraints.CENTER;

                Component preview = thumbnailData.renderToComponent(width / 3, height / 3);
                if (preview == null)
                    preview = new JLabel("N/A");
                panel.add(preview, constraints);
            }
            return panel;
        }
    }

    @Override
    public boolean hasSize() {
        return false;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
