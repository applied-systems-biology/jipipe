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

package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.ScrollableSizeHint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;

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
     * Flag that makes the content be wrapped in a {@link JScrollPane}
     */
    public static final int WITH_SCROLLING = 2;

    /**
     * Flag that indicates that documentation should be shown below if enabled.
     * This does not enable documentation! Use WITH_DOCUMENTATION for this.
     */
    public static final int DOCUMENTATION_BELOW = 4;

    /**
     * Flag that items should only be displayed when necessary
     */
    public static final int WITH_INFINITE_SCROLLING = 8;

    private final EventBus eventBus = new EventBus();
    private final boolean isInfiniteScrolling;
    private int numRows = 0;
    private JXPanel contentPanel = new JXPanel();
    private MarkdownReader parameterHelp;
    private JScrollPane scrollPane;
    private JLabel parameterHelpDrillDown = new JLabel();
    private ArrayDeque<FutureComponent> infiniteScrollingQueue = new ArrayDeque<>();

    /**
     * Creates a new instance
     *
     * @param document optional documentation
     * @param flags    flags for this component
     */
    public FormPanel(MarkdownDocument document, int flags) {

        if ((flags & WITH_INFINITE_SCROLLING) == WITH_INFINITE_SCROLLING)
            flags |= WITH_SCROLLING;
        this.isInfiniteScrolling = (flags & WITH_INFINITE_SCROLLING) == WITH_INFINITE_SCROLLING;

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

        setScrollableWidthHint(ScrollableSizeHint.FIT);
        setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);
        contentPanel.setScrollableWidthHint(ScrollableSizeHint.FIT);
        contentPanel.setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);

        Component content;
        if ((flags & WITH_SCROLLING) == WITH_SCROLLING) {
            scrollPane = new JScrollPane(contentPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(10);
            content = scrollPane;
        } else
            content = contentPanel;

        if ((flags & WITH_DOCUMENTATION) == WITH_DOCUMENTATION) {
            boolean documentationBelow = (flags & DOCUMENTATION_BELOW) == DOCUMENTATION_BELOW;
            JSplitPane splitPane = new JSplitPane(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT, content, helpPanel);
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
        } else {
            add(content, BorderLayout.CENTER);
        }
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

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private void documentComponent(Component component, MarkdownDocument componentDocument) {
        if (componentDocument != null) {
            Toolkit.getDefaultToolkit().addAWTEventListener(new ComponentDocumentationHandler(this, component, componentDocument),
                    AWTEvent.MOUSE_EVENT_MASK);
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    parameterHelp.setTemporaryDocument(componentDocument);
                    getEventBus().post(new HoverHelpEvent(componentDocument));
                    updateParameterHelpDrillDown();
                }
            });
            component.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    super.focusGained(e);
                    parameterHelp.setTemporaryDocument(componentDocument);
                    getEventBus().post(new HoverHelpEvent(componentDocument));
                    updateParameterHelpDrillDown();
                }
            });
        }
    }

    private void updateParameterHelpDrillDown() {
        MarkdownDocument current = parameterHelp.getTemporaryDocument();
        if (current == null) {
            parameterHelpDrillDown.setIcon(null);
            parameterHelpDrillDown.setText("");
            return;
        }
        if (StringUtils.orElse(current.getMarkdown(), "").startsWith("#")) {
            String s = current.getMarkdown().split("\n")[0];
            s = s.substring(s.lastIndexOf('#') + 1);
            parameterHelpDrillDown.setIcon(UIUtils.getIconFromResources("actions/arrow-right.png"));
            parameterHelpDrillDown.setText(s);
        } else {
            parameterHelpDrillDown.setIcon(UIUtils.getIconFromResources("actions/arrow-right.png"));
            parameterHelpDrillDown.setText("...");
        }

    }

    /**
     * Adds a component to the form
     *
     * @param component     The component
     * @param documentation Optional documentation for this component. Can be null.
     * @param <T>           Component type
     * @return The component
     */
    public <T extends Component> T addToForm(T component, MarkdownDocument documentation) {
        GridBagConstraints contentPosition = new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 0;
                gridwidth = 2;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        if (!isInfiniteScrolling)
            contentPanel.add(component, contentPosition);
        else
            infiniteScrollingQueue.add(new FutureComponent(null, null, component, contentPosition, false));
        ++numRows;
        if (documentation != null)
            documentComponent(component, documentation);
        return component;
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
                gridx = 1;
                gridwidth = 1;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        if (!isInfiniteScrolling)
            contentPanel.add(component, contentPosition);
        if (description != null) {
            GridBagConstraints labelPosition = new GridBagConstraints() {
                {
                    anchor = GridBagConstraints.WEST;
                    gridx = 0;
                    gridwidth = 1;
                    gridy = numRows;
                    insets = UI_PADDING;
                }
            };
            if (!isInfiniteScrolling)
                contentPanel.add(description, labelPosition);
            else {
                infiniteScrollingQueue.add(new FutureComponent(description, labelPosition, component, contentPosition, false));
            }
        } else if (isInfiniteScrolling) {
            infiniteScrollingQueue.add(new FutureComponent(null, null, component, contentPosition, false));
        }
        ++numRows;
        if (documentation != null)
            documentComponent(component, documentation);
        if (documentation != null && description != null)
            documentComponent(description, documentation);
        return component;
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
                gridx = 0;
                gridwidth = 2;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        };
        if (!isInfiniteScrolling)
            contentPanel.add(component, gridBagConstraints);
        else
            infiniteScrollingQueue.add(new FutureComponent(null, null, component, gridBagConstraints, true));
        ++numRows;
        if (documentation != null)
            documentComponent(component, documentation);
        return component;
    }

    /**
     * Adds a group header
     *
     * @param text Group text
     * @param icon Group icon
     * @return the panel that allows adding more components to it
     */
    public GroupHeaderPanel addGroupHeader(String text, Icon icon) {
        GroupHeaderPanel panel = new GroupHeaderPanel(text, icon);
        addWideToForm(panel, null);
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
        ++numRows;
    }

    /**
     * Removes the last row. Silently fails if there are no rows.
     */
    public void removeLastRow() {
        if (isInfiniteScrolling && !infiniteScrollingQueue.isEmpty()) {
            infiniteScrollingQueue.removeLast();
        } else if (contentPanel.getComponentCount() > 0) {
            contentPanel.remove(contentPanel.getComponentCount() - 1);
            --numRows;
        }
    }

    /**
     * Removes all components
     */
    public void clear() {
        infiniteScrollingQueue.clear();
        contentPanel.removeAll();
        numRows = 0;
        revalidate();
        repaint();
    }

    public MarkdownReader getParameterHelp() {
        return parameterHelp;
    }

    public EventBus getEventBus() {
        return eventBus;
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
                gridx = 0;
                gridy = numRows;
                fill = GridBagConstraints.VERTICAL;
                gridwidth = 2;
                weightx = 1;
                weighty = 1;
            }
        });
        documentComponent(component, document);
        ++numRows;
    }

    /**
     * Panel that contains a group header
     */
    public static class GroupHeaderPanel extends JPanel {
        private final JLabel titleLabel;
        private JTextArea descriptionArea;
        private int columnCount = 0;

        /**
         * @param text the text
         * @param icon the icon
         */
        public GroupHeaderPanel(String text, Icon icon) {
            setBorder(BorderFactory.createEmptyBorder(8, 0, 4, 0));
            setLayout(new GridBagLayout());

            titleLabel = new JLabel(text, icon, JLabel.LEFT);
            add(titleLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    anchor = GridBagConstraints.WEST;
                    insets = UI_PADDING;
                }
            });
            ++columnCount;

            add(new JSeparator(), new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    fill = GridBagConstraints.HORIZONTAL;
                    weightx = 1;
                    anchor = GridBagConstraints.WEST;
                    insets = UI_PADDING;
                }
            });
            ++columnCount;

            descriptionArea = UIUtils.makeReadonlyTextArea("");
            descriptionArea.setOpaque(false);
            descriptionArea.setBorder(null);
            descriptionArea.setVisible(false);
            add(descriptionArea, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 1;
                    gridwidth = 2;
                    fill = GridBagConstraints.HORIZONTAL;
                    anchor = GridBagConstraints.WEST;
                    insets = UI_PADDING;
                }
            });
        }

        public JLabel getTitleLabel() {
            return titleLabel;
        }

        /**
         * Adds an additional component on the right hand side
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

        public JTextArea getDescriptionArea() {
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
     * Mouse handler to target specific components
     */
    private static class ComponentDocumentationHandler implements AWTEventListener {

        private final WeakReference<FormPanel> formPanel;
        private final WeakReference<Component> target;
        private final MarkdownDocument componentDocument;

        private ComponentDocumentationHandler(FormPanel formPanel, Component component, MarkdownDocument componentDocument) {
            this.formPanel = new WeakReference<>(formPanel);
            this.target = new WeakReference<>(component);
            this.componentDocument = componentDocument;
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            Component component = target.get();
            FormPanel panel = formPanel.get();
            if (component == null || panel == null) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                return;
            }
            if (!component.isDisplayable()) {
                return;
            }
            if (!component.isVisible()) {
                return;
            }
            if (event instanceof MouseEvent) {
                MouseEvent mouseEvent = (MouseEvent) event;
                if (((MouseEvent) event).getComponent() == component || SwingUtilities.isDescendingFrom(mouseEvent.getComponent(), component)) {
                    try {
                        Point componentLocation = component.getLocationOnScreen();
                        boolean isInComponent = mouseEvent.getXOnScreen() >= componentLocation.x &&
                                mouseEvent.getYOnScreen() >= componentLocation.y &&
                                mouseEvent.getXOnScreen() < (component.getWidth() + componentLocation.x) &&
                                mouseEvent.getYOnScreen() < (component.getHeight() + componentLocation.y);
                        if (isInComponent && panel.parameterHelp.getTemporaryDocument() != componentDocument) {
                            panel.parameterHelp.setTemporaryDocument(componentDocument);
                            panel.getEventBus().post(new HoverHelpEvent(componentDocument));
                            panel.updateParameterHelpDrillDown();
                        }
                    } catch (IllegalComponentStateException e) {
                        // Workaround for Java bug
                        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                    }
                }
            }
        }
    }

    public static class FutureComponent {
        private final Component label;
        private final GridBagConstraints labelPosition;
        private final Component content;
        private final GridBagConstraints contentPosition;
        private final boolean wide;

        public FutureComponent(Component label, GridBagConstraints labelPosition, Component content, GridBagConstraints contentPosition, boolean wide) {
            this.label = label;
            this.labelPosition = labelPosition;
            this.content = content;
            this.contentPosition = contentPosition;
            this.wide = wide;
        }
    }

    /**
     * Event triggered when the user triggers a hover-help
     */
    public static class HoverHelpEvent {
        private final MarkdownDocument document;

        /**
         * Creates a new instance
         *
         * @param document the document
         */
        public HoverHelpEvent(MarkdownDocument document) {
            this.document = document;
        }

        public MarkdownDocument getDocument() {
            return document;
        }
    }
}
