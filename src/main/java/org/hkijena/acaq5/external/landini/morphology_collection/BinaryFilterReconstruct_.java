package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

//Binary Filter Reconstruct by Gabriel Landini, G.Landini@bham.ac.uk
// v1.2  20/11/2004  ~x4 speed increase in the reconstruction
// v1.3 29/Jun/2006 speed up via floodfilling instead of old method of reconstruction
// v1.4 1/May/2008 fixed bugs when using black particles or inverted LUT images
// 1.5  26/Dec/2008 slight speed improvement.
// 1.6  25/May/2009  fore/background.speedup via 1D image arrays

public class BinaryFilterReconstruct_ implements PlugInFilter {
	protected int nerosions;
	protected boolean doIwhite=Prefs.blackBackground;

	public int setup(String arg, ImagePlus imp) {
		ImageStatistics stats;
		stats=imp.getStatistics();
		if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return DONE;
		}

		if (arg.equals("about"))
			{showAbout(); return DONE;}
		GenericDialog gd = new GenericDialog("Binary Filter Reconstruct", IJ.getInstance());
		gd.addMessage("Binary Filter Reconstruct v1.6");
		gd.addNumericField("Erosions",1,0);
		gd.addCheckbox("White particles on black background", doIwhite);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;
        	nerosions=(int) gd.getNextNumber();
		doIwhite = gd.getNextBoolean ();
		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		int x, y, i, offset, size = width * height;
		int foreground = 255, background = 0;

		byte[] pixels =(byte []) ip.getPixels();
		int [] mask = new int [size];
		int [] seed = new int [size];

		if (!doIwhite){
			foreground = 0;
			background = 255;
		}

		for(i=0;i<size;i++) {
			mask[i]=(int)(pixels[i]&0xff);
		}
		//erode n times
 		if (ip.isInvertedLut() || !doIwhite){
			for(i=0;i<nerosions;i++)
				ip.erode(); // erodes black
		}
		else{
			for(i=0;i<nerosions;i++)
				ip.dilate(); // erodes black
		}
		for(i=0;i<size;i++) 
			seed[i]=(int)(pixels[i]&0xff); //set seed to the eroded values

		for(i=0;i<size;i++) 
			pixels[i]= (mask[i]==foreground)?(byte)(127 & 0xff): (byte) (background & 0xff); //set image to 127 if mask

		FloodFiller ff = new FloodFiller(ip);

		ip.setColor(foreground);

		for(y=0;y<height;y++) {
			offset = y * width;
			for(x=0;x<width;x++){
				if ((int) (seed[offset +x] & 0xff) ==foreground) {
					if ((int)(pixels[offset + x] & 0xff)==127)
						ff.fill8(x, y);
				}
			}
		}

		for(y=0;y<height;y++) {
			offset = y * width;
			for(x=0;x<width;x++){
				if ((int)(pixels[offset + x] & 0xff)==127)
					pixels[offset + x] = (byte) (background & 0xff);
			}
		}
	}

	void showAbout() {
		IJ.showMessage("About BinaryFilterReconstruct_...",
		"BinaryFilterReconstruct_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for deleting particles that would disappear after\n"+
		"n erosions. The advantage over morphological opening is that\n"+
		"this plugin preserves the original shape of particles that\n"+
		"are not deleted (i.e. no particle smoothing takes place).\n"+
		"Supports both black and white binary particles.\n"+
		"In images with an Inverted LUT, black is white and viceversa!");
	}
}
