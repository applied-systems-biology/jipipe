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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.ui.swing.updater.SwingTools;
import net.imagej.ui.swing.updater.UpdaterFrame;
import net.imagej.updater.Conflicts;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Copy of {@link ConflictDialog} because the original one expects an {@link UpdaterFrame}
 */
public abstract class ConflictDialog extends JDialog implements ActionListener {

    public JTextPane panel; // this is public for debugging purposes
    protected JPanel rootPanel;
    protected SimpleAttributeSet bold, indented, italic, normal, red;
    protected JButton ok, cancel;

    protected List<Conflicts.Conflict> conflictList;
    protected boolean wasCanceled;

    public ConflictDialog(Window owner, final String title) {
        super(owner, title);

        rootPanel = SwingTools.verticalPanel();
        setContentPane(rootPanel);

        panel = new JTextPane();
        panel.setEditable(false);

        bold = new SimpleAttributeSet();
        StyleConstants.setBold(bold, true);
        StyleConstants.setFontSize(bold, 16);
        indented = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(indented, 40);
        italic = new SimpleAttributeSet();
        StyleConstants.setItalic(italic, true);
        normal = new SimpleAttributeSet();
        red = new SimpleAttributeSet();
        StyleConstants.setForeground(red, Color.RED);

        SwingTools.scrollPane(panel, 650, 450, rootPanel);

        final JPanel buttons = new JPanel();
        ok = SwingTools.button("OK", "OK", this, buttons);
        cancel = SwingTools.button("Cancel", "Cancel", this, buttons);
        rootPanel.add(buttons);

        // do not show, right now
        pack();
        setModal(true);
        setLocationRelativeTo(owner);

        final int ctrl = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        SwingTools.addAccelerator(cancel, rootPanel, this, KeyEvent.VK_ESCAPE, 0);
        SwingTools.addAccelerator(cancel, rootPanel, this, KeyEvent.VK_W, ctrl);
        SwingTools.addAccelerator(ok, rootPanel, this, KeyEvent.VK_ENTER, 0);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                updateConflictList();
                if (conflictList.size() > 0)
                    wasCanceled = true;
            }
        });
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == cancel) {
            wasCanceled = true;
            dispose();
        } else if (e.getSource() == ok) {
            if (!ok.isEnabled()) return;
            dispose();
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        if (SwingUtilities.isEventDispatchThread()) super.setVisible(visible);
        else try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    setVisible(visible);
                }
            });
        } catch (final InterruptedException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public boolean resolve() {
        listIssues();

        if (panel.getDocument().getLength() > 0)
            setVisible(true);
        return !wasCanceled;
    }

    protected abstract void updateConflictList();

    protected void listIssues() {
        updateConflictList();
        panel.setText("");

        for (final Conflicts.Conflict conflict : conflictList) {
            maybeAddSeparator();
            newText(conflict.getSeverity().toString() + ": ", conflict.isError() ? red : normal);
            final String filename = conflict.getFilename();
            if (filename != null) addText(filename, bold);
            addText("\n" + conflict.getConflict());
            addText("\n");
            for (final Conflicts.Resolution resolution : conflict.getResolutions()) {
                addText("\n    ");
                addButton(resolution.getDescription(), new ActionListener() {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        resolution.resolve();
                        listIssues();
                    }
                });
            }
        }

        ok.setEnabled(!Conflicts.needsFeedback(conflictList));
        if (ok.isEnabled()) ok.requestFocus();

        if (isShowing()) {
            if (panel.getStyledDocument().getLength() == 0) addText(
                    "No more issues to be resolved!", italic);
            panel.setCaretPosition(0);
            panel.repaint();
        }
    }

    protected void addButton(final String label, final ActionListener listener) {
        final JButton button = SwingTools.button(label, null, listener, null);
        selectEnd();
        panel.insertComponent(button);
    }

    protected void selectEnd() {
        final int end = panel.getStyledDocument().getLength();
        panel.select(end, end);
    }

    protected void newText(final String message) {
        newText(message, normal);
    }

    protected void newText(final String message, final SimpleAttributeSet style) {
        if (panel.getStyledDocument().getLength() > 0) addText("\n\n");
        addText(message, style);
    }

    protected void addText(final String message) {
        addText(message, normal);
    }

    protected void addText(final String message, final SimpleAttributeSet style) {
        final int end = panel.getStyledDocument().getLength();
        try {
            panel.getStyledDocument().insertString(end, message, style);
        } catch (final BadLocationException e) {
            e.printStackTrace();
        }
    }

    protected void maybeAddSeparator() {
        if (panel.getText().isEmpty() && panel.getComponents().length == 0) return;
        addText("\n");
        selectEnd();
        panel.insertComponent(new JSeparator());
    }

}
