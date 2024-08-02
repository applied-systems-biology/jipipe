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

package org.hkijena.jipipe.desktop.commons.components;

import com.google.common.collect.Sets;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
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
public class JIPipeDesktopFormPanel extends JPanel {

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
     * Flag that makes the form panel limit the width of components
     */
    public static final int WITH_LIMIT_WIDTH = 64;

    /**
     * Flag that makes the documentation panel not appear in the split pane
     */
    public static final int DOCUMENTATION_EXTERNAL = 128;

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
     * Puts the documentation into a {@link JIPipeDesktopTabPane} if WITH_DOCUMENTATION is active
     */
    public static final int TABBED_DOCUMENTATION = 8;

    private static final int COLUMN_PROPERTIES = 2;

    private static final int COLUMN_LABEL_OR_WIDE_CONTENT = 0;

    private static final int COLUMN_LABELLED_CONTENT = 1;

    private final ContextHelpEventEmitter contextHelpEventEmitter = new ContextHelpEventEmitter();
    private final FormPanelContentPanel contentPanel = new FormPanelContentPanel();
    private final boolean withDocumentation;
    private final boolean documentationHasUI;
    private final List<GroupHeaderPanel> groupHeaderPanels = new ArrayList<>();
    private final JPanel staticContentPanel;
    private final int flags;
    private int numRows = 0;
    private JScrollPane scrollPane;
    private boolean hasVerticalGlue;
    private JIPipeDesktopFormHelpPanel helpPanel = new JIPipeDesktopFormHelpPanel();

    private JIPipeDesktopFormPanel redirectDocumentationTarget;

    private JIPipeDesktopTabPane documentationTabPane;

    private final List<FormPanelEntry> entries = new ArrayList<>();

    public JIPipeDesktopFormPanel(int flags) {
        this(null, flags);
    }

    /**
     * Creates a new instance
     *
     * @param document optional documentation
     * @param flags    flags for this component
     */
    public JIPipeDesktopFormPanel(MarkdownText document, int flags) {
        this.flags = flags;
        this.helpPanel.setDefaultContent(document);
        setLayout(new BorderLayout());
        contentPanel.setLayout(new GridBagLayout());

        boolean opaque = (flags & TRANSPARENT_BACKGROUND) != TRANSPARENT_BACKGROUND;
//        setScrollableWidthHint(ScrollableSizeHint.FIT);
//        setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        contentPanel.setScrollableWidthHint(ScrollableSizeHint.FIT);
        contentPanel.setScrollableHeightHint(ScrollableSizeHint.NONE);
        contentPanel.setOpaque(opaque);
        setOpaque(opaque);

        // Determine the component that will be displayed in the help pane
        Component helpComponent;
        if ((flags & TABBED_DOCUMENTATION) == TABBED_DOCUMENTATION) {
            documentationTabPane = new JIPipeDesktopTabPane(false, JIPipeDesktopTabPane.TabPlacement.Top);
            documentationTabPane.addTab("Documentation", UIUtils.getIconFromResources("actions/help.png"), helpPanel, JIPipeDesktopTabPane.CloseMode.withoutCloseButton);
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
            if((flags & DOCUMENTATION_EXTERNAL) == DOCUMENTATION_EXTERNAL) {
                // External documentation panel
                this.documentationHasUI = true;
                add(content, BorderLayout.CENTER);
            }
            else {
                if ((flags & DOCUMENTATION_NO_UI) != DOCUMENTATION_NO_UI) {
                    this.documentationHasUI = true;
                    boolean documentationBelow = (flags & DOCUMENTATION_BELOW) == DOCUMENTATION_BELOW;
                    JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT, content, helpComponent, JIPipeDesktopSplitPane.RATIO_3_TO_1);
                    add(splitPane, BorderLayout.CENTER);
                } else {
                    this.documentationHasUI = false;
                    add(content, BorderLayout.CENTER);
                }
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

    public JIPipeDesktopTabPane getDocumentationTabPane() {
        return documentationTabPane;
    }

    public JIPipeDesktopFormPanel getRedirectDocumentationTarget() {
        return redirectDocumentationTarget;
    }

    public void setRedirectDocumentationTarget(JIPipeDesktopFormPanel redirectDocumentationTarget) {
        this.redirectDocumentationTarget = redirectDocumentationTarget;
    }

    public JIPipeDesktopFormHelpPanel getHelpPanel() {
        return helpPanel;
    }

    public void setHelpPanel(JIPipeDesktopFormHelpPanel helpPanel) {
        this.helpPanel = helpPanel;
    }

    public boolean isWithDocumentation() {
        return withDocumentation;
    }

    public boolean isHasVerticalGlue() {
        return hasVerticalGlue;
    }

    public ContextHelpEventEmitter getContextHelpEventEmitter() {
        return contextHelpEventEmitter;
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
    public <T extends Component> T addToForm(T component, Component description, MarkdownText documentation) {
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

    private Component createAndAddEntryPropertiesComponent(Component component, Component description, int row, MarkdownText documentation) {
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
    public <T extends Component> T addWideToForm(T component, MarkdownText documentation) {
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

    public void showDocumentation(MarkdownText documentation) {
        if (redirectDocumentationTarget == null) {
            if (withDocumentation && documentationHasUI) {
                helpPanel.showContent(documentation);
                contextHelpEventEmitter.emit(new ContextHelpEvent(this, documentation));
            } else {
                // Just popup the documentation
                JIPipeDesktopMarkdownReader reader = new JIPipeDesktopMarkdownReader(false, documentation);
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

    protected Component createEntryPropertiesComponent(Component component, Component description, int row, MarkdownText documentation) {
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
        if ((flags & TRANSPARENT_BACKGROUND) == TRANSPARENT_BACKGROUND) {
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
     * @param text                Group text
     * @param description         Group description
     * @param collapseDescription if the description should be collapsed
     * @param icon                Group icon
     * @return the panel that allows adding more components to it
     */
    public GroupHeaderPanel addGroupHeader(String text, String description, boolean collapseDescription, Icon icon) {
        GroupHeaderPanel panel = new GroupHeaderPanel(text, icon, getGroupHeaderPanels().isEmpty() ? 8 : 32);
        if ((flags & TRANSPARENT_BACKGROUND) == TRANSPARENT_BACKGROUND) {
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

        if (!StringUtils.isNullOrEmpty(description)) {
            if (collapseDescription) {
                panel.addDescriptionPopupToTitlePanel(description);
            } else {
                panel.addDescriptionRow(description);
            }
        }

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

    /**
     * Adds a vertical glue component
     *
     * @param component the component
     * @param document  optional documentation
     */
    public void addVerticalGlue(Component component, MarkdownText document) {
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

    public interface ContextHelpEventListener {
        void onFormPanelContextHelp(ContextHelpEvent event);
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
        private final int marginTop;
        private final Color backgroundColor;
        private final Color borderColor;
        private JPanel titlePanel;

        /**
         * @param text      the text
         * @param icon      the icon
         * @param marginTop the margin to the top
         */
        public GroupHeaderPanel(String text, Icon icon, int marginTop) {
            this.marginTop = marginTop;

            this.backgroundColor = ColorUtils.mix(JIPipeDesktopModernMetalTheme.PRIMARY5, ColorUtils.scaleHSV(UIManager.getColor("Panel.background"), 1, 1, 0.98f), 0.92);
            this.borderColor = backgroundColor;

            setBorder(BorderFactory.createEmptyBorder(marginTop, 0, 8, 0));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            initializeTitlePanel(text, icon);
        }

        private void initializeTitlePanel(String text, Icon icon) {
            titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.X_AXIS));

            // Create and add title
            JLabel titleLabel = new JLabel(text, icon, JLabel.LEFT);
            titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));

            titlePanel.add(titleLabel);

            // Add spacer
            titlePanel.add(Box.createHorizontalGlue());
            titlePanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            add(titlePanel);
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

        /**
         * Adds a component on the right hand side
         *
         * @param component the component
         */
        public void addToTitlePanel(Component component) {
            titlePanel.add(component);
        }

        public void addDescriptionRow(String text) {
            JTextPane descriptionArea = UIUtils.createBorderlessReadonlyTextPane(text, true);
            descriptionArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            descriptionArea.setOpaque(false);
            descriptionArea.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            add(descriptionArea);
        }

        public void addDescriptionPopupToTitlePanel(String text) {
            JButton helpButton = new JButton("Info", UIUtils.getIconFromResources("actions/help.png"));
            UIUtils.addBalloonToComponent(helpButton, text);
            helpButton.setOpaque(false);
            addToTitlePanel(helpButton);
        }
    }

    /**
     * Event triggered when the user triggers a hover-help
     */
    public static class ContextHelpEvent extends AbstractJIPipeEvent {
        private final JIPipeDesktopFormPanel formPanel;
        private final MarkdownText document;

        /**
         * Creates a new instance
         *
         * @param document the document
         */
        public ContextHelpEvent(JIPipeDesktopFormPanel formPanel, MarkdownText document) {
            super(formPanel);
            this.formPanel = formPanel;
            this.document = document;
        }

        public JIPipeDesktopFormPanel getFormPanel() {
            return formPanel;
        }

        public MarkdownText getDocument() {
            return document;
        }
    }

    public static class ContextHelpEventEmitter extends JIPipeEventEmitter<ContextHelpEvent, ContextHelpEventListener> {

        @Override
        protected void call(ContextHelpEventListener hoverHelpEventListener, ContextHelpEvent event) {
            hoverHelpEventListener.onFormPanelContextHelp(event);
        }
    }
}
