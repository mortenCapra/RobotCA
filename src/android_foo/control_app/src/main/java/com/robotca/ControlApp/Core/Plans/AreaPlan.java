package com.robotca.ControlApp.Core.Plans;

import android.location.Location;
import android.util.Log;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.Fragments.MapFragment;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Random;

public class AreaPlan extends RobotPlan {

    private static final String TAG = "AreaPlan";

    private final Random random;
    private ArrayList<Double> results;
    private ControlApp controlApp;

    /**
     * Creates an AreaPlan with the specified area.
     */
    public AreaPlan(ControlApp controlApp) {
        this.controlApp = controlApp;
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

        Log.d(TAG, "Started");

        ArrayList<GeoPoint> areaPoints;

        double robotLat, robotLon;
        Location location;

        while (!isInterrupted()) {

            Log.d(TAG, "Begin loop");

            while (controlApp.getAreaPoints() == null || controlApp.getAreaPoints().size() < 4) {
                waitFor(1000L);
            }

            location = controlApp.getRobotController().LOCATION_PROVIDER.getLastKnownLocation();

            areaPoints = controlApp.getAreaPoints();

            robotLat = location.getLatitude();
            robotLon = location.getLongitude();

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
            GeoPoint second;
            if (first == areaPoints.get(0)) {
                second = areaPoints.get(areaPoints.size() - 2);
            } else {
                second = areaPoints.get(results.indexOf(minValue) - 1);
            }
            GeoPoint third = areaPoints.get(results.indexOf(minValue) + 1);

            results.clear();

            // Calculate distance from closest point to previous point and next point
            double distanceSecond = MapFragment.computeDistanceToKilometers(first.getLatitude(), first.getLongitude(), second.getLatitude(), second.getLongitude());
            double distanceThird = MapFragment.computeDistanceToKilometers(first.getLatitude(), first.getLongitude(), third.getLatitude(), third.getLongitude());
            double A;

            // Calculate angle between robot and the two points
            if (distanceSecond < distanceThird || distanceSecond == distanceThird) {
                double distBetweenSecondAndRobot = MapFragment.computeDistanceToKilometers(robotLat, robotLon, second.getLatitude(), second.getLongitude());
                A = Math.acos((Math.pow(minValue, 2) + Math.pow(distBetweenSecondAndRobot, 2) - Math.pow(distanceSecond, 2)) / (2 * minValue * distBetweenSecondAndRobot));
            } else if (distanceSecond > distanceThird) {
                double distBetweenThirdAndRobot = MapFragment.computeDistanceToKilometers(robotLat, robotLon, third.getLatitude(), third.getLongitude());
                A = Math.acos((Math.pow(minValue, 2) + Math.pow(distBetweenThirdAndRobot, 2) - Math.pow(distanceThird, 2)) / (2 * minValue * distBetweenThirdAndRobot));
            } else {
                A = 0;
            }

            A = A * 180 / Math.PI;

            // If angle gets to big, change direction
            if (A > 160) {
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
