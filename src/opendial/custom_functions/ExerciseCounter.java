// =================================================================                                                                   
/*
* Copyright (c) 2016, Joao Avelino.
*
*
*/
// =================================================================

package opendial.custom_functions;

import java.util.List;
import java.util.function.Function;
import java.util.Random;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.templates.Template;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ExerciseCounter implements Function<List<String>,Value> {


    public Value apply(List<String> args) {

    	String curCounterStr = args.get(0);
    	String maxCounterStr = args.get(1);

    	int curCounter = Integer.parseInt(curCounterStr);
    	int maxCounter = Integer.parseInt(maxCounterStr);

    	if(maxCounter-curCounter == 1)
    	{
    		return ValueFactory.create("count(oneMore)");

    	}else if(curCounter*100/maxCounter == 30)
    	{

   			return ValueFactory.create("count(started)");

    	}else if(curCounter*100/maxCounter == 50)
    	{
    		return ValueFactory.create("count(halfway)");

    	}else if(curCounter*100/maxCounter == 70)
    	{
    		return ValueFactory.create("count(almost)");
    	}else if(curCounter*100/maxCounter == 100)
    	{
    		return ValueFactory.create("count(done)");
    	}


    	return ValueFactory.create("");

    }
}