package org.hkijena.jipipe.desktop.commons.components.ai;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class JIPipeDesktopAIStatusControl extends JButton {
    private final JIPipeDesktopProjectWorkbench workbench;
    private final JPopupMenu popupMenu = new JPopupMenu();
    private final AIApplicationSettings settings;
    private final ImageIcon defaultIcon;
    private final SpinnerIcon busyIcon; // Icon for when the model is not unloaded or idle

    public JIPipeDesktopAIStatusControl(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        this.settings = AIApplicationSettings.getInstance();
        this.defaultIcon = JIPipe.RESOURCES.getIcon16("actions/ai.png");
        this.busyIcon = new SpinnerIcon(this);
        initialize();
        updateStatus();

        // TODO: regularly update status
    }

    private void initialize() {
        UIUtils.makeButtonFlat(this);
        setIcon(defaultIcon);
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
        popupMenu.add(UIUtils.createMenuItem("Configure ...", "Opens the settings page for AI", JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openApplicationSettings));
    }

    private void openApplicationSettings() {
        workbench.openApplicationSettings("/General/AI");
    }

    private void startEmbeddingModel() {
        if(!JIPipeDesktopAISetupDialog.checkFirstTimeSetup(workbench)) {
            return;
        }
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        aiService.tryStartEmbeddingModel();
    }

    private void stopEmbeddingModel() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        aiService.tryStopEmbeddingModel();
    }

    private void updateStatus() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        if(aiService.hasEmbeddingModel()) {
            setText("AI " + aiService.getEmbeddingModelStatus());
        }
        else {
            setText("AI is offline");
        }
    }
}
