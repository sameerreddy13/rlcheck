package edu.berkeley.cs.jqf.examples.xml;

import com.pholser.junit.quickcheck.Pair;
import edu.berkeley.cs.jqf.fuzz.rl.RLGenerator;
import edu.berkeley.cs.jqf.fuzz.rl.RLOracle;
import edu.berkeley.cs.jqf.fuzz.rl.RLParams;
import org.junit.Assume;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Created by clemieux on 6/17/19.
 */
public class XmlRLGeneratorFC implements RLGenerator {

    private RLOracle oracle;


    private static DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();


    /** Mean number of attributes for each XML element. */
    private static final int MEAN_NUM_ATTRIBUTES = 2;


    /**
     * Minimum size of XML tree.
     */
    private int minDepth = 0;

    /**
     * Maximum size of XML tree.
     */
    private int maxDepth = 5;
    // Used in stateToString
    public static final String stateDelim = " | ";
    // Terminal action output
    public static final String terminal = "END";


    private int stateSize;
    private int tagId;
    private int attrId;
    private int numcId;
    private int numaId;
    private int bools1Id;
    private int bools2Id;
    private int bools3Id;
    private int text1id;
    private int text2id;
    private int attrTextId;

    /* Need to initialize with parameters using init method after constructor is called. */
    public XmlRLGeneratorFC() {
    }

    public void init(RLParams params) {
        if (params.exists("seed")){
            oracle = new RLOracle((long) params.get("seed"));
        } else {
            oracle = new RLOracle();
        }

        this.stateSize = (int) params.get("stateSize");
        List<Object> tags = (List<Object>) params.get("tags");
        List<Object> numc = (List<Object>) params.get("numc");
        List<Object> numa = (List<Object>) params.get("numa");
        List<Object> bools = (List<Object>) params.get("boolL");
        double tagEpsilon = (double) params.get("tagEpsilon");
        double attrEpsilon = (double) params.get("attrEpsilon");
        double numcEpsilon = (double) params.get("numcEpsilon");
        double numaEpsilon = (double) params.get("numaEpsilon");


        tags.add(terminal);
        tagId = oracle.addLearner( tags, tagEpsilon);
        attrId = oracle.addLearner(tags, attrEpsilon);
        attrTextId = oracle.addLearner(tags, attrEpsilon);
        numcId=  oracle.addLearner(numc, numcEpsilon);
        numaId=  oracle.addLearner( numa, numaEpsilon);
        bools1Id = oracle.addLearner(bools, 0.25);
        bools2Id = oracle.addLearner(bools, 0.25);
        bools3Id = oracle.addLearner(bools, 0.25);
        text1id = oracle.addLearner(tags, tagEpsilon);
        text2id = oracle.addLearner(tags, tagEpsilon);

        //seedAnt();

    }


    /**
     * Generators a random XML document.
     * @return a randomly-generated XML document
     */
    @Override
    public String generate() {
        DocumentBuilder builder;
        try {
            builder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        Document document = builder.newDocument();
        try {
            populateDocument(document);
        } catch (DOMException e) {
            Assume.assumeNoException(e);
        }
        return  XMLDocumentUtils.documentToString(document);
    }

    public void seedOracle(List<Pair<Integer, AbstractMap.SimpleEntry<String,Object>>> episode) {
        oracle.seed(episode);
        oracle.update(20);

    }

    public void seedAnt(){
        List<Pair<Integer, AbstractMap.SimpleEntry<String,Object>>> episode = new ArrayList<>();
        String[] stateArr = new String[stateSize];
        Pair<Integer, AbstractMap.SimpleEntry<String,Object>> project_tag = new Pair<>(tagId, new AbstractMap.SimpleEntry<>(stateToString(stateArr), "project"));
        stateArr= updateState(stateArr, "project");
        Pair<Integer, AbstractMap.SimpleEntry<String,Object>> one_attribute = new Pair<>(numaId, new AbstractMap.SimpleEntry<>(stateToString(stateArr), 1));
        Pair<Integer, AbstractMap.SimpleEntry<String,Object>> default_attr = new Pair<>(attrId, new AbstractMap.SimpleEntry<>(stateToString(stateArr), "default"));
        episode.add(project_tag);
        episode.add(one_attribute);
        episode.add(default_attr);
        seedOracle(episode);
    }

    public void update(int r){
        oracle.update(r);
    }

    private String makeTag(String state) {
        return (String) oracle.select(state, tagId);
    }

    private String makeAttribute(String state) {
        return (String) oracle.select(state, attrId);
    }

    private String makeAttributeText(String state) {
        return (String) oracle.select(state, attrTextId);
    }

    private Integer makeNumChildren(String state) {
        return (Integer) oracle.select(state, numcId);
    }

    private Integer makeNumAttributes(String state) { return (Integer) oracle.select(state, numaId);}

    private String makeText(String state) {return (String) oracle.select(state, text1id); }

    private String makeCDATAText(String state) {return (String) oracle.select(state, text2id); }

    private Document populateDocument(Document document) {
//        String rootTag = makeTag("");
//        Element root = document.createElement(rootTag);
//
//        String[] stateArr = new String[]{rootTag};
//        populateElement(stateArr, document, root, 0);
//        document.appendChild(root);
//        return document;
        Element root = null;
        String[] stateArr = new String[stateSize];
        while (root == null) {
            root = generateTree(stateArr, document, 0);
        }
        document.appendChild(root);
        return document;
    }

    private Element generateTree(String[] stateArr, Document document, int depth) {
        String state = stateToString(stateArr);
        String rootTag = makeTag(state);
        if (rootTag.equals(terminal)) {
            return null;
        }

        Element root = document.createElement(rootTag);
        String[] newStateArr = updateState(stateArr, rootTag);
        String newState = stateToString(newStateArr);

        // Add attributes
        int numAttributes = makeNumAttributes(newState);
        for (int i = 0; i < numAttributes; i++) {
            String attrKey = makeAttribute(newState);
            if (!attrKey.equals(terminal)){
                String [] attrState = updateState(newStateArr, attrKey);
                String attrValue = makeAttributeText(stateToString(attrState));
                if (!attrValue.equals(terminal)) {
                    root.setAttribute(attrKey, attrValue);
                }
            }
        }
        // Make children recursively
        if (depth < minDepth || (depth < maxDepth && (Boolean) oracle.select(newState, bools1Id))) {
            int numChildren = makeNumChildren(newState);
            for (int i = 0; i < numChildren; i++) {
                Element child = generateTree(newStateArr, document, depth + 1);
                if (child != null) {
                    root.appendChild(child);
                }
            }
        } else if ((Boolean) oracle.select(newState, bools2Id)) {
            // Add text
            Text text = document.createTextNode(makeText(newState));
            root.appendChild(text);
        } else if ((Boolean) oracle.select(newState, bools3Id)) {
            // Add text as CDATA
            Text text = document.createCDATASection(makeCDATAText(newState));
            root.appendChild(text);
        }
        return root;
    }

    /* Update and return new state. Removes items if too long */
    private String[] updateState(String[] stateArr, String stateX) {
        String[] newState = new String[stateSize];
        int end = stateSize - 1;
        newState[end] = stateX;
        for (int i = 0; i < end; i++) {
            newState[i] = stateArr[i + 1];
        }
        return newState;
    }

    private String stateToString(String[] stateArr) {
        return String.join(stateDelim, stateArr);
    }

}





