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

public class OptionListParser implements Function<List<String>,Value> {
	
	//Function found on stackoverflow posted by: Jaskey
    private static boolean isContain(String source, String subItem){
        String pattern = "\\b"+subItem+"\\b";
        Pattern p=Pattern.compile(pattern);
        Matcher m=p.matcher(source);
        return m.find();
   }
   /*****************************************************************/
    
    private static ArrayList<Double> getMaxIndex(ArrayList<Double> scoreList)
    {
    	ArrayList<Double> scoreOut = new ArrayList<>();
    	// scoreOut(0) -> index
    	// scoreOut(1) -> score
    	
    	double maxi = 0;
    	int index = 0;
    	
    	int i = 0;
    	for(double score : scoreList)
    	{
    		if(score > maxi)
    		{
    			maxi = score;
    			index = i;
    		}
    		
    		i++;
    	}
    	
    	scoreOut.add(new Double(index));
    	scoreOut.add(maxi);
    	
    	return scoreOut;
    	
    }

    public Value apply(List<String> args) {

        String utterance = args.get(0);

        //Temporary. Remove after last version of OpenDial
        if(utterance.equals("{u_u}"))
            utterance = "(None)";
        // -----------------------------------------------
        
        String listOfOptionsString = args.get(1);

        String[] optionsList = listOfOptionsString.split(",");
        String[] uttWordsList = utterance.split("[ ]");
        
        //For each option
        
        ArrayList<Double> scoreList = new ArrayList<>();
        
        for(String opt : optionsList)
        {
        	//Number of nonspace chars on option
        	double numNonSpaceChars = opt.replaceAll(" ", "").length();
        	
        	//Check if each word from the utterance is present on the string. If so, add it's number of chars
        	//as matched chars for score
        	
        	double preScore = 0;
        	
        	for(String utWord : uttWordsList)
        	{
        		if(isContain(opt.toUpperCase(), utWord.toUpperCase()))
        			preScore += utWord.length();
        	}
        	
        	scoreList.add(preScore/numNonSpaceChars);
        }
        
        ArrayList<Double> maximumScore = getMaxIndex(scoreList);
        
        String selectedOption = optionsList[maximumScore.get(0).intValue()];
        
        if(maximumScore.get(1) < 0.25)
        	return ValueFactory.create("Unrecognized");
        else
        	return ValueFactory.create(""+selectedOption);
           
    }
}