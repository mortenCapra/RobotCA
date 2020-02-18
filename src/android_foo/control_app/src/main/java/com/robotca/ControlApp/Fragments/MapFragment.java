package com.robotca.ControlApp.Fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.robotca.ControlApp.BuildConfig;
import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.LocationProvider;
import com.robotca.ControlApp.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.lang.annotation.Native;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Fragment containing the Map screen showing the real-world position of the Robot.
 *
 */
public class MapFragment extends Fragment implements MapEventsReceiver {

    private static final int EARTH_RADIUS = 6371; // Approx Earth radius in KM

    private MyLocationNewOverlay myLocationOverlay;
    private MyLocationNewOverlay secondMyLocationOverlay;
    private MapView mapView;
    Button robotRecenterButton, clearAreaButton, clearRouteButton, clearObstacleButton, areaButton, routingButton, newObstacleButton;

    ArrayList<Double> results = new ArrayList<>();
    ArrayList<Marker> areaMarkers = new ArrayList<>();
    ArrayList<Marker> routingMarkers = new ArrayList<>();
    ArrayList<Marker> obstacleMarkers = new ArrayList<>();
    ArrayList<GeoPoint> areaPoints = new ArrayList<>();
    ArrayList<GeoPoint> waypoints = new ArrayList<>();
    ArrayList<GeoPoint> obstaclePoints = new ArrayList<>();
    ArrayList<Polygon> obstacles = new ArrayList<>();

    ArrayList<ArrayList<Marker>> allObstacleMarkers = new ArrayList<>();
    ArrayList<ArrayList<GeoPoint>> allObstaclePoints = new ArrayList<>();

    private String markerStrategy = "area";

    Polygon area;
    Polygon obstacle;
    Polyline route;

    boolean movingMarker = false;

    int areaPointCheck = 0;
    int obstaclePointCheck = 0;

    /**
     * Default Constructor.
     */
    public MapFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_map, null);
        mapView = view.findViewById(R.id.mapview);
        robotRecenterButton = view.findViewById(R.id.recenter);
        clearAreaButton = view.findViewById(R.id.clear_area_button);
        areaButton = view.findViewById(R.id.area_button);
        routingButton = view.findViewById(R.id.routing_button);
        clearRouteButton = view.findViewById(R.id.clear_route_button);
        clearObstacleButton = view.findViewById(R.id.clear_obstacle_button);
        newObstacleButton = view.findViewById(R.id.new_obstacle_button);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        mapView.setClickable(true);
        mapView.setMultiTouchControls(true);
        mapView.setUseDataConnection(true);
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        LocationProvider locationProvider = ((ControlApp) getActivity()).getRobotController().LOCATION_PROVIDER;

        // Location overlay using the robot's GPS
        myLocationOverlay = new MyLocationNewOverlay(locationProvider, mapView);
        // Location overlay using Android's GPS
        secondMyLocationOverlay = new MyLocationNewOverlay(mapView);
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);

        // Allow GPS updates
        myLocationOverlay.enableMyLocation();
        secondMyLocationOverlay.enableMyLocation();

        // Center on and follow the robot by default
        myLocationOverlay.enableFollowLocation();

        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlays().add(secondMyLocationOverlay);
        mapView.getOverlays().add(0, mapEventsOverlay);

        // Set up the Center button
        robotRecenterButton.setFocusable(false);
        robotRecenterButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Center or recenter on Android with a long press
                mapView.postInvalidate();
                myLocationOverlay.disableFollowLocation();
                mapView.postInvalidate();
                Toast.makeText(mapView.getContext(), "Centered on you", Toast.LENGTH_SHORT).show();
                secondMyLocationOverlay.enableFollowLocation();
                mapView.postInvalidate();
                return true;
            }
        });

        robotRecenterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Center or recenter on robot with a tap
                secondMyLocationOverlay.disableFollowLocation();
                mapView.postInvalidate();
                Toast.makeText(mapView.getContext(), "Centered on the Robot", Toast.LENGTH_SHORT).show();
                myLocationOverlay.enableFollowLocation();
                mapView.postInvalidate();
            }
        });

        IMapController mapViewController = mapView.getController();
        mapViewController.setZoom(18.0);

        clearAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear area on map
                for (Marker marker: areaMarkers) {
                    mapView.getOverlays().remove(marker);
                }
                mapView.getOverlays().remove(area);
                areaMarkers.clear();
                areaPoints.clear();
                area = null;
                mapView.invalidate();
                areaPointCheck = 0;
            }
        });

        clearRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear route on map
                for (Marker marker: routingMarkers) {
                    mapView.getOverlays().remove(marker);
                }
                mapView.getOverlays().remove(route);
                routingMarkers.clear();
                waypoints.clear();
                route = null;
                mapView.invalidate();
            }
        });

        clearObstacleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear obstacle on map
                allObstacleMarkers.add(obstacleMarkers);
                allObstaclePoints.add(obstaclePoints);

                for (int i = 0; i < allObstacleMarkers.size(); i++) {
                    for (Marker marker: allObstacleMarkers.get(i)) {
                        mapView.getOverlays().remove(marker);
                    }
                }

                for (Polygon polygon: obstacles) {
                    mapView.getOverlays().remove(polygon);
                }

                allObstacleMarkers.clear();
                allObstaclePoints.clear();
                obstacleMarkers.clear();
                obstaclePoints.clear();
                obstacle = null;
                mapView.invalidate();
                obstaclePointCheck = 0;
            }
        });

        areaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change marking strategy to area
                markerStrategy = "area";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();
            }
        });

        routingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change marking strategy to routing
                markerStrategy = "routing";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();
            }
        });

        newObstacleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markerStrategy = "obstacle";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();

                for (Marker marker: obstacleMarkers) {
                    marker.setDraggable(false);
                }

                obstacle = null;
                allObstacleMarkers.add(obstacleMarkers);
                allObstaclePoints.add(obstaclePoints);
                obstacleMarkers = new ArrayList<>();
                obstaclePoints = new ArrayList<>();
                obstaclePointCheck = 0;
            }
        });

        return view;
    }

    /**
     * Tell the user the coordinates of a tapped point on the map.
     * @param geoPoint The tapped point
     * @return True
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
        Toast.makeText(mapView.getContext(), "Tapped on (" + geoPoint.getLatitude() + "," +
                geoPoint.getLongitude() + ")", Toast.LENGTH_LONG).show();

        return true;
    }

    /**
     * Place a marker using a long press.
     * @param geoPoint The point to place the marker
     * @return True
     */
    @Override
    public boolean longPressHelper(GeoPoint geoPoint) {
        Marker newMarker = new Marker(mapView);
        newMarker.setPosition(geoPoint);
        newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        newMarker.setDraggable(true);
        newMarker.setInfoWindow(null);
        addMarker(newMarker);

        switch (markerStrategy) {
            case "area":
                areaPoints.add(geoPoint);

                handleAreaMarker(newMarker);

                if (areaMarkers.size() > 1) {
                    addArea(geoPoint, area, areaPointCheck, areaPoints);
                }
                break;

            case "routing":
                waypoints.add(geoPoint);

                handleRouteMarker(newMarker);

                if(routingMarkers.size() > 1) {
                    addRoute(geoPoint);
                }
                break;

            case "obstacle":
                obstaclePoints.add(geoPoint);

                handleObstacleMarker(newMarker);

                if (obstacleMarkers.size() > 1) {
                    addArea(geoPoint, obstacle, obstaclePointCheck, obstaclePoints);
                }
        }

        return true;
    }

    public void addMarker(Marker marker) {
        mapView.getOverlays().add(marker);
        mapView.invalidate();

        switch (markerStrategy) {
            case "area":
                areaMarkers.add(marker);
                break;

            case "routing":
                routingMarkers.add(marker);
                break;

            case "obstacle":
                obstacleMarkers.add(marker);
                break;
        }
    }

    /*public void removeMarker(Marker marker) {
        mapView.getOverlays().remove(marker);
        mapView.invalidate();
        if(markerStrategy.equals("area"))
            areaMarkers.remove(marker);
        else if(markerStrategy.equals("routing")){
            routingMarkers.remove(marker);
        }
    }*/

    private void handleAreaMarker(Marker marker) {
        marker.setDefaultIcon();
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            GeoPoint startMarker;
            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                movingMarker = true;

                int index = areaPoints.indexOf(startMarker);

                if (marker.getPosition() == areaMarkers.get(0).getPosition()) {
                    areaPoints.set(0, marker.getPosition());
                    areaPoints.set(areaPoints.indexOf(areaPoints.get(areaPoints.size() - 1)), marker.getPosition());
                } else {
                    areaPoints.set(index, marker.getPosition());
                }

                areaMarkers.set(areaMarkers.indexOf(marker), marker);

                addArea(marker.getPosition(), area, areaPointCheck, areaPoints);
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                startMarker = marker.getPosition();
            }
        });
    }

    private void handleObstacleMarker(Marker marker) {
        marker.setDefaultIcon();
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            GeoPoint startMarker;
            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                movingMarker = true;

                int index = obstaclePoints.indexOf(startMarker);

                if (marker.getPosition() == obstacleMarkers.get(0).getPosition()) {
                    obstaclePoints.set(0, marker.getPosition());
                    obstaclePoints.set(obstaclePoints.indexOf(obstaclePoints.get(obstaclePoints.size() - 1)), marker.getPosition());
                } else {
                    obstaclePoints.set(index, marker.getPosition());
                }

                obstacleMarkers.set(obstacleMarkers.indexOf(marker), marker);

                addArea(marker.getPosition(), obstacle, obstaclePointCheck, obstaclePoints);
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                startMarker = marker.getPosition();
            }
        });
    }

    private void handleRouteMarker(Marker marker) {
        marker.setIcon(getResources().getDrawable(R.drawable.ic_flag_black_24dp).mutate());
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                waypoints.add(routingMarkers.indexOf(marker), marker.getPosition());
                route.setPoints(waypoints);
                mapView.invalidate();
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                waypoints.remove(marker.getPosition());
            }
        });
    }

    private void addRoute(GeoPoint geoPoint) {
        if (route != null){
            route.addPoint(geoPoint);
            mapView.invalidate();
        } else {
            route = new Polyline();
            route.setPoints(waypoints);
            mapView.getOverlayManager().add(route);
            mapView.invalidate();
        }
    }

    private void addArea(GeoPoint geoPoint, Polygon polygon, int pointCheck, ArrayList<GeoPoint> points) {
        if (polygon != null) {
            mapView.getOverlays().remove(polygon);

            if (!movingMarker && pointCheck >= 2) {
                points.remove(points.size() - 2);

                int index = calculateClosestMarker(geoPoint, points);

                points.remove(points.size() - 1);
                points.add(index + 1, geoPoint);
                points.add(points.get(0));

                results.clear();
            } else if (pointCheck < 2 && !movingMarker) {
                points.remove(points.size() - 2);
                points.add(points.get(0));
                pointCheck++;
            }

            if (markerStrategy.equals("area")) {
                polygon.getFillPaint().setARGB(180, 0, 255, 0);
            } else if (markerStrategy.equals("obstacle")) {
                polygon.getFillPaint().setARGB(180, 255, 0, 0);
            }

            polygon.setPoints(points);

            mapView.getOverlayManager().add(polygon);
            mapView.invalidate();

            movingMarker = false;
        } else {
            polygon = new Polygon();

            if (markerStrategy.equals("area")) {
                polygon.getFillPaint().setARGB(180, 0, 255, 0);
            } else if (markerStrategy.equals("obstacle")) {
                polygon.getFillPaint().setARGB(180, 255, 0, 0);
            }

            points.add(points.get(0));
            polygon.setPoints(points);

            mapView.getOverlayManager().add(polygon);
            mapView.invalidate();

            if (markerStrategy.equals("obstacle")) {
                obstacles.add(polygon);
            }
        }

        if (markerStrategy.equals("area")) {
            area = polygon;
            areaPointCheck = pointCheck;
            areaPoints = points;
        } else if (markerStrategy.equals("obstacle")) {
            obstacle = polygon;
            obstaclePointCheck = pointCheck;
            obstaclePoints = points;
        }
    }

    private int calculateClosestMarker(GeoPoint geoPoint, ArrayList<GeoPoint> points) {
        double newLon = geoPoint.getLongitude();
        double newLat = geoPoint.getLatitude();

        for (int i = 0; i < points.size() - 1; i++) {
            double lon = points.get(i).getLongitude();
            double lat = points.get(i).getLatitude();

            double distance = computeDistanceToKilometers(lat, lon, newLat, newLon);

            results.add(distance);
        }

        ArrayList<Double> copyResults = (ArrayList<Double>) results.clone();
        Collections.sort(copyResults);
        double minValue = copyResults.get(0);
        double secondMinValue = copyResults.get(1);

        if (markerStrategy.equals("area"))
        {
            areaPoints = points;

        } else if (markerStrategy.equals("obstacle")) {

            obstaclePoints = points;
        }

        if (results.indexOf(minValue) > results.indexOf(secondMinValue)) {
            return results.indexOf(secondMinValue);
        }

        return results.indexOf(minValue);
    }

    public static double computeDistanceToKilometers(double startLat, double startLon, double endLat, double endLon) {
        double dLat = Math.toRadians((endLat - startLat));
        double dLon = Math.toRadians((endLon - startLon));

        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);

        double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLon);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    public static double haversin(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }

    // Function to compute distance between 2 points as well as the angle (bearing) between them
    @SuppressWarnings("unused")
    private static void computeDistanceAndBearing(double lat1, double lon1,
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
}

