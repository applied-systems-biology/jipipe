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

package org.hkijena.jipipe.api.grouping.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FancyTextField;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link GraphNodeParameterReference}
 */
public class GraphNodeParameterReferenceUI extends JPanel {
    private final GraphNodeParameterReferenceGroupUI referenceGroupUI;
    private final GraphNodeParameterReference reference;

    /**
     * Creates a new instance
     *
     * @param referenceGroupUI the parent
     * @param reference        the reference to edit
     */
    public GraphNodeParameterReferenceUI(GraphNodeParameterReferenceGroupUI referenceGroupUI, GraphNodeParameterReference reference) {
        this.referenceGroupUI = referenceGroupUI;
        this.reference = reference;
        this.initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout(4, 0));
        JIPipeParameterTree tree = referenceGroupUI.getParametersUI().getTree();

        JLabel infoLabel = new JLabel(UIUtils.getIconFromResources("parameters.png"));

        JIPipeParameterAccess referencedParameter = reference.resolve(tree);
        if (referencedParameter != null) {
            infoLabel.setToolTipText(String.format("<html><strong>Reference to parameter '%s'</strong><br/><br/>" +
                            "Type <strong>'%s'</strong><br/>" +
                            "Unique key <strong>%s</strong><br/><br/>%s</html>",
                    reference.getOriginalName(tree),
                    JIPipeParameterTypeRegistry.getInstance().getInfoByFieldClass(referencedParameter.getFieldClass()).getName(),
                    reference.getPath(),
                    referencedParameter.getDescription()));
            FancyTextField nameEditor = new FancyTextField(new JLabel(UIUtils.getIconFromResources("cog.png")),
                    reference.getOriginalName(tree));
            nameEditor.setText(reference.getName(tree));
            nameEditor.getTextField().getDocument().addDocumentListener(new DocumentChangeListener() {
                @Override
                public void changed(DocumentEvent documentEvent) {
                    reference.setCustomName(nameEditor.getText());
                }
            });
            add(nameEditor, BorderLayout.CENTER);

            JButton changeDescriptionButton = new JButton(UIUtils.getIconFromResources("text2.png"));
            UIUtils.makeFlat(changeDescriptionButton);
            changeDescriptionButton.setToolTipText("Change description");
            changeDescriptionButton.addActionListener(e -> changeDescription());
            add(changeDescriptionButton, BorderLayout.EAST);
        } else {
            infoLabel.setIcon(UIUtils.getIconFromResources("error.png"));
            infoLabel.setText("Not found");
            infoLabel.setToolTipText("The parameter '" + reference.getPath() + "' was not found. Please remove this item.");
        }
        add(infoLabel, BorderLayout.WEST);
    }

    private void changeDescription() {
        JIPipeParameterTree tree = referenceGroupUI.getParametersUI().getTree();
        String currentDescription = reference.getDescription(tree);
        String newDescription = UIUtils.getMultiLineStringByDialog(this, "Set description", "Please enter a new description:", currentDescription);
        if (newDescription != null) {
            reference.setCustomDescription(newDescription);
        }
    }
}
