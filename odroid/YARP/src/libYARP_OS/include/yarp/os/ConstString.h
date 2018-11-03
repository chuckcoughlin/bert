/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_CONSTSTRING_H
#define YARP_OS_CONSTSTRING_H

#include <yarp/os/api.h>

#include <string>

namespace yarp {
namespace os {

YARP_DEPRECATED_TYPEDEF_MSG("Use std::string instead")
std::string ConstString;

} // namespace os
} // namespace yarp

#endif // YARP_OS_CONSTSTRING_H
