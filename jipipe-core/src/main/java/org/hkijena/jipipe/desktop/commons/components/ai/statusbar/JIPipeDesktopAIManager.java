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

package org.hkijena.jipipe.desktop.commons.components.ai.statusbar;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.microservice.MicroserviceState;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeEvent;
import org.hkijena.jipipe.api.microservice.MicroserviceStateChangeListener;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEvent;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEventListener;
import org.hkijena.jipipe.desktop.commons.components.ai.JIPipeDesktopAISetupDialog;
import org.hkijena.jipipe.desktop.commons.components.ai.monitor.JIPipeDesktopAIMonitorWindow;
import org.hkijena.jipipe.plugins.automation.settings.MCPApplicationSettings;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The unified AI status bar item: a flat AI button with the AI activity graph next to it.
 * Structurally parallel to {@link org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopMemoryManager}.
 * <p>
 * It replaces the former AI status control and MCP status control: the graph shows the
 * AI phase timeline and an MCP indicator line (see {@link JIPipeDesktopAIStatusGraph}),
 * and the popup merges the embedding model entries with the AI and MCP settings entries.
 * The MCP layer controls (start/stop, server URL and token, grants, pending asks) and
 * the operation and broker logs live in the AI monitor window.
 * <p>
 * The MCP Ask bubble is a {@link BalloonTip} (EdgedBalloonStyle, orientation
 * LEFT_ABOVE) attached to this item: the same bubble system as the missing author
 * info prompt, the usage statistics ask, and the project ownership prompt. Its
 * trigger is purely event-driven:
 * {@link JIPipeDesktopMCPIntegration#STATUS_EVENTS} fires whenever an ask is queued,
 * answered, timed out, or cleared by an uninstall; the bubble then shows the next queued
 * ask that targets this window (or any window when the target is unresolvable). The
 * queue, the answer paths, and the timeout live in {@link JIPipeDesktopMCPIntegration}.
 * <p>
 * All event handling marshals to the event dispatch thread: status events are emitted
 * from the endpoint's HTTP executor and the MCP lifecycle executor, embedding state
 * events from the embedding service threads.
 */
public class JIPipeDesktopAIManager extends JIPipeDesktopProjectWorkbenchPanel
        implements MicroserviceStateChangeListener, MCPStatusEventListener {

    private final JIPipeDesktopAIStatusGraph aiStatusGraph;
    private final JPopupMenu optionsMenu = new JPopupMenu();

    /**
     * The Ask request currently shown in the bubble (EDT-confined), or null.
     */
    private JIPipeDesktopMCPIntegration.PendingAsk displayedAsk;

    /**
     * The ask balloon (EDT-confined), or null until the first ask is shown. Created
     * once and hidden via {@code setVisible(false)}; the content panel is rebuilt
     * per ask and swapped in via {@link BalloonTip#setContents}.
     */
    private BalloonTip askBubble;

    public JIPipeDesktopAIManager(JIPipeDesktopProjectWorkbench workbenchUI) {
        super(workbenchUI);
        aiStatusGraph = new JIPipeDesktopAIStatusGraph(ThemeUtils.getCurrentStyle().getFontSizeTiny(), false);
        initialize();
        aiStatusGraph.startRepaintTimer();

        // Embedding status changes (weak listener; the service outlives this item).
        JIPipe.getInstance().getAiService().getEmbeddingModelService().getStateChangeEventEmitter().subscribeWeak(this);

        // MCP status changes drive the ask bubble and the graph's MCP line.
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);
    }

    private void initialize() {
        setOpaque(false);
        setLayout(new BorderLayout());
        int width = 200;
        Dimension graphSize = aiStatusGraph.getPreferredSize();
        aiStatusGraph.setPreferredSize(new Dimension(width, graphSize.height));
        aiStatusGraph.setMinimumSize(new Dimension(width, graphSize.height));
        aiStatusGraph.setMaximumSize(new Dimension(width, Short.MAX_VALUE));
        add(createOptionsButton(), BorderLayout.WEST);
        add(aiStatusGraph, BorderLayout.CENTER);

        aiStatusGraph.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 0, ThemeUtils.getCurrentStyle().getBorderColor()));
        aiStatusGraph.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                rebuildOptionsMenu();
                optionsMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    public JButton createOptionsButton() {
        JButton button = new JButton(JIPipe.RESOURCES.getIcon16("actions/ai.png"));
        UIUtils.makeButtonFlat25x25(button);
        UIUtils.addReloadablePopupMenuToButton(button, optionsMenu, this::rebuildOptionsMenu);
        return button;
    }

    /**
     * The unified popup: the embedding model entries (ported from the former AI status
     * control) plus the AI and MCP settings entries. The MCP layer controls and logs
     * live in the AI monitor window ("Open AI monitor").
     */
    private void rebuildOptionsMenu() {
        optionsMenu.removeAll();

        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        if (aiService != null) {
            MicroserviceState status = aiService.getEmbeddingModelStatus();
            String detail = aiService.getEmbeddingModelService().getStateDetail();
            boolean isBusy = status == MicroserviceState.Ready && "Busy".equals(detail);

            // Load embedding model (available when stopped or failed)
            if (status == MicroserviceState.Stopped || status == MicroserviceState.Failed) {
                optionsMenu.add(UIUtils.createMenuItem("Load embedding model", "Loads the embedding model",
                        JIPipe.RESOURCES.getIcon16("actions/circle-play.png"), this::startEmbeddingModel));
            }

            // Unload embedding model (available when ready; busy defers unload until task completes)
            if (status == MicroserviceState.Ready) {
                String text = isBusy
                        ? "Unload embedding model (after current task)"
                        : "Unload embedding model";
                String tooltip = isBusy
                        ? "Unloads the embedding model after the current task completes"
                        : "Unloads the current embedding model";
                optionsMenu.add(UIUtils.createMenuItem(text, tooltip,
                        JIPipe.RESOURCES.getIcon16("actions/circle-stop.png"), this::stopEmbeddingModel));
            }

            // Show error details if failed
            if (status == MicroserviceState.Failed) {
                String error = aiService.getEmbeddingModelError();
                if (error != null && !error.isEmpty()) {
                    optionsMenu.add(UIUtils.createMenuItem("Show error details", "Shows the error that occurred",
                            JIPipe.RESOURCES.getIcon16("actions/help.png"), this::showErrorDetails));
                }
            }
        }

        optionsMenu.addSeparator();
        optionsMenu.add(UIUtils.createMenuItem("Open AI monitor", "Opens the AI monitor window",
                JIPipe.RESOURCES.getIcon16("actions/ai.png"), this::openAIMonitor));
        optionsMenu.addSeparator();
        optionsMenu.add(UIUtils.createMenuItem("Configure AI ...", "Opens the settings page for AI",
                JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openAISettingsSection));
        optionsMenu.add(UIUtils.createMenuItem("Configure MCP ...", "Opens the settings page for the MCP server",
                JIPipe.RESOURCES.getIcon16("actions/configure.png"), this::openMCPSettingsSection));
    }

    private void openAIMonitor() {
        JIPipeDesktopAIMonitorWindow window = new JIPipeDesktopAIMonitorWindow(getDesktopWorkbench());
        window.setLocationRelativeTo(getDesktopWorkbench().getWindow());
        window.setVisible(true);
    }

    private void openAISettingsSection() {
        getDesktopProjectWorkbench().openSettingsSection(SettingsScope.APPLICATION, AIApplicationSettings.ID);
    }

    private void openMCPSettingsSection() {
        getDesktopProjectWorkbench().openSettingsSection(SettingsScope.APPLICATION, MCPApplicationSettings.ID);
    }

    private void startEmbeddingModel() {
        if (!JIPipeDesktopAISetupDialog.checkFirstTimeSetup(getDesktopProjectWorkbench())) {
            return;
        }
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        aiService.tryStartEmbeddingModel();
    }

    private void stopEmbeddingModel() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        aiService.tryStopEmbeddingModel();
    }

    private void showErrorDetails() {
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        String error = aiService.getEmbeddingModelError();
        org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs.message(this, "AI Error",
                "AI model error:\n" + (error != null ? error : "Unknown error"));
    }

    @Override
    public void onMicroserviceStateChanged(MicroserviceStateChangeEvent event) {
        // State changes arrive from the service thread; the graph repaints on the EDT.
        // The popup rebuilds on demand when opened.
        SwingUtilities.invokeLater(() -> UIUtils.repaintLater(aiStatusGraph));
    }

    @Override
    public void onMCPStatusChanged(MCPStatusEvent event) {
        // Status events arrive from background threads; every UI update marshals to the EDT.
        SwingUtilities.invokeLater(this::refreshAskBubble);
    }

    // ── Ask bubble (re-hosted from the former MCP status control) ──

    /**
     * Updates the bubble after a status change: hides it when the displayed ask is gone,
     * then shows the next queued ask that targets this window (or any window, when the
     * target is unresolvable). EDT only.
     */
    private void refreshAskBubble() {
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (displayedAsk != null && (integration == null || displayedAsk.isAnswered()
                || !integration.getPendingAsks().contains(displayedAsk))) {
            hideAskBubble();
        }
        if (displayedAsk == null && integration != null) {
            for (JIPipeDesktopMCPIntegration.PendingAsk ask : integration.getPendingAsks()) {
                if (isTargetOf(ask)) {
                    showAskBubble(ask);
                    break;
                }
            }
        }
    }

    /**
     * Whether this item should display the ask: its project targets this window, or the
     * target is unresolvable (those show in every open window's item).
     */
    private boolean isTargetOf(JIPipeDesktopMCPIntegration.PendingAsk ask) {
        if (ask.getProjectKey() == null) {
            return true;
        }
        JIPipeDesktopProjectWindow target = findWindowByKey(ask.getProjectKey());
        return target == null || target == getDesktopProjectWorkbench().getWindow();
    }

    /**
     * Shows the ask balloon attached to this item. EDT only. One ask at a time; an
     * already shown ask is not re-shown.
     */
    private void showAskBubble(JIPipeDesktopMCPIntegration.PendingAsk ask) {
        if (displayedAsk == ask && askBubble != null && askBubble.isVisible()) {
            return;
        }
        hideAskBubble();
        displayedAsk = ask;

        JPanel content = buildAskBalloonContent(ask);
        if (askBubble == null) {
            askBubble = createAskBalloon(content);
        } else {
            askBubble.setContents(content);
        }
        askBubble.refreshLocation();
        askBubble.setVisible(true);
        getDesktopProjectWorkbench().getProjectWindow().registerBalloon(askBubble);
    }

    /**
     * Creates the ask balloon once, in the house style of the other project window
     * balloons (missing author info prompt, usage statistics ask, project ownership
     * prompt). Hidden until {@code showAskBubble} displays it.
     */
    private BalloonTip createAskBalloon(JPanel content) {
        EdgedBalloonStyle style = new EdgedBalloonStyle(UIManager.getColor("TextField.background"), ThemeUtils.getCurrentStyle().getPrimaryColor());
        BalloonTip balloonTip = new BalloonTip(
                this,
                content,
                style,
                BalloonTip.Orientation.LEFT_ABOVE,
                BalloonTip.AttachLocation.ALIGNED,
                30, 10,
                true
        );
        balloonTip.setVisible(false);

        JButton closeButton = new JButton(JIPipe.RESOURCES.getIcon16("actions/window-close.png"));
        closeButton.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        closeButton.setOpaque(false);
        closeButton.addActionListener(e -> hideAskBubble());
        balloonTip.setCloseButton(closeButton, false);
        return balloonTip;
    }

    /**
     * Builds the balloon content for one ask: the title, the operation id and the
     * project display name, and the answer buttons. The buttons only answer; the
     * queue, grants, and timeout live in {@link JIPipeDesktopMCPIntegration}.
     */
    private JPanel buildAskBalloonContent(JIPipeDesktopMCPIntegration.PendingAsk ask) {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(UIUtils.createJLabel("MCP asks to run", 16), BorderLayout.NORTH);
        content.add(new JLabel("<html><strong>" + escapeHtml(ask.getOperationId()) + "</strong><br/>Project: "
                + escapeHtml(displayProjectName(ask.getProjectKey())) + "</html>"), BorderLayout.CENTER);
        JPanel buttons = UIUtils.boxHorizontal(
                UIUtils.createButton("Allow once", JIPipe.RESOURCES.getIcon16("actions/dialog-apply.png"), () -> answerAsk(ask, true, false)),
                UIUtils.createButton("Allow always (this project)", JIPipe.RESOURCES.getIcon16("actions/im-user-online.png"), () -> answerAsk(ask, true, true)),
                UIUtils.createButton("Deny", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), () -> answerAsk(ask, false, false))
        );
        buttons.setOpaque(false);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }

    /**
     * Escapes a dynamic value (operation id, project name) for safe rendering inside
     * the balloon's HTML label.
     */
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void hideAskBubble() {
        if (askBubble != null) {
            askBubble.setVisible(false);
        }
        displayedAsk = null;
    }

    private void answerAsk(JIPipeDesktopMCPIntegration.PendingAsk ask, boolean allow, boolean grantAlways) {
        hideAskBubble();
        JIPipeDesktopMCPIntegration integration = JIPipeDesktopMCPIntegration.getActive();
        if (integration != null) {
            integration.answerAsk(ask, allow, grantAlways);
        }
    }

    /**
     * Display name of a project key: the project name when the window is open, otherwise
     * the shortened key. Must run on the EDT (reads the open-window set).
     */
    private String displayProjectName(String projectKey) {
        JIPipeDesktopProjectWindow window = findWindowByKey(projectKey);
        if (window != null && window.getProject() != null && window.getProject().getMetadata() != null
                && window.getProject().getMetadata().getName() != null) {
            return window.getProject().getMetadata().getName();
        }
        if (projectKey != null && projectKey.length() > 8) {
            return "..." + projectKey.substring(projectKey.length() - 8);
        }
        return projectKey != null ? projectKey : "unknown project";
    }

    private JIPipeDesktopProjectWindow findWindowByKey(String projectKey) {
        if (projectKey == null) {
            return null;
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.getSessionId().toString().equals(projectKey)) {
                return window;
            }
        }
        return null;
    }
}
