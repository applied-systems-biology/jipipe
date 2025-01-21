package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.browser.JIPipeDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeDataTableBrowser;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * A viewer for {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public abstract class JIPipeDesktopDataViewer extends JIPipeDesktopWorkbenchPanel implements Disposable {
    public static final String LOADING_PLACEHOLDER_TEXT = "[Please wait ...]";
    public static final String ERROR_PLACEHOLDER_TEXT = "[Error]";
    private final JIPipeDesktopDataViewerWindow dataViewerWindow;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public JIPipeDesktopDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow.getDesktopWorkbench());
        this.dataViewerWindow = dataViewerWindow;
        setLayout(new BorderLayout());
    }

    public JIPipeDesktopDataViewerWindow getDataViewerWindow() {
        return dataViewerWindow;
    }

    /**
     * Called after preOnDataChanged() in the phase where the ribbon is re-built
     *
     * @param ribbon the ribbon
     */
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {

    }

    /**
     * Called before rebuildRibbon()
     *
     * @param dockPanel the dock panel
     */
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    /**
     * Called after rebuildDock()
     *
     * @param statusBar the status bar (left side)
     */
    public void rebuildStatusBar(JToolBar statusBar) {

    }

    protected void showError(JIPipeDesktopDockPanel dockPanel, String text, String subtext) {
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.add(UIUtils.createInfoLabel(text,
                subtext,
                UIUtils.getIcon32FromResources("actions/circle-xmark.png")), BorderLayout.CENTER);
        dockPanel.setBackgroundComponent(errorPanel);
    }

    /**
     * Called when the data browser was changed.
     */
    public void preOnDataChanged() {

    }

    /**
     * Called when the data browser was changed.
     * Called after rebuildRibbon(), rebuildDock(), and rebuildStatusBar()
     */
    public void postOnDataChanged() {

    }

    /**
     * Called when the full data was downloaded.
     *
     * @param data the downloaded data. please check using instanceof if the correct data is present
     */
    public void onDataDownloaded(JIPipeData data) {

    }


    /**
     * Gets the current data browser
     *
     * @return the data browser. Can be null
     */
    public JIPipeDataBrowser getDataBrowser() {
        return dataViewerWindow.getDataBrowser();
    }

    /**
     * Gets the current data table browser
     *
     * @return the data table browser. Can be null.
     */
    public JIPipeDataTableBrowser getDataTableBrowser() {
        return dataViewerWindow.getDataTableBrowser();
    }

    @Override
    public void dispose() {
        Disposable.super.dispose();
        executorService.shutdownNow();
    }

    /**
     * Gets a future and passes it into Swing.invokeLater
     *
     * @param future   the future
     * @param consumer the consumer
     * @param <T>      the type
     */
    public <T> void awaitToSwing(Future<T> future, Consumer<T> consumer) {
        executorService.submit(() -> {
            try {
                T t = future.get();
                SwingUtilities.invokeLater(() -> consumer.accept(t));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
