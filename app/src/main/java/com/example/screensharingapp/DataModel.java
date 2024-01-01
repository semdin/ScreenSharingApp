package com.example.screensharingapp;

enum DataModelType {
    SignIn, StartStreaming, EndCall, Offer, Answer, IceCandidates
}


public class DataModel {
    private DataModelType type;
    private String username;
    private String target;
    private Object data;

    public DataModel(DataModelType type, String username, String target, Object data) {
        this.type = type;
        this.username = username;
        this.target = target;
        this.data = data;
    }

    public DataModelType getType() {
        return type;
    }

    public void setType(DataModelType type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}

