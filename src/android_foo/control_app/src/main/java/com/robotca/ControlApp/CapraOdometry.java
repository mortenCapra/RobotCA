package com.robotca.ControlApp;

import org.ros.internal.message.Message;

import geometry_msgs.PoseWithCovariance;
import geometry_msgs.TwistWithCovariance;
import std_msgs.Header;
import geometry_msgs.Point;
import geometry_msgs.Quaternion;

public interface CapraOdometry extends Message {
    String _TYPE = "nav_msgs/Odometry";

    Header getHeader();

    void setHeader(Header var1);

    String getChildFrameId();

    PoseWithCovariance getPose();

    void setPose(PoseWithCovariance var1);

    TwistWithCovariance getTwist();

    void setTwist(TwistWithCovariance var1);

    Point getPosition();

    void setPosition(Point var1);

    Quaternion getOrientation();

    void setOrientation(Quaternion var1);
}
