/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
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

// ImagePort.cpp: implementation of the ImagePort class.
//
//////////////////////////////////////////////////////////////////////

#include "ImagePort.h"
#include <yarp/sig/ImageFile.h>

using namespace yarp::os;

#include <sstream>

InputCallback::InputCallback()
{
    sigHandler = nullptr;
    counter = 0;
}

InputCallback::~InputCallback()
{}

/*! \brief the function callback
    \param img the image received
*/
#ifdef YARP_LITTLE_ENDIAN
void InputCallback::onRead(yarp::sig::ImageOf<yarp::sig::PixelBgra> &img)
#else
void InputCallback::onRead(yarp::sig::ImageOf<yarp::sig::PixelRgba> &img)
#endif
{

    uchar *tmpBuf;
    QSize s = (QSize(img.width(),img.height()));
#if QT_VERSION >= 0x050302
    int imgSize = img.getRawImageSize();
#else
    int imgSize = s.width() * s.height() * img.getPixelSize();
#endif

    // Allocate a QVideoFrame
    QVideoFrame frame(imgSize,
              s,
#if QT_VERSION >= 0x050302
              img.getRowSize(),
#else
              s.width() * img.getPixelSize(),
#endif
              QVideoFrame::Format_RGB32);

    // Maps the buffer
    frame.map(QAbstractVideoBuffer::WriteOnly);
    // Takes the ownership of the buffer in write only mode
    tmpBuf = frame.bits();
    unsigned char *rawImg = img.getRawImage();
    //int j = 0;
    // Inverts the planes because Qt Wants an image in RGB format instead of BGR
   /* for(int i=0; i<imgSize; i++){
        tmpBuf[j+2] = rawImg[i];
        i++;
        tmpBuf[j+1] = rawImg[i];
        i++;
        tmpBuf[j] = rawImg[i];
        tmpBuf[j+3] = 0;
        j+=4;
    }*/

#if QT_VERSION >= 0x050302
    memcpy(tmpBuf,rawImg,imgSize);
#else
    for(int x =0; x < s.height(); x++) {
        memcpy(tmpBuf + x * (img.width() * img.getPixelSize()),
               rawImg + x * (img.getRowSize()),
               img.width() * img.getPixelSize());
    }
#endif

    //unmap the buffer
    frame.unmap();
    if(sigHandler){
        sigHandler->sendVideoFrame(frame);
    }

}

/*! \brief sets the signalhandler to the class
    \param handler the signal handler
*/
void InputCallback::setSignalHandler(SignalHandler *handler)
{
    sigHandler = handler;
}

