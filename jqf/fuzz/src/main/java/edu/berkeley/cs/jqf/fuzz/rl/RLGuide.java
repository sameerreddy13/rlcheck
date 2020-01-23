package edu.berkeley.cs.jqf.fuzz.rl;

import com.pholser.junit.quickcheck.Pair;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.lang.Math;
/**
 * Created by Sameer on 6/18/19.
 */

/**
 * Guide used by RL generator to make choices.
 * Functionality is as in GeneratorMC found in python/Generator.py
*/
public class RLGuide implements Guide {

    /**  Used in stateToString */
    public static final String stateDelim = " | ";

    /** Maps id to learning agent. Used for tracking tables for different action types. */
    private HashMap<Integer, RLLearner> idToRL;

    /** Counter for generating ids */
    private int ctr = 0;

    /** Source of randomness, shared across all learners */
    private Random rand; // Source of randomness. Can be initialized with chosen seed.

    /** Construct with random seed */
    public RLGuide() {
        this.idToRL= new HashMap<>();
        rand = new Random();
    }

    /** Construct with custom seed */
    public RLGuide(long seed) {
        this.idToRL= new HashMap<>();
        rand = new Random(seed);
    }

    /**
     * Add learner (uses Monte Carlo Control)
     * Returns: Corresponding id for learner
     */
    public int addLearner(List<Object> actionSpace, double epsilon) {
        return addLearner(actionSpace, epsilon, epsilon, 1.0);
    }

    /** With epsilon decay */
    public int addLearner(List<Object> actionSpace, double epsilon, double minEpsilon, double decay) {
        assert !idToRL.containsKey(ctr);
        RLLearner newLearner = new RLLearner(ctr, actionSpace, epsilon, minEpsilon, decay, rand);
        idToRL.put(ctr, newLearner);
        return ctr++;
    }

    /**
     * Selects action from actions using learner id.
     * It is necessary to create a learner with addLearner before calling select.
     */
    @Override
    public Object select(List<Object> actions, String state, int id) {
        RLLearner l = getLearner(id);
        return l.select(actions, state);
    }

    public Object select(List<Object> actions, String[] stateArr, int id) {
        String state = stateToString(stateArr);
        RLLearner l = getLearner(id);
        return l.select(actions, state);
    }

    /**
     * Selects from all possible actions.
     */
    public Object select(String state, int id) {
        RLLearner l = getLearner(id);
        return l.select(state);
    }

    public Object select(String[] stateArr, int id) {
        String state = stateToString(stateArr);
        RLLearner l = getLearner(id);
        return l.select(state);
    }

    /** Iteratively updates each learner */
    public void update(int r) {
        for (RLLearner l : idToRL.values()) {
            l.update(r);
            l.episode.clear();
            l.epsilon = Math.min(l.minEpsilon, l.decay * l.epsilon);
        }
    }

    /* Generates integer interval [lower, upper] */
    public static Integer[] range(int lower, int upper) {
        Integer[] intRange = new Integer[upper - lower + 1];
        for (int i = lower; i < upper + 1; i++)
            intRange[i - lower] = i;
        return intRange;
    }


    private RLLearner getLearner(int id) {
        return idToRL.get(id);
    }

    private String stateToString(String[] stateArr) {
        String state = "";
        for (int i = 0; i < stateArr.length; i++) {
            String s = stateArr[i];
            if (s != null) {
                if (i != stateArr.length - 1) {
                    s += stateDelim;
                }
                state += s;
            }
        }
        return state;
    }
    
    /** Force the episode */
    public void seed(List<Pair<Integer, SimpleEntry<String,Object>>> episode) {
        for (Pair<Integer, SimpleEntry<String, Object>> idToAction : episode) {
            idToRL.get(idToAction.first).forceAction(idToAction.second);
        }
    }


    public boolean bernoulli(double p) {
        assert p <= 1;
        return rand.nextDouble() <= 1;
    }

    /** Return integer in [0, 1, ..., bound). */
    public int randomInt(int bound) {
        return rand.nextInt(bound);
    }

    /** Get action space of corresponding learner. */
    public List<Object> getActions(int id) {
        return new ArrayList<>(getLearner(id).actionSpace);
    }

    public void clearEpisode(int id) {
        getLearner(id).episode.clear();
    }

    public void printEpisode(int id) {
        getLearner(id).printEpisode();
    }

}




