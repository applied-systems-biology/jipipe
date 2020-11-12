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

package org.hkijena.jipipe.extensions.parameters.expressions;

import com.fathzer.soft.javaluator.Operator;
import com.google.common.html.HtmlEscapers;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.SearchTextField;
import org.hkijena.jipipe.utils.RankedData;
import org.hkijena.jipipe.utils.RankingFunction;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExpressionBuilderUI extends JPanel {
    public static final Color COLOR_VARIABLE = new Color(0xffaf0a);
    public static final Color COLOR_CONSTANT = Color.BLUE;
    public static final Color COLOR_OPERATOR = new Color(0x854745);
    public static final Color COLOR_FUNCTION = new Color(0xb38814);
    private static final EntryRankingFunction RANKING_FUNCTION = new EntryRankingFunction();
    private static final Function<Object, String> ENTRY_TO_STRING_FUNCTION = new EntryToStringFunction();
    private Set<ExpressionParameterVariable> variables;
    private JList<Object> commandPaletteList = new JList<>();
    private List<ExpressionOperatorEntry> operatorEntryList;
    private List<ExpressionConstantEntry> constantEntryList;
    private SearchTextField searchField = new SearchTextField();
    private DefaultExpressionEvaluatorSyntaxTokenMaker tokenMaker = new DefaultExpressionEvaluatorSyntaxTokenMaker();
    private RSyntaxTextArea expressionEditor;
    private FormPanel inserterForm = new FormPanel(null, FormPanel.WITH_SCROLLING);
    private JPanel inserterButtonPanel = new JPanel();
    private JButton syntaxCheckLabel;

    public ExpressionBuilderUI(String expression, Set<ExpressionParameterVariable> variables) {
        this.variables = variables;
        this.operatorEntryList = ExpressionOperatorEntry.fromEvaluator(DefaultExpressionParameter.EVALUATOR, true);
        this.operatorEntryList.sort(Comparator.comparing(ExpressionOperatorEntry::getName));
        this.constantEntryList = ExpressionConstantEntry.fromEvaluator(DefaultExpressionParameter.EVALUATOR, true);
        this.constantEntryList.sort(Comparator.comparing(ExpressionConstantEntry::getName));
        initialize();
        expressionEditor.setText(expression);
        rebuildPalette();
        updateInserter();
    }

    private void rebuildPalette() {
        List<Object> dataItems = new ArrayList<>();
        if (!variables.isEmpty()) {
            dataItems.addAll(variables.stream().sorted(Comparator.comparing(ExpressionParameterVariable::getName)).collect(Collectors.toList()));
        }
        dataItems.addAll(constantEntryList);
        dataItems.addAll(operatorEntryList);
        dataItems.addAll(JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().values().stream()
                .sorted(Comparator.comparing(JIPipeExpressionRegistry.ExpressionFunctionEntry::getName)).collect(Collectors.toList()));
        String[] searchStrings = searchField.getSearchStrings();
        if (searchStrings == null || searchStrings.length == 0) {
            DefaultListModel<Object> model = new DefaultListModel<>();
            for (Object dataItem : dataItems) {
                model.addElement(dataItem);
            }
            commandPaletteList.setModel(model);
        } else {
            DefaultListModel<Object> model = new DefaultListModel<>();
            for (Object item : RankedData.getSortedAndFilteredData(dataItems, ENTRY_TO_STRING_FUNCTION, RANKING_FUNCTION, searchStrings)) {
                model.addElement(item);
            }
            commandPaletteList.setModel(model);
        }
    }

    private void initialize() {
        // Content
        setLayout(new BorderLayout());

        inserterButtonPanel.setLayout(new BoxLayout(inserterButtonPanel, BoxLayout.X_AXIS));
        JPanel inserterPanel = new JPanel(new BorderLayout());
        inserterPanel.add(inserterForm, BorderLayout.CENTER);
        inserterPanel.add(inserterButtonPanel, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel(new BorderLayout(4, 16));
        TokenMakerFactory tokenMakerFactory = new TokenMakerFactory() {
            @Override
            protected TokenMaker getTokenMakerImpl(String key) {
                return tokenMaker;
            }

            @Override
            public Set<String> keySet() {
                return Collections.singleton("text/expression");
            }
        };
        RSyntaxDocument document = new RSyntaxDocument(tokenMakerFactory, "text/expression");
        expressionEditor = new RSyntaxTextArea(document);
        UIUtils.applyThemeToCodeEditor(expressionEditor);
        expressionEditor.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                expressionEditor.getCaret().setVisible(true);
                expressionEditor.getCaret().setSelectionVisible(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                expressionEditor.getCaret().setVisible(true);
                expressionEditor.getCaret().setSelectionVisible(true);
            }
        });
        expressionEditor.getCaret().setVisible(true);
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);
        JPanel expressionEditorPanel = new JPanel(new BorderLayout());
        expressionEditorPanel.setBorder(BorderFactory.createEtchedBorder());
        expressionEditorPanel.add(expressionEditor, BorderLayout.CENTER);
        contentPanel.add(expressionEditorPanel, BorderLayout.SOUTH);
        contentPanel.add(inserterPanel, BorderLayout.CENTER);

        // Additional buttons for expression editor
        JPanel expressionAdditionalButtonsPanel = new JPanel();
        expressionAdditionalButtonsPanel.setLayout(new BoxLayout(expressionAdditionalButtonsPanel, BoxLayout.Y_AXIS));

        JButton insertBracketsButton = new JButton("()");
        insertBracketsButton.addActionListener(e -> insertBrackets());
        UIUtils.makeFlat25x25(insertBracketsButton);
        expressionAdditionalButtonsPanel.add(insertBracketsButton);

        JButton insertVariableButton = new JButton(UIUtils.getIconFromResources("actions/variable.png"));
        insertVariableButton.addActionListener(e -> insertCustomVariable());
        insertVariableButton.setToolTipText("Inserts a custom variable");
        UIUtils.makeFlat25x25(insertVariableButton);
        expressionAdditionalButtonsPanel.add(insertVariableButton);

        expressionEditorPanel.add(expressionAdditionalButtonsPanel, BorderLayout.WEST);

        // Command panel
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandPaletteList.setCellRenderer(new EntryRenderer());
        commandPaletteList.addListSelectionListener(e -> {
            updateInserter();
        });
        commandPanel.add(new JScrollPane(commandPaletteList), BorderLayout.CENTER);

        commandPanel.add(searchField, BorderLayout.NORTH);
        searchField.addActionListener(e -> rebuildPalette());

        // Main panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentPanel, commandPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        add(splitPane, BorderLayout.CENTER);
    }

    private void insertCustomVariable() {
        JTextField textField = new JTextField();
        textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        int result = JOptionPane.showOptionDialog(
                this,
                new Object[]{"Please input the name of the variable:", textField},
                "Insert custom variable",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, null, null);

        if (result == JOptionPane.OK_OPTION) {
            String variableName = textField.getText();
            if (variableName != null && !variableName.isEmpty()) {
                insertVariableAtCaret(variableName);
            }
        }
    }

    private void insertVariableAtCaret(String variableName) {
        insertAtCaret(DefaultExpressionEvaluator.escapeVariable(variableName));
    }

    private void insertBrackets() {
        int start = expressionEditor.getSelectionStart();
        int end = expressionEditor.getSelectionEnd();
        expressionEditor.insert("(", start);
        expressionEditor.insert(")", end + 1);
    }

    private void updateInserter() {
        inserterForm.clear();
        inserterButtonPanel.removeAll();
        inserterButtonPanel.revalidate();
        inserterButtonPanel.repaint();
        Object value = commandPaletteList.getSelectedValue();
        if (value instanceof ExpressionParameterVariable) {
            ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #ffdc53; \">Variable</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(variable.getName()),
                    HtmlEscapers.htmlEscaper().escape(variable.getKey())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(variable.getDescription());

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.addActionListener(e -> insertAtCaret(variable.getKey()));
            inserterButtonPanel.add(Box.createHorizontalGlue());
            inserterButtonPanel.add(insertButton);
        } else if (value instanceof ExpressionConstantEntry) {
            ExpressionConstantEntry constantEntry = (ExpressionConstantEntry) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #0000ff; \">Constant</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(constantEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(constantEntry.getConstant().getName())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(constantEntry.getDescription());

            inserterButtonPanel.add(Box.createHorizontalGlue());

            JButton insertSimilarVariable = new JButton("Insert variable with same name", UIUtils.getIconFromResources("actions/variable.png"));
            insertSimilarVariable.setToolTipText("Inserts a variable that has the same name as the constant.");
            insertSimilarVariable.addActionListener(e -> insertAtCaret("$\"" + constantEntry.getConstant().getName() + "\""));
            inserterButtonPanel.add(insertSimilarVariable);

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.setToolTipText("Inserts the constant.");
            insertButton.addActionListener(e -> insertAtCaret(constantEntry.getConstant().getName()));
            inserterButtonPanel.add(insertButton);
        } else if (value instanceof ExpressionOperatorEntry) {
            ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #854745; \">Operator</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(operatorEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(operatorEntry.getSignature())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(operatorEntry.getDescription());

            List<ExpressionBuilderParameterUI> parameterEditorUIList = new ArrayList<>();
            for (int i = 0; i < operatorEntry.getOperator().getOperandCount(); i++) {
                ParameterInfo info = operatorEntry.getParameterInfo(i);
                ExpressionBuilderParameterUI parameterUI = new ExpressionBuilderParameterUI();
                JLabel infoLabel = new JLabel(info.getName());
                appendTooltipForParameterLabel(info, infoLabel);
                inserterForm.addToForm(parameterUI, infoLabel, null);
                parameterEditorUIList.add(parameterUI);
            }

            inserterButtonPanel.add(Box.createHorizontalGlue());

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.setToolTipText("Inserts the operator with parameters.");
            insertButton.addActionListener(e -> insertOperator(operatorEntry, parameterEditorUIList));
            inserterButtonPanel.add(insertButton);

            JButton insertSymbolButton = new JButton("Insert only symbol", UIUtils.getIconFromResources("actions/format-text-symbol.png"));
            insertSymbolButton.setToolTipText("Inserts the operator symbol.");
            insertSymbolButton.addActionListener(e -> insertAtCaret(operatorEntry.getOperator().getSymbol()));
            inserterButtonPanel.add(insertSymbolButton);
        } else if (value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
            JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader(String.format("<html><i style=\"color: #b38814; \">Function</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(functionEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(functionEntry.getFunction().getName())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(functionEntry.getDescription());

            List<ExpressionBuilderParameterUI> parameterEditorUIList = new ArrayList<>();
            if (functionEntry.getFunction().getMinimumArgumentCount() < functionEntry.getFunction().getMaximumArgumentCount()) {
                JButton addParameterButton = new JButton("Add parameter", UIUtils.getIconFromResources("actions/list-add.png"));
                addParameterButton.addActionListener(e -> {
                    if (parameterEditorUIList.size() < functionEntry.getFunction().getMaximumArgumentCount()) {
                        ParameterInfo info = functionEntry.getFunction().getParameterInfo(parameterEditorUIList.size());
                        ExpressionBuilderParameterUI parameterUI = new ExpressionBuilderParameterUI();
                        JLabel infoLabel = new JLabel(info.getName());
                        appendTooltipForParameterLabel(info, infoLabel);
                        inserterForm.removeLastRow();
                        inserterForm.addToForm(parameterUI, infoLabel, null);
                        inserterForm.addVerticalGlue();
                        parameterEditorUIList.add(parameterUI);
                        inserterForm.revalidate();
                        inserterForm.repaint();
                    }
                });
                groupHeader.addColumn(addParameterButton);
            }
            for (int i = 0; i < functionEntry.getFunction().getMinimumArgumentCount(); i++) {
                ParameterInfo info = functionEntry.getFunction().getParameterInfo(i);
                ExpressionBuilderParameterUI parameterUI = new ExpressionBuilderParameterUI();
                JLabel infoLabel = new JLabel(info.getName());
                appendTooltipForParameterLabel(info, infoLabel);
                inserterForm.addToForm(parameterUI, infoLabel, null);
                parameterEditorUIList.add(parameterUI);
            }

            inserterButtonPanel.add(Box.createHorizontalGlue());

            JButton insertButton = new JButton("Insert", UIUtils.getIconFromResources("actions/insert-object.png"));
            insertButton.setToolTipText("Inserts the function with parameters.");
            insertButton.addActionListener(e -> insertFunction(functionEntry, parameterEditorUIList));
            inserterButtonPanel.add(insertButton);

            JButton insertSymbolButton = new JButton("Insert only symbol", UIUtils.getIconFromResources("actions/format-text-symbol.png"));
            insertSymbolButton.setToolTipText("Inserts the function symbol.");
            insertSymbolButton.addActionListener(e -> insertAtCaret(functionEntry.getFunction().getName()));
            inserterButtonPanel.add(insertSymbolButton);
        } else {
            FormPanel.GroupHeaderPanel groupHeader = inserterForm.addGroupHeader("Expression builder", UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription("Welcome to the expression builder that simplifies the creation of expressions. On the right-hand side you will find a list of all available functions and operators. " +
                    "Please note that the list of variables can be incomplete depending on various factors.");
        }
        inserterForm.addVerticalGlue();
    }

    private void appendTooltipForParameterLabel(ParameterInfo info, JLabel infoLabel) {
        if (!StringUtils.isNullOrEmpty(info.getDescription()) && !info.getTypes().isEmpty())
            return;
        StringBuilder tooltipBuilder = new StringBuilder();
        tooltipBuilder.append("<html>");
        if (!StringUtils.isNullOrEmpty(info.getDescription()))
            tooltipBuilder.append(info.getDescription()).append("<br/><br/>");
        for (Class<?> type : info.getTypes()) {
            if (type == String.class) {
                tooltipBuilder.append("Accepts: Strings<br/>");
            } else if (Number.class.isAssignableFrom(type)) {
                tooltipBuilder.append("Accepts: Numbers<br/>");
            } else if (Collection.class.isAssignableFrom(type)) {
                tooltipBuilder.append("Accepts: Arrays<br/>");
            } else if (Map.class.isAssignableFrom(type)) {
                tooltipBuilder.append("Accepts: Maps<br/>");
            } else {
                tooltipBuilder.append("Accepts: ").append(type.getSimpleName()).append("<br/>");
            }
        }
        tooltipBuilder.append("</html>");
        infoLabel.setToolTipText(tooltipBuilder.toString());
    }

    private void insertFunction(JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry, List<ExpressionBuilderParameterUI> parameterEditorUIList) {
        StringBuilder result = new StringBuilder();
        result.append(functionEntry.getFunction().getName());
        result.append("(");
        boolean first = true;
        for (ExpressionBuilderParameterUI parameterUI : parameterEditorUIList) {
            if (first)
                first = false;
            else
                result.append(", ");
            result.append(parameterUI.getCurrentExpressionValue());
        }
        result.append(")");
        insertAtCaret(result.toString());
    }

    private void insertOperator(ExpressionOperatorEntry operatorEntry, List<ExpressionBuilderParameterUI> parameterEditorUIList) {
        if (operatorEntry.getOperator().getOperandCount() == 1) {
            if (operatorEntry.getOperator().getAssociativity() == Operator.Associativity.LEFT) {
                boolean symbolic = DefaultExpressionParameter.EVALUATOR.getKnownNonAlphanumericOperatorTokens().contains(operatorEntry.getOperator().getSymbol());
                if (symbolic)
                    insertAtCaret(parameterEditorUIList.get(0).getCurrentExpressionValue() + operatorEntry.getOperator().getSymbol());
                else
                    insertAtCaret(parameterEditorUIList.get(0).getCurrentExpressionValue() + " " + operatorEntry.getOperator().getSymbol());
            } else {
                boolean symbolic = DefaultExpressionParameter.EVALUATOR.getKnownNonAlphanumericOperatorTokens().contains(operatorEntry.getOperator().getSymbol());
                if (symbolic)
                    insertAtCaret(operatorEntry.getOperator().getSymbol() + parameterEditorUIList.get(0).getCurrentExpressionValue());
                else
                    insertAtCaret(operatorEntry.getOperator().getSymbol() + parameterEditorUIList.get(0).getCurrentExpressionValue());
            }
        } else {
            insertAtCaret(parameterEditorUIList.get(0).getCurrentExpressionValue() + " " + operatorEntry.getOperator().getSymbol() + " " + parameterEditorUIList.get(1).getCurrentExpressionValue());
        }
    }

    private void insertAtCaret(String text) {
        int caret = expressionEditor.getCaretPosition();
        if (caret > 0) {
            try {
                if (!Objects.equals(expressionEditor.getText(caret - 1, 1), " "))
                    text = " " + text;
            } catch (BadLocationException e) {
            }
        }
        try {
            if (!Objects.equals(expressionEditor.getText(caret, 1), " "))
                text = text + " ";
        } catch (BadLocationException e) {
        }
        expressionEditor.insert(text, expressionEditor.getCaretPosition());
    }

    private String getExpression() {
        return expressionEditor.getText().trim();
    }

    public static String showDialog(Component parent, String expression, Set<ExpressionParameterVariable> variables) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));

        ExpressionBuilderUI expressionBuilderUI = new ExpressionBuilderUI(expression, variables);
        JPanel contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.add(expressionBuilderUI, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        buttonPanel.add(new ExpressionBuilderSyntaxChecker(expressionBuilderUI.expressionEditor));

        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Discard", UIUtils.getIconFromResources("actions/cancel.png"));
        cancelButton.addActionListener(e -> dialog.setVisible(false));
        buttonPanel.add(cancelButton);

        AtomicBoolean confirmed = new AtomicBoolean(false);
        JButton confirmButton = new JButton("Accept", UIUtils.getIconFromResources("actions/checkmark.png"));
        confirmButton.addActionListener(e -> {
            confirmed.set(true);
            dialog.setVisible(false);
        });
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.setModal(true);
        dialog.setTitle("Expression builder");
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return confirmed.get() ? expressionBuilderUI.getExpression() : null;
    }

    public static class EntryToStringFunction implements Function<Object, String> {

        @Override
        public String apply(Object value) {
            if (value instanceof ExpressionParameterVariable) {
                ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
                return variable.getKey() + " " + variable.getName();
            } else if (value instanceof ExpressionConstantEntry) {
                ExpressionConstantEntry constantEntry = (ExpressionConstantEntry) value;
                return constantEntry.getConstant().getName() + " " + constantEntry.getName();
            } else if (value instanceof ExpressionOperatorEntry) {
                ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
                return operatorEntry.getOperator().getSymbol() + " " + operatorEntry.getName();
            } else if (value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
                return functionEntry.getFunction().getName() + " " + functionEntry.getName();
            } else {
                return "";
            }
        }
    }

    public static class EntryRankingFunction implements RankingFunction<Object> {

        @Override
        public int[] rank(Object value, String[] filterStrings) {
            // Symbol, name, description
            int[] result = new int[3];
            if (filterStrings.length == 0)
                return result;
            if (value instanceof ExpressionParameterVariable) {
                ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
                for (String string : filterStrings) {
                    if (variable.getKey().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if (variable.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if (variable.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            } else if (value instanceof ExpressionConstantEntry) {
                ExpressionConstantEntry constantEntry = (ExpressionConstantEntry) value;
                for (String string : filterStrings) {
                    if (constantEntry.getConstant().getName().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if (constantEntry.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if (constantEntry.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            } else if (value instanceof ExpressionOperatorEntry) {
                ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
                for (String string : filterStrings) {
                    if (operatorEntry.getOperator().getSymbol().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if (operatorEntry.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if (operatorEntry.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            } else if (value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
                for (String string : filterStrings) {
                    if (functionEntry.getFunction().getName().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if (functionEntry.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if (functionEntry.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            }
            if (result[0] == 0 && result[1] == 0 && result[2] == 0) {
                return null;
            } else {
                return result;
            }
        }
    }

    public static class EntryRenderer extends JPanel implements ListCellRenderer<Object> {

        private JLabel typeLabel;
        private JLabel idLabel;
        private JLabel nameLabel;
        private JLabel descriptionLabel;

        public EntryRenderer() {
            initialize();
        }

        private void initialize() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            Insets border = new Insets(2, 4, 2, 2);

            typeLabel = new JLabel();
            typeLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
            add(typeLabel, new GridBagConstraints() {
                {
                    gridx = 0;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });

            idLabel = new JLabel();
            idLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
            add(idLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 0;
                    anchor = WEST;
                    insets = border;
                }
            });

            nameLabel = new JLabel();
            add(nameLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 1;
                    anchor = WEST;
                    insets = border;
                }
            });

            descriptionLabel = new JLabel();
            descriptionLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
            add(descriptionLabel, new GridBagConstraints() {
                {
                    gridx = 1;
                    gridy = 2;
                    anchor = WEST;
                    insets = border;
                }
            });
            JPanel glue = new JPanel();
            glue.setOpaque(false);
            add(glue, new GridBagConstraints() {
                {
                    gridx = 2;
                    weightx = 1;
                }
            });
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof ExpressionParameterVariable) {
                typeLabel.setText("Variable");
                typeLabel.setForeground(COLOR_VARIABLE);
                ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
                idLabel.setText(variable.getKey());
                nameLabel.setText(variable.getName());
            } else if (value instanceof ExpressionConstantEntry) {
                typeLabel.setText("Constant");
                typeLabel.setForeground(COLOR_CONSTANT);
                ExpressionConstantEntry constantEntry = (ExpressionConstantEntry) value;
                idLabel.setText(constantEntry.getConstant().getName());
                nameLabel.setText(constantEntry.getName());
            } else if (value instanceof ExpressionOperatorEntry) {
                typeLabel.setText("Operator");
                typeLabel.setForeground(COLOR_OPERATOR);
                ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
                idLabel.setText(operatorEntry.getSignature());
                nameLabel.setText(operatorEntry.getName());
            } else if (value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                typeLabel.setText("Function");
                typeLabel.setForeground(COLOR_FUNCTION);
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
                idLabel.setText(functionEntry.getFunction().getName());
                nameLabel.setText(functionEntry.getName());
            }
            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }
            return this;
        }
    }
}
