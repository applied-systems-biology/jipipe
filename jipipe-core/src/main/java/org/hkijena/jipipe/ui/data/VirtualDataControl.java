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

package org.hkijena.jipipe.ui.data;

import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.VirtualDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * A tool that automatically runs 'update cache' when any parameter or graph property is changed
 */
public class VirtualDataControl extends JIPipeProjectWorkbenchPanel {

    private final VirtualDataSettings virtualDataSettings = VirtualDataSettings.getInstance();

    /**
     * @param workbenchUI The workbench UI
     */
    public VirtualDataControl(JIPipeProjectWorkbench workbenchUI) {
        super(workbenchUI);
        getProject().getGraph().getEventBus().register(this);
    }

    public JToggleButton createToggleButton() {
        JToggleButton button = new JToggleButton("Reduce memory", UIUtils.getIconFromResources("actions/rabbitvcs-drive.png"));
        button.setSelected(virtualDataSettings.isVirtualMode());
        button.setToolTipText("Enable/disable virtual mode. If enabled, any output indicated by the HDD icon is stored on the hard drive to reduce memory consumption. Increases the run time significantly.");
        button.addActionListener(e -> {
            if (button.isSelected() != virtualDataSettings.isVirtualMode()) {
                virtualDataSettings.setVirtualMode(button.isSelected());
                virtualDataSettings.getEventBus().post(new ParameterChangedEvent(virtualDataSettings, "virtual-mode"));
                if(virtualDataSettings.isVirtualMode()) {
                    checkForCacheToVirtual();
                }
                else {
                    checkForCacheToNonVirtual();
                }
            }
        });
        return button;
    }

    public JButton createOptionsButton() {
        JButton button = new JButton(UIUtils.getIconFromResources("actions/configure.png"));
        JPopupMenu menu = new JPopupMenu();
        UIUtils.addReloadablePopupMenuToComponent(button, menu, () -> {
            menu.removeAll();
            JMenuItem gcItem = new JMenuItem("Clean memory", UIUtils.getIconFromResources("devices/media-memory.png"));
            gcItem.setToolTipText("Runs the garbage collector (GC) that attempts to clean unused memory. Please note that this will shortly freeze the application.");
            gcItem.addActionListener(e -> {
                System.gc();
                getWorkbench().sendStatusBarText("Unused memory was freed");
            });
            menu.add(gcItem);
            menu.addSeparator();
            JMenuItem cacheToVirtualItem = new JMenuItem("Move cache to HDD", UIUtils.getIconFromResources("devices/drive-harddisk.png"));
            cacheToVirtualItem.setToolTipText("Moves all cached data of this project to HDD");
            cacheToVirtualItem.addActionListener(e -> moveCacheToVirtual());
            menu.add(cacheToVirtualItem);
            JMenuItem cacheToNonVirtualItem = new JMenuItem("Move cache to RAM", UIUtils.getIconFromResources("devices/media-memory.png"));
            cacheToNonVirtualItem.setToolTipText("Moves all cached data of this project to system memory");
            cacheToNonVirtualItem.addActionListener(e -> moveCacheToNonVirtual());
            menu.add(cacheToNonVirtualItem);
        });
        return button;
    }

    private void checkForCacheToVirtual() {
        boolean hasCache = false;
        for (JIPipeProjectWindow window : JIPipeProjectWindow.getOpenWindows()) {
            if(!window.getProject().getCache().isEmpty()) {
                hasCache = true;
                break;
            }
        }
        if(hasCache) {
            if(JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                    "You currently have cached data across multiple projects. Should it be moved to the hard drive to free memory?",
                    "Reduce memory", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                moveAllCacheToVirtual();
            }
        }
    }

    private void checkForCacheToNonVirtual() {
        boolean hasCache = false;
        for (JIPipeProjectWindow window : JIPipeProjectWindow.getOpenWindows()) {
            if(!window.getProject().getCache().isEmpty()) {
                hasCache = true;
                break;
            }
        }
        if(hasCache) {
            if(JOptionPane.showConfirmDialog(getWorkbench().getWindow(),
                    "You currently have cached data across multiple projects. Should it be moved to the system memory to speed up the processing time?",
                    "Reduce memory", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                moveAllCacheToNonVirtual();
            }
        }
    }

    public void moveAllCacheToVirtual() {
        // Make all caches virtual by default
        JIPipeParameterCollection.setParameter(virtualDataSettings, "virtual-cache", true);
        JIPipeRunnerQueue.getInstance().enqueue(new AllCacheToVirtualRun());
    }

    public void moveAllCacheToNonVirtual() {
        JIPipeRunnerQueue.getInstance().enqueue(new AllCacheToNonVirtualRun());
    }

    public void moveCacheToVirtual() {
        // Make all caches virtual by default
        JIPipeParameterCollection.setParameter(virtualDataSettings, "virtual-cache", true);
        JIPipeRunnerQueue.getInstance().enqueue(new CacheToVirtualRun(getProject()));
    }

    public void moveCacheToNonVirtual() {
        JIPipeRunnerQueue.getInstance().enqueue(new CacheToNonVirtualRun(getProject()));
    }
}
