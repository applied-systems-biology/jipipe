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

package org.hkijena.jipipe.extensions.expressions.ui;

import com.fathzer.soft.javaluator.Operator;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.components.search.SearchTextField;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.search.RankedData;
import org.hkijena.jipipe.utils.search.RankingFunction;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExpressionBuilderUI extends JPanel {
    public static final Color COLOR_VARIABLE = new Color(0xffaf0a);
    public static final Color COLOR_CONSTANT = Color.BLUE;
    public static final Color COLOR_OPERATOR = new Color(0x854745);
    public static final Color COLOR_FUNCTION = new Color(0x14A0B3);
    public static final Color COLOR_STRING = new Color(0xe80da5);
    private static final EntryRankingFunction RANKING_FUNCTION = new EntryRankingFunction();
    private static final Function<Object, String> ENTRY_TO_STRING_FUNCTION = new EntryToStringFunction();
    private final SearchTextField searchField = new SearchTextField();
    private final DefaultExpressionEvaluatorSyntaxTokenMaker tokenMaker = new DefaultExpressionEvaluatorSyntaxTokenMaker();
    private Set<ExpressionParameterVariable> variables;
    private JList<Object> commandPaletteList = new JList<>();
    private List<ExpressionOperatorEntry> operatorEntryList;
    private List<ExpressionConstantEntry> constantEntryList;
    private RSyntaxTextArea expressionEditor;
    private DocumentTabPane tabPane = new DocumentTabPane();
    private ExpressionBuilderInserterUI lastVariableInserter;

    public ExpressionBuilderUI(String expression, Set<ExpressionParameterVariable> variables) {
        this.variables = variables;
        this.operatorEntryList = ExpressionOperatorEntry.fromEvaluator(DefaultExpressionParameter.getEvaluatorInstance(), true);
        this.operatorEntryList.sort(Comparator.comparing(ExpressionOperatorEntry::getName));
        this.constantEntryList = ExpressionConstantEntry.fromEvaluator(DefaultExpressionParameter.getEvaluatorInstance(), true);
        this.constantEntryList.sort(Comparator.comparing(ExpressionConstantEntry::getName));
        initialize();
        expressionEditor.setText(expression);
        rebuildPalette();
    }

    public static String showDialog(Component parent, String expression, Set<ExpressionParameterVariable> variables) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));
        dialog.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());

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
            if (expressionBuilderUI.checkInserterBeforeAccept()) {
                confirmed.set(true);
                dialog.setVisible(false);
            }
        });
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.setModal(true);
        dialog.setTitle("Expression builder");
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return confirmed.get() ? expressionBuilderUI.getExpression() : null;
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

        // Create documentation
        tabPane.addSingletonTab("DOCUMENTATION",
                "Info",
                UIUtils.getIconFromResources("actions/help-info.png"),
                new MarkdownReader(false, MarkdownDocument.fromPluginResource("documentation/expression-editor.md", Collections.emptyMap())),
                DocumentTabPane.CloseMode.withoutCloseButton, false);

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
        contentPanel.add(tabPane, BorderLayout.CENTER);

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
            createInserter();
        });
        commandPanel.add(new JScrollPane(commandPaletteList), BorderLayout.CENTER);

        commandPanel.add(searchField, BorderLayout.NORTH);
        searchField.addActionListener(e -> rebuildPalette());

        // Main panel
        AutoResizeSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, contentPanel, commandPanel, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);
    }

    private void createInserter() {
        if (commandPaletteList.getSelectedValue() != null) {
            Object value = commandPaletteList.getSelectedValue();

            // Variables and constants will share one tab -> Will re-open
            if (value instanceof ExpressionParameterVariable || value instanceof ExpressionConstantEntry) {
                if (lastVariableInserter != null) {
                    DocumentTabPane.DocumentTab tab = tabPane.getTabContainingContent(lastVariableInserter);
                    tabPane.forceCloseTab(tab);
                }
            } else {
                // Find another tab with the same content, but without parameters
                DocumentTabPane.DocumentTab existing = null;
                for (DocumentTabPane.DocumentTab tab : tabPane.getTabs()) {
                    if (tab.getContent() instanceof ExpressionBuilderInserterUI) {
                        ExpressionBuilderInserterUI inserterUI = (ExpressionBuilderInserterUI) tab.getContent();
                        if (!inserterUI.parametersWereEdited()) {
                            existing = tab;
                            break;
                        }
                    }
                }
                if (existing != null) {
                    tabPane.forceCloseTab(existing);
                }
            }

            String title;
            Icon icon;
            ExpressionBuilderInserterUI inserterUI = new ExpressionBuilderInserterUI(this, value);
            if (value instanceof ExpressionParameterVariable) {
                title = "Variable " + ((ExpressionParameterVariable) value).getName();
                icon = UIUtils.getIconFromResources("actions/variable.png");
                lastVariableInserter = inserterUI;
            } else if (value instanceof ExpressionConstantEntry) {
                title = "Constant " + ((ExpressionConstantEntry) value).getName();
                icon = UIUtils.getIconFromResources("actions/insert-variable.png");
                lastVariableInserter = inserterUI;
            } else if (value instanceof ExpressionOperatorEntry) {
                title = "Operator " + ((ExpressionOperatorEntry) value).getName();
                icon = UIUtils.getIconFromResources("actions/insert-operator.png");
            } else if (value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                title = "Function " + ((JIPipeExpressionRegistry.ExpressionFunctionEntry) value).getName();
                icon = UIUtils.getIconFromResources("actions/insert-math-expression.png");
            } else {
                return;
            }
            tabPane.addTab(title,
                    icon,
                    inserterUI,
                    DocumentTabPane.CloseMode.withSilentCloseButton,
                    false);
            tabPane.switchToLastTab();
        }
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
        expressionEditor.requestFocusInWindow();
    }

    private void insertBrackets() {
        int start = expressionEditor.getSelectionStart();
        int end = expressionEditor.getSelectionEnd();
        expressionEditor.insert("(", start);
        expressionEditor.insert(")", end + 1);
        expressionEditor.requestFocusInWindow();
    }

    public void insertFunction(JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry, List<ExpressionBuilderParameterUI> parameterEditorUIList) {
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
        expressionEditor.requestFocusInWindow();
    }

    public void insertOperator(ExpressionOperatorEntry operatorEntry, List<ExpressionBuilderParameterUI> parameterEditorUIList) {
        if (operatorEntry.getOperator().getOperandCount() == 1) {
            if (operatorEntry.getOperator().getAssociativity() == Operator.Associativity.LEFT) {
                boolean symbolic = DefaultExpressionParameter.getEvaluatorInstance().getKnownNonAlphanumericOperatorTokens().contains(operatorEntry.getOperator().getSymbol());
                if (symbolic)
                    insertAtCaret(parameterEditorUIList.get(0).getCurrentExpressionValue() + operatorEntry.getOperator().getSymbol());
                else
                    insertAtCaret(parameterEditorUIList.get(0).getCurrentExpressionValue() + " " + operatorEntry.getOperator().getSymbol());
            } else {
                boolean symbolic = DefaultExpressionParameter.getEvaluatorInstance().getKnownNonAlphanumericOperatorTokens().contains(operatorEntry.getOperator().getSymbol());
                if (symbolic)
                    insertAtCaret(operatorEntry.getOperator().getSymbol() + parameterEditorUIList.get(0).getCurrentExpressionValue());
                else
                    insertAtCaret(operatorEntry.getOperator().getSymbol() + parameterEditorUIList.get(0).getCurrentExpressionValue());
            }
        } else {
            insertAtCaret(parameterEditorUIList.get(0).getCurrentExpressionValue() + " " + operatorEntry.getOperator().getSymbol() + " " + parameterEditorUIList.get(1).getCurrentExpressionValue());
        }
        expressionEditor.requestFocusInWindow();
    }

    public void insertAtCaret(String text) {
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
        expressionEditor.requestFocusInWindow();
    }

    private String getExpression() {
        return expressionEditor.getText().trim();
    }

    private void insertCurrentlyInsertedValue() {
        if (tabPane.getCurrentContent() != null && tabPane.getCurrentContent() instanceof ExpressionBuilderInserterUI) {
            ExpressionBuilderInserterUI inserterUI = (ExpressionBuilderInserterUI) tabPane.getCurrentContent();
            if (!inserterUI.isInserterCommitted()) {
                Object currentlyInsertedObject = inserterUI.getInsertedObject();
                if (currentlyInsertedObject instanceof ExpressionParameterVariable) {
                    ExpressionParameterVariable variable = (ExpressionParameterVariable) currentlyInsertedObject;
                    if (!StringUtils.isNullOrEmpty(variable.getKey())) {
                        insertAtCaret(variable.getKey());
                    }
                } else if (currentlyInsertedObject instanceof ExpressionConstantEntry) {
                    ExpressionConstantEntry constantEntry = (ExpressionConstantEntry) currentlyInsertedObject;
                    insertAtCaret(constantEntry.getConstant().getName());
                } else if (currentlyInsertedObject instanceof ExpressionOperatorEntry) {
                    insertOperator((ExpressionOperatorEntry) currentlyInsertedObject, inserterUI.getInserterParameterEditorUIList());
                } else if (currentlyInsertedObject instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                    insertFunction((JIPipeExpressionRegistry.ExpressionFunctionEntry) currentlyInsertedObject, inserterUI.getInserterParameterEditorUIList());
                }
            }
        }
    }

    private boolean checkInserterBeforeAccept() {
        if (tabPane.getCurrentContent() != null && tabPane.getCurrentContent() instanceof ExpressionBuilderInserterUI) {
            ExpressionBuilderInserterUI inserterUI = (ExpressionBuilderInserterUI) tabPane.getCurrentContent();
            if (inserterUI.getInsertedObject() != null) {
                if (!inserterUI.isInserterCommitted() && inserterUI.parametersWereEdited()) {
                    int result = JOptionPane.showOptionDialog(this,
                            "You still have uncommitted values in the function builder.\n" +
                                    "Do you want to replace the existing expression or insert the function into the expression?",
                            "Expression builder",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            new Object[]{"Insert", "Replace", "Cancel"},
                            "New window");
                    switch (result) {
                        case JOptionPane.YES_OPTION:
                            insertCurrentlyInsertedValue();
                            break;
                        case JOptionPane.NO_OPTION:
                            expressionEditor.setText("");
                            insertCurrentlyInsertedValue();
                            break;
                        default:
                            return false;
                    }
                }
            }
        }
        return true;
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
                    if (variable.getKey().toLowerCase().startsWith(string.toLowerCase()))
                        result[0] -= 2;
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
                    if (constantEntry.getConstant().getName().toLowerCase().startsWith(string.toLowerCase()))
                        result[0] -= 2;
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