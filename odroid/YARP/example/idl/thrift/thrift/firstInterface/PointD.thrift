/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

namespace yarp yarp.test

struct PointD {
  1: i32 x;
  2: i32 y;
  3: i32 z;
}

struct PointDLists{
  1: string name ="pointLists";
  2: list<PointD> firstList;
  3: list<PointD> secondList; 
}

enum PointQuality{
  UNKNOWN = 0,
  GOOD = 1,
  BAD = 2
}

struct PointWithQuality{
  1: PointD point;
  2: PointQuality quality= PointQuality.UNKNOWN;
}

struct Vector {
  1: list<double> content;
} (
  yarp.name = "yarp::sig::Vector"
  yarp.includefile="yarp/sig/Vector.h"
)

