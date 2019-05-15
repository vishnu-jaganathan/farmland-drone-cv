package com.example.vishn.postflightprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    //variable of outliers points to be visited
    static ArrayList<double[]> outliers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        outliers = new ArrayList<>();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //button that processes images when clicked
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //pics should be saved in directory called images
                //named as latitude-longitude-altitude.extension
                File root = new File(getFilesDir(), "images");
                if (root.exists()){
                    File[] pics = root.listFiles();

                    //goes through every pic in the directory
                    for(File pic : pics){
                        String filePath = pic.getPath();
                        StringBuilder builder = new StringBuilder(filePath);
                        String path = builder.reverse().toString();
                        //removing file extension
                        int i = path.indexOf('.');
                        path = path.substring(i+1);

                        //getting altitude
                        i = path.indexOf('-');
                        builder = new StringBuilder(path.substring(0, i));
                        double altitude = Double.parseDouble(builder.reverse().toString());
                        path = path.substring(i+1);

                        //getting longitude
                        i = path.indexOf('-');
                        builder = new StringBuilder(path.substring(0, i));
                        double longitude = Double.parseDouble(builder.reverse().toString());
                        path = path.substring(i+1);

                        //getting latitude
                        i = path.indexOf('-');
                        builder = new StringBuilder(path.substring(0, i));
                        double latitude = Double.parseDouble(builder.reverse().toString());
                        path = path.substring(i+1);



                        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

                        //loads opencv
                        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                        OpenCVLoader.initDebug();

                        //converts bitmap to opencv's mat format
                        Mat start = new Mat();
                        Bitmap v32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Utils.bitmapToMat(v32, start);

                        //crops out boundaries of image (battery indicator, etc.)
                        Rect crop = new Rect(200, 100, start.width() - 400, start.height() - 200);

                        Mat img = start.submat(crop);
                        Mat other = new Mat();

                        //converts image to grayscale and turns and light colored areas black
                        //this is done since light colored areas in farmland are generally outliers
                        Imgproc.cvtColor(img, other, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.threshold(other, other, 160, 255, Imgproc.THRESH_TOZERO_INV);


                        //calls boxoutliers to generate a list of outliers of that image and add it to our global variable
                        boxOutliers(other, altitude, latitude, longitude);
                    }
                    //getting inputted home latitude
                    EditText latBox = (EditText)findViewById(R.id.latitude);
                    double homeLat = Double.parseDouble(latBox.getText().toString());

                    //getting inputted home longitude
                    EditText longBox = (EditText)findViewById(R.id.latitude);
                    double homeLong = Double.parseDouble(longBox.getText().toString());

                    //getting inputted home altitude
                    EditText altBox = (EditText)findViewById(R.id.latitude);
                    double homeAlt = Double.parseDouble(altBox.getText().toString());
                    Coordinate loc = new Coordinate(homeLat, homeLong, homeAlt);

                    ArrayList<Coordinate> res = generateDetailedFlight(loc, loc);
                }
            }
        });
    }

    //comparator compares two double arrays by their last element
    static Comparator<double[]> comp = new Comparator<double[]>() {
        public int compare(double[] a, double[] b) {
            return (int) (b[3] - a[3]);
        }
    };
    // method used to convert outliers in the picture to a list of lat,long,alt coordinates
    // also draws boxes around the original pictures
    public static void boxOutliers(Mat pic, double altitude, double latitude, double longitude) {

        //calculates ground width/height in meters with formula
        double groundWidthMeters = 2.74 * altitude / 3.0;
        double groundHeightMeters = 3.79 * altitude / 3.0;
        //calculates ground distance in meters per pixel assuming 690 x 720 image
        double xMetersPP = groundWidthMeters / 960.0;
        double yMetersPP = groundHeightMeters / 720.0;



        //visited array for bfs
        boolean[][] visited = new boolean[pic.height()][pic.width()];

        ArrayList<int[]> results = new ArrayList<int[]>();

        //bfs to extract largest contigious regions of interest
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
                    //if outlier is large enough, it is added to list
                    if(right - left > 50 && bottom - top > 50){
                        int[] temp = {right, left, top, bottom};
                        results.add(temp);
                    }
                }
            }
        }


        //here is where results are processed and bounding rectangles are drawn
        for(int i = 0; i < results.size() && i < 10; i++) {
            int[] pts = results.get(i);

            //find altitude neccessary to capture whole outlier
            double altW = (pts[3]-pts[2])*xMetersPP * 3.0 / 2.74;
            double altH = (pts[0]-pts[1])*yMetersPP * 3.0 / 3.79;
            double requiredAlt = Math.max(altH, altW);

            //find cffset from center and converts from pixels to meters to degrees
            double xOffset = (pts[2] + pts[3])/2 - (960/2);
            xOffset *= xMetersPP;
            xOffset /= 111111.0;
            double yOffset = (pts[1] + pts[0])/2 - (720/2);
            yOffset *= yMetersPP;
            yOffset /= 111111.0;

            //creates double array of new lat, long, alt for the drone to go to
            double area = (pts[3] - pts[2])*(pts[0] - pts[1]);
            double[] result = {latitude + xOffset, longitude + yOffset, requiredAlt, area};
            outliers.add(result);





        }
    }

    //finds best path through top 10 outliers constrained by a start and end point
    public static ArrayList<Coordinate> generateDetailedFlight(Coordinate curr, Coordinate home){
        Collections.sort(outliers, comp);
        ArrayList<Coordinate> topTenOutliers = new ArrayList<>();
        for(int i = 0; i < 10; i++){
            double[] temp = outliers.get(i);
            Coordinate temp2 = new Coordinate(temp[0], temp[1], temp[2]);
            topTenOutliers.add(temp2);
            Log.d("outliers:", "starts here");
            Log.d("outliers:", "("+temp[0]+","+temp[1]+","+temp[2]+")");
        }

        ArrayList<Coordinate> efficientOrder = pathFinder(topTenOutliers, curr, home);

        return efficientOrder;


    }

    //tries out all combinations of ordering and uses dynamic programming to speed up the search process
    //at each stage, a for loop to choose some coordinate and continue recursion returning up shortest length
    //uses dp hashmap which caches the optimal path for a current point and a set of points remaining,
    //such that subproblems are not recomputed
    //this turns factorial time of trying all combos into exponential time
    //return the optimal one
    public static ArrayList<Coordinate> pathFinder(ArrayList<Coordinate> points, Coordinate curr, Coordinate endPoint){
        double minLength = Double.MAX_VALUE;
        ArrayList<Coordinate> result = new ArrayList<Coordinate>();

        for(int i = 0; i < points.size(); i++) {
            ArrayList<Coordinate> temp = new ArrayList<Coordinate>();
            Coordinate elem = points.get(i);
            ArrayList<Coordinate> pointsLeft = new ArrayList<Coordinate>();
            pointsLeft.addAll(points.subList(0, i));
            pointsLeft.addAll(points.subList(i+1, points.size()));

            HashMap<String, ArrayList<Coordinate>> dp = new HashMap<String, ArrayList<Coordinate>>();
            double length = curr.distance(elem) + helper(pointsLeft, elem, endPoint, temp, dp);

            if(length < minLength) {
                minLength = length;
                result = temp;
                result.add(0, elem);
            }
        }
        return result;
    }

    //helper method for recursion of pathFinder
    public static double helper(ArrayList<Coordinate> points, Coordinate prev, Coordinate endPoint, ArrayList<Coordinate> order, HashMap<String, ArrayList<Coordinate>> dp) {
        if(points.size() == 0) {
            return prev.distance(endPoint);
        }
        double minLength = Double.MAX_VALUE;
        Coordinate minElem = null;
        ArrayList<Coordinate> minPath = new ArrayList<Coordinate>();



        for(int i = 0; i < points.size(); i++) {
            Coordinate elem = points.get(i);
            ArrayList<Coordinate> pointsLeft = new ArrayList<Coordinate>();
            pointsLeft.addAll(points.subList(0, i));
            pointsLeft.addAll(points.subList(i+1, points.size()));
            String key = "("+elem.latitude+","+elem.longitude+","+elem.altitude+")_";
            for(Coordinate c : pointsLeft) {
                key += "("+c.latitude+","+c.longitude+","+c.altitude+")";
            }

            if(!dp.containsKey(key)) {
                ArrayList<Coordinate> tempPath = new ArrayList<Coordinate>();
                double len = helper(pointsLeft, elem, endPoint, tempPath, dp);
                Coordinate dummy = new Coordinate(len, 0, 0);
                tempPath.add(0, dummy);
                dp.put(key, tempPath);
            }
            double length = prev.distance(elem) + dp.get(key).get(0).latitude;

            if(length < minLength) {
                minLength = length;
                ArrayList<Coordinate> retrieved = dp.get(key);
                minPath = new ArrayList(retrieved.subList(1, retrieved.size()));
                minElem = elem;
            }
        }
        order.add(minElem);
        order.addAll(minPath);

        return minLength;

    }

    //Coordinate class has fields lat, long, and alt. Also can calculate the distance between two inputted coords
    static class Coordinate{
        public Coordinate(double latitude, double longitude, double altitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
        }
        public double distance(Coordinate other) {
            return (this.latitude - other.latitude)*(this.latitude - other.latitude) +
                    (this.longitude - other.longitude)*(this.longitude - other.longitude) +
                    (this.altitude - other.altitude)*(this.altitude - other.altitude);
        }
        public double latitude;
        public double longitude;
        public double altitude;
    }



}
