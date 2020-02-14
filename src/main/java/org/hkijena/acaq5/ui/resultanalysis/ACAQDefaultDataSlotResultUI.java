package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.List;

/**
 * Provides a standard result slot UI that can be also further extended
 */
public class ACAQDefaultDataSlotResultUI extends ACAQResultDataSlotUI<ACAQDataSlot<?>> {

    private List<SlotAction> registeredSlotActions = new ArrayList<>();

    public ACAQDefaultDataSlotResultUI(ACAQDataSlot<?> slot) {
        super(slot);
        registerActions();
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(Box.createHorizontalGlue());
        if(!registeredSlotActions.isEmpty()) {
            SlotAction mainSlotAction = registeredSlotActions.get(registeredSlotActions.size() - 1);
            JButton mainActionButton = new JButton(mainSlotAction.getName(), mainSlotAction.getIcon());
            mainActionButton.addActionListener(e -> mainSlotAction.action.accept(getSlot()));
            add(mainActionButton);
        }
        if(registeredSlotActions.size() > 1) {
            JButton menuButton = new JButton(UIUtils.getIconFromResources("magic.png"));
            menuButton.setToolTipText("More actions ...");
            JPopupMenu menu = UIUtils.addPopupMenuToComponent(menuButton);
            for(int i = 1; i < registeredSlotActions.size(); ++i) {
                SlotAction otherSlotAction = registeredSlotActions.get(i);
                JMenuItem item = new JMenuItem(otherSlotAction.getName(), otherSlotAction.getIcon());
                item.addActionListener(e -> otherSlotAction.getAction().accept(getSlot()));
                menu.add(item);
            }
            add(menuButton);
        }
    }

    /**
     * Override this method to add actions
     * The last added action is displayed as full button
     */
    protected void registerActions() {
        if(getSlot().getStoragePath() != null) {
            registerAction("Open folder", UIUtils.getIconFromResources("open.png"), s -> openFolder());
        }
    }

    /**
     * Registers an action for the data slot
     * @param name
     * @param icon
     * @param action
     */
    protected void registerAction(String name, Icon icon, Consumer<ACAQDataSlot<?>> action) {
        registeredSlotActions.add(new SlotAction(name, icon, action));
    }

    private void openFolder() {
        try {
            Desktop.getDesktop().open(Objects.requireNonNull(getSlot().getStoragePath()).toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class SlotAction {
        private String name;
        private Icon icon;
        private Consumer<ACAQDataSlot<?>> action;

        private SlotAction(String name, Icon icon, Consumer<ACAQDataSlot<?>> action) {
            this.name = name;
            this.icon = icon;
            this.action = action;
        }

        public String getName() {
            return name;
        }

        public Icon getIcon() {
            return icon;
        }

        public Consumer<ACAQDataSlot<?>> getAction() {
            return action;
        }
    }
}
