package org.hkijena.acaq5.ui.components;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hkijena.acaq5.utils.UIUtils.UI_PADDING;

/**
 * Organizes UI in a form layout with integrated help functionality, and grouping with conditional visibility
 */
public class FormPanel extends JPanel {

    private int numRows = 0;
    private String currentGroup;
    private Map<String, List<Component>> componentGroups = new HashMap<>();
    private JPanel forms = new JPanel();
    private MarkdownReader parameterHelp;

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

    public FormPanel(MarkdownDocument document, boolean documentationBelow, boolean withDocumentation) {
        this(document, documentationBelow, withDocumentation, true);
    }

    public FormPanel(MarkdownDocument document, boolean documentationBelow) {
        this(document, documentationBelow, true);
    }

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
        getComponentListForCurrentGroup().add(component);
        documentComponent(component, documentation);
        return component;
    }

    public <T extends Component> T addToForm(T component, JLabel description, MarkdownDocument documentation) {
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
        getComponentListForCurrentGroup().add(component);
        getComponentListForCurrentGroup().add(description);
        documentComponent(component, documentation);
        documentComponent(description, documentation);
        return component;
    }

    private List<Component> getComponentListForCurrentGroup() {
        List<Component> result = componentGroups.getOrDefault(currentGroup, null);
        if (result == null) {
            result = new ArrayList<>();
            componentGroups.put(currentGroup, result);
        }
        return result;
    }

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

    public void addSeparator() {
        forms.add(new JPanel() {
            {
                setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            }
        }, new GridBagConstraints() {
            {
                anchor = GridBagConstraints.WEST;
                gridx = 0;
                gridy = numRows;
                fill = GridBagConstraints.HORIZONTAL;
                weightx = 1;
                gridwidth = 2;
            }
        });
        ++numRows;
    }

    public void setGroupVisiblity(String group, boolean visible) {
        for (Component component : componentGroups.get(group)) {
            component.setVisible(visible);
        }
    }

    public void addGroupToggle(AbstractButton toggle, String group) {
        toggle.addActionListener(e -> setGroupVisiblity(group, toggle.isSelected()));
        setGroupVisiblity(group, toggle.isSelected());
    }

    public String getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(String currentGroup) {
        this.currentGroup = currentGroup;
    }

    public void clear() {
        forms.removeAll();
        numRows = 0;
        revalidate();
        repaint();
    }
}
