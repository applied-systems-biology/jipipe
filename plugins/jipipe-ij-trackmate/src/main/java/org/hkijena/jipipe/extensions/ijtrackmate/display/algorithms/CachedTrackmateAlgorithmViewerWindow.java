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

package org.hkijena.jipipe.extensions.ijtrackmate.display.algorithms;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCacheDataViewerWindow;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotTrackerData;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.ui.swing.script.EditorPane;

import javax.swing.*;
import java.awt.*;

/**
 * Cached viewer for
 */
public class CachedTrackmateAlgorithmViewerWindow extends JIPipeDesktopCacheDataViewerWindow {

    private JToolBar toolBar = new JToolBar();
    private EditorPane textArea;
    private JLabel errorLabel;

    public CachedTrackmateAlgorithmViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName, boolean deferLoading) {
        super(workbench, dataSource, displayName);
        initialize();
        if (!deferLoading)
            reloadDisplayedData();
    }

    private void initialize() {
        textArea = new EditorPane();
        UIUtils.applyThemeToCodeEditor(textArea);
        textArea.setBackground(UIManager.getColor("TextArea.background"));
        textArea.setHighlightCurrentLine(false);
        textArea.setTabSize(4);
        textArea.setEditable(false);
        getWorkbench().getContext().inject(textArea);
        errorLabel = new JLabel(UIUtils.getIconFromResources("emblems/no-data.png"));
        getToolBar().add(errorLabel, 0);

        add(toolBar, BorderLayout.NORTH);
        RTextScrollPane scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setFoldIndicatorEnabled(true);
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public JToolBar getToolBar() {
        return toolBar;
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {

    }

    @Override
    protected void hideErrorUI() {
        errorLabel.setVisible(false);
    }

    @Override
    protected void showErrorUI() {
        if (getAlgorithm() != null) {
            errorLabel.setText(String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow()));
        } else {
            errorLabel.setText("No data available");
        }
        errorLabel.setVisible(true);
        getToolBar().revalidate();
        getToolBar().repaint();
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        JIPipeData data = virtualData.getData(progressInfo);
        if (data instanceof SpotTrackerData) {
            textArea.setText(((SpotTrackerData) data).toJson());
            textArea.setSyntaxEditingStyle("application-json");
        } else if (data instanceof SpotDetectorData) {
            textArea.setText(((SpotDetectorData) data).toJson());
            textArea.setSyntaxEditingStyle("application-json");
        }
    }
}
