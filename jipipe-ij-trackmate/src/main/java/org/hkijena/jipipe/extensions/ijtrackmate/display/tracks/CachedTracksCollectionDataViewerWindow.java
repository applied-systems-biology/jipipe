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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.display.tracks;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTableDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.TrackCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.display.spots.SpotsManagerPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPanel;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.cache.JIPipeCacheDataViewerWindow;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;

public class CachedTracksCollectionDataViewerWindow extends JIPipeCacheDataViewerWindow implements WindowListener {
    private JIPipeImageViewerPanel imageViewerPanel;

    public CachedTracksCollectionDataViewerWindow(JIPipeWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName, boolean deferLoading) {
        super(workbench, dataSource, displayName);
        initialize();
        if (!deferLoading)
            reloadDisplayedData();
        addWindowListener(this);
    }

    private void initialize() {
        imageViewerPanel = JIPipeImageViewerPanel.createForCacheViewer(this, Arrays.asList(TracksManagerPlugin2D.class, SpotsManagerPlugin2D.class));
        setContentPane(imageViewerPanel);
        revalidate();
        repaint();
    }

    @Override
    public JToolBar getToolBar() {
        if (imageViewerPanel == null)
            return null;
        else
            return imageViewerPanel.getToolBar();
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {
    }

    @Override
    protected void hideErrorUI() {
        imageViewerPanel.setError(null);
    }

    @Override
    protected void showErrorUI() {
        String errorLabel;
        if (getAlgorithm() != null) {
            errorLabel = (String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow()));
        } else {
            errorLabel = ("No data available");
        }
        imageViewerPanel.setError(errorLabel);
    }

    @Override
    protected void loadData(JIPipeDataItemStore virtualData, JIPipeProgressInfo progressInfo) {
        SpotsManagerPlugin2D spotsManagerPlugin = imageViewerPanel.getPlugin(SpotsManagerPlugin2D.class);
        TracksManagerPlugin2D tracksManagerPlugin = imageViewerPanel.getPlugin(TracksManagerPlugin2D.class);
        TrackCollectionData data = JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), TrackCollectionData.class);
        boolean fitImage = imageViewerPanel.getImage() == null;
        spotsManagerPlugin.setSpotCollection(data, true);
        tracksManagerPlugin.setTrackCollection(data, getDataSource(), true);
        imageViewerPanel.setError(null);
        imageViewerPanel.setImage(data.getImage());
        if (fitImage)
            SwingUtilities.invokeLater(imageViewerPanel::fitImageToScreen);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        imageViewerPanel.dispose();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
