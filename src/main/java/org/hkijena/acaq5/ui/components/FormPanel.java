package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static org.hkijena.acaq5.utils.UIUtils.UI_PADDING;

/**
 * Organizes UI in a form layout with integrated help functionality, and grouping with conditional visibility
 */
public class FormPanel extends JPanel {

    private int numRows = 0;
    private String currentGroup;
    private JPanel forms = new JPanel();
    private MarkdownReader parameterHelp;

    /**
     * @param document           the default documentation. Can be null.
     * @param documentationBelow if true, show documentation below
     * @param withDocumentation  if true, show documentation
     * @param withScrolling      if true, wrap contents in a scroll bar
     */
    public FormPanel(MarkdownDocument document, boolean documentationBelow, boolean withDocumentation, boolean withScrolling) {
        setLayout(new BorderLayout());
        forms.setLayout(new GridBagLayout());

        JPanel helpPanel = new JPanel(new BorderLayout());
        parameterHelp = new MarkdownReader(false);
        parameterHelp.setDocument(document);
        helpPanel.add(parameterHelp, BorderLayout.CENTER);

        Component content;
        if (withScrolling)
            content = new JScrollPane(forms);
        else
            content = forms;

        if (withDocumentation) {
            JSplitPane splitPane = new JSplitPane(documentationBelow ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT, content, helpPanel);
            splitPane.setDividerSize(3);
            splitPane.setResizeWeight(0.33);
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

    /**
     * Creates a form panel with scrolling
     *
     * @param document           the default documentation. Can be null.
     * @param documentationBelow if true, show documentation below
     * @param withDocumentation  if true, show documentation
     */
    public FormPanel(MarkdownDocument document, boolean documentationBelow, boolean withDocumentation) {
        this(document, documentationBelow, withDocumentation, true);
    }

    /**
     * Creates a form panel with scrolling and documentation
     *
     * @param document           the default documentation. Can be null.
     * @param documentationBelow if true, show documentation below
     */
    public FormPanel(MarkdownDocument document, boolean documentationBelow) {
        this(document, documentationBelow, true);
    }

    /**
     * Creates a form panel without default documentation, documentation shown on the right hand side, and
     * scrolling enabled
     */
    public FormPanel() {
        this(null, false);
    }

    private void documentComponent(Component component, MarkdownDocument componentDocument) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                parameterHelp.setTemporaryDocument(componentDocument);
            }
        });
        component.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusGained(e);
                parameterHelp.setTemporaryDocument(componentDocument);
            }
        });
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
        forms.add(component, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 0;
                gridwidth = 2;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        ++numRows;
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
        forms.add(component, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 1;
                gridwidth = 1;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        forms.add(description, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 0;
                gridwidth = 1;
                gridy = numRows;
                insets = UI_PADDING;
            }
        });
        ++numRows;
        documentComponent(component, documentation);
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
        forms.add(component, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 0;
                gridwidth = 2;
                gridy = numRows;
                insets = UI_PADDING;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
            }
        });
        ++numRows;
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
        forms.add(new JPanel(), new GridBagConstraints() {
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
     * Removes all components
     */
    public void clear() {
        forms.removeAll();
        numRows = 0;
        revalidate();
        repaint();
    }

    public MarkdownReader getParameterHelp() {
        return parameterHelp;
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
                    insets = UI_PADDING;
                }
            });
            ++columnCount;
        }

        public JTextArea getDescriptionArea() {
            return descriptionArea;
        }
    }
}
