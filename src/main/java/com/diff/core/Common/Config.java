package com.diff.core.Common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.nio.charset.Charset;

/**
 * 读取项目配置
 */
public class Config {
    private final static Config instance = new Config();
    private JSONObject configJson;
    private Config(){
        try{
            // 读取jar包内的config.json
            InputStream is = this.getClass().getResourceAsStream("/config.json");
            BufferedReader in = new BufferedReader((new InputStreamReader(is, Charset.forName("UTF-8"))));
            StringBuffer buffer = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            configJson = JSONObject.parseObject(buffer.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static Config getInstance(){
        return instance;
    }

    public void setValue(String key, String value) throws NoSuchFieldException {
        configJson.put(key, value);
    }
    public String getString(String key){
        return configJson.getString(key);
    }
    public JSONObject getJSONObject(String key){
        return configJson.getJSONObject(key);
    }
    public JSONArray getJSONArray(String key){
        return configJson.getJSONArray(key);
    }
    public Integer getInteger(String key){
        return configJson.getInteger(key);
    }
}
