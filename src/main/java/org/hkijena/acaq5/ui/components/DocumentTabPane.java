/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link JTabbedPane} with larger tabs, ability to close tabs, singleton tabs that are hidden instead of being closed
 */
public class DocumentTabPane extends JTabbedPane {

    private List<DocumentTab> tabs = new ArrayList<>();
    /**
     * Contains tabs that can be closed, but opened again
     */
    private Map<String, DocumentTab> singletonTabs = new HashMap<>();

    /**
     * Creates a new instance
     */
    public DocumentTabPane() {
        super(JTabbedPane.TOP);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public List<DocumentTab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    /**
     * Adds a new tab
     *
     * @param title       The tab title
     * @param icon        The tab icon
     * @param component   The tab content
     * @param closeMode   Behavior of the close button
     * @param allowRename if true, users can rename the tab
     * @return The tab
     */
    public DocumentTab addTab(String title, Icon icon, Component component, CloseMode closeMode, boolean allowRename) {

        title = StringUtils.makeUniqueString(title, " ", tabs.stream().map(DocumentTab::getTitle).collect(Collectors.toList()));

        // Create tab panel
        JPanel tabPanel = new JPanel();
        tabPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        tabPanel.setOpaque(false);
        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.LINE_AXIS));
        JLabel titleLabel = new JLabel(title, icon, JLabel.LEFT);
        tabPanel.add(titleLabel);
        tabPanel.add(Box.createHorizontalGlue());

        DocumentTab tab = new DocumentTab(title, icon, tabPanel, component);

        if (allowRename) {
            JButton renameButton = new JButton(UIUtils.getIconFromResources("label.png"));
            renameButton.setToolTipText("Rename tab");
            UIUtils.makeBorderlessWithoutMargin(renameButton);
            renameButton.addActionListener(e -> {
                String newName = JOptionPane.showInputDialog(this, "Rename tab '" + titleLabel.getText() + "' to ...", titleLabel.getText());
                if (newName != null && !newName.isEmpty()) {
                    titleLabel.setText(newName);
                    tab.setTitle(newName);
                }
            });
            tabPanel.add(Box.createHorizontalStrut(8));
            tabPanel.add(renameButton);
        }
        if (closeMode != CloseMode.withoutCloseButton) {
            JButton closeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            closeButton.setToolTipText("Close tab");
            closeButton.setBorder(null);
            closeButton.setBackground(Color.WHITE);
            closeButton.setOpaque(false);
            closeButton.setEnabled(closeMode != CloseMode.withDisabledCloseButton);
            closeButton.addActionListener(e -> {
                if (closeMode == CloseMode.withAskOnCloseButton &&
                        JOptionPane.showConfirmDialog(component, "Do you really want to close this?",
                                "Close tab", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                    return;
                }
                remove(component);
                tabs.remove(tab);
            });
            tabPanel.add(Box.createHorizontalStrut(8));
            tabPanel.add(closeButton);

            tabPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isMiddleMouseButton(e)) {
                        if (closeMode == CloseMode.withAskOnCloseButton &&
                                JOptionPane.showConfirmDialog(component, "Do you really want to close this?",
                                        "Close tab", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
                            return;
                        }
                        remove(component);
                        tabs.remove(tab);
                    } else {
                        setSelectedComponent(component);
                    }
                }
            });
        }

        addTab(tab);
        return tab;
    }

    /**
     * Adds a new tab
     *
     * @param title     the tab icon
     * @param icon      the tab icon
     * @param component the tab content
     * @param closeMode Behavior of the close button
     * @return The tab
     */
    public DocumentTab addTab(String title, Icon icon, Component component, CloseMode closeMode) {
        return addTab(title, icon, component, closeMode, false);
    }

    /**
     * Switches the the last tab.
     * Fails silently if there are no tabs
     */
    public void switchToLastTab() {
        if (getTabCount() > 0) {
            setSelectedIndex(getTabCount() - 1);
        }
    }

    /**
     * Adds a tab that can be silently closed and brought up again
     *
     * @param id        Unique tab identifier
     * @param title     Tab title
     * @param icon      Tab icon
     * @param component Tab content
     * @param hidden    If the tab is hidden by default
     */
    public void addSingletonTab(String id, String title, Icon icon, Component component, boolean hidden) {
        DocumentTab tab = addTab(title, icon, component, CloseMode.withSilentCloseButton);
        singletonTabs.put(id, tab);
        if (hidden) {
            remove(tab.getContent());
        }
    }

    /**
     * Re-opens or selects a singleton tab
     *
     * @param id the singleton tab ID
     */
    public void selectSingletonTab(String id) {
        DocumentTab tab = singletonTabs.get(id);
        for (int i = 0; i < getTabCount(); ++i) {
            if (getTabComponentAt(i) == tab.getTabComponent()) {
                setSelectedComponent(tab.getContent());
                return;
            }
        }

        // Was closed; reinstantiate the component
        addTab(tab);
        setSelectedIndex(getTabCount() - 1);
    }

    private void addTab(DocumentTab tab) {
        addTab(tab.getTitle(), tab.getIcon(), tab.getContent());
        setTabComponentAt(getTabCount() - 1, tab.getTabComponent());
        tabs.add(tab);
    }

    /**
     * Finds the tab title for the content
     *
     * @param component the tab content
     * @return the tab title. Returns "Document" if the tab could not be found
     */
    public String findTabNameFor(Component component) {
        for (DocumentTab tab : getTabs()) {
            if (tab.getContent() == component)
                return tab.getTitle();
        }
        return "Document";
    }

    /**
     * Behavior of the tab close button
     */
    public enum CloseMode {
        /**
         * Close button silently closes the tab
         */
        withSilentCloseButton,
        /**
         * User must confirm to close the tab
         */
        withAskOnCloseButton,
        /**
         * No close button
         */
        withoutCloseButton,
        /**
         * Close button is shown, but disabled
         */
        withDisabledCloseButton
    }

    /**
     * Encapsulates a tab
     */
    public static class DocumentTab {
        private String title;
        private Icon icon;
        private Component tabComponent;
        private Component content;

        private DocumentTab(String title, Icon icon, Component tabComponent, Component content) {
            this.title = title;
            this.icon = icon;
            this.tabComponent = tabComponent;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Icon getIcon() {
            return icon;
        }

        public Component getTabComponent() {
            return tabComponent;
        }

        public Component getContent() {
            return content;
        }
    }

}
