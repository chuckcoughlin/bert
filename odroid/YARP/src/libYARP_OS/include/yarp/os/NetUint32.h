/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_NETUINT32_H
#define YARP_OS_NETUINT32_H

#include <yarp/conf/numeric.h>

#include <yarp/os/api.h>

////////////////////////////////////////////////////////////////////////
//
// The goal of this file is just to define a 32 bit unsigned little-endian
// integer type.
//
////////////////////////////////////////////////////////////////////////

namespace yarp {
namespace os {

/**
 * Definition of the NetUint32 type
 */

#ifdef YARP_LITTLE_ENDIAN

typedef std::uint32_t NetUint32;

#else // YARP_LITTLE_ENDIAN

class YARP_OS_API NetUint32
{
private:
    std::uint32_t raw_value;
    std::uint32_t swap(std::uint32_t x) const;
    std::uint32_t get() const;
    void set(std::uint32_t v);

public:
    NetUint32();
    NetUint32(std::uint32_t val);
    operator std::uint32_t() const;
    std::uint32_t operator+(std::uint32_t v) const;
    std::uint32_t operator-(std::uint32_t v) const;
    std::uint32_t operator*(std::uint32_t v) const;
    std::uint32_t operator/(std::uint32_t v) const;
    void operator+=(std::uint32_t v);
    void operator-=(std::uint32_t v);
    void operator*=(std::uint32_t v);
    void operator/=(std::uint32_t v);
    void operator++(int);
    void operator--(int);
};

#endif // YARP_LITTLE_ENDIAN

} // namespace os
} // namespace yarp

#endif // YARP_OS_NETUINT32_H
