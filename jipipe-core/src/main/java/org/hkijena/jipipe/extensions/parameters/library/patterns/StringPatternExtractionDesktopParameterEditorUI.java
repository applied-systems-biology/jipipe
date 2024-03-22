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

package org.hkijena.jipipe.extensions.parameters.library.patterns;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Editor for {@link StringPatternExtraction}
 */
public class StringPatternExtractionDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {

    /**
     * @param workbench       workbench
     * @param parameterAccess the parameter
     */
    public StringPatternExtractionDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        reload();
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        removeAll();
        StringPatternExtraction filter = getParameter(StringPatternExtraction.class);

        switch (filter.getMode()) {
            case Regex:
                createRegexUI(filter);
                break;
            case SplitAndFind:
                createSplitAndFindUI(filter);
                break;
            case SplitAndPick:
                createSplitAndPickUI(filter);
                break;
        }

        add(Box.createHorizontalStrut(8));

        ButtonGroup group = new ButtonGroup();
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("actions/color-select.png"),
                StringPatternExtraction.Mode.SplitAndPick,
                "Split string and picks the n-th component. The first index is zero.");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("actions/edit-select-text.png"),
                StringPatternExtraction.Mode.SplitAndFind,
                "Split string and picks the component that matches the RegEx string.");
        addFilterModeSelection(filter,
                group,
                UIUtils.getIconFromResources("actions/code-context.png"),
                StringPatternExtraction.Mode.Regex,
                "Use a RegEx matcher to extract the pattern. This requires at least one RegEx group defined by a bracket around the expression.");
        revalidate();
        repaint();
    }

    private void createSplitAndPickUI(StringPatternExtraction filter) {
        add(new JLabel("Split by"));

        JXTextField splitChar = new JXTextField("Split character");
        splitChar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        splitChar.setToolTipText("Splits the input string by this text");
        splitChar.setText(filter.getSplitCharacter());
        splitChar.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                filter.setSplitCharacter(splitChar.getText());
            }
        });
        add(splitChar);

        add(new JLabel("then select nth"));
        SpinnerNumberModel indexModel = new SpinnerNumberModel(filter.getSplitPickedIndex(), 0, Integer.MAX_VALUE, 1);
        JSpinner indexSpinner = new JSpinner(indexModel);
        indexSpinner.setToolTipText("Selects the n-th split component. The first index is zero.");
        indexModel.addChangeListener(e -> filter.setSplitPickedIndex(indexModel.getNumber().intValue()));
        add(indexSpinner);
    }

    private void createSplitAndFindUI(StringPatternExtraction filter) {
        add(new JLabel("Split by"));

        JXTextField splitChar = new JXTextField("Split character");
        splitChar.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        splitChar.setToolTipText("Splits the input string by this text");
        splitChar.setText(filter.getSplitCharacter());
        splitChar.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                filter.setSplitCharacter(splitChar.getText());
            }
        });
        add(splitChar);

        add(new JLabel("then select by RegEx"));
        JXTextField regexString = new JXTextField("RegEx find");
        regexString.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        regexString.setToolTipText("Selects the component that matches this regex string");
        regexString.setText(filter.getRegexString());
        regexString.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                filter.setRegexString(regexString.getText());
            }
        });
        add(regexString);
    }

    private void createRegexUI(StringPatternExtraction filter) {
        JXTextField regexString = new JXTextField("RegEx selector");
        regexString.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        regexString.setToolTipText("A regular expression (RegEx) that contains a group. The group is selected.");
        regexString.setText(filter.getRegexString());
        regexString.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                filter.setRegexString(regexString.getText());
            }
        });
        add(regexString);
    }

    private void addFilterModeSelection(StringPatternExtraction filter, ButtonGroup group, Icon icon, StringPatternExtraction.Mode mode, String description) {
        JToggleButton toggleButton = new JToggleButton(icon);
        UIUtils.makeFlat25x25(toggleButton);
        toggleButton.setSelected(filter.getMode() == mode);
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                filter.setMode(mode);
                reload();
            }
        });
        toggleButton.setToolTipText(description);
        group.add(toggleButton);
        add(toggleButton);
    }
}
