package com.robotca.ControlApp.Fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.robotca.ControlApp.BuildConfig;
import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.ControlMode;
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
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.ros.rosjava_geometry.Vector3;

import java.util.ArrayList;
import java.util.Collections;

import static com.robotca.ControlApp.Core.Utils2.computeDistanceAndBearing;
import static com.robotca.ControlApp.Core.Utils2.computeDistanceToKilometers;

/**
 * Fragment containing the Map screen showing the real-world position of the Robot.
 *
 */
public class MapFragment extends Fragment implements MapEventsReceiver {

    private MyLocationNewOverlay myLocationOverlay;
    private MyLocationNewOverlay secondMyLocationOverlay;
    private MapView mapView;
    GeoPoint initialPoint;

    Button robotRecenterButton, clearAreaButton, clearRouteButton, clearObstacleButton, clearAll, newObstacleButton, markerOptionButton;

    ArrayList<Double> results = new ArrayList<>();
    ArrayList<Marker> areaMarkers = new ArrayList<>();
    ArrayList<Marker> routingMarkers = new ArrayList<>();
    ArrayList<Marker> obstacleMarkers = new ArrayList<>();
    ArrayList<GeoPoint> areaPoints = new ArrayList<>();
    ArrayList<GeoPoint> wayPoints = new ArrayList<>();
    ArrayList<GeoPoint> obstaclePoints = new ArrayList<>();
    ArrayList<Polygon> obstacles = new ArrayList<>();

    ArrayList<ArrayList<Marker>> allObstacleMarkers = new ArrayList<>();
    ArrayList<ArrayList<GeoPoint>> allObstaclePoints = new ArrayList<>();

    private String markerStrategy = null;

    Polygon area;
    Polygon obstacle;
    Polyline route;

    boolean movingMarker = false;

    int areaPointCheck = 0;
    int obstaclePointCheck = 0;

    ControlApp controlApp;
    ControlMode controlMode = ControlMode.Joystick;

    private LocalBroadcastManager localBroadcastManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            controlMode = (ControlMode) intent.getSerializableExtra("MODE");
            controlMode();
        }
    };

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
        clearAll = view.findViewById(R.id.clear_all_button);
        clearAreaButton = view.findViewById(R.id.clear_area_button);
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
        myLocationOverlay.enableFollowLocation();
        secondMyLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlays().add(secondMyLocationOverlay);
        mapView.getOverlays().add(0, mapEventsOverlay);

        mapView.getController().setCenter(myLocationOverlay.getMyLocation());
        mapView.getController().animateTo(myLocationOverlay.getMyLocation());
        myLocationOverlay.enableFollowLocation();
        initialPoint = myLocationOverlay.getMyLocation();


        // Set up the Center button
        robotRecenterButton.setFocusable(false);
        robotRecenterButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Center or recenter on Android with a long press
                myLocationOverlay.disableFollowLocation();
                mapView.postInvalidate();
                Toast.makeText(mapView.getContext(), "Centered on you", Toast.LENGTH_SHORT).show();
                secondMyLocationOverlay.enableFollowLocation();
                mapView.invalidate();
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
                mapView.getController().setCenter(myLocationOverlay.getMyLocation());
                mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                mapView.invalidate();
            }
        });

        IMapController mapViewController = mapView.getController();
        mapViewController.setZoom(23.0);

        clearAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAreaButton.performClick();

                clearRouteButton.performClick();

                clearObstacleButton.performClick();
            }
        });

        clearAreaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearArea();
            }
        });

        clearRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearRoute();
            }
        });

        clearObstacleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearObstacleOnMap();
            }
        });

        newObstacleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prepareForNewObstacle();
            }
        });

        if(savedInstanceState != null) {

            initialPoint = savedInstanceState.getParcelable("initialPoint");
            mapView.getController().setZoom(savedInstanceState.getDouble("zoomLevel"));
            GeoPoint center = new GeoPoint(savedInstanceState.getDouble("mapLocationLat"), savedInstanceState.getDouble("mapLocationLong"));
            myLocationOverlay.disableFollowLocation();
            secondMyLocationOverlay.disableFollowLocation();
            mapView.getController().setCenter(center);
            mapView.invalidate();
            markerStrategy = savedInstanceState.getString("markerStrategy");
            wayPoints = savedInstanceState.getParcelableArrayList("wayPoints");
            areaPoints = savedInstanceState.getParcelableArrayList("areaPoints");
            obstaclePoints = savedInstanceState.getParcelableArrayList("obstaclePoints");
            int size = savedInstanceState.getInt("size");
            for (int i = 0; i < size; i++){
                allObstaclePoints.add(i, savedInstanceState.getParcelableArrayList("item" + i));
            }
            obstaclePointCheck = savedInstanceState.getInt("obstaclePointCheck");
            areaPointCheck = savedInstanceState.getInt("areaPointCheck");


            //Initialize saved area
            if (!areaPoints.isEmpty()) {
                area = new Polygon();
                area.setPoints(areaPoints);
                area.getFillPaint().setARGB(180, 0, 255, 0);
                mapView.getOverlays().add(area);

                for (int i = 0; i < areaPoints.size() - 1; i++) {
                    Marker newMarker = initializeMarker(areaPoints.get(i));
                    areaMarkers.add(newMarker);
                    handleAreaMarker(newMarker);
                }
            }

            //initialize saved route
            if (!wayPoints.isEmpty()) {
                route = new Polyline();
                route.setPoints(wayPoints);
                mapView.getOverlays().add(route);

                for (int i = 1; i < wayPoints.size(); i++) {
                    Marker newMarker = initializeMarker(wayPoints.get(i));
                    routingMarkers.add(newMarker);
                    handleRouteMarker(newMarker);
                }
            }

            //initialize saved Obstacles
            for (int i = 0; i < allObstaclePoints.size(); i++) {
                Polygon polygon = new Polygon();
                obstacles.add(polygon);
                polygon.setPoints(allObstaclePoints.get(i));
                polygon.getFillPaint().setARGB(180, 255, 0, 0);
                mapView.getOverlays().add(polygon);
                ArrayList<Marker> markers = new ArrayList<>();
                allObstacleMarkers.add(markers);
                if (obstaclePoints == allObstaclePoints.get(i)){
                    obstacle = polygon;
                }

                for (int j = 0; j < allObstaclePoints.get(i).size() - 1; j++){
                    Marker newMarker = initializeMarker(allObstaclePoints.get(i).get(j));
                    newMarker.setDraggable(false);
                    markers.add(newMarker);
                    handleObstacleMarker(newMarker);
                    if (obstaclePoints == allObstaclePoints.get(i)){
                        obstacleMarkers.add(newMarker);
                        newMarker.setDraggable(true);
                    }
                }
            }
            mapView.invalidate();
        }
        removePointsFromRoute(((ControlApp) getActivity()).getRoutePoints().size());
        controlMode = ((ControlApp) getActivity()).getControlMode();
        controlMode();
        mapView.setMinZoomLevel(4.0);
        mapView.invalidate();
        controlApp = (ControlApp) getActivity();

        //if it only worked
        /*
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setEnableAdjustLength(true);
        scaleBarOverlay.setAlignBottom(true);
        scaleBarOverlay.setAlignRight(true);
        mapView.getOverlays().add(scaleBarOverlay);
        
         */

        return view;
    }

    private void prepareForNewObstacle() {
        markerStrategy = "obstacle";
        Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();

        for (Marker marker: obstacleMarkers) {
            marker.setDraggable(false);
        }

        obstacle = null;
        obstacleMarkers = new ArrayList<>();
        obstaclePoints = new ArrayList<>();
        obstaclePointCheck = 0;
    }

    private void clearObstacleOnMap() {
        for (int i = 0; i < allObstacleMarkers.size(); i++) {
            for (Marker marker : allObstacleMarkers.get(i)) {
                mapView.getOverlays().remove(marker);
            }
        }

        for (Polygon polygon : obstacles) {
            mapView.getOverlays().remove(polygon);
        }

        allObstacleMarkers.clear();
        allObstaclePoints.clear();
        obstacleMarkers.clear();
        obstaclePoints.clear();
        obstacle = null;
        mapView.invalidate();
        obstaclePointCheck = 0;
        ((ControlApp) getActivity()).clearObstaclePoints();
    }

    private void clearRoute() {
        // Clear route on map
        for (Marker marker : routingMarkers) {
            mapView.getOverlays().remove(marker);
        }
        mapView.getOverlays().remove(route);
        routingMarkers.clear();
        wayPoints.clear();
        route = null;
        mapView.invalidate();
        ((ControlApp) getActivity()).clearRoute();
    }

    private void clearArea() {
        // Clear area on map
        for (Marker marker : areaMarkers) {
            mapView.getOverlays().remove(marker);
        }
        mapView.getOverlays().remove(area);
        areaMarkers.clear();
        areaPoints.clear();
        area = null;
        mapView.invalidate();
        areaPointCheck = 0;
        ((ControlApp) getActivity()).setAreaPoints(null);
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
        if (markerStrategy != null) {
            Marker newMarker = initializeMarker(geoPoint);

            switch (markerStrategy) {
                case "area":
                    areaPoints.add(geoPoint);
                    areaMarkers.add(newMarker);
                    handleAreaMarker(newMarker);

                    if (areaMarkers.size() > 1) {
                        addArea(geoPoint, area, areaPointCheck, areaPoints);
                    }
                    break;

                case "routing":
                    wayPoints.add(geoPoint);
                    routingMarkers.add(newMarker);
                    handleRouteMarker(newMarker);
                    addRoute(geoPoint);
                    controlApp.addPointToRoute(geoPoint);
                    break;

                case "obstacle":
                    obstaclePoints.add(geoPoint);
                    obstacleMarkers.add(newMarker);
                    if (!allObstacleMarkers.contains(obstacleMarkers))
                        allObstacleMarkers.add(obstacleMarkers);

                    handleObstacleMarker(newMarker);

                    if (obstacleMarkers.size() > 1) {
                        addArea(geoPoint, obstacle, obstaclePointCheck, obstaclePoints);
                    }
                    break;
            }
        }

        return true;
    }

    public static Vector3 createVectorFromGeoPoint(GeoPoint geoPoint1, GeoPoint geoPoint2) {
        float[] res = new float[3];
        computeDistanceAndBearing(geoPoint1.getLatitude(), geoPoint1.getLongitude(), geoPoint2.getLatitude(), geoPoint1.getLongitude(), res);
        float x = res[0];
        //  float b12 = res[2];
        computeDistanceAndBearing(geoPoint1.getLatitude(), geoPoint1.getLongitude(), geoPoint1.getLatitude(), geoPoint2.getLongitude(), res);
        float y = res[0];
        //  float b22= res[2];
        computeDistanceAndBearing(geoPoint1.getLatitude(), geoPoint1.getLongitude(), geoPoint2.getLatitude(), geoPoint2.getLongitude(), res);
        float b = res[2];
        // to accomodate the right heading for capra robot
        if(b < 90 && b > 0){
            x = -x;
        } else if(b < 0 && b > -90){
            y = -y;
            x = -x;
        } else if( b < -90){
            y = -y;
        }
        return new Vector3(x,y,0.0);
    }

    public Marker initializeMarker(GeoPoint geoPoint) {
        Marker newMarker = new Marker(mapView);
        newMarker.setPosition(geoPoint);
        newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        newMarker.setDraggable(true);
        newMarker.setInfoWindow(null);
        mapView.getOverlays().add(newMarker);
        mapView.invalidate();
        return newMarker;
    }


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

                if (markerStrategy.equals("obstacle")) {
                    markerStrategy = "area";
                    addArea(marker.getPosition(), area, areaPointCheck, areaPoints);
                    markerStrategy = "obstacle";
                } else if (markerStrategy.equals("area")) {
                    addArea(marker.getPosition(), area, areaPointCheck, areaPoints);
                }
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

                if (markerStrategy.equals("area")) {
                    markerStrategy = "obstacle";
                    addArea(marker.getPosition(), obstacle, obstaclePointCheck, obstaclePoints);
                    markerStrategy = "area";
                } else if (markerStrategy.equals("obstacle")) {
                    addArea(marker.getPosition(), obstacle, obstaclePointCheck, obstaclePoints);
                }
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
            GeoPoint oldP;
            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                wayPoints.add(routingMarkers.indexOf(marker) + 1, marker.getPosition());
                route.setPoints(wayPoints);
                mapView.invalidate();
                controlApp.alterPointInRoute(oldP, marker.getPosition());
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                wayPoints.remove(marker.getPosition());
                oldP = marker.getPosition();
            }
        });
    }

    private void addRoute(GeoPoint geoPoint) {
        if (route != null){
            route.addPoint(geoPoint);
            mapView.invalidate();
        } else {
            route = new Polyline();
            wayPoints.add(0, myLocationOverlay.getMyLocation());
            route.setPoints(wayPoints);
            mapView.getOverlays().add(1, route);
            mapView.invalidate();
        }
    }

    public void removePointsFromRoute(int pointsInRoute){
        int pointsPassed = wayPoints.size() - pointsInRoute;
        for(int i = 0; i < pointsPassed - 1; i++) {
            wayPoints.remove(0);
            route.setPoints(wayPoints);
            mapView.getOverlays().remove(routingMarkers.get(0));
            routingMarkers.remove(0);
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

            if (markerStrategy.equals("area")) {
                mapView.getOverlays().add(0, polygon);
            } else if (markerStrategy.equals("obstacle")) {
                mapView.getOverlays().add(polygon);
            }

            polygon.setOnClickListener(new Polygon.OnClickListener() {
                @Override
                public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {

                    mapView.getOverlays().remove(polygon);
                    ArrayList<GeoPoint> tempArray = (ArrayList<GeoPoint>) polygon.getPoints();

                    if (!allObstaclePoints.contains(tempArray)) {
                        allObstaclePoints.add(tempArray);
                        allObstacleMarkers.add(obstacleMarkers);
                    }

                    int tempInt = allObstaclePoints.indexOf(tempArray);
                    for (Marker marker : allObstacleMarkers.get(tempInt)) {
                        mapView.getOverlays().remove(marker);
                    }
                    allObstaclePoints.remove(tempInt);
                    allObstacleMarkers.remove(tempInt);

                    obstacles.remove(polygon);

                    if (allObstacleMarkers.isEmpty() || allObstacleMarkers.size() == tempInt) {
                        obstacleMarkers.clear();
                        obstaclePoints.clear();

                        obstacle = null;
                    }

                    mapView.invalidate();

                    return true;
                }
            });

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

            mapView.getOverlays().add(polygon);
            mapView.invalidate();

            if (markerStrategy.equals("obstacle")) {
                obstacles.add(polygon);
            }

            if (!allObstaclePoints.contains(obstaclePoints)) {
                allObstaclePoints.add(obstaclePoints);
                controlApp.addObstaclePoints(obstaclePoints);

            }
        }

        if (markerStrategy.equals("area")) {
            area = polygon;
            areaPointCheck = pointCheck;
            areaPoints = points;
            ((ControlApp) getActivity()).setAreaPoints(areaPoints);
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

        @SuppressWarnings("unchecked")
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter("KEY"));
    }

    @Override
    public void onResume() {
        super.onResume();

        localBroadcastManager.registerReceiver(broadcastReceiver, new IntentFilter("KEY"));
    }

    @Override
    public void onPause() {
        super.onPause();

        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("initialPoint", initialPoint);
        outState.putParcelableArrayList("areaPoints", areaPoints);
        outState.putParcelableArrayList("wayPoints", wayPoints);
        outState.putParcelableArrayList("obstaclePoints", obstaclePoints);
        outState.putInt("size", allObstaclePoints.size());
        for (int i = 0; i < allObstaclePoints.size(); i++){
            outState.putParcelableArrayList("item" + i, allObstaclePoints.get(i));
        }
        outState.putString("markerStrategy", markerStrategy);
        outState.putInt("areaPointCheck", areaPointCheck);
        outState.putInt("obstaclePointCheck", obstaclePointCheck);
        outState.putDouble("zoomLevel", mapView.getZoomLevelDouble());
        outState.putDouble("mapLocationLat", mapView.getMapCenter().getLatitude());
        outState.putDouble("mapLocationLong", mapView.getMapCenter().getLongitude());
    }

    public void controlMode() {
        switch (controlMode) {
            case Routing:
                markerStrategy = "routing";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();

                clearAll.setVisibility(View.VISIBLE);
                clearRouteButton.setVisibility(View.VISIBLE);
                clearAreaButton.setVisibility(View.GONE);
                newObstacleButton.setVisibility(View.GONE);
                clearObstacleButton.setVisibility(View.GONE);
                break;

            case Area:
                markerStrategy = "area";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();

                clearAll.setVisibility(View.VISIBLE);
                clearAreaButton.setVisibility(View.VISIBLE);
                clearRouteButton.setVisibility(View.GONE);
                newObstacleButton.setVisibility(View.GONE);
                clearObstacleButton.setVisibility(View.GONE);
                break;

            case Obstacles:
                markerStrategy = "obstacle";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();

                clearAll.setVisibility(View.VISIBLE);
                newObstacleButton.setVisibility(View.VISIBLE);
                clearObstacleButton.setVisibility(View.VISIBLE);
                clearAreaButton.setVisibility(View.GONE);
                clearRouteButton.setVisibility(View.GONE);
                break;

            default:
                Toast.makeText(mapView.getContext(), "Change Control Mode to Routing, Area or Obstacles to add markers", Toast.LENGTH_SHORT).show();

                clearAll.setVisibility(View.GONE);
                clearRouteButton.setVisibility(View.GONE);
                clearAreaButton.setVisibility(View.GONE);
                newObstacleButton.setVisibility(View.GONE);
                clearObstacleButton.setVisibility(View.GONE);
                break;
        }
    }
}

