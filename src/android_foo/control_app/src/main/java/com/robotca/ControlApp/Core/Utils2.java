package com.robotca.ControlApp.Core;

import com.robotca.ControlApp.Core.Dijkstra.DistanceScorer;
import com.robotca.ControlApp.Core.Dijkstra.GeoPointNode;
import com.robotca.ControlApp.Core.Dijkstra.Graph;
import com.robotca.ControlApp.Core.Dijkstra.RouteFinder;
import com.robotca.ControlApp.Fragments.MapFragment;

import org.osmdroid.util.GeoPoint;
import org.ros.rosjava_geometry.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Utils2 {
    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM


    /**
     * Method to identify if a point is located inside an obstacle
     * @param obstacle the point might be inside
     * @param location Geopoint
     * @return boolean value for if the point is contained in the obstacle
     */
    public static boolean isPointContainedInObstacle(ArrayList<GeoPoint> obstacle , GeoPoint location) {
        int j, k;
        boolean result = false;
        for (j = 0, k = obstacle.size() - 1; j < obstacle.size(); k = j++) {
            if ((obstacle.get(j).getLongitude() > location.getLongitude()) != (obstacle.get(k).getLongitude() > location.getLongitude()) &&
                    (location.getLatitude() < (obstacle.get(k).getLatitude() - obstacle.get(j).getLatitude()) * (location.getLongitude() -
                            obstacle.get(j).getLongitude()) / (obstacle.get(k).getLongitude() - obstacle.get(j).getLongitude()) +
                            obstacle.get(j).getLatitude())) {
                result = !result;
            }
        }
        return result;
    }

    /**
     * Method to signify if the linesegments which the two pairs span intersect
     * @param p1 first geopoint of pair one
     * @param q1 second geopoint of pair one
     * @param p2 first geopoint of pair two
     * @param q2 second geopoint of pair two
     * @return boolean value signifying whether the line segments intersect
     */
    public static boolean doIntersect(GeoPoint p1, GeoPoint q1, GeoPoint p2, GeoPoint q2){
        // Find the four orientations needed for general and
        // special cases
        int o1 = orientation(p1, q1, p2);
        int o2 = orientation(p1, q1, q2);
        int o3 = orientation(p2, q2, p1);
        int o4 = orientation(p2, q2, q1);

        // General case
        if (o1 != o2 && o3 != o4)
            return true;

        // Special Cases
        // p1, q1 and p2 are colinear and p2 lies on segment p1q1
        if (o1 == 0 && onSegment(p1, p2, q1)) return true;

        // p1, q1 and q2 are colinear and q2 lies on segment p1q1
        if (o2 == 0 && onSegment(p1, q2, q1)) return true;

        // p2, q2 and p1 are colinear and p1 lies on segment p2q2
        if (o3 == 0 && onSegment(p2, p1, q2)) return true;

        // p2, q2 and q1 are colinear and q1 lies on segment p2q2
        if (o4 == 0 && onSegment(p2, q1, q2)) return true;

        return false; // Doesn't fall in any of the above cases
    }

    /**
     * find the orientation of the triplet (p, q, r)
     * 0 --> p, q and r are colinear
     * 1 --> clockwise
     * 2 --> counterclockwise
     * @param p fist geopoint
     * @param q second geopoint
     * @param r third geopoint
     * @return 0, 1 or 2
     */
    public static int orientation(GeoPoint p, GeoPoint q, GeoPoint r){
        double val = (q.getLongitude() - p.getLongitude()) * (r.getLatitude() - q.getLatitude()) -
                (q.getLatitude() - p.getLatitude()) * (r.getLongitude() - q.getLongitude());

        if (val == 0) return 0; // colinear

        return (val > 0)? 1: 2; // clock or counterclock wise
    }

    /**
     * given three colinear points p, q, r, the function checks if the point q lies on line segment 'pr'
     * @param p
     * @param q
     * @param r
     * @return
     */
    public static boolean onSegment(GeoPoint p, GeoPoint q, GeoPoint r){
        if (q.getLatitude() <= Math.max(p.getLatitude(), r.getLatitude()) && q.getLatitude() >= Math.min(p.getLatitude(), r.getLatitude()) &&
                q.getLongitude() <= Math.max(p.getLongitude(), r.getLongitude()) && q.getLongitude() >= Math.min(p.getLongitude(), r.getLongitude()))
            return true;

        return false;
    }

    /**
     * calculate positive bearing in radians
     * @param bearing bearing wanted in positive radians
     */
    public static double getPositiveBearing(double bearing){
        double b = bearing;
        if (bearing < 0){
            b = 2 * Math.PI + bearing;
        }
        return b;
    }

    /**
     * Method to find the point which is 0.5 meters outside the obstacle.
     * @param point specific corner of obstacle
     * @param obstacle obstalcle which the point is a part of
     * @return new geopoint 0.5 meters outside the obstacle
     */
    public static GeoPoint calculatePointOutsideObstacle(GeoPoint point, ArrayList<GeoPoint> obstacle){
        GeoPoint[] nn = getNeighboursInObstacle(point, obstacle);
        GeoPoint n1 = nn[0];
        GeoPoint n2 = nn[1];

        double b1 = getPositiveBearing(Math.toRadians(computeBearingBetweenTwoPoints(point, n1)));
        double b2 = getPositiveBearing(Math.toRadians(computeBearingBetweenTwoPoints(point, n2)));
        double b3 = ((b1 + b2)/2 + Math.PI)%(2*Math.PI);

        GeoPoint p = inverseHaversine(point, b3, 0.5);
        if (isPointContainedInObstacle(obstacle, p)){
            return inverseHaversine(point, (b3 + Math.PI)%(2*Math.PI), 0.5);
        } else{
            return p;
        }
    }

    /**
     * Method to get the neighbours of a point in an obstacle
     * @param point of which the neighbours are wanted
     * @param obstacle which the point is a part of
     * @return an array of the neighbours. basically a tuple.
     */
    public static GeoPoint[] getNeighboursInObstacle(GeoPoint point, ArrayList<GeoPoint> obstacle){
        GeoPoint[] neighbours = new GeoPoint[2];
        int indexOfPoint = obstacle.indexOf(point);
        if (indexOfPoint == 0){
            neighbours[0] = obstacle.get(obstacle.size()-2);
        } else {
            neighbours[0] = obstacle.get(indexOfPoint-1);
        }
        if (indexOfPoint == obstacle.size()-2){
            neighbours[1] = obstacle.get(0);
        } else{
            neighbours[1] = obstacle.get(indexOfPoint + 1);
        }
        return neighbours;
    }

    /**
     * method to figure out if a linesegment intersects with an obstacle
     * @param start of line segment
     * @param end of line segment
     * @param obstacle obstacle to check
     * @return boolean value whether the segment and obstacle intersects.
     */
    public static boolean doesLineSegmentIntersectWithObstacle(GeoPoint start, GeoPoint end, ArrayList<GeoPoint> obstacle) {
        for (int k = 0; k < obstacle.size()-1; k++){
            GeoPoint p2 = obstacle.get(k);
            GeoPoint q2;
            if (k == obstacle.size()-2){
                q2 = obstacle.get(0);
            } else{
                q2 = obstacle.get(k+1);
            }
            if (doIntersect(start, end, p2, q2)){
                return true;
            }
        }
        return false;
    }

    /**
     * compute the distance between two geopoints in kilometers
     * @param startLat start latitude
     * @param startLon start longitude
     * @param endLat end latitude
     * @param endLon end longitude
     * @return distance in kilometers
     */
    public static double computeDistanceToKilometers(double startLat, double startLon, double endLat, double endLon) {
        double dLat = Math.toRadians((endLat - startLat));
        double dLon = Math.toRadians((endLon - startLon));

        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);

        double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLon);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * simple haversine formula
     * @param val value
     * @return
     */
    public static double haversin(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }

    /**
     * Function to compute distance between 2 points as well as the angle (bearing) between them
     * @param lat1 latitude of geopoint 1
     * @param lon1 longitude of geopoint 1
     * @param lat2 latitude of geopoint 2
     * @param lon2 longitude of geopoint 2
     * @param results array to contain results
     */
    public static void computeDistanceAndBearing(double lat1, double lon1,
                                                 double lat2, double lon2, float[] results) {
        // Based on http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
        // using the "Inverse Formula" (section 4)

        int MAXITERS = 20;
        // Convert lat/long to radians
        lat1 *= Math.PI / 180.0;
        lat2 *= Math.PI / 180.0;
        lon1 *= Math.PI / 180.0;
        lon2 *= Math.PI / 180.0;

        double a = 6378137.0; // WGS84 major axis
        double b = 6356752.3142; // WGS84 semi-major axis
        double f = (a - b) / a;
        double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

        double L = lon2 - lon1;
        double A = 0.0;
        double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
        double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

        double cosU1 = Math.cos(U1);
        double cosU2 = Math.cos(U2);
        double sinU1 = Math.sin(U1);
        double sinU2 = Math.sin(U2);
        double cosU1cosU2 = cosU1 * cosU2;
        double sinU1sinU2 = sinU1 * sinU2;

        double sigma = 0.0;
        double deltaSigma = 0.0;
        double cosSqAlpha;
        double cos2SM;
        double cosSigma;
        double sinSigma;
        double cosLambda = 0.0;
        double sinLambda = 0.0;

        double lambda = L; // initial guess
        for (int iter = 0; iter < MAXITERS; iter++) {
            double lambdaOrig = lambda;
            cosLambda = Math.cos(lambda);
            sinLambda = Math.sin(lambda);
            double t1 = cosU2 * sinLambda;
            double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
            double sinSqSigma = t1 * t1 + t2 * t2; // (14)
            sinSigma = Math.sqrt(sinSqSigma);
            cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
            sigma = Math.atan2(sinSigma, cosSigma); // (16)
            double sinAlpha = (sinSigma == 0) ? 0.0 :
                    cosU1cosU2 * sinLambda / sinSigma; // (17)
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
            cos2SM = (cosSqAlpha == 0) ? 0.0 :
                    cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

            double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
            A = 1 + (uSquared / 16384.0) * // (3)
                    (4096.0 + uSquared *
                            (-768 + uSquared * (320.0 - 175.0 * uSquared)));
            double B = (uSquared / 1024.0) * // (4)
                    (256.0 + uSquared *
                            (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
            double C = (f / 16.0) *
                    cosSqAlpha *
                    (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
            double cos2SMSq = cos2SM * cos2SM;
            deltaSigma = B * sinSigma * // (6)
                    (cos2SM + (B / 4.0) *
                            (cosSigma * (-1.0 + 2.0 * cos2SMSq) -
                                    (B / 6.0) * cos2SM *
                                            (-3.0 + 4.0 * sinSigma * sinSigma) *
                                            (-3.0 + 4.0 * cos2SMSq)));

            lambda = L +
                    (1.0 - C) * f * sinAlpha *
                            (sigma + C * sinSigma *
                                    (cos2SM + C * cosSigma *
                                            (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

            double delta = (lambda - lambdaOrig) / lambda;
            if (Math.abs(delta) < 1.0e-12) {
                break;
            }
        }

        float distance = (float) (b * A * (sigma - deltaSigma));
        results[0] = distance;
        if (results.length > 1) {
            float initialBearing = (float) Math.atan2(cosU2 * sinLambda,
                    cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
            initialBearing *= 180.0 / Math.PI;
            results[1] = initialBearing;
            if (results.length > 2) {
                float finalBearing = (float) Math.atan2(cosU1 * sinLambda,
                        -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
                finalBearing *= 180.0 / Math.PI;
                results[2] = finalBearing;
            }
        }
    }

    /**
     * function to compute the distance between two geopoints. A shortcut of function "computeDistanceAndBearing"
     * @param p1 geopoint 1
     * @param p2 geopoint 2
     * @return distance
     */
    public static double computeDistanceBetweenTwoPoints(GeoPoint p1, GeoPoint p2){
        float[] res = new float[3];
        computeDistanceAndBearing(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), res);
        return res[0];
    }

    /**
     * function to compute the bearing between two geopoints. A shortcut of function "computeDistanceAndBearing"
     * @param p1 geopoint 1
     * @param p2 geopoint 2
     * @return distance
     */
    public static double computeBearingBetweenTwoPoints(GeoPoint p1, GeoPoint p2){
        float[] res = new float[3];
        computeDistanceAndBearing(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), res);
        return res[2];
    }

    /**
     * inverse haversine function. Function to create a bew geopoint, with a certain bearing and distance to an already existing geopoint
     * @param coordinate already existing geopoint
     * @param bearing bearing between new point and existing point
     * @param distance distance between new point and existing point
     * @return new geopoint
     */
    public static GeoPoint inverseHaversine(GeoPoint coordinate, double bearing, double distance){
        double lat1 = coordinate.getLatitude();
        double lon1 = coordinate.getLongitude();
        lat1 *= Math.PI / 180.0; // to radians
        lon1 *= Math.PI / 180.0;
        double a = 6378137.0; //Earth's radius in meters


        double lat2 = Math.asin(Math.sin(lat1)*Math.cos(distance/a) +
                Math.cos(lat1)*Math.sin(distance/a)*Math.cos(bearing));

        double lon2 = lon1 + Math.atan2(Math.sin(bearing)*Math.sin(distance/a)*Math.cos(lat1),
                Math.cos(distance/a)-Math.sin(lat1)*Math.sin(lat2));

        lat2 = Math.toDegrees(lat2);
        lon2 = Math.toDegrees(lon2);

        return new GeoPoint(lat2, lon2);
    }

    /**
     * convert a geopoint to a vector. The coordinatesystem has the second geopoint at orego
     * @param geoPoint1 geopoint to convert
     * @param geoPoint2 geopoint at orego
     * @return vector in vehicle coordinates
     */
    public static Vector3 createVectorFromGeoPoint(GeoPoint geoPoint1, GeoPoint geoPoint2) {
        float[] res = new float[3];
        computeDistanceAndBearing(geoPoint1.getLatitude(), geoPoint1.getLongitude(), geoPoint2.getLatitude(), geoPoint1.getLongitude(), res);
        float y = res[0];
        computeDistanceAndBearing(geoPoint1.getLatitude(), geoPoint1.getLongitude(), geoPoint1.getLatitude(), geoPoint2.getLongitude(), res);
        float x = res[0];
        computeDistanceAndBearing(geoPoint1.getLatitude(), geoPoint1.getLongitude(), geoPoint2.getLatitude(), geoPoint2.getLongitude(), res);
        float b = res[2];
        //To give correct signs
        if(b > 90){
            x = -x;
        } else if(b < 0 && b > -90){
            y = -y;
        } else if( b < 90 && b > 0){
            x = -x;
            y = -y;
        }

        // assign reverse values to accomodate vehicle coordinates
        return new Vector3(y,x,0.0);
    }

    /**
     * create a new geopoint from a vector and a geopoint
     * @param p already existing point
     * @param v vector which gives distance and bearing to be between new point and already existing point
     * @return new geopoint
     */
    public static GeoPoint createGeoPointFromPointandVector(GeoPoint p, Vector3 v) {
        GeoPoint res = inverseHaversine(p, Math.atan2(v.getY(), v.getX()), v.getMagnitude());

        return res;
    }

    /**
     * Function to check if a segment of the route intersects with an obstalce, and in that case find a way around it.
     * @param obstacle obstacle to check
     * @param routePoints routepoints in route
     * @param i index of the end of the route segment
     * @return return a list of points, which navigates around the obstacle.
     */
    public static LinkedList<GeoPoint> checkObstacle(ArrayList<GeoPoint> obstacle, LinkedList<GeoPoint> routePoints, int i) {
        GeoPoint start;
        if (i == 0) {
            start = RobotController.getCurrentGPSLocation();
        } else {
            start = routePoints.get(i - 1);
        }
        GeoPoint goal = routePoints.get(i);
        if (doesLineSegmentIntersectWithObstacle(start, goal, obstacle)) {
            Set<GeoPointNode> nodes = new HashSet<>();
            nodes.add(new GeoPointNode("start", start));
            nodes.add(new GeoPointNode("goal", goal));
            Map<String, Set<String>> connections = new HashMap<>();
            for (int s = 0; s < obstacle.size() - 1; s++) {
                nodes.add(new GeoPointNode("" + s, calculatePointOutsideObstacle(obstacle.get(s), obstacle)));
            }
            for (GeoPointNode p : nodes) {
                Set<String> ids = new HashSet<>();
                for (GeoPointNode n : nodes) {
                    if (p != n && !doesLineSegmentIntersectWithObstacle(p.getGeoPoint(), n.getGeoPoint(), obstacle)) {
                        ids.add(n.getId());
                    }
                }
                connections.put(p.getId(), ids);
            }
            Graph<GeoPointNode> graph = new Graph<>(nodes, connections);
            RouteFinder<GeoPointNode> routeFinder = new RouteFinder<>(graph, new DistanceScorer(), new DistanceScorer());
            List<GeoPointNode> route = routeFinder.findRoute(graph.getNode("start"), graph.getNode("goal"));
            if (route == null){
                return null;
            } else {
                for (int s = route.size()-1; s > -1; s--){
                    routePoints.add(i, route.get(s).getGeoPoint());
                }
                return routePoints;
            }
        }
        return routePoints;
    }

    /**
     * get the vectorprojection of p onto segment between a and b
     * @param p vector to project
     * @param a vector in segment
     * @param b vector in segment
     * @return normalvector
     */
    public static Vector3 getNormalPoint(Vector3 p, Vector3 a, Vector3 b){
        Vector3 ap = p.subtract(a);
        Vector3 ab = b.subtract(a);

        ab = ab.normalize();
        ab = ab.scale(ap.dotProduct(ab));

        return a.add(ab);
    }
}
