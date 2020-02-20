package org.hkijena.acaq5.ui.grapheditor;

import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.traits.ACAQMutablePreprocessingTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.*;
import java.util.List;

public class ACAQMutablePreprocessingTraitConfigurationSlotUI extends JPanel {

    private String slotName;
    private ACAQMutablePreprocessingTraitConfiguration configuration;
    private JXTextField searchField;
    private JPanel traitList;

    public ACAQMutablePreprocessingTraitConfigurationSlotUI(String slotName, ACAQMutablePreprocessingTraitConfiguration configuration) {
        this.slotName = slotName;
        this.configuration = configuration;
        initialize();
        reloadTraitList();
    }

    private void reloadTraitList() {
        traitList.removeAll();
        String[] searchStrings = getSearchStrings();

        for(Class<? extends ACAQTrait> trait : ACAQRegistryService.getInstance().getTraitRegistry().getTraits()) {
            if(!searchStringsMatches(trait, searchStrings))
                continue;

            JToggleButton traitButton = new JToggleButton(ACAQTrait.getNameOf(trait),
                    ACAQRegistryService.getInstance().getUITraitRegistry().getIconFor(trait));
//            traitButton.setBackground(new Color(189, 213, 243));
//            traitButton.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(4, 57, 124), 1, true),
//                    BorderFactory.createEmptyBorder(4,4,4,4)));
////            traitButton.setForeground(Color.WHITE);
//            traitButton.setOpaque(true);
            UIUtils.makeFlat(traitButton);
            traitButton.setToolTipText(ACAQTrait.getTooltipOf(trait));
            traitButton.setSelected(configuration.getTraitsOf(slotName).contains(trait));
            traitButton.addActionListener(e -> {
                if(configuration.getTraitsOf(slotName).contains(trait)) {
                    configuration.removeTraitFrom(slotName, trait);
                    traitButton.setSelected(false);
                }
                else {
                    configuration.addTraitTo(slotName, trait);
                    traitButton.setSelected(true);
                }
            });

            traitList.add(traitButton);
        }

        traitList.revalidate();
        traitList.repaint();
    }

    private boolean searchStringsMatches(Class<? extends ACAQTrait> trait, String[] strings){
        if(strings == null)
            return true;
        String traitName = trait.getName() + " " + trait.getCanonicalName();
        for(String str : strings) {
            if(traitName.toLowerCase().contains(str.toLowerCase()))
                return true;
        }
        return false;
    }

    private String[] getSearchStrings() {
        String[] searchStrings = null;
        if(searchField.getText() != null ) {
            String str = searchField.getText().trim();
            if(!str.isEmpty()) {
                searchStrings = str.split(" ");
            }
        }
        return searchStrings;
    }

    private void initialize() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();

        searchField = new JXTextField("Search ...");
        searchField.getDocument().addDocumentListener(new DocumentChangeListener() {
            @Override
            public void changed(DocumentEvent documentEvent) {
                reloadTraitList();
            }
        });
        toolBar.add(searchField);

        JButton clearSearchButton = new JButton(UIUtils.getIconFromResources("clear.png"));
        clearSearchButton.addActionListener(e -> searchField.setText(null));
        toolBar.add(clearSearchButton);

        add(toolBar, BorderLayout.NORTH);

        traitList = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(ACAQMutablePreprocessingTraitConfigurationSlotUI.this.getWidth() - 16,
                        super.getPreferredSize().height);
            }
        };
        traitList.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JScrollPane(traitList), BorderLayout.CENTER);
    }
}
