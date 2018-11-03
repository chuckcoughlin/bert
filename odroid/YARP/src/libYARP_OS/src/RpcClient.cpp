/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#include <yarp/os/RpcClient.h>
#include <yarp/os/impl/Logger.h>

using namespace yarp::os;
using namespace yarp::os::impl;


class RpcClient::Private
{
public:
    // an RpcClient may be implemented with a regular port
    Port port;
};


RpcClient::RpcClient() :
        mPriv(new Private)
{
    mPriv->port.setInputMode(false);
    mPriv->port.setOutputMode(true);
    mPriv->port.setRpcMode(true);
}

RpcClient::~RpcClient()
{
    mPriv->port.close();
    delete mPriv;
}

bool RpcClient::read(PortReader& reader, bool willReply)
{
    YARP_UNUSED(reader);
    YARP_UNUSED(willReply);
    YARP_SPRINTF1(Logger::get(),
                  error,
                  "cannot read from RpcClient %s, please use a regular Port for that",
                  mPriv->port.getName().c_str());
    return false;
}

bool RpcClient::reply(PortWriter& writer)
{
    YARP_UNUSED(writer);
    return false;
}

bool RpcClient::replyAndDrop(PortWriter& writer)
{
    YARP_UNUSED(writer);
    return false;
}

void RpcClient::setInputMode(bool expectInput)
{
    yAssert(!expectInput);
}

void RpcClient::setOutputMode(bool expectOutput)
{
    yAssert(expectOutput);
}

void RpcClient::setRpcMode(bool expectRpc)
{
    yAssert(expectRpc);
}

Port& RpcClient::asPort()
{
    return mPriv->port;
}

const Port& RpcClient::asPort() const
{
    return mPriv->port;
}
