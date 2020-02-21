package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitGenerator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;
import org.jgrapht.event.GraphChangeEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;

public class ACAQTraitGeneratorUI extends JPanel {

    private String slotName;
    private ACAQMutableTraitGenerator configuration;
    private JXTextField searchField;
    private JPanel traitList;

    public ACAQTraitGeneratorUI(String slotName, ACAQMutableTraitGenerator configuration, ACAQAlgorithmGraph graph) {
        this.slotName = slotName;
        this.configuration = configuration;
        initialize();
        reloadTraitList();
        graph.getEventBus().register(this);
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
            makeToggleToEditor(trait, traitButton);

            traitList.add(traitButton);
        }

        traitList.revalidate();
        traitList.repaint();
    }

    private void makeToggleToEditor(Class<? extends ACAQTrait> trait, JToggleButton traitButton) {
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
        toolBar.setFloatable(false);

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
                return new Dimension(ACAQTraitGeneratorUI.this.getWidth() - 16,
                        super.getPreferredSize().height);
            }
        };
        traitList.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JScrollPane(traitList), BorderLayout.CENTER);
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        reloadTraitList();
    }
}
