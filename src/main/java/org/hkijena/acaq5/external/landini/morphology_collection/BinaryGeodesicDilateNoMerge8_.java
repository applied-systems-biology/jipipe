package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;

//BinaryGeodesicDilateNoMerge8_ by Gabriel Landini, G.Landini@bham.ac.uk..
//modified from BinaryDilateNoMerge8_
// 1.1 process borders too

public class BinaryGeodesicDilateNoMerge8_ implements PlugIn {
	        public void run(String arg) {
		//if (IJ.versionLessThan("1.37f")) return;
		int[] wList = WindowManager.getIDList();

		if (wList==null || wList.length<2) {
			IJ.showMessage("Geodesic Distance", "There must be at least two binary images open.");
			return;
		}
		String[] titles = new String[wList.length];
//		String[] conn = {"8"}; //, "4", "Alternate"};

		for (int i=0, k=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (null !=imp)
				titles[k++] = imp.getTitle();
		}

		// 1 - Obtain the currently active image if necessary:
		// not today

                // 2 - Ask for parameters:
		boolean extended = true, details = false; //, whiteParticles =Prefs.blackBackground;
		GenericDialog gd = new GenericDialog("Geodesic Distance");
		gd.addMessage("BinaryGeodesicDilateNoMerge8  v1.1");
		gd.addMessage("White objects (mask and seed)\non black background only!");
		gd.addMessage("Dilation is 8-connected")
;		gd.addChoice("mask :", titles, titles[0]);
		gd.addChoice("seed :", titles, titles[1]);
		gd.addNumericField("Iterations (-1=all)",-1,0);
//		gd.addChoice("Connectivity :", conn, conn[0]);
		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog
		int i1Index = gd.getNextChoiceIndex();
		int i2Index = gd.getNextChoiceIndex();
		int iterations = (int) gd.getNextNumber();
//		int i3Index = gd.getNextChoiceIndex(); // for the future, when this supports 4-connected as well

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

		if (imp1.isInvertedLut() || imp2.isInvertedLut() ) {
			IJ.showMessage("Error", "No inverted LUTs, please");
			return;
		}

		if ((imp1.getWidth() !=  imp2.getWidth()) || (imp1.getHeight() !=  imp2.getHeight())) {
			IJ.showMessage("Error", "The mask and seed images have different sizes.");
			return;
		}


		ImageStatistics stats;
		// Check images are binary. You must check this before calling the exec method.
		stats = imp1.getStatistics();
		if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount){
			IJ.error("8-bit binary mask image (0 and 255) required.");
			return;
		}

		stats = imp2.getStatistics();
			if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount){
			IJ.error("8-bit binary seed image (0 and 255) required.");
			return;
		}

		 // 4 - Execute!
		Object[] result = exec(imp1, imp2, iterations); 

		// 5 - If all went well, show the image:
		if (null != result) {
			// IJ.log("Result : "+result[0]);
			ImagePlus resultImage = (ImagePlus) result[0];
			resultImage.show(); // it is the seed image that gets processed
		}
	}


	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Warning!: This method does NOT check whether the two input images are binary, this is checked in the setup,
	*  so careful when calling this method from another plugin. Make sure both images are binary!!
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp1, ImagePlus imp2, int iterations) {

		// set options
		IJ.run("Options...", "iterations=1 count=1 black pad edm=Overwrite do=Nothing");
		IJ.run("Colors...", "foreground=white background=black selection=yellow");

		// 0 - Check validity of parameters
		if (null == imp1) return null;
		if (null == imp2) return null;

		ImageProcessor ip1 = imp1.getProcessor(); // the seed 
		ImageProcessor ip2 = imp2.getProcessor(); // the seed

		//IJ.showStatus("Geodesic Distance..."); 
		ip2.snapshot(); //undo
		Undo.setup(Undo.FILTER, imp2);

		long start = System.currentTimeMillis();
		int xe = ip2.getWidth();
		int ye = ip2.getHeight();
		int x, y, color, ncols=0,pass=0, count, xp1, xm1, yp1, ym1, sumcurr;
		int [][] pixel = new int [xe][ye];
		int [][] pixel2 = new int [xe][ye];
		int [][] pixel0 = new int [xe+2][ye+2];
		int [][] mask = new int [xe][ye];
		boolean ok;

		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel0[x+1][y+1]=ip2.getPixel(x,y);//original
				mask[x][y] = ip1.getPixel(x,y);
			}
		}

		dolabel(pixel0, xe+2, ye+2); // find 8-connected particles

		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel2[x][y]=pixel0[x+1][y+1];//original
				pixel[x][y]=pixel2[x][y];
			}
		}

		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				if (pixel[x][y]>ncols)
					ncols=pixel[x][y];
			}
		}
		//System.out.println("Debug: labels: "+ncols);

		int[] currp = new int [ncols];
		//System.out.println("Debug:"+ncols);
		for (color=0;color<ncols;color++){
			currp[color]=1;
		}

		sumcurr=1;
		while (sumcurr>0){
			pass++;
			IJ.showStatus("Dilating: "+ pass);
			for (color=1;color<=ncols;color++){
				if (currp[color-1]>0){
					//IJ.showStatus("Dilating: "+ pass + "  Label: "+ color);
					count=0;
					for (y=1; y<ye-1; y++) {
						ym1=y-1;
						yp1=y+1;
						for (x=1; x<xe-1; x++) {
							 if(mask[x][y]>0){  // if oustside the mask, don't process
								if (pixel2[x][y]==0) { //empty and inside mask
									xm1=x-1;
									xp1=x+1;
									if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xp1][y]+pixel2[xm1][y]+
									   pixel2[xp1][yp1]+pixel2[xm1][ym1]+pixel2[xp1][ym1]+pixel2[xm1][yp1]>0){ //occupied neighbour

										ok = true;
										if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
										if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
										if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;
										if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;
										if((pixel2[xp1][yp1]>0) && (pixel2[xp1][yp1]!=color)) ok = false;
										if((pixel2[xm1][ym1]>0) && (pixel2[xm1][ym1]!=color)) ok = false;
										if((pixel2[xp1][ym1]>0) && (pixel2[xp1][ym1]!=color)) ok = false;
										if((pixel2[xm1][yp1]>0) && (pixel2[xm1][yp1]!=color)) ok = false;

										if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
										if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
										if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;
										if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;
										if((pixel[xp1][yp1]>0) && (pixel[xp1][yp1]!=color)) ok = false;
										if((pixel[xm1][ym1]>0) && (pixel[xm1][ym1]!=color)) ok = false;
										if((pixel[xp1][ym1]>0) && (pixel[xp1][ym1]!=color)) ok = false;
										if((pixel[xm1][yp1]>0) && (pixel[xm1][yp1]!=color)) ok = false;

										if (ok){
											pixel[x][y]=color;
										    count++;
										}
										//else
										//	pixel[x][y]=0;
									}
								}
							}
						}
					}
					// process top border
					y=0;
					yp1=y+1;
					for(x=1;x<xe-1;x++){
						 if(mask[x][y]>0){  // if oustside the mask, don't process
							if (pixel2[x][y]==0) { //empty and inside mask
								xm1=x-1;
								xp1=x+1;
									if (pixel2[x][yp1]+pixel2[xp1][y]+pixel2[xm1][y]+pixel2[xp1][yp1]+pixel2[xm1][yp1]>0){ //occupied neighbour

									ok = true;
									if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
									if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;
									if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;
									if((pixel2[xp1][yp1]>0) && (pixel2[xp1][yp1]!=color)) ok = false;
									if((pixel2[xm1][yp1]>0) && (pixel2[xm1][yp1]!=color)) ok = false;

									if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
									if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;
									if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;
									if((pixel[xp1][yp1]>0) && (pixel[xp1][yp1]!=color)) ok = false;
									if((pixel[xm1][yp1]>0) && (pixel[xm1][yp1]!=color)) ok = false;

									if (ok){
										pixel[x][y]=color;
									    count++;
									}
								}
							}
						}
					}

					// process bottom border
					y=ye-1;
					ym1=y-1;
					for(x=1;x<xe-1;x++){
						 if(mask[x][y]>0){  // if oustside the mask, don't process
							if (pixel2[x][y]==0) { //empty and inside mask
								xm1=x-1;
								xp1=x+1;
								if (pixel2[x][ym1]+pixel2[xp1][y]+pixel2[xm1][y]+pixel2[xp1][ym1]+pixel2[xm1][ym1]>0){ //occupied neighbour
									ok = true;
									if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
									if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;
									if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;
									if((pixel2[xm1][ym1]>0) && (pixel2[xm1][ym1]!=color)) ok = false;
									if((pixel2[xp1][ym1]>0) && (pixel2[xp1][ym1]!=color)) ok = false;

									if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
									if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;
									if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;
									if((pixel[xm1][ym1]>0) && (pixel[xm1][ym1]!=color)) ok = false;
									if((pixel[xp1][ym1]>0) && (pixel[xp1][ym1]!=color)) ok = false;

									if (ok){
										pixel[x][y]=color;
									    count++;
									}
								}
							}
						}
					}

					// process left border
					x=0;
					xp1=x+1;
					for(y=1;y<ye-1;y++){
						 if(mask[x][y]>0){  // if oustside the mask, don't process
							if (pixel2[x][y]==0) { //empty and inside mask
								ym1=y-1;
								yp1=y+1;
								if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xp1][y]+pixel2[xp1][yp1]+pixel2[xp1][ym1]>0){ //occupied neighbour
									ok = true;
									if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
									if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
									if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;
									if((pixel2[xp1][yp1]>0) && (pixel2[xp1][yp1]!=color)) ok = false;
									if((pixel2[xp1][ym1]>0) && (pixel2[xp1][ym1]!=color)) ok = false;

									if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
									if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
									if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;
									if((pixel[xp1][yp1]>0) && (pixel[xp1][yp1]!=color)) ok = false;
									if((pixel[xp1][ym1]>0) && (pixel[xp1][ym1]!=color)) ok = false;

									if (ok){
										pixel[x][y]=color;
									    count++;
									}
								}
							}
						}
					}


					// process right border
					x=xe-1;
					xm1=x-1;
					for(y=1;y<ye-1;y++){
						 if(mask[x][y]>0){  // if oustside the mask, don't process
							if (pixel2[x][y]==0) { //empty and inside mask
								ym1=y-1;
								yp1=y+1;
								if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xm1][y]+pixel2[xm1][ym1]+pixel2[xm1][yp1]>0){ //occupied neighbour
									ok = true;
									if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
									if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
									if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;
									if((pixel2[xm1][ym1]>0) && (pixel2[xm1][ym1]!=color)) ok = false;
									if((pixel2[xm1][yp1]>0) && (pixel2[xm1][yp1]!=color)) ok = false;

									if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
									if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
									if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;
									if((pixel[xm1][ym1]>0) && (pixel[xm1][ym1]!=color)) ok = false;
									if((pixel[xm1][yp1]>0) && (pixel[xm1][yp1]!=color)) ok = false;

									if (ok){
										pixel[x][y]=color;
									    count++;
									}
								}
							}
						}
					}

					// process top left pixel
					 if(mask[0][0]>0){  // if oustside the mask, don't process
						if (pixel2[0][0]==0) { //empty and inside mask
							if (pixel2[0][1]+pixel2[1][0]+pixel2[1][1]>0){ //occupied neighbour
								ok = true;
								if((pixel2[0][1]>0) && (pixel2[0][1]!=color)) ok = false;
								if((pixel2[1][0]>0) && (pixel2[1][0]!=color))  ok = false;
								if((pixel2[1][1]>0) && (pixel2[1][1]!=color)) ok = false;

								if((pixel[0][1]>0) && (pixel[0][1]!=color)) ok = false;
								if((pixel[1][0]>0) && (pixel[1][0]!=color)) ok = false;
								if((pixel[1][1]>0) && (pixel[1][1]!=color)) ok = false;

								if (ok){
									pixel[0][0]=color;
									 count++;
								}
							}
						}
					}

					// process top right pixel
					x=xe-1;
					xm1= x-1;
					if(mask[x][0]>0){  // if oustside the mask, don't process
						if (pixel2[x][0]==0) { //empty and inside mask
							if (pixel2[xm1][0]+pixel2[xm1][1]+pixel2[x][1]>0){ //occupied neighbour
								ok = true;
								if((pixel2[xm1][0]>0) && (pixel2[xm1][0]!=color)) ok = false;
								if((pixel2[xm1][1]>0) && (pixel2[xm1][1]!=color))  ok = false;
								if((pixel2[x][1]>0)   && (pixel2[x][1]!=color)) ok = false;

								if((pixel[xm1][0]>0) && (pixel[xm1][0]!=color)) ok = false;
								if((pixel[xm1][1]>0) && (pixel[xm1][1]!=color)) ok = false;
								if((pixel[x][1]>0)   && (pixel[x][1]!=color)) ok = false;

								if (ok){
									pixel[x][0]=color;
									 count++;
								}
							}
						}
					}

					// process bottom left pixel
					y=ye-1;
					ym1=y-1;
					if(mask[0][y]>0){  // if oustside the mask, don't process
						if (pixel2[0][y]==0) { //empty and inside mask
							if (pixel2[0][ym1]+pixel2[1][ym1]+pixel2[1][y]>0){ //occupied neighbour
								ok = true;
								if((pixel2[0][ym1]>0) && (pixel2[0][ym1]!=color)) ok = false;
								if((pixel2[1][ym1]>0) && (pixel2[1][ym1]!=color))  ok = false;
								if((pixel2[1][y]>0) && (pixel2[1][y]!=color)) ok = false;

								if((pixel[0][ym1]>0) && (pixel[0][ym1]!=color)) ok = false;
								if((pixel[1][ym1]>0) && (pixel[1][ym1]!=color)) ok = false;
								if((pixel[1][y]>0) && (pixel[1][y]!=color)) ok = false;

								if (ok){
									pixel[0][y]=color;
									 count++;
								}
							}
						}
					}

					// process bottom right pixel
					y=ye-1;
					ym1=y-1;
                                        x=xe-1;
                                        xm1=x-1;
					if(mask[x][y]>0){  // if oustside the mask, don't process
						if (pixel2[x][y]==0) { //empty and inside mask
							if (pixel2[xm1][ym1]+pixel2[xm1][y]+pixel2[x][ym1]>0){ //occupied neighbour
								ok = true;
								if((pixel2[xm1][ym1]>0) && (pixel2[xm1][ym1]!=color)) ok = false;
								if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;
								if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;

								if((pixel[xm1][ym1]>0) && (pixel[xm1][ym1]!=color)) ok = false;
								if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;
								if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;

								if (ok){
									pixel[x][y]=color;
									 count++;
								}
							}
						}
					}



					//put result
					for (x=0; x<xe; x++) {
						for (y=0; y<ye; y++) {
							 pixel2[x][y]=pixel[x][y];
							//ip2.putPixel(x,y, pixel[x][y]);
						}
					}
					currp[color-1]=count;
				}
			}
			// IJ.log("Pass: "+pass);
			if (pass==iterations)
				break;
			sumcurr=0;
			for (color=0;color<ncols;color++){
				sumcurr+=currp[color];
			}
			//System.out.println("Debug: summ: "+sumcurr);

		}//while

//		//binarise
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++)
				ip2.putPixel(x,y,255*pixel2[x][y]);
		}

		imp2.updateAndDraw();

		IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds, "+pass+" passes, "+ncols+" seeds.");
		return new Object[]{imp2}; //return the Geodesic image
        }

	void dolabel( int [][] pixel0, int xe, int ye){
		int [] xd = {1,1,1,0,-1,-1,-1,0};
		int [] yd = {-1,0,1,1,1,0,-1,-1};
		int [] g = {6,0,0,2,2,4,4,6};

		int x=0, y=0, x1, y1, q, uy, dy, rx, lx, fig, py, px, newcol;
		boolean b;

//		if (doIwhite==false){
//			for(y=1;y<ye-1;y++) {
//				for(x=1;x<xe-1;x++)
//					pixel0[x][y] = 255 - pixel0[x][y];// convert to 16 bit
//			}
//		}

		for(y=1;y<ye-1;y++) {
			for(x=1;x<xe-1;x++)
				pixel0[x][y]*= 257; // convert to 16 bit
		}

		for(y=0;y<ye;y++){
			pixel0[0][y]=0;
			pixel0[xe-1][y]=0;
		}

		for(x=0;x<xe;x++){
			pixel0[x][0]=0;
			pixel0[x][ye-1]=0;
		}

		fig=0;
		// find first scanned pixel
		for (y=1; y<ye-1; y++){
			for (x=1; x<xe-1; x++){
				if (pixel0[x][y]==65535){
					x1=x;
					y1=y;
					//bounding box
					uy=y;//upper y
					dy=1;//lower y
					rx=1;//right x
					lx=xe-1;//left x

					//single pixel?
					if(pixel0[x+1][y]+pixel0[x][y+1]+pixel0[x-1][y+1]+pixel0[x+1][y+1]==0){
						pixel0[x][y]=(fig  % 65531) + 1; //new colour if labelling
						fig++;
					}
					else{ //go around the edge
						x1=x;
						y1=y;
						pixel0[x][y]=65534;
						q=0;
						while(true){
							if (pixel0[x1+xd[q]][y1+yd[q]]>0){
								x1+=xd[q];
								y1+=yd[q];
								pixel0[x1][y1]=65534;
								//update coordinates of bounding box
								if(x1 > rx) rx = x1;
								if(x1 < lx) lx = x1;
								if(y1 > dy) dy = y1;
								if(y1 < uy) uy = y1;
								q=g[q];

								//check for *that* unique pixel configuration
								if (x1==x){
									if (y1==y){
										if (pixel0[x-1][y+1]!=65535){
											break;
										}
									}
								}
							}
							else
								q=(q+1)%8;
						}

						//label from top to bottom and vice versa until no new
						//pixels are labelled. No need to check all 8 neighbours in
						//each pass, but only the previous 4 in the scanning direction
						b=true;
						while(b){
							b=false;
							//scan bounding box from top left to bottom right
							for(py=uy+1;py<dy;py++) {
								for(px=lx+1;px<rx;px++) {
									if (pixel0[px][py]==65535){
										if(pixel0[px-1][py-1]==65534 ||
										   pixel0[px][py-1]==65534 ||
										   pixel0[px+1][py-1]==65534 ||
										   pixel0[px-1][py]==65534) {
											pixel0[px][py]=65534;
											b=true; // this pixel is connected to the border
										}
									}
								}
							}
							//scan bounding box bottom right to top left
							for(py=dy-1;py>=uy;py--) {
								for(px=rx-1;px>=lx;px--) {
									if (pixel0[px][py]==65535){
										if(pixel0[px+1][py]==65534 ||
										   pixel0[px+1][py+1]==65534 ||
										   pixel0[px][py+1]==65534 ||
										   pixel0[px-1][py+1]==65534) {
											pixel0[px][py]=65534;
											b=true; // this pixel is connected to the border
										}
									}
								}
							}
						}
						newcol = (fig  % 65531) + 1; //new colour if labelling
						//a2 = newcol + 1
						for(py=uy;py<=dy;py++){
							for(px=lx;px<=rx;px++){
								if(pixel0[px][py]== 65534){
									pixel0[px][py]=newcol;
								}
							}
						}
						fig++;
					}
				}
			}
		}
		if (fig > 65530) IJ.log("More than 65530 particles found, some may get merged.");
		// IJ.log("Labelling... "+fig+" blobs");
	}

	void showAbout() {
		IJ.showMessage("About BinaryGeodesicDilateNoMerge8...",
		"BinaryGeodesicDilateNoMerge8_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for morphological geodesic dilation (8 neighbours) of a binary\n"+
		"image without merging.\n"+
		"Dilates the entire image, (processes borders Ok) and can label the domains\n"+
		"using 65530 different colours (1 to 65531).\n"+
		"The algorithm is quite slow, so please let me know of any speedups!");
	}

}
