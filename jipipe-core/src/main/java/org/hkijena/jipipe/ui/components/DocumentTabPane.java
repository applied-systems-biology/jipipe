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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.theme.CustomTabbedPaneUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link JTabbedPane} with larger tabs, ability to close tabs, singleton tabs that are hidden instead of being closed
 */
public class DocumentTabPane extends JPanel {

    private DnDTabbedPane tabbedPane;

    /**
     * List of open tabs
     */
    private Set<DocumentTab> tabs = new HashSet<>();

    /**
     * Last tabs have priority over lower index tabs
     */
    private List<DocumentTab> tabHistory = new ArrayList<>();

    /**
     * Contains tabs that can be closed, but opened again
     */
    private BiMap<String, DocumentTab> singletonTabs = HashBiMap.create();

    /**
     * Creates a new instance
     */
    public DocumentTabPane() {
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabbedPane = new DnDTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.addChangeListener(e -> updateTabHistory());
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    int tabIndex = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    if (tabIndex >= 0 && tabIndex < tabbedPane.getTabCount()) {
                        Component component = tabbedPane.getTabComponentAt(tabIndex);
                        for (DocumentTab tab : tabs) {
                            if (tab.getTabComponent() == component) {
                                closeTab(tab);
                                break;
                            }
                        }

                    }
                }
            }
        });
        if (JIPipe.getInstance() != null && GeneralUISettings.getInstance().getTheme().isModern()) {
            tabbedPane.setUI(new CustomTabbedPaneUI());
        }
        add(tabbedPane, BorderLayout.CENTER);
    }

    public BiMap<String, DocumentTab> getSingletonTabs() {
        return ImmutableBiMap.copyOf(singletonTabs);
    }

    /**
     * Updates the order of tabs we go though when tabs are closed
     */
    private void updateTabHistory() {
        DocumentTab tab = getTabContaining(tabbedPane.getSelectedComponent());
        if (tab != null) {
            int indexInHistory = tabHistory.indexOf(tab);
            if (indexInHistory >= 0) {
                tabHistory.remove(indexInHistory);
            }
            tabHistory.add(tab);
        }
    }

    /**
     * Returns the tab that contains the specified content
     *
     * @param content the content
     * @return the tab containing the content. Null if not found
     */
    public DocumentTab getTabContaining(Component content) {
        return tabs.stream().filter(t -> t.getContent() == content).findFirst().orElse(null);
    }

    public Set<DocumentTab> getTabs() {
        return Collections.unmodifiableSet(tabs);
    }

    /**
     * Returns all tabs that contain components of provided type or components that inherit from it
     *
     * @param klass component type.
     * @return tabs that contain components of provided type or components that inherit from it
     */
    public List<DocumentTab> getTabsContaining(Class<? extends Component> klass) {
        return tabs.stream().filter(tab -> klass.isAssignableFrom(tab.getContent().getClass())).collect(Collectors.toList());
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

        tabPanel.setOpaque(false);

//        if (GeneralUISettings.getInstance().getLookAndFeel() == GeneralUISettings.LookAndFeel.FlatIntelliJLaf) {
//            tabPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, Color.GRAY),
//                    BorderFactory.createEmptyBorder(4, 4, 2, 4)));
//        } else {
//            tabPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
//        }
        tabPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.LINE_AXIS));
        JLabel titleLabel = new JLabel(title, icon, JLabel.LEFT);
        tabPanel.add(titleLabel);
        tabPanel.add(Box.createHorizontalGlue());

        DocumentTab tab = new DocumentTab(title, icon, tabPanel, component, closeMode);

        tab.getEventBus().register(new Object() {
            @Subscribe
            public void onPropertyChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                titleLabel.setText(tab.getTitle());
            }
        });

        if (allowRename) {
            JButton renameButton = new JButton(UIUtils.getIconFromResources("actions/tag.png"));
            renameButton.setToolTipText("Rename tab");
            UIUtils.makeBorderlessWithoutMargin(renameButton);
            renameButton.addActionListener(e -> {
                String newName = JOptionPane.showInputDialog(this, "Rename tab '" + titleLabel.getText() + "' to ...", titleLabel.getText());
                if (newName != null && !newName.isEmpty()) {
                    tab.setTitle(newName);
                }
            });
            tabPanel.add(Box.createHorizontalStrut(8));
            tabPanel.add(renameButton);
        }
        if (closeMode != CloseMode.withoutCloseButton) {
            JButton closeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
            closeButton.setToolTipText("Close tab");
            closeButton.setBorder(null);
//            if (GeneralUISettings.getInstance().getLookAndFeel() == GeneralUISettings.LookAndFeel.FlatIntelliJLaf) {
//                closeButton.setBackground(new Color(242, 242, 242));
//            } else {
//                closeButton.setBackground(Color.WHITE);
//            }
            closeButton.setBackground(UIManager.getColor("TextArea.background"));

            closeButton.setOpaque(false);
            closeButton.setEnabled(closeMode != CloseMode.withDisabledCloseButton);
            closeButton.addActionListener(e -> closeTab(tab));
            tabPanel.add(Box.createHorizontalStrut(8));
            tabPanel.add(closeButton);
        }

        addTab(tab);
        return tab;
    }

    /**
     * Closes the tab. This includes user interaction.
     * Silently fails if the tab is not closable
     *
     * @param tab the tab
     */
    public void closeTab(DocumentTab tab) {
        if (!tabs.contains(tab))
            return;
        if (tab.closeMode == CloseMode.withoutCloseButton || tab.closeMode == CloseMode.withDisabledCloseButton)
            return;
        if (!GeneralUISettings.getInstance().isNeverAskOnClosingTabs() && tab.closeMode == CloseMode.withAskOnCloseButton) {
            if (JOptionPane.showConfirmDialog(tab.getContent(), "Do you really want to close this?",
                    "Close tab", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                forceCloseTab(tab);
            }
        } else {
            forceCloseTab(tab);
        }
    }

    /**
     * Closes the tab. Has no user interaction.
     *
     * @param tab the tab
     */
    public void forceCloseTab(DocumentTab tab) {
        tabs.remove(tab);
        tabHistory.remove(tab);

        if (!tabHistory.isEmpty()) {
            tabbedPane.setSelectedComponent(tabHistory.get(tabHistory.size() - 1).getContent());
        }
        tabbedPane.remove(tab.getContent());
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

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * @return the number of managed tabs
     */
    public int getTabCount() {
        return tabbedPane.getTabCount();
    }

    /**
     * Switches the the last tab.
     * Fails silently if there are no tabs
     */
    public void switchToLastTab() {
        if (getTabCount() > 0) {
            tabbedPane.setSelectedIndex(getTabCount() - 1);
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
    public DocumentTab addSingletonTab(String id, String title, Icon icon, Component component, boolean hidden) {
        DocumentTab tab = addTab(title, icon, component, CloseMode.withSilentCloseButton);
        singletonTabs.put(id, tab);
        if (hidden) {
            forceCloseTab(tab);
        }
        return tab;
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
    public DocumentTab addSingletonTab(String id, String title, Icon icon, Component component, CloseMode closeMode, boolean hidden) {
        DocumentTab tab = addTab(title, icon, component, closeMode);
        singletonTabs.put(id, tab);
        if (hidden) {
            forceCloseTab(tab);
        }
        return tab;
    }

    /**
     * Re-opens or selects a singleton tab
     *
     * @param id the singleton tab ID
     */
    public DocumentTab selectSingletonTab(String id) {
        DocumentTab tab = singletonTabs.get(id);
        for (int i = 0; i < getTabCount(); ++i) {
            if (tabbedPane.getTabComponentAt(i) == tab.getTabComponent()) {
                tabbedPane.setSelectedComponent(tab.getContent());
                return tab;
            }
        }

        // Was closed; reinstantiate the component
        addTab(tab);
        tabbedPane.setSelectedIndex(getTabCount() - 1);
        return tab;
    }

    private void addTab(DocumentTab tab) {
        tabbedPane.addTab(tab.getTitle(), tab.getIcon(), tab.getContent());
        tabbedPane.setTabComponentAt(getTabCount() - 1, tab.getTabComponent());
        tabs.add(tab);
        tabHistory.add(tab);
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
     * Switches to the provided content
     *
     * @param content the tab content
     */
    public void switchToContent(Component content) {
        tabbedPane.setSelectedComponent(content);
    }

    /**
     * @return the currently selected content
     */
    public Component getCurrentContent() {
        return tabbedPane.getSelectedComponent();
    }

    /**
     * Closes all tabs
     */
    public void closeAllTabs() {
        for (DocumentTab tab : ImmutableList.copyOf(tabs)) {
            forceCloseTab(tab);
        }
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
    public static class DocumentTab implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private String title;
        private Icon icon;
        private Component tabComponent;
        private Component content;
        private CloseMode closeMode;

        private DocumentTab(String title, Icon icon, Component tabComponent, Component content, CloseMode closeMode) {
            this.title = title;
            this.icon = icon;
            this.tabComponent = tabComponent;
            this.content = content;
            this.closeMode = closeMode;
        }

        @JIPipeParameter("title")
        public String getTitle() {
            return title;
        }

        @JIPipeParameter("title")
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

        public CloseMode getCloseMode() {
            return closeMode;
        }

        @Override
        public EventBus getEventBus() {
            return eventBus;
        }
    }
}
