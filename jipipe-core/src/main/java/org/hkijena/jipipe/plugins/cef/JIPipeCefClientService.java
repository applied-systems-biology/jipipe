package org.hkijena.jipipe.plugins.cef;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

/**
 * A service component that takes care of lazily initializing a {@link org.cef.CefClient} if available and
 * automatically destroying it when the parent window is destroyed
 */
public class JIPipeCefClientService implements WindowListener {
    private static final Logger logger = Logger.getLogger(JIPipeCefClientService.class.getName());
    private final Window window;
    private CefClient client;
    private boolean closed = false;

    public JIPipeCefClientService(Window window) {
        this.window = window;
        window.addWindowListener(this);
    }

    public boolean isCefAvailable() {
        return CefPlugin.hasCef();
    }

    public CefClient getClient() {
        if (!isCefAvailable() || closed) {
            return null;
        }
        if (client == null) {
            client = CefPlugin.getApp().createClient();
        }
        return client;
    }

    public CefBrowser createBrowser() {
        CefClient client = getClient();
        if (client == null) {
            return null;
        }
        return client.createBrowser("about:blank", false, false);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {

    }

    @Override
    public void windowClosed(WindowEvent e) {
        try {
            closed = true;
            if (client != null) {
                client.dispose();
            }
            client = null;
        } catch (Exception ex) {
            logger.severe(ex.getMessage());
        }
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }

    public Window getWindow() {
        return window;
    }
}
