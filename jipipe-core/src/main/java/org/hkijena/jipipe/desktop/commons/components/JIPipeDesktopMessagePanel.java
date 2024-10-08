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

import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panel that carries multiple user-friendly messages
 */
public class JIPipeDesktopMessagePanel extends JIPipeDesktopFormPanel {

    private final Set<String> existingMessages = new HashSet<>();

    public JIPipeDesktopMessagePanel() {
        super(null, NONE);
    }

    public Message addMessage(MessageType type, String message, boolean withCloseButton, boolean autoClose, Component... components) {
        if (!existingMessages.contains(message)) {
            Message instance = new Message(this, type, message, withCloseButton, autoClose, components);
            addWideToForm(instance, null);
            revalidate();
            repaint();
            existingMessages.add(message);
            return instance;
        }
        return null;
    }

    public Message addMessage(MessageType type, String message, boolean withCloseButton, boolean autoClose, List<Component> components) {
        return addMessage(type, message, withCloseButton, autoClose, components.toArray(new Component[0]));
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
        Info(new Color(0x8EBFEF), Color.WHITE),
        InfoLight(new Color(0xC1DFF9), new Color(0x444444)),
        Gray(new Color(0xE6E6E6), new Color(0x444444)),
        Success(new Color(0x5CB85C), Color.WHITE),
        Warning(new Color(0xffc155), new Color(0x444444)),
        Error(new Color(0xd7263b), Color.WHITE);

        private final Color background;
        private final Color foreground;


        MessageType(Color background, Color foreground) {
            this.background = background;
            this.foreground = foreground;
        }

        public Color getBackground() {
            return background;
        }

        public Color getForeground() {
            return foreground;
        }
    }

    public static class Message extends JPanel implements MouseListener {
        private final JIPipeDesktopMessagePanel parent;
        private final MessageType type;
        private final String text;
        private final boolean withCloseButton;
        private final boolean autoClose;
        private final Component[] components;

        public Message(JIPipeDesktopMessagePanel parent, MessageType type, String text, boolean withCloseButton, boolean autoClose, Component[] components) {
            this.parent = parent;
            this.type = type;
            this.text = text;
            this.withCloseButton = withCloseButton;
            this.autoClose = autoClose;
            this.components = components;
            this.setOpaque(false);
            initialize();
        }

        private void initialize() {
            setLayout(new BorderLayout());
            setBorder(new RoundedLineBorder(type.background.darker(), 1, 4));
            JTextArea messageTextArea = UIUtils.createReadonlyBorderlessTextArea(text);
            messageTextArea.setForeground(type.foreground);

            add(messageTextArea, BorderLayout.CENTER);
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
            for (Component actionButton : components) {
                if (actionButton != null) {
                    buttonPanel.add(Box.createHorizontalStrut(4));
                    buttonPanel.add(actionButton);
                    if (autoClose && actionButton instanceof AbstractButton) {
                        ((AbstractButton) actionButton).addActionListener(e -> closeMessage());
                    }
                }
            }
            if (withCloseButton) {
                JButton closeButton = new JButton(UIUtils.getIconFromResources("actions/close-tab.png"));
                UIUtils.makeButtonFlat25x25(closeButton);
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
