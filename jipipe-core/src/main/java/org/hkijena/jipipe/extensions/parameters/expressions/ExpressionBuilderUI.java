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
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExpressionBuilderUI extends JPanel {
    public static final Color COLOR_VARIABLE = new Color(0xffdc53);
    public static final Color COLOR_OPERATOR = new Color(0x854745);
    public static final Color COLOR_FUNCTION = new Color(0xb38814);
    private String expression;
    private Set<ExpressionParameterVariable> variables;
    private JList<Object> commandPaletteList = new JList<>();
    private List<ExpressionOperatorEntry> operatorEntryList = new ArrayList<>();
    private SearchTextField searchField = new SearchTextField();
    private static final EntryRankingFunction RANKING_FUNCTION = new EntryRankingFunction();
    private static final Function<Object, String> ENTRY_TO_STRING_FUNCTION = new EntryToStringFunction();
    private DefaultExpressionEvaluatorSyntaxTokenMaker tokenMaker = new DefaultExpressionEvaluatorSyntaxTokenMaker();
    private RSyntaxTextArea expressionEditor;
    private FormPanel inserterPanel = new FormPanel(null, FormPanel.WITH_SCROLLING);

    public ExpressionBuilderUI(String expression, Set<ExpressionParameterVariable> variables) {
        this.expression = expression;
        this.variables = variables;
        for (Operator operator : DefaultExpressionParameter.EVALUATOR.getOperators()) {
            if(operator instanceof ExpressionOperator) {
                operatorEntryList.add(new ExpressionOperatorEntry(operator));
            }
        }
        operatorEntryList.sort(Comparator.comparing(ExpressionOperatorEntry::getName));

        initialize();
        expressionEditor.setText(expression);
        rebuildPalette();
        updateInserter();
    }

    private void rebuildPalette() {
        List<Object> dataItems = new ArrayList<>();
        if(!variables.isEmpty()) {
            dataItems.addAll(variables.stream().sorted(Comparator.comparing(ExpressionParameterVariable::getName)).collect(Collectors.toList()));
        }
        dataItems.addAll(operatorEntryList);
        dataItems.addAll(JIPipe.getInstance().getExpressionRegistry().getRegisteredExpressionFunctions().values().stream()
                .sorted(Comparator.comparing(JIPipeExpressionRegistry.ExpressionFunctionEntry::getName)).collect(Collectors.toList()));
        String[] searchStrings = searchField.getSearchStrings();
        if(searchStrings == null || searchStrings.length == 0) {
            DefaultListModel<Object> model = new DefaultListModel<>();
            for (Object dataItem : dataItems) {
                model.addElement(dataItem);
            }
            commandPaletteList.setModel(model);
        }
        else {
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

        JPanel contentPanel = new JPanel(new BorderLayout());
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
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);
        contentPanel.add(expressionEditor, BorderLayout.SOUTH);
        contentPanel.add(inserterPanel, BorderLayout.CENTER);

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

    private void updateInserter() {
        inserterPanel.clear();
        Object value = commandPaletteList.getSelectedValue();
        if(value instanceof ExpressionParameterVariable) {
            ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterPanel.addGroupHeader(String.format("<html><i style=\"color: #ffdc53; \">Variable</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(variable.getName()),
                    HtmlEscapers.htmlEscaper().escape(variable.getKey())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(variable.getDescription());
        }
        else if(value instanceof ExpressionOperatorEntry) {
            ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterPanel.addGroupHeader(String.format("<html><i style=\"color: #854745; \">Operator</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(operatorEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(operatorEntry.getSignature())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(operatorEntry.getDescription());
        }
        else if(value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
            JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
            FormPanel.GroupHeaderPanel groupHeader = inserterPanel.addGroupHeader(String.format("<html><i style=\"color: #b38814; \">Function</i> %s (<code><strong>%s</strong></code>)</html>",
                    HtmlEscapers.htmlEscaper().escape(functionEntry.getName()),
                    HtmlEscapers.htmlEscaper().escape(functionEntry.getFunction().getName())), UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription(functionEntry.getDescription());
        }
        else {
            FormPanel.GroupHeaderPanel groupHeader = inserterPanel.addGroupHeader("Expression builder", UIUtils.getIconFromResources("actions/insert-function.png"));
            groupHeader.setDescription("Welcome to the expression builder that simplifies the creation of expressions. On the right-hand side you will find a list of all available functions and operators. " +
                    "Please note that the list of variables can be incomplete depending on various factors.");
        }
        inserterPanel.addVerticalGlue();
    }

    public static String showDialog(Component parent, String expression, Set<ExpressionParameterVariable> variables) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent));

        ExpressionBuilderUI expressionBuilderUI = new ExpressionBuilderUI(expression, variables);
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(expressionBuilderUI, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
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
        expressionBuilderUI.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    confirmed.set(true);
                    dialog.setVisible(false);
                }
            }
        });
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.setModal(true);
        dialog.setTitle("Expression builder");
        dialog.pack();
        dialog.setSize(800,600);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        return confirmed.get() ? expressionBuilderUI.expression : null;
    }

    public static class EntryToStringFunction implements Function<Object, String> {

        @Override
        public String apply(Object value) {
            if(value instanceof ExpressionParameterVariable) {
                ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
              return variable.getKey() + " " + variable.getName();
            }
            else if(value instanceof ExpressionOperatorEntry) {
                ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
                return operatorEntry.getOperator().getSymbol() + " " + operatorEntry.getName();
            }
            else if(value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
                return functionEntry.getFunction().getName() + " " + functionEntry.getName();
            }
            else {
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
            if(value instanceof ExpressionParameterVariable) {
                ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
                for (String string : filterStrings) {
                    if(variable.getKey().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if(variable.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if(variable.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            }
            else if(value instanceof ExpressionOperatorEntry) {
                ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
                for (String string : filterStrings) {
                    if(operatorEntry.getOperator().getSymbol().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if(operatorEntry.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if(operatorEntry.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            }
            else if(value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
                for (String string : filterStrings) {
                    if(functionEntry.getFunction().getName().toLowerCase().contains(string.toLowerCase()))
                        --result[0];
                    if(functionEntry.getName().toLowerCase().contains(string.toLowerCase()))
                        --result[1];
                    if(functionEntry.getDescription().toLowerCase().contains(string.toLowerCase()))
                        --result[2];
                }
            }
            if (result[0] == 0 && result[1] == 0 && result[2] == 0) {
                return null;
            }
            else {
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
            if(value instanceof ExpressionParameterVariable) {
                typeLabel.setText("Variable");
                typeLabel.setForeground(COLOR_VARIABLE);
                ExpressionParameterVariable variable = (ExpressionParameterVariable) value;
                idLabel.setText(variable.getKey());
                nameLabel.setText(variable.getName());
//                descriptionLabel.setText(variable.getDescription());
            }
            else if(value instanceof ExpressionOperatorEntry) {
                typeLabel.setText("Operator");
                typeLabel.setForeground(COLOR_OPERATOR);
                ExpressionOperatorEntry operatorEntry = (ExpressionOperatorEntry) value;
                idLabel.setText(operatorEntry.getSignature());
                nameLabel.setText(operatorEntry.getName());
//                descriptionLabel.setText(operatorEntry.getDescription());
            }
            else if(value instanceof JIPipeExpressionRegistry.ExpressionFunctionEntry) {
                typeLabel.setText("Function");
                typeLabel.setForeground(COLOR_FUNCTION);
                JIPipeExpressionRegistry.ExpressionFunctionEntry functionEntry = (JIPipeExpressionRegistry.ExpressionFunctionEntry) value;
                idLabel.setText(functionEntry.getFunction().getName());
                nameLabel.setText(functionEntry.getName());
//                descriptionLabel.setText(functionEntry.getDescription());
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
