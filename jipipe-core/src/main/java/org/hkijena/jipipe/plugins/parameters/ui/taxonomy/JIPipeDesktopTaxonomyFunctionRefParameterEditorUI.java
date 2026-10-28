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

package org.hkijena.jipipe.plugins.parameters.ui.taxonomy;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.taxonomy.FunctionRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.nodebrowser.NodeBrowserTreeController;
import com.google.common.html.HtmlEscapers;
import org.hkijena.jipipe.desktop.commons.components.textfield.JIPipeDesktopDocumentChangeListener;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter editor for {@link JIPipeTaxonomyFunctionRef}.
 * <p>
 * Shows a button with the resolved function's name (or "Not set") next to a
 * "Browse…" button that opens a modal function-tree picker. The picker reuses the
 * node browser's {@link NodeBrowserTreeController} (and therefore the shared
 * {@code FunctionTreeCellRenderer}) so branches and leaves look identical to the
 * add-nodes panel; both branches and leaves are selectable, and an "Unset"
 * action clears the reference.
 */
public class JIPipeDesktopTaxonomyFunctionRefParameterEditorUI extends JIPipeDesktopParameterEditorUI<JIPipeTaxonomyFunctionRef> {

    private static final Icon NOT_SET_ICON = JIPipe.RESOURCES.getIcon16("emblems/vcs-conflicting.png");

    private JButton currentlyDisplayed;
    private JDialog pickerDialog;

    public JIPipeDesktopTaxonomyFunctionRefParameterEditorUI(InitializationParameters parameters) {
        super(JIPipeTaxonomyFunctionRef.class, parameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        currentlyDisplayed = new JButton();
        currentlyDisplayed.setHorizontalAlignment(SwingConstants.LEFT);
        currentlyDisplayed.addActionListener(e -> openPicker());
        currentlyDisplayed.setToolTipText("Select the taxonomy function");
        UIUtils.setStandardButtonBorder(currentlyDisplayed);
        // #1339 item 6: no separate "Browse" button — the display button opens
        // the same picker.
        add(currentlyDisplayed, BorderLayout.CENTER);
    }

    @Override
    public void reload() {
        JIPipeTaxonomyFunctionRef ref = getParameter();
        JIPipeNodeTaxonomyFunction function = ref.getFunction();
        if (function != null) {
            currentlyDisplayed.setText(function.getName());
            currentlyDisplayed.setToolTipText("<html><u><strong>" + function.getName() + "</strong></u><br/>"
                    + HtmlEscapers.htmlEscaper().escape(function.getDescription()) + "</html>");
            currentlyDisplayed.setIcon(displayIcon(function));
        } else {
            currentlyDisplayed.setText("Not set");
            currentlyDisplayed.setToolTipText(ref.getId() != null
                    ? "Set but unresolved: " + ref.getId()
                    : "No taxonomy function selected");
            currentlyDisplayed.setIcon(NOT_SET_ICON);
        }
    }

    /**
     * The icon for a resolved function: the function's own icon, or the
     * icon of its closest ancestor that carries one (leaf functions have no
     * icon of their own — falling back to the branch icon instead of the
     * "not set" emblem, which wrongly signalled an error for a selected
     * function).
     */
    private static Icon displayIcon(JIPipeNodeTaxonomyFunction function) {
        Set<String> visited = new HashSet<>();
        JIPipeNodeTaxonomyFunction current = function;
        while (current != null && visited.add(current.getId())) {
            Icon icon = current.getIcon();
            if (icon != null) {
                return icon;
            }
            current = current.getParent();
        }
        return JIPipe.RESOURCES.getIcon16("actions/tag.png");
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    // ------------------------------------------------------------------
    // Picker dialog (lazily built; the tree is refreshed on every open so it
    // always reflects the live registry and the current filter)
    // ------------------------------------------------------------------

    private NodeBrowserTreeController treeController;
    private JTextField filterField;
    private JTree tree;

    private void openPicker() {
        if (pickerDialog == null) {
            buildPickerDialog();
        }
        // Reset the filter and refresh from the live registry, then reveal the current value
        filterField.setText("");
        refreshTree();
        JIPipeTaxonomyFunctionRef ref = getParameter();
        JIPipeNodeTaxonomyFunction function = ref.getFunction();
        if (function != null) {
            TreePath path = treeController.functionRow(function);
            if (path != null) {
                treeController.selectRow(path);
            }
        }
        pickerDialog.setVisible(true);
    }

    private void buildPickerDialog() {
        treeController = new NodeBrowserTreeController();
        tree = treeController.getTree();

        // Double-click on a function row confirms the selection
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && treeController.functionFromTreePath(tree.getSelectionPath()) != null) {
                    confirmSelection();
                }
            }
        });

        filterField = new JTextField();
        filterField.setToolTipText("Filter by name");
        filterField.getDocument().addDocumentListener(new JIPipeDesktopDocumentChangeListener() {
            @Override
            public void changed(DocumentEvent e) {
                refreshTree();
            }
        });

        JButton selectButton = new JButton("Select");
        UIUtils.setStandardButtonBorder(selectButton);
        selectButton.addActionListener(e -> confirmSelection());

        JButton resetButton = new JButton("Reset");
        UIUtils.setStandardButtonBorder(resetButton);
        resetButton.setToolTipText("Clear the taxonomy function");
        resetButton.addActionListener(e -> {
            setParameter(new JIPipeTaxonomyFunctionRef(), true);
            closePicker();
        });

        JButton cancelButton = new JButton("Cancel");
        UIUtils.setStandardButtonBorder(cancelButton);
        cancelButton.addActionListener(e -> closePicker());

        // [Reset] ←glue→ [Cancel][Select] — the standard dialog button order
        // (reset separated; affirmative last, nearest the corner).
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonBar.add(resetButton);
        buttonBar.add(Box.createHorizontalGlue());
        buttonBar.add(cancelButton);
        buttonBar.add(selectButton);

        JPanel filterBar = new JPanel(new BorderLayout(6, 0));
        filterBar.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        filterBar.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterBar.add(filterField, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.add(filterBar, BorderLayout.NORTH);
        content.add(treeController, BorderLayout.CENTER);
        content.add(buttonBar, BorderLayout.SOUTH);

        pickerDialog = new JDialog();
        pickerDialog.setTitle("Select taxonomy function");
        pickerDialog.setModal(true);
        pickerDialog.setContentPane(content);
        pickerDialog.setIconImage(UIUtils.getJIPipeIcon128());
        pickerDialog.getRootPane().setDefaultButton(selectButton);
        pickerDialog.pack();
        pickerDialog.setSize(new Dimension(480, 420));
        pickerDialog.setLocationRelativeTo(this);
    }

    /**
     * Populates the picker tree from the live {@link FunctionRegistry}, applying the
     * current filter. With an empty filter every function is shown; otherwise a
     * function row is kept when it (or an ancestor) matches the name filter, and
     * the ancestor chain of a matching row is retained so it stays reachable.
     */
    private void refreshTree() {
        FunctionRegistry registry = JIPipe.getNodes().getFunctions();
        String filter = filterField == null ? "" : filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        Set<JIPipeNodeTaxonomyFunction> matched = new HashSet<>();
        for (JIPipeNodeTaxonomyFunction function : registry.all()) {
            if (filter.isEmpty() || function.getName().toLowerCase().contains(filter)) {
                matched.add(function);
            }
        }

        DefaultTreeModel treeModel = treeController.getTreeModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        root.removeAllChildren();
        for (JIPipeNodeTaxonomyFunction top : registry.topLevel()) {
            DefaultMutableTreeNode node = buildFunctionTreeNode(top, registry, matched, false);
            if (node != null) {
                root.add(node);
            }
        }
        treeModel.reload();
        // Keep the top level open so the tree is usable immediately
        treeController.getTree().expandPath(new TreePath(new Object[]{root}));
    }

    /**
     * Recursively builds a (filtered) tree node for a function.
     *
     * @return the node, or {@code null} when the function and its subtree are
     * both filtered out (so empty branches are not shown)
     */
    private static DefaultMutableTreeNode buildFunctionTreeNode(JIPipeNodeTaxonomyFunction function,
                                                                FunctionRegistry registry,
                                                                Set<JIPipeNodeTaxonomyFunction> matched,
                                                                boolean ancestorMatched) {
        boolean selfMatched = matched.contains(function);
        boolean keep = ancestorMatched || selfMatched;

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(function);
        for (JIPipeNodeTaxonomyFunction child : registry.children(function)) {
            DefaultMutableTreeNode childNode = buildFunctionTreeNode(child, registry, matched, ancestorMatched || selfMatched);
            if (childNode != null) {
                node.add(childNode);
            }
        }

        // A non-matching branch with no visible descendants is pruned
        if (!keep && node.getChildCount() == 0) {
            return null;
        }
        return node;
    }

    private void confirmSelection() {
        JIPipeNodeTaxonomyFunction function = treeController.functionFromTreePath(tree.getSelectionPath());
        if (function == null) {
            return;
        }
        setParameter(new JIPipeTaxonomyFunctionRef(function.getId()), true);
        closePicker();
    }

    private void closePicker() {
        pickerDialog.setVisible(false);
    }
}
