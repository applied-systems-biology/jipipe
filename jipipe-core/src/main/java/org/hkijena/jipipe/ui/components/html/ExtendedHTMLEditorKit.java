package org.hkijena.jipipe.ui.components.html;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * {@link HTMLEditorKit}
 */
public class ExtendedHTMLEditorKit extends HTMLEditorKit {
    private static HTMLFactory factory = null;
    private HyperlinkHoverLinkController handler= new HyperlinkHoverLinkController();

    public ExtendedHTMLEditorKit() {
        super();
        setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

    @Override
    public void install(JEditorPane c) {
        MouseListener[] oldMouseListeners=c.getMouseListeners();
        MouseMotionListener[] oldMouseMotionListeners=c.getMouseMotionListeners();
        super.install(c);
        //the following code removes link handler added by original
        //HTMLEditorKit

        for (MouseListener l: c.getMouseListeners()) {
            c.removeMouseListener(l);
        }
        for (MouseListener l: oldMouseListeners) {
            c.addMouseListener(l);
        }

        for (MouseMotionListener l: c.getMouseMotionListeners()) {
            c.removeMouseMotionListener(l);
        }
        for (MouseMotionListener l: oldMouseMotionListeners) {
            c.addMouseMotionListener(l);
        }

        //add out link handler instead of removed one
        c.addMouseListener(handler);
        c.addMouseMotionListener(handler);
    }

    @Override
    public ViewFactory getViewFactory() {
        if (factory == null) {
            factory = new HTMLFactory() {

                @Override
                public View create(Element elem) {
                    AttributeSet attrs = elem.getAttributes();
                    Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
                    Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
                    if (o instanceof HTML.Tag) {
                        HTML.Tag kind = (HTML.Tag) o;
                        if (kind == HTML.Tag.IMG) {
                            // HERE is the call to the special class...
                            return new ExtendedImageView(elem);
                        }
                    }
                    return super.create(elem);
                }
            };
        }
        return factory;
    }

    public static class HyperlinkHoverLinkController extends LinkController {

        public void mouseClicked(MouseEvent e) {
            JEditorPane editor = (JEditorPane) e.getSource();

            if (editor.isEditable() && SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount()==2) {
                    editor.setEditable(false);
                    super.mouseClicked(e);
                    editor.setEditable(true);
                }
            }

        }
        public void mouseMoved(MouseEvent e) {
            JEditorPane editor = (JEditorPane) e.getSource();

            if (editor.isEditable()) {
//                isNeedCursorChange=false;
                editor.setEditable(false);
//                isNeedCursorChange=true;
                super.mouseMoved(e);
//                isNeedCursorChange=false;
                editor.setEditable(true);
//                isNeedCursorChange=true;
            }
        }

    }
}
