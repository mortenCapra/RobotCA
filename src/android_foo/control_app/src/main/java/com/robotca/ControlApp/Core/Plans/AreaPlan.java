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
    private ControlApp controlApp;
    private ArrayList<Double> distances;
    private ArrayList<Double> angles;

    /**
     * Creates an AreaPlan with the specified area.
     */
    public AreaPlan(ControlApp controlApp) {
        this.controlApp = controlApp;
        random = new Random();
        distances = new ArrayList<>();
        angles = new ArrayList<>();
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

        double result;

        while (!isInterrupted()) {

            Log.d(TAG, "Begin loop");

            while (controlApp.getAreaPoints() == null || controlApp.getAreaPoints().size() < 5) {
                waitFor(1000L);
            }

            ArrayList<GeoPoint> areaPoints = controlApp.getAreaPoints();
            Location location = controlApp.getRobotController().LOCATION_PROVIDER.getLastKnownLocation();

            while (!contains(areaPoints, location)) {
                waitFor(1000L);
            }

            calculateDistFromRobotToAreaLine(areaPoints, location);

            if (controlApp.getObstaclePoints() != null) {
                calculateDistFromRobotToObstacleLine();
            }

            result = distances.get(0);

            for (int i = 0; i < distances.size(); i++) {
                if (result > distances.get(i)) {
                    result = distances.get(i);
                }
            }
            double r = RobotController.getHeading();
            angleOf(areaPoints);

            double heading = headingToNavigateFrom();

            if (result < 150) {

                int indexOf = distances.indexOf(result);

                double maxAngle = angles.get(indexOf);
                double minAngle;
                if (maxAngle < 180)
                    minAngle = maxAngle - 180d + 360d;
                else
                    minAngle = maxAngle - 180d;

                if (maxAngle < minAngle) {
                    double tempAngle = maxAngle;
                    maxAngle = minAngle;
                    minAngle = tempAngle;
                }

                if (heading <= 90d) {
                    while (!(heading > minAngle && heading < maxAngle)) {
                        rotateRobot(controller);
                        heading = headingToNavigateFrom();
                    }
                } else if (heading > 90d && heading <= 180d) {
                    while (!(heading < minAngle || heading > maxAngle)) {
                        rotateRobot(controller);
                        heading = headingToNavigateFrom();
                    }
                } else if (heading > 180d && heading < 270d) {
                    while (!(heading < minAngle || heading > maxAngle)) {
                        rotateRobot(controller);
                        heading = headingToNavigateFrom();
                    }
                } else if (heading > 270d) {
                    while (!(heading > minAngle && heading < maxAngle)) {
                        rotateRobot(controller);
                        heading = headingToNavigateFrom();
                    }
                }
                controller.publishVelocity(.75, 0, 0);
                // For debugging
                waitFor(2000);
                controller.publishVelocity(0, 0, 0);
            } else {
                controller.publishVelocity(.75, 0, 0);
                // For debugging
                waitFor(1000);
                controller.publishVelocity(0, 0, 0);
            }
        }
    }

    private void calculateDistFromRobotToAreaLine(ArrayList<GeoPoint> areaPoints, Location location) {
        distances.clear();

        ArrayList<Double> distBetweenAreaPoints = new ArrayList<>();
        ArrayList<Double> distBetweenAreaPointAndRobot = new ArrayList<>();

        for (int i = 0; i < areaPoints.size() - 1; i++) {
            double startLon = areaPoints.get(i).getLongitude();
            double startLat = areaPoints.get(i).getLatitude();
            double endLon = areaPoints.get(i+1).getLongitude();
            double endLat = areaPoints.get(i+1).getLatitude();

            double dPoints = MapFragment.computeDistanceToKilometers(startLat, startLon, endLat, endLon);
            double dRobot = MapFragment.computeDistanceToKilometers(startLat, startLon, location.getLatitude(), location.getLongitude());

            distBetweenAreaPoints.add(dPoints);
            distBetweenAreaPointAndRobot.add(dRobot);
            if (i == areaPoints.size() - 2) {
                distBetweenAreaPointAndRobot.add(distBetweenAreaPointAndRobot.get(0));
            }
        }

        for (int i = 0; i < areaPoints.size() - 1; i++) {
            double perimeter = (distBetweenAreaPoints.get(i) + distBetweenAreaPointAndRobot.get(i) + distBetweenAreaPointAndRobot.get(i+1)) / 2;
            double area = Math.sqrt(perimeter * (perimeter - distBetweenAreaPoints.get(i)) * (perimeter - distBetweenAreaPointAndRobot.get(i)) * (perimeter - distBetweenAreaPointAndRobot.get(i+1)));
            double d = ((2 * area) / distBetweenAreaPoints.get(i)) * 100000;

            distances.add(d);
        }

        distBetweenAreaPoints.clear();
        distBetweenAreaPointAndRobot.clear();
    }

    private void calculateDistFromRobotToObstacleLine() {
        ArrayList<Double> distBetweenObstaclePoints = new ArrayList<>();
        ArrayList<Double> distBetweenObstaclePointAndRobot = new ArrayList<>();

        ArrayList<ArrayList<GeoPoint>> obstaclePoints = controlApp.getObstaclePoints();
        Location location = controlApp.getRobotController().LOCATION_PROVIDER.getLastKnownLocation();

        for (int i = 0; i < obstaclePoints.size(); i++) {
            for (int j = 0; j < obstaclePoints.get(i).size() - 1; j++) {
                double startLon = obstaclePoints.get(i).get(j).getLongitude();
                double startLat = obstaclePoints.get(i).get(j).getLatitude();
                double endLon = obstaclePoints.get(i).get(j+1).getLongitude();
                double endLat = obstaclePoints.get(i).get(j+1).getLatitude();

                double dPoints = MapFragment.computeDistanceToKilometers(startLat, startLon, endLat, endLon);
                double dRobot = MapFragment.computeDistanceToKilometers(startLat, startLon, location.getLatitude(), location.getLongitude());

                distBetweenObstaclePoints.add(dPoints);
                distBetweenObstaclePointAndRobot.add(dRobot);
                if (j == obstaclePoints.get(i).size() - 2) {
                    distBetweenObstaclePointAndRobot.add(distBetweenObstaclePointAndRobot.get(0));
                }
            }

            for (int j = 0; j < obstaclePoints.get(i).size() - 1; j++) {
                double perimeter = (distBetweenObstaclePoints.get(j) + distBetweenObstaclePointAndRobot.get(j) + distBetweenObstaclePointAndRobot.get(j+1)) / 2;
                double area = Math.sqrt(perimeter * (perimeter - distBetweenObstaclePoints.get(j)) * (perimeter - distBetweenObstaclePointAndRobot.get(j)) * (perimeter - distBetweenObstaclePointAndRobot.get(j+1)));
                double d = ((2 * area) / distBetweenObstaclePoints.get(j)) * 100000;

                distances.add(d);
            }

            distBetweenObstaclePoints.clear();
            distBetweenObstaclePointAndRobot.clear();
        }
    }

    private double headingToNavigateFrom() {
        double heading = RobotController.getHeading();

        if (heading < 0) {
            double h = Math.PI + heading;
            heading = Math.PI + h;
        }

        return Math.toDegrees(heading);
    }

    private void angleOf(ArrayList<GeoPoint> geoPoints) {
        angles.clear();

        for (int i = 0; i < geoPoints.size() - 1; i++) {
            final double deltaY = (geoPoints.get(i).getLongitude() - geoPoints.get(i+1).getLongitude());
            final double deltaX = (geoPoints.get(i).getLatitude() - geoPoints.get(i+1).getLatitude());
            final double result = Math.toDegrees(Math.atan2(deltaY, deltaX));

            angles.add((result < 0) ? (360d + result) : result);
        }
    }

    private void rotateRobot(RobotController controller) throws Exception {
        controller.publishVelocity(0, 0, 0);
        waitFor(1000);
        long delay = (long) (2000 * (1 + random.nextFloat()));
        controller.publishVelocity(0, 0, .75);
        waitFor(delay);
        controller.publishVelocity(0, 0, 0);
    }

    private boolean contains(ArrayList<GeoPoint> areaPoints, Location location) {
        int i, j;
        boolean result = false;

        for (i = 0, j = areaPoints.size() - 1; i < areaPoints.size(); j = i++) {
            if ((areaPoints.get(i).getLongitude() > location.getLongitude()) != (areaPoints.get(j).getLongitude() > location.getLongitude()) &&
                    (location.getLatitude() < (areaPoints.get(j).getLatitude() - areaPoints.get(i).getLatitude()) * (location.getLongitude() - areaPoints.get(i).getLongitude())
                    / (areaPoints.get(j).getLongitude() - areaPoints.get(i).getLongitude()) + areaPoints.get(i).getLatitude())) {
                result = !result;
            }
        }
        return result;
    }
}
