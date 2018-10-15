/*
 * Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import QtQuick 2.0
import QtQuick.Window 2.1
import QtQuick.Controls 1.1


import robotology.yarp.scope 1.0
import robotology.yarp.view 1.0
import "qrc:/YARPScope/"
import "qrc:/YARPView/"

ApplicationWindow {
    id: window
    width: 360
    height: 360

    /*************************************************/
    menuBar: YARPScopeMenu{
        id: menu
    }

    toolBar: YARPScopeToolBar{
        id: toolBar
    }

    statusBar: YARPViewStatusBar{
        id: statusBar
    }

    Rectangle{
        id: mainContainer
        anchors.fill: parent

        VideoSurface{
            id: vSurface
            objectName: "YARPVideoSurface"
            x: 0
            y: 0
            width: mainContainer.width/2
            height: mainContainer.height
            dataArea: statusBar
            //menuHeight: menuH
        }

        QtYARPScopePlugin{
            x: mainContainer.width/2
            y: 0
            width: mainContainer.width/2
            height: mainContainer.height

            id: graph
            objectName: "YARPScope1"
        }


    }


    /*************************************************/

    function parseParameters(params){
        var ret = graph.parseParameters(params)

        vSurface.parseParameters(params)
        return ret
    }


    /**************************************************/

    Connections{
        target: vSurface
        onSetName:{
            statusBar.setName(name)
        }
    }

    Connections{
        target: toolBar
        onPlayPressed:{
            graph.playPressed(pressed)
        }
        onClear:{
            graph.clear()
        }
        onRescale:{
            graph.rescale()
        }
        onChangeInterval:{
            graph.changeInterval(interval)
        }
    }

    Connections{
        target: menu
        onPlayPressed:{
            graph.playPressed(pressed)
        }
        onClear:{
            graph.clear()
        }
        onRescale:{
            graph.rescale()
        }

        onAbout:{
            aboutDlg.visibility = Window.Windowed
        }
    }


    Connections{
        target: graph
        onIntervalLoaded:{
            toolBar.refreshInterval(interval)
        }
        onSetWindowPosition:{
            window.x = x
            window.y = y
        }
        onSetWindowSize:{
            window.width = w
            window.height = h
        }
    }

}
