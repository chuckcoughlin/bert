/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 * Copyright (C) 2006-2010 RobotCub Consortium
 * All rights reserved.
 *
 * This software may be modified and distributed under the terms of the
 * BSD-3-Clause license. See the accompanying LICENSE file for details.
 */

#include <yarp/os/impl/Terminal.h>
#include <yarp/os/impl/PlatformUnistd.h>
#include <yarp/os/impl/PlatformStdio.h>

#include <yarp/os/Port.h>
#include <yarp/os/Bottle.h>
#include <yarp/os/Vocab.h>

#include <cstdio>
#include <cstring>

#ifdef WITH_LIBEDIT
#include <editline/readline.h>
char* szLine = (char*)nullptr;
bool readlineEOF=false;
#endif // WITH_LIBEDIT

bool yarp::os::impl::Terminal::EOFreached()
{
#ifdef WITH_LIBEDIT
    if (yarp::os::impl::isatty(yarp::os::impl::fileno(stdin))) {
        return readlineEOF;
    }
#endif // WITH_LIBEDIT
    return feof(stdin);
}

std::string yarp::os::impl::Terminal::getStdin() {
    std::string txt = "";

#ifdef WITH_LIBEDIT
    if (yarp::os::impl::isatty(yarp::os::impl::fileno(stdin))) {
        if (szLine) {
            free(szLine);
            szLine = (char*)nullptr;
        }

        szLine = readline(">>");
        if (szLine && *szLine) {
            txt = szLine;
            add_history(szLine);
        } else if (!szLine) {
            readlineEOF=true;
        }
        return txt;
    }
#endif // WITH_LIBEDIT

    bool done = false;
    char buf[2048];
    while (!done) {
        char *result = fgets(buf, sizeof(buf), stdin);
        if (result != nullptr) {
            for (unsigned int i=0; i<strlen(buf); i++) {
                if (buf[i]=='\n') {
                    buf[i] = '\0';
                    done = true;
                    break;
                }
            }
            txt += buf;
        } else {
            done = true;
        }
    }
    return txt;
}

std::string yarp::os::impl::Terminal::readString(bool *eof) {
    bool end = false;

    std::string txt;

    if (!EOFreached()) {
        txt = getStdin();
    }

    if (EOFreached()) {
        end = true;
    } else if (txt.length()>0 && txt[0]<32 && txt[0]!='\n' &&
               txt[0]!='\r') {
        end = true;
    }
    if (end) {
        txt = "";
    }
    if (eof != nullptr) {
        *eof = end;
    }
    return txt;
}
