/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

// This is an automatically generated file.

// Generated from the following "std_msgs/Int16MultiArray" msg definition:
//   # Please look at the MultiArrayLayout message definition for
//   # documentation on all multiarrays.
//   
//   MultiArrayLayout  layout        # specification of data layout
//   int16[]           data          # array of data
//   
// Instances of this class can be read and written with YARP ports,
// using a ROS-compatible format.

#ifndef YARP_ROSMSG_std_msgs_Int16MultiArray_h
#define YARP_ROSMSG_std_msgs_Int16MultiArray_h

#include <yarp/os/Wire.h>
#include <yarp/os/Type.h>
#include <yarp/os/idl/WireTypes.h>
#include <string>
#include <vector>
#include <yarp/rosmsg/std_msgs/MultiArrayLayout.h>

namespace yarp {
namespace rosmsg {
namespace std_msgs {

class Int16MultiArray : public yarp::os::idl::WirePortable
{
public:
    yarp::rosmsg::std_msgs::MultiArrayLayout layout;
    std::vector<std::int16_t> data;

    Int16MultiArray() :
            layout(),
            data()
    {
    }

    void clear()
    {
        // *** layout ***
        layout.clear();

        // *** data ***
        data.clear();
    }

    bool readBare(yarp::os::ConnectionReader& connection) override
    {
        // *** layout ***
        if (!layout.read(connection)) {
            return false;
        }

        // *** data ***
        int len = connection.expectInt32();
        data.resize(len);
        if (len > 0 && !connection.expectBlock((char*)&data[0], sizeof(std::int16_t)*len)) {
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

        // *** layout ***
        if (!layout.read(connection)) {
            return false;
        }

        // *** data ***
        if (connection.expectInt32() != (BOTTLE_TAG_LIST|BOTTLE_TAG_INT16)) {
            return false;
        }
        int len = connection.expectInt32();
        data.resize(len);
        for (int i=0; i<len; i++) {
            data[i] = (std::int16_t)connection.expectInt16();
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
        // *** layout ***
        if (!layout.write(connection)) {
            return false;
        }

        // *** data ***
        connection.appendInt32(data.size());
        if (data.size()>0) {
            connection.appendExternalBlock((char*)&data[0], sizeof(std::int16_t)*data.size());
        }

        return !connection.isError();
    }

    bool writeBottle(yarp::os::ConnectionWriter& connection) const override
    {
        connection.appendInt32(BOTTLE_TAG_LIST);
        connection.appendInt32(2);

        // *** layout ***
        if (!layout.write(connection)) {
            return false;
        }

        // *** data ***
        connection.appendInt32(BOTTLE_TAG_LIST|BOTTLE_TAG_INT16);
        connection.appendInt32(data.size());
        for (size_t i=0; i<data.size(); i++) {
            connection.appendInt16(data[i]);
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
    typedef yarp::os::idl::BareStyle<yarp::rosmsg::std_msgs::Int16MultiArray> rosStyle;
    typedef yarp::os::idl::BottleStyle<yarp::rosmsg::std_msgs::Int16MultiArray> bottleStyle;

    // The name for this message, ROS will need this
    static constexpr const char* typeName = "std_msgs/Int16MultiArray";

    // The checksum for this message, ROS will need this
    static constexpr const char* typeChecksum = "d9338d7f523fcb692fae9d0a0e9f067c";

    // The source text for this message, ROS will need this
    static constexpr const char* typeText = "\
# Please look at the MultiArrayLayout message definition for\n\
# documentation on all multiarrays.\n\
\n\
MultiArrayLayout  layout        # specification of data layout\n\
int16[]           data          # array of data\n\
\n\
\n\
================================================================================\n\
MSG: std_msgs/MultiArrayLayout\n\
# The multiarray declares a generic multi-dimensional array of a\n\
# particular data type.  Dimensions are ordered from outer most\n\
# to inner most.\n\
\n\
MultiArrayDimension[] dim # Array of dimension properties\n\
uint32 data_offset        # padding elements at front of data\n\
\n\
# Accessors should ALWAYS be written in terms of dimension stride\n\
# and specified outer-most dimension first.\n\
# \n\
# multiarray(i,j,k) = data[data_offset + dim_stride[1]*i + dim_stride[2]*j + k]\n\
#\n\
# A standard, 3-channel 640x480 image with interleaved color channels\n\
# would be specified as:\n\
#\n\
# dim[0].label  = \"height\"\n\
# dim[0].size   = 480\n\
# dim[0].stride = 3*640*480 = 921600  (note dim[0] stride is just size of image)\n\
# dim[1].label  = \"width\"\n\
# dim[1].size   = 640\n\
# dim[1].stride = 3*640 = 1920\n\
# dim[2].label  = \"channel\"\n\
# dim[2].size   = 3\n\
# dim[2].stride = 3\n\
#\n\
# multiarray(i,j,k) refers to the ith row, jth column, and kth channel.\n\
\n\
================================================================================\n\
MSG: std_msgs/MultiArrayDimension\n\
string label   # label of given dimension\n\
uint32 size    # size of given dimension (in type units)\n\
uint32 stride  # stride of given dimension\n\
";

    yarp::os::Type getType() const override
    {
        yarp::os::Type typ = yarp::os::Type::byName(typeName, typeName);
        typ.addProperty("md5sum", yarp::os::Value(typeChecksum));
        typ.addProperty("message_definition", yarp::os::Value(typeText));
        return typ;
    }
};

} // namespace std_msgs
} // namespace rosmsg
} // namespace yarp

#endif // YARP_ROSMSG_std_msgs_Int16MultiArray_h
