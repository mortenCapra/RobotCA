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

import geometry_msgs.Pose;

public class AreaPlan extends RobotPlan {

    private static final String TAG = "AreaPlan";

    private final Random random;
    private ArrayList<Double> results;
    private ControlApp controlApp;

    ArrayList<Double> a;
    ArrayList<Double> b;
    ArrayList<Double> distance;

    /**
     * Creates an AreaPlan with the specified area.
     */
    public AreaPlan(ControlApp controlApp) {
        this.controlApp = controlApp;
        random = new Random();
        results = new ArrayList<>();
        a = new ArrayList<>();
        b = new ArrayList<>();
        distance = new ArrayList<>();
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

        double robotLatX, robotLonY;
        Location location;

        while (!isInterrupted()) {

            Log.d(TAG, "Begin loop");

            while (controlApp.getAreaPoints() == null || controlApp.getAreaPoints().size() < 4) {
                waitFor(1000L);
            }

            location = controlApp.getRobotController().LOCATION_PROVIDER.getLastKnownLocation();

            areaPoints = controlApp.getAreaPoints();

            for (int i = 0; i < areaPoints.size() - 1; i++) {
                double aTemp = ((areaPoints.get(i + 1).getLongitude() - areaPoints.get(i).getLongitude()) / (areaPoints.get(i + 1).getLatitude() - areaPoints.get(i).getLatitude()));
                double bTemp = areaPoints.get(i).getLongitude() - aTemp * areaPoints.get(i).getLatitude();

                a.add(aTemp);
                b.add(bTemp);
            }

            for (int i = 0; i < a.size() - 1; i++) {
                double y2 = areaPoints.get(i + 2).getLongitude();
                double y1 = areaPoints.get(i).getLongitude();
                double y0 = location.getLongitude();
                double x2 = areaPoints.get(i + 2).getLatitude();
                double x1 = areaPoints.get(i).getLatitude();
                double x0 = location.getLatitude();

                double d = (Math.abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1) / Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2)));

                distance.add(d);
            }

            for (double d: distance) {
                if (d < 1) {
                    controller.publishVelocity(0, 0, 0);
                } else {
                    controller.publishVelocity(1, 0, 0);
                }
            }

            a.clear();
            b.clear();
            distance.clear();

            /*double highestLonY = areaPoints.get(0).getLongitude();
            double highestLatX = areaPoints.get(0).getLatitude();
            double lowestLonY = areaPoints.get(0).getLongitude();
            double lowestLatX = areaPoints.get(0).getLatitude();

            for (int i = 0; i < areaPoints.size() - 1; i++) {
                if (highestLonY < areaPoints.get(i).getLongitude()) {
                    highestLonY = areaPoints.get(i).getLongitude();
                }
                if (highestLatX < areaPoints.get(i).getLatitude()) {
                    highestLatX = areaPoints.get(i).getLatitude();
                }
                if (lowestLonY > areaPoints.get(i).getLongitude()) {
                    lowestLonY = areaPoints.get(i).getLongitude();
                }
                if (lowestLatX > areaPoints.get(i).getLatitude()) {
                    lowestLatX = areaPoints.get(i).getLatitude();
                }
            }

            if (location.getLongitude() >= lowestLonY && location.getLatitude() >= lowestLatX
                    && location.getLongitude() <= highestLonY && location.getLatitude() <= highestLatX) {
                controller.publishVelocity(1, 0, 0);
            } else if (location.getLongitude() <= lowestLonY || location.getLatitude() <= lowestLatX
                    || location.getLongitude() >= highestLonY || location.getLatitude() >= highestLatX) {
                controller.publishVelocity(-1, 0, 0);
                waitFor(1000);
                long delay = (long) (2000 * (1 + random.nextFloat()));
                controller.publishVelocity(0, 0, .75);
                waitFor(delay);
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
                controller.publishVelocity(1, 0, 0);
                waitFor(1000);
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
            }*/

            /*robotLatX = location.getLatitude();
            robotLonY = location.getLongitude();

            // Calculate distance from robot to all points
            for (int i = 0; i < areaPoints.size() - 1; i++) {
                double latX = areaPoints.get(i).getLatitude();
                double lonY = areaPoints.get(i).getLongitude();

                double distance = MapFragment.computeDistanceToKilometers(latX, lonY, robotLatX, robotLonY);

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
                double distBetweenSecondAndRobot = MapFragment.computeDistanceToKilometers(robotLatX, robotLonY, second.getLatitude(), second.getLongitude());
                A = Math.acos((Math.pow(minValue, 2) + Math.pow(distBetweenSecondAndRobot, 2) - Math.pow(distanceSecond, 2)) / (2 * minValue * distBetweenSecondAndRobot));
            } else if (distanceSecond > distanceThird) {
                double distBetweenThirdAndRobot = MapFragment.computeDistanceToKilometers(robotLatX, robotLonY, third.getLatitude(), third.getLongitude());
                A = Math.acos((Math.pow(minValue, 2) + Math.pow(distBetweenThirdAndRobot, 2) - Math.pow(distanceThird, 2)) / (2 * minValue * distBetweenThirdAndRobot));
            } else {
                A = 0;
            }

            A = A * 180 / Math.PI;*/

            // If angle gets to big, change direction
            /*if (A > 160) {
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
                controller.publishVelocity(-1, 0, 0);
                waitFor(1000);
                long delay = (long) (2000 * (1 + random.nextFloat()));
                controller.publishVelocity(0, 0, .75);
                waitFor(delay);
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
            } else {
                controller.publishVelocity(1, 0, 0);
                waitFor(1000);
                controller.publishVelocity(0, 0, 0);
            }*/
        }
    }
}
