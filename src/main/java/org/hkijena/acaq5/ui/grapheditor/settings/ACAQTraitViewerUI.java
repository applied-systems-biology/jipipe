package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.ui.components.DocumentChangeListener;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTextField;
import org.jgrapht.event.GraphChangeEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.Set;

public class ACAQTraitViewerUI extends JPanel {

    private ACAQDataSlot<?> slot;
    private ACAQAlgorithmGraph graph;
    private JXTextField searchField;
    private JPanel traitList;

    public ACAQTraitViewerUI(ACAQDataSlot<?> slot, ACAQAlgorithmGraph graph) {
        this.slot = slot;
        this.graph = graph;
        initialize();
        reloadTraitList();
        graph.getEventBus().register(this);
    }

    private void reloadTraitList() {
        traitList.removeAll();
        String[] searchStrings = getSearchStrings();
        Set<Class<? extends ACAQTrait>> currentTraits = graph.getAlgorithmTraits().get(slot);

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
            makeToggleToReadonly(trait, traitButton, currentTraits);

            traitList.add(traitButton);
        }

        traitList.revalidate();
        traitList.repaint();
    }

    @Subscribe
    public void onAlgorithmGraphChanged(AlgorithmGraphChangedEvent event) {
        reloadTraitList();
    }

    private void makeToggleToReadonly(Class<? extends ACAQTrait> trait, JToggleButton traitButton, Set<Class<? extends ACAQTrait>> currentTraits) {
        traitButton.setToolTipText(ACAQTrait.getTooltipOf(trait));
        traitButton.setSelected(currentTraits.contains(trait));
        traitButton.addActionListener(e -> {
           traitButton.setSelected(!traitButton.isSelected());
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
                return new Dimension(ACAQTraitViewerUI.this.getWidth() - 16,
                        super.getPreferredSize().height);
            }
        };
        traitList.setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JScrollPane(traitList), BorderLayout.CENTER);
    }
}
