/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#ifndef YARP_OS_ABSTRACTCARRIER_H
#define YARP_OS_ABSTRACTCARRIER_H

#include <yarp/os/Carrier.h>
#include <yarp/os/NetType.h>

namespace yarp {
namespace os {

/**
 * \brief A starter class for implementing simple carriers.
 *
 * It implements reasonable default behavior.
 */
class YARP_OS_API AbstractCarrier : public Carrier
{
public:
    /** @{ */

    // Documented in Carrier
    virtual Carrier* create() const override = 0;

    // Documented in Carrier
    virtual std::string getName() const override = 0;

    // Documented in Carrier
    virtual bool checkHeader(const yarp::os::Bytes& header) override = 0;

    // Documented in Carrier
    virtual void setParameters(const yarp::os::Bytes& header) override;

    // Documented in Carrier
    virtual void getHeader(yarp::os::Bytes& header) const override = 0;

    // Documented in Carrier
    virtual void setCarrierParams(const yarp::os::Property& params) override;
    // Documented in Carrier
    virtual void getCarrierParams(yarp::os::Property& params) const override;

    /** @} */
    /** @{ */

    // Documented in Carrier
    virtual bool isConnectionless() const override;
    // Documented in Carrier
    virtual bool supportReply() const override;
    // Documented in Carrier
    virtual bool canAccept() const override;
    // Documented in Carrier
    virtual bool canOffer() const override;
    // Documented in Carrier
    virtual bool isTextMode() const override;
    // Documented in Carrier
    virtual bool requireAck() const override;
    // Documented in Carrier
    virtual bool canEscape() const override;
    // Documented in Carrier
    virtual bool isLocal() const override;
    // Documented in Carrier
    virtual std::string toString() const override;

    // Documented in Carrier
    virtual bool isActive() const override;

    /** @} */
    /** @{ */
    // Sender methods

    // Documented in Carrier
    virtual bool prepareSend(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool sendHeader(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool expectReplyToHeader(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool sendIndex(ConnectionState& proto, SizedWriter& writer);

    /** @} */
    /** @{ */
    // Receiver methods

    // Documented in Carrier
    virtual bool expectExtraHeader(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool respondToHeader(ConnectionState& proto) override = 0; // left abstract, no good default
    // Documented in Carrier
    virtual bool expectIndex(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool expectSenderSpecifier(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool sendAck(ConnectionState& proto) override;
    // Documented in Carrier
    virtual bool expectAck(ConnectionState& proto) override;

    /** @} */
    /** @{ */
    // some default implementations of protocol phases used by
    // certain YARP carriers

    /**
     * Default implementation for the sendHeader method
     */
    bool defaultSendHeader(ConnectionState& proto);

    /**
     * Default implementation for the expectIndex method
     */
    bool defaultExpectIndex(ConnectionState& proto);

    /**
     * Default implementation for the sendIndex method
     */
    bool defaultSendIndex(ConnectionState& proto, SizedWriter& writer);

    /**
     * Default implementation for the expectAck method
     */
    bool defaultExpectAck(ConnectionState& proto);

    /**
     * Default implementation for the sendAck method
     */
    bool defaultSendAck(ConnectionState& proto);

    /**
     * Read 8 bytes and interpret them as a YARP number
     */
    int readYarpInt(ConnectionState& proto);

    /**
     * Write \c n as an 8 bytes yarp number
     */
    void writeYarpInt(int n, ConnectionState& proto);

    /** @} */

protected:
    /** @{ */

    int getSpecifier(const Bytes& b) const;
    void createStandardHeader(int specifier, yarp::os::Bytes& header) const;

    // Documented in Carrier
    virtual bool write(ConnectionState& proto, SizedWriter& writer) override;

    bool sendConnectionStateSpecifier(ConnectionState& proto);
    bool sendSenderSpecifier(ConnectionState& proto);

    /** @} */
    /** @{ */

    static int interpretYarpNumber(const yarp::os::Bytes& b);
    static void createYarpNumber(int x, yarp::os::Bytes& header);

    /** @} */
};

} // namespace os
} // namespace yarp

#endif // YARP_OS_ABSTRACTCARRIER_H
