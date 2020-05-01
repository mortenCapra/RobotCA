package com.robotca.ControlApp.Core.Plans;

import android.util.Log;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.Core.Utils;

import org.osmdroid.util.GeoPoint;
import org.ros.rosjava_geometry.Vector3;

import static com.robotca.ControlApp.Core.Utils2.computeDistanceAndBearing;
import static com.robotca.ControlApp.Core.Utils2.computeDistanceBetweenTwoPoints;
import static com.robotca.ControlApp.Core.Utils2.createVectorFromGeoPoint;

/**
 * Rudimentary waypoint plan for testing. No collision detection, just moves towards the next waypoint.
 *
 * Created by Nathaniel Stone on 3/4/16.
 *
 */
public class RoutePlan extends RobotPlan {

    private static final double MINIMUM_DISTANCE = 0.5;
    private static final double LOOKAHEAD_FACTOR = 1;
    private final ControlApp controlApp;

    private static final String TAG = "RoutePlan";

    private static final double MAX_SPEED = 1.00;


    private final static double GAMMA = 2;
    private final static double KAPPA = 0.4;

    private GeoPoint initialPoint = RobotController.getStartGpsLocation();
    private GeoPoint currentPoint;
    private GeoPoint startPoint;
    private GeoPoint goalPoint;


    private Vector3 currentPosition;
    private double currentHeading;
    private Vector3 goalPosition;
    private Vector3 startPosition;

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
        startPoint = initialPoint;
        double dist, spd;
        while(!isInterrupted()){

            while (controlApp.getNextPointInRoute() == null) {
                controller.publishVelocity(0, 0, 0);
                waitFor(1000L);
            }

            goalPoint = controlApp.getNextPointInRoute();
            startPosition = createVectorFromGeoPoint(startPoint, initialPoint);
            goalPosition = createVectorFromGeoPoint(goalPoint, initialPoint);
            spd = 0.0;

            do {
                spd += MAX_SPEED / 15.0;
                if (spd > MAX_SPEED) {
                    spd = MAX_SPEED;
                }

                currentPoint = RobotController.getCurrentGPSLocation();
                currentHeading = RobotController.getHeading();
                currentPosition = createVectorFromGeoPoint(currentPoint, initialPoint);

                Vector3 normalPosition = getNormalPoint(currentPosition, startPosition, goalPosition);
                Vector3 forwardVector = goalPosition.subtract(startPosition).normalize();

                normalPosition = normalPosition.add(forwardVector.scale(LOOKAHEAD_FACTOR*spd));

                if (Utils.distance(startPosition.getX(), startPosition.getY(), goalPosition.getX(), goalPosition.getY())
                        < Utils.distance(startPosition.getX(), startPosition.getY(), normalPosition.getX(), normalPosition.getY())){
                    normalPosition = goalPosition;
                }

                Vector3 robotToNormal = normalPosition.subtract(currentPosition);
                double angle = -Math.atan2(robotToNormal.getY(), robotToNormal.getX());
                double angleDiff = Utils.angleDifference(angle, currentHeading);

                double angleVel = Math.atan((2*Math.sin(angleDiff))/robotToNormal.getMagnitude());
                double linearVel;
                if (angleVel < 1){
                    linearVel = spd;
                } else{
                    linearVel = spd/Math.abs(angleVel);
                }
                controller.publishVelocity(spd, 0, -angleVel);

                dist = computeDistanceBetweenTwoPoints(currentPoint, goalPoint);

            } while(!(isInterrupted()) && dist > MINIMUM_DISTANCE && goalPoint == controlApp.getNextPointInRoute());
            final int N = 15;
            for (int i = N - 1; i >= 0 && !isInterrupted(); --i) {


                // Check angle to target
                Vector3 robotToGoal = goalPosition.subtract(currentPosition);
                double angle = -Math.atan2(robotToGoal.getY(), robotToGoal.getX());
                double angleDiff = Utils.angleDifference(angle, currentHeading);

                double angleVel = Math.atan((2*Math.sin(angleDiff))/robotToGoal.getMagnitude());

                // Slow down
                controller.publishVelocity(spd * ((double)i / N), 0.0,((double)i / N) * angleVel);
                waitFor(N);
            }

            controlApp.pollNextPointInRoute();
            startPoint = goalPoint;
        }
    }

    private Vector3 getNormalPoint(Vector3 p, Vector3 a, Vector3 b){
        Vector3 ap = p.subtract(a);
        Vector3 ab = b.subtract(a);

        ab = ab.normalize();
        ab = ab.scale(ap.dotProduct(ab));

        return a.add(ab);
    }

    protected void start2(RobotController controller) throws Exception {

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
            int counter = 0;
            do {
                // Increment speed
                spd += MAX_SPEED / 15.0;
                if (spd > MAX_SPEED)
                    spd = MAX_SPEED;

                GeoPoint point = RobotController.getCurrentGPSLocation();

                float[] res = new float[3];
                computeDistanceAndBearing(point.getLatitude(), point.getLongitude(), next.getLatitude(), next.getLongitude(), res);

                double bearing = Math.toRadians(res[2]);
                double heading = RobotController.getHeading();
                // Check angle to target - bearing is negative to accomodate the standard of angles in ros
                dir = Utils.angleDifference(heading, -bearing);
                dist = res[0];
                /*
                //initialize route with correct angle
                if (counter == 0){
                    while(!(dir < 0.2 && dir > -0.2)) {
                        controller.publishVelocity(0.0, 0.0, dir);
                        double tempHeading = RobotController.getHeading();
                        dir = Utils.angleDifference(tempHeading, -bearing);
                    }
                }
                */

                controller.publishVelocity(spd * Math.cos(dir), 0.0, spd * Math.sin(dir));

                counter = counter + 1;
                // Check distance to target
            } while (!isInterrupted() && dist > MINIMUM_DISTANCE && next.equals(controlApp.getNextPointInRoute()));
            // Stop
            final int N = 15;
            for (int i = N - 1; i >= 0 && !isInterrupted(); --i) {

                GeoPoint point = RobotController.getCurrentGPSLocation();

                float[] res = new float[3];
                computeDistanceAndBearing(point.getLatitude(), point.getLongitude(), next.getLatitude(), next.getLongitude(), res);

                // Check angle to target
                dir = Utils.angleDifference(RobotController.getHeading(), -Math.toRadians(res[2])) / 2.0;

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
