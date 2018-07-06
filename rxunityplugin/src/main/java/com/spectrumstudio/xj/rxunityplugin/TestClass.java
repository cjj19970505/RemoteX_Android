package com.spectrumstudio.xj.rxunityplugin;

public class TestClass {

    private String name;
    public static TestClass GetInstance(String name) {
        return new TestClass(name);
    }

    public TestClass(String name){
        this.name = name;
    }

    public int CalculateAdd(int one, int another) {
        return one + another;
    }



}
