package edu.berkeley.cs.jqf.fuzz.rl;

/**
 * Created by Sameer on 6/21/19.
 */

import java.util.*;

/**
 * And individual learner that learns using Monte Carlo control.
 * Stores our policy and Q-table.
 */
public class RLLearner {
    double epsilon;
    double decay;
    double minEpsilon;

    /**
     * Space of possible actions
     */
    List<Object> actionSpace;
    /**
     * Stores Q and C values. State maps to map which maps action to (C, Q) pairs.
     */
    private HashMap<String, HashMap> qcTable;
    /**
     * Current episode
     */
    List<AbstractMap.SimpleEntry<String, Object>> episode;
    /**
     * Source of randomness
     */
    private Random rand;

    // Currently unused
    private int id;

    RLLearner(int id, List<Object> actionSpace, double epsilon, double minEpsilon, double decay, Random rand) {
        this.id = id;
        this.actionSpace = actionSpace;
        this.epsilon = epsilon;
        this.minEpsilon = minEpsilon;
        this.decay = decay;
        this.rand = rand;

        this.episode = new ArrayList<>();
        this.qcTable = new HashMap<>();

    }

    /*
     * MCC update from episode and reward.
    */
    void update(int r) {
        AbstractMap.SimpleEntry<String, Object> saPair;
        AbstractMap.SimpleEntry<Double, Double> qcPair;
        int T = episode.size();
        int G = r;
        int W = 1;
        for (int i = 0; i < T; i++) {
            saPair = episode.get(T - i - 1);
            String state = getState(saPair);
            Object action = getAction(saPair);
            qcPair = QC(state, action);

            Double new_c = getC(qcPair) + W;
            Double new_q = getQ(qcPair) + (W / new_c) * (G - getQ(qcPair));
            updateQC(state, action, new_q, new_c);
        }
    }


    /*
     * Select best action from given state.
     * Uses epsilon-greedy strategy. Guided by qcTable Q values.
     * Saves state action pair to episode.
     * */
     Object select(List<Object> actions, String state) {
        assert actions.size() > 0;
        List<Object> best_actions = new ArrayList<>();
        AbstractMap.SimpleEntry<String, Object> saPair;
        Double best_Q = -Double.MAX_VALUE;

        // Use all actions with probability epsilon
        if (rand.nextDouble() <= epsilon || get_action_table(state) == null) {
            best_actions = actions;
        } else {
            // Use actions with maximal Q-value
            for (Object action : actions) {
                Double q = getQ(QC(state, action));
                if (q >= best_Q) {
                    if (q > best_Q) {
                        best_Q = q;
                        best_actions.clear();
                    }
                    best_actions.add(action);
                }
            }
        }
        // We return a random action from best_actions
        int random_idx = rand.nextInt(best_actions.size());
        Object chosen_action = best_actions.get(random_idx);
        saPair = new AbstractMap.SimpleEntry<>(state, chosen_action);
        episode.add(saPair);
        return chosen_action;
    }

    /**
     * Select from all choices (actionSpace)
     */
    Object select(String state) {
        return select(actionSpace, state);
    }

    void printEpisode() {
        String printStr = "[";
        for (AbstractMap.SimpleEntry<String, Object> saPair : episode) {
            printStr += "(STATE: " + getState(saPair) + " ACTION: " + getAction(saPair).toString() + "), ";
        }
        printStr += "]";
        System.out.println(printStr);
    }

    /**
     *  Returns SimpleEntry "pair" with "key" as Q value, "value" as C value
     *  Use getQ and getC as helpers.
     */
    private AbstractMap.SimpleEntry<Double, Double>
    QC(String state, Object action) {
        HashMap<Object, AbstractMap.SimpleEntry> QC_action = get_action_table(state);
        if (QC_action != null) {
            if (QC_action.containsKey(action)) {
                return QC_action.get(action);
            }
        }
        return new AbstractMap.SimpleEntry<>(0., 0.);
    }

    /** Returns action table for given state. Returns null if state not found. */
    private HashMap<Object, AbstractMap.SimpleEntry>
    get_action_table(String state) {
        if (qcTable.containsKey(state)) {
            return qcTable.get(state);
        }
        return null;
    }

    private void updateQC(String state, Object action, Double q, Double c) {
        AbstractMap.SimpleEntry<Double, Double> new_qc = new AbstractMap.SimpleEntry<>(q, c);
        HashMap<Object, AbstractMap.SimpleEntry> QC_action;
        QC_action = get_action_table(state);
        if (QC_action == null) {
            // Table for given state not found
            // Create action table
            QC_action = new HashMap<>();
            // Add to qcTable
            qcTable.put(state, QC_action);
        }
        QC_action.put(action, new_qc);
    }

    private Double getQ(AbstractMap.SimpleEntry<Double, Double> qcPair) {
        return qcPair.getKey();
    }

    private Double getC(AbstractMap.SimpleEntry<Double, Double> qcPair) {
        return qcPair.getValue();
    }

    private String getState(AbstractMap.SimpleEntry<String, Object> saPair  ) {
        return saPair.getKey();
    }

    private Object getAction(AbstractMap.SimpleEntry<String, Object> saPair  ) {
        return saPair.getValue();
    }

    public void forceAction(AbstractMap.SimpleEntry<String, Object> action) {
        episode.add(action);
    }

}