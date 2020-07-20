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

import org.hkijena.jipipe.utils.RoundedLineBorder;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Panel that carries multiple user-friendly messages
 */
public class MessagePanel extends FormPanel {

    private Set<String> existingMessages = new HashSet<>();

    public MessagePanel() {
        super(null, FormPanel.NONE);
    }

    public void addMessage(MessageType type, String message, JButton actionButton) {
        if(!existingMessages.contains(message)) {
            addWideToForm(new Message(this, type, message, actionButton), null);
            revalidate();
            repaint();
            existingMessages.add(message);
        }
    }

    @Override
    public void clear() {
        super.clear();
        existingMessages.clear();
    }

    public static class Message extends JPanel implements MouseListener {
        private final MessagePanel parent;
        private final MessageType type;
        private final String text;
        private final JButton actionButton;

        public Message(MessagePanel parent, MessageType type, String text, JButton actionButton) {
            this.parent = parent;
            this.type = type;
            this.text = text;
            this.actionButton = actionButton;
            this.setOpaque(false);
            initialize();
        }

        private void initialize() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setBorder(new RoundedLineBorder(type.background.darker(), 1, 4));
            JTextArea messageTextArea = UIUtils.makeReadonlyBorderlessTextArea(text);
            messageTextArea.setForeground(type.foreground);

            add(messageTextArea);
            add(Box.createHorizontalGlue());
            if(actionButton != null) {
                add(actionButton);
                add(Box.createHorizontalStrut(8));
                actionButton.addActionListener(e -> closeMessage());
            }

            JButton closeButton = new JButton(UIUtils.getIconFromResources("close-tab.png"));
            UIUtils.makeFlat25x25(closeButton);
            closeButton.addActionListener(e -> {
                closeMessage();
            });
            add(closeButton);
        }

        public void closeMessage() {
            parent.getContentPanel().remove(this);
            parent.existingMessages.remove(text);
            parent.revalidate();
            parent.repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setPaint(type.background);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

            super.paint(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getButton() == MouseEvent.BUTTON2) {
                closeMessage();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }

    public enum MessageType {
        Info(new Color(0x65a4e3), Color.WHITE),
        Warning(new Color(0xffc155), Color.DARK_GRAY),
        Error(new Color(0xd7263b), Color.WHITE);

        private final Color background;
        private final Color foreground;


        MessageType(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }
    }
}
