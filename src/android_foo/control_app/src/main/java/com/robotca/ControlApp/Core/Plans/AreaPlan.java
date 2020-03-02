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

    /**
     * Creates an AreaPlan with the specified area.
     */
    public AreaPlan(ControlApp controlApp) {
        this.controlApp = controlApp;
        random = new Random();
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
        ArrayList<GeoPoint> areaPoints;
        ArrayList<Double> distance = new ArrayList<>();
        ArrayList<Double> distanceBetweenPoints = new ArrayList<>();
        ArrayList<Double> distanceBetweenPointAndRobot = new ArrayList<>();
        Location location;

        while (!isInterrupted()) {

            Log.d(TAG, "Begin loop");

            while (controlApp.getAreaPoints() == null || controlApp.getAreaPoints().size() < 4) {
                waitFor(1000L);
            }

            location = controlApp.getRobotController().LOCATION_PROVIDER.getLastKnownLocation();

            areaPoints = controlApp.getAreaPoints();

            for (int i = 0; i < areaPoints.size() - 1; i++) {
                double startLon = areaPoints.get(i).getLongitude();
                double startLat = areaPoints.get(i).getLatitude();
                double endLon = areaPoints.get(i+1).getLongitude();
                double endLat = areaPoints.get(i+1).getLatitude();

                double dPoints = MapFragment.computeDistanceToKilometers(startLat, startLon, endLat, endLon);
                double dRobot = MapFragment.computeDistanceToKilometers(startLat, startLon, location.getLatitude(), location.getLongitude());

                distanceBetweenPoints.add(dPoints);
                distanceBetweenPointAndRobot.add(dRobot);
                if (i == areaPoints.size() - 2) {
                    distanceBetweenPointAndRobot.add(distanceBetweenPointAndRobot.get(0));
                }
            }

            for (int i = 0; i < areaPoints.size() - 1; i++) {
                double perimeter = (distanceBetweenPoints.get(i) + distanceBetweenPointAndRobot.get(i) + distanceBetweenPointAndRobot.get(i+1)) / 2;
                double area = Math.sqrt(perimeter * (perimeter - distanceBetweenPoints.get(i)) * (perimeter - distanceBetweenPointAndRobot.get(i)) * (perimeter - distanceBetweenPointAndRobot.get(i+1)));
                double d = ((2 * area) / distanceBetweenPoints.get(i)) * 100000;

                distance.add(d);
            }

            result = distance.get(0);

            for (int i = 0; i < distance.size(); i++) {
                if (result > distance.get(i)) {
                    result = distance.get(i);
                }
            }

            if (result < 150) {
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
                controller.publishVelocity(-1, 0, 0);
                waitFor(2000);
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
                long delay = (long) (2000 * (1 + random.nextFloat()));
                controller.publishVelocity(0, 0, .75);
                waitFor(delay);
                controller.publishVelocity(0, 0, 0);
                waitFor(1000);
            } else {
                controller.publishVelocity(1, 0, 0);
                // For debugging
                waitFor(1000);
                controller.publishVelocity(0, 0, 0);
            }

            distance.clear();
            distanceBetweenPoints.clear();
            distanceBetweenPointAndRobot.clear();
        }
    }
}
