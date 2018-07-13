package com.spectrumstudio.xj.remotex;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.spectrumstudio.xj.rxunityplugin.BluetoothConnectionManager;
import com.spectrumstudio.xj.rxunityplugin.ConnectionManager;
import com.spectrumstudio.xj.rxunityplugin.IConnection;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ConnectionManager.Callback {
    private static final String TAG = "MainActivity";
    BluetoothConnectionManager bluetoothConnectionManager;
    IConnection connection;

    ConnectionManager connectionManager;

    Button btn_Connect;
    Button btn_Abort;
    Button btn_Scan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothConnectionManager = new BluetoothConnectionManager();
        connectionManager = new ConnectionManager();
        connectionManager.addCallback(this);

        btn_Connect = (Button)findViewById(R.id.btn_Connect);
        btn_Connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mac = "DC:53:60:DD:AE:63";
                UUID uuid = UUID.fromString("14c5449a-6267-4c7e-bd10-63dd79740e50");
                BluetoothDevice device = bluetoothConnectionManager.GetBluetoothDevice(mac);
                connection = bluetoothConnectionManager.createRfcommClientConnection(device, uuid);
                connectionManager.setControllerConnection(connection);
                connection.connect();
            }
        });
        btn_Abort = (Button)findViewById(R.id.btn_Abort);
        btn_Abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connection.abortConnecting();
            }
        });
        btn_Scan = (Button)findViewById(R.id.btn_Scan);
        btn_Scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new IntentIntegrator(MainActivity.this).initiateScan();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onControllerConnectionEstablishResult(IConnection connection, final IConnection.ConnectionEstablishState connectionEstablishState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, connectionEstablishState.toString(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onControllerConnectionReceiveMessage(IConnection connection, byte[] message) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<message.length;i++){
            sb.append("(byte)("+(int)(message[i])+")"+", ");
        }
        Log.i(TAG, sb.toString());
    }
}
