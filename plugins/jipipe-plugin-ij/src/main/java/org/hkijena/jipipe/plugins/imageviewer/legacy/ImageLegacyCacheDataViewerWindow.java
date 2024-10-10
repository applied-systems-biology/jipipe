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

package org.hkijena.jipipe.plugins.imageviewer.legacy;

import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopLegacyCacheDataViewerWindow;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link JIPipeDesktopLegacyCacheDataViewerWindow} that contains a {@link JIPipeDesktopLegacyImageViewer}
 */
public abstract class ImageLegacyCacheDataViewerWindow extends JIPipeDesktopLegacyCacheDataViewerWindow implements WindowListener {
    private JIPipeDesktopLegacyImageViewer imageViewer;
    private boolean fitImageTriggered;

    public ImageLegacyCacheDataViewerWindow(JIPipeDesktopWorkbench workbench, JIPipeDataTableDataSource dataSource, String displayName) {
        super(workbench, dataSource, displayName);
        initialize();
        addWindowListener(this);
    }

    private void initialize() {
        List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> plugins = new ArrayList<>();
        Map<Class<?>, Object> contextObjects = new HashMap<>();
        initializePlugins(plugins, contextObjects);
        imageViewer = new JIPipeDesktopLegacyImageViewer(getWorkbench(), plugins, contextObjects);
        setContentPane(imageViewer);
        revalidate();
        repaint();
    }

    /**
     * Override this method to register plugins and set context objects
     * By default, it will register {@link JIPipeDesktopLegacyImageViewer}.DEFAULT_PLUGINS and the window as context object (accessor JIPipeCacheDataViewerWindow)
     *
     * @param plugins        plugins to register
     * @param contextObjects the context objects to register
     */
    protected void initializePlugins(List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> plugins, Map<Class<?>, Object> contextObjects) {
        plugins.addAll(JIPipeDesktopLegacyImageViewer.DEFAULT_PLUGINS);
        contextObjects.put(JIPipeDesktopLegacyCacheDataViewerWindow.class, this);
        contextObjects.put(ImageLegacyCacheDataViewerWindow.class, this);
    }

    /**
     * Triggers the fitImage() command if applied once
     */
    public void fitImageToScreenOnce() {
        if (!fitImageTriggered && imageViewer.getImagePlus() != null) {
            SwingUtilities.invokeLater(imageViewer::fitImageToScreen);
            fitImageTriggered = true;
        }
    }

    @Override
    protected void hideErrorUI() {
        imageViewer.setError(null);
    }

    public JIPipeDesktopLegacyImageViewer getImageViewer() {
        return imageViewer;
    }

    @Override
    protected void showErrorUI() {
        String errorLabel;
        if (getAlgorithm() != null) {
            errorLabel = String.format("No data available in node '%s', slot '%s', row %d", getAlgorithm().getName(), getSlotName(), getDataSource().getRow());
        } else {
            errorLabel = "No data available";
        }
        imageViewer.setError(errorLabel);
    }

    @Override
    public void windowOpened(WindowEvent e) {
        imageViewer.setName(getTitle());
    }

    @Override
    public JToolBar getToolBar() {
        if (imageViewer == null)
            return null;
        else
            return imageViewer.getToolBar();
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        imageViewer.setName(getTitle());
    }

    @Override
    protected void beforeSetRow() {

    }

    @Override
    protected void afterSetRow() {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        imageViewer.dispose();
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

    @Override
    public void dispose() {
        super.dispose();
        imageViewer.dispose();
    }
}
