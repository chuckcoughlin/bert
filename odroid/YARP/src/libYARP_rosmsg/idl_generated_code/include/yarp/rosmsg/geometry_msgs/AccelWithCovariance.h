/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

// This is an automatically generated file.

// Generated from the following "geometry_msgs/AccelWithCovariance" msg definition:
//   # This expresses acceleration in free space with uncertainty.
//   
//   Accel accel
//   
//   # Row-major representation of the 6x6 covariance matrix
//   # The orientation parameters use a fixed-axis representation.
//   # In order, the parameters are:
//   # (x, y, z, rotation about X axis, rotation about Y axis, rotation about Z axis)
//   float64[36] covariance
// Instances of this class can be read and written with YARP ports,
// using a ROS-compatible format.

#ifndef YARP_ROSMSG_geometry_msgs_AccelWithCovariance_h
#define YARP_ROSMSG_geometry_msgs_AccelWithCovariance_h

#include <yarp/os/Wire.h>
#include <yarp/os/Type.h>
#include <yarp/os/idl/WireTypes.h>
#include <string>
#include <vector>
#include <yarp/rosmsg/geometry_msgs/Accel.h>

namespace yarp {
namespace rosmsg {
namespace geometry_msgs {

class AccelWithCovariance : public yarp::os::idl::WirePortable
{
public:
    yarp::rosmsg::geometry_msgs::Accel accel;
    std::vector<yarp::conf::float64_t> covariance;

    AccelWithCovariance() :
            accel(),
            covariance()
    {
        covariance.resize(36, 0.0);
    }

    void clear()
    {
        // *** accel ***
        accel.clear();

        // *** covariance ***
        covariance.clear();
        covariance.resize(36, 0.0);
    }

    bool readBare(yarp::os::ConnectionReader& connection) override
    {
        // *** accel ***
        if (!accel.read(connection)) {
            return false;
        }

        // *** covariance ***
        int len = 36;
        covariance.resize(len);
        if (len > 0 && !connection.expectBlock((char*)&covariance[0], sizeof(yarp::conf::float64_t)*len)) {
            return false;
        }

        return !connection.isError();
    }

    bool readBottle(yarp::os::ConnectionReader& connection) override
    {
        connection.convertTextMode();
        yarp::os::idl::WireReader reader(connection);
        if (!reader.readListHeader(2)) {
            return false;
        }

        // *** accel ***
        if (!accel.read(connection)) {
            return false;
        }

        // *** covariance ***
        if (connection.expectInt32() != (BOTTLE_TAG_LIST|BOTTLE_TAG_FLOAT64)) {
            return false;
        }
        int len = connection.expectInt32();
        covariance.resize(len);
        for (int i=0; i<len; i++) {
            covariance[i] = (yarp::conf::float64_t)connection.expectFloat64();
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
        // *** accel ***
        if (!accel.write(connection)) {
            return false;
        }

        // *** covariance ***
        if (covariance.size()>0) {
            connection.appendExternalBlock((char*)&covariance[0], sizeof(yarp::conf::float64_t)*covariance.size());
        }

        return !connection.isError();
    }

    bool writeBottle(yarp::os::ConnectionWriter& connection) const override
    {
        connection.appendInt32(BOTTLE_TAG_LIST);
        connection.appendInt32(2);

        // *** accel ***
        if (!accel.write(connection)) {
            return false;
        }

        // *** covariance ***
        connection.appendInt32(BOTTLE_TAG_LIST|BOTTLE_TAG_FLOAT64);
        connection.appendInt32(covariance.size());
        for (size_t i=0; i<covariance.size(); i++) {
            connection.appendFloat64(covariance[i]);
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
    typedef yarp::os::idl::BareStyle<yarp::rosmsg::geometry_msgs::AccelWithCovariance> rosStyle;
    typedef yarp::os::idl::BottleStyle<yarp::rosmsg::geometry_msgs::AccelWithCovariance> bottleStyle;

    // The name for this message, ROS will need this
    static constexpr const char* typeName = "geometry_msgs/AccelWithCovariance";

    // The checksum for this message, ROS will need this
    static constexpr const char* typeChecksum = "ad5a718d699c6be72a02b8d6a139f334";

    // The source text for this message, ROS will need this
    static constexpr const char* typeText = "\
# This expresses acceleration in free space with uncertainty.\n\
\n\
Accel accel\n\
\n\
# Row-major representation of the 6x6 covariance matrix\n\
# The orientation parameters use a fixed-axis representation.\n\
# In order, the parameters are:\n\
# (x, y, z, rotation about X axis, rotation about Y axis, rotation about Z axis)\n\
float64[36] covariance\n\
\n\
================================================================================\n\
MSG: geometry_msgs/Accel\n\
# This expresses acceleration in free space broken into its linear and angular parts.\n\
Vector3  linear\n\
Vector3  angular\n\
\n\
================================================================================\n\
MSG: geometry_msgs/Vector3\n\
# This represents a vector in free space. \n\
# It is only meant to represent a direction. Therefore, it does not\n\
# make sense to apply a translation to it (e.g., when applying a \n\
# generic rigid transformation to a Vector3, tf2 will only apply the\n\
# rotation). If you want your data to be translatable too, use the\n\
# geometry_msgs/Point message instead.\n\
\n\
float64 x\n\
float64 y\n\
float64 z\n\
";

    yarp::os::Type getType() const override
    {
        yarp::os::Type typ = yarp::os::Type::byName(typeName, typeName);
        typ.addProperty("md5sum", yarp::os::Value(typeChecksum));
        typ.addProperty("message_definition", yarp::os::Value(typeText));
        return typ;
    }
};

} // namespace geometry_msgs
} // namespace rosmsg
} // namespace yarp

#endif // YARP_ROSMSG_geometry_msgs_AccelWithCovariance_h
