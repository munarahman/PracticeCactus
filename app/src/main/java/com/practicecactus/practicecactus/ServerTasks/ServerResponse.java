package com.practicecactus.practicecactus.ServerTasks;

import org.json.JSONObject;

/**
 * Created by christopherarnold on 2016-11-08.
 */

public class ServerResponse {

    private int code;
    private JSONObject response;

    public ServerResponse(int code, JSONObject response) {
        this.code = code;
        this.response = response;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int newCode) { this.code = newCode; }

    public JSONObject getResponse() {
        return this.response;
    }
}
