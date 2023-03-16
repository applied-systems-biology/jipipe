package org.hkijena.jipipe.extensions.ij3d.utils;

import ij.ImageStack;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.geom.Vector3D;
import mcib3d.image3d.ImageHandler;

public class ExtendedObjectCreator3D extends ObjectCreator3D {
    public ExtendedObjectCreator3D(ImageHandler image) {
        super(image);
    }

    public ExtendedObjectCreator3D(ImageStack stack) {
        super(stack);
    }

    public ExtendedObjectCreator3D(int sizex, int sizey, int sizez) {
        super(sizex, sizey, sizez);
    }

    public void createLine(int x0, int y0, int z0, int x1, int y1, int z1, float val, int rad0, int rad1) {
        ImageHandler img = getImageHandler();
        Vector3D V = new Vector3D(x1 - x0, y1 - y0, z1 - z0);
        double len = V.getLength();
        V.normalize();
        double vx = V.getX();
        double vy = V.getY();
        double vz = V.getZ();
        for (int i = 0; i < (int) len; i++) {
            double perc = i / len;
            int rad = (int)(rad0 + perc * (rad1 - rad0));
            if (rad <= 0) {
                int xx = (int) (x0 + i * vx);
                int yy = (int) (y0 + i * vy);
                int zz = (int) (z0 + i * vz);
                if (img.contains(xx, yy, zz))
                    img.setPixel(xx, yy, zz, val);
            } else {
                createEllipsoid((int) (x0 + i * vx), (int) (y0 + i * vy), (int) (z0 + i * vz), rad, rad, rad, val, false);
            }
        }
    }
}
