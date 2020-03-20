package com.robotca.ControlApp.Core.Messages;

import geometry_msgs.Point;
import geometry_msgs.Quaternion;
import nav_msgs.Odometry;

public interface CapraOdometry extends Odometry {
    String _TYPE = "sensor_msgs/Imu";
    String _DEFINITION = "# This represents an estimate of a position and velocity in free space.  \n# The pose in this message should be specified in the coordinate frame given by header.frame_id.\n# The twist in this message should be specified in the coordinate frame given by the child_frame_id\nHeader header\nstring child_frame_id\ngeometry_msgs/PoseWithCovariance pose\ngeometry_msgs/TwistWithCovariance twist\n";


    Point getPosition();

    void setPosition(Point var1);

    Quaternion getOrientation();

    void setOrientation(Quaternion var1);

}
