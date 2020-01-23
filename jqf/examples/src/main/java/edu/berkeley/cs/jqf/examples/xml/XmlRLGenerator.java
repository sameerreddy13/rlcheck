package edu.berkeley.cs.jqf.examples.xml;

import edu.berkeley.cs.jqf.fuzz.rl.RLGenerator;
import edu.berkeley.cs.jqf.fuzz.rl.RLGuide;
import edu.berkeley.cs.jqf.fuzz.rl.RLParams;
import org.junit.Assume;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * Created by clemieux on 6/17/19.
 */
public class XmlRLGenerator implements RLGenerator {

    private RLGuide guide;


    private static DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();


    /** Max number of child nodes for each XML element. */
    private static final int MAX_NUM_CHILDREN = 4;

    /** Max number of attributes for each XML element. */
    private static final int MAX_NUM_ATTRIBUTES = 2;

    /**
     * Minimum size of XML tree.
     */
    private int minDepth = 0;

    /**
     * Maximum size of XML tree.
     */
    private int maxDepth = 5;

    /** Terminal action output */
//    public static final String terminal = "END";
    private final List<Object> BOOLEANS = Arrays.asList(new Boolean[]{true, false});
    private final List<Object> NUM_C =  Arrays.asList(RLGuide.range(0, MAX_NUM_CHILDREN));
    private final List<Object> NUM_A =  Arrays.asList(RLGuide.range(0, MAX_NUM_ATTRIBUTES));


    private int stateSize;
    private int boolId;
    private int textId;
    private int numcId;
    private int numaId;

    /* Need to initialize with parameters using init method after constructor is called. */
    public XmlRLGenerator() {}

    /**
     * Initialize models and generator parameters
     * @param params:
     *              stateSize,
     *              tags,
     *              numc,
     *              defaultEpsilon
     * */
    @Override
    public void init(RLParams params) {
        if (params.exists("seed")){
            guide = new RLGuide((long) params.get("seed"));
        } else {
            guide = new RLGuide();
        }
        double e = (double) params.get("defaultEpsilon", true);
        List<Object> text = (List<Object>) params.get("tags", true);

        this.stateSize = (int) params.get("stateSize", true);
        this.textId = guide.addLearner(text, e); // used to select tags, attributes, text, CDATA
        this.boolId = guide.addLearner(BOOLEANS, e); // used for all booleans
        this.numcId = guide.addLearner(NUM_C, e); // num children
        this.numaId = guide.addLearner(NUM_A, e); // num attributes
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

    /** Update using reward r */
    @Override
    public void update(int r){
        guide.update(r);
    }


    private Document populateDocument(Document document) {
        String[] stateArr = new String[stateSize];
        Element root = generateXmlTree(stateArr, document, 0);
        if (root != null) {
            document.appendChild(root);
        }
        return document;
    }

    /** Recursively generate XML */
    private Element generateXmlTree(String[] stateArr, Document document, int depth) {
        String rootTag = (String) guide.select(stateArr, textId);

        Element root = document.createElement(rootTag);
        stateArr = updateState(stateArr, "tag=" + rootTag);

        // Add attributes
        int numAttributes = (Integer) guide.select(stateArr, numaId);
        for (int i = 0; i < numAttributes; i++) {
            String [] attrState = updateState(stateArr, "attrVal");
            String attrKey = (String) guide.select(attrState, textId);
            attrState = updateState(stateArr, "attrKey="+attrKey);
            String attrValue = (String) guide.select(attrState, textId);
            root.setAttribute(attrKey, attrValue);
        }
        // Make children recursively or text or CDATA
        String[] textState = updateState(stateArr, "text");
        String[] CDATAState = updateState(stateArr, "CDATA");
        String[] childState = updateState(stateArr, "child");
        String textVal = null;
        if (depth < minDepth ||
                (depth < maxDepth && (boolean) guide.select(childState, boolId))) {
            int numChildren = (Integer) guide.select(stateArr, numcId);
            for (int i = 0; i < numChildren; i++) {
                Element child = generateXmlTree(stateArr, document, depth + 1);
                if (child != null) {
                    root.appendChild(child);
                }
            }
        } else if ((boolean) guide.select(textState, boolId)) {
            textVal = (String) guide.select(textState, textId);
            Text text = document.createTextNode(textVal);
            root.appendChild(text);
        } else if ((boolean) guide.select(CDATAState, boolId)){
            textVal = (String) guide.select(CDATAState, textId);
            Text text = document.createCDATASection(textVal);
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


}





