/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_IDL_UNWRAPPED_H
#define YARP_OS_IDL_UNWRAPPED_H

#include <yarp/os/idl/WirePortable.h>
#include <yarp/os/idl/WireReader.h>
#include <yarp/os/idl/WireWriter.h>

namespace yarp {
namespace os {
namespace idl {

template <class T>
class Unwrapped : public yarp::os::Portable
{
public:
    T content;

    virtual bool read(yarp::os::ConnectionReader& reader) override
    {
        WireReader wreader(reader);
        return content.read(wreader);
    }

    virtual bool write(yarp::os::ConnectionWriter& writer) const override
    {
        WireWriter wwriter(writer);
        return content.write(wwriter);
    }
};

template <class T>
class UnwrappedView : public yarp::os::Portable
{
public:
    T& content;

    UnwrappedView(T& content) : content(content) {}

    virtual bool read(yarp::os::ConnectionReader& reader) override
    {
        WireReader wreader(reader);
        return content.read(wreader);
    }

    virtual bool write(yarp::os::ConnectionWriter& writer) const override
    {
        WireWriter wwriter(writer);
        return content.write(wwriter);
    }
};

} // namespace idl
} // namespace os
} // namespace yarp

#endif // YARP_OS_IDL_UNWRAPPED_H
