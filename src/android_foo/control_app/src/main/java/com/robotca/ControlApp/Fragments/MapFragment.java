package com.robotca.ControlApp.Fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.robotca.ControlApp.BuildConfig;
import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.LocationProvider;
import com.robotca.ControlApp.Core.Savable;
import com.robotca.ControlApp.R;

import org.osmdroid.api.IMapController;
//import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
//import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
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

import java.util.ArrayList;
import java.util.Collections;

/**
 * Fragment containing the Map screen showing the real-world position of the Robot.
 *
 */
public class MapFragment extends Fragment implements MapEventsReceiver {

    // Log tag String
    private static final String TAG = "MapFragment";

    private MyLocationNewOverlay myLocationOverlay;
    private MyLocationNewOverlay secondMyLocationOverlay;
    private MapView mapView;
    Button robotRecenterButton, clearAreaButton, clearRouteButton, areaButton, routingButton;

    private int areaPosition = 0;

    ArrayList<Double> results = new ArrayList<>();
    ArrayList<GeoPoint> geoPoints = new ArrayList<>();
    ArrayList<Marker> areaMarkers = new ArrayList<>();
    ArrayList<Marker> routingMarkers = new ArrayList<>();
    ArrayList<GeoPoint> wayPoints = new ArrayList<>();
    private String markerStrategy = "route";

    Polygon polygon;
    Polyline route;

    /**
     * Default Constructor.
     */
    public MapFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("geoPoints", geoPoints);
        outState.putParcelableArrayList("wayPoints", wayPoints);
        outState.putInt("areaPosition", areaPosition);
        outState.putString("markerStrategy", markerStrategy);
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.fragment_map, null);
        mapView = (MapView) view.findViewById(R.id.mapview);
        robotRecenterButton = (Button) view.findViewById(R.id.recenter);
        clearAreaButton = view.findViewById(R.id.clear_area_button);
        areaButton = view.findViewById(R.id.area_button);
        routingButton = view.findViewById(R.id.routing_button);
        clearRouteButton = view.findViewById(R.id.clear_route_button);

        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        mapView.setClickable(true);
        //mapView.setBuiltInZoomControls(true);
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
            // Center or recenter on Android with a long press
            public boolean onLongClick(View v) {

                mapView.postInvalidate();
                myLocationOverlay.disableFollowLocation();
                mapView.postInvalidate();
                Toast.makeText(mapView.getContext(), "Centered on you", Toast.LENGTH_SHORT).show();
                secondMyLocationOverlay.enableFollowLocation();
                mapView.postInvalidate();
                return true;

            }
        });
        robotRecenterButton.setOnClickListener(new OnClickListener() {
            @Override
            // Center or recenter on robot with a tap
            public void onClick(View v) {
                secondMyLocationOverlay.disableFollowLocation();
                mapView.postInvalidate();
                Toast.makeText(mapView.getContext(), "Centered on the Robot", Toast.LENGTH_SHORT).show();
                myLocationOverlay.enableFollowLocation();
                mapView.postInvalidate();

            }
        });

        IMapController mapViewController = mapView.getController();
        mapViewController.setZoom(18.0);

        clearAreaButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Marker marker: areaMarkers) {
                    mapView.getOverlays().remove(marker);
                }
                mapView.getOverlays().remove(polygon);
                areaMarkers.clear();
                geoPoints.clear();
                areaPosition = 0;
                polygon = null;
                mapView.invalidate();
            }
        });

        clearRouteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Marker marker: routingMarkers) {
                    mapView.getOverlays().remove(marker);
                }
                mapView.getOverlays().remove(route);
                routingMarkers.clear();
                wayPoints.clear();
                route = null;
                mapView.invalidate();
            }
        });

        areaButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                markerStrategy = "area";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();
            }
        });

        routingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                markerStrategy = "routing";
                Toast.makeText(mapView.getContext(), "Marking-Strategy set to " + markerStrategy, Toast.LENGTH_LONG).show();
            }
        });

        if(savedInstanceState != null) {
            areaPosition = savedInstanceState.getInt("areaPosition");
            markerStrategy = savedInstanceState.getString("markerStrategy");
            geoPoints = savedInstanceState.getParcelableArrayList("geoPoints");
            wayPoints = savedInstanceState.getParcelableArrayList("wayPoints");

            if(!geoPoints.isEmpty()){
                addArea(areaPosition ,geoPoints.get(geoPoints.size()-1));
            }
            if(!wayPoints.isEmpty()){
                addRoute(wayPoints.get(wayPoints.size()-1));
                wayPoints.remove(1);
                for(int i = 1; i < wayPoints.size(); i++){
                    initializeRouteMarker(wayPoints.get(i));
                }
            }
        }

        return view;
    }

    private void initializeRouteMarker(GeoPoint geoPoint) {
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setDraggable(true);
        marker.setIcon(getResources().getDrawable(R.drawable.ic_flag_black_24dp).mutate());
        setRouteDragListener(marker);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
        routingMarkers.add(marker);
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
        Marker marker = new Marker(mapView);
        marker.setPosition(geoPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setDraggable(true);
        marker.setInfoWindow(null);
        addMarker(marker);

        if (markerStrategy.equals("area")) {
            geoPoints.add(geoPoint);
            marker.setDefaultIcon();
            if (areaMarkers.size() > 1) {
                addArea(areaPosition, geoPoint);
                areaPosition++;
            }
        } else if(markerStrategy.equals("routing")){
            wayPoints.add(geoPoint);
            marker.setIcon(getResources().getDrawable(R.drawable.ic_flag_black_24dp).mutate());
            setRouteDragListener(marker);
        addRoute(geoPoint);
        }
        /*GroundOverlay myGroundOverlay = new GroundOverlay(getActivity());
        myGroundOverlay.setPosition(geoPoint);
        try {
            //noinspection ConstantConditions,deprecation
            myGroundOverlay.setImage(getResources().getDrawable(R.drawable.ic_flag_black_24dp).mutate());
        }
        catch (NullPointerException e) {
            Log.e(TAG, "", e);
        }
        myGroundOverlay.setDimensions(25.0f);
        mapView.getOverlays().add(myGroundOverlay);
        mapView.postInvalidate();

//        // keep storage of areaMarkers and current location
//        waypoints.add(myLocationOverlay.getMyLocation());
//        waypoints.add(geoPoint);
//
//        Toast.makeText(mapView.getContext(), "Marked on (" + geoPoint.getLatitude() + "," +
//                geoPoint.getLongitude() + ")", Toast.LENGTH_LONG).show();
*/
        return true;
    }

    private void setRouteDragListener(Marker marker){
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                wayPoints.add(routingMarkers.indexOf(marker)+1, marker.getPosition());
                route.setPoints(wayPoints);
                mapView.invalidate();
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                wayPoints.remove(marker.getPosition());
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
            mapView.getOverlayManager().add(route);
            mapView.invalidate();

        }
    }

    public void addMarker(Marker marker) {
        mapView.getOverlays().add(marker);
        mapView.invalidate();
        if(markerStrategy.equals("area"))
            areaMarkers.add(marker);
        else if(markerStrategy.equals("routing")){
            routingMarkers.add(marker);
        }
    }

    public void removeMarker(Marker marker) {
        mapView.getOverlays().remove(marker);
        mapView.invalidate();
        if(markerStrategy.equals("area"))
            areaMarkers.remove(marker);
        else if(markerStrategy.equals("routing")){
            routingMarkers.remove(marker);
        }
    }

    public void addArea(int position, GeoPoint geoPoint) {
        /*Polyline polyline = new Polyline();
        polylines.add(polyline);
        ArrayList<GeoPoint> test = new ArrayList<>();
        test.add(geoPoints.get(geoPoints.size() - 2));
        test.add(geoPoints.get(geoPoints.size() - 1));
        polylines.get(position).setPoints(test);
        mapView.getOverlayManager().add(polylines.get(position));
        test.clear();*/

        if (polygon != null)
        {
            mapView.getOverlays().remove(polygon);

            geoPoints.remove(geoPoints.size() - 2);

            double newLon = geoPoint.getLongitude();
            double newLat = geoPoint.getLatitude();

            for (int i = 0; i < geoPoints.size() - 1; i++) {
                double lon = geoPoints.get(i).getLongitude();
                double lat = geoPoints.get(i).getLatitude();

                double distance = Math.sqrt(Math.pow((newLon - lon), 2) + Math.pow((newLat - lat), 2));

                results.add(distance);
                results = results;
            }

            ArrayList<Double> copyResults = (ArrayList<Double>) results.clone();
            Collections.sort(copyResults);
            double minValue = copyResults.get(0);
            double secondMinValue = copyResults.get(1);

            int i = results.indexOf(minValue);

            geoPoints.add(i + 1, geoPoint);
            geoPoints.remove(geoPoints.size() - 1);

            //geoPoints.remove(geoPoints.size() - 2);
            mapView.getOverlayManager().remove(polygon);

            polygon.getFillPaint().setARGB(75, 255, 0, 0);
            geoPoints.add(geoPoints.get(0));
            polygon.setPoints(geoPoints);

            mapView.getOverlayManager().add(polygon);
            mapView.invalidate();

            results.clear();
            copyResults.clear();
        } else {
            polygon = new Polygon();

            polygon.getFillPaint().setARGB(75, 255, 0, 0);
            geoPoints.add(geoPoints.get(0));
            polygon.setPoints(geoPoints);

            mapView.getOverlayManager().add(polygon);
            mapView.invalidate();
        }
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

