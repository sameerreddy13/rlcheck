package edu.berkeley.cs.jqf.fuzz.rl;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by clemieux on 6/17/19.
 */
public interface RLGenerator {

    /**
     * Parameter initialization function
     * @param params: table of parameters
     */
    void init(RLParams params);
    /**
     * Generate the next input
     * @return The next input as an InputStream
     */
    String generate();

    /**
     * Update the state of the generator given a reward
     * @param r reward
     */
    void update(int r);


}
