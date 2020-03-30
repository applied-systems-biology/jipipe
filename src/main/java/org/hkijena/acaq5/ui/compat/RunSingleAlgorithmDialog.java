package org.hkijena.acaq5.ui.compat;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compat.AlgorithmDeclarationListCellRenderer;
import org.hkijena.acaq5.api.compat.SingleImageJAlgorithmRun;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RunSingleAlgorithmDialog extends JDialog {
    private Context context;
    private boolean canceled = true;
    private SingleImageJAlgorithmRun runSettings;
    private ACAQAlgorithm algorithm;
    private JList<ACAQAlgorithmDeclaration> algorithmList;
    private JXTextField searchField;
    private JSplitPane splitPane;
    private FormPanel formPanel;

    public RunSingleAlgorithmDialog(Context context) {
        this.context = context;
        initialize();
    }

    private void initialize() {
        JPanel contentPanel = new JPanel(new BorderLayout(8,8));

        JPanel listPanel = new JPanel(new BorderLayout());
        formPanel = new FormPanel(null, false, false);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, formPanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.33);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.33);
            }
        });
        contentPanel.add(splitPane, BorderLayout.CENTER);

        initializeToolbar(listPanel);
        initializeList(listPanel);
        initializeButtonPanel(contentPanel);
        setContentPane(contentPanel);
        reloadAlgorithmList();
    }

    private void initializeToolbar(JPanel contentPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                reloadAlgorithmList();
            }
        });
        toolBar.add(searchField);

        JButton clearSearchButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearSearchButton.addActionListener(e -> searchField.setText(null));
        toolBar.add(clearSearchButton);

        add(toolBar, BorderLayout.NORTH);

        contentPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void reloadAlgorithmList() {
        List<ACAQAlgorithmDeclaration> declarations = getFilteredAndSortedDeclarations();
        DefaultListModel<ACAQAlgorithmDeclaration> model = (DefaultListModel<ACAQAlgorithmDeclaration>) algorithmList.getModel();
        model.clear();
        for (ACAQAlgorithmDeclaration declaration : declarations) {
            if(SingleImageJAlgorithmRun.isCompatible(declaration))
                model.addElement(declaration);
        }

        if(!model.isEmpty())
            algorithmList.setSelectedIndex(0);
        else
            selectAlgorithmDeclaration(null);
    }

    private List<ACAQAlgorithmDeclaration> getFilteredAndSortedDeclarations() {
        String[] searchStrings = getSearchStrings();
        Predicate<ACAQAlgorithmDeclaration> filterFunction = declaration -> {
            if (searchStrings != null && searchStrings.length > 0) {
                boolean matches = true;
                String name = declaration.getName();
                for (String searchString : searchStrings) {
                    if (!name.toLowerCase().contains(searchString.toLowerCase())) {
                        matches = false;
                        break;
                    }
                }
                return matches;
            } else {
                return true;
            }
        };

        return ACAQAlgorithmRegistry.getInstance().getRegisteredAlgorithms().values().stream().filter(filterFunction)
                .sorted(Comparator.comparing(ACAQAlgorithmDeclaration::getName)).collect(Collectors.toList());
    }

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if (searchField.getText() != null) {
            String str = searchField.getText().trim();
            if (!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
    }


    private void initializeList(JPanel listPanel) {
        algorithmList = new JList<>();
        algorithmList.setBorder(BorderFactory.createEtchedBorder());
        algorithmList.setCellRenderer(new AlgorithmDeclarationListCellRenderer());
        algorithmList.setModel(new DefaultListModel<>());
        algorithmList.addListSelectionListener(e -> {
            selectAlgorithmDeclaration(algorithmList.getSelectedValue());
        });
        listPanel.add(new JScrollPane(algorithmList), BorderLayout.CENTER);
    }

    private void selectAlgorithmDeclaration(ACAQAlgorithmDeclaration declaration) {
        formPanel.clear();
        if(declaration != null) {
            algorithm = declaration.newInstance();

            // Add some descriptions
            JTextPane descriptions = new JTextPane();
            descriptions.setContentType("text/html");
            descriptions.setText(TooltipUtils.getAlgorithmTooltip(declaration, false));
            descriptions.setEditable(false);
            descriptions.setBorder(null);
            descriptions.setOpaque(false);
            formPanel.addWideToForm(descriptions, null);

            // Add parameter editor
            formPanel.addGroupHeader("Algorithm parameters", UIUtils.getIconFromResources("wrench.png"));
            formPanel.addWideToForm(new ACAQParameterAccessUI(context, algorithm, null, false,
                    false, false), null);

        }
        formPanel.addVerticalGlue();
    }

    private void initializeButtonPanel(JPanel contentPanel) {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = new JButton("Cancel", UIUtils.getIconFromResources("remove.png"));
        cancelButton.addActionListener(e -> {
            this.canceled = true;
            this.setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton confirmButton = new JButton("Run", UIUtils.getIconFromResources("run.png"));
        confirmButton.addActionListener(e -> runNow());
        buttonPanel.add(confirmButton);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void runNow() {

    }

    public boolean isCanceled() {
        return canceled;
    }

    public String getAlgorithmId() {
        return algorithm.getDeclaration().getId();
    }

    public String getAlgorithmParametersJson() {
        try {
            return JsonUtils.getObjectMapper().writeValueAsString(runSettings);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    public SingleImageJAlgorithmRun getRunSettings() {
        return runSettings;
    }
}
