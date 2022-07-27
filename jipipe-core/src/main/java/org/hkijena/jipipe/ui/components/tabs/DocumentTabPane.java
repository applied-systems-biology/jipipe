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

package org.hkijena.jipipe.ui.components.tabs;

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
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.plaf.basic.BasicStatusBarUI;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link JTabbedPane} with larger tabs, ability to close tabs, singleton tabs that are hidden instead of being closed
 */
public class DocumentTabPane extends JPanel {

    /**
     * List of open tabs
     */
    private final Set<DocumentTab> tabs = new HashSet<>();
    /**
     * Last tabs have priority over lower index tabs
     */
    private final List<DocumentTab> tabHistory = new ArrayList<>();
    /**
     * Contains tabs that can be closed, but opened again
     */
    private final BiMap<String, SingletonTab> singletonTabs = HashBiMap.create();
    private final BiMap<String, DocumentTab> singletonTabInstances = HashBiMap.create();

    private DnDTabbedPane tabbedPane;
    private boolean enableTabContextMenu = true;

    private Border tabPanelBorder = BorderFactory.createEmptyBorder(4, 0, 4, 0);

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
                        closeTab(getTabContainingTabComponent(component));
                    }
                } else if (enableTabContextMenu && e.getButton() == MouseEvent.BUTTON3) {
                    int tabIndex = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    if (tabIndex >= 0 && tabIndex < tabbedPane.getTabCount()) {
                        Component component = tabbedPane.getTabComponentAt(tabIndex);
                        DocumentTab tab = getTabContainingTabComponent(component);
                        System.out.println(tab.getPopupMenu());
                        if (tab.getPopupMenu() != null) {
                            tab.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
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

    public BiMap<String, SingletonTab> getSingletonTabs() {
        return ImmutableBiMap.copyOf(singletonTabs);
    }

    public BiMap<String, DocumentTab> getSingletonTabInstances() {
        return ImmutableBiMap.copyOf(singletonTabInstances);
    }

    /**
     * Updates the order of tabs we go though when tabs are closed
     */
    private void updateTabHistory() {
        DocumentTab tab = getTabContainingContent(tabbedPane.getSelectedComponent());
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
    public DocumentTab getTabContainingContent(Component content) {
        return tabs.stream().filter(t -> t.getContent() == content).findFirst().orElse(null);
    }

    /**
     * Returns the tab that contains the specified tab component
     *
     * @param content the content
     * @return the tab containing the content. Null if not found
     */
    public DocumentTab getTabContainingTabComponent(Component content) {
        return tabs.stream().filter(t -> t.getTabComponent() == content).findFirst().orElse(null);
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

    public Border getTabPanelBorder() {
        return tabPanelBorder;
    }

    public void setTabPanelBorder(Border tabPanelBorder) {
        this.tabPanelBorder = tabPanelBorder;
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
        tabPanel.setBorder(tabPanelBorder);

        tabPanel.setLayout(new BoxLayout(tabPanel, BoxLayout.LINE_AXIS));
        JLabel titleLabel = new JLabel(title, icon, JLabel.LEFT);
        tabPanel.add(titleLabel);
        tabPanel.add(Box.createHorizontalGlue());

        JPopupMenu popupMenu = new JPopupMenu();
//        tabPanel.setComponentPopupMenu(popupMenu);

        DocumentTab tab = new DocumentTab(title, icon, tabPanel, component, closeMode, popupMenu);

        tab.getEventBus().register(new Object() {
            @Subscribe
            public void onPropertyChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
                titleLabel.setText(tab.getTitle());
            }
        });

        JMenuItem closeItem = new JMenuItem("Close", UIUtils.getIconFromResources("actions/tab-close.png"));
        closeItem.addActionListener(e -> closeTab(tab));
        popupMenu.add(closeItem);

        JMenuItem closeOthersItem = new JMenuItem("Close other tabs", UIUtils.getIconFromResources("actions/tab-close-other.png"));
        closeOthersItem.addActionListener(e -> closeAllTabsExcept(tab, false));
        popupMenu.add(closeOthersItem);

        JMenuItem closeAllItem = new JMenuItem("Close all tabs", UIUtils.getIconFromResources("actions/tab-close-other.png"));
        closeAllItem.addActionListener(e -> closeAllTabs(false));
        popupMenu.add(closeAllItem);

        JMenuItem closeLeftItem = new JMenuItem("Close tabs to the left", UIUtils.getIconFromResources("actions/view-left-close.png"));
        closeLeftItem.addActionListener(e -> closeAllTabsToTheLeft(tab, false));
        popupMenu.add(closeLeftItem);

        JMenuItem closeRightItem = new JMenuItem("Close tabs to the right", UIUtils.getIconFromResources("actions/view-right-close.png"));
        closeRightItem.addActionListener(e -> closeAllTabsToTheRight(tab, false));
        popupMenu.add(closeRightItem);

        popupMenu.addSeparator();

        JMenuItem detachItem = new JMenuItem("Detach tab", UIUtils.getIconFromResources("actions/tab-detach.png"));
        detachItem.addActionListener(e -> detachTab(this, tab, true));
        popupMenu.add(detachItem);

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
            closeItem.setEnabled(true);
        } else {
            closeItem.setEnabled(false);
        }
        if (allowRename) {
            JMenuItem renameButton = new JMenuItem("Rename", UIUtils.getIconFromResources("actions/tag.png"));
            UIUtils.makeBorderlessWithoutMargin(renameButton);
            renameButton.addActionListener(e -> {
                String newName = JOptionPane.showInputDialog(this, "Rename tab '" + titleLabel.getText() + "' to ...", titleLabel.getText());
                if (newName != null && !newName.isEmpty()) {
                    tab.setTitle(newName);
                    titleLabel.setText(newName);
                }
            });
            popupMenu.add(renameButton);
        }


        addTab(tab);
        return tab;
    }

    /**
     * Closes all tabs to the right of the provided tab.
     *
     * @param tab   the tab
     * @param force if non-closable tabs are affected
     */
    private void closeAllTabsToTheRight(DocumentTab tab, boolean force) {
        int index = -1;
        for (int i = 0; i < getTabCount(); i++) {
            if (getTabbedPane().getTabComponentAt(i) == tab.getTabComponent()) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            Set<Component> toClose = new HashSet<>();
            for (int i = index + 1; i < getTabCount(); i++) {
                toClose.add(getTabbedPane().getTabComponentAt(i));
            }
            for (Component component : toClose) {
                closeTab(getTabContainingTabComponent(component), force);
            }
        }
    }

    /**
     * Closes all tabs to the left of the provided tab.
     *
     * @param tab   the tab
     * @param force if non-closable tabs are affected
     */
    private void closeAllTabsToTheLeft(DocumentTab tab, boolean force) {
        int index = -1;
        for (int i = 0; i < getTabCount(); i++) {
            if (getTabbedPane().getTabComponentAt(i) == tab.getTabComponent()) {
                index = i;
                break;
            }
        }
        if (index > 0) {
            Set<Component> toClose = new HashSet<>();
            for (int i = 0; i < index; i++) {
                toClose.add(getTabbedPane().getTabComponentAt(i));
            }
            for (Component component : toClose) {
                closeTab(getTabContainingTabComponent(component), force);
            }
        }
    }

    /**
     * Closes all tabs except the provided one.
     *
     * @param exception the tab that should be excluded
     * @param force     if non-closable tabs are affected
     */
    public void closeAllTabsExcept(DocumentTab exception, boolean force) {
        for (DocumentTab tab : ImmutableList.copyOf(tabs)) {
            if (tab == exception)
                continue;
            if (force)
                forceCloseTab(tab);
            else
                closeTab(tab);
        }
    }

    /**
     * Detaches a tab
     *
     * @param tab                the tab
     * @param reattachAfterClose if the window is closed, re-attach the tab (always true for non-closable tabs)
     */
    public JFrame detachTab(JComponent parent, DocumentTab tab, boolean reattachAfterClose) {
        forceCloseTab(tab);
        JFrame frame = new JFrame(UIUtils.getAWTWindowTitle(SwingUtilities.getWindowAncestor(this)) + ": Tab <" + tab.getTitle() + ">");
        frame.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        frame.setSize(getSize());
        frame.setLocationRelativeTo(parent);

        if (reattachAfterClose || tab.getCloseMode() == CloseMode.withoutCloseButton) {
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    addTab(tab);
                    switchToLastTab();
                }
            });
        }

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.add(tab.getContent(), BorderLayout.CENTER);

        JXStatusBar statusBar = new JXStatusBar();
        statusBar.putClientProperty(BasicStatusBarUI.AUTO_ADD_SEPARATOR, false);

        statusBar.add(new JLabel("Detached tab from '" + UIUtils.getAWTWindowTitle(SwingUtilities.getWindowAncestor(this)) + "'",
                UIUtils.getIconFromResources("actions/window-duplicate.png"), SwingConstants.LEFT));
        statusBar.add(Box.createHorizontalGlue(), new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL));
        JButton reAttachButton = new JButton("Reattach", UIUtils.getIconFromResources("actions/tab_breakoff.png"));
        reAttachButton.addActionListener(e -> {
            frame.setVisible(false);
            frame.setContentPane(new JPanel());
            frame.dispose();
            addTab(tab);
            switchToLastTab();
        });
        UIUtils.makeFlatH25(reAttachButton);
        statusBar.add(reAttachButton);

        contentPane.add(statusBar, BorderLayout.SOUTH);

        frame.setContentPane(contentPane);
        frame.setVisible(true);
        frame.revalidate();
        frame.repaint();
        return frame;
    }

    /**
     * Closes a tab.
     *
     * @param tab   the tab.
     * @param force if non-closable tabs are affected
     */
    public void closeTab(DocumentTab tab, boolean force) {
        if (force)
            forceCloseTab(tab);
        else
            closeTab(tab);
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
        singletonTabInstances.inverse().remove(tab);

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
     * @param id                Unique tab identifier
     * @param title             Tab title
     * @param icon              Tab icon
     * @param componentSupplier Tab content
     * @param singletonTabMode  If the tab is hidden by default
     */
    public SingletonTab registerSingletonTab(String id, String title, Icon icon, Supplier<Component> componentSupplier, SingletonTabMode singletonTabMode) {
        return registerSingletonTab(id, title, icon, componentSupplier, CloseMode.withSilentCloseButton, singletonTabMode);
    }

    /**
     * Returns the currently selected singleton tab ID.
     * Null otherwise.
     *
     * @return the singleton tab ID. Null otherwise.
     */
    public String getCurrentlySelectedSingletonTabId() {
        for (Map.Entry<String, DocumentTab> entry : singletonTabInstances.entrySet()) {
            if (getCurrentContent() == entry.getValue().getContent()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Adds a tab that can be silently closed and brought up again
     *
     * @param id                Unique tab identifier
     * @param title             Tab title
     * @param icon              Tab icon
     * @param componentSupplier Tab content
     * @param singletonTabMode  How the singleton tab is displayed
     */
    public SingletonTab registerSingletonTab(String id, String title, Icon icon, Supplier<Component> componentSupplier, CloseMode closeMode, SingletonTabMode singletonTabMode) {
        if (singletonTabs.containsKey(id))
            throw new IllegalArgumentException("Already contains singleton tab with ID " + id);
        SingletonTab singletonTab = new SingletonTab(icon, closeMode, componentSupplier, title);
        singletonTabs.put(id, singletonTab);
        switch (singletonTabMode) {
            case Present:
                getOrCreateSingletonTab(id);
                break;
            case Selected:
                selectSingletonTab(id);
                break;
        }
        return singletonTab;
    }

    /**
     * Re-opens or selects a singleton tab
     *
     * @param id the singleton tab ID
     * @return the tab. null if there is no such singleton tab
     */
    public DocumentTab selectSingletonTab(String id) {
        DocumentTab tab = getOrCreateSingletonTab(id);
        if (tab != null) {
            switchToTab(tab);
        }
        return tab;
    }

    private DocumentTab getOrCreateSingletonTab(String id) {
        if (singletonTabInstances.containsKey(id)) {
            return singletonTabInstances.get(id);
        } else {
            SingletonTab tab = singletonTabs.getOrDefault(id, null);
            if (tab == null) {
                return null;
            }
            // Create a new instance
            Component component = tab.contentSupplier.get();
            DocumentTab documentTab = addTab(tab.title, tab.icon, component, tab.closeMode, false);
            singletonTabInstances.put(id, documentTab);
            return documentTab;
        }
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
     * Switches to the provided tab
     *
     * @param tab the tab
     */
    public void switchToTab(DocumentTab tab) {
        switchToContent(tab.getContent());
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
     *
     * @param force if non-closable tabs will be closed
     */
    public void closeAllTabs(boolean force) {
        for (DocumentTab tab : ImmutableList.copyOf(tabs)) {
            if (force)
                forceCloseTab(tab);
            else
                closeTab(tab);
        }
    }

    public boolean isEnableTabContextMenu() {
        return enableTabContextMenu;
    }

    public void setEnableTabContextMenu(boolean enableTabContextMenu) {
        this.enableTabContextMenu = enableTabContextMenu;
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

    public enum SingletonTabMode {
        Hidden,
        Present,
        Selected
    }

    /**
     * Encapsulates a singleton tab
     */
    public static class SingletonTab {
        private final Icon icon;
        private final CloseMode closeMode;
        private final Supplier<Component> contentSupplier;
        private String title;

        public SingletonTab(Icon icon, CloseMode closeMode, Supplier<Component> contentSupplier, String title) {
            this.icon = icon;
            this.closeMode = closeMode;
            this.contentSupplier = contentSupplier;
            this.title = title;
        }

        public Icon getIcon() {
            return icon;
        }

        public CloseMode getCloseMode() {
            return closeMode;
        }

        public Supplier<Component> getContentSupplier() {
            return contentSupplier;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * Encapsulates a tab
     */
    public static class DocumentTab implements JIPipeParameterCollection {
        private final EventBus eventBus = new EventBus();
        private final Icon icon;
        private final Component tabComponent;
        private final Component content;
        private final CloseMode closeMode;
        private final JPopupMenu popupMenu;
        private String title;

        private DocumentTab(String title, Icon icon, Component tabComponent, Component content, CloseMode closeMode, JPopupMenu popupMenu) {
            this.title = title;
            this.icon = icon;
            this.tabComponent = tabComponent;
            this.content = content;
            this.closeMode = closeMode;
            this.popupMenu = popupMenu;
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

        public JPopupMenu getPopupMenu() {
            return popupMenu;
        }
    }
}
