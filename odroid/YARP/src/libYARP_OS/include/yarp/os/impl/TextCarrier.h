/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_IMPL_TEXTCARRIER_H
#define YARP_OS_IMPL_TEXTCARRIER_H

#include <yarp/os/impl/TcpCarrier.h>

namespace yarp {
namespace os {
namespace impl {

/**
 * Communicating between two ports via a plain-text protocol.
 */
class TextCarrier : public TcpCarrier
{

public:
    TextCarrier(bool ackVariant = false);

    virtual Carrier* create() const override;

    virtual std::string getName() const override;

    virtual std::string getSpecifierName() const;

    virtual bool checkHeader(const Bytes& header) override;
    virtual void getHeader(Bytes& header) const override;
    virtual bool requireAck() const override;
    virtual bool isTextMode() const override;
    virtual bool supportReply() const override;
    virtual bool sendHeader(ConnectionState& proto) override;
    virtual bool expectReplyToHeader(ConnectionState& proto) override;
    virtual bool expectSenderSpecifier(ConnectionState& proto) override;
    virtual bool sendIndex(ConnectionState& proto, SizedWriter& writer) override;
    virtual bool expectIndex(ConnectionState& proto) override;
    virtual bool sendAck(ConnectionState& proto) override;
    virtual bool expectAck(ConnectionState& proto) override;
    virtual bool respondToHeader(ConnectionState& proto) override;

private:
    bool ackVariant;
};

} // namespace impl
} // namespace os
} // namespace yarp

#endif // YARP_OS_IMPL_TEXTCARRIER_H
