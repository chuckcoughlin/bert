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

#ifndef SEQUENCEWINDOW_H
#define SEQUENCEWINDOW_H

#include <QMainWindow>
#include <QCloseEvent>
#include <QTreeWidgetItem>
#include <QDropEvent>
#include <QDebug>
#include <QItemDelegate>
#include <yarp/conf/compiler.h>

class SequenceTreeWidget;
class SequenceItem;

namespace Ui {
class SequenceWindow;
}


class SequenceWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit SequenceWindow(QString partName, int count,QWidget *parent = 0);
    ~SequenceWindow();
    bool checkAndGo();
    bool checkAndRun();
    bool checkAndRunTime();
    bool checkAndCycleSeq();
    bool checkAndCycleTimeSeq();
    void save(QString global_filename);
    void loadSequence(QList<SequenceItem>);

protected:
    void closeEvent(QCloseEvent *event) override;

private:
     QList<SequenceItem> getValuesFromList();
     QAction *goAction;
     QAction *runTimeAction;
     QAction *saveAction;
     QAction *openAction;
     QAction *cycleAllAction;
     QAction *cycleTimeAction;
     QAction *stopAction;

     int prevCurrentIndex;


private:
    Ui::SequenceWindow *ui;
    int jointCount;

signals:
    void itemDoubleClicked(int sequenceNum);
    void goToPosition(SequenceItem);
    void runTime(QList <SequenceItem>);
    void run(QList <SequenceItem>);
    void cycle(QList <SequenceItem>);
    void cycleTime(QList <SequenceItem>);
    void saveSequence(QList <SequenceItem>, QString);
    void openSequence();
    void stopSequence();

public slots:
    void onReceiveValues(int sequenceNum, QList<double>, QList<double> speeds);
private slots:
    void onDoubleClickPositions(QTreeWidgetItem*, int);
    void onDoubleClickSpeed(QTreeWidgetItem *item,int column);
    void onGo();
    void onRunTime();
    void onCycleTime();
    void onCycle();
    void onSave();
    void onOpen();
    void onStopSequence();

    void onRunTimeSequence();
    void onCycleTimeSequence();
    void onCycleSequence();
    void onStoppedSequence();
    void onSetCurrentSequenceIndex(int index);
    void onDeletePosition(int index);
    void onMovePositions(int index, int dragIndex);

};



class SequenceTreeWidget : public QTreeWidget
{
    Q_OBJECT

public:
    SequenceTreeWidget(QWidget * parent = 0);

private:
    QStringList copyValues;

protected:
    void dropEvent(QDropEvent *event) override;

signals:
    void deletedItem(int index);
    void moveItem(int itemBefore,int item);



private slots:
    void onContextMenuRequested(QPoint point);


};

class SequenceItem
{


public:
    SequenceItem(){
        sequenceNumber = 0;
        timing = -1;
    }

    void setSequenceNumber(int index){
        this->sequenceNumber = index;
    }

    void setTiming(double timing){
        this->timing = timing;
    }

    void addValue(double pos, double speed){
        positionsList.append(pos);
        speedsList.append(speed);
    }
    void addPositionValue(double pos){
        positionsList.append(pos);
    }
    void addSpeedValue(double speed){
        //qDebug() << "speedsList.append(speed);" << speed;
        speedsList.append(speed);
    }

    int getSequenceNumber(){
        return sequenceNumber;
    }

    double getTiming(){
        return timing;
    }

    QList<double> getPositions(){
        return positionsList;
    }

    QList<double> getSpeeds(){
        return speedsList;
    }

    SequenceItem &operator =(const SequenceItem &other){
        sequenceNumber = other.sequenceNumber;
        timing = other.timing;
        positionsList = other.positionsList;
        speedsList = other.speedsList;
        return *this;
    }

private:

    int sequenceNumber;
    double timing;
    QList<double>positionsList;
    QList<double>speedsList;
};



#endif // SEQUENCEWINDOW_H

