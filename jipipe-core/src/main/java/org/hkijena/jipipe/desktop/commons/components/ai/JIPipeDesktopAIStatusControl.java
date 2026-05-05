package org.hkijena.jipipe.desktop.commons.components.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopAIStatusControl extends JButton {
    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final AIApplicationSettings settings;

    public JIPipeDesktopAIStatusControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.settings = AIApplicationSettings.getInstance();
        initialize();
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(JIPipe.RESOURCES.getIcon16("actions/ai.png"));
        setText("N/A");
        UIUtils.addReloadablePopupMenuToButton(this, popupMenu, this::reloadMenu);
    }

    private void reloadMenu() {
        popupMenu.removeAll();
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();

        if(aiService.hasEmbeddingModel()) {
            popupMenu.add(UIUtils.createMenuItem("Unload embedding model", "Unloads the current embedding model", JIPipe.RESOURCES.getIcon16("actions/circle-stop.png"), this::stopEmbeddingModel));
        }
        else {
            popupMenu.add(UIUtils.createMenuItem("Load embedding model", "Loads the embedding model", JIPipe.RESOURCES.getIcon16("actions/circle-play.png"), this::startEmbeddingModel));
        }
    }

    private void startEmbeddingModel() {
        JIPipeDesktopAISetupDialog.checkFirstTimeSetup(workbench);
    }

    private void stopEmbeddingModel() {

    }
}
