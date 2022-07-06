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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.display.tracks;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.features.FeatureUtils;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.UIUtils;
import org.jgrapht.graph.DefaultWeightedEdge;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class TrackListCellRenderer extends JPanel implements ListCellRenderer<Integer> {

    private final TracksManagerPlugin tracksManagerPlugin;
    private SolidColorIcon strokeFillPreview = new SolidColorIcon(16, 16);
    private JLabel iconLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JLabel infoLabel = new JLabel();

    private FeatureColorGenerator<DefaultWeightedEdge> strokeColorGenerator;

    public TrackListCellRenderer(TracksManagerPlugin tracksManagerPlugin) {
        this.tracksManagerPlugin = tracksManagerPlugin;
        initialize();
        updateColorMaps();
        tracksManagerPlugin.getDisplaySettings().listeners().add(this::updateColorMaps);
    }

    public void updateColorMaps() {
        if(tracksManagerPlugin.getTracksCollection() != null) {
            strokeColorGenerator = FeatureUtils.createTrackColorGenerator(tracksManagerPlugin.getTracksCollection().getModel(), tracksManagerPlugin.getDisplaySettings());
        }
    }

    private void initialize() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        iconLabel.setIcon(strokeFillPreview);
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

        nameLabel.setText(tracksManagerPlugin.getTracksCollection().getTracks().name(trackId));

        strokeFillPreview.setFillColor(Color.WHITE);
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
