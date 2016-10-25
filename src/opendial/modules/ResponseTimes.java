// =================================================================                                                                   
/*
* Copyright (c) 2016, Joao Avelino.
*
*
*/
// =================================================================    

package opendial.modules;

import java.util.logging.*;
import java.util.Collection;
import opendial.DialogueState;
import opendial.DialogueSystem;
import opendial.modules.Module;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.Timer;

//Time Monitor

public class ResponseTimes implements Module {
	
	boolean paused = true;
	
	DialogueSystem system;
	String varSystem;
	String varUser;
	
	// logger
	final static Logger log = Logger.getLogger("OpenDial");
	
	boolean timeOutEnabled = false;
	double seconds;
	
	double lastMachineActionTime;
	double deltaT;
	double nTimeOuts = 0;
	
	Timer timer;
	
	class CheckTimeout extends TimerTask {
		
			
        public void run() {

			if(timeOutEnabled)
			{
				deltaT = System.currentTimeMillis()-lastMachineActionTime;
					
				if(deltaT/1000 > seconds*(nTimeOuts+1))
				{
					nTimeOuts++;
					system.addContent("nTimeOuts", nTimeOuts);
					system.addContent("delta_t", deltaT/1000);
				}					
			}
			else
			{
				timer.cancel();
				timer.purge();
			}
        }
	}

	public ResponseTimes(DialogueSystem system) {
		
		this.system = system;
		List<String> missingParams =
				new LinkedList<String>(Arrays.asList("userVar", "systemVar"));
		missingParams.removeAll(system.getSettings().params.keySet());
		if (!missingParams.isEmpty()) {
			throw new RuntimeException("Missing parameters: " + missingParams);
		}
		
		varSystem = system.getSettings().params.getProperty("systemVar");
		varUser = system.getSettings().params.getProperty("userVar");
		
		lastMachineActionTime = System.currentTimeMillis();
	}

	
	@Override
	public void start() {
		
		
		paused = false;
	}

	
	@Override
	public void trigger(DialogueState state, Collection<String> updatedVars) {
		
		
		//Measure time of user response to system actions
		
		//Start counting: get time when the machine acts
		if (updatedVars.contains(varSystem) && state.hasChanceNode(varSystem) && !paused) {
			String systemAction = state.queryProb(varSystem).getBest().toString();
			
			String userUtterance = state.queryProb(varUser).getBest().toString();

			
			if(!systemAction.equals(""))
			{
			lastMachineActionTime = System.currentTimeMillis();
			
			nTimeOuts = 0;
			system.addContent("nTimeOuts", 0);
			system.addContent("delta_t", 0);
			}


		}
		
		//Measure time: get time when the user acts
		if (updatedVars.contains(varUser) && state.hasChanceNode(varUser) && !paused) {
			
			String userUtterance = state.queryProb(varUser).getBest().toString();
			
			if(!userUtterance.equals(""))
			{
				double now = System.currentTimeMillis();
				deltaT = now-lastMachineActionTime;

				system.addContent("delta_t", deltaT/1000);
				
				//If timeout is active, reset it
				
				if(timeOutEnabled)
				{
					//reset deltaT and nTimeOuts to reset the timer
					lastMachineActionTime = System.currentTimeMillis();
					nTimeOuts = 0;
					system.addContent("nTimeOuts", 0);
					system.addContent("delta_t", 0);
				}

				
			}
		}
		
		//Enable or disable timeout warning
		if(updatedVars.contains("timeoutOpts") && state.hasChanceNode("timeoutOpts") && !paused)
		{
			String timeOutValue = state.queryProb("timeoutOpts").getBest().toString();
			
			if(!timeOutValue.equals(""))
			{
				//Enable timeout counter
				if(!timeOutValue.equals("disabled"))
				{
					try
					{
						seconds = Double.parseDouble(timeOutValue);
						
						if(seconds < 0)
							log.warning("number of seconds is negative!");
					
						//If we are activating the timeout, deltaT is reseted
						
						lastMachineActionTime = System.currentTimeMillis();
						nTimeOuts = 0;
						system.addContent("nTimeOuts", 0);
						system.addContent("delta_t", 0);
						
						//If the timeout was not enabled before create a new CheckTimout. Otherwise just reset deltaT and nTimeOuts
						if(!timeOutEnabled)
						{
						timer = new Timer(true);
						timer.scheduleAtFixedRate(new CheckTimeout(), 0, 100);
						timeOutEnabled = true;							
						}

						
					}
					catch(NumberFormatException e)
					{
						log.warning("could not define timeout value (seconds): " + e);
					}
					
				}
				//Disable timeout counter
				else
				{
					if(!timeOutEnabled)
						log.warning("tried to disable timeout that is not currently enabled");
					else
					{
						timer.cancel();
						timer.purge();
						timeOutEnabled = false;
					}
				}
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