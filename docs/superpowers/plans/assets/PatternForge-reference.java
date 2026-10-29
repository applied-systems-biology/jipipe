import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Throwaway prototype: candidate framework-pattern families rendered in Java2D.
 * Every family must FILL the whole area edge-to-edge (no centered motifs).
 * Color-neutral: single white ink on transparent, antialiased.
 */
public class PatternForge {

    // deterministic seed from a string (stable across JVMs)
    static Random seedOf(String s) {
        return new Random(s.hashCode());
    }

    /**
     * The family registry (shared with the node-wall preview).
     */
    public static Map<String, Family> families() {
        Map<String, Family> F = new LinkedHashMap<>();

        // ------------------------------------------------------------- families
        // 1. stripes: wavy vertical bands (filled strips); the two frame-adjacent
        //    strips are straight so the left/right borders always carry ink
        F.put("stripes", (g, w, h, rng) -> {
            int n = 5 + rng.nextInt(3);
            double bw = w / (double) n;
            double stripW = bw * 0.55;
            for (int i = 0; i <= n; i++) {
                boolean frame = (i == 0) || (i == n);
                double x0 = i * bw - stripW / 2;
                double amp = frame ? 0 : stripW * (0.35 + rng.nextDouble() * 0.5);
                double k = 1 + rng.nextInt(2);
                double ph = rng.nextDouble() * Math.PI * 2;
                Path2D p = new Path2D.Double();
                for (int y = -2; y <= h + 2; y += 2) {
                    double x = x0 + amp * Math.sin(y / h * Math.PI * 2 * k + ph);
                    if (y == -2) p.moveTo(x, y);
                    else p.lineTo(x, y);
                }
                for (int y = h + 2; y >= -2; y -= 2) {
                    double x = x0 + stripW + amp * Math.sin(y / h * Math.PI * 2 * k + ph + 0.6);
                    p.lineTo(x, y);
                }
                p.closePath();
                g.fill(p);
            }
        });

        // 2. waves: isocurves of a periodic smooth field (marching squares)
        F.put("waves", (g, w, h, rng) -> {
            int[] fx = new int[4];
            double[] amp = new double[4];
            double[] ph = new double[4];
            for (int i = 0; i < 4; i++) {
                fx[i] = 1 + rng.nextInt(3);
                amp[i] = 0.4 + rng.nextDouble() * 0.6;
                ph[i] = rng.nextDouble() * Math.PI * 2;
            }
            double[][] fy = new double[4][2];
            for (int i = 0; i < 4; i++) {
                fy[i][0] = 1 + rng.nextInt(3);
                fy[i][1] = rng.nextDouble() * Math.PI * 2;
            }
            int step = 4;
            double[] L = {-0.55, -0.18, 0.18, 0.55};
            for (double lv : L) {
                for (int gy = 0; gy < h - step; gy += step)
                    for (int gx = 0; gx < w - step; gx += step) {
                        double[] v = {
                                field(gx, gy, fx, fy, amp, ph, w, h), field(gx + step, gy, fx, fy, amp, ph, w, h),
                                field(gx + step, gy + step, fx, fy, amp, ph, w, h), field(gx, gy + step, fx, fy, amp, ph, w, h)};
                        double[] a = {gx, gy}, b = {gx + step, gy}, c = {gx + step, gy + step}, d = {gx, gy + step};
                        for (int e = 0; e < 4; e++) {
                            double v1 = v[e], v2 = v[(e + 1) % 4];
                            if ((v1 - lv) * (v2 - lv) <= 0 && v1 != v2) {
                                double t = (lv - v1) / (v2 - v1);
                                double[] p1 = {a[e * 2] + t * step, a[e * 2 + 1] + t * step};
                                // edge from corner e to corner e+1
                                double[] c1 = e == 0 ? a : e == 1 ? b : e == 2 ? c : d;
                                double[] c2 = e == 0 ? b : e == 1 ? c : e == 2 ? d : a;
                                double[] p2 = {c1[0] + t * (c2[0] - c1[0]), c1[1] + t * (c2[1] - c1[1])};
                                // collect both crossings then draw
                                if (e == 0) {
                                    double[] p3 = {a[0], a[1]};
                                    // store in thread-unsafe trick: just draw segment if next edge also crosses
                                }
                                // simpler: accumulate crossings per cell below
                            }
                        }
                    }
                // NOTE: proper marching squares done in second pass (see wavesDraw)
            }
        });

        // proper marching-squares waves (replaces the above placeholder)
        F.put("waves", (g, w, h, rng) -> {
            int n = 3;
            int[] fx = new int[n], fyy = new int[n];
            double[] amp = new double[n], ph = new double[n];
            for (int i = 0; i < n; i++) {
                fx[i] = 1 + rng.nextInt(3);
                fyy[i] = 1 + rng.nextInt(3);
                amp[i] = 0.5 + rng.nextDouble() * 0.7;
                ph[i] = rng.nextDouble() * Math.PI * 2;
            }
            int step = 4;
            for (double lv : new double[]{-0.5, 0.0, 0.5}) {
                BasicStroke bs = new BasicStroke(1.6f);
                g.setStroke(bs);
                for (int gy = 0; gy < h - step; gy += step)
                    for (int gx = 0; gx < w - step; gx += step) {
                        double v00 = f(gx, gy, fx, fyy, amp, ph, w, h);
                        double v10 = f(gx + step, gy, fx, fyy, amp, ph, w, h);
                        double v11 = f(gx + step, gy + step, fx, fyy, amp, ph, w, h);
                        double v01 = f(gx, gy + step, fx, fyy, amp, ph, w, h);
                        double[][] segs = crossSegs(gx, gy, step, v00, v10, v11, v01, lv);
                        if (segs != null && segs.length == 2) {
                            g.draw(new Line2D.Double(segs[0][0], segs[0][1], segs[1][0], segs[1][1]));
                        } else if (segs != null && segs.length == 4) {
                            g.draw(new Line2D.Double(segs[0][0], segs[0][1], segs[1][0], segs[1][1]));
                            g.draw(new Line2D.Double(segs[2][0], segs[2][1], segs[3][0], segs[3][1]));
                        }
                    }
            }
        });

        // 3. weave: over/under bands
        F.put("weave", (g, w, h, rng) -> {
            int n = 4 + rng.nextInt(2);
            double bw = Math.min(w, h) / n * (0.5 + rng.nextDouble() * 0.3);
            double pitch = Math.max(w, h) / n;
            // horizontal bands
            for (int i = 0; i < n + 1; i++) {
                double y = i * pitch - pitch / 2;
                fillBandH(g, w, h, y, bw);
            }
            // vertical bands (over where (i+j) even)
            for (int j = 0; j < n + 1; j++) {
                double x = j * pitch - pitch / 2;
                for (int i = 0; i < n + 1; i++) {
                    if ((i + j) % 2 == 0) {
                        double y = i * pitch - pitch / 2;
                        double gap = bw * 0.28;
                        g.fill(new RoundRectangle2D.Double(x, y - pitch / 2 + gap, bw, pitch - 2 * gap, bw * 0.6, bw * 0.6));
                    }
                }
            }
        });

        // 4. patchwork: recursive subdivision, alternating fills
        F.put("patchwork", (g, w, h, rng) -> {
            subdivide(g, rng, 0, 0, w, h, 4, true);
        });

        // 5. loops: scattered open loop motifs on a jittered grid
        F.put("loops", (g, w, h, rng) -> {
            int n = 6;
            double cell = w / n;
            g.setStroke(new BasicStroke((float) Math.max(1.4, cell * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -1; i <= n; i++)
                for (int j = -1; j <= n; j++) {
                    double cx = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                    double cy = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                    double r = cell * (0.28 + rng.nextDouble() * 0.2);
                    double a0 = rng.nextDouble() * Math.PI * 2;
                    double sweep = Math.PI * (1.2 + rng.nextDouble() * 1.2);
                    Path2D p = new Path2D.Double();
                    p.append(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, toDeg(a0), toDeg(sweep), Arc2D.OPEN), false);
                    g.draw(p);
                }
        });

        // 6. disco: confetti polygons on a jittered grid
        F.put("disco", (g, w, h, rng) -> {
            int n = 7;
            double cell = w / n;
            for (int i = -1; i <= n; i++)
                for (int j = -1; j <= n; j++) {
                    // shapes may clip at the frame -> the pattern continues beyond the tile
                    double cx = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                    double cy = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.5) * cell;
                    double r = cell * (0.30 + rng.nextDouble() * 0.30);
                    int sides = 3 + rng.nextInt(3);
                    double rot = rng.nextDouble() * Math.PI;
                    Path2D p = new Path2D.Double();
                    for (int s = 0; s < sides; s++) {
                        double a = rot + s * 2 * Math.PI / sides;
                        double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
                        if (s == 0) p.moveTo(x, y);
                        else p.lineTo(x, y);
                    }
                    p.closePath();
                    if (rng.nextDouble() < 0.5) g.fill(p);
                    else g.draw(p);
                }
        });

        // 7. triangles: jittered grid triangulation (edges)
        F.put("triangles", (g, w, h, rng) -> {
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
                    if (i == 0) px[i][j] = 0;
                    else if (i == n) px[i][j] = w;
                    if (j == 0) py[i][j] = 0;
                    else if (j == n) py[i][j] = h;
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
        });

        // 8. landscape: stacked wavy strata (KDE 'Nuvole'-like bands); each
        //    band is the region between two consecutive curves, so no alpha
        //    stacking; the top band starts at y=0 and the bottom at y=h
        F.put("landscape", (g, w, h, rng) -> {
            int layers = 5 + rng.nextInt(2);
            double[] amp = new double[layers];
            double[] k = new double[layers];
            double[] ph = new double[layers];
            double[] yb = new double[layers];
            for (int L = 0; L < layers; L++) {
                yb[L] = -h * 0.04 + h * (L + 0.15) / layers;
                amp[L] = h * (0.025 + rng.nextDouble() * 0.045);
                k[L] = 1 + rng.nextInt(2);
                ph[L] = rng.nextDouble() * Math.PI * 2;
            }
            // top cap: from the frame top down to curve 0 (dark band)
            {
                Path2D p = new Path2D.Double();
                p.moveTo(0, -2);
                p.lineTo(w, -2);
                for (int x = w; x >= 0; x -= 4) p.lineTo(x, curveY(x, yb[0], amp[0], k[0], ph[0], w));
                p.closePath();
                g.setColor(new Color(255, 255, 255, 52));
                g.fill(p);
                g.setColor(Color.WHITE);
            }
            for (int L = 0; L < layers; L++) {
                // band between curve L and curve L+1 (or the bottom edge)
                Path2D p = new Path2D.Double();
                p.moveTo(0, curveY(0, yb[L], amp[L], k[L], ph[L], w));
                for (int x = 0; x <= w; x += 4) p.lineTo(x, curveY(x, yb[L], amp[L], k[L], ph[L], w));
                if (L + 1 < layers) {
                    for (int x = w; x >= 0; x -= 4)
                        p.lineTo(x, curveY(x, yb[L + 1], amp[L + 1], k[L + 1], ph[L + 1], w));
                } else {
                    p.lineTo(w, h);
                    p.lineTo(0, h);
                }
                p.closePath();
                int alpha = 210 - (layers - 1 - L) * (170 / layers);
                g.setColor(new Color(255, 255, 255, alpha));
                g.fill(p);
                g.setColor(Color.WHITE);
            }
        });

        // 9. blobs: scattered smooth filled blobs (superellipse + noise)
        F.put("blobs", (g, w, h, rng) -> {
            int n = 8;
            for (int i = 0; i < n; i++) {
                // centers may overshoot the frame -> blobs clip at the borders
                double cx = (rng.nextDouble() * 1.3 - 0.15) * w;
                double cy = (rng.nextDouble() * 1.3 - 0.15) * h;
                double r = (w / 5) * (0.5 + rng.nextDouble() * 0.8);
                double[] rr = new double[12];
                for (int k = 0; k < 12; k++) rr[k] = r * (0.72 + rng.nextDouble() * 0.5);
                double rot = rng.nextDouble() * Math.PI * 2;
                double[][] pts = new double[12][2];
                for (int k = 0; k < 12; k++) {
                    double a = rot + k * 2 * Math.PI / 12;
                    pts[k][0] = cx + rr[k] * Math.cos(a);
                    pts[k][1] = cy + rr[k] * Math.sin(a);
                }
                Path2D p = new Path2D.Double();
                double[] mx = new double[12], my = new double[12];
                for (int k = 0; k < 12; k++) {
                    mx[k] = (pts[k][0] + pts[(k + 1) % 12][0]) / 2;
                    my[k] = (pts[k][1] + pts[(k + 1) % 12][1]) / 2;
                }
                p.moveTo(mx[0], my[0]);
                for (int k = 1; k <= 12; k++) {
                    int k2 = k % 12;
                    p.quadTo(pts[k2][0], pts[k2][1], mx[k2], my[k2]);
                }
                p.closePath();
                g.fill(p);
            }
        });

        // 10. net: kNN graph (Vanta 'net')
        F.put("net", (g, w, h, rng) -> {
            int side = 7;
            int n = (side + 2) * (side + 2);
            double[] px = new double[n], py = new double[n];
            // jittered grid for even coverage; the -1..side overshoot puts points
            // across all four borders so edges cross the frame (plane-filling)
            int c = 0;
            for (int i = -1; i <= side; i++)
                for (int j = -1; j <= side; j++, c++) {
                    px[c] = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.8) * w / side;
                    py[c] = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.8) * h / side;
                }
            double maxD = w / side * 2.4;
            g.setStroke(new BasicStroke(1.1f));
            for (int i = 0; i < n; i++) {
                int[] near = nearest(px[i], py[i], px, py, n, 3, maxD);
                for (int j : near) {
                    if (j > i) g.draw(new Line2D.Double(px[i], py[i], px[j], py[j]));
                }
                g.fill(new Ellipse2D.Double(px[i] - 2.2, py[i] - 2.2, 4.4, 4.4));
            }
        });

        // 11. cells: voronoi membranes (Vanta 'cells')
        F.put("cells", (g, w, h, rng) -> {
            int side = 6;
            double[] sx = new double[side * side], sy = new double[side * side];
            for (int i = 0; i < side; i++)
                for (int j = 0; j < side; j++) {
                    sx[i * side + j] = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.7) * w / side;
                    sy[i * side + j] = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.7) * h / side;
                }
            g.setStroke(new BasicStroke(1.5f));
            int n = side * side;
            int step = 2;
            for (int y = 0; y < h - step; y += step)
                for (int x = 0; x < w - step; x += step) {
                    int a = owner(x, y, sx, sy, n);
                    int b = owner(x + step, y, sx, sy, n);
                    int c2 = owner(x, y + step, sx, sy, n);
                    if (a != b) g.draw(new Line2D.Double(x, y, x + step, y));
                    if (a != c2) g.draw(new Line2D.Double(x, y, x, y + step));
                }
        });

        // 12. flow: flow-field lines (Vanta 'topology')
        F.put("flow", (g, w, h, rng) -> {
            double a1 = (1 + rng.nextInt(2)) * 2 * Math.PI / w;
            double a2 = (1 + rng.nextInt(2)) * 2 * Math.PI / h;
            double p1 = rng.nextDouble() * Math.PI * 2, p2 = rng.nextDouble() * Math.PI * 2;
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 240; i++) {
                double x = rng.nextDouble() * w, y = rng.nextDouble() * h;
                Path2D p = new Path2D.Double();
                p.moveTo(x, y);
                for (int s = 0; s < 22; s++) {
                    double th = Math.sin(x * a1 + p1) + Math.cos(y * a2 + p2);
                    double ang = th * 2.0;
                    x += Math.cos(ang) * 3.2;
                    y += Math.sin(ang) * 3.2;
                    p.lineTo(x, y);
                }
                g.draw(p);
            }
        });

        // 13. dots: modulated dot grid (Vanta 'dots')
        F.put("dots", (g, w, h, rng) -> {
            int n = 9;
            double cell = w / n;
            double kx = 1 + rng.nextInt(2), ky = 1 + rng.nextInt(2);
            double ph = rng.nextDouble() * Math.PI * 2;
            for (int i = 0; i <= n; i++)
                for (int j = 0; j <= n; j++) {
                    // centers on the border grid (i*cell) so dots clip at the
                    // frame edges -> the field continues beyond the tile
                    double cx = i * cell + (rng.nextDouble() - 0.5) * 0.3 * cell;
                    double cy = j * cell + (rng.nextDouble() - 0.5) * 0.3 * cell;
                    double v = 0.5 + 0.5 * Math.sin(cx * kx * 2 * Math.PI / w + ph) * Math.cos(cy * ky * 2 * Math.PI / h);
                    double r = cell * (0.12 + v * v * 0.34);
                    g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
                }
        });

        // 14. cellular: organic voronoi — each cell as one smooth wobbly
        //     closed membrane (KDE 'Cluster' / Vanta 'cells' look)
        F.put("cellular", (g, w, h, rng) -> {
            int side = 4;
            int n = side * side;
            // interior sites
            double[] sx = new double[n], sy = new double[n];
            for (int i = 0; i < side; i++)
                for (int j = 0; j < side; j++) {
                    sx[i * side + j] = (i + 0.5 + (rng.nextDouble() - 0.5) * 0.7) * w / side;
                    sy[i * side + j] = (j + 0.5 + (rng.nextDouble() - 0.5) * 0.7) * h / side;
                }
            // border sites (just outside the frame) -> cells clip at all four edges
            int extra = side * 4;
            double[] ex = new double[n + extra], ey = new double[n + extra];
            System.arraycopy(sx, 0, ex, 0, n);
            System.arraycopy(sy, 0, ey, 0, n);
            int c = n;
            for (int i = 0; i < side; i++) {
                double t = (i + 0.5) / side;
                ex[c] = -w * 0.06;
                ey[c] = t * h;
                c++;
                ex[c] = w * 1.06;
                ey[c] = t * h;
                c++;
                ex[c] = t * w;
                ey[c] = -h * 0.06;
                c++;
                ex[c] = t * w;
                ey[c] = h * 1.06;
                c++;
            }
            int total = n + extra;
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int segs = 48;
            for (int s = 0; s < n; s++) {
                Path2D p = new Path2D.Double();
                for (int k = 0; k <= segs; k++) {
                    double a = k / (double) segs * 2 * Math.PI;
                    double dxr = Math.cos(a), dyr = Math.sin(a);
                    double maxR = Double.POSITIVE_INFINITY;
                    for (int t = 0; t < total; t++) {
                        if (t == s) continue;
                        double ox = ex[t] - ex[s], oy = ey[t] - ey[s];
                        double dot = ox * dxr + oy * dyr;
                        if (dot <= 1e-9) continue; // bisector behind or parallel
                        double r = (ox * ox + oy * oy) / (2 * dot);
                        if (r < maxR) maxR = r;
                    }
                    if (Double.isInfinite(maxR)) maxR = 1e9;
                    // organic wobble of the membrane
                    double wob = maxR * (0.07 * Math.sin(3 * a + s * 1.7) + 0.05 * Math.cos(5 * a + s * 0.9));
                    double bx = ex[s] + (maxR + wob) * dxr;
                    double by = ey[s] + (maxR + wob) * dyr;
                    if (k == 0) p.moveTo(bx, by);
                    else p.lineTo(bx, by);
                }
                p.closePath();
                g.draw(p);
            }
        });

        // 15. carpet: Sierpinski carpet (plane-filling fractal)
        F.put("carpet", (g, w, h, rng) -> {
            g.setStroke(new BasicStroke(1.1f));
            carpet(g, 0, 0, w, h, 3);
        });

        // 16. hilbert: space-filling Hilbert curve (correct d2xy)
        F.put("hilbert", (g, w, h, rng) -> {
            int order = 6;
            int n = 1 << order;
            double m = (Math.min(w, h) - 8.0) / n;
            double ox = (w - n * m) / 2, oy = (h - n * m) / 2;
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D p = new Path2D.Double();
            for (int d = 0; d < n * n; d++) {
                int[] xy = d2xy(n, d);
                double x = ox + (xy[0] + 0.5) * m, y = oy + (xy[1] + 0.5) * m;
                if (d == 0) p.moveTo(x, y);
                else p.lineTo(x, y);
            }
            g.draw(p);
        });

        // 17. honeycomb: wobbly hexagonal tiling (KDE 'Canopee' / 'Flow')
        F.put("honeycomb", (g, w, h, rng) -> {
            double r = 13 + rng.nextDouble() * 8; // hex radius
            double hexW = Math.sqrt(3) * r;
            double hexH = 1.5 * r;
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cols = (int) (w / hexW) + 3, rows = (int) (h / hexH) + 3;
            for (int i = -1; i < cols; i++) {
                for (int j = -1; j < rows; j++) {
                    double cx = i * hexW + (j % 2 == 0 ? 0 : hexW / 2);
                    double cy = j * hexH;
                    Path2D p = new Path2D.Double();
                    for (int k = 0; k < 6; k++) {
                        double a = Math.toRadians(60 * k - 30 + (rng.nextDouble() - 0.5) * 14);
                        double rr = r * (0.94 + rng.nextDouble() * 0.09);
                        double x = cx + rr * Math.cos(a), y = cy + rr * Math.sin(a);
                        if (k == 0) p.moveTo(x, y);
                        else p.lineTo(x, y);
                    }
                    p.closePath();
                    g.draw(p);
                    if (rng.nextDouble() < 0.18) {
                        // some cells carry a small centered dot (KDE 'Flow' accent)
                        g.fill(new Ellipse2D.Double(cx - 2, cy - 2, 4, 4));
                    }
                }
            }
        });

        // 18. ripple: radial fan + concentric rings from an interior point
        //     (Vanta 'ripple' / 'trunk'); full 360° crosses all four borders
        F.put("ripple", (g, w, h, rng) -> {
            double cx = (0.25 + rng.nextDouble() * 0.5) * w;
            double cy = (0.25 + rng.nextDouble() * 0.5) * h;
            g.setStroke(new BasicStroke(1.0f));
            int lines = 48;
            double len = Math.max(Math.hypot(cx, cy), Math.hypot(w - cx, h - cy)) * 1.6;
            double startDeg = rng.nextDouble() * 360;
            for (int i = 0; i < lines; i++) {
                double a = Math.toRadians(startDeg + i * 360.0 / lines);
                g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * len, cy + Math.sin(a) * len));
            }
            for (double r = Math.min(w, h) * 0.10; r < len; r *= 1.30) {
                Path2D p = new Path2D.Double();
                p.append(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r), false);
                g.draw(p);
            }
        });
        return F;
    }

    public static void main(String[] args) throws Exception {
        Map<String, Family> F = families();

        // ------------------------------------------------------------- edge audit
        // The plane-filling contract: ink must reach all four borders (>=4% of the
        // 6px border band), with both seed variants. Catches centered-motif regressions.
        int S = 200;
        System.out.println("=== edge coverage (top/right/bottom/left), seedA | seedB ===");
        for (var e : F.entrySet()) {
            String rep = "";
            boolean ok = true;
            for (String sd : new String[]{"A", "B"}) {
                double[] c = edgeCoverage(render(e.getValue(), S, S, seedOf(e.getKey() + sd)));
                rep += String.format("%.2f/%.2f/%.2f/%.2f  ", c[0], c[1], c[2], c[3]);
                if (Math.min(Math.min(c[0], c[1]), Math.min(c[2], c[3])) < 0.04) ok = false;
            }
            System.out.printf("%-12s %s%s%n", e.getKey(), rep, ok ? "ok" : "FAIL (bounded motif?)");
        }

        // ------------------------------------------------------------- render
        String[] seeds = {"A", "B"};
        int cols = 6;
        int rows = (F.size() * seeds.length + cols - 1) / cols;
        BufferedImage sheet = new BufferedImage(cols * (S + 12) + 12, rows * (S + 34) + 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = sheet.createGraphics();
        sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        sg.setColor(new Color(0x0d, 0x0f, 0x13));
        sg.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        sg.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int idx = 0;
        for (var e : F.entrySet())
            for (String sd : seeds) {
                BufferedImage tile = render(e.getValue(), S, S, seedOf(e.getKey() + sd));
                int x = 12 + (idx % cols) * (S + 12);
                int y = 30 + (idx / cols) * (S + 34);
                sg.drawImage(tile, x, y, null);
                sg.setColor(new Color(0xcc, 0xd6, 0xe0));
                sg.drawString(e.getKey() + "·" + sd, x + 4, y + S + 16);
                idx++;
            }
        sg.dispose();
        ImageIO.write(sheet, "png", new File("patterns-java-sheet.png"));

        // node mockups: 4 families over 3 hues at 15% ink
        String[] picks = {"cells", "waves", "net", "stripes"};
        double[] hues = {18, 126, 270};
        BufferedImage mock = new BufferedImage(3 * 240 + 20, 4 * 120 + 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D mg = mock.createGraphics();
        mg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mg.setColor(new Color(0x0d, 0x0f, 0x13));
        mg.fillRect(0, 0, mock.getWidth(), mock.getHeight());
        for (int i = 0; i < picks.length; i++) {
            for (int j = 0; j < hues.length; j++) {
                int x = 10 + j * 240, y = 10 + i * 120;
                float hueF = (float) (hues[j] / 360.0);
                mg.setColor(Color.getHSBColor(hueF, 0.6f, 0.42f));
                mg.fillRoundRect(x, y, 220, 100, 14, 14);
                BufferedImage tile = render(F.get(picks[i]), 220, 100, seedOf("jipipe:framework:" + picks[i]));
                mg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                mg.drawImage(tile, x, y, null);
                mg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                mg.setColor(Color.WHITE);
                mg.drawString(picks[i], x + 8, y + 18);
            }
        }
        mg.dispose();
        ImageIO.write(mock, "png", new File("patterns-java-mockups.png"));
        System.out.println("done: " + F.size() + " families");
    }

    // ------------------------------------------------------------ helpers
    static double curveY(double x, double yb, double amp, double kk, double ph, int ww) {
        return yb + amp * Math.sin(x / ww * Math.PI * 2 * kk + ph) + amp * 0.5 * Math.sin(x / ww * Math.PI * 4 + ph * 2);
    }

    static double f(double x, double y, int[] fx, int[] fy, double[] amp, double[] ph, int w, int h) {
        double v = 0;
        for (int i = 0; i < fx.length; i++)
            v += amp[i] * Math.sin(2 * Math.PI * fx[i] * x / w + ph[i]) * Math.cos(2 * Math.PI * fy[i] * y / h + ph[i] * 1.7);
        return v;
    }

    static double field(double x, double y, int[] fx, double[][] fy, double[] amp, double[] ph, int w, int h) {
        double v = 0;
        for (int i = 0; i < fx.length; i++)
            v += amp[i] * Math.sin(2 * Math.PI * fx[i] * x / w + ph[i]) * Math.cos(2 * Math.PI * fy[i][0] * y / h + fy[i][1]);
        return v;
    }

    // returns 2 or 4 crossing points per cell
    static double[][] crossSegs(int gx, int gy, int step, double v00, double v10, double v11, double v01, double lv) {
        double[][] corners = {{gx, gy}, {gx + step, gy}, {gx + step, gy + step}, {gx, gy + step}};
        double[] vals = {v00, v10, v11, v01};
        List<double[]> pts = new ArrayList<>();
        for (int e = 0; e < 4; e++) {
            double v1 = vals[e], v2 = vals[(e + 1) % 4];
            if ((v1 - lv) * (v2 - lv) <= 0 && v1 != v2) {
                double t = (lv - v1) / (v2 - v1);
                double[] c1 = corners[e], c2 = corners[(e + 1) % 4];
                pts.add(new double[]{c1[0] + t * (c2[0] - c1[0]), c1[1] + t * (c2[1] - c1[1])});
            }
        }
        if (pts.isEmpty() || pts.size() > 4) return null;
        return pts.toArray(new double[0][]);
    }

    static void fillBandH(Graphics2D g, int w, int h, double y, double bw) {
        g.fill(new RoundRectangle2D.Double(-bw, y, w + 2 * bw, bw, bw * 0.6, bw * 0.6));
    }

    static void subdivide(Graphics2D g, Random rng, double x, double y, double w, double h, int depth, boolean fill) {
        if (depth == 0 || (w < 26 && h < 26)) {
            if (fill) g.fill(new Rectangle2D.Double(x + 1.5, y + 1.5, Math.max(1, w - 3), Math.max(1, h - 3)));
            return;
        }
        if (w > h * 1.3 || (w > h && rng.nextDouble() < 0.65)) {
            double split = x + w * (0.3 + rng.nextDouble() * 0.4);
            subdivide(g, rng, x, y, split - x, h, depth - 1, !fill);
            subdivide(g, rng, split, y, x + w - split, h, depth - 1, !fill);
        } else {
            double split = y + h * (0.3 + rng.nextDouble() * 0.4);
            subdivide(g, rng, x, y, w, split - y, depth - 1, !fill);
            subdivide(g, rng, x, split, w, y + h - split, depth - 1, !fill);
        }
    }

    static int owner(double x, double y, double[] sx, double[] sy, int n) {
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double dx = x - sx[i], dy = y - sy[i];
            double d = dx * dx + dy * dy;
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    static int[] nearest(double x, double y, double[] px, double[] py, int n, int k, double maxD) {
        double[] d = new double[n];
        for (int i = 0; i < n; i++) d[i] = (px[i] - x) * (px[i] - x) + (py[i] - y) * (py[i] - y);
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(d[a], d[b]));
        int[] out = new int[Math.min(k, n)];
        for (int i = 1; i <= Math.min(k, n); i++) {
            if (d[idx[i]] <= maxD * maxD) out[i - 1] = idx[i];
            else break;
        }
        return out;
    }

    static void carpet(Graphics2D g, double x, double y, double w, double h, int depth) {
        if (depth == 0) {
            g.draw(new Rectangle2D.Double(x, y, w, h));
            return;
        }
        double cw = w / 3, ch = h / 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) continue;
                carpet(g, x + i * cw, y + j * ch, cw, ch, depth - 1);
            }
    }

    static int[] d2xy(int n, int d) {
        // canonical Wikipedia Hilbert d2xy: rotate steps inside-out (s=1 up)
        int x = 0, y = 0, s, t = d;
        for (s = 1; s < n; s *= 2) {
            int rx = 1 & (t / 2);
            int ry = 1 & (t ^ rx);
            if (ry == 0) {
                if (rx == 1) {
                    x = s - 1 - x;
                    y = s - 1 - y;
                }
                int tmp = x;
                x = y;
                y = tmp;
            }
            x += s * rx;
            y += s * ry;
            t /= 4;
        }
        return new int[]{x, y};
    }

    static double toDeg(double rad) {
        return Math.toDegrees(rad);
    }

    static double[] edgeCoverage(BufferedImage img) {
        int S = img.getWidth();
        int edgeW = 6;
        double[] acc = new double[4];
        for (int i = 0; i < edgeW; i++) {
            for (int j = 0; j < S; j++) if (ink(img.getRGB(j, i))) acc[0]++;
            for (int j = 0; j < S; j++) if (ink(img.getRGB(i, j))) acc[1]++;
            for (int j = 0; j < S; j++) if (ink(img.getRGB(j, S - 1 - i))) acc[2]++;
            for (int j = 0; j < S; j++) if (ink(img.getRGB(S - 1 - i, j))) acc[3]++;
        }
        double n = edgeW * S;
        return new double[]{acc[0] / n, acc[1] / n, acc[2] / n, acc[3] / n};
    }

    static boolean ink(int argb) {
        return ((argb >> 24) & 0xFF) > 40;
    }

    public static BufferedImage render(Family fam, int w, int h, Random rng) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        fam.paint(g, w, h, rng);
        g.dispose();
        return img;
    }

    public interface Family {
        void paint(Graphics2D g, int w, int h, Random rng);
    }
}
