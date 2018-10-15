/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

// This is an automatically generated file.
// It could get re-generated if the ALLOW_IDL_GENERATION flag is on.

#include <jointData.h>

bool jointData::read_jointPosition(yarp::os::idl::WireReader& reader) {
  if (!reader.read(jointPosition)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_jointPosition(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(jointPosition)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_jointPosition_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(jointPosition_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_jointPosition_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(jointPosition_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_jointVelocity(yarp::os::idl::WireReader& reader) {
  if (!reader.read(jointVelocity)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_jointVelocity(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(jointVelocity)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_jointVelocity_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(jointVelocity_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_jointVelocity_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(jointVelocity_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_jointAcceleration(yarp::os::idl::WireReader& reader) {
  if (!reader.read(jointAcceleration)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_jointAcceleration(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(jointAcceleration)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_jointAcceleration_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(jointAcceleration_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_jointAcceleration_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(jointAcceleration_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_motorPosition(yarp::os::idl::WireReader& reader) {
  if (!reader.read(motorPosition)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_motorPosition(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(motorPosition)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_motorPosition_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(motorPosition_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_motorPosition_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(motorPosition_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_motorVelocity(yarp::os::idl::WireReader& reader) {
  if (!reader.read(motorVelocity)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_motorVelocity(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(motorVelocity)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_motorVelocity_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(motorVelocity_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_motorVelocity_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(motorVelocity_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_motorAcceleration(yarp::os::idl::WireReader& reader) {
  if (!reader.read(motorAcceleration)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_motorAcceleration(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(motorAcceleration)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_motorAcceleration_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(motorAcceleration_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_motorAcceleration_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(motorAcceleration_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_torque(yarp::os::idl::WireReader& reader) {
  if (!reader.read(torque)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_torque(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(torque)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_torque_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(torque_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_torque_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(torque_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_pwmDutycycle(yarp::os::idl::WireReader& reader) {
  if (!reader.read(pwmDutycycle)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_pwmDutycycle(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(pwmDutycycle)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_pwmDutycycle_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(pwmDutycycle_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_pwmDutycycle_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(pwmDutycycle_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_current(yarp::os::idl::WireReader& reader) {
  if (!reader.read(current)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_current(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(current)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_current_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(current_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_current_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(current_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_controlMode(yarp::os::idl::WireReader& reader) {
  if (!reader.read(controlMode)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_controlMode(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(controlMode)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_controlMode_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(controlMode_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_controlMode_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(controlMode_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_interactionMode(yarp::os::idl::WireReader& reader) {
  if (!reader.read(interactionMode)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_interactionMode(yarp::os::idl::WireReader& reader) {
  if (!reader.readNested(interactionMode)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read_interactionMode_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(interactionMode_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::nested_read_interactionMode_isValid(yarp::os::idl::WireReader& reader) {
  if (!reader.readBool(interactionMode_isValid)) {
    reader.fail();
    return false;
  }
  return true;
}
bool jointData::read(yarp::os::idl::WireReader& reader) {
  if (!read_jointPosition(reader)) return false;
  if (!read_jointPosition_isValid(reader)) return false;
  if (!read_jointVelocity(reader)) return false;
  if (!read_jointVelocity_isValid(reader)) return false;
  if (!read_jointAcceleration(reader)) return false;
  if (!read_jointAcceleration_isValid(reader)) return false;
  if (!read_motorPosition(reader)) return false;
  if (!read_motorPosition_isValid(reader)) return false;
  if (!read_motorVelocity(reader)) return false;
  if (!read_motorVelocity_isValid(reader)) return false;
  if (!read_motorAcceleration(reader)) return false;
  if (!read_motorAcceleration_isValid(reader)) return false;
  if (!read_torque(reader)) return false;
  if (!read_torque_isValid(reader)) return false;
  if (!read_pwmDutycycle(reader)) return false;
  if (!read_pwmDutycycle_isValid(reader)) return false;
  if (!read_current(reader)) return false;
  if (!read_current_isValid(reader)) return false;
  if (!read_controlMode(reader)) return false;
  if (!read_controlMode_isValid(reader)) return false;
  if (!read_interactionMode(reader)) return false;
  if (!read_interactionMode_isValid(reader)) return false;
  return !reader.isError();
}

bool jointData::read(yarp::os::ConnectionReader& connection) {
  yarp::os::idl::WireReader reader(connection);
  if (!reader.readListHeader(22)) return false;
  return read(reader);
}

bool jointData::write_jointPosition(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(jointPosition)) return false;
  return true;
}
bool jointData::nested_write_jointPosition(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(jointPosition)) return false;
  return true;
}
bool jointData::write_jointPosition_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(jointPosition_isValid)) return false;
  return true;
}
bool jointData::nested_write_jointPosition_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(jointPosition_isValid)) return false;
  return true;
}
bool jointData::write_jointVelocity(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(jointVelocity)) return false;
  return true;
}
bool jointData::nested_write_jointVelocity(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(jointVelocity)) return false;
  return true;
}
bool jointData::write_jointVelocity_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(jointVelocity_isValid)) return false;
  return true;
}
bool jointData::nested_write_jointVelocity_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(jointVelocity_isValid)) return false;
  return true;
}
bool jointData::write_jointAcceleration(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(jointAcceleration)) return false;
  return true;
}
bool jointData::nested_write_jointAcceleration(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(jointAcceleration)) return false;
  return true;
}
bool jointData::write_jointAcceleration_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(jointAcceleration_isValid)) return false;
  return true;
}
bool jointData::nested_write_jointAcceleration_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(jointAcceleration_isValid)) return false;
  return true;
}
bool jointData::write_motorPosition(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(motorPosition)) return false;
  return true;
}
bool jointData::nested_write_motorPosition(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(motorPosition)) return false;
  return true;
}
bool jointData::write_motorPosition_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(motorPosition_isValid)) return false;
  return true;
}
bool jointData::nested_write_motorPosition_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(motorPosition_isValid)) return false;
  return true;
}
bool jointData::write_motorVelocity(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(motorVelocity)) return false;
  return true;
}
bool jointData::nested_write_motorVelocity(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(motorVelocity)) return false;
  return true;
}
bool jointData::write_motorVelocity_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(motorVelocity_isValid)) return false;
  return true;
}
bool jointData::nested_write_motorVelocity_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(motorVelocity_isValid)) return false;
  return true;
}
bool jointData::write_motorAcceleration(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(motorAcceleration)) return false;
  return true;
}
bool jointData::nested_write_motorAcceleration(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(motorAcceleration)) return false;
  return true;
}
bool jointData::write_motorAcceleration_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(motorAcceleration_isValid)) return false;
  return true;
}
bool jointData::nested_write_motorAcceleration_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(motorAcceleration_isValid)) return false;
  return true;
}
bool jointData::write_torque(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(torque)) return false;
  return true;
}
bool jointData::nested_write_torque(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(torque)) return false;
  return true;
}
bool jointData::write_torque_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(torque_isValid)) return false;
  return true;
}
bool jointData::nested_write_torque_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(torque_isValid)) return false;
  return true;
}
bool jointData::write_pwmDutycycle(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(pwmDutycycle)) return false;
  return true;
}
bool jointData::nested_write_pwmDutycycle(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(pwmDutycycle)) return false;
  return true;
}
bool jointData::write_pwmDutycycle_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(pwmDutycycle_isValid)) return false;
  return true;
}
bool jointData::nested_write_pwmDutycycle_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(pwmDutycycle_isValid)) return false;
  return true;
}
bool jointData::write_current(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(current)) return false;
  return true;
}
bool jointData::nested_write_current(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(current)) return false;
  return true;
}
bool jointData::write_current_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(current_isValid)) return false;
  return true;
}
bool jointData::nested_write_current_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(current_isValid)) return false;
  return true;
}
bool jointData::write_controlMode(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(controlMode)) return false;
  return true;
}
bool jointData::nested_write_controlMode(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(controlMode)) return false;
  return true;
}
bool jointData::write_controlMode_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(controlMode_isValid)) return false;
  return true;
}
bool jointData::nested_write_controlMode_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(controlMode_isValid)) return false;
  return true;
}
bool jointData::write_interactionMode(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.write(interactionMode)) return false;
  return true;
}
bool jointData::nested_write_interactionMode(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeNested(interactionMode)) return false;
  return true;
}
bool jointData::write_interactionMode_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(interactionMode_isValid)) return false;
  return true;
}
bool jointData::nested_write_interactionMode_isValid(const yarp::os::idl::WireWriter& writer) const {
  if (!writer.writeBool(interactionMode_isValid)) return false;
  return true;
}
bool jointData::write(const yarp::os::idl::WireWriter& writer) const {
  if (!write_jointPosition(writer)) return false;
  if (!write_jointPosition_isValid(writer)) return false;
  if (!write_jointVelocity(writer)) return false;
  if (!write_jointVelocity_isValid(writer)) return false;
  if (!write_jointAcceleration(writer)) return false;
  if (!write_jointAcceleration_isValid(writer)) return false;
  if (!write_motorPosition(writer)) return false;
  if (!write_motorPosition_isValid(writer)) return false;
  if (!write_motorVelocity(writer)) return false;
  if (!write_motorVelocity_isValid(writer)) return false;
  if (!write_motorAcceleration(writer)) return false;
  if (!write_motorAcceleration_isValid(writer)) return false;
  if (!write_torque(writer)) return false;
  if (!write_torque_isValid(writer)) return false;
  if (!write_pwmDutycycle(writer)) return false;
  if (!write_pwmDutycycle_isValid(writer)) return false;
  if (!write_current(writer)) return false;
  if (!write_current_isValid(writer)) return false;
  if (!write_controlMode(writer)) return false;
  if (!write_controlMode_isValid(writer)) return false;
  if (!write_interactionMode(writer)) return false;
  if (!write_interactionMode_isValid(writer)) return false;
  return !writer.isError();
}

bool jointData::write(yarp::os::ConnectionWriter& connection) const {
  yarp::os::idl::WireWriter writer(connection);
  if (!writer.writeListHeader(22)) return false;
  return write(writer);
}
bool jointData::Editor::write(yarp::os::ConnectionWriter& connection) const {
  if (!isValid()) return false;
  yarp::os::idl::WireWriter writer(connection);
  if (!writer.writeListHeader(dirty_count+1)) return false;
  if (!writer.writeString("patch")) return false;
  if (is_dirty_jointPosition) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("jointPosition")) return false;
    if (!obj->nested_write_jointPosition(writer)) return false;
  }
  if (is_dirty_jointPosition_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("jointPosition_isValid")) return false;
    if (!obj->nested_write_jointPosition_isValid(writer)) return false;
  }
  if (is_dirty_jointVelocity) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("jointVelocity")) return false;
    if (!obj->nested_write_jointVelocity(writer)) return false;
  }
  if (is_dirty_jointVelocity_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("jointVelocity_isValid")) return false;
    if (!obj->nested_write_jointVelocity_isValid(writer)) return false;
  }
  if (is_dirty_jointAcceleration) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("jointAcceleration")) return false;
    if (!obj->nested_write_jointAcceleration(writer)) return false;
  }
  if (is_dirty_jointAcceleration_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("jointAcceleration_isValid")) return false;
    if (!obj->nested_write_jointAcceleration_isValid(writer)) return false;
  }
  if (is_dirty_motorPosition) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("motorPosition")) return false;
    if (!obj->nested_write_motorPosition(writer)) return false;
  }
  if (is_dirty_motorPosition_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("motorPosition_isValid")) return false;
    if (!obj->nested_write_motorPosition_isValid(writer)) return false;
  }
  if (is_dirty_motorVelocity) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("motorVelocity")) return false;
    if (!obj->nested_write_motorVelocity(writer)) return false;
  }
  if (is_dirty_motorVelocity_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("motorVelocity_isValid")) return false;
    if (!obj->nested_write_motorVelocity_isValid(writer)) return false;
  }
  if (is_dirty_motorAcceleration) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("motorAcceleration")) return false;
    if (!obj->nested_write_motorAcceleration(writer)) return false;
  }
  if (is_dirty_motorAcceleration_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("motorAcceleration_isValid")) return false;
    if (!obj->nested_write_motorAcceleration_isValid(writer)) return false;
  }
  if (is_dirty_torque) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("torque")) return false;
    if (!obj->nested_write_torque(writer)) return false;
  }
  if (is_dirty_torque_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("torque_isValid")) return false;
    if (!obj->nested_write_torque_isValid(writer)) return false;
  }
  if (is_dirty_pwmDutycycle) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("pwmDutycycle")) return false;
    if (!obj->nested_write_pwmDutycycle(writer)) return false;
  }
  if (is_dirty_pwmDutycycle_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("pwmDutycycle_isValid")) return false;
    if (!obj->nested_write_pwmDutycycle_isValid(writer)) return false;
  }
  if (is_dirty_current) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("current")) return false;
    if (!obj->nested_write_current(writer)) return false;
  }
  if (is_dirty_current_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("current_isValid")) return false;
    if (!obj->nested_write_current_isValid(writer)) return false;
  }
  if (is_dirty_controlMode) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("controlMode")) return false;
    if (!obj->nested_write_controlMode(writer)) return false;
  }
  if (is_dirty_controlMode_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("controlMode_isValid")) return false;
    if (!obj->nested_write_controlMode_isValid(writer)) return false;
  }
  if (is_dirty_interactionMode) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("interactionMode")) return false;
    if (!obj->nested_write_interactionMode(writer)) return false;
  }
  if (is_dirty_interactionMode_isValid) {
    if (!writer.writeListHeader(3)) return false;
    if (!writer.writeString("set")) return false;
    if (!writer.writeString("interactionMode_isValid")) return false;
    if (!obj->nested_write_interactionMode_isValid(writer)) return false;
  }
  return !writer.isError();
}
bool jointData::Editor::read(yarp::os::ConnectionReader& connection) {
  if (!isValid()) return false;
  yarp::os::idl::WireReader reader(connection);
  reader.expectAccept();
  if (!reader.readListHeader()) return false;
  int len = reader.getLength();
  if (len==0) {
    yarp::os::idl::WireWriter writer(reader);
    if (writer.isNull()) return true;
    if (!writer.writeListHeader(1)) return false;
    writer.writeString("send: 'help' or 'patch (param1 val1) (param2 val2)'");
    return true;
  }
  std::string tag;
  if (!reader.readString(tag)) return false;
  if (tag=="help") {
    yarp::os::idl::WireWriter writer(reader);
    if (writer.isNull()) return true;
    if (!writer.writeListHeader(2)) return false;
    if (!writer.writeTag("many",1, 0)) return false;
    if (reader.getLength()>0) {
      std::string field;
      if (!reader.readString(field)) return false;
      if (field=="jointPosition") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> jointPosition")) return false;
      }
      if (field=="jointPosition_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool jointPosition_isValid")) return false;
      }
      if (field=="jointVelocity") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> jointVelocity")) return false;
      }
      if (field=="jointVelocity_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool jointVelocity_isValid")) return false;
      }
      if (field=="jointAcceleration") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> jointAcceleration")) return false;
      }
      if (field=="jointAcceleration_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool jointAcceleration_isValid")) return false;
      }
      if (field=="motorPosition") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> motorPosition")) return false;
      }
      if (field=="motorPosition_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool motorPosition_isValid")) return false;
      }
      if (field=="motorVelocity") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> motorVelocity")) return false;
      }
      if (field=="motorVelocity_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool motorVelocity_isValid")) return false;
      }
      if (field=="motorAcceleration") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> motorAcceleration")) return false;
      }
      if (field=="motorAcceleration_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool motorAcceleration_isValid")) return false;
      }
      if (field=="torque") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> torque")) return false;
      }
      if (field=="torque_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool torque_isValid")) return false;
      }
      if (field=="pwmDutycycle") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> pwmDutycycle")) return false;
      }
      if (field=="pwmDutycycle_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool pwmDutycycle_isValid")) return false;
      }
      if (field=="current") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<double> current")) return false;
      }
      if (field=="current_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool current_isValid")) return false;
      }
      if (field=="controlMode") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<int> controlMode")) return false;
      }
      if (field=="controlMode_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool controlMode_isValid")) return false;
      }
      if (field=="interactionMode") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("yarp::sig::VectorOf<int> interactionMode")) return false;
      }
      if (field=="interactionMode_isValid") {
        if (!writer.writeListHeader(1)) return false;
        if (!writer.writeString("bool interactionMode_isValid")) return false;
      }
    }
    if (!writer.writeListHeader(23)) return false;
    writer.writeString("*** Available fields:");
    writer.writeString("jointPosition");
    writer.writeString("jointPosition_isValid");
    writer.writeString("jointVelocity");
    writer.writeString("jointVelocity_isValid");
    writer.writeString("jointAcceleration");
    writer.writeString("jointAcceleration_isValid");
    writer.writeString("motorPosition");
    writer.writeString("motorPosition_isValid");
    writer.writeString("motorVelocity");
    writer.writeString("motorVelocity_isValid");
    writer.writeString("motorAcceleration");
    writer.writeString("motorAcceleration_isValid");
    writer.writeString("torque");
    writer.writeString("torque_isValid");
    writer.writeString("pwmDutycycle");
    writer.writeString("pwmDutycycle_isValid");
    writer.writeString("current");
    writer.writeString("current_isValid");
    writer.writeString("controlMode");
    writer.writeString("controlMode_isValid");
    writer.writeString("interactionMode");
    writer.writeString("interactionMode_isValid");
    return true;
  }
  bool nested = true;
  bool have_act = false;
  if (tag!="patch") {
    if ((len-1)%2 != 0) return false;
    len = 1 + ((len-1)/2);
    nested = false;
    have_act = true;
  }
  for (int i=1; i<len; i++) {
    if (nested && !reader.readListHeader(3)) return false;
    std::string act;
    std::string key;
    if (have_act) {
      act = tag;
    } else {
      if (!reader.readString(act)) return false;
    }
    if (!reader.readString(key)) return false;
    // inefficient code follows, bug paulfitz to improve it
    if (key == "jointPosition") {
      will_set_jointPosition();
      if (!obj->nested_read_jointPosition(reader)) return false;
      did_set_jointPosition();
    } else if (key == "jointPosition_isValid") {
      will_set_jointPosition_isValid();
      if (!obj->nested_read_jointPosition_isValid(reader)) return false;
      did_set_jointPosition_isValid();
    } else if (key == "jointVelocity") {
      will_set_jointVelocity();
      if (!obj->nested_read_jointVelocity(reader)) return false;
      did_set_jointVelocity();
    } else if (key == "jointVelocity_isValid") {
      will_set_jointVelocity_isValid();
      if (!obj->nested_read_jointVelocity_isValid(reader)) return false;
      did_set_jointVelocity_isValid();
    } else if (key == "jointAcceleration") {
      will_set_jointAcceleration();
      if (!obj->nested_read_jointAcceleration(reader)) return false;
      did_set_jointAcceleration();
    } else if (key == "jointAcceleration_isValid") {
      will_set_jointAcceleration_isValid();
      if (!obj->nested_read_jointAcceleration_isValid(reader)) return false;
      did_set_jointAcceleration_isValid();
    } else if (key == "motorPosition") {
      will_set_motorPosition();
      if (!obj->nested_read_motorPosition(reader)) return false;
      did_set_motorPosition();
    } else if (key == "motorPosition_isValid") {
      will_set_motorPosition_isValid();
      if (!obj->nested_read_motorPosition_isValid(reader)) return false;
      did_set_motorPosition_isValid();
    } else if (key == "motorVelocity") {
      will_set_motorVelocity();
      if (!obj->nested_read_motorVelocity(reader)) return false;
      did_set_motorVelocity();
    } else if (key == "motorVelocity_isValid") {
      will_set_motorVelocity_isValid();
      if (!obj->nested_read_motorVelocity_isValid(reader)) return false;
      did_set_motorVelocity_isValid();
    } else if (key == "motorAcceleration") {
      will_set_motorAcceleration();
      if (!obj->nested_read_motorAcceleration(reader)) return false;
      did_set_motorAcceleration();
    } else if (key == "motorAcceleration_isValid") {
      will_set_motorAcceleration_isValid();
      if (!obj->nested_read_motorAcceleration_isValid(reader)) return false;
      did_set_motorAcceleration_isValid();
    } else if (key == "torque") {
      will_set_torque();
      if (!obj->nested_read_torque(reader)) return false;
      did_set_torque();
    } else if (key == "torque_isValid") {
      will_set_torque_isValid();
      if (!obj->nested_read_torque_isValid(reader)) return false;
      did_set_torque_isValid();
    } else if (key == "pwmDutycycle") {
      will_set_pwmDutycycle();
      if (!obj->nested_read_pwmDutycycle(reader)) return false;
      did_set_pwmDutycycle();
    } else if (key == "pwmDutycycle_isValid") {
      will_set_pwmDutycycle_isValid();
      if (!obj->nested_read_pwmDutycycle_isValid(reader)) return false;
      did_set_pwmDutycycle_isValid();
    } else if (key == "current") {
      will_set_current();
      if (!obj->nested_read_current(reader)) return false;
      did_set_current();
    } else if (key == "current_isValid") {
      will_set_current_isValid();
      if (!obj->nested_read_current_isValid(reader)) return false;
      did_set_current_isValid();
    } else if (key == "controlMode") {
      will_set_controlMode();
      if (!obj->nested_read_controlMode(reader)) return false;
      did_set_controlMode();
    } else if (key == "controlMode_isValid") {
      will_set_controlMode_isValid();
      if (!obj->nested_read_controlMode_isValid(reader)) return false;
      did_set_controlMode_isValid();
    } else if (key == "interactionMode") {
      will_set_interactionMode();
      if (!obj->nested_read_interactionMode(reader)) return false;
      did_set_interactionMode();
    } else if (key == "interactionMode_isValid") {
      will_set_interactionMode_isValid();
      if (!obj->nested_read_interactionMode_isValid(reader)) return false;
      did_set_interactionMode_isValid();
    } else {
      // would be useful to have a fallback here
    }
  }
  reader.accept();
  yarp::os::idl::WireWriter writer(reader);
  if (writer.isNull()) return true;
  writer.writeListHeader(1);
  writer.writeVocab(yarp::os::createVocab('o','k'));
  return true;
}

std::string jointData::toString() const {
  yarp::os::Bottle b;
  b.read(*this);
  return b.toString();
}
