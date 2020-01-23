package edu.berkeley.cs.jqf.fuzz.rl;

import java.util.HashMap;
/**
 * Created by Sameer on 6/19/19.
 */
public class RLParams {
    private HashMap<String, Object> params;
    /* Initialize from hashmap */
    public RLParams(HashMap<String, Object> p) {
        params = new HashMap<>(p); // Shallow copy
    }

    public RLParams(){
        params = new HashMap<>();
    }

    public void add(String name, Object value) {
        params.put(name, value);
    }

    /*
    Gets param with given name.
    If assertExists = true, method errors if param not found.
     */
    public Object get(String name, boolean assertExists) {
        if (assertExists) {
            System.out.println("Checking if " + name +  " is in parameters");
            assert this.exists(name);
        }
        return this.get(name);
    }

    /* Same as get above but with assertExists set to false. */
    public Object get(String name) {
        return params.get(name);
    }

    public boolean exists(String name) {
        return params.containsKey(name);
    }
}
