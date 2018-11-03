/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

// This is an automatically generated file.

// Generated from the following "trajectory_msgs/JointTrajectory" msg definition:
//   Header header
//   string[] joint_names
//   JointTrajectoryPoint[] points// Instances of this class can be read and written with YARP ports,
// using a ROS-compatible format.

#ifndef YARP_ROSMSG_trajectory_msgs_JointTrajectory_h
#define YARP_ROSMSG_trajectory_msgs_JointTrajectory_h

#include <yarp/os/Wire.h>
#include <yarp/os/Type.h>
#include <yarp/os/idl/WireTypes.h>
#include <string>
#include <vector>
#include <yarp/rosmsg/std_msgs/Header.h>
#include <yarp/rosmsg/trajectory_msgs/JointTrajectoryPoint.h>

namespace yarp {
namespace rosmsg {
namespace trajectory_msgs {

class JointTrajectory : public yarp::os::idl::WirePortable
{
public:
    yarp::rosmsg::std_msgs::Header header;
    std::vector<std::string> joint_names;
    std::vector<yarp::rosmsg::trajectory_msgs::JointTrajectoryPoint> points;

    JointTrajectory() :
            header(),
            joint_names(),
            points()
    {
    }

    void clear()
    {
        // *** header ***
        header.clear();

        // *** joint_names ***
        joint_names.clear();

        // *** points ***
        points.clear();
    }

    bool readBare(yarp::os::ConnectionReader& connection) override
    {
        // *** header ***
        if (!header.read(connection)) {
            return false;
        }

        // *** joint_names ***
        int len = connection.expectInt32();
        joint_names.resize(len);
        for (int i=0; i<len; i++) {
            int len2 = connection.expectInt32();
            joint_names[i].resize(len2);
            if (!connection.expectBlock((char*)joint_names[i].c_str(), len2)) {
                return false;
            }
        }

        // *** points ***
        len = connection.expectInt32();
        points.resize(len);
        for (int i=0; i<len; i++) {
            if (!points[i].read(connection)) {
                return false;
            }
        }

        return !connection.isError();
    }

    bool readBottle(yarp::os::ConnectionReader& connection) override
    {
        connection.convertTextMode();
        yarp::os::idl::WireReader reader(connection);
        if (!reader.readListHeader(3)) {
            return false;
        }

        // *** header ***
        if (!header.read(connection)) {
            return false;
        }

        // *** joint_names ***
        if (connection.expectInt32() != (BOTTLE_TAG_LIST|BOTTLE_TAG_STRING)) {
            return false;
        }
        int len = connection.expectInt32();
        joint_names.resize(len);
        for (int i=0; i<len; i++) {
            int len2 = connection.expectInt32();
            joint_names[i].resize(len2);
            if (!connection.expectBlock((char*)joint_names[i].c_str(), len2)) {
                return false;
            }
        }

        // *** points ***
        if (connection.expectInt32() != BOTTLE_TAG_LIST) {
            return false;
        }
        len = connection.expectInt32();
        points.resize(len);
        for (int i=0; i<len; i++) {
            if (!points[i].read(connection)) {
                return false;
            }
        }

        return !connection.isError();
    }

    using yarp::os::idl::WirePortable::read;
    bool read(yarp::os::ConnectionReader& connection) override
    {
        return (connection.isBareMode() ? readBare(connection)
                                        : readBottle(connection));
    }

    bool writeBare(yarp::os::ConnectionWriter& connection) const override
    {
        // *** header ***
        if (!header.write(connection)) {
            return false;
        }

        // *** joint_names ***
        connection.appendInt32(joint_names.size());
        for (size_t i=0; i<joint_names.size(); i++) {
            connection.appendInt32(joint_names[i].length());
            connection.appendExternalBlock((char*)joint_names[i].c_str(), joint_names[i].length());
        }

        // *** points ***
        connection.appendInt32(points.size());
        for (size_t i=0; i<points.size(); i++) {
            if (!points[i].write(connection)) {
                return false;
            }
        }

        return !connection.isError();
    }

    bool writeBottle(yarp::os::ConnectionWriter& connection) const override
    {
        connection.appendInt32(BOTTLE_TAG_LIST);
        connection.appendInt32(3);

        // *** header ***
        if (!header.write(connection)) {
            return false;
        }

        // *** joint_names ***
        connection.appendInt32(BOTTLE_TAG_LIST|BOTTLE_TAG_STRING);
        connection.appendInt32(joint_names.size());
        for (size_t i=0; i<joint_names.size(); i++) {
            connection.appendInt32(joint_names[i].length());
            connection.appendExternalBlock((char*)joint_names[i].c_str(), joint_names[i].length());
        }

        // *** points ***
        connection.appendInt32(BOTTLE_TAG_LIST);
        connection.appendInt32(points.size());
        for (size_t i=0; i<points.size(); i++) {
            if (!points[i].write(connection)) {
                return false;
            }
        }

        connection.convertTextMode();
        return !connection.isError();
    }

    using yarp::os::idl::WirePortable::write;
    bool write(yarp::os::ConnectionWriter& connection) const override
    {
        return (connection.isBareMode() ? writeBare(connection)
                                        : writeBottle(connection));
    }

    // This class will serialize ROS style or YARP style depending on protocol.
    // If you need to force a serialization style, use one of these classes:
    typedef yarp::os::idl::BareStyle<yarp::rosmsg::trajectory_msgs::JointTrajectory> rosStyle;
    typedef yarp::os::idl::BottleStyle<yarp::rosmsg::trajectory_msgs::JointTrajectory> bottleStyle;

    // The name for this message, ROS will need this
    static constexpr const char* typeName = "trajectory_msgs/JointTrajectory";

    // The checksum for this message, ROS will need this
    static constexpr const char* typeChecksum = "65b4f94a94d1ed67169da35a02f33d3f";

    // The source text for this message, ROS will need this
    static constexpr const char* typeText = "\
Header header\n\
string[] joint_names\n\
JointTrajectoryPoint[] points\n\
================================================================================\n\
MSG: std_msgs/Header\n\
# Standard metadata for higher-level stamped data types.\n\
# This is generally used to communicate timestamped data \n\
# in a particular coordinate frame.\n\
# \n\
# sequence ID: consecutively increasing ID \n\
uint32 seq\n\
#Two-integer timestamp that is expressed as:\n\
# * stamp.sec: seconds (stamp_secs) since epoch (in Python the variable is called 'secs')\n\
# * stamp.nsec: nanoseconds since stamp_secs (in Python the variable is called 'nsecs')\n\
# time-handling sugar is provided by the client library\n\
time stamp\n\
#Frame this data is associated with\n\
# 0: no frame\n\
# 1: global frame\n\
string frame_id\n\
\n\
================================================================================\n\
MSG: trajectory_msgs/JointTrajectoryPoint\n\
# Each trajectory point specifies either positions[, velocities[, accelerations]]\n\
# or positions[, effort] for the trajectory to be executed.\n\
# All specified values are in the same order as the joint names in JointTrajectory.msg\n\
\n\
float64[] positions\n\
float64[] velocities\n\
float64[] accelerations\n\
float64[] effort\n\
duration time_from_start\n\
";

    yarp::os::Type getType() const override
    {
        yarp::os::Type typ = yarp::os::Type::byName(typeName, typeName);
        typ.addProperty("md5sum", yarp::os::Value(typeChecksum));
        typ.addProperty("message_definition", yarp::os::Value(typeText));
        return typ;
    }
};

} // namespace trajectory_msgs
} // namespace rosmsg
} // namespace yarp

#endif // YARP_ROSMSG_trajectory_msgs_JointTrajectory_h
