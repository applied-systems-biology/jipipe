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
 *
 */

package org.hkijena.jipipe.extensions.ij3d.utils;

import ij.ImagePlus;
import ij.ImageStack;
import mcib3d.geom.Object3D;
import mcib3d.geom.Object3DVoxels;
import mcib3d.geom.Voxel3D;
import mcib3d.image3d.ImageHandler;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extended version of {@link Object3DVoxels} that comes with some additional loading methods
 */
public class ExtendedObject3DVoxels extends Object3DVoxels {
    public ExtendedObject3DVoxels() {
    }

    public ExtendedObject3DVoxels(int val) {
        super(val);
    }

    public ExtendedObject3DVoxels(ImageHandler ima, int val) {
        super(ima, val);
    }

    public ExtendedObject3DVoxels(ImageHandler ima) {
        super(ima);
    }

    public ExtendedObject3DVoxels(ImageHandler imaSeg, ImageHandler imaRaw) {
        super(imaSeg, imaRaw);
    }

    public ExtendedObject3DVoxels(ImagePlus plus, int val) {
        super(plus, val);
    }

    public ExtendedObject3DVoxels(ImageStack stack, int val) {
        super(stack, val);
    }

    public ExtendedObject3DVoxels(List<Voxel3D> al) {
        super(al);
    }

    public ExtendedObject3DVoxels(Object3DVoxels other) {
        super(other);
    }

    public ExtendedObject3DVoxels(Object3D other) {
        super(other);
    }

    /**
     * Saves the voxels to a stream
     *
     * @param stream the stream
     */
    public void saveObjectToStream(OutputStream stream) {
        Voxel3D pixel;
        BufferedWriter bf;
        int c = 0;
        try {
            bf = new BufferedWriter(new OutputStreamWriter(stream));
            saveInfo(bf);
            for (Voxel3D voxel : getVoxels()) {
                c++;
                pixel = new Voxel3D(voxel);
                bf.write(c + "\t" + pixel.getX() + "\t" + pixel.getY() + "\t" + pixel.getZ() + "\t" + pixel.getValue() + "\n");
            }
            bf.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the voxels from a stream
     *
     * @param stream the input stream
     * @param name   the name
     */
    public void loadObjectFromStream(InputStream stream, String name) {
        BufferedReader bf;
        String data;
        String[] coord;
        double dx, dy, dz;
        int v;
        this.setName(name);
        LinkedList<Voxel3D> voxels = new LinkedList<>();
        try {
            bf = new BufferedReader(new InputStreamReader(stream));
            data = loadInfo(bf);
            while (data != null) {
                coord = data.split("\t");
                dx = Double.parseDouble(coord[1]);
                dy = Double.parseDouble(coord[2]);
                dz = Double.parseDouble(coord[3]);
                v = (int) Double.parseDouble(coord[4]);
                voxels.add(new Voxel3D(dx, dy, dz, v));
                data = bf.readLine();
            }
            bf.close();
            setVoxels(voxels);
        } catch (IOException ex) {
            Logger.getLogger(Object3DVoxels.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
