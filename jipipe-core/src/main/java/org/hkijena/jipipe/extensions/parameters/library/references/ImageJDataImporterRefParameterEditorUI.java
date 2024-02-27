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

package org.hkijena.jipipe.extensions.parameters.library.references;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.pickers.ImageJDataImporterPicker;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Parameter for {@link JIPipeNodeInfoRef}
 */
public class ImageJDataImporterRefParameterEditorUI extends JIPipeParameterEditorUI {

    private ImageJDataImporterPicker picker;
    private JButton currentlyDisplayed;

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public ImageJDataImporterRefParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.addActionListener(e -> pick());
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JButton selectButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.setToolTipText("Select algorithm type");
        selectButton.addActionListener(e -> pick());
        add(selectButton, BorderLayout.EAST);

        picker = new ImageJDataImporterPicker(getWorkbench().getWindow());
    }


    @Override
    public void reload() {
        ImageJDataImporterRef infoRef = getParameter(ImageJDataImporterRef.class);
        ImageJDataImporter importer = JIPipe.getImageJAdapters().getImporterById(infoRef.getId());
        if (importer != null) {
            currentlyDisplayed.setText(importer.getName());
            currentlyDisplayed.setToolTipText(importer.getDescription());
            currentlyDisplayed.setIcon(JIPipe.getDataTypes().getIconFor(importer.getImportedJIPipeDataType()));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
        }
    }


    private void pick() {
        Class<? extends JIPipeData> baseClass = JIPipeData.class;
        boolean includeConvertible = true;
        ImageJImporterParameterSettings annotation = getParameterAccess().getAnnotationOfType(ImageJImporterParameterSettings.class);
        if (annotation != null) {
            baseClass = annotation.baseClass();
            includeConvertible = annotation.includeConvertible();
        }
        picker.setAvailableItems(new ArrayList<>(JIPipe.getImageJAdapters().getAvailableImporters(baseClass, includeConvertible)));
        ImageJDataImporterRef infoRef = getParameter(ImageJDataImporterRef.class);
        ImageJDataImporter importer = JIPipe.getImageJAdapters().getImporterById(infoRef.getId());
        picker.setSelectedItem(importer);
        ImageJDataImporter result = picker.showDialog();
        if (result != null) {
            setParameter(new ImageJDataImporterRef(JIPipe.getImageJAdapters().getIdOf(result)), true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
