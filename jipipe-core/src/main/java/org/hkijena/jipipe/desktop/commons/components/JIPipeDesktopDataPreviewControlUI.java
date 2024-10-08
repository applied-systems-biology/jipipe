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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Optional;

public class JIPipeDesktopDataPreviewControlUI extends JPanel implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeGeneralDataApplicationSettings dataSettings = JIPipeGeneralDataApplicationSettings.getInstance();
    private final JButton zoomStatusButton = new JButton();

    public JIPipeDesktopDataPreviewControlUI() {
        initialize();
        refreshZoomStatus();
        dataSettings.getParameterChangedEventEmitter().subscribeWeak(this);
    }

    private void refreshZoomStatus() {
        zoomStatusButton.setText(dataSettings.getPreviewSize() + "px");
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/square-minus.png"));
        UIUtils.makeButtonFlat25x25(zoomOutButton);
        zoomOutButton.addActionListener(e -> decreaseSize());
        add(zoomOutButton);

        UIUtils.makeButtonBorderlessWithoutMargin(zoomStatusButton);
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToButton(zoomStatusButton);
        initializeZoomMenu(zoomMenu);
        add(zoomStatusButton);

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/square-plus.png"));
        UIUtils.makeButtonFlat25x25(zoomInButton);
        zoomInButton.addActionListener(e -> increaseSize());
        add(zoomInButton);
    }

    private void initializeZoomMenu(JPopupMenu zoomMenu) {
        addZoomOption(zoomMenu, 32);
        addZoomOption(zoomMenu, 48);
        addZoomOption(zoomMenu, 64);
        addZoomOption(zoomMenu, 120);
        addZoomOption(zoomMenu, 180);
        addZoomOption(zoomMenu, 240);
        addZoomOption(zoomMenu, 350);
        zoomMenu.addSeparator();
        {
            JMenuItem menuItem = new JMenuItem("Custom preview size ...");
            menuItem.addActionListener(e -> setSizeManually());
            zoomMenu.add(menuItem);
        }
    }

    private void addZoomOption(JPopupMenu zoomMenu, int size) {
        JMenuItem menuItem = new JMenuItem(size + " px");
        menuItem.setIcon(UIUtils.getIconFromResources("actions/zoom.png"));
        menuItem.addActionListener(e -> {
            ParameterUtils.setParameter(dataSettings, "preview-size", size);
        });
        zoomMenu.add(menuItem);
    }

    private void setSizeManually() {
        int current = dataSettings.getPreviewSize();
        Optional<Integer> selected = UIUtils.getIntegerByDialog(this, "Set preview size", "Set the preview size in pixels:", current, 40, Integer.MAX_VALUE);
        if (selected.isPresent()) {
            int newSize = selected.get();
            if (newSize != current) {
                ParameterUtils.setParameter(dataSettings, "preview-size", newSize);
            }
        }
    }

    private void decreaseSize() {
        int current = dataSettings.getPreviewSize();
        int change = 10;
        int newSize = Math.max(40, current - change);
        if (newSize != current) {
            ParameterUtils.setParameter(dataSettings, "preview-size", newSize);
        }
    }

    private void increaseSize() {
        int current = dataSettings.getPreviewSize();
        int change = 10;
        int newSize = current + change;
        if (newSize != current) {
            ParameterUtils.setParameter(dataSettings, "preview-size", newSize);
        }
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        if ("preview-size".equals(event.getKey())) {
            refreshZoomStatus();
        }
    }
}
