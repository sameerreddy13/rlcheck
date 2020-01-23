package edu.berkeley.cs.jqf.fuzz.rl;

import java.util.HashMap;
import java.util.Random;

/**
 * Created by clemieux on 6/17/19.
 */
public class MockRLGenerator implements  RLGenerator{

    private String[] choices = {"abcd", "<broken", "<a> </a>", "<project/>", "<project> </project>", "<project> <namespace/> </project>"};
    private Random random = new Random();

    public MockRLGenerator() {}

    public String generate() {
        int pos = random.nextInt(choices.length);
        return choices[pos];
    }

    public void update(int r) {

    }
    public void init(RLParams params) {

    }
}
