/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_DEV_IMPLEMENTCONTROLCALIBRATION_H
#define YARP_DEV_IMPLEMENTCONTROLCALIBRATION_H

#include <yarp/dev/ControlBoardInterfaces.h>
#include <yarp/dev/IControlCalibration.h>
#include <yarp/dev/ControlBoardHelper.h>

#include <yarp/dev/api.h>
namespace yarp{
    namespace dev {
        class ImplementControlCalibration;
    }
}

class YARP_dev_API yarp::dev::ImplementControlCalibration : public IControlCalibration
{
protected:
    IControlCalibrationRaw *iCalibrate;
    void *helper;
    double *temp;

    /**
     * Initialize the internal data and alloc memory.
     * @param size is the number of controlled axes the driver deals with.
     * @param amap is a lookup table mapping axes onto physical drivers.
     * @param enc is an array containing the encoder to angles conversion factors.
     * @param zos is an array containing the zeros of the encoders.
     * @return true if initialized succeeded, false if it wasn't executed, or assert.
     */
    bool initialize (int size, const int *amap, const double *enc, const double *zos);

    /**
     * Clean up internal data and memory.
     * @return true if uninitialization is executed, false otherwise.
     */
    bool uninitialize ();

public:
    /* Constructor.
     * @param y is the pointer to the class instance inheriting from this
     *  implementation.
     */
    ImplementControlCalibration(yarp::dev::IControlCalibrationRaw *y);

    /**
     * Destructor. Perform uninitialize if needed.
     */
    virtual ~ImplementControlCalibration();

    virtual bool calibrateAxisWithParams(int axis, unsigned int type, double p1, double p2, double p3) override;

    virtual bool setCalibrationParameters(int axis, const CalibrationParameters& params) override;

    virtual bool calibrationDone(int j) override;
};


#endif // YARP_DEV_IMPLEMENTCONTROLCALIBRATION_H
