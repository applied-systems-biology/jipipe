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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import inra.ijpb.plugins.Connectivity2D;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;

public class LabelConnectedComponents {
    private LabelConnectedComponents() {

    }

    public static ImageProcessor process(ImageProcessor ip, Neighborhood2D connectivity) {

        int width = ip.getWidth();
        int height = ip.getHeight();

        // Output image for labeled components
        ImageProcessor labeled;
        if(ip instanceof ShortProcessor) {
            labeled = new ShortProcessor(width, height);
        }
        else {
            labeled = new FloatProcessor(width, height);
        }
        int[] labels = new int[width * height];
        int[] labelRoots = new int[width * height + 1]; // +1 to handle label indices starting from 1
        for (int i = 0; i < labelRoots.length; i++) {
            labelRoots[i] = i; // Initialize each label as its own root
        }
        int nextLabel = 1; // Start labeling from 1

        TIntArrayList neighbors = new TIntArrayList(8); // Preallocate for neighbor labels

        // First pass: Label components and record equivalences
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = (int) ip.getf(x, y); // Get pixel value as int
                if (pixel == 0) continue; // Skip background (assumes background is 0)

                neighbors.resetQuick(); // Clear the neighbors list
                getConnectedNeighbors(x, y, ip, labeled, connectivity, neighbors);
                if (neighbors.isEmpty()) {
                    labels[y * width + x] = nextLabel++;
                } else {
                    int minLabel = neighbors.min();
                    labels[y * width + x] = minLabel;
                    for (int i = 0; i < neighbors.size(); i++) {
                        int neighborLabel = neighbors.get(i);
                        if (neighborLabel > 0) { // Only union valid labels
                            union(minLabel, neighborLabel, labelRoots);
                        }
                    }
                }
            }
        }

        // Second pass: Relabel components with compact labels
        TIntIntHashMap newLabels = new TIntIntHashMap();
        int newLabelCount = 1;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] > 0) {
                int rootLabel = find(labels[i], labelRoots);
                if (!newLabels.containsKey(rootLabel)) {
                    newLabels.put(rootLabel, newLabelCount++);
                }
                labels[i] = newLabels.get(rootLabel);
            }
        }

        // Apply new labels to the output image
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                labeled.setf(x, y, labels[y * width + x]); // Set label as float
            }
        }

        return labeled;
    }

    private static void getConnectedNeighbors(int x, int y, ImageProcessor ip, ImageProcessor labeled, Neighborhood2D connectivity, TIntArrayList neighbors) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int pixelValue = (int) ip.getf(x, y);

        int[] dx, dy;
        if (connectivity == Neighborhood2D.EightConnected) {
            dx = new int[]{-1, 0, 1, -1, 1, -1, 0, 1};
            dy = new int[]{-1, -1, -1, 0, 0, 1, 1, 1};
        } else { // 4-connectivity
            dx = new int[]{-1, 0, 1, 0};
            dy = new int[]{0, -1, 0, 1};
        }

        for (int i = 0; i < dx.length; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                int nv = (int)ip.getf(nx, ny);
                if(nv == pixelValue) {
                    int neighborLabel = labeled.getPixel(nx, ny);
                    if (neighborLabel > 0) {
                        neighbors.add(neighborLabel);
                    }
                }
            }
        }
    }

    // Find operation for Union-Find with path compression
    private static int find(int label, int[] labelRoots) {
        int root = label;

        // Find the root of the component
        while (labelRoots[root] != root) {
            root = labelRoots[root];
        }

        // Path compression: Make all nodes on the path point directly to the root
        int current = label;
        while (current != root) {
            int parent = labelRoots[current];
            labelRoots[current] = root;
            current = parent;
        }

        return root;
    }

    // Union operation for Union-Find
    private static void union(int label1, int label2, int[] labelRoots) {
        int root1 = find(label1, labelRoots);
        int root2 = find(label2, labelRoots);
        if (root1 != root2) {
            labelRoots[root2] = root1;
        }
    }
}
