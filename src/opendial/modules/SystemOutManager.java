package opendial.modules;

import java.util.logging.*;
import java.util.Collection;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Opendial stuff
import opendial.bn.distribs.CategoricalTable;
import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.modules.Module;



public class SystemOutManager implements Module {

	// logger
	final static Logger log = Logger.getLogger("OpenDial");

	/** the dialogue system */
	DialogueSystem system;

	final BlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<String>();
	
	boolean paused = true;

	class OutputManagerThread extends Thread
	{


		public boolean threadAlive=true;
		int numberTries = 1;

		public void run()
		{
			while(threadAlive)
			{
				//Take this out later. Just for the demo. There must be a bug in the audio module!
				System.out.println("floor on loop: "+floor);

				if(floor.equals("free"))
				{
					try {
					
						String system_utterance = linkedBlockingQueue.take();

						if(floor.equals("free"))
						{
						System.out.println("Taking: "+system_utterance);
						floor="system";
						system.addContent("u_m", system_utterance);
						}else{
							Thread.sleep(1000);
							linkedBlockingQueue.put(system_utterance);
							System.out.println("It's not free");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}else{

					try{

						Thread.sleep(1000);
					}catch(InterruptedException e){
						e.printStackTrace();
					}

				} 
			}
		}
	}

	OutputManagerThread manager;
	volatile String floor;


	public SystemOutManager(DialogueSystem system) {

		this.system = system;


/*		TAKE CARE OF THIS LATER. FOR NOW FIX THE PARAMETER
		List<String> missingParams =
				new LinkedList<String>(Arrays.asList("mongodbURI", "openDialIn", "openDialOut", "databaseName"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + missingParams);
		}*/

	}

	/*Start the management thread*/

	@Override
	public void start() {

		
		manager = new OutputManagerThread();
		
		//Start the manager thread
		manager.start();
		floor = "free";
		paused = false;


	}

	/**
	 *
	 * Check
	 *
	 */
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {


		//Let's check all the updated vars and publish them to the collection.
		//For now it will only send the most probable value

		for(String var : updatedVars)
		{

			if(state.hasChanceNode(var) && !paused && var.equals("preu_m"))
			{
				
				String sm = state.queryProb(var).getBest().toString();

				if(floor.equals("free"))
				{	
					System.out.println("floor: "+floor);
					floor="system";
					System.out.println("sm: "+sm);
					system.addContent("u_m", sm);
				}else{
					try {
					System.out.println("Putting: " + sm);
					linkedBlockingQueue.put(sm);
					} catch (InterruptedException e) {
					e.printStackTrace();
					}
				}

				return;
			}if(state.hasChanceNode(var) && !paused && var.equals("floor"))
			{
				floor = system.getFloor();
				System.out.println("floor update: "+floor);
			}
			

		}
		


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
