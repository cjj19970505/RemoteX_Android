package com.spectrumstudio.xj.rxunityplugin;

public class UnityByteArrayWrapper{
    private byte[] array;
    public UnityByteArrayWrapper(byte[] array){
        this.array = array;
    }
    public byte get(int index){
        return array[index];
    }
    public int getLength(){
        return array.length;
    }
}
