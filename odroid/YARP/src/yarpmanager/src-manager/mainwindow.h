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

#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QFileSystemWatcher>
#include <yarp/os/Network.h>
#include <yarp/os/Property.h>
#include <string>
#include <yarp/manager/manager.h>
#include "entitiestreewidget.h"
#include "genericviewwidget.h"
#include "newapplicationwizard.h"
#include "clusterWidget.h"
//#include "message_list.h"
//#include "application_list.h"

namespace Ui {
class MainWindow;
}

/*! \class MainWindow
    \brief MainWindow class.
*/
class MainWindow : public QMainWindow
{
    Q_OBJECT
    friend class NewApplicationWizard;

public:
    explicit MainWindow(QWidget *parent = 0);
    void init(yarp::os::Property config);
    ~MainWindow();

    void reportErrors();

private:
    void syncApplicationList(QString selectNodeForEditing = "", bool open = false);
    bool loadRecursiveTemplates(const char* szPath);
    bool loadRecursiveApplications(const char* szPath);
    bool initializeFile(std::string _class);
    int  getAppTabIndex(QString appName);
    QString getAppNameFromXml(QString fileName);

private:
    Ui::MainWindow *ui;
    yarp::manager::Manager lazyManager;
    yarp::os::Property config;
    QString fileName;
    QString currentAppName;
    QString currentAppDescription;
    QString currentAppVersion;
    QStringList listOfAppFiles;

    QFileSystemWatcher* watcher;

    EntitiesTreeWidget *entitiesTree;
    QToolBar *builderToolBar;
    GenericViewWidget *prevWidget;

    std::string ext_editor;

protected:
    void closeEvent(QCloseEvent *) override;


private slots:
    void onSave();
    void onSaveAs();
    void onOpen();
    void onClose();
    void onImportFiles();
    void onNewModule();
    void onNewResource();
    void onNewApplication();
    void onExportGraph();
    void onRun(bool onlySelected=false);
    void onStop(bool onlySelected=false);
    void onKill(bool onlySelected=false);
    void onConnect(bool onlySelected=false);
    void onDisconnect(bool onlySelected=false);
    void onRunSelected();
    void onStopSelected();
    void onKillSelected();
    void onConnectSelected();
    void onDisconnectSelected();
    void onRefresh();
    void onSelectAll();
    bool onTabClose(int);
    void onLogError(QString);
    void onLogWarning(QString);
    void onLogMessage(QString);
    void onHelp();
    void onAbout();
    void onBuilderWindowFloating(bool);
    void onWizardError(QString);
    void onViewBuilderWindows();

    void onModified(bool);
    void onFileChanged(const QString & path);
    void onYarpClean();
    void onYarpNameList();

public slots:
    void onTabChangeItem(int);
    void viewModule(yarp::manager::Module*);
    void viewResource(yarp::manager::Computer *res);
    void viewApplication(yarp::manager::Application *app, bool editingMode);

    void onRemoveApplication(QString , QString);
    void onRemoveModule(QString);
    void onRemoveResource(QString);
    void onReopenApplication(QString,QString);
    void onReopenModule(QString,QString);
    void onReopenResource(QString,QString);
    void onApplicationSelectionChanged();

signals:
    void selectItem(QString, bool);
};

#endif // MAINWINDOW_H
