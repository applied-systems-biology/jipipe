package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.ui.ACAQJsonExtensionUI;
import org.hkijena.acaq5.ui.ACAQProjectUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.utils.UIUtils;
import org.scijava.Context;
import org.scijava.Contextual;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ACAQParameterAccessUI extends FormPanel implements Contextual {
    private Context context;
    private ACAQParameterHolder parameterHolder;

    public ACAQParameterAccessUI(Context context, ACAQParameterHolder parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        super(documentation, documentationBelow, withDocumentation);
        this.context = context;
        this.parameterHolder = parameterHolder;
        reloadForm();
    }

    public ACAQParameterAccessUI(ACAQProjectUI workbenchUI, ACAQParameterHolder parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        this(workbenchUI.getContext(), parameterHolder, documentation, documentationBelow, withDocumentation);
    }

    public ACAQParameterAccessUI(ACAQJsonExtensionUI workbenchUI, ACAQParameterHolder parameterHolder, MarkdownDocument documentation, boolean documentationBelow, boolean withDocumentation) {
        this(workbenchUI.getContext(), parameterHolder, documentation, documentationBelow, withDocumentation);
    }

    public void reloadForm() {
        clear();
        Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(getParameterHolder());
        boolean hasElements = false;

        Map<ACAQParameterHolder, List<String>> groupedByHolder = parameters.keySet().stream().collect(Collectors.groupingBy(key -> parameters.get(key).getParameterHolder()));

        // First display all parameters of the current holder
        if (groupedByHolder.containsKey(parameterHolder)) {
            for (String key : groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList())) {
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
        for (ACAQDynamicParameterHolder holder : ACAQDynamicParameterHolder.findDynamicParameterHolders(parameterHolder).values()) {
            if (!groupedByHolder.containsKey(holder))
                groupedByHolder.put(holder, new ArrayList<>());
        }

        // Display parameters of all other holders
        for (ACAQParameterHolder parameterHolder : groupedByHolder.keySet()) {
            if (parameterHolder == this.parameterHolder)
                continue;

            List<String> parameterIds = groupedByHolder.get(parameterHolder).stream().sorted().collect(Collectors.toList());
            parameterIds.removeIf(key -> {
                ACAQParameterAccess parameterAccess = parameters.get(key);
                return parameterAccess.getVisibility() == ACAQParameterVisibility.Hidden ||
                        parameterAccess.getVisibility() == ACAQParameterVisibility.Visible;
            });

            boolean parameterHolderIsDynamic = parameterHolder instanceof ACAQDynamicParameterHolder && ((ACAQDynamicParameterHolder) parameterHolder).isAllowModification();

            if (parameterIds.isEmpty() && !parameterHolderIsDynamic)
                continue;

            boolean foundHolderName = false;

            JPanel subAlgorithmGroupTitle = new JPanel(new BorderLayout());
            if (hasElements) {
                subAlgorithmGroupTitle.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(8, 0, 4, 0),
                        BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY)),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)
                ));
            } else {
                subAlgorithmGroupTitle.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
            }
            JLabel holderNameLabel = new JLabel();
            subAlgorithmGroupTitle.add(holderNameLabel, BorderLayout.CENTER);
            addToForm(subAlgorithmGroupTitle, null);

            if (parameterHolderIsDynamic) {
                holderNameLabel.setText(((ACAQDynamicParameterHolder) parameterHolder).getName());
                holderNameLabel.setToolTipText(((ACAQDynamicParameterHolder) parameterHolder).getDescription());
                holderNameLabel.setIcon(UIUtils.getIconFromResources("cog.png"));
                foundHolderName = true;
                JButton addButton = new JButton(UIUtils.getIconFromResources("add.png"));
                initializeAddDynamicParameterButton(addButton, (ACAQDynamicParameterHolder) parameterHolder);
                addButton.setToolTipText("Add new parameter");
                UIUtils.makeFlat25x25(addButton);
                subAlgorithmGroupTitle.add(addButton, BorderLayout.EAST);
            }

            for (String key : parameterIds) {
                ACAQParameterAccess parameterAccess = parameters.get(key);

                if (!foundHolderName) {
                    holderNameLabel.setText(parameterAccess.getHolderName());
                    holderNameLabel.setIcon(UIUtils.getIconFromResources("cog.png"));
                    foundHolderName = true;
                }

                ACAQParameterEditorUI ui = ACAQDefaultRegistry.getInstance()
                        .getUIParametertypeRegistry().createEditorFor(getContext(), parameterAccess);

                JPanel labelPanel = new JPanel(new BorderLayout());
                if (ui.isUILabelEnabled())
                    labelPanel.add(new JLabel(parameterAccess.getName()));
                if (parameterHolder instanceof ACAQDynamicParameterHolder && ((ACAQDynamicParameterHolder) parameterHolder).isAllowModification()) {
                    JButton removeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
                    removeButton.setToolTipText("Remove this parameter");
                    UIUtils.makeBorderlessWithoutMargin(removeButton);
                    removeButton.addActionListener(e -> removeDynamicParameter(parameterAccess.getKey(), (ACAQDynamicParameterHolder) parameterHolder));
                    labelPanel.add(removeButton, BorderLayout.WEST);
                }

                if (ui.isUILabelEnabled() || parameterHolder instanceof ACAQDynamicParameterHolder)
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
        markdownString.append("# Parameter '" + access.getName() + "'\n\n");
        if (access.getDescription() != null && !access.getDescription().isEmpty()) {
            markdownString.append(access.getDescription());
        } else {
            markdownString.append("No description provided.");
        }
        return new MarkdownDocument(markdownString.toString());
    }

    private void removeDynamicParameter(String key, ACAQDynamicParameterHolder parameterHolder) {
        ACAQMutableParameterAccess parameter = parameterHolder.getParameter(key);
        if (JOptionPane.showConfirmDialog(this, "Do you really want to remove the parameter '" + parameter.getName() + "'?",
                "Remove parameter", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            parameterHolder.removeParameter(key);
            reloadForm();
        }
    }

    private void initializeAddDynamicParameterButton(JButton addButton, ACAQDynamicParameterHolder parameterHolder) {
        JPopupMenu menu = UIUtils.addPopupMenuToComponent(addButton);
        for (Class<?> allowedType : parameterHolder.getAllowedTypes()) {
            JMenuItem addItem = new JMenuItem(allowedType.getSimpleName(), UIUtils.getIconFromResources("add.png"));
            addItem.addActionListener(e -> addDynamicParameter(parameterHolder, allowedType));
            menu.add(addItem);
        }
    }

    private void addDynamicParameter(ACAQDynamicParameterHolder parameterHolder, Class<?> fieldType) {
        String name = UIUtils.getUniqueStringByDialog(this, "Please set the parameter name: ", fieldType.getSimpleName(),
                s -> parameterHolder.getCustomParameters().values().stream().anyMatch(p -> Objects.equals(p.getName(), s)));
        if (name != null) {
            ACAQMutableParameterAccess parameterAccess = parameterHolder.addParameter(name, fieldType);
            parameterAccess.setName(name);
            reloadForm();
        }
    }

    public ACAQParameterHolder getParameterHolder() {
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
}
