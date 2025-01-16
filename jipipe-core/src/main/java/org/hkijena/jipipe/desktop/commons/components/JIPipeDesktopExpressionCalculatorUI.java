package org.hkijena.jipipe.desktop.commons.components;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluatorSyntaxTokenMaker;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.expressions.ui.ExpressionBuilderUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.scripting.MacroUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class JIPipeDesktopExpressionCalculatorUI extends JIPipeDesktopWorkbenchPanel {

    private final JIPipeExpressionEvaluatorSyntaxTokenMaker tokenMaker = new JIPipeExpressionEvaluatorSyntaxTokenMaker();
    private final JIPipeDesktopFormPanel resultPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
    private final List<ResultItem> resultItems = new ArrayList<>();
    private final JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
    private RSyntaxTextArea expressionEditor;

    public JIPipeDesktopExpressionCalculatorUI(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(UIUtils.createButton("Clear", UIUtils.getIconFromResources("actions/edit-clear-all.png"), this::clearAll));
        toolBar.add(UIUtils.createButton("Set variable", UIUtils.getIconFromResources("actions/math0.png"), this::setVariableAssistant));
        add(toolBar, BorderLayout.NORTH);

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
        expressionEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.isControlDown() || e.isShiftDown())) {
                    evaluate();
                    e.consume();
                    return;
                }
                super.keyReleased(e);
            }
        });
        expressionEditor.getCaret().setVisible(true);
        expressionEditor.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditor.setLineWrap(true);
        expressionEditor.setHighlightCurrentLine(false);

        JPanel expressionEditorPanel = new JPanel(new BorderLayout());
        expressionEditorPanel.setBackground(UIManager.getColor("TextArea.background"));
        expressionEditorPanel.setPreferredSize(new Dimension(256, 64));
        expressionEditorPanel.setMinimumSize(new Dimension(256, 64));
        expressionEditorPanel.setBorder(BorderFactory.createCompoundBorder(UIUtils.createControlBorder(), UIUtils.createEmptyBorder(4)));
        expressionEditorPanel.add(expressionEditor, BorderLayout.CENTER);


        JPanel buttonsPanel = UIUtils.boxHorizontal(
                UIUtils.createIconOnlyButton("Open in expression editor", UIUtils.getIconFromResources("actions/edit.png"), this::showExpressionEditor),
                UIUtils.createIconOnlyButton("<html>Execute expression<br>" +
                        "<i>Shortcut: Ctrl+Enter / Shift+Enter</i></html>", UIUtils.getIconFromResources("actions/run-install.png"), this::evaluate)
        );
        buttonsPanel.setBackground(UIManager.getColor("TextArea.background"));

        expressionEditorPanel.add(buttonsPanel, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(expressionEditorPanel, BorderLayout.NORTH);
//        resultPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
        contentPanel.add(resultPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    private void setVariableAssistant() {
        JTextField keyField = new JTextField();
        JTextField valueField = new JTextField();
        keyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        valueField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
        formPanel.addToForm(keyField, new JLabel("Key"));
        formPanel.addToForm(valueField, new JLabel("Value"));
        formPanel.addVerticalGlue();

        UIUtils.requestFocusOnShown(keyField);


        if (JOptionPane.showConfirmDialog(this, formPanel, "Set variable", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (!StringUtils.isNullOrEmpty(keyField.getText()) && JIPipeExpressionParameter.isValidVariableName(keyField.getText())) {
                if (StringUtils.isValidDouble(valueField.getText())) {
                    evaluate("SET_VARIABLE(\"" + keyField.getText() + "\", " + valueField.getText() + ")");
                } else {
                    evaluate("SET_VARIABLE(\"" + keyField.getText() + "\", \"" + MacroUtils.escapeString(valueField.getText()) + "\")");
                }
                expressionEditor.requestFocusInWindow();
            } else {
                JOptionPane.showMessageDialog(this, "You did not specify a valid variable name", "Set variable", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearAll() {
        resultItems.clear();
        rebuildResultItems();
    }

    private void showExpressionEditor() {
        Set<JIPipeExpressionParameterVariableInfo> variableInfos = new HashSet<>();
        for (Map.Entry<String, Object> entry : variablesMap.entrySet()) {
            variableInfos.add(new JIPipeExpressionParameterVariableInfo(entry.getKey(),
                    entry.getKey(),
                    ""));
        }

        String expression = ExpressionBuilderUI.showDialog(getDesktopWorkbench().getWindow(), expressionEditor.getText(), variableInfos);
        if (expression != null) {
            expressionEditor.setText(expression);
        }
    }

    private void evaluate() {
        evaluate(expressionEditor.getText());
    }

    private void evaluate(String expression) {
        try {
            resultItems.add(new ResultItem(expression, JIPipeExpressionParameter.getEvaluatorInstance().evaluate(expression, variablesMap)));
        } catch (Throwable e) {
            resultItems.add(new ResultItem(expression, e));
        }
        rebuildResultItems();
    }

    private void rebuildResultItems() {
        resultPanel.clear();
        for (ResultItem resultItem : resultItems) {
            JTextField expressionField = UIUtils.createReadonlyBorderlessTextField(resultItem.expression);
            expressionField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            String value;

            if (resultItem.result instanceof Throwable) {
                value = ((Throwable) resultItem.result).getMessage();
            } else {
                value = JsonUtils.toJsonString(resultItem.result);
            }

            JButton copyValueButton = UIUtils.createButton("", UIUtils.getIconFromResources("actions/edit-copy.png"), () -> {
                UIUtils.copyToClipboard(value);
            });
            UIUtils.makeButtonBorderlessWithoutMargin(copyValueButton);

            JTextField valueField = UIUtils.createReadonlyBorderlessTextField(value);

            if (resultItem.result instanceof Throwable) {
                valueField.setForeground(Color.RED);
            }

            valueField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));

            JIPipeDesktopFormPanel itemPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
            itemPanel.setBorder(UIUtils.createControlBorder());
            itemPanel.addWideToForm(expressionField);
            itemPanel.addToForm(valueField, copyValueButton);
            resultPanel.addWideToForm(itemPanel);
        }
        resultPanel.addVerticalGlue();
        UIUtils.invokeScrollToBottom(resultPanel.getScrollPane());
    }

    public static class ResultItem {
        private final String expression;
        private final Object result;

        public ResultItem(String expression, Object result) {
            this.expression = expression;
            this.result = result;
        }

        public String getExpression() {
            return expression;
        }

        public Object getResult() {
            return result;
        }
    }

}
