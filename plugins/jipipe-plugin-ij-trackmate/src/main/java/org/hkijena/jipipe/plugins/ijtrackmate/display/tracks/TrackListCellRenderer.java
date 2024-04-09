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

package org.hkijena.jipipe.plugins.ijtrackmate.display.tracks;

import org.hkijena.jipipe.plugins.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class TrackListCellRenderer extends JPanel implements ListCellRenderer<Integer> {

    private final TracksManagerPlugin2D tracksManagerPlugin;
    //    private SolidColorIcon strokeFillPreview = new SolidColorIcon(16, 16);
    private JLabel iconLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JLabel infoLabel = new JLabel();

    public TrackListCellRenderer(TracksManagerPlugin2D tracksManagerPlugin) {
        this.tracksManagerPlugin = tracksManagerPlugin;
        initialize();
        updateColorMaps();
    }

    public void updateColorMaps() {
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        iconLabel.setIcon(TrackMatePlugin.RESOURCES.getIconFromResources("trackscheme.png"));
        infoLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        add(iconLabel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(nameLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 0;
                weightx = 1;
                anchor = WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
        add(infoLabel, new GridBagConstraints() {
            {
                gridx = 1;
                gridy = 1;
                weightx = 1;
                anchor = WEST;
                insets = UIUtils.UI_PADDING;
            }
        });
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Integer> list, Integer trackId, int index, boolean isSelected, boolean cellHasFocus) {

        nameLabel.setText(tracksManagerPlugin.getTracksCollection().getTrackModel().name(trackId));

//        strokeFillPreview.setFillColor(Color.WHITE);
        iconLabel.setText("" + index);
        infoLabel.setText(tracksManagerPlugin.getTracksCollection().getTrackSpots(trackId).size() + " spots");

        if (isSelected) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("List.background"));
        }
        return this;
    }
}
