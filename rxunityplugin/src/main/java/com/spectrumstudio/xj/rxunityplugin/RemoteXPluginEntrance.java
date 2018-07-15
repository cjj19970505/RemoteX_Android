package com.spectrumstudio.xj.rxunityplugin;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.unity3d.player.UnityPlayer;

import java.util.UUID;

public class RemoteXPluginEntrance extends Fragment implements ConnectionManager.Callback{
    private static final String TAG = "RemoteXPluginEntrance";
    private static RemoteXPluginEntrance instance;

    public static RemoteXPluginEntrance getInstance() {
        if(instance == null){
            instance = new RemoteXPluginEntrance();
        }
        return instance;
    }
    private ConnectionManager connectionManager;
    private BluetoothConnectionManager bluetoothConnectionManager;
    private Activity unityPlayerActivity;
    private IRemoteXPluginCallback callback;
    private String pluginGameObjectName;

    public RemoteXPluginEntrance(){
        super();
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
        //BluetoothDevice device = bluetoothConnectionManager.GetBluetoothDevice(mac);
        //IConnection connection = bluetoothConnectionManager.createRfcommClientConnection(device, uuid);
        //connectionManager.setControllerConnection(connection);
        //connection.connect();
        UnityPlayer.currentActivity.getFragmentManager().beginTransaction().add(instance, "RemoteXPluginEntrance").commit();
        unityPlayerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(unityPlayerActivity, "RemoteX Successfully launched", Toast.LENGTH_LONG).show();
            }
        });
    }



    public void startScanQRCode(){
        IntentIntegrator.forFragment(this).initiateScan();
    }


    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public BluetoothConnectionManager getBluetoothConnectionManager() {
        return bluetoothConnectionManager;
    }

    @Override
    public void onControllerConnectionEstablishResult(IConnection connection, final IConnection.ConnectionEstablishState connectionEstablishState) {
        UnityPlayer.UnitySendMessage(pluginGameObjectName, "DroidOnControllerConnectionEstablishResultCallback", connectionEstablishState.getStateCode()+"");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(unityPlayerActivity, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(unityPlayerActivity, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                UnityPlayer.UnitySendMessage(pluginGameObjectName, "DroidOnScanQRCodeResult", result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    //========Some Helper Function======================
    public boolean isSameInstance(Object o1, Object o2){
        return o1 == o2;
    }

    public UUID getUuidFromString(String sUuid){
        return UUID.fromString(sUuid);
    }


}
