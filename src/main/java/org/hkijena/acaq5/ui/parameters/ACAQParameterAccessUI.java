package org.hkijena.acaq5.ui.parameters;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
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
        Map<String, ACAQParameterAccess> parameters = ACAQParameterCollection.getParameters(getParameterHolder());
        boolean hasElements = false;

        Map<ACAQParameterCollection, List<String>> groupedByHolder = parameters.keySet().stream().collect(Collectors.groupingBy(key -> parameters.get(key).getParameterHolder()));

        // First display all parameters of the current holder
        if (groupedByHolder.containsKey(parameterHolder)) {
            for (String key : getParameterKeysSortedByParameterName(parameters, groupedByHolder.get(parameterHolder))) {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                if (parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden)
                    continue;

                ACAQParameterEditorUI ui = ACAQDefaultRegistry.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(getContext(), parameterAccess);
                if (ui.isUILabelEnabled())
                    addToForm(ui, new JLabel(parameterAccess.getName()), generateParameterDocumentation(parameterAccess));
                else
                    addToForm(ui, generateParameterDocumentation(parameterAccess));
                hasElements = true;
            }
        }

        // Add missing dynamic parameter holders
        for (ACAQDynamicParameterCollection holder : ACAQDynamicParameterCollection.findDynamicParameterHolders(parameterHolder).values()) {
            if (!groupedByHolder.containsKey(holder))
                groupedByHolder.put(holder, new ArrayList<>());
        }

        // Display parameters of all other holders
        for (ACAQParameterCollection parameterHolder : groupedByHolder.keySet()) {
            if (parameterHolder == this.parameterHolder)
                continue;

            List<String> parameterIds = getParameterKeysSortedByParameterName(parameters, groupedByHolder.get(parameterHolder));
            parameterIds.removeIf(key -> {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                return parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden ||
                        parameterAccess.getVisibility() == ACAQParameterVisibility.Visible;
            });

            boolean parameterHolderIsDynamic = parameterHolder instanceof ACAQDynamicParameterCollection && ((ACAQDynamicParameterCollection) parameterHolder).isAllowUserModification();

            if (parameterIds.isEmpty() && !parameterHolderIsDynamic)
                continue;

            boolean foundHolderName = false;
            boolean foundHolderDescription = false;
            GroupHeaderPanel groupHeaderPanel = addGroupHeader("", null);
            groupHeaderPanel.getDescriptionArea().setVisible(false);
            JLabel holderNameLabel = groupHeaderPanel.getTitleLabel();

            if (parameterHolderIsDynamic) {
                holderNameLabel.setText(((ACAQDynamicParameterCollection) parameterHolder).getName());
                holderNameLabel.setToolTipText(((ACAQDynamicParameterCollection) parameterHolder).getDescription());
                holderNameLabel.setIcon(UIUtils.getIconFromResources("cog.png"));
                foundHolderName = true;
                JButton addButton = new JButton(UIUtils.getIconFromResources("add.png"));
                initializeAddDynamicParameterButton(addButton, (ACAQDynamicParameterCollection) parameterHolder);
                addButton.setToolTipText("Add new parameter");
                UIUtils.makeFlat25x25(addButton);
                groupHeaderPanel.addColumn(addButton);
            }

            for (String key : parameterIds) {
                ACAQParameterAccess parameterAccess = parameters.get(key);

                if (!foundHolderName && !StringUtils.isNullOrEmpty(parameterAccess.getHolderName())) {
                    holderNameLabel.setText(parameterAccess.getHolderName());
                    holderNameLabel.setIcon(UIUtils.getIconFromResources("cog.png"));
                    foundHolderName = true;
                }
                if (!foundHolderDescription && !StringUtils.isNullOrEmpty(parameterAccess.getHolderDescription())) {
                    groupHeaderPanel.getDescriptionArea().setText(parameterAccess.getHolderDescription());
                    groupHeaderPanel.getDescriptionArea().setVisible(true);
                    foundHolderDescription = true;
                }

                ACAQParameterEditorUI ui = ACAQDefaultRegistry.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(getContext(), parameterAccess);

                JPanel labelPanel = new JPanel(new BorderLayout());
                if (ui.isUILabelEnabled())
                    labelPanel.add(new JLabel(parameterAccess.getName()));
                if (parameterHolder instanceof ACAQDynamicParameterCollection && ((ACAQDynamicParameterCollection) parameterHolder).isAllowUserModification()) {
                    JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
                    removeButton.setToolTipText("Remove this parameter");
                    UIUtils.makeBorderlessWithoutMargin(removeButton);
                    removeButton.addActionListener(e -> removeDynamicParameter(parameterAccess.getSlotName(), (ACAQDynamicParameterCollection) parameterHolder));
                    labelPanel.add(removeButton, BorderLayout.WEST);
                }

                if (ui.isUILabelEnabled() || parameterHolder instanceof ACAQDynamicParameterCollection)
                    addToForm(ui, labelPanel, generateParameterDocumentation(parameterAccess));
                else
                    addToForm(ui, generateParameterDocumentation(parameterAccess));

                hasElements = true;
            }
        }

        if (!hasElements) {
            addToForm(new JLabel("There are no parameters",
                            UIUtils.getIconFromResources("info.png"), JLabel.LEFT),
                    null);
        }
        addVerticalGlue();
    }

    private MarkdownDocument generateParameterDocumentation(ACAQParameterAccess access) {
        StringBuilder markdownString = new StringBuilder();
        markdownString.append("# Parameter '").append(access.getName()).append("'\n\n");
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
                s -> parameterHolder.getCustomParameters().values().stream().anyMatch(p -> Objects.equals(p.getName(), s)));
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
