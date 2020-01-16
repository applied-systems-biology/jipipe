package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

//Binary Dilate 4-connected without merging by Gabriel Landini, G.Landini@bham.ac.uk
//v2: 22/Nov/2003 16 bit labels
//v2.1 17/Apr/2004 minimal speed increase
//v2.2 9/Nov/2011 process borders correctly
//v2.3 9/Nov/2011 4conn seeds correctly labelled


public class BinaryDilateNoMerge4_ implements PlugInFilter {
	protected boolean doIwhite;
	protected boolean doIlabel;
	protected int iterations;

	public int setup(String arg, ImagePlus imp) {
		ImageStatistics stats;
		stats=imp.getStatistics();
		if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return DONE;
		}

		if (arg.equals("about"))
			{showAbout(); return DONE;}
		GenericDialog gd = new GenericDialog("BinaryDilateNoMerge4", IJ.getInstance());
		gd.addMessage("Binary dilation without merging v2.3");
		gd.addMessage("4-connected");
		gd.addMessage("This is really slow...");
		gd.addNumericField("Iterations (-1=all)",-1,0);
		gd.addCheckbox("White foreground",true);
		gd.addCheckbox("Label domains",false);


		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;
		iterations = (int) gd.getNextNumber();
		doIwhite = gd.getNextBoolean ();
		doIlabel = gd.getNextBoolean ();
		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		long start = System.currentTimeMillis();
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y, color, ncols=0,pass=0, count, xp1, xm1, yp1, ym1, sumcurr;
		int [][] pixel = new int [xe][ye];
		int [][] pixel2 = new int [xe][ye];
		int [][] pixel0 = new int [xe+2][ye+2];

		boolean ok;

		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel0[x+1][y+1]=ip.getPixel(x,y);//original
			}
		}

		dolabel(pixel0, xe+2, ye+2);

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
			for (color=1;color<=ncols;color++){
				if (currp[color-1]>0){
					IJ.showStatus("Dilating: "+ pass + "  Label: "+ color);
					count=0;
					for (y=1; y<ye-1; y++) {
						ym1=y-1;
						yp1=y+1;
						for (x=1; x<xe-1; x++) {
							if (pixel2[x][y]==0){ //empty
								xm1=x-1;
								xp1=x+1;
//								if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xp1][y]+pixel2[xm1][y]+
//								   pixel2[xp1][yp1]+pixel2[xm1][ym1]+pixel2[xp1][ym1]+pixel2[xm1][yp1]>0){ //occupied neighbour
								if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xp1][y]+pixel2[xm1][y]>0){ //occupied neighbour

									ok = true;
									if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color))     ok = false;
									if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color))     ok = false;
									if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))     ok = false;
									if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))     ok = false;

									if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color))     ok = false;
									if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color))     ok = false;
									if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color))     ok = false;
									if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color))     ok = false;

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
					// process top border
					y=0;
					yp1=y+1;
					for(x=1;x<xe-1;x++){
						if (pixel2[x][y]==0) { //empty and inside mask
							xm1=x-1;
							xp1=x+1;
							if (pixel2[x][yp1]+pixel2[xp1][y]+pixel2[xm1][y]>0){ //occupied neighbour

								ok = true;
								if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
								if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;
								if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;

								if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
								if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;
								if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;

								if (ok){
									pixel[x][y]=color;
								    count++;
								}
							}
						}
					}

					// process bottom border
					y=ye-1;
					ym1=y-1;
					for(x=1;x<xe-1;x++){
						if (pixel2[x][y]==0) { //empty and inside mask
							xm1=x-1;
							xp1=x+1;
							if (pixel2[x][ym1]+pixel2[xp1][y]+pixel2[xm1][y]>0){ //occupied neighbour
								ok = true;
								if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
								if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;
								if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;

								if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
								if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;
								if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;

								if (ok){
									pixel[x][y]=color;
								    count++;
								}
							}
						}
					}

					// process left border
					x=0;
					xp1=x+1;
					for(y=1;y<ye-1;y++){
						if (pixel2[x][y]==0) { //empty and inside mask
							ym1=y-1;
							yp1=y+1;
							if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xp1][y]>0){ //occupied neighbour
								ok = true;
								if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
								if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
								if((pixel2[xp1][y]>0) && (pixel2[xp1][y]!=color))  ok = false;

								if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
								if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
								if((pixel[xp1][y]>0) && (pixel[xp1][y]!=color)) ok = false;

								if (ok){
									pixel[x][y]=color;
								    count++;
								}
							}
						}
					}


					// process right border
					x=xe-1;
					xm1=x-1;
					for(y=1;y<ye-1;y++){
						if (pixel2[x][y]==0) { //empty and inside mask
							ym1=y-1;
							yp1=y+1;
							if (pixel2[x][yp1]+pixel2[x][ym1]+pixel2[xm1][y]>0){ //occupied neighbour
								ok = true;
								if((pixel2[x][yp1]>0) && (pixel2[x][yp1]!=color)) ok = false;
								if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;
								if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;

								if((pixel[x][yp1]>0) && (pixel[x][yp1]!=color)) ok = false;
								if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;
								if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;

								if (ok){
									pixel[x][y]=color;
								    count++;
								}
							}
						}
					}

					// process top left pixel
					if (pixel2[0][0]==0) { //empty and inside mask
						if (pixel2[0][1]+pixel2[1][0]>0){ //occupied neighbour
							ok = true;
							if((pixel2[0][1]>0) && (pixel2[0][1]!=color)) ok = false;
							if((pixel2[1][0]>0) && (pixel2[1][0]!=color))  ok = false;

							if((pixel[0][1]>0) && (pixel[0][1]!=color)) ok = false;
							if((pixel[1][0]>0) && (pixel[1][0]!=color)) ok = false;

							if (ok){
								pixel[0][0]=color;
								 count++;
							}
						}
					}

					// process top right pixel
					x=xe-1;
					xm1= x-1;
					if (pixel2[x][0]==0) { //empty and inside mask
						if (pixel2[xm1][0]+pixel2[x][1]>0){ //occupied neighbour
							ok = true;
							if((pixel2[xm1][0]>0) && (pixel2[xm1][0]!=color)) ok = false;
							if((pixel2[x][1]>0)   && (pixel2[x][1]!=color)) ok = false;

							if((pixel[xm1][0]>0) && (pixel[xm1][0]!=color)) ok = false;
							if((pixel[x][1]>0)   && (pixel[x][1]!=color)) ok = false;

							if (ok){
								pixel[x][0]=color;
								 count++;
							}
						}
					}

					// process bottom left pixel
					y=ye-1;
					ym1=y-1;
					if (pixel2[0][y]==0) { //empty and inside mask
						if (pixel2[0][ym1]+pixel2[1][y]>0){ //occupied neighbour
							ok = true;
							if((pixel2[0][ym1]>0) && (pixel2[0][ym1]!=color)) ok = false;
							if((pixel2[1][y]>0) && (pixel2[1][y]!=color)) ok = false;

							if((pixel[0][ym1]>0) && (pixel[0][ym1]!=color)) ok = false;
							if((pixel[1][y]>0) && (pixel[1][y]!=color)) ok = false;

							if (ok){
								pixel[0][y]=color;
								 count++;
							}
						}
					}

					// process bottom right pixel
					y=ye-1;
					ym1=y-1;
                                        x=xe-1;
                                        xm1=x-1;
					if (pixel2[x][y]==0) { //empty and inside mask
						if (pixel2[xm1][y]+pixel2[x][ym1]>0){ //occupied neighbour
							ok = true;
							if((pixel2[xm1][y]>0) && (pixel2[xm1][y]!=color))  ok = false;
							if((pixel2[x][ym1]>0) && (pixel2[x][ym1]!=color)) ok = false;

							if((pixel[xm1][y]>0) && (pixel[xm1][y]!=color)) ok = false;
							if((pixel[x][ym1]>0) && (pixel[x][ym1]!=color)) ok = false;

							if (ok){
								pixel[x][y]=color;
								 count++;
							}
						}
					}

					//put result
					for (x=0; x<xe; x++) {
						for (y=0; y<ye; y++) {
							pixel2[x][y]=pixel[x][y];
							//ip.putPixel(x,y, pixel[x][y]);
						}
					}
					currp[color-1]=count;
				}
			}
			if (pass==iterations)
				break;
			sumcurr=0;
			for (color=0;color<ncols;color++){
				sumcurr+=currp[color];
			}
			//System.out.println("Debug: summ: "+sumcurr);

		}//while

//		//binarise
		if (doIwhite){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255*pixel2[x][y]);
			}
		}
		else{
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255-(255*pixel2[x][y]));
			}
		}

		if (doIlabel){
			IJ.run("New...", "name='Domains' type='16-bit' fill='Black' width="+xe+" height="+ye);
			ImagePlus imp2 = WindowManager.getCurrentImage();
			ImageProcessor ip2 = imp2.getProcessor();
			for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++)
						ip2.putPixel(x,y,pixel2[x][y]);
			}
			IJ.resetMinAndMax();
		}

		IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");

	}

	void dolabel( int [][] pixel0, int xe, int ye){
		int [] xd = {1,0,-1,0};
		int [] yd = {0,1,0,-1};
		int [] g = {3,0,1,2};

		int x=0, y=0, x1, y1, q, uy, dy, rx, lx, fig, py, px, newcol;
		boolean b;

		if (doIwhite==false){
			for(y=1;y<ye-1;y++) {
				for(x=1;x<xe-1;x++)
					pixel0[x][y] = 255 - pixel0[x][y];// convert to 16 bit
			}
		}

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
					if(pixel0[x+1][y]+pixel0[x][y+1]==0){
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
										if (pixel0[x][y+1]!=65535){
											break;
										}
									}
								}
							}
							else
								q=(q+1)%4;
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
										if(pixel0[px][py-1]==65534 ||
										   pixel0[px-1][py]==65534 ||
										   pixel0[px+1][py]==65534 ||
										   pixel0[px][py+1]==65534) {
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
										   pixel0[px][py+1]==65534 ||
										   pixel0[px][py-1]==65534 ||
										   pixel0[px-1][py]==65534) {
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
	}

	void showAbout() {
		IJ.showMessage("About BinaryDilateNoMerge4_...",
		"BinaryDilateNoMerge4_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for morphological dilation (4 neighbours) of a binary image\n"+
		"without merging.\n"+
		"Dilates the entire image, (processes borders Ok) and can label the domains\n"+
		"using 65530 different colours (1 to 65531).\n"+
		"The algorithm is quite slow, so please let me know of any speedups!");
	}

}