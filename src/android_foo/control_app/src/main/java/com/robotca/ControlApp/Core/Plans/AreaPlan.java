package com.robotca.ControlApp.Core.Plans;

import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.Fragments.MapFragment;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Random;

public class AreaPlan extends RobotPlan {

    private final Random random;
    private GeoPoint robotPosition;
    private ArrayList<GeoPoint> areaPoints;
    private ArrayList<Double> results;

    MapFragment mapFragment;

    /**
     * Creates an AreaPlan with the specified area.
     */
    public AreaPlan() {
        //this.robotPosition = robotPosition;
        //areaPoints = (ArrayList<GeoPoint>) area.getPoints();
        random = new Random();
        results = new ArrayList<>();
    }

    /**
     * @return The ControlMode for this RobotPlan
     */
    @Override
    public ControlMode getControlMode() {
        return ControlMode.Area;
    }

    @Override
    public void start(final RobotController controller) throws Exception {

        areaPoints = mapFragment.getAreaPoints();

        double robotLat = robotPosition.getLatitude();
        double robotLon = robotPosition.getLongitude();

        while (!isInterrupted()) {

            // Calculate distance from robot to all points
            for (int i = 0; i < areaPoints.size() - 1; i++) {
               double lon = areaPoints.get(i).getLongitude();
               double lat = areaPoints.get(i).getLatitude();

               double distance = MapFragment.computeDistanceToKilometers(lat, lon, robotLat, robotLon);

               results.add(distance);
            }

            double minValue = results.get(0);

            // Calculate closest point to robot
            for (int i = 0; i < results.size() - 1; i++) {
                if (minValue > results.get(i)) {
                    minValue = results.get(i);
                }
            }

            // Get closest point, previous point and next point to closest point
            GeoPoint first = areaPoints.get(results.indexOf(minValue));
            GeoPoint second = areaPoints.get(results.indexOf(minValue) - 1);
            GeoPoint third = areaPoints.get(results.indexOf(minValue) + 1);

            results.clear();

            // Calculate distance from closest point to previous point and next point
            double distanceSecond = MapFragment.computeDistanceToKilometers(second.getLatitude(), second.getLongitude(), first.getLatitude(), first.getLongitude());
            double distanceThird = MapFragment.computeDistanceToKilometers(third.getLatitude(), third.getLongitude(), first.getLatitude(), first.getLongitude());
            double distBetweenPoints;
            double A = (double) 0;

            // Calculate angle between robot and the two points
            if (distanceSecond < distanceThird || distanceSecond == distanceThird) {
                distBetweenPoints = MapFragment.computeDistanceToKilometers(first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude());
                A = Math.acos((Math.pow(minValue, 2) + Math.pow(distanceSecond, 2) - Math.pow(distBetweenPoints, 2)) / (2 * minValue * distanceSecond));
            } else if (distanceSecond > distanceThird) {
                distBetweenPoints = MapFragment.computeDistanceToKilometers(first.getLatitude(), first.getLongitude(), third.getLatitude(), third.getLongitude());
                A = Math.acos((Math.pow(minValue, 2) + Math.pow(distanceThird, 2) - Math.pow(distBetweenPoints, 2)) / (2 * minValue * distanceThird));
            }

            A = A * 180 / Math.PI;

            // If angle gets to big, change direction
            if (A > 175) {
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);

                long delay = (long) (2000 * (1 + random.nextFloat()));
                controller.publishVelocity(0, 0, .75);
                waitFor(delay);
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
            } else {
                controller.publishVelocity(.75, 0, 0);
            }
        }
    }
}
