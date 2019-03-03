import org.opencv.core.Point;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class HelloCV {
	
	public static void main(String[] args) {

		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		
		File f = new File("C:\\Users\\vishn\\Desktop\\to_undistort\\GOPR1921.JPG");
		BufferedImage bi = null;
		
		
		
		
		try {
			bi = ImageIO.read(f);
			
			Mat start = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
			byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
			start.put(0, 0, data);
			
			
			Mat camera = new Mat(3, 3, CvType.CV_64FC1);
            double[] cameraVals = {1.749447157305313340e+03, 0.0, 2.096810232232002818e+03,
                    0.0, 1.747502864738538165e+03, 1.529293823675952126e+03,
                    0.0, 0.0, 1.0};
            camera.put(0, 0, cameraVals);

            Mat coeffs = new Mat(5, 1, CvType.CV_64FC1);
            double[] coeffVals = {-2.584685443772606339e-01, 8.875303650616475637e-02,
                    2.675119325706701626e-04, 9.552281367488971143e-05, -1.565040488561297155e-02};
            coeffs.put(0, 0, coeffVals);

            Mat img = new Mat();

            Imgproc.undistort(start, img, camera, coeffs);
			
			
			Mat other = new Mat();
			
			Size sz = new Size(400, 300);
			
			
			Imgproc.resize(img, img, sz);
			
			
			
			Imgproc.cvtColor(img, other, Imgproc.COLOR_BGR2GRAY);
			
			
			Imgproc.threshold(other, other, 175, 255, Imgproc.THRESH_TOZERO_INV);
			
			
			System.out.println("made it");
			
			
			Mat picToSave = boxOutliers(other, img);
			
			
			File outputfile = new File("C:\\Users\\vishn\\Desktop\\to_undistort\\image.jpg");
	            
	            
	        MatOfByte mob = new MatOfByte();
	        Imgcodecs.imencode(".jpg", picToSave, mob);
	        BufferedImage imgToSave = ImageIO.read(new ByteArrayInputStream(mob.toArray()));
	            
	            
	        ImageIO.write(imgToSave, "jpg", outputfile);
			
			
		}
		catch(Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
		
		System.out.println("done");
		
		
	}
	
	public static Mat boxOutliers(Mat pic, Mat canvas) {
		
		boolean[][] visited = new boolean[pic.height()][pic.width()];
		
		ArrayList<int[]> results = new ArrayList<int[]>();
		
		for(int i = 0; i < pic.height(); i++) {
			for(int j = 0; j < pic.width(); j++) {
				if (pic.get(i, j)[0] == 0 && !visited[i][j]) {
					
                    Queue<Point> q = new LinkedList<>();
                    q.offer(new Point(i, j));
                    visited[i][j] = true;

                    int left = i;
                    int right = i;
                    int top = j;
                    int bottom = j;

                    while (!q.isEmpty()) {
                    	//System.out.println("hi");
                        Point p = q.poll();
                        int x = (int) p.x;
                        int y = (int) p.y;

                        //expanding bounds
                        if (x > right) {
                            right = x;
                        }
                        if (x < left) {
                            left = x;
                        }
                        if (y < top) {
                            top = y;
                        }
                        if (y > bottom) {
                            bottom = y;
                        }


                        //adding neighbors

                        //upper neighbor
                        if (x > 0 && pic.get(x-1, y)[0] == 0 && !visited[x - 1][y]) {
                            visited[x - 1][y] = true;
                            q.offer(new Point(x - 1, y));
                        }

                        //lower neighbor
                        if (x < pic.height() - 1 && pic.get(x+1, y)[0] == 0 && !visited[x + 1][y]) {
                            visited[x + 1][y] = true;
                            q.offer(new Point(x + 1, y));
                        }

                        //left neighbor
                        if (y > 0 && pic.get(x, y-1)[0] == 0 && !visited[x][y - 1]) {
                            visited[x][y - 1] = true;
                            q.offer(new Point(x, y - 1));
                        }

                        //right neighbor
                        if (y < pic.width() - 1 && pic.get(x, y+1)[0] == 0 && !visited[x][y + 1]) {
                            visited[x][y + 1] = true;
                            q.offer(new Point(x, y + 1));
                        }
                    }
                    if(right - left > 50 && bottom - top > 50){
                    	int[] temp = {right, left, top, bottom};
                    	results.add(temp);
                    }
				}
			}
		}
		
		System.out.println(results.size());
		
		for(int i = 0; i < results.size(); i++) {
			int[] pts = results.get(i);
			Point topLeft = new Point(pts[2], pts[1]);
			Point bottomRight = new Point(pts[3], pts[0]);
			Scalar c = new Scalar(0, 0, 255);
			Imgproc.rectangle(canvas, topLeft, bottomRight, c, 3);
		}
		
		
		return canvas;
	}
		
		
}
