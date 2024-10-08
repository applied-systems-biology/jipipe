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

package org.hkijena.jipipe.desktop.commons.components.markup;

import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

/**
 * {@link HTMLEditorKit}
 */
public class JIPipeDesktopHTMLEditorKit extends HTMLEditorKit {
    private static HTMLFactory factory = null;
//    private final HyperlinkHoverLinkController handler = new HyperlinkHoverLinkController();

    public JIPipeDesktopHTMLEditorKit() {
        super();
        setDefaultCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    }

//    @Override
//    public void install(JEditorPane c) {
//        MouseListener[] oldMouseListeners = c.getMouseListeners();
//        MouseMotionListener[] oldMouseMotionListeners = c.getMouseMotionListeners();
//        super.install(c);
//        //the following code removes link handler added by original
//        //HTMLEditorKit
//
//        for (MouseListener l : c.getMouseListeners()) {
//            c.removeMouseListener(l);
//        }
//        for (MouseListener l : oldMouseListeners) {
//            c.addMouseListener(l);
//        }
//
//        for (MouseMotionListener l : c.getMouseMotionListeners()) {
//            c.removeMouseMotionListener(l);
//        }
//        for (MouseMotionListener l : oldMouseMotionListeners) {
//            c.addMouseMotionListener(l);
//        }
//
//        //add out link handler instead of removed one
//        c.addMouseListener(handler);
//        c.addMouseMotionListener(handler);
//    }

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
                            return new JIPipeDesktopExtendedImageView(elem);
                        }
                    }
                    return super.create(elem);
                }
            };
        }
        return factory;
    }

//    public static class HyperlinkHoverLinkController extends LinkController {
//
//        public void mouseClicked(MouseEvent e) {
//            JEditorPane editor = (JEditorPane) e.getSource();
//
//            if (editor.isEditable() && SwingUtilities.isLeftMouseButton(e)) {
//                if (e.getClickCount() == 2) {
//                    editor.setEditable(false);
//                    super.mouseClicked(e);
//                    editor.setEditable(true);
//                }
//            }
//
//        }
//
//        public void mouseMoved(MouseEvent e) {
//            JEditorPane editor = (JEditorPane) e.getSource();
//
//            if (editor.isEditable()) {
////                isNeedCursorChange=false;
//                editor.setEditable(false);
////                isNeedCursorChange=true;
//                super.mouseMoved(e);
////                isNeedCursorChange=false;
//                editor.setEditable(true);
////                isNeedCursorChange=true;
//            }
//        }
//
//    }
}
