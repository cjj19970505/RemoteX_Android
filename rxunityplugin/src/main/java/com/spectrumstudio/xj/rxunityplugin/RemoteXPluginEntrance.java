package com.spectrumstudio.xj.rxunityplugin;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.util.UUID;

public class RemoteXPluginEntrance implements ConnectionManager.Callback{
    private static final String TAG = "RemoteXPluginEntrance";
    private static RemoteXPluginEntrance instance;

    public static RemoteXPluginEntrance getInstance() {
        if(instance == null){
            instance = new RemoteXPluginEntrance();
        }
        return instance;
    }
    /*
    public interface Callback{
        void test();
    }*/
    private ConnectionManager connectionManager;
    private BluetoothConnectionManager bluetoothConnectionManager;
    private Activity unityPlayerActivity;
    private IRemoteXPluginCallback callback;
    private String pluginGameObjectName;

    private RemoteXPluginEntrance(){

    }

    public void initiate(String pluginGameObjectName ,final Activity unityPlayerActivity, IRemoteXPluginCallback callback){
        this.pluginGameObjectName = pluginGameObjectName;
        this.unityPlayerActivity = unityPlayerActivity;
        connectionManager = new ConnectionManager();
        connectionManager.addCallback(this);
        bluetoothConnectionManager = new BluetoothConnectionManager();
        this.callback = callback;
        String mac = "DC:53:60:DD:AE:63";
        UUID uuid = UUID.fromString("14c5449a-6267-4c7e-bd10-63dd79740e50");
        BluetoothDevice device = bluetoothConnectionManager.GetBluetoothDevice(mac);
        IConnection connection = bluetoothConnectionManager.createRfcommClientConnection(device, uuid);
        connectionManager.setControllerConnection(connection);
        connection.connect();
        unityPlayerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(unityPlayerActivity, "RemoteX Successfully launched", Toast.LENGTH_LONG).show();
            }
        });

    }



    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    @Override
    public void onControllerConnectionEstablishResult(IConnection connection, final IConnection.ConnectionEstablishState connectionEstablishState) {
        //UnityPlayer.UnitySendMessage();
        if(connectionEstablishState == IConnection.ConnectionEstablishState.Succeed){
            callback.test();
        }
        unityPlayerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(unityPlayerActivity, connectionEstablishState.toString(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onControllerConnectionReceiveMessage(IConnection connection, final byte[] message) {
        //callback.onReceiveMessage(connection, new UnityByteArrayWrapper(message));
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i<message.length;i++){
            sb.append(""+message[i]);
            sb.append(',');
        }
        UnityPlayer.UnitySendMessage(pluginGameObjectName, "DroidOnReceiveMessageCallback", sb.toString());
    }

    //========Some Helper Function======================
    public boolean isSameInstance(Object o1, Object o2){
        return o1 == o2;
    }


}
