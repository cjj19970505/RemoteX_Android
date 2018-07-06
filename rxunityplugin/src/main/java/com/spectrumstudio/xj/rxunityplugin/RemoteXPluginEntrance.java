package com.spectrumstudio.xj.rxunityplugin;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.util.UUID;

public class RemoteXPluginEntrance implements ConnectionManager.Callback{
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

    private RemoteXPluginEntrance(){

    }
    public int CalculateAdd(int one, int another) {
        return one + another;
    }

    public void initiate(final Activity unityPlayerActivity, IRemoteXPluginCallback callback){
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
        unityPlayerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(unityPlayerActivity,"Get ControllerManager",Toast.LENGTH_SHORT).show();
            }
        });
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
    public void onControllerConnectionReceiveMessage(IConnection connection, byte[] message) {

    }

    public void ArrayTest(final byte[] bytes){
        unityPlayerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(unityPlayerActivity, ByteUtil.getString(bytes), Toast.LENGTH_LONG).show();
            }
        });

    }

    //========Some Helper Function======================
    public boolean isSameInstance(Object o1, Object o2){
        return o1 == o2;
    }
}
