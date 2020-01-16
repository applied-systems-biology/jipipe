package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
/**
Greyscale reconstruction, by G.Landini. 30/Oct/2003 for ImageJ

Reconstructs a greyscale image (the "mask" image) based on a "seed"
image.
This is an implementation of the greyscale reconstruction from:

Vincent L. Morphological greyscale reconstuction in Image Analysis:
Applications and efficient algorithms. IEEE Trans Image Proc 2(2)
176-201, 1993.

It is imperative to read Vincent's paper to understand greyscale
reconstruction and its applications.

The original reconstruction algorithm is: iterated geodesic dilations of the
seed UNDER the mask image until stability is reached (the idempotent limit).

Send any improvements or bugs to G.Landini@bham.ac.uk

v2.0 22/12/2008 Rewrite following the guidelines at http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines
     This version computes the result differently. It binary reconstructs the thresholded mask by the thresholded seed
     and keeps the maximum greylevel at which the reconstruction was done.

  Apart from being immensely faster, now it can be called from another plugin without having to show the images. 
  It cannot process stacks anymore, but its use was very limited anyway.

v2.1 4/5/2009 4-connected option, fixed skipping empty extreme of histogram
v2.2 24/5/2009  speed up due to BinaryReconstruct 

*/

public class GreyscaleReconstruct_ implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {

		if (IJ.versionLessThan("1.37f")) return;
		int[] wList = WindowManager.getIDList();

		if (wList==null || wList.length<2) {
			IJ.showMessage("Greyscale Reconstruction", "There must be at least two windows open");
			return;
		}
		String[] titles = new String[wList.length];
		for (int i=0, k=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (null !=imp)
				titles[k++] = imp.getTitle();
		}
		// 1 - Obtain the currently active image if necessary:
		// Erm... no.

                // 2 - Ask for parameters:
		boolean createWindow = true, connect4=false;
		GenericDialog gd = new GenericDialog("Greyscale Reconstruction");
		gd.addMessage("Greyscale Reconstruction v 2.2");
		gd.addChoice("mask i1:", titles, titles[0]);
		gd.addChoice("seed i2:", titles, titles[1]);
		gd.addCheckbox("Create New Window", createWindow);
		gd.addCheckbox("4 connected", false);

		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog
		int i1Index = gd.getNextChoiceIndex();
		int i2Index = gd.getNextChoiceIndex();
		createWindow = gd.getNextBoolean();
		connect4 = gd.getNextBoolean();
		ImagePlus imp1 = WindowManager.getImage(wList[i1Index]);
		ImagePlus imp2 = WindowManager.getImage(wList[i2Index]);

		if (imp1.getStackSize()>1 || imp2.getStackSize()>1) {
			IJ.showMessage("Error", "Stacks not supported");
			return;
		}
		if (imp1.getBitDepth()!=8 || imp2.getBitDepth()!=8) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}
 
		String name = null;

		if (createWindow)
			name = "Reconstructed";

		 // 4 - Execute!
		long start = System.currentTimeMillis();
		Object[] result = exec(imp1, imp2, name, createWindow, connect4);

		// 5 - If all went well, show the image:
		if (null != result) {
			ImagePlus resultImage = (ImagePlus) result[1];
			if (createWindow)
				resultImage.show();
			else {
				imp2.setProcessor(imp2.getTitle(),resultImage.getProcessor());//copy the resultImage into the seed image,
			}
		}
		IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
	}


	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp1, ImagePlus imp2, String new_name, boolean createWindow, boolean connect4) {

		// 0 - Check validity of parameters
		if (null == imp1) return null;
		if (null == imp2) return null;
		if (null == new_name) new_name = imp2.getTitle();
 
		int width  = imp1.getWidth();
		int height = imp1.getHeight();
		ImageProcessor ip1, ip2, ip3, ip4, ip5;
		ImagePlus imp3, imp4, imp5;
		ImageStatistics stats;
		int i, j, x, y, size;
		byte b_0 = (byte) (0 & 0xff);
		byte b_255 = (byte) (255 & 0xff);
		IJ.showStatus("Greyscale Reconstruction...");

		// 1 - Perform the magic
		ip1 = imp1.getProcessor();
		ip2 = imp2.getProcessor();
		stats=imp2.getStatistics();
			
		byte[] pixels1 = (byte[])ip1.getPixels();
		byte[] pixels2 = (byte[])ip2.getPixels();
		size=pixels1.length;
		byte[] pixels3 = new byte[size]; //r
		byte[] pixels4 = new byte[size]; //m
		byte[] pixels5 = new byte[size]; //s
		int[] intHisto = new int[256]; 

		intHisto[255]=stats.histogram[255];

		for (j=254; j>-1; j--) { intHisto[j]=intHisto[j+1]+stats.histogram[j]; } // cumulative histogram the way round

		for (j=0; j<size; j++) { pixels3[j] = b_0; } // set r accumulator to 0

		ip3 = new ByteProcessor(width, height, pixels3, null);
		imp3 = new ImagePlus(new_name, ip3);

		for( i=255;i>-1;i--) {
			if (intHisto[i]>0) {
				System.arraycopy(pixels1, 0, pixels4, 0, size);
				System.arraycopy(pixels2, 0, pixels5, 0, size);
				for (j=0;j<size; j++){
					//Threshold mask
					pixels4[j]=(((int)pixels4[j] & 0xff) < i)?b_0:b_255;

					//Threshold seed
					pixels5[j]=(((int)pixels5[j] & 0xff) < i)?b_0:b_255;
				}

				ip4 = new ByteProcessor(width, height, pixels4, null);
				imp4 = new ImagePlus("_mask", ip4);

				ip5 = new ByteProcessor(width, height, pixels5, null);
				imp5 = new ImagePlus("_seed", ip5);

				BinaryReconstruct_ br = new BinaryReconstruct_();
				/** Careful! the exec method of BinaryReconstruct does not check whether the images are binary !! */ 
				Object[] result = br.exec(imp4, imp5, null, false, true, connect4 ); // the result is returned in imp5 if createWindow is false
				//  exec(ImagePlus imp1, ImagePlus imp2, String new_name, boolean createWindow, boolean whiteParticles boolean connect4) 
				for (j=0; j<size; j++){
					if(((int) pixels5[j] & 0xff)==255) pixels5[j]=(byte)(i & 0xff); // set the thresholded pixels to the greylevel
					if(((int)pixels5[j] & 0xff) > ((int)pixels3[j] & 0xff))  pixels3[j]=pixels5[j]; //keep always the max greylevel
				}
				imp4.close();
				imp5.close();
			}
			IJ.showProgress((double)(255-i)/255);
		}
		imp3.updateAndDraw();
		// 2 - Return the new name and the image
		return new Object[]{new_name, imp3};
	}
}




