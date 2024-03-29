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

package org.hkijena.jipipe.ui.components;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.ScrollableSizeHint;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

import static org.hkijena.jipipe.utils.UIUtils.UI_PADDING;

/**
 * Organizes UI in a form layout with integrated help functionality, and grouping with conditional visibility
 */
public class FormPanel extends JXPanel {

    /**
     * Flag that indicates no modifications, meaning (1) No documentation, and (2) no scrolling
     */
    public static final int NONE = 0;

    /**
     * Flag that indicates that a documentation panel is shown. The documentation panel is
     * attached on the right-hand side. Use DOCUMENTATION_BELOW to move it below the contents.
     * The documentation panel is shown even if the documentation provided in the constructor is null.
     */
    public static final int WITH_DOCUMENTATION = 1;

    /**
     * Flag that indicates that documentation should be supported, but there should be no dedicated panel
     */
    public static final int DOCUMENTATION_NO_UI = 16;

    /**
     * Flag that makes the backgrounds of the components transparent (non-opaque) if possible
     */
    public static final int TRANSPARENT_BACKGROUND = 32;

    /**
     * Flag that makes the content be wrapped in a {@link JScrollPane}
     */
    public static final int WITH_SCROLLING = 2;

    /**
     * Flag that indicates that documentation should be shown below if enabled.
     * This does not enable documentation! Use WITH_DOCUMENTATION for this.
     */
    public static final int DOCUMENTATION_BELOW = 4;

    /**
     * Puts the documentation into a {@link org.hkijena.jipipe.ui.components.tabs.DocumentTabPane} if WITH_DOCUMENTATION is active
     */
    public static final int TABBED_DOCUMENTATION = 8;

    private static final int COLUMN_PROPERTIES = 2;

    private static final int COLUMN_LABEL_OR_WIDE_CONTENT = 0;

    private static final int COLUMN_LABELLED_CONTENT = 1;

    private final HoverHelpEventEmitter hoverHelpEventEmitter = new HoverHelpEventEmitter();
    private final FormPanelContentPanel contentPanel = new FormPanelContentPanel();
    private final MarkdownReader parameterHelp;
    private final JLabel parameterHelpDrillDown = new JLabel();
    private final boolean withDocumentation;
    private final boolean documentationHasUI;
    private final List<GroupHeaderPanel> groupHeaderPanels = new ArrayList<>();
    private final JPanel staticContentPanel;
    private final int flags;
    private int numRows = 0;
    private JScrollPane scrollPane;
    private boolean hasVerticalGlue;

    private FormPanel redirectDocumentationTarget;

    private DocumentTabPane documentationTabPane;

    private List<FormPanelEntry> entries = new ArrayList<>();

    public FormPanel(int flags) {
        this(null, flags);
    }

    /**
     * Creates a new instance
     *
     * @param document optional documentation
     * @param flags    flags for this component
     */
    public FormPanel(MarkdownDocument document, int flags) {
        this.flags = flags;
        setLayout(new BorderLayout());
        contentPanel.setLayout(new GridBagLayout());

        JPanel helpPanel = new JPanel(new BorderLayout());
        parameterHelp = new MarkdownReader(false);
        parameterHelp.setDocument(document);
        helpPanel.add(parameterHelp, BorderLayout.CENTER);

        JToolBar helpToolbar = new JToolBar();
        helpToolbar.setFloatable(false);

        JButton switchToDefaultHelpButton = new JButton(UIUtils.getIconFromResources("actions/go-home.png"));
        UIUtils.makeFlat25x25(switchToDefaultHelpButton);
        switchToDefaultHelpButton.addActionListener(e -> switchToDefaultHelp());
        helpToolbar.add(switchToDefaultHelpButton);

        helpToolbar.add(parameterHelpDrillDown);

        helpPanel.add(helpToolbar, BorderLayout.SOUTH);

        boolean opaque = (flags & TRANSPARENT_BACKGROUND) != TRANSPARENT_BACKGROUND;
        setScrollableWidthHint(ScrollableSizeHint.FIT);
        setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        contentPanel.setScrollableWidthHint(ScrollableSizeHint.FIT);
        contentPanel.setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        contentPanel.setOpaque(opaque);
        setOpaque(opaque);


        // Determine the component that will be displayed in the help pane
        Component helpComponent;
        if ((flags & TABBED_DOCUMENTATION) == TABBED_DOCUMENTATION) {
            documentationTabPane = new DocumentTabPane(false, DocumentTabPane.TabPlacement.Top);
            documentationTabPane.addTab("Documentation", UIUtils.getIconFromResources("actions/help.png"), helpPanel, DocumentTabPane.CloseMode.withoutCloseButton);
            helpComponent = documentationTabPane;
        } else {
            helpComponent = helpPanel;
        }

        staticContentPanel = new JPanel(new BorderLayout());
        staticContentPanel.setOpaque(opaque);

        final Component content = staticContentPanel;
        if ((flags & WITH_SCROLLING) == WITH_SCROLLING) {
            scrollPane = new JScrollPane(contentPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(10);
            scrollPane.setOpaque(opaque);
            staticContentPanel.add(scrollPane, BorderLayout.CENTER);
        } else {
            staticContentPanel.add(contentPanel, BorderLayout.CENTER);
        }

        if ((flags & WITH_DOCUMENTATION) == WITH_DOCUMENTATION) {
            this.withDocumentation = true;
            if ((flags & DOCUMENTATION_NO_UI) != DOCUMENTATION_NO_UI) {
                this.documentationHasUI = true;
                boolean documentationBelow = (flags & DOCUMENTATION_BELOW) == DOCUMENTATION_BELOW;
                AutoResizeSplitPane splitPane = new AutoResizeSplitPane(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT, content, helpComponent, AutoResizeSplitPane.RATIO_3_TO_1);
                add(splitPane, BorderLayout.CENTER);
            } else {
                this.documentationHasUI = false;
                add(content, BorderLayout.CENTER);
            }
        } else {
            this.withDocumentation = false;
            this.documentationHasUI = false;
            add(content, BorderLayout.CENTER);
        }
    }

    public List<GroupHeaderPanel> getGroupHeaderPanels() {
        return Collections.unmodifiableList(groupHeaderPanels);
    }

    /**
     * The panel that surrounds the scroll pane or content panel
     *
     * @return the static content panel
     */
    public JPanel getStaticContentPanel() {
        return staticContentPanel;
    }

    public DocumentTabPane getDocumentationTabPane() {
        return documentationTabPane;
    }

    public FormPanel getRedirectDocumentationTarget() {
        return redirectDocumentationTarget;
    }

    public void setRedirectDocumentationTarget(FormPanel redirectDocumentationTarget) {
        this.redirectDocumentationTarget = redirectDocumentationTarget;
    }

    public boolean isWithDocumentation() {
        return withDocumentation;
    }

    public boolean isHasVerticalGlue() {
        return hasVerticalGlue;
    }

    public HoverHelpEventEmitter getHoverHelpEventEmitter() {
        return hoverHelpEventEmitter;
    }

    private void switchToDefaultHelp() {
        parameterHelp.setDocument(parameterHelp.getDocument());
        parameterHelpDrillDown.setIcon(null);
        parameterHelpDrillDown.setText("");
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        super.setOpaque(isOpaque);
    }

    public JXPanel getContentPanel() {
        return contentPanel;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public void updateParameterHelpDrillDown() {
        MarkdownDocument current = parameterHelp.getTemporaryDocument();
        if (current == null) {
            parameterHelpDrillDown.setIcon(null);
            parameterHelpDrillDown.setText("");
            return;
        }
        if (StringUtils.orElse(current.getMarkdown(), "").startsWith("#")) {
            String s = current.getMarkdown().split("\n")[0];
            s = s.substring(s.lastIndexOf('#') + 1);
            parameterHelpDrillDown.setIcon(UIUtils.getIconFromResources("actions/caret-right.png"));
            parameterHelpDrillDown.setText(s);
        } else {
            parameterHelpDrillDown.setIcon(UIUtils.getIconFromResources("actions/caret-right.png"));
            parameterHelpDrillDown.setText("...");
        }

    }

    /**
     * Adds a component to the form
     *
     * @param component   The component
     * @param description A description component displayed on the left hand side
     * @param <T>         Component type
     * @return The component
     */
    public <T extends Component> T addToForm(T component, Component description) {
        return addToForm(component, description, null);
    }

    /**
     * Adds a component to the form
     *
     * @param component     The component
     * @param description   A description component displayed on the left hand side
     * @param documentation Optional documentation for this component. Can be null.
     * @param <T>           Component type
     * @return The component
     */
    public <T extends Component> T addToForm(T component, Component description, MarkdownDocument documentation) {
        GridBagConstraints contentPosition = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = COLUMN_LABELLED_CONTENT;
                gridwidth = 1;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        contentPanel.add(component, contentPosition);
        if (description != null) {
            GridBagConstraints labelPosition = new GridBagConstraints() {
                {
                    anchor = GridBagConstraints.WEST;
                    gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
                    gridwidth = 1;
                    gridy = numRows;
                    insets = UI_PADDING;
                    fill = GridBagConstraints.HORIZONTAL;
                }
            };
            contentPanel.add(description, labelPosition);
        }
        Component propertiesComponent;
        if (withDocumentation)
            propertiesComponent = createAndAddEntryPropertiesComponent(component, description, numRows, documentation);
        else
            propertiesComponent = null;
        entries.add(new FormPanelEntry(numRows, description, component, propertiesComponent, false));
        ++numRows;
        return component;
    }

    private Component createAndAddEntryPropertiesComponent(Component component, Component description, int row, MarkdownDocument documentation) {
        Component newComponent = createEntryPropertiesComponent(component, description, row, documentation);
        if (newComponent != null) {
            GridBagConstraints gridBagConstraints = new GridBagConstraints() {
                {
                    anchor = GridBagConstraints.WEST;
                    gridx = COLUMN_PROPERTIES;
                    gridwidth = 1;
                    gridy = row;
                    insets = UI_PADDING;
                    fill = GridBagConstraints.NONE;
                }
            };
            contentPanel.add(newComponent, gridBagConstraints);
        }
        return newComponent;
    }

    /**
     * Adds a component. Its size is two columns.
     *
     * @param component The component
     * @param <T>       Component type
     * @return The component
     */
    public <T extends Component> T addWideToForm(T component) {
        return addWideToForm(component, null);
    }

    /**
     * Adds a component. Its size is two columns.
     *
     * @param component     The component
     * @param documentation Optional documentation. Can be null.
     * @param <T>           Component type
     * @return The component
     */
    public <T extends Component> T addWideToForm(T component, MarkdownDocument documentation) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
                gridwidth = 2;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        Component propertiesComponent;
        if (withDocumentation)
            propertiesComponent = createAndAddEntryPropertiesComponent(component, null, numRows, documentation);
        else
            propertiesComponent = null;
        contentPanel.add(component, gridBagConstraints);
        entries.add(new FormPanelEntry(numRows, null, component, propertiesComponent, true));
        ++numRows;
        return component;
    }

    public void showDocumentation(MarkdownDocument documentation) {
        if (redirectDocumentationTarget == null) {
            if (withDocumentation && documentationHasUI) {
                parameterHelp.setTemporaryDocument(documentation);
                hoverHelpEventEmitter.emit(new HoverHelpEvent(this, documentation));
                updateParameterHelpDrillDown();
            } else {
                // Just popup the documentation
                MarkdownReader reader = new MarkdownReader(false, documentation);
                JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this));
                dialog.setIconImage(UIUtils.getJIPipeIcon128());
                JPanel panel = new JPanel(new BorderLayout(8, 8));
                panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                panel.add(reader, BorderLayout.CENTER);

                dialog.setContentPane(panel);
                dialog.setTitle("Documentation");
                dialog.setModal(true);
                dialog.pack();
                dialog.setSize(new Dimension(640, 480));
                dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
                UIUtils.addEscapeListener(dialog);
                dialog.setVisible(true);
            }
        } else {
            redirectDocumentationTarget.showDocumentation(documentation);
        }
    }

    protected Component createEntryPropertiesComponent(Component component, Component description, int row, MarkdownDocument documentation) {
        if (documentation != null) {
            JButton helpButton = new JButton(UIUtils.getIconFromResources("actions/help-muted.png"));
            helpButton.setBorder(null);
            helpButton.addActionListener(e -> {
                showDocumentation(documentation);
            });
            installComponentHighlighter(helpButton, Sets.newHashSet(component, description));
            return helpButton;
        }
        return null;
    }

    public int getNumRows() {
        return numRows;
    }

    /**
     * Adds a group header
     *
     * @param text Group text
     * @param icon Group icon
     * @return the panel that allows adding more components to it
     */
    public GroupHeaderPanel addGroupHeader(String text, Icon icon) {
        GroupHeaderPanel panel = new GroupHeaderPanel(text, icon, getGroupHeaderPanels().isEmpty() ? 8 : 32);
        if((flags & TRANSPARENT_BACKGROUND) == TRANSPARENT_BACKGROUND) {
            panel.setOpaque(false);
        }
        GridBagConstraints gridBagConstraints = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
                gridwidth = 3;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        contentPanel.add(panel, gridBagConstraints);
        entries.add(new FormPanelEntry(numRows, null, panel, null, true));
        groupHeaderPanels.add(panel);
        ++numRows;
        return panel;
    }

    /**
     * Adds a group header
     *
     * @param text Group text
     * @param description  Group description
     * @param icon Group icon
     * @return the panel that allows adding more components to it
     */
    public GroupHeaderPanel addGroupHeader(String text, String description, Icon icon) {
        GroupHeaderPanel panel = new GroupHeaderPanel(text, icon, getGroupHeaderPanels().isEmpty() ? 8 : 32);
        if((flags & TRANSPARENT_BACKGROUND) == TRANSPARENT_BACKGROUND) {
            panel.setOpaque(false);
        }
        GridBagConstraints gridBagConstraints = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
                gridwidth = 3;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        contentPanel.add(panel, gridBagConstraints);
        entries.add(new FormPanelEntry(numRows, null, panel, null, true));
        groupHeaderPanels.add(panel);
        ++numRows;
        panel.setDescription(description);
        return panel;
    }

    /**
     * Adds a component that acts as Box.verticalGlue()
     */
    public void addVerticalGlue() {
        JPanel glue = new JPanel();
        glue.setOpaque(contentPanel.isOpaque());
        contentPanel.add(glue, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 0;
                gridy = numRows;
                fill = GridBagConstraints.VERTICAL;
                weightx = 0;
                weighty = 1;
            }
        });
        entries.add(new FormPanelEntry(numRows, null, glue, null, true));
        ++numRows;
        hasVerticalGlue = true;
    }

    /**
     * Removes the last row. Silently fails if there are no rows.
     */
    public void removeLastRow() {
        if (contentPanel.getComponentCount() > 0) {
            FormPanelEntry lastEntry = entries.get(entries.size() - 1);
            if (lastEntry.getContent() instanceof GroupHeaderPanel) {
                groupHeaderPanels.remove(lastEntry.getContent());
            }
            entries.remove(entries.size() - 1);
            contentPanel.remove(contentPanel.getComponentCount() - 1);
            --numRows;
        }
    }

    /**
     * Removes all components
     */
    public void clear() {
        for (FormPanelEntry entry : entries) {
            if (entry.label instanceof Disposable) {
                ((Disposable) entry.label).dispose();
            }
            if (entry.properties instanceof Disposable) {
                ((Disposable) entry.properties).dispose();
            }
            if (entry.content instanceof Disposable) {
                ((Disposable) entry.content).dispose();
            }
        }
        groupHeaderPanels.clear();
        entries.clear();
        contentPanel.removeAll();
        numRows = 0;
        hasVerticalGlue = false;
        revalidate();
        repaint();
    }

    public List<FormPanelEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public MarkdownReader getParameterHelp() {
        return parameterHelp;
    }

    /**
     * Adds a vertical glue component
     *
     * @param component the component
     * @param document  optional documentation
     */
    public void addVerticalGlue(Component component, MarkdownDocument document) {
        contentPanel.add(component, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = COLUMN_LABEL_OR_WIDE_CONTENT;
                gridy = numRows;
                fill = GridBagConstraints.BOTH;
                gridwidth = 2;
                weightx = 1;
                weighty = 1;
                insets = UI_PADDING;
            }
        });
        Component propertiesComponent = createAndAddEntryPropertiesComponent(component, null, numRows, document);
        entries.add(new FormPanelEntry(numRows, null, component, propertiesComponent, true));
        ++numRows;
        hasVerticalGlue = true;
    }

    public void installComponentHighlighter(JComponent triggerComponent, Set<Component> targetComponents) {
        triggerComponent.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                contentPanel.setHighlightedComponents(targetComponents);
            }

            @Override
            public void focusLost(FocusEvent e) {
                contentPanel.removeHighlightedComponents(targetComponents);
            }
        });
        triggerComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                contentPanel.setHighlightedComponents(targetComponents);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                contentPanel.removeHighlightedComponents(targetComponents);
            }
        });
    }

    public interface HoverHelpEventListener {
        void onFormPanelHoverHelp(HoverHelpEvent event);
    }

    public static class FormPanelEntry {

        private final int row;
        private final Component label;
        private final Component content;
        private final Component properties;
        private final boolean wide;

        public FormPanelEntry(int row, Component label, Component content, Component properties, boolean wide) {
            this.row = row;
            this.label = label;
            this.content = content;
            this.properties = properties;
            this.wide = wide;
        }

        public int getRow() {
            return row;
        }

        public Component getLabel() {
            return label;
        }

        public Component getContent() {
            return content;
        }

        public Component getProperties() {
            return properties;
        }

        public boolean isWide() {
            return wide;
        }
    }

    public static class FormPanelContentPanel extends JXPanel {
        private Set<Component> highlightedComponents;

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (highlightedComponents != null) {
                g.setColor(Color.RED);
                for (Component component : highlightedComponents) {
                    if (component != null && component.isDisplayable() && component.isVisible()) {
                        g.drawRect(component.getX(), component.getY(), component.getWidth(), component.getHeight());
                    }
                }
            }
        }

        public Set<Component> getHighlightedComponents() {
            return highlightedComponents;
        }

        public void setHighlightedComponents(Set<Component> highlightedComponents) {
            this.highlightedComponents = new HashSet<>(highlightedComponents);
            repaint();
        }

        public void removeHighlightedComponents(Set<Component> highlightedComponents) {
            if (this.highlightedComponents != null) {
                this.highlightedComponents.removeAll(highlightedComponents);
                repaint();
            }
        }
    }

    /**
     * Panel that contains a group header
     */
    public static class GroupHeaderPanel extends JPanel {
        private final JLabel titleLabel;
        private final JTextPane descriptionArea;
        private final int marginTop;
        private final Color backgroundColor;
        private final Color borderColor;
        private int columnCount = 0;

        /**
         * @param text           the text
         * @param icon           the icon
         * @param marginTop      the margin to the top
         * @param leftComponents Components added after the icon
         */
        public GroupHeaderPanel(String text, Icon icon, int marginTop, Component... leftComponents) {
            this.marginTop = marginTop;

            this.backgroundColor = ColorUtils.mix(ModernMetalTheme.PRIMARY5, ColorUtils.scaleHSV(UIManager.getColor("Panel.background"), 1, 1, 0.98f), 0.92);
            this.borderColor = backgroundColor;

            setBorder(BorderFactory.createEmptyBorder(marginTop, 0, 8, 0));
            setLayout(new GridBagLayout());

            for (Component leftComponent : leftComponents) {
                addColumn(leftComponent);
            }

            titleLabel = new JLabel(text, icon, JLabel.LEFT);
            titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            add(titleLabel, new GridBagConstraints() {
                {
                    gridx = columnCount;
                    gridy = 0;
                    anchor = GridBagConstraints.WEST;
                    insets = UI_PADDING;
                }
            });
            ++columnCount;
            add(Box.createHorizontalGlue(), new GridBagConstraints() {
                {
                    gridx = columnCount;
                    gridy = 0;
                    fill = GridBagConstraints.HORIZONTAL;
                    weightx = 1;
                    anchor = GridBagConstraints.WEST;
                    insets = UI_PADDING;
                }
            });
            ++columnCount;

            descriptionArea = UIUtils.makeBorderlessReadonlyTextPane("", true);
            descriptionArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            descriptionArea.setOpaque(false);
            descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            descriptionArea.setVisible(false);
            add(descriptionArea, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 1;
                    gridwidth = columnCount;
                    fill = GridBagConstraints.HORIZONTAL;
                    anchor = GridBagConstraints.WEST;
                    insets = UI_PADDING;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(backgroundColor);
            int x = 1;
            int y = marginTop;
            int w = getWidth() - x - 1;
            int h = getHeight() - y - 1 - 8;
            g2.fillRoundRect(x, y, w, h, 4, 4);
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, w, h, 4, 4);
        }

        public JLabel getTitleLabel() {
            return titleLabel;
        }

        /**
         * Adds a component on the right hand side
         *
         * @param component the component
         */
        public void addColumn(Component component) {
            add(component, new GridBagConstraints() {
                {
                    gridx = columnCount;
                    gridy = 0;
                    anchor = GridBagConstraints.WEST;
                    insets = new Insets(4, 2, 4, 2);
                }
            });
            ++columnCount;
        }

        public JTextPane getDescriptionArea() {
            return descriptionArea;
        }

        /**
         * Sets the description and sets the visibility
         *
         * @param description the description
         */
        public void setDescription(String description) {
            descriptionArea.setText(description);
            descriptionArea.setVisible(!StringUtils.isNullOrEmpty(description));
        }
    }

    /**
     * Event triggered when the user triggers a hover-help
     */
    public static class HoverHelpEvent extends AbstractJIPipeEvent {
        private final FormPanel formPanel;
        private final MarkdownDocument document;

        /**
         * Creates a new instance
         *
         * @param document the document
         */
        public HoverHelpEvent(FormPanel formPanel, MarkdownDocument document) {
            super(formPanel);
            this.formPanel = formPanel;
            this.document = document;
        }

        public FormPanel getFormPanel() {
            return formPanel;
        }

        public MarkdownDocument getDocument() {
            return document;
        }
    }

    public static class HoverHelpEventEmitter extends JIPipeEventEmitter<HoverHelpEvent, HoverHelpEventListener> {

        @Override
        protected void call(HoverHelpEventListener hoverHelpEventListener, HoverHelpEvent event) {
            hoverHelpEventListener.onFormPanelHoverHelp(event);
        }
    }
}
