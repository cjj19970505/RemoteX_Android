package com.spectrumstudio.xj.rxunityplugin;

public interface IRemoteXPluginCallback {
    void test();
    void onReceiveMessage(IConnection connection, UnityByteArrayWrapper message);
}
