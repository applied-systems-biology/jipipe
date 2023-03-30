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

package org.hkijena.jipipe.ui.parameters;

import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.extensions.settings.GeneralUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;
import org.scijava.Contextual;
import org.scijava.Disposable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * UI around a {@link JIPipeParameterCollection}
 */
public class ParameterPanel extends FormPanel implements Contextual, Disposable {
    /**
     * Flag for {@link ParameterPanel}. Makes that no group headers are created.
     * This includes dynamic parameter group headers that contain buttons for modification.
     */
    public static final int NO_GROUP_HEADERS = 64;
    /**
     * Flag for {@link ParameterPanel}. Makes that group headers without name or description or special functionality (like
     * dynamic parameters) are not shown. Overridden by NO_GROUP_HEADERS.
     */
    public static final int NO_EMPTY_GROUP_HEADERS = 128;

    /**
     * With this flag, the parameter collection is always traversed, even if the provided parameter collection was already traversed
     */
    public static final int FORCE_TRAVERSE = 256;

    /**
     * With this flag, there will be a search bar for parameters.
     * {@link org.hkijena.jipipe.extensions.settings.GeneralUISettings}.isShowParameterSearchBar() will override this setting
     */
    public static final int WITH_SEARCH_BAR = 512;

    /**
     * With this flag, parameters that do not show a label are not put below anymore
     */
    public static final int WITHOUT_LABEL_SEPARATION = 1024;

    /**
     * With this flag, collapsing is disabled
     */
    public static final int WITHOUT_COLLAPSE = 2048;

    /**
     * Flags suitable for standalone dialogs
     */
    public static final int DEFAULT_DIALOG_FLAGS = WITH_SEARCH_BAR | WITH_DOCUMENTATION | WITH_SCROLLING;

    private final JIPipeWorkbench workbench;
    private final boolean noGroupHeaders;
    private final boolean noEmptyGroupHeaders;
    private final boolean forceTraverse;
    private final boolean withSearchBar;
    private final boolean withoutLabelSeparation;
    private final boolean allowCollapse;
    private final SearchTextField searchField = new SearchTextField();
    private final Map<JIPipeParameterCollection, Boolean> collapseStates = new HashMap<>();
    private Context context;
    private JIPipeParameterCollection displayedParameters;
    private JIPipeParameterTree traversed;
    private BiFunction<JIPipeParameterTree, JIPipeParameterAccess, Boolean> customIsParameterVisible;
    private BiFunction<JIPipeParameterTree, JIPipeParameterCollection, Boolean> customIsParameterCollectionVisible;

    /**
     * @param workbench           SciJava context
     * @param displayedParameters Object containing the parameters. If the object is an {@link JIPipeParameterTree} and FORCE_TRAVERSE is not set, it will be used directly. Can be null.
     * @param documentation       Optional documentation
     * @param flags               Flags
     */
    public ParameterPanel(JIPipeWorkbench workbench, JIPipeParameterCollection displayedParameters, MarkdownDocument documentation, int flags) {
        super(documentation, flags);
        this.noGroupHeaders = (flags & NO_GROUP_HEADERS) == NO_GROUP_HEADERS;
        this.noEmptyGroupHeaders = (flags & NO_EMPTY_GROUP_HEADERS) == NO_EMPTY_GROUP_HEADERS;
        this.forceTraverse = (flags & FORCE_TRAVERSE) == FORCE_TRAVERSE;
        this.withSearchBar = (flags & WITH_SEARCH_BAR) == WITH_SEARCH_BAR;
        this.withoutLabelSeparation = (flags & WITHOUT_LABEL_SEPARATION) == WITHOUT_LABEL_SEPARATION;
        this.allowCollapse = (flags & WITHOUT_COLLAPSE) != WITHOUT_COLLAPSE;
        this.workbench = workbench;
        this.context = workbench.getContext();
        this.displayedParameters = displayedParameters;
        initialize();

        if (displayedParameters != null) {
            reloadForm();
            this.displayedParameters.getEventBus().register(this);
        }
    }

    /**
     * Shows a parameter collection inside a modal dialog
     *
     * @param workbench           parent component
     * @param parameterCollection the parameter collection
     * @param flags               flags for the editor
     * @return if the user clicked "OK"
     */
    public static boolean showDialog(JIPipeWorkbench workbench, JIPipeParameterCollection parameterCollection, MarkdownDocument defaultDocumentation, String title, int flags) {
        return showDialog(workbench, workbench.getWindow(), parameterCollection, defaultDocumentation, title, flags);
    }

    /**
     * Shows a parameter collection inside a modal dialog
     *
     * @param workbench           parent component
     * @param parameterCollection the parameter collection
     * @param flags               flags for the editor
     * @return if the user clicked "OK"
     */
    public static boolean showDialog(JIPipeWorkbench workbench, Component parent, JIPipeParameterCollection parameterCollection, MarkdownDocument defaultDocumentation, String title, int flags) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        ParameterPanel parameterPanel = new ParameterPanel(workbench, parameterCollection, defaultDocumentation, flags);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(parameterPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        AtomicBoolean clickedOK = new AtomicBoolean(false);

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            clickedOK.set(false);
            dialog.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("OK", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> {
            clickedOK.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setTitle(title);
        dialog.setModal(true);
        dialog.pack();
        dialog.setSize(new Dimension(800, 600));
        dialog.setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(dialog);
        dialog.setVisible(true);

        return clickedOK.get();
    }

    public static MarkdownDocument generateParameterDocumentation(JIPipeParameterAccess access, JIPipeParameterTree tree) {
        StringBuilder markdownString = new StringBuilder();
        markdownString.append("# ").append(access.getName()).append("\n\n");
        markdownString.append("<table>");

        if (access.isImportant()) {
            markdownString.append("<tr><td><img src=\"").append(ResourceUtils.getPluginResource("icons/emblems/important.png")).append("\" /></td>");
            markdownString.append("<td><strong>Important parameter</strong>: The developer marked this parameter as especially important</td></tr>\n\n");
        }

        markdownString.append("<tr><td><img src=\"").append(ResourceUtils.getPluginResource("icons/actions/dialog-xml-editor.png")).append("\" /></td>");
        markdownString.append("<td><strong>Unique identifier</strong>: <code>");
        markdownString.append(HtmlEscapers.htmlEscaper().escape(tree != null ? tree.getUniqueKey(access) : access.getKey())).append("</code></td></tr>\n\n");

        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(access.getFieldClass());
        if (info != null) {
            markdownString.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/data-types/data-type.png")).append("\" /></td>");
            markdownString.append("<td><strong>").append(HtmlEscapers.htmlEscaper().escape(info.getName())).append("</strong>: ");
            markdownString.append(HtmlEscapers.htmlEscaper().escape(info.getDescription())).append("</td></tr>");
        }
        markdownString.append("</table>\n\n");

        if (access.getDescription() != null && !access.getDescription().isEmpty()) {
            markdownString.append(access.getDescription());
        } else {
            markdownString.append("No description provided.");
        }
        return new MarkdownDocument(markdownString.toString());
    }

    @Override
    public void dispose() {
        clear();
        if(displayedParameters != null) {
            UIUtils.unregisterEventBus(this.displayedParameters.getEventBus(), this);
        }
        traversed = null;
        displayedParameters = null;
    }

    public BiFunction<JIPipeParameterTree, JIPipeParameterAccess, Boolean> getCustomIsParameterVisible() {
        return customIsParameterVisible;
    }

    public void setCustomIsParameterVisible(BiFunction<JIPipeParameterTree, JIPipeParameterAccess, Boolean> customIsParameterVisible) {
        this.customIsParameterVisible = customIsParameterVisible;
        refreshForm();
    }

    public BiFunction<JIPipeParameterTree, JIPipeParameterCollection, Boolean> getCustomIsParameterCollectionVisible() {
        return customIsParameterCollectionVisible;
    }

    public void setCustomIsParameterCollectionVisible(BiFunction<JIPipeParameterTree, JIPipeParameterCollection, Boolean> customIsParameterCollectionVisible) {
        this.customIsParameterCollectionVisible = customIsParameterCollectionVisible;
        refreshForm();
    }

    private void initialize() {
        if (withSearchBar) {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);

            searchField.addActionListener(e -> refreshForm());
            toolBar.add(searchField);

            add(toolBar, BorderLayout.NORTH);
        }
    }

    /**
     * Reloads the form. This re-traverses the parameters and recreates the UI elements
     */
    public void reloadForm() {
        if (displayedParameters != null) {
            if (forceTraverse || !(getDisplayedParameters() instanceof JIPipeParameterTree))
                traversed = new JIPipeParameterTree(getDisplayedParameters());
            else
                traversed = (JIPipeParameterTree) getDisplayedParameters();
        }
        refreshForm();
    }

    /**
     * Recreates the UI elements. Does not re-traverse the parameters, meaning that the parameter structure is not updated
     */
    public void refreshForm() {
        int scrollValueBackup = getScrollPane() != null ? getScrollPane().getVerticalScrollBar().getValue() : 0;

        clear();

        // Create list of filtered-out nodes
        Set<JIPipeParameterCollection> hiddenCollections = new HashSet<>();
        Set<JIPipeParameterAccess> hiddenAccesses = new HashSet<>();

        if(traversed == null) {
            return;
        }

        JIPipeParameterCollection rootCollection = traversed.getRoot().getCollection();

        for (JIPipeParameterCollection source : traversed.getRegisteredSources()) {

            JIPipeParameterTree.Node sourceNode = traversed.getSourceNode(source);

            // Hidden parameter groups
            if (rootCollection != null) {
                if (customIsParameterCollectionVisible == null) {
                    if (rootCollection != source && !rootCollection.isParameterUIVisible(traversed, source))
                        hiddenCollections.add(source);
                } else {
                    if (rootCollection != source && !customIsParameterCollectionVisible.apply(traversed, source))
                        hiddenCollections.add(source);
                }
            } else if (sourceNode.getParent() != null && sourceNode.getParent().getCollection() != null) {
                if (!sourceNode.getParent().getCollection().isParameterUIVisible(traversed, source))
                    hiddenCollections.add(source);
            }

            // Visibility check
            int parameterCount = sourceNode.getParameters().size();
            for (JIPipeParameterAccess parameterAccess : sourceNode.getParameters().values()) {
                boolean visible;
                if (customIsParameterVisible == null)
                    visible = (rootCollection != null ? rootCollection.isParameterUIVisible(traversed, parameterAccess) : !parameterAccess.isHidden());
                else
                    visible = (rootCollection != null ? customIsParameterVisible.apply(traversed, parameterAccess) : !parameterAccess.isHidden());
                if (!visible) {
                    hiddenAccesses.add(parameterAccess);
                    --parameterCount;
                }
            }

            if (parameterCount <= 0 && !(source instanceof JIPipeDynamicParameterCollection)) {
                hiddenCollections.add(source);
            }
        }

        // Generate form
        Map<JIPipeParameterCollection, List<JIPipeParameterAccess>> groupedBySource = traversed.getGroupedBySource();
        if (groupedBySource.containsKey(this.displayedParameters) && !hiddenCollections.contains(displayedParameters)) {
            addToForm(traversed, this.displayedParameters, groupedBySource.get(this.displayedParameters), hiddenAccesses);
        }

        for (JIPipeParameterCollection collection : groupedBySource.keySet().stream().sorted(
                        Comparator.comparing(traversed::getSourceCollapsed).thenComparing(traversed::getSourceUIOrder).thenComparing(
                                Comparator.nullsFirst(Comparator.comparing(traversed::getSourceDocumentationName))))
                .collect(Collectors.toList())) {
            if (collection == this.displayedParameters)
                continue;
            if (hiddenCollections.contains(collection))
                continue;
            addToForm(traversed, collection, groupedBySource.get(collection), hiddenAccesses);
        }
        addVerticalGlue();

        if (getScrollPane() != null) {
            SwingUtilities.invokeLater(() -> getScrollPane().getVerticalScrollBar().setValue(scrollValueBackup));
        }
    }

    private void addToForm(JIPipeParameterTree tree, JIPipeParameterCollection parameterCollection, List<JIPipeParameterAccess> parameterAccesses, Set<JIPipeParameterAccess> hiddenAccesses) {
        boolean isModifiable = parameterCollection instanceof JIPipeDynamicParameterCollection && ((JIPipeDynamicParameterCollection) parameterCollection).isAllowUserModification();

        if (!isModifiable && parameterAccesses.isEmpty())
            return;

        JIPipeParameterTree.Node node = tree.getSourceNode(parameterCollection);

        JCheckBox collapseButton = new JCheckBox();
        collapseButton.setToolTipText("Collapse/Show this category");
        collapseButton.setOpaque(false);
        collapseButton.setIcon(UIUtils.getIconFromResources("actions/arrow-right.png"));
        collapseButton.setSelectedIcon(UIUtils.getIconFromResources("actions/arrow-down.png"));
        collapseButton.setSelected(!node.isCollapsed());
        if (!collapseButton.isSelected()) {
            if (!GeneralUISettings.getInstance().isAllowDefaultCollapsedParameters())
                collapseButton.setSelected(true);
        }

        if (!noGroupHeaders) {
            JIPipeDocumentation documentation = tree.getSourceDocumentation(parameterCollection);
            boolean documentationIsEmpty = documentation == null || (StringUtils.isNullOrEmpty(documentation.name())
                    && StringUtils.isNullOrEmpty(DocumentationUtils.getDocumentationDescription(documentation)));
            boolean groupHeaderIsEmpty = documentationIsEmpty && !isModifiable && node.getActions().isEmpty();

            if (!noEmptyGroupHeaders || !groupHeaderIsEmpty) {
                Component[] leftComponents;
                if (allowCollapse)
                    leftComponents = new Component[]{collapseButton};
                else
                    leftComponents = new Component[0];
                Icon groupIcon;
                if (UIUtils.DARK_THEME && !StringUtils.isNullOrEmpty(node.getDarkIconURL())) {
                    groupIcon = new ImageIcon(node.getResourceClass().getResource(node.getDarkIconURL()));
                } else {
                    if (!StringUtils.isNullOrEmpty(node.getIconURL())) {
                        groupIcon = new ImageIcon(node.getResourceClass().getResource(node.getIconURL()));
                    } else {
                        groupIcon = UIUtils.getIconFromResources("actions/configure.png");
                    }
                }
                GroupHeaderPanel groupHeaderPanel = addGroupHeader(tree.getSourceDocumentationName(parameterCollection), groupIcon);
                for (Component leftComponent : leftComponents) {
                    groupHeaderPanel.addColumn(leftComponent);
                }

                if (documentation != null && !StringUtils.isNullOrEmpty(DocumentationUtils.getDocumentationDescription(documentation))) {
                    groupHeaderPanel.getDescriptionArea().setVisible(true);
                    groupHeaderPanel.getDescriptionArea().setText(DocumentationUtils.getDocumentationDescription(documentation));
                }

                for (JIPipeParameterCollectionContextAction action : node.getActions()) {
                    Icon icon = action.getIconURL() != null ? new ImageIcon(action.getIconURL()) : null;
                    JButton actionButton = new JButton(action.getDocumentation().name(), icon);
                    actionButton.setToolTipText(DocumentationUtils.getDocumentationDescription(action.getDocumentation()));
                    actionButton.addActionListener(e -> action.accept(workbench));
                    UIUtils.setStandardButtonBorder(actionButton);
                    groupHeaderPanel.addColumn(actionButton);
                }

                if (isModifiable) {
                    JButton addButton = new JButton("Edit", UIUtils.getIconFromResources("actions/edit.png"));
                    addButton.addActionListener(e -> {
                        DynamicParameterEditorDialog dialog = new DynamicParameterEditorDialog(SwingUtilities.getWindowAncestor(this), workbench, (JIPipeDynamicParameterCollection) parameterCollection);
                        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
                        dialog.setModal(true);
                        dialog.setVisible(true);
                    });
                    addButton.setToolTipText("Allows to add/remove parameters in this group");
                    UIUtils.setStandardButtonBorder(addButton);
                    groupHeaderPanel.addColumn(addButton);
                }
            }
        }

        List<JIPipeParameterEditorUI> uiList = new ArrayList<>();
        List<Component> uiComponents = new ArrayList<>();

        for (JIPipeParameterAccess parameterAccess : parameterAccesses) {
            if (hiddenAccesses.contains(parameterAccess))
                continue;
            if (withSearchBar && !searchField.test(parameterAccess.getName() + " " + parameterAccess.getDescription()))
                continue;
            if (allowCollapse && !StringUtils.isNullOrEmpty(searchField.getText())) {
                collapseButton.setSelected(true);
            }

            JIPipeParameterEditorUI ui = JIPipe.getParameterTypes().createEditorFor(workbench, parameterAccess);
            uiList.add(ui);
            uiComponents.add(ui);
        }
        Comparator<JIPipeParameterEditorUI> comparator;
        if (withoutLabelSeparation) {
            comparator = Comparator.comparing((JIPipeParameterEditorUI u) -> !u.getParameterAccess().isPinned())
                    .thenComparing((JIPipeParameterEditorUI u) -> u.getParameterAccess().getUIOrder())
                    .thenComparing(JIPipeParameterEditorUI::getUIControlStyleType)
                    .thenComparing(u -> u.getParameterAccess().getName());
        } else {
            comparator = Comparator.comparing((JIPipeParameterEditorUI u) -> !u.getParameterAccess().isPinned())
                    .thenComparing((JIPipeParameterEditorUI u) -> !u.isUILabelEnabled())
                    .thenComparing(u -> u.getParameterAccess().getUIOrder())
                    .thenComparing(JIPipeParameterEditorUI::getUIControlStyleType)
                    .thenComparing(u -> u.getParameterAccess().getName());
        }

        boolean pinModeStarted = false;

        for (JIPipeParameterEditorUI ui : uiList.stream().sorted(comparator).collect(Collectors.toList())) {
            JIPipeParameterAccess parameterAccess = ui.getParameterAccess();

            if (parameterAccess.isPinned()) {
                pinModeStarted = true;
            } else if (pinModeStarted) {
                // Add some spacing
                JPanel strut = new JPanel(new BorderLayout());
                strut.add(new JSeparator(SwingConstants.HORIZONTAL));
                strut.setBorder(BorderFactory.createEmptyBorder(12, 32, 12, 0));
                addWideToForm(strut);
                uiComponents.add(strut);
                pinModeStarted = false;
            }

            JPanel labelPanel = new JPanel(new BorderLayout());
            MarkdownDocument documentation = generateParameterDocumentation(parameterAccess, tree);

            // Label panel
            if (ui.isUILabelEnabled() || (parameterAccess.isImportant() && ui.isUIImportantLabelEnabled())) {
                JLabel label = new JLabel();
                if (ui.isUILabelEnabled())
                    label.setText(parameterAccess.getName());
                if (parameterAccess.isImportant())
                    label.setIcon(UIUtils.getIconFromResources("emblems/important.png"));
                if (!isWithDocumentation())
                    label.setToolTipText("<html>" + documentation.getRenderedHTML() + "</html>");
                labelPanel.add(label, BorderLayout.CENTER);
            }
            if (!isWithDocumentation()) {
                ui.setToolTipText("<html>" + documentation.getRenderedHTML() + "</html>");
            }

            // Editor if modifiable
//            if (isModifiable) {
//                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
//                UIUtils.makeBorderlessWithoutMargin(removeButton);
//                removeButton.addActionListener(e -> removeDynamicParameter(parameterAccess.getKey(), (JIPipeDynamicParameterCollection) parameterCollection));
//                labelPanel.add(removeButton, BorderLayout.WEST);
//            }

            // Add to form
            JComponent wrappedUI = displayedParameters.installUIOverrideParameterEditor(this, ui);
            if (ui.isUILabelEnabled() || parameterCollection instanceof JIPipeDynamicParameterCollection) {
                addToForm(wrappedUI, labelPanel, documentation);
                uiComponents.add(labelPanel);
            } else {
                if (!parameterAccess.isImportant() || !ui.isUIImportantLabelEnabled()) {
                    addWideToForm(wrappedUI, documentation);
                } else {
                    JPanel wrapperPanel = new JPanel(new BorderLayout());
                    wrapperPanel.add(wrappedUI, BorderLayout.CENTER);
                    wrapperPanel.add(labelPanel, BorderLayout.WEST);
                    addWideToForm(wrapperPanel, documentation);
                    uiComponents.add(labelPanel);
                }
            }

            // Get the entry
            FormPanelEntry entry = getEntries().get(getNumRows() - 1);
            if (entry.getProperties() != null) {
                uiComponents.add(entry.getProperties());
            }

        }

        if (allowCollapse) {

            // Restore the collapse
            if (collapseStates.containsKey(parameterCollection)) {
                collapseButton.setSelected(collapseStates.get(parameterCollection));
            }

            showCollapse(uiComponents, collapseButton.isSelected());
            collapseButton.addActionListener(e -> {
                showCollapse(uiComponents, collapseButton.isSelected());
                collapseStates.put(parameterCollection, collapseButton.isSelected());
            });

            collapseStates.put(parameterCollection, collapseButton.isSelected());
        }
    }

    @Override
    protected Component createEntryPropertiesComponent(Component component, Component description, int row, MarkdownDocument documentation) {
        JPanel propertyPanel = new JPanel(new BorderLayout());

        // Help
        if (documentation != null || component instanceof JIPipeParameterEditorUI) {
            JButton helpButton = new JButton(UIUtils.getIconFromResources("actions/help-muted.png"));
            helpButton.setBorder(null);
            helpButton.addActionListener(e -> {
                showDocumentation(documentation);
            });
            installComponentHighlighter(helpButton, Sets.newHashSet(component, description));
            propertyPanel.add(helpButton, BorderLayout.WEST);
        }

        // Options menu
        if (component instanceof JIPipeParameterEditorUI) {
            JIPipeParameterEditorUI editorUI = (JIPipeParameterEditorUI) component;
            JButton optionsButton = new JButton(UIUtils.getIconFromResources("actions/draw-triangle4-muted.png"));
            optionsButton.setBorder(null);

            JPopupMenu optionsMenu = UIUtils.addPopupMenuToComponent(optionsButton);

            JMenuItem copyItem = new JMenuItem("Copy", UIUtils.getIconFromResources("actions/edit-copy.png"));
            copyItem.addActionListener(e -> {
                Object parameter = editorUI.getParameter(Object.class);
                UIUtils.copyToClipboard(JsonUtils.toJsonString(parameter));
                workbench.sendStatusBarText("Copied parameter '" + editorUI.getParameterAccess().getName() + "' to the clipboard.");
            });
            optionsMenu.add(copyItem);

            JMenuItem pasteItem = new JMenuItem("Paste", UIUtils.getIconFromResources("actions/edit-paste.png"));
            pasteItem.addActionListener(e -> {
                try {
                    Object o = JsonUtils.readFromString(UIUtils.getStringFromClipboard(), editorUI.getParameterAccess().getFieldClass());
                    editorUI.getParameterAccess().set(o);
                    workbench.sendStatusBarText("Pasted value into parameter '" + editorUI.getParameterAccess().getName() + "'.");
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(workbench.getWindow(), "Unable to paste data into parameter '" + editorUI.getParameterAccess().getName() + "'!", "Paste parameter value", JOptionPane.ERROR_MESSAGE);
                }
            });
            optionsMenu.add(pasteItem);

            // Set to default function
            if (displayedParameters instanceof JIPipeGraphNode) {
                JMenuItem restoreDefaultItem = new JMenuItem("Restore to default value", UIUtils.getIconFromResources("actions/edit-undo.png"));
                restoreDefaultItem.addActionListener(e -> {
                    JIPipeParameterCollection defaultCollection = ((JIPipeGraphNode) displayedParameters).getInfo().newInstance();
                    JIPipeParameterTree defaultTree = new JIPipeParameterTree(defaultCollection);
                    JIPipeParameterAccess otherAccess = defaultTree.getParameters().getOrDefault(editorUI.getParameterAccess().getKey(), null);
                    editorUI.getParameterAccess().set(otherAccess.get(Object.class));
                });
                optionsMenu.addSeparator();
                optionsMenu.add(restoreDefaultItem);
            } else if (ReflectionUtils.hasDefaultConstructor(displayedParameters.getClass())) {
                JMenuItem restoreDefaultItem = new JMenuItem("Restore to default value", UIUtils.getIconFromResources("actions/edit-undo.png"));
                restoreDefaultItem.addActionListener(e -> {
                    JIPipeParameterCollection defaultCollection = (JIPipeParameterCollection) ReflectionUtils.newInstance(displayedParameters.getClass());
                    JIPipeParameterTree defaultTree = new JIPipeParameterTree(defaultCollection);
                    JIPipeParameterAccess otherAccess = defaultTree.getParameters().getOrDefault(editorUI.getParameterAccess().getKey(), null);
                    editorUI.getParameterAccess().set(otherAccess.get(Object.class));
                });
                optionsMenu.addSeparator();
                optionsMenu.add(restoreDefaultItem);
            }

            // Additional options
            displayedParameters.installUIParameterOptions(this, (JIPipeParameterEditorUI) component, optionsMenu);

            propertyPanel.add(optionsButton, BorderLayout.EAST);
            installComponentHighlighter(optionsButton, Sets.newHashSet(component, description));
        }


        return propertyPanel;
    }

    private void showCollapse(List<Component> uiComponents, boolean selected) {
        for (Component component : uiComponents) {
            component.setVisible(selected);
        }
    }

    /**
     * Triggered when the parameter structure was changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(JIPipeParameterCollection.ParameterStructureChangedEvent event) {
        reloadForm();
    }

    /**
     * Triggered when the parameter UI should be updated
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterUIChanged(JIPipeParameterCollection.ParameterUIChangedEvent event) {
        refreshForm();
    }

    /**
     * @return The parameterized object
     */
    public JIPipeParameterCollection getDisplayedParameters() {
        return displayedParameters;
    }

    public void setDisplayedParameters(JIPipeParameterCollection displayedParameters) {
        if (this.displayedParameters != null) {
            this.displayedParameters.getEventBus().unregister(this);
        }
        this.displayedParameters = displayedParameters;
        this.displayedParameters.getEventBus().register(this);
        reloadForm();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void setContext(Context context) {
        this.context = context;
        this.context.inject(this);
    }

    @Override
    public Context context() {
        return context;
    }
}