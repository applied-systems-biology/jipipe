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

package org.hkijena.jipipe.plugins.clij2.ui;

import net.haesleinhuepf.clij.converters.CLIJConverterService;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopImageFrameComponent;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.clij2.CLIJPluginApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.SizeFitMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;

/**
 * Graphical control panel for CLIJ
 */
public class CLIJControlPanel extends JIPipeDesktopWorkbenchPanel {

    private final JTextField openCLInfo = UIUtils.makeReadonlyBorderlessTextField("N/A");
    private final JTextField gpuModel = UIUtils.makeReadonlyBorderlessTextField("N/A");
    private final JTextField gpuMemory = UIUtils.makeReadonlyBorderlessTextField("N/A");
    private final JLabel readyLabel = new JLabel();
    private JButton clearMemoryButton;

    /**
     * @param workbench the workbench
     */
    public CLIJControlPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        initialize();
        refresh();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopMarkdownReader documentation = new JIPipeDesktopMarkdownReader(false, MarkdownText.fromPluginResource("extensions/clij2/introduction.md", new HashMap<>()));
        documentation.getScrollPane().setBorder(null);
        JIPipeDesktopParameterPanel parameterPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(),
                CLIJPluginApplicationSettings.getInstance(),
                null,
                JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterPanel.DOCUMENTATION_BELOW);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                documentation,
                parameterPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });
        add(splitPane, BorderLayout.CENTER);

        initializeHeaderPanel();
    }

    private void initializeHeaderPanel() {
        JPanel headerPanel;
        headerPanel = new JIPipeDesktopImageFrameComponent(UIUtils.getHeaderPanelBackground(), false, SizeFitMode.FitHeight, false);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));
        JLabel logo = new JLabel(new ImageIcon(ResourceUtils.getPluginResource("extensions/clij2/clij_logo.png")));
        logo.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
        headerPanel.add(logo, BorderLayout.WEST);

        JIPipeDesktopFormPanel technicalInfo = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.NONE);
        technicalInfo.setOpaque(false);
        technicalInfo.getContentPanel().setOpaque(false);

        technicalInfo.addToForm(openCLInfo, new JLabel("OpenCL version"), null);
        technicalInfo.addToForm(gpuModel, new JLabel("GPU model"), null);
        technicalInfo.addToForm(gpuMemory, new JLabel("Max GPU memory"), null);
        technicalInfo.addToForm(readyLabel, new JPanel(), null);
        technicalInfo.addVerticalGlue();

        headerPanel.add(technicalInfo, BorderLayout.EAST);

        initializeToolbar(headerPanel);

        add(headerPanel, BorderLayout.NORTH);
    }

    private void refresh() {
        try {
            CLIJ2 clij2 = CLIJ2.getInstance();
            openCLInfo.setText("" + clij2.getOpenCLVersion());
            gpuModel.setText(clij2.getGPUName());
            gpuMemory.setText(clij2.getCLIJ().getGPUMemoryInBytes() / 1024 / 1024 + " MB");
            clearMemoryButton.setEnabled(true);
            CLIJConverterService clijConverterService = getDesktopWorkbench().getContext().getService(CLIJConverterService.class);
            if (clijConverterService.getCLIJ() == clij2.getCLIJ()) {
                readyLabel.setText("Ready");
                readyLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-normal.png"));
            } else {
                readyLabel.setText("Not ready");
                readyLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
            }
        } catch (Exception e) {
            switchToUnsuccessfulLoad();
        }
    }

    private void switchToUnsuccessfulLoad() {
        openCLInfo.setText("N/A");
        gpuModel.setText("N/A");
        gpuMemory.setText("N/A");
        clearMemoryButton.setEnabled(false);
        readyLabel.setText("Not ready");
        readyLabel.setIcon(UIUtils.getIconFromResources("emblems/vcs-conflicting.png"));
    }

    private void initializeToolbar(JPanel topPanel) {
        JPanel toolBar = new JPanel();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 32, 8, 0));
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);

        JButton openWebsiteButton = new JButton("CLIJ2 website", UIUtils.getIconFromResources("actions/web-browser.png"));
        openWebsiteButton.setToolTipText("https://clij.github.io/");
        openWebsiteButton.addActionListener(e -> UIUtils.openWebsite("https://clij.github.io/"));
        openWebsiteButton.setOpaque(false);
        openWebsiteButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openWebsiteButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton initializeButton = new JButton("Re-initialize", UIUtils.getIconFromResources("actions/plug.png"));
        initializeButton.setToolTipText("(Re)Initializes CLIJ2. This is required to run GPU operations.");
        initializeButton.addActionListener(e -> reinitializeCLIJ());
        initializeButton.setOpaque(false);
        initializeButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(initializeButton);
        toolBar.add(Box.createHorizontalStrut(4));

        clearMemoryButton = new JButton("Clear memory", UIUtils.getIconFromResources("actions/delete.png"));
        clearMemoryButton.setToolTipText("Clears all images from GPU memory. Please note that this can break running analyses and cached items.");
        clearMemoryButton.addActionListener(e -> clearMemory());
        clearMemoryButton.setOpaque(false);
        clearMemoryButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(clearMemoryButton);
        toolBar.add(Box.createHorizontalStrut(4));

        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("actions/view-refresh.png"));
        refreshButton.addActionListener(e -> refresh());
        refreshButton.setOpaque(false);
        refreshButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(refreshButton);
        toolBar.add(Box.createHorizontalStrut(4));

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

    private void clearMemory() {
        getDesktopWorkbench().sendStatusBarText("Cleared all images from GPU memory. Please note that this can break running analyses and cached items.");
        CLIJ2.getInstance().clear();
        JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(), "Memory cleared. Please clear all cached data if it contains GPU images.");
    }

    private void reinitializeCLIJ() {
        try {
            CLIJPluginApplicationSettings.initializeCLIJ(getDesktopWorkbench().getContext(), true);
            getDesktopWorkbench().sendStatusBarText("Re-initialized GPU.");
        } catch (Exception e) {
            UIUtils.openErrorDialog(getDesktopWorkbench(), this, e);
        } finally {
            refresh();
        }
    }
}
