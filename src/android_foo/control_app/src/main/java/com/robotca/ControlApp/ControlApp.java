package com.robotca.ControlApp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;

import com.robotca.ControlApp.Core.ControlMode;
import com.robotca.ControlApp.Core.Dijkstra.DistanceScorer;
import com.robotca.ControlApp.Core.Dijkstra.GeoPointNode;
import com.robotca.ControlApp.Core.Dijkstra.Graph;
import com.robotca.ControlApp.Core.Dijkstra.RouteFinder;
import com.robotca.ControlApp.Core.DrawerItem;
import com.robotca.ControlApp.Core.IWaypointProvider;
import com.robotca.ControlApp.Core.NavDrawerAdapter;
import com.robotca.ControlApp.Core.Plans.RobotPlan;
import com.robotca.ControlApp.Core.RobotController;
import com.robotca.ControlApp.Core.RobotInfo;
import com.robotca.ControlApp.Core.RobotStorage;
import com.robotca.ControlApp.Core.Savable;
import com.robotca.ControlApp.Core.Utils;
import com.robotca.ControlApp.Core.Utils2;
import com.robotca.ControlApp.Core.WarningSystem;
import com.robotca.ControlApp.Fragments.AboutFragment;
import com.robotca.ControlApp.Fragments.CameraViewFragment;
import com.robotca.ControlApp.Fragments.HUDFragment;
import com.robotca.ControlApp.Fragments.HelpFragment;
import com.robotca.ControlApp.Fragments.JoystickFragment;
import com.robotca.ControlApp.Fragments.MapFragment;
import com.robotca.ControlApp.Fragments.OverviewFragment;
import com.robotca.ControlApp.Fragments.PreferencesFragment;
import com.robotca.ControlApp.Fragments.RosFragment;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.rosjava_geometry.Vector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.robotca.ControlApp.Core.Utils2.*;

/**
 * Main Activity for the App. The RobotController manages the connection with the Robot while this
 * class handles the UI.
 */
public class ControlApp extends RosActivity implements ListView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    /** Notification ticker for the App */
    public static final String NOTIFICATION_TICKER = "ROS Control";
    /** Notification title for the App */
    public static final String NOTIFICATION_TITLE = "ROS Control";
    /** Permission request code */
    private static final int PERMISSIONS_REQUEST_CODE = 123;

    /** The RobotInfo of the connected Robot */
    public static RobotInfo ROBOT_INFO;

    // Variables for managing the DrawerLayout
    private String[] mFeatureTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    // NodeMainExecutor encapsulating the Robot's connection
    private NodeMainExecutor nodeMainExecutor;
    // The NodeConfiguration for the connection
    private NodeConfiguration nodeConfiguration;

    // Fragment for the Joystick
    private JoystickFragment joystickFragment;
    // Fragment for the HUD
    private HUDFragment hudFragment;

    // The RobotController for managing the connection to the Robot
    private RobotController controller;
    // The WarningSystem used for detecting imminent collisions
    private WarningSystem warningSystem;

    // Stuff for managing the current fragment
    private Fragment fragment = null;
    private MapFragment map = null;
    FragmentManager fragmentManager;
    int fragmentsCreatedCounter = 0;

    // The index of the currently visible drawer
    private int drawerIndex = 1;

    // Log tag String
    private static final String TAG = "ControlApp";
    // List of routePoints
    private boolean loopFlag;
    private LinkedList<GeoPoint> routePoints;
    private LinkedList<GeoPoint> routePointsCopy;

    // Bundle keys
    private static final String SELECTED_VIEW_NUMBER_BUNDLE_ID = "com.robotca.ControlApp.drawerIndex";
    private static final String CONTROL_MODE_BUNDLE_ID = "com.robotca.Views.Fragments.JoystickFragment.controlMode";

    private HashMap<Integer, Fragment.SavedState> fragmentSavedStates = new HashMap<>();

    // The saved instance state
    private Bundle savedInstanceState;

    private Fragment.SavedState savedState;

    private LocalBroadcastManager localBroadcastManager;
    //
    ArrayList<GeoPoint> randomTrackPoints = new ArrayList<>();
    ArrayList<ArrayList<GeoPoint>> obstaclePoints = new ArrayList<>();

    Polygon randomTrack = new Polygon();
    MapView mapView;

    /**
     * Default Constructor.
     */
    public ControlApp() {
        super(NOTIFICATION_TICKER, NOTIFICATION_TITLE, ROBOT_INFO.getUri());

        routePoints = new LinkedList<>();
        routePointsCopy = new LinkedList<>();

        // Create the laserScanMap
        // laserScanMap = new LaserScanMap();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Check for permissions
        checkPermissions();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        // Set default preference values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);

        if (ROBOT_INFO != null) {
            ROBOT_INFO.save(editor);

        // editor.putString(getString(R.string.prefs_joystick_topic_edittext_key), ROBOT_INFO.getJoystickTopic());
        // editor.putString(getString(R.string.prefs_laserscan_topic_edittext_key), ROBOT_INFO.getLaserTopic());
        // editor.putString(getString(R.string.prefs_camera_topic_edittext_key), ROBOT_INFO.getCameraTopic());
        // editor.putString(getString(R.string.prefs_navsat_topic_edittext_key), ROBOT_INFO.getNavSatTopic());
        // editor.putString(getString(R.string.prefs_odometry_topic_edittext_key), ROBOT_INFO.getOdometryTopic());
        // editor.putString(getString(R.string.prefs_pose_topic_edittext_key), ROBOT_INFO.getPoseTopic());
        }

        // editor.putBoolean(getString(R.string.prefs_warning_checkbox_key), true);
        // editor.putBoolean(getString(R.string.prefs_warning_safemode_key), true);
        // editor.putBoolean(getString(R.string.prefs_warning_beep_key), true);

        editor.apply();

        // Set the main content view
        setContentView(R.layout.main);

        mFeatureTitles = getResources().getStringArray(R.array.feature_titles); // Where you set drawer item titles
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerList = findViewById(R.id.left_drawer);

        if (getActionBar() != null) {
            ActionBar actionBar = getActionBar();

            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setLogo(R.drawable.capra_logo_small);
            actionBar.setDisplayUseLogoEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);

            actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.White)));
            actionBar.setIcon(new ColorDrawable(Color.TRANSPARENT));

            /*
            // Set custom Action Bar view
            LayoutInflater inflater = (LayoutInflater) this .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.actionbar_dropdown_menu, null);


            actionBar.setCustomView(v);
            actionBar.setDisplayShowCustomEnabled(true);

             */

        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                //R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                //getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                //getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to
                // onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        //noinspection deprecation
        mDrawerLayout.setDrawerListener(mDrawerToggle);


        int[] imgRes = new int[]{
                R.drawable.ic_android_black_24dp,
                //R.drawable.ic_view_quilt_black_24dp,
                //R.drawable.ic_linked_camera_black_24dp,
                R.drawable.ic_navigation_black_24dp,
                //R.drawable.ic_terrain_black_24dp,
                R.drawable.ic_settings_black_24dp,
                R.drawable.ic_info_outline_black_24dp
        };

        List<DrawerItem> drawerItems = new ArrayList<>();

        for (int i = 0; i < mFeatureTitles.length; i++) {
            drawerItems.add(new DrawerItem(mFeatureTitles[i], imgRes[i]));
        }

        NavDrawerAdapter drawerAdapter = new NavDrawerAdapter(this,
                R.layout.nav_drawer_menu_item,
                drawerItems);

        mDrawerList.setAdapter(drawerAdapter);
        mDrawerList.setOnItemClickListener(this);

        // Find the Joystick fragment
        joystickFragment = (JoystickFragment) getFragmentManager().findFragmentById(R.id.joystick_fragment_right);

        // Create the RobotController
        controller = new RobotController(this);

        this.savedInstanceState = savedInstanceState;

        if (savedInstanceState != null) {
            setControlMode(ControlMode.values()[savedInstanceState.getInt(CONTROL_MODE_BUNDLE_ID)]);
            drawerIndex = savedInstanceState.getInt(SELECTED_VIEW_NUMBER_BUNDLE_ID);

            // Load the controller
            controller.load(savedInstanceState);
        }
        // Hud fragment
        hudFragment = (HUDFragment) getFragmentManager().findFragmentById(R.id.hud_fragment);
    }

    @Override
    protected void onStop() {
        RobotStorage.update(this, ROBOT_INFO);

        Log.d(TAG, "onStop()");

        if (controller != null)
            controller.stop();

        if (joystickFragment != null)
            joystickFragment.stop();
        onTrimMemory(TRIM_MEMORY_BACKGROUND);
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        if (controller != null)
            controller.stop();

        if (joystickFragment != null)
            joystickFragment.stop();

        onTrimMemory(TRIM_MEMORY_BACKGROUND);
        onTrimMemory(TRIM_MEMORY_COMPLETE);
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        // Save Control Mode
        bundle.putInt(CONTROL_MODE_BUNDLE_ID, getControlMode().ordinal());
        // Save current drawer
        bundle.putInt(SELECTED_VIEW_NUMBER_BUNDLE_ID, drawerIndex);

        // Save the RobotController
        if (controller != null)
            controller.save(bundle);

        // Save the current fragment if applicable
        if (fragment instanceof Savable)
            ((Savable) fragment).save(bundle);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(), getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();

            this.nodeMainExecutor = nodeMainExecutor;
            this.nodeConfiguration =
                    NodeConfiguration.newPublic(local_network_address.getHostAddress(), getMasterUri());


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    joystickFragment.invalidate();
                }
            });

            //controller.setTopicName(PreferenceManager.getDefaultSharedPreferences(this).getString("edittext_joystick_topic", getString(R.string.joy_topic)));
            controller.initialize(nodeMainExecutor, nodeConfiguration);

            // Add the HUDFragment to the RobotController's odometry listener
            controller.addOdometryListener(hudFragment);
            controller.addimuListener(hudFragment);
            // Add the JoystickView to the RobotController's odometry listener
            controller.addOdometryListener(joystickFragment.getJoystickView());
            controller.addimuListener(joystickFragment.getJoystickView());
            // Create and add a WarningSystem
            controller.addLaserScanListener(warningSystem = new WarningSystem(this));

            // Add the LaserScanMap
            // controller.addLaserScanListener(laserScanMap);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    View temp = mDrawerList.getChildAt(drawerIndex);
                    mDrawerList.onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                            SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, temp.getX(), temp.getY(), 0));

                    selectItem(drawerIndex);
                }
            });
        } catch (Exception e) {
            // Socket problem
            Log.e(TAG, "socket error trying to get networking information from the master uri", e);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        saveState(fragment, drawerIndex);
        selectItem(position);
    }

    /**
     * @return The RobotController
     */
    public RobotController getRobotController() {
        return controller;
    }

    /**
     * @return The HUDFragment
     */
    public HUDFragment getHUDFragment() {
        return hudFragment;
    }

    /**
     * @return The WarningSystem
     */
    public WarningSystem getWarningSystem() {
        return warningSystem;
    }

    /**
     * Called when a collision is imminent from the WarningSystem.
     */
    public void collisionWarning() {
//        Log.d(TAG, "Collision Warning!");

        hudFragment.warn();
    }

    /**
     * Call to stop the Robot.
     *
     * @param cancelMotionPlan Whether to cancel the current motion plan
     * @return True if a resumable RobotPlan was stopped
     */
    public boolean stopRobot(boolean cancelMotionPlan) {
        Log.d(TAG, "Stopping Robot");
        joystickFragment.stop();
        return controller.stop(cancelMotionPlan);
    }

    // /**
    // * @return The current laser scan map
    // */
    // public static LaserScanMap getLaserScanMap()
    // {
    //      return laserScanMap;
    // }

    // /**
    // * Call to stop the Robot.
    // *
    // * @return True if a resumable RobotPlan was stopped
    // */
    // public boolean stopRobot() {
    //      return stopRobot(true);
    // }

    /**
     * Locks/unlocks the screen orientation.
     * Adapted from an answer on StackOverflow by jp36
     *
     * @param lock Whether to lock the orientation
     */
    public void lockOrientation(boolean lock) {

        if (lock) {
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            int rotation = display.getRotation();
            int tempOrientation = getResources().getConfiguration().orientation;
            int orientation = 0;

            switch (tempOrientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                        orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    else
                        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Configuration.ORIENTATION_PORTRAIT:
                    if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                        orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    else
                        orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
            }

            //noinspection ResourceType
            setRequestedOrientation(orientation);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    /**
     * get the current fragment being shown
     * @return the fragment being shown
     */
    public Fragment getFragment(){
        return fragment;
    }

    /**
     * get the saved state of the latest mapfragment
     * @return
     */
    public Fragment.SavedState getSavedState(){
        return savedState;
    }


    /**
     * Swaps fragments in the main content view.
     * @param position of the item in the navdrawer pressed. A hack with camerafragment and overview has been applied
     */
    public void selectItem(int position) {

        Bundle args = new Bundle();

        if (joystickFragment != null && getControlMode().ordinal() <= ControlMode.Tilt.ordinal()) {
            joystickFragment.show();
        }

        if (hudFragment != null) {
            hudFragment.show();
        }

        if (controller != null) {
            controller.initialize();
        }
        fragmentManager = getFragmentManager();

        if (fragment instanceof OverviewFragment || fragment instanceof MapFragment){
            savedState = fragmentManager.saveFragmentInstanceState(fragment);
        }


        switch (position) {
            case 10:

                fragment = new CameraViewFragment();
                fragmentsCreatedCounter = fragmentsCreatedCounter + 1;
                position = 1;
                break;

            case 20:
                fragment = new OverviewFragment();
                fragment.setInitialSavedState(savedState);
                fragmentsCreatedCounter = fragmentsCreatedCounter +1;
                position = 1;
                break;

            case 0:
                Log.d(TAG, "Drawer item 0 selected, finishing");

                fragmentsCreatedCounter = 0;

                int count = fragmentManager.getBackStackEntryCount();
                for (int i = 0; i < count; ++i) {
                    fragmentManager.popBackStackImmediate();
                }

                if (controller != null) {
                    controller.shutdownTopics();

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            nodeMainExecutor.shutdownNodeMain(controller);
                            return null;
                        }
                    }.execute();
                }

                finish();

                return;

            case 1:
                fragment = new MapFragment();
                fragment.setInitialSavedState(savedState);
                map = (MapFragment) fragment;
                fragmentsCreatedCounter = fragmentsCreatedCounter + 1;
                break;

            case 2:
                if (joystickFragment != null)
                    joystickFragment.hide();
                if (hudFragment != null) {
                    hudFragment.hide();

                    boolean stop = controller.getMotionPlan() == null || !controller.getMotionPlan().isResumable();
                    stop &= !controller.hasPausedPlan();
                    hudFragment.toggleEmergencyStopUI(stop);
                }

                stopRobot(false);

                fragment = new PreferencesFragment();
                fragmentsCreatedCounter = fragmentsCreatedCounter + 1;
                break;

            case 3:
                if (joystickFragment != null)
                    joystickFragment.hide();
                if (hudFragment != null) {
                    hudFragment.hide();

                    boolean stop = controller.getMotionPlan() == null || !controller.getMotionPlan().isResumable();
                    stop &= !controller.hasPausedPlan();
                    hudFragment.toggleEmergencyStopUI(stop);
                }

                stopRobot(false);

                fragment = new AboutFragment();
                fragmentsCreatedCounter = fragmentsCreatedCounter + 1;

            default:
                break;
        }

        drawerIndex = position;

        try {
            //noinspection ConstantConditions
            ((RosFragment) fragment).initialize(nodeMainExecutor, nodeConfiguration);
        } catch (Exception e) {
            // Ignore
        }

        if (fragment != null) {
            fragment.setArguments(args);

            // Insert the fragment by replacing any existing fragment
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();

            restoreState(fragment, position);

            /*if (fragment instanceof Savable && savedInstanceState != null)
                ((Savable) fragment).load(savedInstanceState);
*/
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
        setTitle(mFeatureTitles[position]);
    }

    @Override
    public void setTitle(CharSequence title) {
        try {
            //noinspection ConstantConditions
            getActionBar().setTitle(title);
        } catch (NullPointerException e) {
            // Ignore
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        switch (item.getItemId()) {
            case R.id.action_joystick_control:
                setControlMode(ControlMode.Joystick);
                return true;

            case R.id.action_motion_control:
                setControlMode(ControlMode.Tilt);
                return true;

            case R.id.action_routing_control:
                setControlMode(ControlMode.Routing);
                return true;

            case R.id.action_random_track_control:
                setControlMode(ControlMode.RandomTrack);
                return true;

            case R.id.action_obstacle_control:
                setControlMode(ControlMode.Obstacles);
                return true;

            default:
                return mDrawerToggle.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {

        if (fragmentsCreatedCounter >= 1) {
            selectItem(1);
            fragmentsCreatedCounter = 0;
        } else {
            super.onBackPressed();
        }
    }

    /*@Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        for (int i = 0; i < menu.size(); i++) {
        menu.getItem(i).setChecked(false);

        if (i == 1)
            menu.getItem(1).setEnabled(actionMenuEnabled && joystickFragment.hasAccelerometer());
        else
            menu.getItem(i).setEnabled(actionMenuEnabled);
        }

        menu.getItem(getControlMode().ordinal()).setChecked(true);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_control_app, menu);
        menu.getItem(0).setChecked(true);
        return true;
    }*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Called when the preferences may have changed.
     *
     * @param prefs The SharedPrefences object
     */
    public void onPreferencesChanged(SharedPreferences prefs) {

        // Warning System
        warningSystem.setEnabled(prefs.getBoolean(getString(R.string.prefs_warning_checkbox_key), true));
        warningSystem.enableSafemode(prefs.getBoolean(getString(R.string.prefs_warning_safemode_key), true));

        // Beep beep
        hudFragment.setBeepsEnabled(prefs.getBoolean(getString(R.string.prefs_warning_beep_key), true));

        // Refresh topic subscribers/publishers
        controller.refreshTopics();

    }

    /**
     * @return the Robot's current ControlMode
     */
    public ControlMode getControlMode() {
        return joystickFragment.getControlMode();
    }

    /**
     * Sets the ControlMode for controlling the Robot.
     *
     * @param controlMode The new ControlMode
     */
    public void setControlMode(ControlMode controlMode) {

        if (joystickFragment.getControlMode() == controlMode)
            return;

        // Lock the orientation for tilt controls
        lockOrientation(controlMode == ControlMode.Tilt);

        // Notify the Joystick on the new ControlMode
        joystickFragment.setControlMode(controlMode);
        hudFragment.toggleEmergencyStopUI(true);

        // If the ControlMode has an associated RobotPlan, run the plan
        RobotPlan robotPlan = ControlMode.getRobotPlan(this, controlMode);
        if (robotPlan != null) {
            controller.runPlan(robotPlan);
        } else {
            controller.stop();
        }

        invalidateOptionsMenu();

       /* if (controlMode == ControlMode.Waypoint) {
            Toast.makeText(this, "Tap twice to place or delete a waypoint. " +
                    "Tap and hold a waypoint to move it.", Toast.LENGTH_LONG).show();
        }

        */

        if (controlMode == ControlMode.Routing){
            Toast.makeText(this, "Look at and change route in map", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Callback for when a Spinner item is selected from the ActionBar.
     *
     * @param parent   The AdapterView where the selection happened
     * @param view     The view within the AdapterView that was clicked
     * @param position The position of the view in the adapter
     * @param id       The row id of the item that is selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        setControlMode(ControlMode.values()[position]);
        Intent intent = new Intent("KEY");
        intent.putExtra("MODE", getControlMode());
        localBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Callback for when a Spinner item is selected from the ActionBar.
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Check for Permissions
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                + ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                }, PERMISSIONS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.CAMERA
                }, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1] + grantResults[2] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    /**
     * restores the state of a fragment if a savedstate exists
     * @param fragment to restore state of
     * @param key to find correct saved state
     */
    public void restoreState(Fragment fragment, int key){
        if(fragmentSavedStates.get(key) != null){
            if(!fragment.isAdded()){
                fragment.setInitialSavedState(fragmentSavedStates.get(key));
            }
        }
    }

    /**
     * save the state of a fragment
     * @param fragment to save state of
     * @param key to the saved state for alter retrieval
     */
    public void saveState(Fragment fragment, int key){
        if(fragment.isAdded()){
            fragmentSavedStates.put(key, fragmentManager.saveFragmentInstanceState(fragment));
        }
    }

    /**
     * sets the randomTrackPoints
     * @param randomTrackPoints to set
     */
    public void setRandomTrackPoints(ArrayList<GeoPoint> randomTrackPoints) {
        this.randomTrackPoints = randomTrackPoints;
    }

    /**
     * gets the randomTrackPoints
     * @return the randomTrackPoints
     */
    public ArrayList<GeoPoint> getRandomTrackPoints() {
        return randomTrackPoints;
    }

    public void setRandomTrack(Polygon randomTrack) {
        this.randomTrack = randomTrack;
    }

    public Polygon getRandomTrack() {
        return randomTrack;
    }

    public void setMapView(MapView mapView) {
        this.mapView = mapView;
    }

    public MapView getMapView() {
        return mapView;
    }

    /**
     * add obstacle points
     * @param obstaclePoints to add
     */
    public void addObstaclePoints(ArrayList<GeoPoint> obstaclePoints) {
        this.obstaclePoints.add(obstaclePoints);
        checkRoute(0, this.obstaclePoints.indexOf(obstaclePoints));
    }

    /**
     * clears the obstacles
     */
    public void clearObstaclePoints(){
        obstaclePoints.clear();
    }

    /**
     * returns the obstalce points
     * @return a list of obstalces
     */
    public ArrayList<ArrayList<GeoPoint>> getObstaclePoints() {
        return obstaclePoints;
    }

    /**
     * add a point to a route
     * @param v the point to add
     */
    public void addPointToRoute(GeoPoint v){
        routePoints.addLast(v);
        routePointsCopy.addLast(v);
        checkRoute(routePoints.indexOf(v), 0);
    }

    /**
     * clears the route
     */
    public void clearRoute(){
        routePoints.clear();
        routePointsCopy.clear();
        RobotController.resetGps();
    }

    /**
     * gets the next point in route
     * @return the first point in the route
     */
    public GeoPoint getNextPointInRoute(){
        return routePoints.peekFirst();
    }

    /**
     * polls the next point in the route
     * @return the polled point
     */
    public GeoPoint pollNextPointInRoute(){
        GeoPoint p;
        p = routePoints.pollFirst();
        routePointsCopy.remove(p);
        if (loopFlag) {
            routePoints.addLast(p);
            routePointsCopy.addLast(p);
        } else if (fragment == getMap()) {
            getMap().removePointsFromRoute(routePointsCopy.size());
        }
        return p;
    }

    /**
     * alter a point in the route
     * @param oldP the old point
     * @param newP the new point
     */
    public void alterPointInRoute(GeoPoint oldP, GeoPoint newP){
        int j = routePointsCopy.indexOf(oldP);
        routePointsCopy.remove(oldP);
        routePointsCopy.add(j, newP);
        routePoints = routePointsCopy;
        checkRoute(0, 0);

    }

    /**
     * gets the points in the route
     * @return a linkedlsit of the routepoints
     */
    public LinkedList<GeoPoint> getRoutePoints(){
        return routePoints;
    }

    /**
     * gets the mapfragment of the application
     */
    public MapFragment getMap(){
        return map;
    }

    /**
     * checks the route for intersections with obstacles, and finds a way around them
     * @param routeIndex index of the where on the route to start checking from
     * @param obstacleIndex index of what obstalce to start checking from
     * @return false if no fix is applicable
     */
    public boolean checkRoute(int routeIndex, int obstacleIndex) {
        for(int i = routeIndex; i < routePoints.size(); i++) {
            for (int j = obstacleIndex; j < obstaclePoints.size(); j++) {
                ArrayList<GeoPoint> obstacle = obstaclePoints.get(j);
                if (isPointContainedInObstacle(obstacle, RobotController.getCurrentGPSLocation())){
                    Toast.makeText(this, "Robot is inside an obstacle. Move Robot or obstacle to continue", Toast.LENGTH_LONG).show();
                    stopRobot(true);
                    hudFragment.toggleEmergencyStopUI(false);
                    return false;
                }

                if (isPointContainedInObstacle(obstacle, routePoints.get(i))) {
                    routePoints.remove(i);
                    routePointsCopy.remove(i);

                    return checkRoute(i, j);
                }

                LinkedList<GeoPoint> res = checkObstacle(obstacle, routePoints, i);
                if (res == null){
                    Toast.makeText(this, "This route is not possible", Toast.LENGTH_LONG).show();
                    hudFragment.toggleEmergencyStopUI(false);
                    stopRobot(true);
                    return false;
                } else{
                    routePoints = res;
                }
            }
        }
        return true;
    }

    public void setLoopFlag(Boolean value){
        loopFlag = value;
        if (value) {
            Toast.makeText(this, "Route is looping", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Route is not looping", Toast.LENGTH_LONG).show();
        }
    }
}
