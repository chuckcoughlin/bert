/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_DEV_IMPLEMENT_VIRTUAL_ANALOG_SENSOR_H
#define YARP_DEV_IMPLEMENT_VIRTUAL_ANALOG_SENSOR_H

#include <yarp/dev/IVirtualAnalogSensor.h>
#include <yarp/dev/api.h>
#include <yarp/conf/system.h>

namespace yarp {
    namespace dev {
        class ImplementVirtualAnalogSensor;
    }
}

class YARP_dev_API yarp::dev::ImplementVirtualAnalogSensor: public IVirtualAnalogSensor
{
    void *helper;
    yarp::dev::IVirtualAnalogSensorRaw *iVASRaw;

public:
    bool initialize(int k, const int *amap, const double* userToRaw);
    bool uninitialize();
    ImplementVirtualAnalogSensor(IVirtualAnalogSensorRaw *virt);
    ~ImplementVirtualAnalogSensor();

    VAS_status getVirtualAnalogSensorStatus(int ch) override;
    int getVirtualAnalogSensorChannels() override;
    bool updateVirtualAnalogSensorMeasure(yarp::sig::Vector &measure) override;
    bool updateVirtualAnalogSensorMeasure(int ch, double &measure) override;
};

#endif // YARP_DEV_IMPLEMENT_VIRTUAL_ANALOG_SENSOR_H
