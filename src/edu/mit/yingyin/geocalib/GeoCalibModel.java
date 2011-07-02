package edu.mit.yingyin.geocalib;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import edu.mit.yingyin.util.FileUtil;

public class GeoCalibModel {

  /**
   * enum to indicate image type. POROJECTOR is for image projected from
   * projector to the tabletop. CAMERA is for image captured by the camera.
   * 
   * @author Ying Yin
   */
  public enum CalibImageType {
    PROJECTOR, CAMERA
  };

  private static final int NUM_ROW = 6;
  private static final int NUM_COL = 8;

  private List<Point> imagePoints; // list of points in the screen coordinates
  private BufferedImage bi;
  private String ptsFileName;

  public GeoCalibModel(String imagePath) {
    imagePoints = new ArrayList<Point>();

    try {
      bi = ImageIO.read(new File(imagePath));
    } catch (IOException e) {
      System.err.println(imagePath);
      e.printStackTrace();
    }
    
    ptsFileName = FileUtil.setExtension(imagePath, "pts");
  }

  public void removeLastPoint() {
    if (!imagePoints.isEmpty())
      imagePoints.remove(imagePoints.size() - 1);
  }

  public void addImagePoint(Point p) {
    imagePoints.add(p);
    System.out.println("Added point:" + p);
  }

  /**
   * Use the first three clicked points to calculate the rest points.
   * 
   * 0---1 
   * | 
   * 2
   */
  public void createPoints() {
    int width = imagePoints.get(1).x - imagePoints.get(0).x;
    int height = imagePoints.get(2).y - imagePoints.get(0).y;
    int x0 = imagePoints.get(0).x;
    int y0 = imagePoints.get(0).y;

    imagePoints.clear();

    for (int i = 0; i < NUM_ROW; i++)
      for (int j = 0; j < NUM_COL; j++) {
        Point p = new Point(x0 + width * j, y0 + height * i);
        imagePoints.add(p);
      }
  }

  /**
   * Get the list of image points
   * 
   * @return the list of points which are instantiated from the points in the
   *         data model
   */
  public List<Point> getImagePoints() {
    List<Point> pts = new ArrayList<Point>(imagePoints.size());

    for (Point p : imagePoints) {
      Point newPoint = new Point(p);
      pts.add(newPoint);
    }
    return pts;
  }

  public void saveImagePoints() {
    PrintStream ps = null;

    try {
      ps = new PrintStream(new FileOutputStream(ptsFileName));
      for (Point p : imagePoints) {
        ps.println(p.x + " " + p.y);
      }
      System.out.println("Written to file.");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    if (ps != null)
      ps.close();
  }

  public BufferedImage getImage() { return bi; }

  public void clearPoints() { imagePoints.clear(); }
}
