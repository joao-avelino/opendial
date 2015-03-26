// =================================================================                                                                   
// Copyright (C) 2011-2015 Pierre Lison (plison@ifi.uio.no)
                                                                            
// Permission is hereby granted, free of charge, to any person 
// obtaining a copy of this software and associated documentation 
// files (the "Software"), to deal in the Software without restriction, 
// including without limitation the rights to use, copy, modify, merge, 
// publish, distribute, sublicense, and/or sell copies of the Software, 
// and to permit persons to whom the Software is furnished to do so, 
// subject to the following conditions:

// The above copyright notice and this permission notice shall be 
// included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
// SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// =================================================================                                                                   

package opendial.plugins;

import java.util.ArrayList;
import java.util.List;

import org.maltparser.concurrent.graph.ConcurrentDependencyGraph;
import org.maltparser.concurrent.graph.ConcurrentDependencyNode;

import opendial.arch.DialException;
import opendial.arch.Logger;
import opendial.bn.values.StringVal;
import opendial.bn.values.Value;


/**
 * Representation of a dependency parse of a particular utterance.  The
 * parse contains the sequence of words (after tokenisation) together with
 * their POS tags and labelled relation to their head.
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 */
public class ParseValue implements Value {

	// logger
	public static Logger log = new Logger("ParseValue", Logger.Level.DEBUG);

	/**
	 * The list of words together with their POS-tags and dependencies.
	 */
	List<FactoredWord> parsedInput;
	
	/**
	 * Constructs a new parse value from a dependency graph generated by the
	 * MaltParser.
	 * 
	 * @param graph the dependency graph from the MaltParser.
	 */
	public ParseValue(ConcurrentDependencyGraph graph) {
		parsedInput = new ArrayList<FactoredWord>();
		for (int i = 1 ; i < graph.nTokenNodes() ; i++) {
			ConcurrentDependencyNode node = graph.getTokenNode(i);
			String word = node.getLabel(1);
			String posTag = node.getLabel(3);
			int head = node.getHeadIndex();
			String headRelation = node.getLabel(7);
			FactoredWord fw = new FactoredWord(word, posTag, head, headRelation);
			parsedInput.add(fw);
		}
	}
	
	/**
	 * Copies the ParseValue
	 * 
	 * @param parsedInput the existing list of factored words
	 */
	public ParseValue(List<FactoredWord> parsedInput) {
		this.parsedInput = new ArrayList<FactoredWord>(parsedInput);		
	}
	
	/**
	 * Returns the hashcode for the parse value.
	 */
	@Override
	public int hashCode() {
		return parsedInput.hashCode();
	}
	
	/**
	 * Returns a string representation of the parse
	 */
	@Override
	public String toString() {
		String str = "";
		for (FactoredWord fw : parsedInput) {
			if (fw.head > 0) {
				String head = parsedInput.get(fw.head-1).word;
				str += "("+head+","+fw.headRelation+","+fw.word+"),";
			}
		}
		return str.substring(0, str.length()-1);
	}
	
	/**
	 * Returns true if the object o is a parse with identical elements,
	 * and false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ParseValue) {
			return ((ParseValue)o).parsedInput.equals(parsedInput);
		}
		return false;
	}
	
	/**
	 * Compares the hashcodes of the two parse values.
	 */
	@Override
	public int compareTo(Value o) {
		return hashCode() - o.hashCode();
	}

	/**
	 * Copies the parse value.
	 */
	@Override
	public Value copy() {
		return new ParseValue(parsedInput);
	}

	
	/**
	 * Returns true if the subvalue is a string of format "(x,y,z)" and there is
	 * a dependency relation between the words x and z with label y in the parse.
	 */
	@Override
	public boolean contains(Value subvalue) {
		if (subvalue instanceof StringVal) {
			String str = ((StringVal)subvalue).getString().trim();
			if (str.startsWith("(") && str.split(",").length == 3 && str.endsWith(")")) {
				str = str.substring(1, str.length()-1);
				String[] split = str.split(",");
				String head = split[0].trim();
				String relation = split[1].trim();
				String dependent = split[2].trim();
				return parsedInput.stream()
				.filter(w -> dependent.equalsIgnoreCase(w.word))
				.filter(w -> relation.equalsIgnoreCase(w.headRelation))
				.filter(w -> head.equalsIgnoreCase(parsedInput.get(w.head-1).word))
				.findFirst().isPresent();
			}
		}
		return false;
	}

	/**
	 * If value is a parsevalue, concatenates the parse.  Else, returns
	 * the current value.
	 */
	@Override
	public Value concatenate(Value value) throws DialException {
		List<FactoredWord> concat = new ArrayList<FactoredWord>(parsedInput);
		if (value instanceof ParseValue) {
			concat.addAll(((ParseValue)value).parsedInput);
		}
		else {
			log.warning("cannot concatenate " + toString() + " and " + value);
		}
		return new ParseValue(concat);
	}
	
	
	
	/**
	 * Representation of a word together with its POS-tag, index to its head,
	 * and labelled relation to the head.
	 *
	 */
	final class FactoredWord {
		
		String word;
		String posTag;
		int head;
		String headRelation;
		
		/**
		 * Creates a new factored word
		 * @param word the word
		 * @param posTag the POS-tag
		 * @param head the index to the head (0 if root)
		 * @param headRelation relation label with the head (null if root)
		 */
		public FactoredWord(String word, String posTag, int head, String headRelation) {
			this.word = word;
			this.posTag = posTag;
			this.head = head;
			this.headRelation = headRelation;
		}
		
		/**
		 * Returns a string representation of the factored word
		 */
		@Override
		public String toString() {
			return word + "_" + posTag + "_" + head + "_" + headRelation;
		}
		
		/**
		 * Returns the hashcode for the factored word
		 */
		@Override
		public int hashCode() {
			return (word.hashCode() - posTag.hashCode() + head - headRelation.hashCode());
		}
		
		/**
		 * Returns true if o is a factored word with identical content, and false otherwise.
		 */
		@Override
		public boolean equals(Object o) {
			if (o instanceof FactoredWord) {
				FactoredWord fw = (FactoredWord)o;
				return (fw.word.equals(word) && fw.posTag.equals(posTag) 
						&& fw.head == head && fw.headRelation.equals(headRelation));
			}
			return false;
		}
	}

}