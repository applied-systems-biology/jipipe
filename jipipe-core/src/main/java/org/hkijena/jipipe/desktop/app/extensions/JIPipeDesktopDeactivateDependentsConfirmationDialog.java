/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.NaturalOrderComparator;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class JIPipeDesktopDeactivateDependentsConfirmationDialog extends JDialog {
    private final JIPipeDesktopModernPluginManager pluginManager;
    private final JIPipePlugin extension;
    private boolean cancelled = true;

    public JIPipeDesktopDeactivateDependentsConfirmationDialog(Component parent, JIPipeDesktopModernPluginManager pluginManager, JIPipePlugin extension) {
        super(SwingUtilities.getWindowAncestor(parent));
        this.pluginManager = pluginManager;
        this.extension = extension;
        initialize();
    }

    private JIPipeExtensionRegistry getExtensionRegistry() {
        return JIPipe.getInstance().getExtensionRegistry();
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
        setTitle("Deactivate " + extension.getMetadata().getName());

        getContentPane().setLayout(new BorderLayout());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(null, JIPipeDesktopFormPanel.WITH_SCROLLING);

        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Deactivating dependents", UIUtils.getIconFromResources("emblems/important.png"));
        groupHeader.setDescription("The following extensions rely on functionality of '" + extension.getMetadata().getName() + "' and will be deactivated:");

        Set<String> dependents = getExtensionRegistry().getAllDependentsOf(extension.getDependencyId());
        dependents.stream().sorted(NaturalOrderComparator.INSTANCE).forEach(id -> {
            JIPipePlugin dependency = getExtensionRegistry().getKnownExtensionById(id);
            JPanel dependencyPanel = new JPanel(new GridBagLayout());
            dependencyPanel.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 2), BorderFactory.createEmptyBorder(8, 8, 8, 8)));
            dependencyPanel.add(UIUtils.createJLabel(dependency.getMetadata().getName(), UIUtils.getIcon32FromResources("module-json.png"), 16), new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
            JTextField idField = UIUtils.makeReadonlyBorderlessTextField("ID: " + dependency.getDependencyId() + ", version: " + dependency.getDependencyVersion());
            idField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            dependencyPanel.add(idField, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
            dependencyPanel.add(UIUtils.makeBorderlessReadonlyTextPane(dependency.getMetadata().getDescription().getHtml(), false), new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0));
            formPanel.addWideToForm(dependencyPanel);
        });

        formPanel.addVerticalGlue();
        getContentPane().add(formPanel, BorderLayout.CENTER);

        initializeButtonPanel();
        UIUtils.invokeScrollToTop(formPanel.getScrollPane());
    }

    private void initializeButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(Box.createHorizontalGlue());
        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton installButton = new JButton("Deactivate all and continue", UIUtils.getIconFromResources("actions/checkmark.png"));
        installButton.addActionListener(e -> {
            cancelled = false;
            setVisible(false);
        });
        buttonPanel.add(installButton);

        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isCancelled() {
        return cancelled;
    }
}
