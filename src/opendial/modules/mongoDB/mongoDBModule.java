// =================================================================                                                                   
/*
* Copyright (c) 2016, Joao Avelino.
*
*
*/
// =================================================================                                                                   

package opendial.modules.mongoDBModules;

import java.util.logging.*;
import java.util.Collection;
import com.mongodb.Mongo;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import com.mongodb.CursorType;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.MongoException;

// Java
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// Opendial stuff
import opendial.bn.distribs.CategoricalTable;
import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.modules.Module;



public class mongoDBModule implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");
	MongoCollection<Document> collection;
	
	/** the dialogue system */
	DialogueSystem system;

	// the in and out topics
	String openDialIn;
	String openDialOut;
	String databaseName;
	
	boolean paused = true;

	class openDialInSubscriber extends Thread
	{


		public boolean threadAlive=true;
		public static final int numberTriesBetweenWarnings = 100;
		int numberTries = 1;

		public void run()
		{
			while(threadAlive)
			{

				MongoCursor<Document> cursor = collection.find().cursorType(CursorType.TailableAwait).noCursorTimeout(true).iterator();

				try {
				    while (cursor.hasNext()) {
				        
				        Document doc = cursor.next();

				        //Get the variable name
				        String varName;
				        if(doc.containsKey("varName"))
				        {
				        	varName = doc.get("varName").toString();
				        }else{
				        	log.warning("Received badly structured data... I could not find the varName field...");
				       		continue;
				        }


				        @SuppressWarnings("unchecked")
				        List<Document> nBestList = (List<Document>) doc.get("nBestList");

				       	if(nBestList == null)
				       	{
				       		log.warning("Received badly structured data... I could not find the nBestList field...");
				       		continue;
				       	}

				       	
				       	//Let's build the distribution table
				       	CategoricalTable.Builder builder = new CategoricalTable.Builder(varName);

				       	for(Document element : nBestList)
				       	{
				       		String value;
				       		String probString;
				       		Double prob;

				       		//Check if values and probability fields exist and process them.

				  			if(element.containsKey("value"))
				  			{
				  				value = element.get("value").toString();
				  			}else{

				       			log.warning("Received badly structured data... I could not find the value field...");
				       			continue;				  				
				  			}

				  			if(element.containsKey("prob"))
				  			{
				  				probString = element.get("prob").toString();
				  				prob = Double.parseDouble(probString);
				  			}else{
				  				prob = 1.0;
				  				log.info("Cant find probability associated with an event. Assuming 1.0. The distribution will then be renormalized");
				  			}

				  			builder.addRow(value, prob);

				       		System.out.println(varName +": " + value + "(" + prob.toString()+")");

				       	}

				       	system.addContent(builder.build());

				       	// Normalize the probabilities if they sum up to more than 1. If less, openDial will take care of it.

				       	/*CODE*/

				        //Let's update the state variables with the received values
				       /* int i = 0;
				        for(String key : keys)
				        {

				        	//Useless
				       		Map<String, Double> nBestList = new HashMap<String, Double>();
				       		nBestList.put(values.get(i).toString(), 0.3);
				       		//*******
				        	
				        	CategoricalTable.Builder builder = new CategoricalTable.Builder(key);

				        	for(String input : nBestList.keySet())
				        	{

				        		builder.addRow(input, nBestList.get(input));
				        	}
				        	system.addContent(builder.build());
				        	i++;
				        }*/
				        				        

				    }
				} finally {
				    cursor.close();

				    if(numberTries%numberTriesBetweenWarnings == 0)
				    {
				    	log.warning("No data received yet");

				    	if(numberTries > numberTriesBetweenWarnings)
				    		numberTries = 1;
				    }

				    numberTries++;
				    
				}

				try{

					Thread.sleep(100);
				}catch(InterruptedException e){

					log.warning("Subscriber error: " + e);
				}
			}
		}
	}

	openDialInSubscriber sub;


	public mongoDBModule(DialogueSystem system) {

		this.system = system;

		List<String> missingParams =
				new LinkedList<String>(Arrays.asList("mongodbURI", "openDialIn", "openDialOut", "databaseName"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + missingParams);
		}

	}

	@Override
	public void start() {

	/*
	*  Connect to a mongoDB database so that we can subscribe and publish events
	*	
	*/

		String textUri = system.getSettings().params.getProperty("mongodbURI");
		openDialIn = system.getSettings().params.getProperty("openDialIn");
		openDialOut = system.getSettings().params.getProperty("openDialOut");
		databaseName = system.getSettings().params.getProperty("databaseName");

		MongoClientURI uri = new MongoClientURI(textUri);

		MongoClient mongoClient = new MongoClient(uri);

		mongoClient.getDatabase(databaseName).getCollection(openDialIn).drop();
		mongoClient.getDatabase(databaseName).createCollection(openDialIn, new CreateCollectionOptions().capped(true).sizeInBytes(0x100000));	
		collection = mongoClient.getDatabase(databaseName).getCollection(openDialIn);

		
		sub = new openDialInSubscriber();
		
		//Start the subscriber thread
		sub.start();
		
		paused = false;


	}

	/**
	 *
	 *
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {



		


	}


	/**
	 * Pauses the module.
	 */
	@Override
	public void pause(boolean toPause) {
		paused = toPause;
	}

	/**
	 * Returns true is the module is not paused, and false otherwise.
	 */
	@Override
	public boolean isRunning() {
		return !paused;
	}

}
