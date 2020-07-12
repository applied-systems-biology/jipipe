package org.hkijena.jipipe.extensions.clij2.ui;

import net.haesleinhuepf.clij.converters.CLIJConverterService;
import net.haesleinhuepf.clij2.CLIJ2;
import org.hkijena.jipipe.extensions.clij2.CLIJSettings;
import org.hkijena.jipipe.ui.JIPipeInfoUI;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.MarkdownReader;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Graphical control panel for CLIJ
 */
public class CLIJControlPanel extends JIPipeWorkbenchPanel {

    private final JTextField openCLInfo = UIUtils.makeReadonlyBorderlessTextField("NA");
    private final JTextField gpuModel = UIUtils.makeReadonlyBorderlessTextField("NA");
    private final JTextField gpuMemory = UIUtils.makeReadonlyBorderlessTextField("NA");
    private final JLabel readyLabel = new JLabel();
    private JButton clearMemoryButton;

    /**
     * @param workbench the workbench
     */
    public CLIJControlPanel(JIPipeWorkbench workbench) {
        super(workbench);
        initialize();
        refresh();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        MarkdownReader documentation = new MarkdownReader(false, MarkdownDocument.fromPluginResource("extensions/clij2/introduction.md"));
        documentation.getScrollPane().setBorder(null);
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(),
                CLIJSettings.getInstance(),
                null,
                ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.DOCUMENTATION_BELOW);
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
        JPanel headerPanel = new JIPipeInfoUI.BackgroundPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.DARK_GRAY));
        headerPanel.setPreferredSize(new Dimension(headerPanel.getPreferredSize().width, 200));
        JLabel logo = new JLabel(new ImageIcon(ResourceUtils.getPluginResource("extensions/clij2/clij_logo.png")));
        logo.setBorder(BorderFactory.createEmptyBorder(0, 32, 0, 0));
        headerPanel.add(logo, BorderLayout.WEST);

        FormPanel technicalInfo = new FormPanel(null, FormPanel.NONE);
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
            CLIJConverterService clijConverterService = getWorkbench().getContext().getService(CLIJConverterService.class);
            if (clijConverterService.getCLIJ() == clij2.getCLIJ()) {
                readyLabel.setText("Ready");
                readyLabel.setIcon(UIUtils.getIconFromResources("check-circle-green.png"));
            } else {
                readyLabel.setText("Not ready");
                readyLabel.setIcon(UIUtils.getIconFromResources("error.png"));
            }
        } catch (Exception e) {
            switchToUnsuccessfulLoad();
        }
    }

    private void switchToUnsuccessfulLoad() {
        openCLInfo.setText("NA");
        gpuModel.setText("NA");
        gpuMemory.setText("NA");
        clearMemoryButton.setEnabled(false);
        readyLabel.setText("Not ready");
        readyLabel.setIcon(UIUtils.getIconFromResources("error.png"));
    }

    private void initializeToolbar(JPanel topPanel) {
        JPanel toolBar = new JPanel();
        toolBar.setBorder(BorderFactory.createEmptyBorder(0, 32, 8, 0));
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setOpaque(false);

        JButton openWebsiteButton = new JButton("CLIJ2 website", UIUtils.getIconFromResources("filetype-html.png"));
        openWebsiteButton.setToolTipText("https://clij.github.io/");
        openWebsiteButton.addActionListener(e -> UIUtils.openWebsite("https://clij.github.io/"));
        openWebsiteButton.setOpaque(false);
        openWebsiteButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(openWebsiteButton);
        toolBar.add(Box.createHorizontalStrut(4));

        JButton initializeButton = new JButton("Re-initialize", UIUtils.getIconFromResources("plug.png"));
        initializeButton.setToolTipText("(Re)Initializes CLIJ2. This is required to run GPU operations.");
        initializeButton.addActionListener(e -> reinitializeCLIJ());
        initializeButton.setOpaque(false);
        initializeButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(initializeButton);
        toolBar.add(Box.createHorizontalStrut(4));

        clearMemoryButton = new JButton("Clear memory", UIUtils.getIconFromResources("delete.png"));
        clearMemoryButton.setToolTipText("Clears all images from GPU memory. Please note that this can break running analyses and cached items.");
        clearMemoryButton.addActionListener(e -> clearMemory());
        clearMemoryButton.setOpaque(false);
        clearMemoryButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(clearMemoryButton);
        toolBar.add(Box.createHorizontalStrut(4));

        toolBar.add(Box.createHorizontalGlue());

        JButton refreshButton = new JButton("Refresh", UIUtils.getIconFromResources("refresh.png"));
        refreshButton.addActionListener(e -> refresh());
        refreshButton.setOpaque(false);
        refreshButton.setBackground(new Color(0, 0, 0, 0));
        toolBar.add(refreshButton);
        toolBar.add(Box.createHorizontalStrut(4));

        topPanel.add(toolBar, BorderLayout.SOUTH);
    }

    private void clearMemory() {
        getWorkbench().sendStatusBarText("Cleared all images from GPU memory. Please note that this can break running analyses and cached items.");
        CLIJ2.getInstance().clear();
        JOptionPane.showMessageDialog(getWorkbench().getWindow(), "Memory cleared. Please clear all cached data if it contains GPU images.");
    }

    private void reinitializeCLIJ() {
        try {
            CLIJSettings.initializeCLIJ(getWorkbench().getContext(), true);
            getWorkbench().sendStatusBarText("Re-initialized GPU.");
        } catch (Exception e) {
            UIUtils.openErrorDialog(this, e);
        } finally {
            refresh();
        }
    }
}
