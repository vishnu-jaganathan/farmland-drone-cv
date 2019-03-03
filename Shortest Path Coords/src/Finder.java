import java.util.ArrayList;
import java.util.HashMap;

public class Finder {
	
	public static class Coordinate{
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
	
	public static ArrayList<Coordinate> pathFinder(ArrayList<Coordinate> points, Coordinate curr){
		double minLength = Double.MAX_VALUE;
		ArrayList<Coordinate> result = new ArrayList<Coordinate>();
		
		for(int i = 0; i < points.size(); i++) {
			ArrayList<Coordinate> temp = new ArrayList<Coordinate>();
			Coordinate elem = points.get(i);
			ArrayList<Coordinate> pointsLeft = new ArrayList<Coordinate>();
			pointsLeft.addAll(points.subList(0, i));
			pointsLeft.addAll(points.subList(i+1, points.size()));
			
			HashMap<String, ArrayList<Coordinate>> dp = new HashMap<String, ArrayList<Coordinate>>();
			double length = curr.distance(elem) + helper(pointsLeft, elem, temp, dp);
			
			if(length < minLength) {
				minLength = length;
				result = temp;
				result.add(0, elem);
			}
		}
		return result;
	}
	public static double helper(ArrayList<Coordinate> points, Coordinate prev, ArrayList<Coordinate> order, HashMap<String, ArrayList<Coordinate>> dp) {
		if(points.size() == 0) {
			return 0;
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
				double len = helper(pointsLeft, elem, tempPath, dp);
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
	public static void main(String[] args) {
		
		
		Coordinate begin = new Coordinate(2.0, 2.0, 2.0);
		Coordinate one = new Coordinate(2.0, 7.0, 2.0);
		Coordinate two = new Coordinate(2.0, 3.0, 2.0);
		Coordinate three = new Coordinate(3.0, 3.0, 3.0);
		
		ArrayList<Coordinate> arr = new ArrayList<>();
		arr.add(one);
		arr.add(two);
		arr.add(three);
		
		ArrayList<Coordinate> result = pathFinder(arr, begin);
		
		for(Coordinate c : result) {
			System.out.println(c.latitude);
			System.out.println(c.longitude);
			System.out.println(c.altitude);
			System.out.println("___________");
		}
	}
}
