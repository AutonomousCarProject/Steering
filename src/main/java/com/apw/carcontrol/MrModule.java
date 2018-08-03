package com.apw.carcontrol;

import com.apw.imagemanagement.ImageManagementModule;
import com.apw.sbcio.PWMController;
import com.apw.sbcio.fakefirm.ArduinoIO;
import com.apw.sbcio.fakefirm.ArduinoModule;
import com.apw.speedcon.Constants;
import com.apw.speedcon.Settings;
import com.apw.speedcon.SpeedControlModule;

import com.apw.steering.SteeringModule;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MrModule extends JFrame implements Runnable, KeyListener {

    private ScheduledExecutorService executorService;
    private BufferedImage displayImage, bufferImage;
    private GraphicsDevice graphicsDevice;
    private PWMController driveSys = new ArduinoIO();
    private ArrayList<Module> modules;
    private CarControl control;
    private boolean fullscreen;

    // FIXME breaks if dimensions are not 912x480
    private final int windowWidth = 912;
    private final int windowHeight = 480;

    private MrModule(boolean renderWindow) {
        if (renderWindow) {
            control = new TrakSimControl(driveSys);
            headlessInit();
            setupWindow();
        } else {
            control = null;
            headlessInit();
        }
        
        createModules();
    }
    
    private void headlessInit() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        modules = new ArrayList<>();
        executorService.scheduleAtFixedRate(this, 0, 1000 / 15, TimeUnit.MILLISECONDS);
    }
    
    private void setupWindow() {
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        displayImage = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_RGB);
        bufferImage = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_RGB);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(windowWidth, windowHeight + 25);
        setResizable(true);
        setVisible(true);
        addKeyListener(this);
        setIgnoreRepaint(true);
    }
    private void createModules() {
        modules.add(new ImageManagementModule(windowWidth, windowHeight));
        modules.add(new SpeedControlModule());
        modules.add(new SteeringModule());
        modules.add(new ArduinoModule(driveSys));
      
        for (Module module : modules)
            module.initialize(control);
    }
    
    private void update() {
        if (control instanceof TrakSimControl) {
            ((TrakSimControl) control).cam.theSim.SimStep(1);
        }
        
        control.readCameraImage();
        control.setEdges(getInsets());
        for (Module module : modules) {
            module.update(control);
        }
    }

    private void paint() {
        Graphics g;
        g = this.getGraphics();
        if (!(control instanceof TrakSimControl)) {
            return;
        }

        int[] renderedImage = ((TrakSimControl) control).getRenderedImage();

        if (renderedImage != null) {
            int[] displayPixels =
                ((DataBufferInt) bufferImage.getRaster().getDataBuffer()).getData();
            System.arraycopy(renderedImage, 0, displayPixels, 0, renderedImage.length);
            
            BufferedImage tempImage = displayImage;
            displayImage = bufferImage;
            bufferImage = tempImage;

            g.drawImage(displayImage, 0, 0, getWidth(), getHeight(), null);
        }
        
        for (Module module : modules) {
            module.paint(control, g);
        }
    }
  
    @Override
    public void run() {
        update();
        paint();
    }
    
    public static void main(String[] args) {
        boolean renderWindow = true;
        if(args.length > 0 && args[0].toLowerCase().equals("nosim")) {
            renderWindow = false;
        }
        new MrModule(renderWindow);
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (!(control instanceof TrakSimControl)) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_F) {
            fullscreen = !fullscreen;
            setVisible(false);
            dispose();
            setUndecorated(fullscreen);
            if (fullscreen) {
                graphicsDevice.setFullScreenWindow(this);
                validate();
            } else {
                graphicsDevice.setFullScreenWindow(null);
                setVisible(true);
            }
        }
      
        for (Map.Entry<Integer, Runnable> binding : ((TrakSimControl) control).keyBindings.entrySet()) {
            if (e.getKeyCode() == binding.getKey()) {
                binding.getValue().run();
            }
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {  }
    
    @Override
    public void keyReleased(KeyEvent e) {  }
}
