package com.robotca.ControlApp.Core.Plans;

import android.util.Log;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.Core.Utils;
import com.robotca.ControlApp.Fragments.MapFragment;

import org.osmdroid.util.GeoPoint;
import org.ros.rosjava_geometry.Vector3;

/**
 * Rudimentary waypoint plan for testing. No collision detection, just moves towards the next waypoint.
 *
 * Created by Nathaniel Stone on 3/4/16.
 * Altered by Christian Munklinde Leth-Espensen 13/03/2020
 *
 */
public class RoutePlan extends RobotPlan {

    private static final double MINIMUM_DISTANCE = 0.25;
    private final ControlApp controlApp;

    private static final String TAG = "RoutePlan";

    private static final double MAX_SPEED = 1.00;

    /**
     * Creates a RoutePlan for the specified ControlApp.
     * @param controlApp The ControlApp
     */
    public RoutePlan(ControlApp controlApp) {
        this.controlApp = controlApp;
        stop();
    }

    /**
     * @return The ControlMode for this RobotPlan
     */
    @Override
    public ControlMode getControlMode() {
        return ControlMode.Routing;
    }

    @Override
    protected void start(RobotController controller) throws Exception {

        Log.d(TAG, "Started");

        GeoPoint next;
        double dir, dist, spd;

        while (!isInterrupted()) {

            Log.d(TAG, "begin loop");

            // Wait for the next point to become available
            while (controlApp.getNextPointInRoute() == null)
                waitFor(1000L);

            next = controlApp.getNextPointInRoute();
            Log.d(TAG, "Found next point: (" + next.getLatitude() + ", " + next.getLongitude() + ")");

            spd = 0.0;

            do {
                // Increment speed
                spd += MAX_SPEED / 15.0;
                if (spd > MAX_SPEED)
                    spd = MAX_SPEED;

                GeoPoint point = RobotController.getCurrentGPSLocation();

                float[] res = new float[3];
                MapFragment.computeDistanceAndBearing(point.getLatitude(), point.getLongitude(), next.getLatitude(), next.getLongitude(), res);

                // Check angle to target
                dir = Utils.angleDifference(RobotController.getHeading(), Math.toRadians(res[2]));

                controller.publishVelocity(spd * Math.cos(dir), 0.0, spd * Math.sin(dir));

                // Check distance to target
                dist = res[0];

            } while (!isInterrupted() && dist > MINIMUM_DISTANCE && next.equals(controlApp.getNextPointInRoute()));

            // Stop
            final int N = 15;
            for (int i = N - 1; i >= 0 && !isInterrupted(); --i) {

                GeoPoint point = RobotController.getCurrentGPSLocation();

                float[] res = new float[3];
                MapFragment.computeDistanceAndBearing(point.getLatitude(), point.getLongitude(), next.getLatitude(), next.getLongitude(), res);

                // Check angle to target
                dir = Utils.angleDifference(RobotController.getHeading(), Math.toRadians(res[2])) / 2.0;

                // Slow down
                controller.publishVelocity(spd * ((double)i / N) * Math.cos(dir), 0.0, spd * ((double)i / N) * Math.sin(dir));
                waitFor(N);
            }

            // Remove the way point
            if (!isInterrupted() && next.equals(controlApp.getNextPointInRoute()))
                controlApp.pollNextPointInRoute();
        }
    }
}
