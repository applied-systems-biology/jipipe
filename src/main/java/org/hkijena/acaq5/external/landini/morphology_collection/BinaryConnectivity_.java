package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

//Binary Connectivity by Gabriel Landini, G.Landini@bham.ac.uk

public class BinaryConnectivity_ implements PlugInFilter {
	protected boolean doIwhite;

	public int setup(String arg, ImagePlus imp) {

		ImageStatistics stats;
		stats=imp.getStatistics();
		if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return DONE;
		}

		if (arg.equals("about"))
			{showAbout(); return DONE;}
		GenericDialog gd = new GenericDialog("BinaryConnectivity", IJ.getInstance());
		gd.addMessage("Connectivity");
		gd.addCheckbox("White particles on black background",true);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;

		doIwhite = gd.getNextBoolean ();
		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y;
		int [][] pixel = new int [xe][ye];

		//original converted to white particles
		if (doIwhite==false){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255-ip.getPixel(x,y));
			}
		}

		//get original
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel[x][y]=ip.getPixel(x,y);
				ip.putPixel(x,y,0);
			}
		}

		for(y=1;y<ye-1;y++) {
			for(x=1;x<xe-1;x++) {
				if (pixel[x][y]==255)
					ip.putPixel(x,y,(pixel[x-1][y-1]+pixel[x][y-1]+pixel[x+1][y-1]+pixel[x-1][y]+pixel[x][y]+pixel[x+1][y]+
					pixel[x-1][y+1]+pixel[x][y+1]+pixel[x+1][y+1])/255);
			}
		}

		//here I assume that pixels outside the image are "empty"
		y=0; //upper row
		for (x=1; x<xe-1; x++) {
			if(pixel[x][y]==255)
				ip.putPixel(x,y,(pixel[x-1][y]+pixel[x][y]+pixel[x+1][y]+pixel[x-1][y+1]+pixel[x][y+1]+pixel[x+1][y+1])/255);
		}

		y=ye-1; //lower row
		for (x=1; x<xe-1; x++) {
			if(pixel[x][y]==255)
				ip.putPixel(x,y,(pixel[x-1][y-1]+pixel[x][y-1]+pixel[x+1][y-1]+pixel[x-1][y]+pixel[x][y]+pixel[x+1][y])/255);
		}

		x=0; //left column
		for (y=1; y<ye-1; y++) {
			if(pixel[x][y]==255)
				ip.putPixel(x,y,(pixel[x][y-1]+pixel[x+1][y-1]+pixel[x][y]+	pixel[x+1][y]+pixel[x][y+1]+pixel[x+1][y+1])/255);
		}

		x=xe-1;//right column
		for (y=1; y<ye-1; y++) {
			if(pixel[x][y]==255)
				ip.putPixel(x,y,(pixel[x-1][y-1]+pixel[x][y-1]+pixel[x-1][y]+pixel[x][y]+pixel[x-1][y+1]+pixel[x][y+1])/255);
		}

		x=0; //upper left corner
		y=0;
		if(pixel[x][y]==255)
			ip.putPixel(x,y,(pixel[x+1][y]+pixel[x][y+1]+pixel[x+1][y+1]+pixel[x][y])/255);

		x=xe-1; //upper right corner
		//y=0;
		if(pixel[x][y]==255)
			ip.putPixel(x,y,(pixel[x-1][y]+pixel[x-1][y+1]+pixel[x][y+1]+pixel[x][y])/255);

		x=0; //lower left corner
		y=ye-1;
		if(pixel[x][y]==255)
			ip.putPixel(x,y,(pixel[x][y-1]+pixel[x+1][y-1]+pixel[x+1][y]+pixel[x][y])/255);

		x=xe-1; //lower right corner
		y=ye-1;
		if(pixel[x][y]==255)
			ip.putPixel(x,y,(pixel[x-1][y-1]+pixel[x][y-1]+pixel[x-1][y]+pixel[x][y])/255);

	}


	void showAbout() {
		IJ.showMessage("About BinaryConnectivity_...",
		"BinaryConnectivity_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin that returns the number of connected pixels (+1) to each\n"+
		"foreground pixel (8 neighbours):\n"+
		"Background = 0, single pixel = 1, end of a line = 2, bifurcations = 3, etc.\n"+
		"Brightness/Contrast must be adjusted to see the result.\n"+
		"Supports black and white foregrounds.");
	}

}
