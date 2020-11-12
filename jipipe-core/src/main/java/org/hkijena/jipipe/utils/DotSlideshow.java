package org.hkijena.jipipe.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class DotSlideshow extends JPanel {
    private final JPanel target = new JPanel();
    private ButtonGroup buttonGroup = new ButtonGroup();
    private BiMap<String, Component> slides = HashBiMap.create();
    private BiMap<String, JRadioButton> dots = HashBiMap.create();
    private List<String> slideOrder = new ArrayList<>();
    private JPanel bottomPanel = new JPanel();
    private String currentKey;

    public DotSlideshow() {
        setLayout(new BorderLayout());
        setOpaque(false);
        initialize();
        refreshDots();
    }

    private void initialize() {
        target.setLayout(new CardLayout());
        target.setOpaque(false);
        add(target, BorderLayout.CENTER);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setOpaque(false);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void addSlide(Component component, String title) {
        slides.put(title, component);
        slideOrder.add(title);
        target.add(component, title);
        refreshDots();
    }

    public void showSlide(String title) {
        currentKey = title;
        ((CardLayout) target.getLayout()).show(target, title);
        dots.get(title).setSelected(true);
    }

    private void refreshDots() {
        bottomPanel.removeAll();
        buttonGroup = new ButtonGroup();
        dots.clear();

        bottomPanel.add(Box.createHorizontalGlue());
        JButton previousButton = new JButton(UIUtils.getIconFromResources("actions/arrow-left.png"));
        UIUtils.makeFlat25x25(previousButton);
        previousButton.addActionListener(e -> previousSlide());
        bottomPanel.add(previousButton);
        for (String key : slideOrder) {
            JRadioButton radioButton = new JRadioButton("");
            radioButton.setIcon(UIUtils.getIconFromResources("actions/xfce-wm-unstick.png"));
            radioButton.setSelectedIcon(UIUtils.getIconFromResources("actions/xfce-wm-stick.png"));
            radioButton.setOpaque(false);
            buttonGroup.add(radioButton);
            radioButton.addActionListener(e -> {
                showSlide(key);
            });
            bottomPanel.add(radioButton);
            dots.put(key, radioButton);
        }
        JButton nextButton = new JButton(UIUtils.getIconFromResources("actions/arrow-right.png"));
        UIUtils.makeFlat25x25(nextButton);
        nextButton.addActionListener(e -> nextSlide());
        bottomPanel.add(nextButton);
        bottomPanel.add(Box.createHorizontalGlue());
        revalidate();
        repaint();
    }

    private void nextSlide() {
        if (currentKey != null && !slideOrder.isEmpty()) {
            int index = slideOrder.indexOf(currentKey);
            if (index == -1)
                return;
            if (index + 1 <= slideOrder.size() - 1)
                showSlide(slideOrder.get(index + 1));
            else
                showSlide(slideOrder.get(0));
        }
    }

    public void previousSlide() {
        if (currentKey != null && !slideOrder.isEmpty()) {
            int index = slideOrder.indexOf(currentKey);
            if (index == -1)
                return;
            if (index - 1 >= 0)
                showSlide(slideOrder.get(index - 1));
            else
                showSlide(slideOrder.get(slideOrder.size() - 1));
        }
    }
}
