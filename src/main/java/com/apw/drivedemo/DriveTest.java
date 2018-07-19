package com.apw.drivedemo;

import com.apw.ImageManagement.ImageManager;
import com.apw.apw3.SimCamera;
import com.apw.apw3.TrakSim;
import com.apw.fakefirm.Arduino;
import com.apw.fly2cam.FlyCamera;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DriveTest extends TimerTask {
    public static final int FRAME_RATE_NUMBER = 4;	//4 corresponds to 30fps
    public static final int FPS = 30;

    private JFrame window;
    private TrakSim sim;
	private FlyCamera simcam;
	private ImageManager imagemanager;
	private int nrows, ncols;
	private Arduino driveSys;
	private BufferedImage displayimage;
	private Icon displayicon;
	private JLabel displaylabel;
	private int viewType;

        	public DriveTest(TrakSim sim, FlyCamera simcam, int viewType) {
        		this.sim=sim;
        		this.simcam=simcam;
        		this.viewType = viewType;
				finishInit();
        	}
        	public DriveTest(){
				sim = new TrakSim();
				simcam = new SimCamera();
				simcam.Connect(FRAME_RATE_NUMBER);
				viewType = 1;
				finishInit();
			}
			private void finishInit(){
				imagemanager = new ImageManager(simcam);
        		nrows = imagemanager.getNrows();
				ncols = imagemanager.getNcols();

				window = new JFrame();
				window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				window.setSize(ncols, nrows);
				switch(viewType){
					case 2:
						displayimage = new BufferedImage(ncols, nrows, BufferedImage.TYPE_BYTE_GRAY);
						break;
					case 1:
					case 3:
					case 4:
						displayimage = new BufferedImage(ncols, nrows, BufferedImage.TYPE_INT_RGB);
						break;
				}
				displayicon = new ImageIcon(displayimage);
				displaylabel = new JLabel();
				displaylabel.setIcon(displayicon);
				//window.addMouseListener(this);
				window.add(displaylabel);
				window.setVisible(true);

				System.out.println("**************" + nrows + " " + ncols);
			}
        	public static void main(String[] args) {
        		Timer displayTaskTimer = new Timer();
        		displayTaskTimer.scheduleAtFixedRate(new DriveTest(), new Date(), 1000/FPS);
        	}
			public static void subMain(TrakSim sim, FlyCamera simcam, int viewType){
				Timer displayTaskTimer = new Timer();
				displayTaskTimer.scheduleAtFixedRate(new DriveTest(sim, simcam, viewType), new Date(), 1000/FPS);
			}
        	@Override
	public void run() {
				switch(viewType){
					case 1:
						int[] imagepixels = imagemanager.getRGBRaster();
						int[] displaypixels = ((DataBufferInt) displayimage.getRaster().getDataBuffer()).getData();
						System.arraycopy(imagepixels, 0, displaypixels, 0, imagepixels.length);
						break;
					case 2:
						byte[] Aimagepixels = imagemanager.getMonochromeRaster();
						byte[] Adisplaypixels = ((DataBufferByte) displayimage.getRaster().getDataBuffer()).getData();
						System.arraycopy(Aimagepixels, 0, Adisplaypixels, 0, Aimagepixels.length);
						break;
					case 3:
						byte[] Bimagepixels = imagemanager.getSimpleColorRaster();
						int[] Bdisplaypixels = ((DataBufferInt) displayimage.getRaster().getDataBuffer()).getData();
						int[] simpleRGB = new int[Bimagepixels.length];
						imagemanager.convertSimpleToRGB(Bimagepixels, simpleRGB, Bimagepixels.length);
						System.arraycopy(simpleRGB, 0, Bdisplaypixels, 0, Bimagepixels.length);
						break;
					case 4:
						byte[] Cimagepixels = imagemanager.getBlackWhiteRaster();
						int[] Cdisplaypixels = ((DataBufferInt) displayimage.getRaster().getDataBuffer()).getData();
						int[] CsimpleRGB = new int[Cimagepixels.length];
						imagemanager.convertBWToRGB(Cimagepixels, CsimpleRGB, Cimagepixels.length);
						System.arraycopy(CsimpleRGB, 0, Cdisplaypixels, 0, Cimagepixels.length);
						break;
				}
				System.out.println("Repainting");

				window.repaint();
			}
}
