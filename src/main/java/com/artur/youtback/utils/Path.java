package com.artur.youtback.utils;

public enum Path {
    IMAGE("./image");


    private String path;
    Path(String path){
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return this.path;
    }
}
