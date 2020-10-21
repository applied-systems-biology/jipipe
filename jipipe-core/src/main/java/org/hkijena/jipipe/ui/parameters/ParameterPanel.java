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

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.AddDynamicParameterPanel;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI around a {@link JIPipeParameterCollection}
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
     * {@link org.hkijena.jipipe.extensions.settings.GeneralUISettings}.isShowParameterSearchBar() will override this setting
     */
    public static final int WITH_SEARCH_BAR = 512;

    /**
     * With this flag, parameters that do not show a label are not put below anymore
     */
    public static final int WITHOUT_LABEL_SEPARATION = 1024;

    private JIPipeWorkbench workbench;
    private Context context;
    private JIPipeParameterCollection displayedParameters;
    private boolean noGroupHeaders;
    private boolean noEmptyGroupHeaders;
    private boolean forceTraverse;
    private boolean withSearchBar;
    private boolean withoutLabelSeparation;
    private JIPipeParameterTree traversed;
    private SearchTextField searchField = new SearchTextField();

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
        clear();

        // Create list of filtered-out nodes
        Set<JIPipeParameterCollection> hidden = new HashSet<>();
        for (JIPipeParameterCollection source : traversed.getRegisteredSources()) {
            JIPipeParameterTree.Node sourceNode = traversed.getSourceNode(source);
            for (String subParameterId : sourceNode.getUiExcludedSubParameters()) {
                JIPipeParameterTree.Node subParameterNode = sourceNode.getChildren().getOrDefault(subParameterId, null);
                if (subParameterNode != null) {
                    hidden.add(subParameterNode.getCollection());
                }
            }
        }

        // Generate form
        Map<JIPipeParameterCollection, List<JIPipeParameterAccess>> groupedBySource = traversed.getGroupedBySource();
        if (groupedBySource.containsKey(this.displayedParameters) && !hidden.contains(displayedParameters)) {
            addToForm(traversed, this.displayedParameters, groupedBySource.get(this.displayedParameters));
        }

        for (JIPipeParameterCollection collection : groupedBySource.keySet().stream().sorted(
                Comparator.comparing(traversed::getSourceUIOrder).thenComparing(
                        Comparator.nullsFirst(Comparator.comparing(traversed::getSourceDocumentationName))))
                .collect(Collectors.toList())) {
            if (collection == this.displayedParameters)
                continue;
            if (hidden.contains(collection))
                continue;
            addToForm(traversed, collection, groupedBySource.get(collection));
        }
        addVerticalGlue();

        if (getScrollPane() != null) {
            SwingUtilities.invokeLater(() -> getScrollPane().getVerticalScrollBar().setValue(0));
        }
    }

    private void addToForm(JIPipeParameterTree traversed, JIPipeParameterCollection parameterHolder, List<JIPipeParameterAccess> parameterAccesses) {
        boolean isModifiable = parameterHolder instanceof JIPipeDynamicParameterCollection && ((JIPipeDynamicParameterCollection) parameterHolder).isAllowUserModification();

        if (!isModifiable && parameterAccesses.isEmpty())
            return;

        if (!noGroupHeaders) {
            JIPipeParameterTree.Node node = traversed.getSourceNode(parameterHolder);
            JIPipeDocumentation documentation = traversed.getSourceDocumentation(parameterHolder);
            boolean documentationIsEmpty = documentation == null || (StringUtils.isNullOrEmpty(documentation.name()) && StringUtils.isNullOrEmpty(documentation.description()));
            boolean groupHeaderIsEmpty = documentationIsEmpty && !isModifiable && node.getActions().isEmpty();

            if (!noEmptyGroupHeaders || !groupHeaderIsEmpty) {
                GroupHeaderPanel groupHeaderPanel = addGroupHeader(traversed.getSourceDocumentationName(parameterHolder),
                        UIUtils.getIconFromResources("actions/configure.png"));

                if (documentation != null && !StringUtils.isNullOrEmpty(documentation.description())) {
                    groupHeaderPanel.getDescriptionArea().setVisible(true);
                    groupHeaderPanel.getDescriptionArea().setText(documentation.description());
                }

                for (JIPipeParameterTree.ContextAction action : node.getActions()) {
                    Icon icon = action.getIconURL() != null ? new ImageIcon(action.getIconURL()) : null;
                    JButton actionButton = new JButton(action.getDocumentation().name(), icon);
                    actionButton.setToolTipText(action.getDocumentation().description());
                    actionButton.addActionListener(e -> action.accept(workbench));
                    UIUtils.makeFlat(actionButton);
                    groupHeaderPanel.addColumn(actionButton);
                }


                if (isModifiable) {
                    JButton addButton = new JButton("Add parameter", UIUtils.getIconFromResources("actions/list-add.png"));
                    addButton.addActionListener(e -> addDynamicParameter((JIPipeDynamicParameterCollection) parameterHolder));
                    addButton.setToolTipText("Add new parameter");
                    UIUtils.makeFlat(addButton);
                    groupHeaderPanel.addColumn(addButton);
                }
            }
        }

        List<JIPipeParameterEditorUI> uiList = new ArrayList<>();
        JIPipeParameterVisibility sourceVisibility = traversed.getSourceVisibility(parameterHolder);
        for (JIPipeParameterAccess parameterAccess : parameterAccesses) {
            JIPipeParameterVisibility visibility = parameterAccess.getVisibility();
            if (!visibility.isVisibleIn(sourceVisibility))
                continue;
            if (withSearchBar && !searchField.test(parameterAccess.getName() + " " + parameterAccess.getDescription()))
                continue;

            JIPipeParameterEditorUI ui = JIPipe.getParameterTypes().createEditorFor(workbench, parameterAccess);
            uiList.add(ui);
        }
        Comparator<JIPipeParameterEditorUI> comparator;
        if (withoutLabelSeparation) {
            comparator = Comparator.comparing((JIPipeParameterEditorUI u) -> u.getParameterAccess().getUIOrder())
                    .thenComparing(u -> u.getParameterAccess().getName());
        } else {
            comparator = Comparator.comparing((JIPipeParameterEditorUI u) -> !u.isUILabelEnabled())
                    .thenComparing(u -> u.getParameterAccess().getUIOrder())
                    .thenComparing(u -> u.getParameterAccess().getName());
        }
        for (JIPipeParameterEditorUI ui : uiList.stream().sorted(comparator).collect(Collectors.toList())) {
            JIPipeParameterAccess parameterAccess = ui.getParameterAccess();
            JPanel labelPanel = new JPanel(new BorderLayout());
            if (ui.isUILabelEnabled()) {
                JLabel label = new JLabel(parameterAccess.getName());
                labelPanel.add(label, BorderLayout.CENTER);
            }
            if (isModifiable) {
                JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
                UIUtils.makeBorderlessWithoutMargin(removeButton);
                removeButton.addActionListener(e -> removeDynamicParameter(parameterAccess.getKey(), (JIPipeDynamicParameterCollection) parameterHolder));
                labelPanel.add(removeButton, BorderLayout.WEST);
            }

            if (ui.isUILabelEnabled() || parameterHolder instanceof JIPipeDynamicParameterCollection)
                addToForm(ui, labelPanel, generateParameterDocumentation(parameterAccess));
            else
                addToForm(ui, generateParameterDocumentation(parameterAccess));
        }
    }

    private MarkdownDocument generateParameterDocumentation(JIPipeParameterAccess access) {
        StringBuilder markdownString = new StringBuilder();
        markdownString.append("# Parameter '").append(access.getName()).append("'\n\n");
        markdownString.append("<table><tr>");
        markdownString.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/actions/dialog-xml-editor.png")).append("\" /></td>");
        markdownString.append("<td><strong>Unique identifier</strong>: <code>");
        markdownString.append(HtmlEscapers.htmlEscaper().escape(access.getKey())).append("</code></td></tr>\n\n");

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

    private void removeDynamicParameter(String key, JIPipeDynamicParameterCollection parameterHolder) {
        JIPipeMutableParameterAccess parameter = parameterHolder.getParameter(key);
        if (!GraphEditorUISettings.getInstance().isAskOnDeleteParameter() || JOptionPane.showConfirmDialog(this, "Do you really want to remove the parameter '" + parameter.getName() + "'?",
                "Remove parameter", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            parameterHolder.removeParameter(key);
            reloadForm();
        }
    }

    private void addDynamicParameter(JIPipeDynamicParameterCollection parameterHolder) {
        AddDynamicParameterPanel.showDialog(this, parameterHolder);
        reloadForm();
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

    private static List<String> getParameterKeysSortedByParameterName(Map<String, JIPipeParameterAccess> parameters, Collection<String> keys) {
        return keys.stream().sorted(Comparator.comparing(k0 -> parameters.get(k0).getName())).collect(Collectors.toList());
    }
}