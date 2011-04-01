package edu.mit.yingyin.camera;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;

import webcam.IWebcamDriver;
import yingyin.webcam.gui.CameraView;
import edu.mit.yingyin.image.ImageConvertUtils;

public class CameraModel {
  
  private class SavingThread extends Thread {
    private String imagePrefix;
    private String imageType;
    private int index = 0;
    
    public SavingThread(String imagePrefix, String imageType) {
      this.imagePrefix = imagePrefix;
      this.imageType = imageType;
    }
    
    public void run() {
      System.out.println("Started continuous recording");
      while(continuousRecording) {
        String fileName = String.format("%s%04d.%s", imagePrefix, index, 
            imageType);
        index++;         
        try {
          ImageIO.write(blockingQueue.take(), "PNG", new File(fileName));
        } catch (IOException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      System.out.println("Stopped continuous recording");
    }
  }
	
	private static final int WIDTH = 640;
	private static final int HEIGHT = 480;
	
	private boolean captureStarted = false;
	private boolean continuousRecording = false;
	
	private IntBuffer ib;
	private BufferedImage bi;
	private IWebcamDriver driver = null;
	private CameraView wv = null;
	private Thread captureThread = null;
	private Thread savingThread = null;
	private BlockingQueue<BufferedImage> blockingQueue = 
	    new LinkedBlockingQueue<BufferedImage>();
	
	public CameraModel(IWebcamDriver webcamDriver) {		
		this.driver = webcamDriver;
		driver.initialize(0);
		
		if (driver instanceof CameraDriverFirei) {
			CameraDriverFirei driverFirei = (CameraDriverFirei)driver;
			driverFirei.setAutoProperty(CameraDriverFirei.CameraControl.UB);
			driverFirei.setAutoProperty(CameraDriverFirei.CameraControl.VR);
			driverFirei.setAutoProperty(CameraDriverFirei.CameraControl.BRIGHTNESS);
			driverFirei.setProperty(CameraDriverFirei.CameraControl.SATURATION,90);
			driverFirei.setProperty(CameraDriverFirei.CameraControl.GAIN, 0); //180
			driverFirei.setProperty(CameraDriverFirei.CameraControl.EXPOSURE, 255); //297
		}
	
    ib = driver.allocateImageBuffer();
    bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	}
	
	public IWebcamDriver getDriver() { return driver; }
	
	/**
	 * If the capturing thread is not started, return;
	 * else stop the capturing thread and wait for the thread to stop.
	 */
	public void stopCapture() {
		if(!captureStarted)
			return;
			
		captureStarted = false;
		
		try {
			captureThread.join();
			if(continuousRecording) {
				continuousRecording = false;
				savingThread.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void exit() {	
		driver.cleanUp();
		System.exit(0);
	}
	
	public void startCapture(CameraView _wv) {		
		if(captureStarted) return;
		
		this.wv = _wv;
		captureStarted = true;
		
		captureThread = new Thread(new Runnable() {
			public void run() {
				long totalElapsed = 0;
				long totalFrames = 0;
				
				while (captureStarted) {
					try {
						long startTime = System.nanoTime();
						driver.captureNow(ib, WIDTH, HEIGHT);
						long elapsed = System.nanoTime() - startTime;
						ImageConvertUtils.IntBuffer2BufferedImage(ib, bi);
						totalElapsed += elapsed;
						totalFrames++;
						wv.setImage(bi);	
						if (continuousRecording)
							blockingQueue.offer(bi);
					} catch (Exception e) {
					  e.printStackTrace();
					}
				}
				
				System.out.println("Average capture elapsed time is " + 
				    totalElapsed / (totalFrames * 1000000) + "ms.");
			}
		});
		
		captureThread.start();
	}
	
	/**
	 * Start a thread to continuously save the image to the local directory.
	 * @param dir directory to save the images
	 */
	public void continuousRecording(String imagePrefix, String imageType) {
		continuousRecording = true;
		savingThread = new SavingThread(imagePrefix, imageType);
		savingThread.start();
	}
	
	public void stopRecording() {
	  continuousRecording = false;
	}
	
	public Vector<Integer> getControlRange(
	    CameraDriverFirei.CameraControl control) {
		Vector<Integer> ret = null;
		
		if(driver instanceof CameraDriverFirei)
			ret = ((CameraDriverFirei)driver).queryProperty(control);
		
		return ret;
	}
	
	public Vector<Integer> getControlValue(
	    CameraDriverFirei.CameraControl control) {
		if (driver instanceof CameraDriverFirei)
			return ((CameraDriverFirei)driver).getCurrentProperty(control);
		
		return null;
	}
	
	public void setControlValue(CameraDriverFirei.CameraControl control, 
	                            int value) {
		if(driver instanceof CameraDriverFirei)
			((CameraDriverFirei)driver).setProperty(control, value);
	}
	
	public void increaseExposure() {
	  if (driver instanceof CameraDriverFirefly) {
	    CameraDriverFirefly firefly = (CameraDriverFirefly) driver;
	    firefly.increaseExposure();
	  }
	}
	
	public void decreaseExposure() {
    if (driver instanceof CameraDriverFirefly) {
      CameraDriverFirefly firefly = (CameraDriverFirefly) driver;
      firefly.decreaseExposure();
    }
	}
}
