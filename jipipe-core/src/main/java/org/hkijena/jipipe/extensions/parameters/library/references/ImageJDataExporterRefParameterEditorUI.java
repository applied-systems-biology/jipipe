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

package org.hkijena.jipipe.extensions.parameters.library.references;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.pickers.ImageJDataExporterPicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Parameter for {@link JIPipeNodeInfoRef}
 */
public class ImageJDataExporterRefParameterEditorUI extends JIPipeParameterEditorUI {

    private ImageJDataExporterPicker picker;
    private JButton currentlyDisplayed;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public ImageJDataExporterRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pick());
        UIUtils.makeFlat(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.makeFlat(selectButton);
        selectButton.setToolTipText("Select algorithm type");
        selectButton.addActionListener(e -> pick());
        add(selectButton, BorderLayout.EAST);

        picker = new ImageJDataExporterPicker(getWorkbench().getWindow());
    }


    @Override
    public void reload() {
        ImageJDataExporterRef infoRef = getParameter(ImageJDataExporterRef.class);
        ImageJDataExporter exporter = JIPipe.getImageJAdapters().getExporterById(infoRef.getId());
        if (exporter != null) {
            currentlyDisplayed.setText(exporter.getName());
            currentlyDisplayed.setToolTipText(exporter.getDescription());
            currentlyDisplayed.addActionListener(e -> pick());
            currentlyDisplayed.setIcon(JIPipe.getDataTypes().getIconFor(exporter.getExportedJIPipeDataType()));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }


    private void pick() {
        Class<? extends JIPipeData> baseClass = JIPipeData.class;
        boolean includeConvertible = true;
        ImageJExporterParameterSettings annotation = getParameterAccess().getAnnotationOfType(ImageJExporterParameterSettings.class);
        if(annotation != null) {
            baseClass = annotation.baseClass();
            includeConvertible = annotation.includeConvertible();
        }
        picker.setAvailableItems(new ArrayList<>(JIPipe.getImageJAdapters().getAvailableExporters(baseClass, includeConvertible)));
        ImageJDataExporterRef infoRef = getParameter(ImageJDataExporterRef.class);
        ImageJDataExporter Exporter = JIPipe.getImageJAdapters().getExporterById(infoRef.getId());
        picker.setSelectedItem(Exporter);
        ImageJDataExporter result = picker.showDialog();
        if(result != null) {
            setParameter(new ImageJDataExporterRef(JIPipe.getImageJAdapters().getIdOf(result)), true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
