/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_NETINT16_H
#define YARP_OS_NETINT16_H

#include <yarp/conf/numeric.h>

#include <yarp/os/api.h>

////////////////////////////////////////////////////////////////////////
//
// The goal of this file is just to define a 16 bit signed little-endian
// integer type.
//
////////////////////////////////////////////////////////////////////////

namespace yarp {
namespace os {

/**
 * Definition of the NetInt16 type
 */

#ifdef YARP_LITTLE_ENDIAN

typedef std::int16_t NetInt16;

#else // YARP_LITTLE_ENDIAN

class YARP_OS_API NetInt16
{
private:
    std::uint16_t raw_value;
    std::uint16_t swap(std::uint16_t x) const;
    std::int16_t get() const;
    void set(std::int16_t v);

public:
    NetInt16();
    NetInt16(std::int16_t val);
    operator std::int16_t() const;
    std::int16_t operator+(std::int16_t v) const;
    std::int16_t operator-(std::int16_t v) const;
    std::int16_t operator*(std::int16_t v) const;
    std::int16_t operator/(std::int16_t v) const;
    void operator+=(std::int16_t v);
    void operator-=(std::int16_t v);
    void operator*=(std::int16_t v);
    void operator/=(std::int16_t v);
    void operator++(int);
    void operator--(int);
};

#endif // YARP_LITTLE_ENDIAN

} // namespace os
} // namespace yarp

#endif // YARP_OS_NETINT16_H
