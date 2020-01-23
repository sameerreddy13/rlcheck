package edu.berkeley.cs.jqf.examples.js;
import edu.berkeley.cs.jqf.fuzz.rl.RLGenerator;
import edu.berkeley.cs.jqf.fuzz.rl.RLOracle;
import edu.berkeley.cs.jqf.fuzz.rl.RLParams;
import java.util.*;
import java.util.function.Function;



public class JavaScriptRLGenerator implements RLGenerator {

    private RLOracle oracle;

    private static final int MAX_IDENTIFIERS = 50;
    private static final int MAX_EXPRESSION_DEPTH = 7;
    private static final int MAX_STATEMENT_DEPTH = 4;
    private Set<String> identifiers;
    private int statementDepth;
    private int expressionDepth;
    private int stateSize;
    private int intId;
    private int boolId;
    private int chrId;
    private int selectId;

    /** Terminal action output */
    public static final String terminal = "END"; // Unused


    private int MIN_INT = 0;
    private int MAX_INT = 5;
    private int MAX_STR_LEN = 3;

    private static final Boolean[] BOOLEANS = {true, false};

    private static final String[] STATEMENTS_1 = {
            "expression", "break", "continue", "return", "throw", "var", "empty"
    };

    private static final String[] STATEMENTS_2 = {
            "if", "for", "while", "namedfunc", "switch", "try", "block"
    };

    private static final String[] EXPRESSIONS_1 = {
            "literal", "ident"
    };
    private static final String[] EXPRESSIONS_2 = {
            "unary", "binary", "ternary", "call", "function",
            "property", "index", "arrow"
    };
    private static final String[] UNARY_TOKENS = {
            "!", "++", "--", "~",
            "delete", "new", "typeof"
    };

    private static final String[] BINARY_TOKENS = {
            "!=", "!==", "%", "%=", "&", "&&", "&=", "*", "*=", "+", "+=", ",",
            "-", "-=", "/", "/=", "<", "<<", ">>=", "<=", "=", "==", "===",
            ">", ">=", ">>", ">>=", ">>>", ">>>=", "^", "^=", "|", "|=", "||",
            "in", "instanceof"
    };

    private static final String[] LITERAL_TYPES = {
            "int", "boolean", "string", "undefined", "null", "this"
    };

    public JavaScriptRLGenerator() {}
    /**
     * Parameter initialization function
     * @param params:
     *              int stateSize,
     *              int seed (optional)
     *              double defaultEpsilon // TODO: per learner epsilon values
     */
    public void init(RLParams params){
        if (params.exists("seed")){
            oracle = new RLOracle((long) params.get("seed"));
        } else {
            oracle = new RLOracle();
        }
        double e = (double) params.get("defaultEpsilon", true);
        List<Object> ints =  Arrays.asList(RLOracle.range(MIN_INT, MAX_INT+1));
        List<Object> bools = Arrays.asList(BOOLEANS);
        List <Object> ascii = new ArrayList<> (26);
        for (char c = 'A'; c <= 'Z'; c++)
            ascii.add(String.valueOf(c));

        this.stateSize = (int) params.get("stateSize", true);
        this.selectId = oracle.addLearner(null, e);
        this.intId = oracle.addLearner(ints, e);
        this.boolId = oracle.addLearner(bools, e);
        this.chrId = oracle.addLearner(ascii, e);

    }

    /**
     * Generate the next input
     * @return The next input as an InputStream
     */
    public String generate(){
        this.identifiers = new HashSet<>();
        this.statementDepth = 0;
        this.expressionDepth = 0;
        String[] state = new String[stateSize];
        return generateStatement(state);
    }

    /**
     * Update the state of the generator given a reward
     * @param r reward
     */
    public void update(int r) {
        oracle.update(r);
    }


    private String generateExpression(String[] stateArr) {
        stateArr = updateState(stateArr, "node=expression");
        expressionDepth++;
        String result;
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || (Boolean) oracle.select(stateArr, boolId)) {
            String fn = (String) oracle.select(
                    Arrays.asList(EXPRESSIONS_1),
                    stateArr,
                    selectId
            );
            switch (fn){
                case "literal":
                    result = generateLiteralNode(stateArr);
                    break;
                case "ident":
                    result = generateIdentNode(stateArr);
                    break;
                default:
                    throw new Error(fn + " NOT FOUND");
            }

        } else {
            String fn = (String) oracle.select(
                    Arrays.asList(EXPRESSIONS_2),
                    stateArr,
                    selectId
            );
            switch (fn) {
                case "unary":
                    result = generateUnaryNode(stateArr);
                    break;
                case "binary":
                    result = generateBinaryNode(stateArr);
                    break;
                case "ternary":
                    result = generateTernaryNode(stateArr);
                    break;
                case "call":
                    result = generateCallNode(stateArr);
                    break;
                case "function":
                    result = generateFunctionNode(stateArr);
                    break;
                case "property":
                    result = generatePropertylNode(stateArr);
                    break;
                case "index":
                    result = generateIndexNode(stateArr);
                    break;
                case "arrow":
                    result = generateArrowFunctionNode(stateArr);
                    break;
                default:
                    throw new Error(fn + " NOT FOUND");
            }
        }
        expressionDepth--;
        return "(" + result + ")";
    }

    private String generateStatement(String[] stateArr) {
        stateArr = updateState(stateArr, "node=statement");
        statementDepth++;
        String result;
        if (statementDepth >= MAX_STATEMENT_DEPTH || (Boolean) oracle.select(stateArr, boolId)) {
            String fn = (String) oracle.select(
                    Arrays.asList(STATEMENTS_1),
                    stateArr,
                    selectId
            );
            switch (fn){
                case "expression":
                    result = generateExpression(stateArr);
                    break;
                case "break":
                    result = generateBreakNode(stateArr);
                    break;
                case "continue":
                    result = generateContinueNode(stateArr);
                    break;
                case "return":
                    result = generateReturnNode(stateArr);
                    break;
                case "throw":
                    result = generateThrowNode(stateArr);
                    break;
                case "var":
                    result = generateVarNode(stateArr);
                    break;
                case "empty":
                    result = generateEmptyNode(stateArr);
                    break;
                default:
                    throw new Error(fn + " NOT FOUND");

            }
        } else {
            String fn = (String) oracle.select(
                    Arrays.asList(STATEMENTS_2),
                    stateArr,
                    selectId
            );
            switch (fn) {
                case "if":
                    result = generateIfNode(stateArr);
                    break;
                case "for":
                    result = generateForNode(stateArr);
                    break;
                case "while":
                    result = generateWhileNode(stateArr);
                    break;
                case "namedfunc":
                    result = generateNamedFunctionNode(stateArr);
                    break;
                case "switch":
                    result = generateSwitchNode(stateArr);
                    break;
                case "try":
                    result = generateTryNode(stateArr);
                    break;
                case "block":
                    result = generateBlock(stateArr);
                    break;
                default:
                    throw new Error(fn + " NOT FOUND");
            }
        }
        statementDepth--;
        return result;
    }


    private String generateLiteralNode(String[] stateArr) {
        String[] stateBool = updateState(stateArr, "node=literal");
        if (expressionDepth < MAX_EXPRESSION_DEPTH && (Boolean) oracle.select(stateBool, boolId)) {
            stateBool = updateState(stateArr, "branch=1");
            //TODO multiple expressions in brackets
            int numArgs = (int) oracle.select(stateBool, intId);
            if ((Boolean) oracle.select(stateBool, boolId)) {
                return "[" + generateItems(this::generateExpression, stateArr, numArgs) + "]";
            } else {
                return "{" + generateItems(this::generateObjectProperty, stateArr, numArgs) + "}";
            }
        } else {
            String type = (String) oracle.select(
                    Arrays.asList(LITERAL_TYPES),
                    stateArr,
                    selectId
            );
            switch (type){
                case "int":
                    return String.valueOf(oracle.select(stateArr, intId));
                case "boolean":
                    return String.valueOf(oracle.select(stateArr, boolId));
                case "string":
                    Function<String[], String> genChr = s -> (String) oracle.select(s, chrId);
                    return String.join("", generateItems(genChr, stateArr, MAX_STR_LEN));
                default:
                    return type;
            }
        }
    }
    
    private String generateIdentNode(String[] stateArr) {
        String identifier;
        stateArr = updateState(stateArr, "node=ident");
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && (Boolean) oracle.select(stateArr, boolId))) {
            identifier = oracle.select(stateArr, chrId) + "_" + identifiers.size();
            identifiers.add(identifier);
        } else {
            List<Object> identList = new ArrayList<>(identifiers);
            identifier = (String) oracle.select(identList, stateArr, selectId);
        }
        return identifier;
    }

    private String generateUnaryNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=unary");
        String token = (String) oracle.select(
                Arrays.asList(UNARY_TOKENS),
                stateArr,
                selectId
        );
        stateArr = updateState(stateArr, "unary=" + token);
        return token + " " + generateExpression(stateArr);
    }

    private String generateBinaryNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=binary");
        String token = (String) oracle.select(
                Arrays.asList(BINARY_TOKENS),
                stateArr,
                selectId
        );
        stateArr = updateState(stateArr, "binary=" + token);
        String lhs = generateExpression(stateArr);
        String rhs = generateExpression(stateArr);
        return lhs + " " + token + " " + rhs;
    }

    private String generateTernaryNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=ternary");
        return generateExpression(stateArr) + " ? " + generateExpression(stateArr) +
                " : " + generateExpression(stateArr);
    }

    private String generateCallNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=call");
        String func = generateExpression(stateArr);

        stateArr = updateState(stateArr, "func=" + func);
        int numArgs = (int) oracle.select(stateArr, intId);
        String args = String.join(",", generateItems(this::generateExpression, stateArr, numArgs));

        stateArr = updateState(stateArr, "args=" + args);
        String call = func + "(" + args + ")";
        if ((Boolean) oracle.select(stateArr, boolId)) {
            return call;
        } else {
            return "new" + call;
        }
    }

    private String generateFunctionNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=function");
        int numArgs = (int) oracle.select(stateArr, intId);
        return "function(" + String.join(", ", generateItems(this::generateIdentNode, stateArr, numArgs)) + ")"
                + generateBlock(stateArr);
    }

    private String generatePropertylNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=property");
        return generateExpression(stateArr) + "." + generateIdentNode(stateArr);
    }

    private String generateIndexNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=index");
        return generateExpression(stateArr) + "[" + generateExpression(stateArr) + "]";

    }

    private String generateArrowFunctionNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=arrow");
        int numArgs = (int) oracle.select(stateArr, intId);
        String params = "(" + String.join(", ", generateItems(this::generateIdentNode, stateArr, numArgs)) + ")";
        if ((Boolean) oracle.select(stateArr, boolId)) {
            return params + " => " + generateBlock(stateArr);
        } else {
            return params + " => " + generateExpression(stateArr);
        }
    }

    private String generateBlock(String[] stateArr) {
        stateArr = updateState(stateArr, "node=block");
        int numArgs = (int) oracle.select(stateArr, intId);
        return "{ " + String.join(";", generateItems(this::generateStatement, stateArr, numArgs)) + " }";

    }

    private String generateBreakNode(String[] stateArr) {
        return "break";
    }

    private String generateContinueNode(String[] stateArr) {
        return "continue";
    }

    private String generateReturnNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=return");
        return (Boolean) oracle.select(stateArr, boolId) ? "return" : "return " + generateExpression(stateArr);
    }

    private String generateThrowNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=throw");
        return "throw " + generateExpression(stateArr);
    }

    private String generateVarNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=var");
        return "var " + generateIdentNode(stateArr);
    }

    private String generateEmptyNode(String[] stateArr) {
        return "";
    }

    private String generateIfNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=if");
        return "if (" +
                generateExpression(stateArr) + ") " +
                generateBlock(stateArr) +
                ((Boolean) oracle.select(stateArr, boolId) ? generateBlock(stateArr) : "");
    }

    private String generateForNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=for");
        String s = "for(";
        if ((Boolean) oracle.select(stateArr, boolId)) {
            stateArr = updateState(stateArr, "branch=1");
            s += generateExpression(stateArr);
        }
        s += ";";
        if ((Boolean) oracle.select(stateArr, boolId)) {
            stateArr = updateState(stateArr, "branch=2");
            s += generateExpression(stateArr);
        }
        s += ";";
        if ((Boolean) oracle.select(stateArr, boolId)) {
            stateArr = updateState(stateArr, "branch=3");
            s += generateExpression(stateArr);
        }
        s += ")";
        s += generateBlock(stateArr);
        return s;
    }

    private String generateWhileNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=while");
        return "while (" + generateExpression(stateArr) + ")" + generateBlock(stateArr);

    }

    private String generateNamedFunctionNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=namedfunc");
        int numArgs = (int) oracle.select(stateArr, intId);
        return "function " + generateIdentNode(stateArr) + "(" + String.join(", ", generateItems(this::generateIdentNode, stateArr, numArgs)) + ")" + generateBlock(stateArr);
    }

    private String generateSwitchNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=switch");
        int numArgs = (int) oracle.select(stateArr, intId);
        return "switch(" + generateExpression(stateArr) + ") {"
                + String.join(" ", generateItems(this::generateCaseNode, stateArr, numArgs) + "}");
    }

    private String generateTryNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=try");
        return "try " + generateBlock(stateArr) + generateCatchNode(stateArr);

    }

    private String generateCatchNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=catch");
        return "catch (" + generateIdentNode(stateArr) + ") " +
                generateBlock(stateArr);
    }



    private String generateObjectProperty(String[] stateArr) {
        stateArr = updateState(stateArr, "node=property");
        return generateIdentNode(stateArr) + ": " + generateExpression(stateArr);
    }


    private String generateCaseNode(String[] stateArr) {
        stateArr = updateState(stateArr, "node=case");
        return "case " + generateExpression(stateArr) + ": " +  generateBlock(stateArr);
    }


    private <T> List<T> generateItems(Function<String[], T> generator, String[] stateArr, int len) {
        List<T> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            stateArr = updateState(stateArr, "index=" + String.valueOf(i));
            items.add(generator.apply(stateArr));
        }
        return items;
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
