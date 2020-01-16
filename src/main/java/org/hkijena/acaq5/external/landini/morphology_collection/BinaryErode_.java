package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

//Binary Erode by Gabriel Landini, G.Landini@bham.ac.uk
//v 1.1 deals with 0 iterations
public class BinaryErode_ implements PlugInFilter {
	protected boolean doIwhite;
	protected int iterations;
	protected int threshold;

	public int setup(String arg, ImagePlus imp) {
		ImageStatistics stats;
		stats=imp.getStatistics();
		if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return DONE;
		}

		if (arg.equals("about"))
			{showAbout(); return DONE;}
		GenericDialog gd = new GenericDialog("BinaryErode", IJ.getInstance());
		gd.addMessage("Binary Erosion (3x3)");
		gd.addNumericField ("Coefficient (0-7)", 0, 0);
		gd.addNumericField ("Iterations (-1=all)", 1, 0);
		gd.addCheckbox("White foreground",false);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;
		threshold = (8-(int) gd.getNextNumber())*255;
		iterations = (int) gd.getNextNumber();
		doIwhite = gd.getNextBoolean ();
		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y, i, count, currcount;
		int [][] pixel = new int [xe][ye];
		int [][] pixel2 = new int [xe][ye];

		if (iterations!=0){
			//original converted to white particles
			if (doIwhite==false){
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++)
						ip.putPixel(x,y,255-ip.getPixel(x,y));
				}
			}
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					pixel2[x][y]=ip.getPixel(x,y);//original
					pixel[x][y]=pixel2[x][y];
				}
			}

			currcount=-2;
			i=0;
			while (currcount!=0){
				IJ.showStatus("Eroding: "+ ++i);
				count=0;
				for (x=1; x<xe-1; x++) {
					for (y=1; y<ye-1; y++) {
						//white Erode of image except borders
						if(pixel[x][y]==255){// some time saving
							if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x+1][y-1]+
								pixel2[x-1][y  ]+pixel2[x+1][y  ]+
								pixel2[x-1][y+1]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<threshold){
								pixel[x][y]=0;
								count++;
							}
						}
					}
				}
				//here I assume that pixels outside the image are "empty"
				y=0;
				for (x=1; x<xe-1; x++) {
					//white dilate upper row
					if(pixel[x][y]==255){
						if (pixel2[x-1][y  ]+pixel2[x+1][y  ]+
							pixel2[x-1][y+1]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<threshold){ //1530
							pixel[x][y]=0;
							count++;
						}
					}
				}


				y=ye-1;
				for (x=1; x<xe-1; x++) {
					//white dilate lower row
					if(pixel[x][y]==255){
						if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x+1][y-1]+
							pixel2[x-1][y  ]+pixel2[x+1][y  ]<threshold){ //1530
							pixel[x][y]=0;
						count++;
						}
					}
				}

				x=0;
				for (y=1; y<ye-1; y++) {
					//white dilate left column
					if(pixel[x][y]==255){
						if (pixel2[x  ][y-1]+pixel2[x+1][y-1]+
							pixel2[x+1][y  ]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<threshold){ //1530
							pixel[x][y]=0;
						count++;
						}
					}
				}

				x=xe-1;
				for (y=1; y<ye-1; y++) {
					//white dilate right column
					if(pixel[x][y]==255){
						if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x-1][y  ]+
							pixel2[x-1][y+1]+pixel2[x  ][y+1]<threshold){ //1530
							pixel[x][y]=0;
							count++;
						}
					}
				}

				x=0; //upper left corner
				y=0;
				if(pixel[x][y]==255){
					if (pixel2[x+1][y  ]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<threshold){ //1020
						pixel[x][y]=0;
						count++;
					}
				}

				x=xe-1; //upper right corner
				//y=0;
				if(pixel[x][y]==255){
					if (pixel2[x-1][y  ]+pixel2[x-1][y+1]+pixel2[x  ][y+1]<threshold){ //1020
						pixel[x][y]=0;
						count++;
					}
				}

				x=0; //lower left corner
				y=ye-1;
				if(pixel[x][y]==255){
					if (pixel2[x  ][y-1]+pixel2[x+1][y-1]+pixel2[x+1][y  ]<threshold){ //1020
						pixel[x][y]=0;
					count++;
					}
				}

				x=xe-1; //lower right corner
				y=ye-1;
				if(pixel[x][y]==255){
					if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x-1][y  ]<threshold){ //1020
						pixel[x][y]=0;
						count++;
					}
				}

				//put result
				for (x=0; x<xe; x++) {
					for (y=0; y<ye; y++)
							pixel2[x][y]=pixel[x][y];
				}

				if (i==iterations)
					break;
				currcount=count;
			}

			for (x=0; x<xe; x++) {
				for (y=0; y<ye; y++)
						ip.putPixel(x,y, pixel[x][y]);
			}



			//return to original state
			if (doIwhite==false){
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++)
						ip.putPixel(x,y,255-ip.getPixel(x,y));
				}
			}
		}
	}


	void showAbout() {
		IJ.showMessage("About BinaryErode_...",
		"BinaryErode_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for morphological dilation of a binary image.\n"+
		"Erodes the entire image, including borders.\n"+
		"Coefficients:\n"+
		"0=classical erosion, 7=removes single pixels,\n"+
		"6=prunning of 8-connected lines, 5=removes irregular particles\n"+
		"Supports black and white foregrounds.");
	}

}
