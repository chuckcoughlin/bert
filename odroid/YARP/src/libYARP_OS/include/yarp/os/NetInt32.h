/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_NETINT32_H
#define YARP_OS_NETINT32_H

#include <yarp/conf/numeric.h>

#include <yarp/os/api.h>

////////////////////////////////////////////////////////////////////////
//
// The goal of this file is just to define a 32 bit signed little-endian
// integer type.
//
////////////////////////////////////////////////////////////////////////

namespace yarp {
namespace os {

/**
 * Definition of the NetInt32 type
 */

#ifdef YARP_LITTLE_ENDIAN

typedef std::int32_t NetInt32;

#else // YARP_LITTLE_ENDIAN

class YARP_OS_API NetInt32
{
private:
    std::uint32_t raw_value;
    std::uint32_t swap(std::uint32_t x) const;
    std::int32_t get() const;
    void set(std::int32_t v);

public:
    NetInt32();
    NetInt32(std::int32_t val);
    operator std::int32_t() const;
    std::int32_t operator+(std::int32_t v) const;
    std::int32_t operator-(std::int32_t v) const;
    std::int32_t operator*(std::int32_t v) const;
    std::int32_t operator/(std::int32_t v) const;
    void operator+=(std::int32_t v);
    void operator-=(std::int32_t v);
    void operator*=(std::int32_t v);
    void operator/=(std::int32_t v);
    void operator++(int);
    void operator--(int);
};

#endif // YARP_LITTLE_ENDIAN

} // namespace os
} // namespace yarp

#endif // YARP_OS_NETINT32_H
