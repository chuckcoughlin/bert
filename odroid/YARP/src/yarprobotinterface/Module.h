/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

#ifndef YARP_YARPROBOTINTERFACE_MODULE_H
#define YARP_YARPROBOTINTERFACE_MODULE_H

#include <yarp/os/RFModule.h>
#include <yarprobotinterfaceRpc.h>

namespace RobotInterface
{

class Module : public yarp::os::RFModule,
               public yarprobotinterfaceRpc
{
public:
    explicit Module();
    virtual ~Module();

    // yarp::os::RFModule
    virtual double getPeriod() override;
    virtual bool updateModule() override;
    virtual bool close() override;
    virtual bool interruptModule() override;
    virtual bool configure(yarp::os::ResourceFinder &rf) override;

    using yarp::os::RFModule::attach;
    virtual bool attach(yarp::os::RpcServer &source) override;

    // yarprobotinterfaceRpc
    virtual std::string get_phase() override;
    virtual int32_t get_level() override;
    virtual bool is_ready() override;
    virtual std::string get_robot() override;

    virtual std::string quit() override;
    inline virtual std::string bye() override { return quit(); }
    inline virtual std::string exit() override { return quit(); }

private:
    class Private;
    Private * const mPriv;
}; // class Module

} // namespace RobotInterface

#endif // YARP_YARPROBOTINTERFACE_MODULE_H
