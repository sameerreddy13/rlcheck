package edu.berkeley.cs.jqf.fuzz.rl;

import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by clemieux on 6/27/19.
 */

@RunWith(JUnitQuickcheck.class)
public class RLParamParserTest {

    @Test
    public void testReadJsonIntArray(){
        RLParamParser parser = new RLParamParser();
        RLParams params = parser.getParams("{\"params\" : [{\"type\": \"int_array\", \"name\": \"numc\", \"val\": [1,2,4]}]}");
        List<Integer> numc = (List<Integer>) params.get("numc");
        Integer[] expectedNumc = new Integer[]{1, 2, 4};
        assertArrayEquals(numc.toArray(), expectedNumc);

    }


    @Test
    public void testManyParams(){
        RLParamParser parser = new RLParamParser();
        RLParams params = parser.getParams("{\"params\" : [{\"type\": \"int\", \"name\": \"numc\", \"val\": 1}," +
                "{\"type\": \"string\", \"name\": \"strval\", \"val\": \"hello\"}]}");
        Integer numc = (Integer) params.get("numc");
        String strval = (String) params.get("strval");
        assertEquals(numc, (Integer) 1);
        assertEquals(strval, "hello");

    }

    @Test
    public void testReadJsonStringArray(){
        RLParamParser parser = new RLParamParser();
        RLParams params = parser.getParams("{\"params\" : [{\"type\": \"string_array\", \"name\": \"numv\", \"val\": [\"asd\", \"asda\"]}]}");
        List<String> numv = (List<String>) params.get("numv");
        String[] expectedNumv = new String[]{"asd", "asda"};
        assertArrayEquals(numv.toArray(), expectedNumv);

    }
}
