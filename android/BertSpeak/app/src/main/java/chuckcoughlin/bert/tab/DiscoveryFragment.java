/**
 * Copyright 2017 Charles Coughlin. All rights reserved.
 * (MIT License)
 */

package chuckcoughlin.bertspeak.tab;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;

import chuckcoughlin.sb.assistant.R;
import chuckcoughlin.sb.assistant.common.SBConstants;
import chuckcoughlin.sb.assistant.db.SBDbManager;
import chuckcoughlin.sb.assistant.dialog.SBRobotViewDialog;
import chuckcoughlin.sb.assistant.dialog.SBWarningDialog;
import chuckcoughlin.sb.assistant.ros.SBRobotManager;
import chuckcoughlin.sb.assistant.ros.SBApplicationManager;
import ros.android.appmanager.BluetoothChecker;
import ros.android.appmanager.MasterChecker;
import ros.android.appmanager.RemoteCommand;
import ros.android.appmanager.SBRemoteCommandListener;
import ros.android.appmanager.SBRobotConnectionHandler;
import ros.android.appmanager.WifiChecker;
import ros.android.util.TabletApplication;
import ros.android.util.RobotDescription;

import static android.content.Context.BLUETOOTH_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static chuckcoughlin.sb.assistant.common.SBConstants.DIALOG_TRANSACTION_KEY;

/**
 * Orchestrate connections to the network, the robot and the desired application.
 * Lifecycle methods are presented here in chronological order. Use the SBRobotManager
 * instance to preserve connection state whenever the fragment is not displayed.
 */
public class DiscoveryFragment extends BasicAssistantListFragment implements SBRobotConnectionHandler,
                                                SBRemoteCommandListener,
                                                AdapterView.OnItemClickListener {
    private final static String CLSS = "DiscoveryFragment";
    private final static String IGNORE_KEY = "IGNORE";
    private final static String SET_APP_COMMAND   = "~/robotics/robot/bin/set_ros_application %s";
    private final static String START_APP_COMMAND = "~/robotics/robot/bin/restart_ros";
    private View contentView = null;
    private ViewGroup viewGroup = null;
    private MasterChecker masterChecker = null;
    private SBDbManager dbManager = null;
    private SBRobotManager robotManager = null;
    private SBApplicationManager applicationManager = null;
    private RemoteCommand command = null;


    // Called when the fragment's instance initializes
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(CLSS, "DiscoveryFragment.onCreate");
        super.onCreate(savedInstanceState);
    }

    // Called to have the fragment instantiate its user interface view.
    // Inflate the view for the fragment based on layout XML. Populate
    // the text fields and application list from the database.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(CLSS, "DiscoveryFragment.onCreateView");
        this.contentView = inflater.inflate(R.layout.fragment_discovery, container, false);
        this.viewGroup = container;
        TextView textView = contentView.findViewById(R.id.fragmentDiscoveryText);
        textView.setText(R.string.fragmentDiscoveryLabel);

        Button button = (Button) contentView.findViewById(R.id.connectButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectButtonClicked();
            }
        });
        button = (Button) contentView.findViewById(R.id.viewButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewRobotClicked();
            }
        });
        button = (Button) contentView.findViewById(R.id.terminateButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
                getActivity().finish();
            }
        });
        return contentView;
    }

    // Executes after onCreateView()
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(CLSS, "DiscoveryFragment.onViewCreated");
        dbManager = SBDbManager.getInstance();
        robotManager = SBRobotManager.getInstance();
        applicationManager = SBApplicationManager.getInstance();
        this.masterChecker = new MasterChecker(this);
        command = new RemoteCommand(dbManager.getSetting(SBConstants.ROS_HOST),
                                    dbManager.getSetting(SBConstants.ROS_USER),
                                    dbManager.getSetting(SBConstants.ROS_USER_PASSWORD),this);
        configureListView();
    }

    // The host activity has been created.
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(CLSS, "DiscoveryFragment.onActivityCreated");
        // We populate the list whether or not there is a robot connection. We simply hide it as appropriate.
        configureListView();
        updateUI();
    }

    private void configureListView() {
        ListView listView = getListView();
        listView.setItemsCanFocus(true);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        listView.setVisibility(View.INVISIBLE);
        listView.setEnabled(false);
        //listView.setOnItemClickListener(this);
        listView.setClickable(true);
        RobotApplicationsAdapter adapter = new RobotApplicationsAdapter(getContext(), new ArrayList<>());
        setListAdapter(adapter);
        adapter.clear();
        List<TabletApplication> applicationList = SBApplicationManager.getInstance().getApplications();
        Log.i(CLSS, String.format("configureListView: will display %d applications for all robots", applicationList.size()));
        adapter.addAll(applicationList);
    }

    // The fragment is visible
    @Override
    public void onStart() {
        super.onStart();
    }

    // The Fragment is visible and inter-actable
    @Override
    public void onResume() {
        super.onResume();
    }

    // Called when user leaves the current fragment or fragment is no longer inter-actable
    @Override
    public void onPause() {
        super.onPause();
    }

    // The fragment is going to be stopped
    @Override
    public void onStop() {
        super.onStop();
    }

    // Cleanup resources created in onCreateView()
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.contentView = null;
        Log.i(CLSS, "DiscoveryFragment.onDestroyView");
    }

    // Execute any final cleanup for the fragment's state
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // The fragment has been disassociated from its hosting activity
    @Override
    public void onDetach() {
        super.onDetach();
    }



    //======================================== Array Adapter ======================================
    public class RobotApplicationsAdapter extends ArrayAdapter<TabletApplication> implements ListAdapter {

        public RobotApplicationsAdapter(Context context, List<TabletApplication> values) {
            super(context, R.layout.discovery_application_item, values);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        // The view here is the row containing app name, description and on/off button. It appears that
        // clickable elements within the layout make the entire row unclickable. So we re-add the listener.
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.i(CLSS, String.format("RobotApplicationsAdapter.getView position =  %d", position));
            // Get the data item for this position
            TabletApplication tabapp = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                //Log.i(CLSS, String.format("RobotApplicationsAdapter.getView convertView was null"));
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.discovery_application_item, parent, false);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    onItemClick(getListView(),view,position,position);
                }
            });

            // Lookup view for data population
            TextView nameView = (TextView) convertView.findViewById(R.id.application_name);
            TextView descriptionView = (TextView) convertView.findViewById(R.id.application_description);
            ToggleButton statusToggle    = convertView.findViewById(R.id.application_selector);
            statusToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startApplicationClicked();
                }
            });

            // Populate the data into the template view using the data object
            nameView.setText(tabapp.getApplicationName());
            descriptionView.setText(tabapp.getDescription());
            updateStatusImage(tabapp,statusToggle);

            // Return the completed view to render on screen
            convertView.postInvalidate(0,0,convertView.getRight(),convertView.getBottom());
            return convertView;
        }
    }

    /**
     * Update the application status icon at the indicated position in the list. Use this version
     * where the application template is known. Run this on the UI Thread.
     *
     * @param app the current application
     * @param toggle button that shows the application state
     */
    private void updateStatusImage(final TabletApplication app, ToggleButton toggle ) {
        Log.i(CLSS, String.format("updateStatusImage: for %s (%s)",app.getApplicationName(),
                (applicationManager.getCurrentApplication()==null?null:applicationManager.getCurrentApplication().getExecutionStatus())));

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                toggle.setVisibility(View.INVISIBLE);
                TabletApplication tapp = applicationManager.getCurrentApplication();
                if( !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_ONLINE) ) {
                    toggle.setEnabled(false);
                    toggle.setChecked(false);
                    Log.i(CLSS, String.format("updateStatusImage: set ball GRAY for %s", app.getApplicationName()));
                }
                else if (tapp != null && app.getApplicationName().equalsIgnoreCase(tapp.getApplicationName())) {
                    if (tapp.getExecutionStatus().equalsIgnoreCase(TabletApplication.STATE_ACTIVE)) {
                        toggle.setEnabled(true);
                        toggle.setChecked(true);
                        Log.i(CLSS, String.format("updateStatusImage: set ball GREEN for %s", app.getApplicationName()));
                    }
                    else {
                        toggle.setEnabled(true);
                        toggle.setChecked(false);
                        Log.i(CLSS, String.format("updateStatusImage: set ball YELLOW for %s", app.getApplicationName()));
                    }
                    toggle.setVisibility(View.VISIBLE);
                }
            }
        });
    }
    // =========================================== Checker Callbacks ====================================
    /**
     * If this was a bluetooth error, then try wi-fi.
     * @param reason error description
     */
    @Override
    public void handleNetworkError(String reason) {
        Log.w(CLSS, String.format("handleNetworkError (%s): %s",robotManager.getConnectionState(),reason));
        if(robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_BLUETOOTH_CONNECTING)) {
            robotManager.setConnectionState(SBRobotManager.STATE_WIFI_CONNECTING);  // Next try Wifi
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Toast toast = Toast.makeText(getActivity().getBaseContext(), reason, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
            Log.i(CLSS, "handleNetworkError: (bluetooth) " + reason);
            checkWifi();
        }
        else {
            robotManager.setConnectionState(SBRobotManager.STATE_UNAVAILABLE);
            SBWarningDialog warning = SBWarningDialog.newInstance("Network Error", reason);
            warning.show(getActivity().getFragmentManager(), DIALOG_TRANSACTION_KEY);
        }
    }
    // We've made a network connection to the robot, but failed to read the parameters we require.
    // It may be that ROS is not running or is has been aborted. Disconnect.
    @Override
    public void handleRobotCommunicationError(String reason) {
        Log.w(CLSS, "handleRobotCommunicationError: " + reason);
        robotManager.setConnectionState(SBRobotManager.STATE_OFFLINE);
        applicationManager.setApplication(null);
        if(getActivity()!=null) {
            SBWarningDialog warning = SBWarningDialog.newInstance("Error connecting to robot", reason);
            warning.show(getActivity().getFragmentManager(), DIALOG_TRANSACTION_KEY);
        }
        robotManager.setConnectionState(SBRobotManager.STATE_OFFLINE);
    }

    // The application name is a global parameter of the robot. This method is called
    // by the MasterChecker once we've made contact with the robot. "Not Running"
    // means that the local counterpart is not running.
    //
    // Display the full list of applications and mark this one as selected.
    // By virtue of its discovery, we know it is running on the robot.
    @Override
    public void receiveApplication(String appName) {
        Log.w(CLSS, "receiveApplication: " + appName);
        applicationManager.setApplication(appName);
        if( getActivity()!=null ) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RobotApplicationsAdapter adapter = (RobotApplicationsAdapter)getListAdapter();
                    ListView listView = getListView();
                    listView.setEnabled(true);
                    updateUI();
                }
            });
        }
    }

    // The basic network connection is made. Now interrogate for robot characteristics.
    @Override
    public void receiveNetworkConnection() {
        Log.i(CLSS, "receiveNetworkConnection: SUCCESS!");
        robotManager.setConnectionState(SBRobotManager.STATE_STARTING_REMOTE);
        robotManager.setMasterURI(dbManager.getSetting(SBConstants.ROS_MASTER_URI));
        masterChecker.beginChecking(robotManager.getRobot(),1);  // Assume ROS running, only get one attempt.
    }

    // Update robot's network connection status
    @Override
    public void receiveRobotConnection() {
        Log.i(CLSS, "receiveRobotConnection: SUCCESS!");
        robotManager.setConnectionState(SBRobotManager.STATE_ONLINE);
    }

    //======================================== OnItemClickListener =====================================
    // If there has been no change to the list selection, do nothing. Otherwise,
    // disable the view to prevent clicks until we've heard from the robot.
    // Start the master checker to obtain robot characteristics - in particular the new application.
    // NOTE: We have circumvented the direct listener, via a click handler in the adapter class.
    //       Android has difficulty with the embedded button.
    @Override
    public void onItemClick(AdapterView<?> adapter, View v, int position,long rowId) {
        Log.i(CLSS, String.format("onItemClick: row %d",position));
        TabletApplication app = (TabletApplication)adapter.getItemAtPosition(position);
        if( applicationManager.getCurrentApplication()==null ||
               ! app.getApplicationName().equalsIgnoreCase(applicationManager.getCurrentApplication().getApplicationName() )) {

            applicationManager.setApplication(app.getApplicationName());
            robotManager.setConnectionState(SBRobotManager.STATE_STARTING_REMOTE);
            command.execute(IGNORE_KEY,String.format(SET_APP_COMMAND,app.getApplicationName()));
            command.sudo(app.getApplicationName(),START_APP_COMMAND);
            updateUI();
            robotManager.setMasterURI(dbManager.getSetting(SBConstants.ROS_MASTER_URI));
            masterChecker.beginChecking(robotManager.getRobot(),5);  // Give ROS some time to restart
        }
    }
    //======================================== Button Callbacks ======================================
    // This dialog is passive and just dismisses itself.
    public void viewRobotClicked() {
        Log.i(CLSS, "View robot clicked");
        SBRobotViewDialog addDialog = new SBRobotViewDialog();
        addDialog.show(getActivity().getFragmentManager(), DIALOG_TRANSACTION_KEY);
    }

    /**
     * Callback for the CONNECT button.
     */
    public void connectButtonClicked() {
        Log.i(CLSS, "Connect button clicked");
        RobotDescription robot = robotManager.getRobot();
        BluetoothManager bluetoothManager = (BluetoothManager)getActivity().getSystemService(BLUETOOTH_SERVICE);
        // If the robot is currently connected, we really mean "Disconnect"
        if(     !robotManager.getConnectionState().equals(SBRobotManager.STATE_UNAVAILABLE) &&
                !robotManager.getConnectionState().equals(SBRobotManager.STATE_UNCONNECTED) ) {

            applicationManager.setApplication(null);   // Shutdown current
            disconnect();
            robotManager.setConnectionState(SBRobotManager.STATE_UNCONNECTED);
            updateUI();

        }
        // Start the network connection attempt with bluetooth
        else {
            robotManager.setConnectionState(SBRobotManager.STATE_BLUETOOTH_CONNECTING);
            BluetoothChecker bluetoothChecker = new BluetoothChecker(this);
            String master = dbManager.getSetting(SBConstants.ROS_MASTER_URI);
            if( master.contains("xxx")) master = null;  // Our "hint" is not the real URI

            if (master != null) {
                robotManager.setMasterURI(master);
                // Supply device name - needs to match robot bluetooth adapter setting.
                robotManager.setDeviceName(dbManager.getSetting(SBConstants.ROS_PAIRED_DEVICE));
                bluetoothChecker.beginChecking(robotManager.getRobot(),bluetoothManager);
            }
            else {
                robotManager.setConnectionState(SBRobotManager.STATE_WIFI_CONNECTING); /// So it's final
                handleNetworkError("The MasterURI must be defined on the Settings panel");
            }
        }
    }

    private void checkWifi() {
        WifiChecker wifiChecker = new WifiChecker(this);
        String master = dbManager.getSetting(SBConstants.ROS_MASTER_URI);
        if( master.contains("xxx")) master = null;  // Our "hint" is not the real URI

        if (master != null) {
            WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
            // Supply SSID in case robot is not the connected WiFi.
            robotManager.setSSID(dbManager.getSetting(SBConstants.ROS_SSID));
            robotManager.setWifiPassword(dbManager.getSetting(SBConstants.ROS_WIFIPWD));
            wifiChecker.beginChecking(robotManager.getRobot(),wifiManager);
        }
        else {
            robotManager.setConnectionState(SBRobotManager.STATE_UNAVAILABLE);
            handleNetworkError("The MasterURI must be defined on the Settings panel");
        }
    }
    // Disconnect both wifi and bluetooth connections
    private void disconnect() {
        BluetoothManager bluetoothManager = (BluetoothManager)getActivity().getSystemService(BLUETOOTH_SERVICE);
        if( bluetoothManager.getAdapter()!=null )  {
            bluetoothManager.getAdapter().disable();
        }
        WifiManager wifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.disconnect();
    }
    //  Start/stop toggle clicked:
    //    If this is not the application currently running on the robot,
    //    restart ROS on the robot with the newly desired application.
    //    Signal any interested listeners that the application has started
    public void startApplicationClicked() {
        TabletApplication tabApp = applicationManager.getCurrentApplication();
        if( tabApp!=null ) {
            if( tabApp.getExecutionStatus().equals(TabletApplication.STATE_ACTIVE) ) {
                Log.i(CLSS, "Stop application clicked");
                applicationManager.stopApplication();
                robotManager.setConnectionState(SBRobotManager.STATE_OFFLINE);
            }
            else {
                Log.i(CLSS, "Start application clicked");
                robotManager.setConnectionState(SBRobotManager.STATE_STARTING_LOCAL);
                applicationManager.startApplication();
                robotManager.setConnectionState(SBRobotManager.STATE_STARTING_REMOTE);
                masterChecker.beginChecking(robotManager.getRobot(),3);
            }
            Log.i(CLSS, String.format("startApplicationClicked: current position is %d",getListView().getSelectedItemPosition()));
            updateUI();
        }
    }
    //======================================== Update the UI ======================================
    /**
     * Keep the views in-sync with the model state
     */
    private void updateUI() {
        RobotDescription robot = robotManager.getRobot();
        if( !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_ONLINE)) {
            robot = null;
        }
        Log.i(CLSS, String.format("updateUI robot:%s app:%s",(robot==null?"null":robot.getRobotName()),
                (applicationManager.getCurrentApplication()==null?"null":applicationManager.getCurrentApplication().getApplicationName())));

        Button button = (Button) contentView.findViewById(R.id.connectButton);
        button.setEnabled(true);
        ImageView robotImage = (ImageView) contentView.findViewById(R.id.robot_icon);
        if (    robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_UNCONNECTED) ||
                robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_UNAVAILABLE)) {
            button.setText(R.string.discoveryButtonConnect);
            robotImage.setVisibility(View.INVISIBLE);
        }
        else {
            button.setText((R.string.discoveryButtonDisconnect));
            robotImage.setVisibility(View.VISIBLE);
        }

        button = (Button) contentView.findViewById(R.id.viewButton);
        button.setEnabled(robot != null);

        // Show the progress bar is we are waiting on a connection
        ProgressBar bar = (ProgressBar) contentView.findViewById(R.id.progress_circle);
        if (    !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_BLUETOOTH_CONNECTING) &&
                !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_WIFI_CONNECTING)   &&
                !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_STARTING_REMOTE) ) {
            bar.setVisibility(View.INVISIBLE);
        }
        else {
            bar.setVisibility(View.VISIBLE);
            bar.setIndeterminate(true);
        }

        TextView tview = (TextView) contentView.findViewById(R.id.robot_name);
        if (robot == null) tview.setVisibility(View.INVISIBLE);
        else {
            tview.setText(robot.getRobotName());
            tview.setVisibility(View.VISIBLE);
        }

        // Show the connection warning indicator if there is no network
        ImageView connectionIndicator = contentView.findViewById(R.id.ros_not_running_icon);
        if (    !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_BLUETOOTH_CONNECTING) &&
                !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_WIFI_CONNECTING)   &&
                !robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_STARTING_REMOTE) ) {
            connectionIndicator.setVisibility(View.INVISIBLE);
        }
        else {
            connectionIndicator.setVisibility(View.VISIBLE);
        }

        // Show the list view if we've got a network connection
        // If we show the list view, show the connection state
        ListView listView = getListView();
        tview = (TextView) contentView.findViewById(R.id.connection_state);
        tview.setText(robotManager.getConnectionState());


        // We have an active application
        if ( robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_ONLINE) ) {
            listView.setVisibility(View.VISIBLE);
            tview.setVisibility(View.VISIBLE);
            // Select the current application in the list.
            int index = 0;
            int selectedPosition = applicationManager.indexOfCurrentApplication();
            List<TabletApplication> applicationList = applicationManager.getApplications();
            for(TabletApplication app:applicationList) {
                if(index == selectedPosition) {
                    listView.setItemChecked(index,true);
                    listView.setSelection(index);
                    Log.i(CLSS, String.format("receiveApplication: selected application %s (%d)",app.getApplicationName(),index));
                }
                else {
                    listView.setItemChecked(index,false);
                }
                index=index+1;
            }
        }
        // We have a network, but no application
        else if (   robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_STARTING_REMOTE) ||
                    robotManager.getConnectionState().equalsIgnoreCase(SBRobotManager.STATE_OFFLINE) ) {

            listView.setVisibility(View.VISIBLE);
            tview.setVisibility(View.VISIBLE);
            // Select the current (now former) application in the list.
            int index = 0;
            int selectedPosition = applicationManager.indexOfCurrentApplication();
            List<TabletApplication> applicationList = applicationManager.getApplications();
            for(TabletApplication app:applicationList) {
                if(index == selectedPosition) {
                    listView.setItemChecked(index,true);
                    listView.setSelection(index);
                    Log.i(CLSS, String.format("receiveApplication: selected application %s (%d)",app.getApplicationName(),index));
                }
                else {
                    listView.setItemChecked(index,false);
                }
                index=index+1;
            }
        }
        else {
            listView.setVisibility(View.INVISIBLE);
            tview.setVisibility(View.INVISIBLE);
        }
    }


    // ==================================== Remote Command Listener ================================
    @Override
    public void handleCommandError(String key,String command,String reason) {
        Log.i(CLSS, String.format("handleCommandError: %s=>%s (%s)",command,reason,key));
        robotManager.setConnectionState(SBRobotManager.STATE_OFFLINE);
        if(getActivity()!=null) {
            SBWarningDialog warning = SBWarningDialog.newInstance(String.format("Error executing: %s",command), reason);
            warning.show(getActivity().getFragmentManager(), DIALOG_TRANSACTION_KEY);
        }
    }

    @Override
    public void handleCommandCompletion(String key,String command,String returnValue) {
        Log.i(CLSS, String.format("handleCommandCompletion: %s=>%s (%s)",command,returnValue,key));
        if( !key.equalsIgnoreCase(IGNORE_KEY) ) {
            applicationManager.setApplication(key);  // Key is the newly selected application
            robotManager.setConnectionState(SBRobotManager.STATE_STARTING_REMOTE);
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    updateUI();
                }
            });
            masterChecker.beginChecking(robotManager.getRobot(),5); // Give it some time to spin up.
        }
    }
}
