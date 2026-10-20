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

package org.hkijena.jipipe.desktop.commons.nodepatterns;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.Random;

/** Jittered grid triangulation (edges); border points are snapped onto the edges so the mesh fills the whole area. */
public final class TrianglesFamily implements NodePatternFamily {
    @Override
    public void paint(Graphics2D g, int w, int h, Random rng) {
        int n = 6;
        double cell = w / n;
        double[][] px = new double[n + 1][n + 1], py = new double[n + 1][n + 1];
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++) {
                double jx = i == 0 || i == n ? 0 : (rng.nextDouble() - 0.5) * 0.7;
                double jy = j == 0 || j == n ? 0 : (rng.nextDouble() - 0.5) * 0.7;
                px[i][j] = (i + jx) * cell;
                py[i][j] = (j + jy) * cell;
            }
        // snap border points onto the edges so the mesh fills the whole area
        for (int i = 0; i <= n; i++)
            for (int j = 0; j <= n; j++) {
                if (i == 0) px[i][j] = 0; else if (i == n) px[i][j] = w;
                if (j == 0) py[i][j] = 0; else if (j == n) py[i][j] = h;
            }
        g.setStroke(new BasicStroke(1.3f));
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                double cx = (i + 0.5) * cell + (rng.nextDouble() - 0.5) * cell * 0.5;
                double cy = (j + 0.5) * cell + (rng.nextDouble() - 0.5) * cell * 0.5;
                double[][] c = {{px[i][j], py[i][j]}, {px[i + 1][j], py[i + 1][j]}, {px[i + 1][j + 1], py[i + 1][j + 1]}, {px[i][j + 1], py[i][j + 1]}};
                for (int k = 0; k < 4; k++)
                    g.draw(new Line2D.Double(c[k][0], c[k][1], cx, cy));
                for (int k = 0; k < 4; k++)
                    g.draw(new Line2D.Double(c[k][0], c[k][1], c[(k + 1) % 4][0], c[(k + 1) % 4][1]));
            }
    }
}
