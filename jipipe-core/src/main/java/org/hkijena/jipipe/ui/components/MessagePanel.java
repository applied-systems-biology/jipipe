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

import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Set;

/**
 * Panel that carries multiple user-friendly messages
 */
public class MessagePanel extends FormPanel {

    private final Set<String> existingMessages = new HashSet<>();

    public MessagePanel() {
        super(null, FormPanel.NONE);
    }

    public Message addMessage(MessageType type, String message, boolean withCloseButton, boolean autoClose, JButton... actionButtons) {
        if (!existingMessages.contains(message)) {
            Message instance = new Message(this, type, message, withCloseButton, autoClose, actionButtons);
            addWideToForm(instance, null);
            revalidate();
            repaint();
            existingMessages.add(message);
            return instance;
        }
        return null;
    }

    public void removeMessage(Message message) {
        getContentPanel().remove(message);
        existingMessages.remove(message.text);
        revalidate();
        repaint();
    }

    @Override
    public void clear() {
        super.clear();
        existingMessages.clear();
    }

    public enum MessageType {
        Info(new Color(0x65a4e3), Color.WHITE),
        Success(new Color(0x5CB85C), Color.WHITE),
        Warning(new Color(0xffc155), Color.DARK_GRAY),
        Error(new Color(0xd7263b), Color.WHITE);

        private final Color background;
        private final Color foreground;


        MessageType(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }
    }

    public static class Message extends JPanel implements MouseListener {
        private final MessagePanel parent;
        private final MessageType type;
        private final String text;
        private final boolean withCloseButton;
        private final boolean autoClose;
        private final JButton[] actionButtons;

        public Message(MessagePanel parent, MessageType type, String text, boolean withCloseButton, boolean autoClose, JButton[] actionButtons) {
            this.parent = parent;
            this.type = type;
            this.text = text;
            this.withCloseButton = withCloseButton;
            this.autoClose = autoClose;
            this.actionButtons = actionButtons;
            this.setOpaque(false);
            initialize();
        }

        private void initialize() {
            setLayout(new BorderLayout());
            setBorder(new RoundedLineBorder(type.background.darker(), 1, 4));
            JTextArea messageTextArea = UIUtils.makeReadonlyBorderlessTextArea(text);
            messageTextArea.setForeground(type.foreground);

            add(messageTextArea, BorderLayout.CENTER);
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            for (JButton actionButton : actionButtons) {
                if (actionButton != null) {
                    buttonPanel.add(Box.createHorizontalStrut(8));
                    buttonPanel.add(actionButton);
                    if(autoClose) {
                        actionButton.addActionListener(e -> closeMessage());
                    }
                }
            }
            if(withCloseButton) {
                JButton closeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
                UIUtils.makeFlat25x25(closeButton);
                closeButton.addActionListener(e -> {
                    closeMessage();
                });
                buttonPanel.add(Box.createHorizontalStrut(8));
                buttonPanel.add(closeButton);
            }

            add(buttonPanel, BorderLayout.EAST);
        }

        public void closeMessage() {
            parent.getContentPanel().remove(this);
            parent.existingMessages.remove(text);
            parent.revalidate();
            parent.repaint();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setPaint(type.background);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

            super.paint(g);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON2) {
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
}
