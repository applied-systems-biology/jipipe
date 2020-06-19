package org.hkijena.acaq5.ui.parameters;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.SearchTextField;
import org.hkijena.acaq5.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UI around a {@link ACAQParameterCollection}
 */
public class ParameterPanel extends FormPanel implements Contextual {
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
     * {@link org.hkijena.acaq5.extensions.settings.GeneralUISettings}.isShowParameterSearchBar() will override this setting
     */
    public static final int WITH_SEARCH_BAR = 512;

    /**
     * With this flag, parameters that do not show a label are not put below anymore
     */
    public static final int WITHOUT_LABEL_SEPARATION = 1024;

    private ACAQWorkbench workbench;
    private Context context;
    private ACAQParameterCollection displayedParameters;
    private boolean noGroupHeaders;
    private boolean noEmptyGroupHeaders;
    private boolean forceTraverse;
    private boolean withSearchBar;
    private boolean withoutLabelSeparation;
    private ACAQParameterTree traversed;
    private SearchTextField searchField = new SearchTextField();

    /**
     * @param workbench           SciJava context
     * @param displayedParameters Object containing the parameters. If the object is an {@link ACAQParameterTree} and FORCE_TRAVERSE is not set, it will be used directly. Can be null.
     * @param documentation       Optional documentation
     * @param flags               Flags
     */
    public ParameterPanel(ACAQWorkbench workbench, ACAQParameterCollection displayedParameters, MarkdownDocument documentation, int flags) {
        super(documentation, flags);
        this.noGroupHeaders = (flags & NO_GROUP_HEADERS) == NO_GROUP_HEADERS;
        this.noEmptyGroupHeaders = (flags & NO_EMPTY_GROUP_HEADERS) == NO_EMPTY_GROUP_HEADERS;
        this.forceTraverse = (flags & FORCE_TRAVERSE) == FORCE_TRAVERSE;
        this.withSearchBar = (flags & WITH_SEARCH_BAR) == WITH_SEARCH_BAR;
        this.withoutLabelSeparation = (flags & WITHOUT_LABEL_SEPARATION) == WITHOUT_LABEL_SEPARATION;
        this.workbench = workbench;
        this.context = workbench.getContext();
        this.displayedParameters = displayedParameters;
        initialize();

        if (displayedParameters != null) {
            reloadForm();
            this.displayedParameters.getEventBus().register(this);
        }
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
        if(displayedParameters != null) {
            if (forceTraverse || !(getDisplayedParameters() instanceof ACAQParameterTree))
                traversed = new ACAQParameterTree(getDisplayedParameters());
            else
                traversed = (ACAQParameterTree) getDisplayedParameters();
        }
        refreshForm();
    }

    /**
     * Recreates the UI elements. Does not re-traverse the parameters, meaning that the parameter structure is not updated
     */
    public void refreshForm() {
        clear();
        Map<ACAQParameterCollection, List<ACAQParameterAccess>> groupedBySource = traversed.getGroupedBySource();
        if (groupedBySource.containsKey(this.displayedParameters)) {
            addToForm(traversed, this.displayedParameters, groupedBySource.get(this.displayedParameters));
        }

        for (ACAQParameterCollection collection : groupedBySource.keySet().stream().sorted(
                Comparator.comparing(traversed::getSourceUIOrder).thenComparing(
                        Comparator.nullsFirst(Comparator.comparing(traversed::getSourceDocumentationName))))
                .collect(Collectors.toList())) {
            if (collection == this.displayedParameters)
                continue;
            addToForm(traversed, collection, groupedBySource.get(collection));
        }
        addVerticalGlue();

        if (getScrollPane() != null) {
            SwingUtilities.invokeLater(() -> getScrollPane().getVerticalScrollBar().setValue(0));
        }
    }

    private void addToForm(ACAQParameterTree traversed, ACAQParameterCollection parameterHolder, List<ACAQParameterAccess> parameterAccesses) {
        boolean isModifiable = parameterHolder instanceof ACAQDynamicParameterCollection && ((ACAQDynamicParameterCollection) parameterHolder).isAllowUserModification();

        if (!isModifiable && parameterAccesses.isEmpty())
            return;

        if (!noGroupHeaders) {
            ACAQDocumentation documentation = traversed.getSourceDocumentation(parameterHolder);
            boolean documentationIsEmpty = documentation == null || (StringUtils.isNullOrEmpty(documentation.name()) && StringUtils.isNullOrEmpty(documentation.description()));

            if (!noEmptyGroupHeaders || (!documentationIsEmpty && !isModifiable)) {
                GroupHeaderPanel groupHeaderPanel = addGroupHeader(traversed.getSourceDocumentationName(parameterHolder),
                        UIUtils.getIconFromResources("cog.png"));

                if (documentation != null && !StringUtils.isNullOrEmpty(documentation.description())) {
                    groupHeaderPanel.getDescriptionArea().setVisible(true);
                    groupHeaderPanel.getDescriptionArea().setText(documentation.description());
                }

                if (isModifiable) {
                    JButton addButton = new JButton(UIUtils.getIconFromResources("add.png"));
                    initializeAddDynamicParameterButton(addButton, (ACAQDynamicParameterCollection) parameterHolder);
                    addButton.setToolTipText("Add new parameter");
                    UIUtils.makeFlat25x25(addButton);
                    groupHeaderPanel.addColumn(addButton);
                }
            }
        }

        String[] searchStrings = searchField.getSearchStrings();

        List<ACAQParameterEditorUI> uiList = new ArrayList<>();
        ACAQParameterVisibility sourceVisibility = traversed.getSourceVisibility(parameterHolder);
        for (ACAQParameterAccess parameterAccess : parameterAccesses) {
            ACAQParameterVisibility visibility = parameterAccess.getVisibility();
            if (!visibility.isVisibleIn(sourceVisibility))
                continue;
            if (!searchStringsMatches(parameterAccess, searchStrings))
                continue;

            ACAQParameterEditorUI ui = ACAQUIParameterTypeRegistry.getInstance().createEditorFor(workbench, parameterAccess);
            uiList.add(ui);
        }
        Comparator<ACAQParameterEditorUI> comparator;
        if(withoutLabelSeparation) {
            comparator = Comparator.comparing((ACAQParameterEditorUI u) -> u.getParameterAccess().getUIOrder())
                    .thenComparing(u -> u.getParameterAccess().getName());
        }
        else {
            comparator = Comparator.comparing((ACAQParameterEditorUI u) -> !u.isUILabelEnabled())
                    .thenComparing(u -> u.getParameterAccess().getUIOrder())
                    .thenComparing(u -> u.getParameterAccess().getName());
        }
        for (ACAQParameterEditorUI ui : uiList.stream().sorted(comparator).collect(Collectors.toList())) {
            ACAQParameterAccess parameterAccess = ui.getParameterAccess();
            JPanel labelPanel = new JPanel(new BorderLayout());
            if (ui.isUILabelEnabled())
                labelPanel.add(new JLabel(parameterAccess.getName()), BorderLayout.CENTER);
            if (isModifiable) {
                JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
                removeButton.setToolTipText("Remove this parameter");
                UIUtils.makeBorderlessWithoutMargin(removeButton);
                removeButton.addActionListener(e -> removeDynamicParameter(parameterAccess.getKey(), (ACAQDynamicParameterCollection) parameterHolder));
                labelPanel.add(removeButton, BorderLayout.WEST);
            }

            if (ui.isUILabelEnabled() || parameterHolder instanceof ACAQDynamicParameterCollection)
                addToForm(ui, labelPanel, generateParameterDocumentation(parameterAccess));
            else
                addToForm(ui, generateParameterDocumentation(parameterAccess));
        }
    }

    private MarkdownDocument generateParameterDocumentation(ACAQParameterAccess access) {
        StringBuilder markdownString = new StringBuilder();
        markdownString.append("# Parameter '").append(access.getName()).append("'\n\n");
        ACAQParameterTypeDeclaration declaration = ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(access.getFieldClass());
        if (declaration != null) {
            markdownString.append("<table><tr>");
            markdownString.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/wrench.png")).append("\" /></td>");
            markdownString.append("<td><strong>").append(HtmlEscapers.htmlEscaper().escape(declaration.getName())).append("</strong>: ");
            markdownString.append(HtmlEscapers.htmlEscaper().escape(declaration.getDescription())).append("</td></tr></table>\n\n");
        }
        if (access.getDescription() != null && !access.getDescription().isEmpty()) {
            markdownString.append(access.getDescription());
        } else {
            markdownString.append("No description provided.");
        }
        return new MarkdownDocument(markdownString.toString());
    }

    private boolean searchStringsMatches(ACAQParameterAccess access, String[] strings) {
        if (access == null)
            return true;
        if (strings == null)
            return true;
        String haystack = access.getName() + " " + access.getDescription();
        for (String str : strings) {
            if (haystack.toLowerCase().contains(str.toLowerCase()))
                return true;
        }
        return false;
    }

    private void removeDynamicParameter(String key, ACAQDynamicParameterCollection parameterHolder) {
        ACAQMutableParameterAccess parameter = parameterHolder.getParameter(key);
        if (JOptionPane.showConfirmDialog(this, "Do you really want to remove the parameter '" + parameter.getName() + "'?",
                "Remove parameter", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            parameterHolder.removeParameter(key);
            reloadForm();
        }
    }

    private void initializeAddDynamicParameterButton(JButton addButton, ACAQDynamicParameterCollection parameterHolder) {
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(addButton);
        for (Class<?> allowedType : parameterHolder.getAllowedTypes()) {
            ACAQParameterTypeDeclaration declaration = ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(allowedType);
            String name = allowedType.getSimpleName();
            String description = "Inserts a new parameter";
            if (declaration != null) {
                name = declaration.getName();
                description = declaration.getDescription();
            }

            JMenuItem addItem = new JMenuItem(name, UIUtils.getIconFromResources("add.png"));
            addItem.setToolTipText(description);
            addItem.addActionListener(e -> addDynamicParameter(parameterHolder, allowedType));
            menu.add(addItem);
        }
    }

    private void addDynamicParameter(ACAQDynamicParameterCollection parameterHolder, Class<?> fieldType) {
        String name = UIUtils.getUniqueStringByDialog(this, "Please set the parameter name: ", fieldType.getSimpleName(),
                s -> parameterHolder.getParameters().values().stream().anyMatch(p -> Objects.equals(p.getName(), s)));
        if (name != null) {
            ACAQMutableParameterAccess parameterAccess = parameterHolder.addParameter(name, fieldType);
            parameterAccess.setName(name);
            reloadForm();
        }
    }

    /**
     * Triggered when the parameter structure was changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onParameterStructureChanged(ParameterStructureChangedEvent event) {
        reloadForm();
    }

    /**
     * @return The parameterized object
     */
    public ACAQParameterCollection getDisplayedParameters() {
        return displayedParameters;
    }

    public void setDisplayedParameters(ACAQParameterCollection displayedParameters) {
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

    private static List<String> getParameterKeysSortedByParameterName(Map<String, ACAQParameterAccess> parameters, Collection<String> keys) {
        return keys.stream().sorted(Comparator.comparing(k0 -> parameters.get(k0).getName())).collect(Collectors.toList());
    }
}