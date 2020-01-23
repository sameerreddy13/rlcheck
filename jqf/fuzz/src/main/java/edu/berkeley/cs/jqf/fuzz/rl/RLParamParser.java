package edu.berkeley.cs.jqf.fuzz.rl;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by clemieux on 6/27/19.
 */
public class RLParamParser {

    public RLParams getParams(String jsonStr) {
        JSONObject jsonParams = new JSONObject(jsonStr);
        RLParams params = new RLParams();
        if (jsonParams.has("params")) {
            JSONArray paramList = jsonParams.getJSONArray("params");
            for (int i = 0; i < paramList.length(); i++){
                JSONObject jsonParam = paramList.getJSONObject(i);
                String varName = jsonParam.getString("name");
                String stringType = jsonParam.getString("type");
                Object value;
                JSONArray jsonArray;
                switch (stringType) {
                    case "string":
                        value = jsonParam.getString("val");
                        params.add(varName, value);
                        break;
                    case "bool":
                        value = jsonParam.getBoolean("val");
                        params.add(varName, value);
                        break;
                    case "long":
                        value = jsonParam.getLong("val");
                        params.add(varName, value);
                        break;
                    case "int":
                        value = jsonParam.getInt("val");
                        params.add(varName, value);
                        break;
                    case "double":
                        value = jsonParam.getDouble("val");
                        params.add(varName, value);
                        break;
                    case "string_array":
                        List<String> strArr = new ArrayList<String>();
                        jsonArray = jsonParam.getJSONArray("val");
                        for (int j = 0; j < jsonArray.length(); j++){
                            strArr.add(jsonArray.getString(j));
                        }
                        params.add(varName, strArr);
                        break;
                    case "bool_array":
                        List<Boolean> boolArr = new ArrayList<Boolean>();
                        jsonArray = jsonParam.getJSONArray("val");
                        for (int j = 0; j < jsonArray.length(); j++){
                            boolArr.add(jsonArray.getBoolean(j));
                        }
                        params.add(varName, boolArr);
                        break;
                    case "int_array":
                        List<Integer> intArr = new ArrayList<Integer>();
                        jsonArray = jsonParam.getJSONArray("val");
                        for (int j = 0; j < jsonArray.length(); j++){
                            intArr.add(jsonArray.getInt(j));
                        }
                        params.add(varName, intArr);
                        break;
                    case "double_array":
                        List<Double> doubleArr = new ArrayList<Double>();
                        jsonArray = jsonParam.getJSONArray("val");
                        for (int j = 0; j < jsonArray.length(); j++){
                            doubleArr.add(jsonArray.getDouble(j));
                        }
                        params.add(varName, doubleArr);
                        break;
                }
            }
            return params;

        } else {
            throw new JSONException("Object needs to have params as root");
        }

    }

    public RLParams getParamsFile(String filename) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filename));
        String fileContents = String.join("\n", lines);
        return getParams(fileContents);

    }
}
