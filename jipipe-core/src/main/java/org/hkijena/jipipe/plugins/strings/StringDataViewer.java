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

package org.hkijena.jipipe.plugins.strings;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.xml.XmlUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class StringDataViewer extends JIPipeDesktopDataViewer {

    private final EditorPane editorPane = new EditorPane();
    private String extension = ".txt";

    public StringDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        initialize();
    }

    private void initialize() {
        UIUtils.applyThemeToCodeEditor(editorPane);
        editorPane.setBackground(UIManager.getColor("TextArea.background"));
        editorPane.setHighlightCurrentLine(false);
        editorPane.setTabSize(4);
        editorPane.setEditable(false);
        getWorkbench().getContext().inject(editorPane);

        RTextScrollPane scrollPane = new RTextScrollPane(editorPane, true);
        scrollPane.setFoldIndicatorEnabled(true);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task generalTask = ribbon.getOrCreateTask("General");
        JIPipeDesktopRibbon.Band editBand = generalTask.getOrCreateBand("Edit");
        JIPipeDesktopRibbon.Band toolsBand = generalTask.getOrCreateBand("Tools");
        editBand.add(new JIPipeDesktopLargeButtonRibbonAction("Undo", "Reverts the last action", UIUtils.getIcon32FromResources("actions/edit-undo.png"), editorPane::undoLastAction));
        editBand.add(new JIPipeDesktopLargeButtonRibbonAction("Redo", "Repeats the last action", UIUtils.getIcon32FromResources("actions/edit-redo.png"), editorPane::redoLastAction));
        if ( XMLData.class.isAssignableFrom(getDataBrowser().getDataClass())) {
            toolsBand.add(new JIPipeDesktopLargeButtonRibbonAction("Prettify", "Formats the XML data", UIUtils.getIcon32FromResources("actions/format-text-code.png"), this::formatXML));
        }
        toolsBand.add(new JIPipeDesktopLargeButtonRibbonAction("External editor", "Opens the text in an external editor", UIUtils.getIcon32FromResources("actions/open-in-new-window.png"), this::openInExternalEditor));
    }

    private void openInExternalEditor() {
        Path outputPath = JIPipeRuntimeApplicationSettings.getTemporaryFile("text", extension);
        try {
            Files.write(outputPath, editorPane.getText().getBytes(StandardCharsets.UTF_8));
            Desktop.getDesktop().open(outputPath.toFile());
        } catch (IOException e) {
            UIUtils.showErrorDialog(getDesktopWorkbench(), this, e);
        }
    }

    private void formatXML() {
        String xml = editorPane.getText();
        String transformed = XmlUtils.prettyPrint(xml, 4, false);
        editorPane.setText(transformed);
    }

    @Override
    public void postOnDataChanged() {
        editorPane.setText(LOADING_PLACEHOLDER_TEXT);
        awaitToSwing(getDataBrowser().getData(StringData.class), (data) -> {
            extension = data.getOutputExtension();
            editorPane.setText(data.getData());
            editorPane.setSyntaxEditingStyle(data.getMimeType());
        });
    }
}
