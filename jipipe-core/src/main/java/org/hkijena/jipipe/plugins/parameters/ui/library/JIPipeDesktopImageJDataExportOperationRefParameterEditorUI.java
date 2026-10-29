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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.pickers.JIPipeDesktopImageJDataExporterPicker;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJDataExportOperationRef;
import org.hkijena.jipipe.plugins.parameters.library.references.ImageJExporterParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.references.JIPipeNodeInfoRef;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Parameter for {@link JIPipeNodeInfoRef}
 */
public class JIPipeDesktopImageJDataExportOperationRefParameterEditorUI extends JIPipeDesktopParameterEditorUI<ImageJDataExportOperationRef> {

    private JIPipeDesktopImageJDataExporterPicker picker;
    private JButton currentlyDisplayed;

    public JIPipeDesktopImageJDataExportOperationRefParameterEditorUI(InitializationParameters parameters) {
        super(ImageJDataExportOperationRef.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.setHorizontalAlignment(SwingConstants.LEFT);
        currentlyDisplayed.addActionListener(e -> pick());
        currentlyDisplayed.setToolTipText("Select algorithm type");
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        add(currentlyDisplayed, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        // #1339 item 6: no separate "select algorithm type" icon button - the
        // display button already opens the same picker. Only Configure (a
        // distinct action) remains.
        JButton configureButton = UIUtils.createParameterIconButton(JIPipe.RESOURCES.getIcon16("actions/configure.png"), "Configure the exporter", e -> configure());
        buttonPanel.add(configureButton);

        add(buttonPanel, BorderLayout.EAST);

        picker = new JIPipeDesktopImageJDataExporterPicker(getDesktopWorkbench().getWindow());
    }

    private void configure() {
        ImageJDataExportOperationRef infoRef = getParameter();
        JIPipeDesktopParameterFormPanel.showDialog(getDesktopWorkbench(),
                infoRef,
                new MarkdownText(""),
                "Configure exporter",
                JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION);
    }


    @Override
    public void reload() {
        ImageJDataExportOperationRef infoRef = getParameter();
        ImageJDataExporter exporter = JIPipe.getImageJAdapters().getExporterById(infoRef.getId());
        if (exporter != null) {
            currentlyDisplayed.setText(exporter.getName());
            currentlyDisplayed.setToolTipText(exporter.getDescription());
            currentlyDisplayed.setIcon(JIPipe.getDataTypes().getIconFor(exporter.getExportedJIPipeDataType()));
        } else {
            currentlyDisplayed.setText("None selected");
            currentlyDisplayed.setIcon(JIPipe.RESOURCES.getIcon16("emblems/vcs-conflicting.png"));
        }
    }


    private void pick() {
        Class<? extends JIPipeData> baseClass = JIPipeData.class;
        boolean includeConvertible = true;
        ImageJExporterParameterSettings annotation = getParameterAccess().getAnnotationOfType(ImageJExporterParameterSettings.class);
        if (annotation != null) {
            baseClass = annotation.baseClass();
            includeConvertible = annotation.includeConvertible();
        }
        picker.setAvailableItems(new ArrayList<>(JIPipe.getImageJAdapters().getAvailableExporters(baseClass, includeConvertible)));
        ImageJDataExportOperationRef infoRef = getParameter();
        ImageJDataExporter Exporter = JIPipe.getImageJAdapters().getExporterById(infoRef.getId());
        picker.setSelectedItem(Exporter);
        ImageJDataExporter result = picker.showDialog();
        if (result != null) {
            setParameter(new ImageJDataExportOperationRef(JIPipe.getImageJAdapters().getIdOf(result)), true);
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }
}
