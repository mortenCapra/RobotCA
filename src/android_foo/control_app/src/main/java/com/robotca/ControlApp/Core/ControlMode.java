package com.robotca.ControlApp.Core;

import com.robotca.ControlApp.ControlApp;
import com.robotca.ControlApp.Core.Plans.RandomTrackPlan;
import com.robotca.ControlApp.Core.Plans.RobotPlan;
import com.robotca.ControlApp.Core.Plans.RoutePlan;
import com.robotca.ControlApp.UnusedCode.Plans.WaypointPlan;

/**
 * Enum for different ways to control the Robot.
 *
 * Created by Michael Brunson on 2/12/16.
 */
public enum ControlMode {
    Joystick (true), // Joystick control
    Tilt (true), // Tilt sensor control
    // Waypoint (false), // Potential field waypoint control
    Routing (false),
    RandomTrack (false),
    Obstacles (false);

    // Whether the user directly controls the Robot in this mode
    public final boolean USER_CONTROLLED;

    /**
     * Creates a ControlMode.
     * @param userControlled Whether the user controls the Robot directly in this mode.
     */
    ControlMode(boolean userControlled)
    {
        USER_CONTROLLED = userControlled;
    }

    /**
     * Creates a RobotPlan for the specified ControlMode if one exists.
     * @param controlApp The ControlApp
     * @param controlMode The ControlMode
     * @return A RobotPlan for the ControlMode or null if none exists
     */
    public static RobotPlan getRobotPlan(ControlApp controlApp, ControlMode controlMode) {

        RobotPlan plan;

        switch (controlMode) {

            case Routing: plan = new RoutePlan(controlApp); break;
            // case Waypoint: plan = new WaypointPlan(controlApp); break;
            case RandomTrack: plan = new RandomTrackPlan(controlApp); break;
            default: plan = null; break;
        }

        return plan;
    }
}
