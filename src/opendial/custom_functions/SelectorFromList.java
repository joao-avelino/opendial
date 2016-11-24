// =================================================================                                                                   
/*
* Copyright (c) 2016, Joao Avelino.
*
*
*/
// =================================================================

package opendial.custom_functions.OptionListParser;

import java.util.List;
import java.util.function.Function;

import opendial.bn.values.Value;
import opendial.bn.values.ValueFactory;
import opendial.templates.Template;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SelectorFromList implements Function<List<String>,Value> {
	

    public Value apply(List<String> args) {

        int optionNumber = Integer.parseInt(args.get(0));
        String listOfOptionsString = args.get(1);

        String[] optionsList = listOfOptionsString.split(",");


        if(optionNumber > optionsList.length || optionsList.length < 1 || optionNumber < 0)
        {
            return ValueFactory.create("Unavailable(" + optionsList.length + ")");

        }else if(optionsList[optionNumber-1].length() < 1)
        {
            return ValueFactory.create("Unavailable()");

        }else{

            String toReturn = optionsList[optionNumber-1];


            for(int i = 0; i < toReturn.length(); i++)
            {      
                if(toReturn.charAt(i) != ' ')
                {
                    toReturn = toReturn.substring(i);
                    break;
                }
            }

            return ValueFactory.create(toReturn);
        }

    }
}