/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
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

#ifndef YCONSOLEMANAGER
#define YCONSOLEMANAGER


#include <yarp/manager/manager.h>
#include <yarp/manager/utility.h>
#include <yarp/os/Network.h>
#include <yarp/os/Property.h>
#include <string>

//namespace ymm {

typedef enum _ColorTheme {
    THEME_DARK,
    THEME_LIGHT,
    THEME_NONE
} ColorTheme;


/**
 * Class YConsoleManager
 */
class YConsoleManager : public yarp::manager::Manager {

public:
    YConsoleManager(int argc, char* argv[]);
    ~YConsoleManager();


protected:
    virtual void onExecutableStart(void* which) override;
    virtual void onExecutableStop(void* which) override;
    virtual void onExecutableDied(void* which) override;
    virtual void onExecutableFailed(void* which) override;
    virtual void onCnnStablished(void* which) override;
    virtual void onCnnFailed(void* which) override;

private:
    bool bShouldRun;
    yarp::os::Property cmdline;
    yarp::os::Property config;

    void help(void);
    void myMain(void);
    bool process(const std::vector<std::string> &cmdList);
    bool exit(void);
    void reportErrors(void);
    void which(void);
    void checkStates(void);
    void checkConnections(void);
    bool loadRecursiveApplications(const char* szPath);
    void updateAppNames(std::vector<std::string>* apps);
    void setColorTheme(ColorTheme theme);
    static void onSignal(int signum);
};


#endif //YCONSOLEMANAGER
