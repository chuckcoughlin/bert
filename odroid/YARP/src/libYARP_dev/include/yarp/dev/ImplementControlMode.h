/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_DEV_IMPLEMENTCONTROLMODE_H
#define YARP_DEV_IMPLEMENTCONTROLMODE_H

#include <yarp/dev/IControlMode.h>
#include <yarp/dev/api.h>

namespace yarp {
    namespace dev {
    class ImplementControlMode;
    }
}

class YARP_dev_API yarp::dev::ImplementControlMode: public IControlMode
{
    void *helper;
    int nj;
    yarp::dev::IControlModeRaw *raw;
public:
    bool initialize(int k, const int *amap);
    bool uninitialize();
    ImplementControlMode(IControlModeRaw *v);
    ~ImplementControlMode();
    bool getControlMode(int j, int *f) override;
    bool getControlModes(int *modes) override;
    bool getControlModes(const int n_joint, const int *joints, int *modes) override;
    bool setControlMode(const int j, const int mode) override;
    bool setControlModes(const int n_joint, const int *joints, int *modes) override;
    bool setControlModes(int *modes) override;
};

#endif // YARP_DEV_IMPLEMENTCONTROLMODE_H
