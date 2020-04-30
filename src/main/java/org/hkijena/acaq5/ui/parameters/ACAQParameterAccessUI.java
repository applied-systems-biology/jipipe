package org.hkijena.acaq5.ui.parameters;

import com.google.common.eventbus.Subscribe;
import com.google.common.html.HtmlEscapers;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
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
public class ACAQParameterAccessUI extends FormPanel implements Contextual {
    private Context context;
    private ACAQParameterCollection parameterHolder;

    /**
     * @param context            SciJava context
     * @param parameterHolder    Parameter holder
     * @param documentation      Optional documentation. Can be null.
     * @param documentationBelow If true, show documentation below
     * @param withDocumentation  If documentation is shown
     * @param withScrolling      Allows disabling the scroll view
     */
    public ACAQParameterAccessUI(Context context, ACAQParameterCollection parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation, boolean withScrolling) {
        super(documentation, documentationBelow, withDocumentation, withScrolling);
        this.context = context;
        this.parameterHolder = parameterHolder;
        reloadForm();
        this.parameterHolder.getEventBus().register(this);
    }

    /**
     * @param context            SciJava context
     * @param parameterHolder    Parameter holder
     * @param documentation      Optional documentation. Can be null.
     * @param documentationBelow If true, show documentation below
     * @param withDocumentation  If documentation is shown
     */
    public ACAQParameterAccessUI(Context context, ACAQParameterCollection parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        this(context, parameterHolder, documentation, documentationBelow, withDocumentation, true);
    }

    /**
     * @param workbenchUI        Workbench UI
     * @param parameterHolder    Parameter holder
     * @param documentation      Optional documentation. Can be null.
     * @param documentationBelow If true, show documentation below
     * @param withDocumentation  If documentation is shown
     */
    public ACAQParameterAccessUI(ACAQProjectWorkbench workbenchUI, ACAQParameterCollection parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        this(workbenchUI.getContext(), parameterHolder, documentation, documentationBelow, withDocumentation, true);
    }

    /**
     * @param workbenchUI        Workbench UI
     * @param parameterHolder    Parameter holder
     * @param documentation      Optional documentation. Can be null.
     * @param documentationBelow If true, show documentation below
     * @param withDocumentation  If documentation is shown
     */
    public ACAQParameterAccessUI(ACAQJsonExtensionWorkbench workbenchUI, ACAQParameterCollection parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        this(workbenchUI.getContext(), parameterHolder, documentation, documentationBelow, withDocumentation, true);
    }

    /**
     * Reloads the form
     */
    public void reloadForm() {
        clear();
        ACAQTraversedParameterCollection parameterCollection = new ACAQTraversedParameterCollection(getParameterHolder());

        Map<ACAQParameterCollection, List<ACAQParameterAccess>> groupedBySource = parameterCollection.getGroupedBySource();
        if (groupedBySource.containsKey(parameterHolder)) {
            addToForm(parameterCollection, parameterHolder, groupedBySource.get(parameterHolder));
        }

        for (ACAQParameterCollection collection : groupedBySource.keySet().stream().sorted(
                Comparator.nullsFirst(Comparator.comparing(parameterCollection::getSourceDocumentationName)))
                .collect(Collectors.toList())) {
            if (collection == parameterHolder)
                continue;
            addToForm(parameterCollection, collection, groupedBySource.get(collection));
        }
        addVerticalGlue();
    }

    private void addToForm(ACAQTraversedParameterCollection parameterCollection, ACAQParameterCollection parameterHolder, List<ACAQParameterAccess> parameterAccesses) {
        boolean isModifiable = parameterHolder instanceof ACAQDynamicParameterCollection && ((ACAQDynamicParameterCollection) parameterHolder).isAllowUserModification();

        if (!isModifiable && parameterAccesses.isEmpty())
            return;

        GroupHeaderPanel groupHeaderPanel = addGroupHeader(parameterCollection.getSourceDocumentationName(parameterHolder),
                UIUtils.getIconFromResources("cog.png"));
        ACAQDocumentation documentation = parameterCollection.getSourceDocumentation(parameterHolder);
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

        List<ACAQParameterEditorUI> uiList = new ArrayList<>();
        for (ACAQParameterAccess parameterAccess : parameterAccesses) {
            ACAQParameterEditorUI ui = ACAQDefaultRegistry.getInstance()
                    .getUIParametertypeRegistry().createEditorFor(getContext(), parameterAccess);
            uiList.add(ui);
        }
        for (ACAQParameterEditorUI ui : uiList.stream().sorted(Comparator.comparing((ACAQParameterEditorUI u) -> !u.isUILabelEnabled())
                .thenComparing(u -> u.getParameterAccess().getName())).collect(Collectors.toList())) {
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
        ACAQDocumentation documentation = ACAQUIParametertypeRegistry.getInstance().getDocumentationFor(access.getFieldClass());
        if (documentation != null) {
            markdownString.append("<table><tr>");
            markdownString.append("<td><img src=\"").append(ResourceUtils.getPluginResource("icons/data-types/data-type-parameters.png")).append("\" /></td>");
            markdownString.append("<td><strong>").append(HtmlEscapers.htmlEscaper().escape(documentation.name())).append("</strong>: ");
            markdownString.append(HtmlEscapers.htmlEscaper().escape(documentation.description())).append("</td></tr></table>\n\n");
        }
        if (access.getDescription() != null && !access.getDescription().isEmpty()) {
            markdownString.append(access.getDescription());
        } else {
            markdownString.append("No description provided.");
        }
        return new MarkdownDocument(markdownString.toString());
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
            ACAQDocumentation documentation = ACAQUIParametertypeRegistry.getInstance().getDocumentationFor(allowedType);
            String name = allowedType.getSimpleName();
            String description = "Inserts a new parameter";
            if (documentation != null) {
                name = documentation.name();
                description = documentation.description();
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
    public ACAQParameterCollection getParameterHolder() {
        return parameterHolder;
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
