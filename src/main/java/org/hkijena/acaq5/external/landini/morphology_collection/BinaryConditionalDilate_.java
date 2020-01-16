package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

/*
Conditional Dilation, by G.Landini. 22/Oct/2003 for ImageJ

Dilates binary elements in the seed image inside the "mask" image,

It can handle black [0] particles on white [255] background or white particles
on a black background.

It supports stacks dilated by a single seed image and also
a single image dilated by a stack of seeds.
Not tested in versions older than 1.31l

Send any improvements or bugs to G.Landini@bham.ac.uk

This plugin is based on Calculator_Plus by Wayne Rasband (wayne@codon.nih.gov)

v1.0 release
v1.1 2/11/2003 replaced IJ erosion/dilations with own version as IJ does not process borders
v1.2 3/11/2003 small speed up. If "iterations" is set -1, then this is the same as "BinaryReconstruction"
*/

public class BinaryConditionalDilate_ implements PlugIn {

	static String title = "Binary Conditional Dilation";
    static boolean createWindow = true, whiteparticles = false;
	int[] wList;
	private String[] titles;
	int i1Index;
	int i2Index;
	ImagePlus i1;
	ImagePlus i2;
	boolean replicate;
	protected int iterations;

	public void run(String arg) {
		if (IJ.versionLessThan("1.31l"))
			return;
		wList = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			IJ.showMessage(title, "There must be at least two windows open");
			return;
		}
		titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp!=null)
				titles[i] = imp.getTitle();
			else
				titles[i] = "";
		}

		if (!showDialog())
			return;

		long start = System.currentTimeMillis();
		boolean calibrated = i1.getCalibration().calibrated() || i2.getCalibration().calibrated();

		if (calibrated)
			createWindow = true;
		if (createWindow) {
			if (replicate)
				i2 = replicateImage(i2, calibrated, i1.getStackSize());
			else
				i2 = duplicateImage(i2, calibrated);
			if (i2==null)
				{IJ.showMessage(title, "Out of memory"); return;}
			i2.show();
		}
		condDilate(i1, i2);
		IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
	}

	public boolean showDialog() {
		GenericDialog gd = new GenericDialog(title);
		gd.addMessage("Binary Conditional Dilation v 1.2");
		gd.addMessage("Dilation inside a mask");
		gd.addChoice("Mask i1:", titles, titles[0]);
		gd.addChoice("Seed i2:", titles, titles[1]);
		gd.addNumericField("Iterations (-1=all):", 1, 0);
		gd.addCheckbox("Create New Window", createWindow);
		gd.addCheckbox("White particles on black background", whiteparticles);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		int i1Index = gd.getNextChoiceIndex();
		int i2Index = gd.getNextChoiceIndex();
		iterations = (int) gd.getNextNumber();
		createWindow = gd.getNextBoolean();
		whiteparticles = gd.getNextBoolean();
		i1 = WindowManager.getImage(wList[i1Index]);
		i2 = WindowManager.getImage(wList[i2Index]);
		int d1 = i1.getStackSize();
		int d2 = i2.getStackSize();
		if (d2==1 && d1>1) {
			createWindow = true;
			replicate = true;
		}
		return true;
 }

	public void condDilate(ImagePlus i1, ImagePlus i2) {
		int width  = i1.getWidth();
		int height = i1.getHeight();
		ImageProcessor ip1, ip2;
		int slices1 = i1.getStackSize();
		int slices2 = i2.getStackSize();
		float[] ctable1 = i1.getCalibration().getCTable();
		float[] ctable2 = i2.getCalibration().getCTable();
		ImageStack stack1 = i1.getStack();
		ImageStack stack2 = i2.getStack();
		int currentSlice = i2.getCurrentSlice();
		ImageStatistics stats;
		int prevpix, currpix, v, hist, pass=0;

		IJ.showStatus("Binary Conditional Dilation...");
		for (int n=1; n<=slices2; n++) {
			ip1 = stack1.getProcessor(n<=slices1?n:slices1);
			stats=i1.getStatistics();
			if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
				IJ.error("8-bit binary image (0 and 255) required.");
				return;
			}
			stats=i2.getStatistics();
				if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
				IJ.error("8-bit binary image (0 and 255) required.");
				return;
			}

			ip2 = stack2.getProcessor(n);
			ip1.setCalibrationTable(ctable1);
			ip2.setCalibrationTable(ctable2);

			currpix = -1;
			//stats=i2.getStatistics();
			prevpix=stats.histogram[0];
			if (iterations!=0){
				while (currpix!=prevpix){
					IJ.showStatus("Dilating: "+ ++pass);
					//i2.updateAndDraw();
					if (whiteparticles){
						ip2.invert();
						myMin(ip2); // min filter
						ip2.invert();
						hist=0;
						for (int x=0; x<width; x++) {
							for (int y=0; y<height; y++) {
								v = ip1.getPixel(x,y)-255 + ip2.getPixel(x,y);
								ip2.putPixel(x, y, v);
								if (v==255) hist++;
							}
						}
					}
					else{
						myMin(ip2);// min filter
						//add = mask
						hist=0;
						for (int x=0; x<width; x++) {
							for (int y=0; y<height; y++) {
								v = ip1.getPixel(x,y) + ip2.getPixel(x,y);
								ip2.putPixel(x, y, v);
								if (v==0) hist++;
							}
						}
					}
					if (pass==iterations)
						break;
					prevpix=currpix;
					currpix = hist;	//IJ.write(prevpix+ " "+ currpix);
				}
			}
			if (n==currentSlice) {
				i2.getProcessor().resetMinAndMax();
				i2.updateAndDraw();
			}

			IJ.showProgress((double)n/slices2);
			IJ.showStatus(n+"/"+slices2);
		}
	}


	void myMin(ImageProcessor ip){
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y;
		int [][] pixel = new int [xe][ye];
		int [][] pixel2 = new int [xe][ye];


		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel2[x][y]=ip.getPixel(x,y);//original
				pixel[x][y]=pixel2[x][y];
			}
		}

		for (x=1; x<xe-1; x++) {
			for (y=1; y<ye-1; y++) {
				//white Erode of image except borders
				if(pixel[x][y]==255){// some time saving, can be eroded
					if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x+1][y-1]+
						pixel2[x-1][y  ]+pixel2[x+1][y  ]+
						pixel2[x-1][y+1]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<2040)
						pixel[x][y]=0;
				}
			}
		}

		y=0;
		for (x=1; x<xe-1; x++) {
			//white dilate upper row
			if (pixel2[x-1][y  ]+pixel2[x  ][y  ]+pixel2[x+1][y  ]+
				pixel2[x-1][y+1]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<1530)
				pixel[x][y]=0;
		}

		y=ye-1;
		for (x=1; x<xe-1; x++) {
			//white dilate lower row
			if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x+1][y-1]+
				pixel2[x-1][y  ]+pixel2[x  ][y  ]+pixel2[x+1][y  ]<1530)
				pixel[x][y]=0;
		}

		x=0;
		for (y=1; y<ye-1; y++) {
			//white dilate left column
			if (pixel2[x  ][y-1]+pixel2[x+1][y-1]+pixel2[x  ][y  ]+
				pixel2[x+1][y  ]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<1530)
				pixel[x][y]=0;
		}

		x=xe-1;
		for (y=1; y<ye-1; y++) {
			//white dilate right column
			if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x-1][y  ]+
				pixel2[x  ][y  ]+pixel2[x-1][y+1]+pixel2[x  ][y+1]<1530)
				pixel[x][y]=0;
		}

		x=0; //upper left corner
		y=0;
		if (pixel2[x  ][y  ]+pixel2[x+1][y  ]+pixel2[x  ][y+1]+pixel2[x+1][y+1]<1020)
			pixel[x][y]=0;
			x=xe-1; //upper right corner
		//y=0;
		if (pixel2[x-1][y  ]+pixel2[x  ][y  ]+pixel2[x-1][y+1]+pixel2[x  ][y+1]<1020)
			pixel[x][y]=0;

		x=0; //lower left corner
		y=ye-1;
		if (pixel2[x  ][y-1]+pixel2[x+1][y-1]+pixel2[x  ][y  ]+pixel2[x+1][y  ]<1020)
			pixel[x][y]=0;

		x=xe-1; //lower right corner
		y=ye-1;
		if (pixel2[x-1][y-1]+pixel2[x  ][y-1]+pixel2[x-1][y  ]+pixel2[x  ][y  ]<1020)
			pixel[x][y]=0;

		//put result
		for (x=0; x<xe; x++) {
			for (y=0; y<ye; y++){
				//pixel2[x][y]=pixel[x][y];
				ip.putPixel(x,y, pixel[x][y]);
			}
		}
	}



	ImagePlus duplicateImage(ImagePlus img1, boolean calibrated) {
		ImageStack stack1 = img1.getStack();
		int n = stack1.getSize();
		ImageStack stack2 = img1.createEmptyStack();
		float[] ctable = img1.getCalibration().getCTable();
		try {
			for (int i=1; i<=n; i++) {
				ImageProcessor ip1 = stack1.getProcessor(i);
				ImageProcessor ip2 = ip1.duplicate();
				if (calibrated) {
					ip2.setCalibrationTable(ctable);
					ip2 = ip2.convertToFloat();
				}
				stack2.addSlice(stack1.getSliceLabel(i), ip2);
			}
		}
		catch(OutOfMemoryError e) {
			stack2.trim();
			stack2 = null;
			return null;
		}
		ImagePlus img2 =  new ImagePlus("Dilated", stack2);
		return img2;
	}

	ImagePlus replicateImage(ImagePlus img1, boolean calibrated, int n) {
		ImageProcessor ip1 = img1.getProcessor();
		ImageStack stack2 = img1.createEmptyStack();
		float[] ctable = img1.getCalibration().getCTable();
		try {
			for (int i=1; i<=n; i++) {
				ImageProcessor ip2 = ip1.duplicate();
				if (calibrated) {
					ip2.setCalibrationTable(ctable);
					ip2 = ip2.convertToFloat();
				}
				stack2.addSlice(null, ip2);
			}
		}
		catch(OutOfMemoryError e) {
			stack2.trim();
			stack2 = null;
			return null;
		}
		ImagePlus img2 =  new ImagePlus("Dilated", stack2);
		return img2;
	}

}

