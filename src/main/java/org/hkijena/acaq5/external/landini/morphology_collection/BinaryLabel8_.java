package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

//Binary Label8 by Gabriel Landini, G.Landini@bham.ac.uk
//v2: 22/Nov/2003 16 bit labels

public class BinaryLabel8_ implements PlugInFilter {
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
		GenericDialog gd = new GenericDialog("BinaryLabel8", IJ.getInstance());
		gd.addMessage("Label binary particles");
		gd.addMessage("8-connected");
		gd.addCheckbox("White foreground",true);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;
		doIwhite = gd.getNextBoolean ();
		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		//long start = System.currentTimeMillis();
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y;
		int [][] pixel0 = new int [xe+2][ye+2];


		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel0[x+1][y+1]=ip.getPixel(x,y);//original
			}
		}

		dolabel(pixel0, xe+2, ye+2);

		//System.out.println("Debug: labels: "+ncols);
		//IJ.run("New...", "name='Labelled' type='16-bit' fill='Black' width="+xe+" height="+ye);
		IJ.newImage("Labelled", "16-bit Black",xe, ye, 1);
		ImagePlus imp2 = WindowManager.getCurrentImage();
		ImageProcessor ip2 = imp2.getProcessor();
		for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
				ip2.putPixel(x,y,pixel0[x+1][y+1]);
		}

		IJ.resetMinAndMax();
		//IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");

	}

	void dolabel( int [][] pixel0, int xe, int ye){
		int [] xd = {1,1,1,0,-1,-1,-1,0};
		int [] yd = {-1,0,1,1,1,0,-1,-1};
		int [] g = {6,0,0,2,2,4,4,6};

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
		if (fig > 65530) IJ.log("More than 65530 particles found, the labels were recycled.");
	}

	void showAbout() {
		IJ.showMessage("About BinaryLabel8_...",
		"BinaryLabel8_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for labelling particles (8 neighbours) in a binary image\n"+
		"Can label up to 65530 particles in a unique colour (from 1 to 65531),\n"+
		"after that, the colours are recycled.\n"+
		"The output is a new 16 bit greyscale image with re-scaled brightness.");
	}
}