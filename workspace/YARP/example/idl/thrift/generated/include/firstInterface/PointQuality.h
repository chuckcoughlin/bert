/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

// This is an automatically generated file.
// It could get re-generated if the ALLOW_IDL_GENERATION flag is on.

#ifndef YARP_THRIFT_GENERATOR_ENUM_PointQuality
#define YARP_THRIFT_GENERATOR_ENUM_PointQuality

#include <yarp/os/Wire.h>
#include <yarp/os/idl/WireTypes.h>

namespace yarp {
  namespace test {
    enum PointQuality {
      UNKNOWN = 0,
      GOOD = 1,
      BAD = 2
    };

    class PointQualityVocab;
  }
}

class yarp::test::PointQualityVocab : public yarp::os::idl::WireVocab {
public:
  virtual int fromString(const std::string& input) override;
  virtual std::string toString(int input) const override;
};


#endif
