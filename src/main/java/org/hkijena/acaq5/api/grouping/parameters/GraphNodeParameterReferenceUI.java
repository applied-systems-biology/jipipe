package org.hkijena.acaq5.api.grouping.parameters;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.api.registries.ACAQParameterTypeRegistry;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.components.FancyTextField;
import org.hkijena.acaq5.utils.UIUtils;

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
        ACAQParameterTree tree = referenceGroupUI.getParametersUI().getTree();

        JLabel infoLabel = new JLabel(UIUtils.getIconFromResources("parameters.png"));

        ACAQParameterAccess referencedParameter = reference.resolve(tree);
        infoLabel.setToolTipText(String.format("<html><strong>Reference to parameter '%s'</strong><br/><br/>" +
                        "Type <strong>'%s'</strong><br/>" +
                        "Unique key <strong>%s</strong><br/><br/>%s</html>",
                reference.getOriginalName(tree),
                ACAQParameterTypeRegistry.getInstance().getDeclarationByFieldClass(referencedParameter.getFieldClass()).getName(),
                reference.getPath(),
                referencedParameter.getDescription()));
        add(infoLabel, BorderLayout.WEST);


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
    }

    private void changeDescription() {
        ACAQParameterTree tree = referenceGroupUI.getParametersUI().getTree();
        String currentDescription = reference.getDescription(tree);
        String newDescription = UIUtils.getMultiLineStringByDialog(this, "Set description", "Please enter a new description:", currentDescription);
        if (newDescription != null) {
            reference.setCustomDescription(newDescription);
        }
    }
}
