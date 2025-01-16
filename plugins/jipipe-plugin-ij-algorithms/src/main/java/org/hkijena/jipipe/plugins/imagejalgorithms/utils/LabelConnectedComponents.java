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
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.plugins.imagejalgorithms.parameters.Neighborhood2D;

public class LabelConnectedComponents {
    private LabelConnectedComponents() {

    }

    public static ImageProcessor process(ImageProcessor ip, Neighborhood2D connectivity) {

        int width = ip.getWidth();
        int height = ip.getHeight();

        // Output image for labeled components
        ImageProcessor output;
        if (ip instanceof ShortProcessor) {
            output = new ShortProcessor(width, height);
        } else {
            output = new FloatProcessor(width, height);
        }
        int[] newLabels = new int[width * height];
        TIntArrayList parent = new TIntArrayList();
        parent.add(0); // Dummy entry at index 0 since labels start from 1
        int nextLabel = 1;
        TIntArrayList neighborhoodNewDifferentLabels = new TIntArrayList(8);

        // Find the neighborhoods and track them
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int currentLabel = (int) ip.getf(x, y);
                if (currentLabel <= 0) {
                    continue;
                }

                neighborhoodNewDifferentLabels.resetQuick();
                int currentNewLabel = nextLabel++;

                // Ensure parent array can handle the new label
                while (parent.size() <= currentNewLabel) {
                    parent.add(parent.size());
                }

                // Check neighbors and track unique labels
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if ((dx == 0 && dy == 0) || // Skip current pixel
                                (connectivity == Neighborhood2D.FourConnected && dx == dy) || // Skip diagonals for 4-connectivity
                                (x + dx < 0 || y + dy < 0 || x + dx >= width || y + dy >= height)) { // Bounds check
                            continue;
                        }

                        int nx = x + dx;
                        int ny = y + dy;
                        final int neighborLabel = (int) ip.getf(nx, ny);

                        if (neighborLabel > 0 && neighborLabel == currentLabel) {
                            int neighborNewLabel = newLabels[ny * width + nx];
                            if (neighborNewLabel > 0) {
                                neighborhoodNewDifferentLabels.add(neighborNewLabel);
                            }
                        }
                    }
                }

                if (neighborhoodNewDifferentLabels.isEmpty()) {
                    // Assign a new label if no neighbors have the same label
                    newLabels[y * width + x] = currentNewLabel;
                } else {
                    // Find the minimum label in the neighborhood and union all labels
                    int minLabel = neighborhoodNewDifferentLabels.min();
                    newLabels[y * width + x] = minLabel;

                    for (int i = 0; i < neighborhoodNewDifferentLabels.size(); i++) {
                        int neighborLabel = neighborhoodNewDifferentLabels.get(i);
                        union(minLabel, neighborLabel, parent);
                    }
                }
            }
        }

        // Second pass: Resolve labels using Union-Find
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int label = newLabels[y * width + x];
                if (label > 0) {
                    newLabels[y * width + x] = find(label, parent);
                }
            }
        }

        // Copy the resolved labels into the output
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int newLabel = newLabels[x + y * width];
                output.setf(x, y, newLabel);
            }
        }

        // Remap labels
        LabelImages.remapLabels(output);

        return output;
    }

    // Union operation for Union-Find
    private static void union(int label1, int label2, TIntArrayList parent) {
        int root1 = find(label1, parent);
        int root2 = find(label2, parent);
        if (root1 != root2) {
            parent.set(root2, root1);
        }
    }

    // Find operation for Union-Find with path compression
    private static int find(int label, TIntArrayList parent) {
        while (parent.get(label) != label) {
            parent.set(label, parent.get(parent.get(label))); // Path compression
            label = parent.get(label);
        }
        return label;
    }
}
