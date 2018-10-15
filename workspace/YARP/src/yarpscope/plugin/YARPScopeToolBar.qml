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
import QtQuick.Controls 1.1

Rectangle {

    signal playPressed(bool pressed);
    signal clear()
    signal rescale()
    signal changeInterval(int interval)

    Connections{
        target: spinBox1
        onEditingFinished:{
            changeInterval(spinBox1.value)
        }
    }

    function refreshInterval(interval){
        spinBox1.value = interval
    }


    function setPlayState(){
        playPauseBtn.checked = true
    }


    height: 70
    gradient: Gradient {
        GradientStop {
            position: 0
            color: "#ffffff"
        }

        GradientStop {
            position: 1
            color: "#000000"
        }
    }

    SpinBox {
        id: spinBox1
        x: 14
        y: 19
        width: 65
        height: 24
        minimumValue: 5
        maximumValue: 1000

    }

    Label {
        id: label1
        x: 14
        y: 49
        width: 65
        height: 13
        text: "Interval"
        horizontalAlignment: Text.AlignHCenter
    }

    ToolButton {
        id: playPauseBtn
        x: 98
        y: 8
        width: 68
        height: 35
        checkable: true
        checked: true
        iconSource: playPauseBtn.checked == true ? "qrc:/YARPScope/images/pause.png" : "qrc:/YARPScope/images/play.png"
        onClicked: {
            playPressed(playPauseBtn.checked)
        }
    }

    ToolButton {
        id: clearBtn
        x: 187
        y: 8
        width: 68
        height: 35
        iconSource: "qrc:/YARPScope/images/action-clear.png"
        onClicked: {
            clear()
        }
    }

    ToolButton {
        id: rescaleBtn
        x: 276
        y: 8
        width: 68
        height: 35
        iconSource: "qrc:/YARPScope/images/action-rescale.png"
        onClicked: {
            rescale()
        }
    }

    Label {
        id: label2
        x: 98
        y: 49
        width: 68
        height: 13
        text: playPauseBtn.checked == true ? "Stop" : "Play"
        horizontalAlignment: Text.AlignHCenter
    }

    Label {
        id: label3
        x: 188
        y: 49
        width: 68
        height: 13
        text: "Clear"
        horizontalAlignment: Text.AlignHCenter
    }

    Label {
        id: label4
        x: 276
        y: 49
        width: 68
        height: 13
        text: "Auto Rescale"
        horizontalAlignment: Text.AlignHCenter
    }
}
