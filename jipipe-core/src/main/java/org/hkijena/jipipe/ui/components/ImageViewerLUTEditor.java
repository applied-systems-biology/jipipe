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
 */

package org.hkijena.jipipe.ui.components;

import ij.ImagePlus;
import ij.process.LUT;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.color.GradientThumbRenderer;
import org.jdesktop.swingx.color.GradientTrackRenderer;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.ThumbListener;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Based on {@link org.jdesktop.swingx.JXGradientChooser}
 */
public class ImageViewerLUTEditor extends JPanel implements ThumbListener {
    private final ImageViewerPanel imageViewerPanel;
    private final int targetChannel;
    /**
     * The multi-thumb slider to use for the gradient stops
     */
    private JXMultiThumbSlider<Color> slider;
    private JButton deleteThumbButton;
    private JButton addThumbButton;
    private JButton changeColorButton;
    private JButton invertColorsButton;
    private ColorIcon changeColorButtonDisplayedColor = new ColorIcon(16,16);
    private boolean isUpdating = false;
    private String channelName;
    private LUT cachedLUT;

    public ImageViewerLUTEditor(ImageViewerPanel imageViewerPanel, int targetChannel) {
        this.imageViewerPanel = imageViewerPanel;
        this.targetChannel = targetChannel;
        this.channelName = "Channel " + (targetChannel + 1);
        initialize();
    }

    public void loadLUTFromImage() {
        ImagePlus image = imageViewerPanel.getImage();
        if(image != null) {
            if(targetChannel < image.getLuts().length) {
                importLUT(image.getLuts()[targetChannel]);
            }
        }
    }

    public void importLUT(LUT lut) {
        isUpdating = true;
        Color black = new Color(lut.getRGB(0));
        Color white = new Color(lut.getRGB(255));
        while(slider.getModel().getThumbCount() > 2) {
            slider.getModel().removeThumb(0);
        }
        slider.getModel().getThumbAt(0).setPosition(0);
        slider.getModel().getThumbAt(1).setPosition(1);
        slider.getModel().getThumbAt(0).setObject(black);
        slider.getModel().getThumbAt(1).setObject(white);
        isUpdating = false;
    }

    public LUT getLUT() {
        if(cachedLUT == null)
            cachedLUT = generateLUT();
        return cachedLUT;
    }

    private LUT generateLUT() {
        List<Thumb<Color>> stops = this.slider.getModel().getSortedThumbs();
        int len = stops.size();
        float[] fractions = new float[len];
        Color[] colors = new Color[len];

        for (int i = 0; i < stops.size(); i++) {
            Thumb<Color> thumb = stops.get(i);
            colors[i] = thumb.getObject();
            fractions[i] = thumb.getPosition();
        }
        MultipleGradientPaint paint = new LinearGradientPaint(0, 0, 256, 256, fractions, colors);
        BufferedImage img = new BufferedImage(256,1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) img.getGraphics();
        graphics.setPaint(paint);
        graphics.fillRect(0,0,256,1);
        byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256];
        for (int i = 0; i < 256; i++) {
            int rgb = img.getRGB(i, 0);
            Color color = new Color(rgb);
            reds[i] = (byte)color.getRed();
            greens[i] = (byte)color.getGreen();
            blues[i] = (byte)color.getBlue();
        }
        return new LUT(reds, greens, blues);
    }

    private void updateFromStop(int thumb, Color color) {
        if(thumb == -1) {
            changeColorButton.setEnabled(false);
            changeColorButtonDisplayedColor.setFillColor(Color.black);
            changeColorButton.repaint();
            deleteThumbButton.setEnabled(false);
        } else {
            changeColorButton.setEnabled(true);
            changeColorButton.repaint();
            changeColorButtonDisplayedColor.setFillColor(color);
            deleteThumbButton.setEnabled(true);
        }
        updateDeleteButtons();
    }

    private void updateDeleteButtons() {
        if(slider.getModel().getThumbCount() <= 2) {
            deleteThumbButton.setEnabled(false);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());
        slider = new JXMultiThumbSlider<>();
        slider.getModel().addThumb(0, Color.BLACK);
        slider.getModel().addThumb(1, Color.WHITE);
        slider.setTrackRenderer(new GradientTrackRenderer());
        slider.setThumbRenderer(new GradientThumbRenderer());
        slider.setPreferredSize(new Dimension(100,35));
        slider.addMultiThumbListener(this);
        changeColorButton = new JButton(changeColorButtonDisplayedColor);
        addThumbButton = new JButton(UIUtils.getIconFromResources("actions/color-add.png"));
        deleteThumbButton = new JButton(UIUtils.getIconFromResources("actions/color-remove.png"));
        invertColorsButton = new JButton(UIUtils.getIconFromResources("actions/object-inverse.png"));
        UIUtils.makeFlat25x25(addThumbButton);
        UIUtils.makeFlat25x25(changeColorButton);
        UIUtils.makeFlat25x25(deleteThumbButton);
        UIUtils.makeFlat25x25(invertColorsButton);

        setLayout(new BorderLayout());
        add(slider, BorderLayout.CENTER);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(12,4,0,0));
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.X_AXIS));
        settingsPanel.add(changeColorButton);
        settingsPanel.add(addThumbButton);
        settingsPanel.add(deleteThumbButton);
        settingsPanel.add(invertColorsButton);
        add(settingsPanel, BorderLayout.EAST);

        addThumbButton.addActionListener(e -> addColor());
        deleteThumbButton.addActionListener(e -> removeColor());
        changeColorButton.addActionListener(e -> changeColor());
        invertColorsButton.addActionListener(e -> invertColors());
    }

    private void invertColors() {
        isUpdating = true;
        List<Color> current = new ArrayList<>();
        for (int i = 0; i < slider.getModel().getThumbCount(); i++) {
            Thumb<Color> thumb = slider.getModel().getThumbAt(i);
            current.add(thumb.getObject());
        }
        for (int i = 0; i < slider.getModel().getThumbCount(); i++) {
            Thumb<Color> thumb = slider.getModel().getThumbAt(i);
            thumb.setObject(current.get(current.size() - i - 1));
        }
        isUpdating = false;
        cachedLUT = null;
        applyLUT();
    }

    private void changeColor() {
        int index = slider.getSelectedIndex();
        if (index >= 0) {
            Color color = slider.getModel().getThumbAt(index).getObject();
            color = JColorChooser.showDialog(this, "Select color", color);
            if(color != null) {
                slider.getModel().getThumbAt(index).setObject(color);
                updateFromStop(index, color);
                cachedLUT = null;
                if(!isUpdating) {
                    applyLUT();
                }
            }
        }
    }

    private void removeColor() {
        if(slider.getModel().getThumbCount() <= 2)
            return;
        int index = slider.getSelectedIndex();
        if (index >= 0) {
            slider.getModel().removeThumb(index);
            updateFromStop(-1, null);
            cachedLUT = null;
        }
    }

    private void addColor() {
        float pos = 0.2f;
        Color color = Color.black;
        int num = slider.getModel().addThumb(pos,color);
        cachedLUT = null;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        Color color = slider.getModel().getThumbAt(thumb).getObject();
        updateFromStop(thumb, color);
        updateDeleteButtons();
        cachedLUT = null;
        if(!isUpdating) {
            applyLUT();
        }
    }

    @Override
    public void thumbSelected(int thumb) {
        if(thumb == -1) {
            updateFromStop(-1, Color.black);
            return;
        }
        float pos = slider.getModel().getThumbAt(thumb).getPosition();
        Color color = slider.getModel().getThumbAt(thumb).getObject();
        updateFromStop(thumb, color);
        updateDeleteButtons();
        slider.repaint();
    }

    @Override
    public void mousePressed(MouseEvent evt) {
        if(evt.getClickCount() > 1) {
            changeColor();
        }
    }

    public void applyLUT() {
        ImagePlus image = imageViewerPanel.getImage();
        if(image != null) {
            if(targetChannel < image.getNChannels()) {
                int z = image.getZ();
                int c = image.getC();
                int t = image.getT();
                image.setPosition(targetChannel + 1, 1, 1);
                image.setLut(getLUT());
                image.setPosition(c, z, t);
                if(c == targetChannel + 1) {
                    imageViewerPanel.uploadSliceToCanvas();
                }
            }
        }
    }
}
