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

#include <fstream>
#include <QFileDialog>
#include <QMessageBox>

#include <yarp/os/Bottle.h>
#include <yarp/os/LogStream.h>

#include "batchqosconfdialog.h"
#include "ui_batchqosconfdialog.h"

#include "qosconfigdialog.h"

using namespace std;
using namespace yarp::os;
using namespace yarp::profiler;

BatchQosConfDialog::BatchQosConfDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::BatchQosConfDialog)
{
    ui->setupUi(this);
    ui->treeWidgetCons->header()->resizeSection(0, 120);
    ui->treeWidgetCons->header()->resizeSection(3, 120);
    ui->treeWidgetCons->header()->resizeSection(4, 120);
    ui->treeWidgetCons->header()->resizeSection(5, 150);

    connect(ui->pushButtonBatchQosOpen, SIGNAL(clicked()), this, SLOT(openCons()));
    connect(ui->pushButtonBatchQosUpdate, SIGNAL(clicked()), this, SLOT(updateQos()));
    connect(ui->pushButtonBatchQosConfigure, SIGNAL(clicked()), this, SLOT(configureQos()));
    ui->pushButtonBatchQosUpdate->setEnabled(false);
    ui->pushButtonBatchQosConfigure->setEnabled(false);

}

BatchQosConfDialog::~BatchQosConfDialog()
{
    delete ui;
}


void BatchQosConfDialog::openCons()
{
    QString filters("Text files (*.txt);;All files (*.*)");
    QString defaultFilter("Connections list file (*.txt)");
    QString filename = QFileDialog::getOpenFileName(nullptr, "Load connections list",
                                                    QDir::homePath(),
                                                    filters, &defaultFilter);
    if(filename.size() == 0)
        return;

    fstream file;
    file.open(filename.toStdString().c_str());
    if (!file.is_open()) {
        QMessageBox::critical(nullptr, QObject::tr("Error"), QObject::tr("Cannot open the file for loading"));
        return;
    }

   ui->treeWidgetCons->clear();
    string line;
    unsigned int count = 0;
    while(getline(file, line)) {
        count++;
        Bottle sample(line.c_str());
        if(sample.size() == 3) {
            //data.addList() = sample;
            //yInfo()<<sample.toString();
            QTreeWidgetItem* item;
            QStringList prop;
            prop.clear();
            prop.append("Unknown");
            prop.append(sample.get(0).asString().c_str());
            prop.append(sample.get(1).asString().c_str());
            prop.append(sample.get(2).asString().c_str());
            item = new QTreeWidgetItem( ui->treeWidgetCons, prop);
            YARP_UNUSED(item);
        }
        else
            yWarning()<<"Wrong connection data at line"<<count;
    }
    file.close();

    if(ui->treeWidgetCons->topLevelItemCount() > 0) {	
        ui->pushButtonBatchQosUpdate->setEnabled(true);
        ui->pushButtonBatchQosConfigure->setEnabled(true);
        updateQos();
    }
    else {
        QMessageBox messageBox;
        messageBox.critical(nullptr,"Error","Could not load any connections! \n Please check the file format.");
        messageBox.setFixedSize(500,200);
    }

}

void BatchQosConfDialog::updateQos()
{
    for( int i=0; i < ui->treeWidgetCons->topLevelItemCount(); ++i ){
        QTreeWidgetItem *item = ui->treeWidgetCons->topLevelItem(i);
        yarp::os::QosStyle fromStyle, toStyle;
        if(yarp::os::NetworkBase::getConnectionQos(item->text(1).toUtf8().constData(),
                                                   item->text(2).toUtf8().constData(), fromStyle, toStyle)) {
            QString qosStr = NetworkProfiler::packetPrioToString(fromStyle.getPacketPriorityAsLevel()).c_str();
            QBrush b;
            switch(fromStyle.getPacketPriorityAsLevel()) {
                case yarp::os::QosStyle::PacketPriorityNormal : {
                    b.setColor(Qt::black);
                    break;
                }
                case yarp::os::QosStyle::PacketPriorityLow : {
                    b.setColor(QColor(255,215,0));
                    break;
                }
                case yarp::os::QosStyle::PacketPriorityHigh : {
                    b.setColor(QColor(255,140,0));
                    break;
                }
                case yarp::os::QosStyle::PacketPriorityCritical : {
                    b.setColor(Qt::red);
                    break;
                }
                case yarp::os::QosStyle::PacketPriorityInvalid : {
                    b.setColor(Qt::black);
                    break;
                }
                default: {
                    b.setColor(Qt::black);
                }
            }
            item->setForeground( 0 , b);
            item->setText(0, qosStr);
            qosStr += ", " + QString::number(fromStyle.getThreadPolicy()) + ", " + QString::number(fromStyle.getThreadPriority());
            item->setText(4, qosStr);
            qosStr.clear();
            qosStr = NetworkProfiler::packetPrioToString(toStyle.getPacketPriorityAsLevel()).c_str();
            qosStr += ", " + QString::number(toStyle.getThreadPolicy()) + ", " + QString::number(toStyle.getThreadPriority());
            item->setText(5, qosStr);
        }
        else
            yWarning()<<"Cannot retrieve Qos property of"<<item->text(0).toUtf8().constData()<<"->"<<item->text(0).toUtf8().constData();
    }
    ui->treeWidgetCons->update();
}

void BatchQosConfDialog::configureQos() {
    yarp::os::QosStyle fromStyle, toStyle;
    QosConfigDialog dialog(nullptr);
    dialog.setModal(true);
    if(dialog.exec() != QDialog::Accepted )
        return;
    dialog.getUserQosStyles(fromStyle, toStyle);
    for( int i=0; i < ui->treeWidgetCons->topLevelItemCount(); ++i ){
        QTreeWidgetItem *item = ui->treeWidgetCons->topLevelItem(i);
        std::string from  = item->text(1).toUtf8().constData();
        std::string to  = item->text(2).toUtf8().constData();
        if(!yarp::os::Network::setConnectionQos(from, to, fromStyle, toStyle)) {
            yWarning()<<"Cannot set Qos property of connection"<<from<<"->"<<to;
        }
    }
    updateQos();
}
