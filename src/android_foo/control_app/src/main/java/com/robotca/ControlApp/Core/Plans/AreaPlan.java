package com.robotca.ControlApp.Core.Plans;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.Core.Utils;
import com.robotca.ControlApp.Fragments.MapFragment;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.ros.rosjava_geometry.Vector3;

import java.util.ArrayList;
import java.util.Random;

import static com.robotca.ControlApp.Core.Utils2.computeDistanceToKilometers;
import static com.robotca.ControlApp.Core.Utils2.computeDistanceAndBearing;
import static com.robotca.ControlApp.Core.Utils2.computeDistanceBetweenTwoPoints;
import static com.robotca.ControlApp.Core.Utils2.createVectorFromGeoPoint;
import static com.robotca.ControlApp.Core.Utils2.getNormalPoint;

public class AreaPlan extends RobotPlan {

    private static final String TAG = "AreaPlan";

    private static final double MINIMUM_DISTANCE = 0.5;
    private static final double LOOKAHEAD_FACTOR = 2;
    private static final double MAX_SPEED = 1.00;

    private GeoPoint initialPoint = RobotController.getStartGpsLocation();
    private GeoPoint currentPoint;
    private GeoPoint startPoint;
    private GeoPoint goalPoint;

    private Vector3 currentPosition;
    private double currentHeading;
    private Vector3 goalPosition;
    private Vector3 startPosition;

    private ControlApp controlApp;
    private Polygon area;
    private MapView mapView;

    /**
     * Creates an AreaPlan with the specified area.
     */
    public AreaPlan(ControlApp controlApp) {
        this.controlApp = controlApp;
        area = new Polygon();
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

        startPoint = initialPoint;
        double dist, spd;

        while (!isInterrupted()) {

            Log.d(TAG, "Begin loop");

            // Wait for enough area points
            while (controlApp.getAreaPoints() == null || controlApp.getAreaPoints().size() < 5) {
                waitFor(1000L);
            }

            // Get area and obstacle points, and location of robot
            area = controlApp.getArea();
            mapView = controlApp.getMapView();
            ArrayList<GeoPoint> areaPoints = controlApp.getAreaPoints();
            ArrayList<ArrayList<GeoPoint>> allObstaclePoints = controlApp.getObstaclePoints();
            Location location = controlApp.getRobotController().LOCATION_PROVIDER.getLastKnownLocation();

            // Generate random points to drive towards
            GeoPoint randomPoint = randomPoint(area, areaPoints, allObstaclePoints);
            Marker marker = addMarker(randomPoint, mapView);
            controlApp.addPointToRoute(randomPoint);

            // If robot is outside area or inside obstacle, stop robot.
            while (!robotInArea(areaPoints, location) || robotInObstacle(allObstaclePoints, location)) {
                waitFor(1000L);
            }

            goalPoint = controlApp.getNextPointInRoute();
            startPosition = createVectorFromGeoPoint(startPoint, initialPoint);
            goalPosition = createVectorFromGeoPoint(goalPoint, initialPoint);
            spd = MAX_SPEED;

            do {
                currentPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                currentHeading = RobotController.getHeading();
                currentPosition = createVectorFromGeoPoint(currentPoint, initialPoint);

                Vector3 normalPosition = getNormalPoint(currentPosition, startPosition, goalPosition);
                Vector3 forwardVector = goalPosition.subtract(startPosition).normalize();

                normalPosition = normalPosition.add(forwardVector.scale(LOOKAHEAD_FACTOR*spd));

                if (Utils.distance(startPosition.getX(), startPosition.getY(), goalPosition.getX(), goalPosition.getY())
                        < Utils.distance(startPosition.getX(), startPosition.getY(), normalPosition.getX(), normalPosition.getY())) {
                    normalPosition = goalPosition;
                }

                Vector3 robotToNormal = normalPosition.subtract(currentPosition);
                double angle = -Math.atan2(robotToNormal.getY(), robotToNormal.getX());
                double angleDiff = Utils.angleDifference(angle, currentHeading);

                double angleVel = Math.atan((2*Math.sin(angleDiff))/robotToNormal.getMagnitude());
                spd = MAX_SPEED * Math.cos(angleDiff);

                if (spd < 0) {
                    spd = 0.2;
                } else if (spd > MAX_SPEED) {
                    spd = MAX_SPEED;
                }

                controller.publishVelocity(spd, 0, -angleVel);

                dist = computeDistanceBetweenTwoPoints(currentPoint, goalPoint);

            } while (!isInterrupted() && dist > MINIMUM_DISTANCE && goalPoint == controlApp.getNextPointInRoute());

            if (!isInterrupted() && goalPoint.equals(controlApp.getNextPointInRoute())) {
                controlApp.pollNextPointInRoute();
                startPoint = goalPoint;
            }

            mapView.getOverlays().remove(marker);
        }
    }

    private Marker addMarker(GeoPoint geoPoint, MapView mapView) {
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setDraggable(false);
        marker.setInfoWindow(null);
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        return marker;
    }

    /**
     * Check if robot is inside the marked area
     * @param areaPoints used to mark area
     * @param location of robot
     * @return true or false depending on if robot is inside or outside
     */
    private boolean robotInArea(ArrayList<GeoPoint> areaPoints, Location location) {
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

    /**
     * Check if robot is inside the marked obstacle
     * @param obstaclePoints used to mark obstacles
     * @param location of robot
     * @return true or false depending on if robot is inside or outside
     */
    private boolean robotInObstacle(@NonNull ArrayList<ArrayList<GeoPoint>> obstaclePoints, Location location) {
        int i, j, k;
        boolean result = false;

        for (i = 0; i < obstaclePoints.size(); i++) {
            for (j = 0, k = obstaclePoints.get(i).size() - 1; j < obstaclePoints.get(i).size(); k = j++) {
                if ((obstaclePoints.get(i).get(j).getLongitude() > location.getLongitude()) != (obstaclePoints.get(i).get(k).getLongitude() > location.getLongitude()) &&
                        (location.getLatitude() < (obstaclePoints.get(i).get(k).getLatitude() - obstaclePoints.get(i).get(j).getLatitude()) * (location.getLongitude() -
                                obstaclePoints.get(i).get(j).getLongitude()) / (obstaclePoints.get(i).get(k).getLongitude() - obstaclePoints.get(i).get(j).getLongitude()) +
                                obstaclePoints.get(i).get(j).getLatitude())) {
                    result = !result;
                }
            }
        }
        return result;
    }

    private GeoPoint randomPoint(Polygon polygon, ArrayList<GeoPoint> points, ArrayList<ArrayList<GeoPoint>> obstacles) {
        BoundingBox bounds = polygon.getBounds();

        double x_min = bounds.getLonEast();
        double x_max = bounds.getLonWest();
        double y_min = bounds.getLatSouth();
        double y_max = bounds.getLatNorth();

        double lat = y_min + (Math.random() * (y_max - y_min));
        double lon = x_min + (Math.random() * (x_max - x_min));

        GeoPoint geoPoint = new GeoPoint(lat, lon);

        if (randomPointInObstacle(obstacles, geoPoint)) {
            return randomPoint(polygon, points, obstacles);
        } else if (randomPointInArea(points, geoPoint)){
            return geoPoint;
        } else {
            return randomPoint(polygon, points, obstacles);
        }
    }

    private boolean randomPointInArea(ArrayList<GeoPoint> areaPoints, GeoPoint geoPoint) {
        int i, j;
        boolean result = false;

        for (i = 0, j = areaPoints.size() - 1; i < areaPoints.size(); j = i++) {
            if ((areaPoints.get(i).getLongitude() > geoPoint.getLongitude()) != (areaPoints.get(j).getLongitude() > geoPoint.getLongitude()) &&
                    (geoPoint.getLatitude() < (areaPoints.get(j).getLatitude() - areaPoints.get(i).getLatitude()) * (geoPoint.getLongitude() - areaPoints.get(i).getLongitude())
                            / (areaPoints.get(j).getLongitude() - areaPoints.get(i).getLongitude()) + areaPoints.get(i).getLatitude())) {
                result = !result;
            }
        }
        return result;
    }

    private boolean randomPointInObstacle(ArrayList<ArrayList<GeoPoint>> obstaclePoints, GeoPoint geoPoint) {
        int i, j, k;
        boolean result = false;

        for (i = 0; i < obstaclePoints.size(); i++) {
            for (j = 0, k = obstaclePoints.get(i).size() - 1; j < obstaclePoints.get(i).size(); k = j++) {
                if ((obstaclePoints.get(i).get(j).getLongitude() > geoPoint.getLongitude()) != (obstaclePoints.get(i).get(k).getLongitude() > geoPoint.getLongitude()) &&
                        (geoPoint.getLatitude() < (obstaclePoints.get(i).get(k).getLatitude() - obstaclePoints.get(i).get(j).getLatitude()) * (geoPoint.getLongitude() -
                                obstaclePoints.get(i).get(j).getLongitude()) / (obstaclePoints.get(i).get(k).getLongitude() - obstaclePoints.get(i).get(j).getLongitude()) +
                                obstaclePoints.get(i).get(j).getLatitude())) {
                    result = !result;
                }
            }
        }
        return result;
    }

    private GeoPoint centerOfPolygon(Polygon polygon) {
        BoundingBox bounds = polygon.getBounds();

        double lat = bounds.getCenterLatitude();
        double lon = bounds.getCenterLongitude();

        GeoPoint geoPoint = new GeoPoint(lat, lon);

        return geoPoint;
    }
}
