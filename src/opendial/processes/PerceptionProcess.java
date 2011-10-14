// =================================================================                                                                   
// Copyright (C) 2011-2013 Pierre Lison (plison@ifi.uio.no)                                                                            
//                                                                                                                                     
// This library is free software; you can redistribute it and/or                                                                       
// modify it under the terms of the GNU Lesser General Public License                                                                  
// as published by the Free Software Foundation; either version 2.1 of                                                                 
// the License, or (at your option) any later version.                                                                                 
//                                                                                                                                     
// This library is distributed in the hope that it will be useful, but                                                                 
// WITHOUT ANY WARRANTY; without even the implied warranty of                                                                          
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU                                                                    
// Lesser General Public License for more details.                                                                                     
//                                                                                                                                     
// You should have received a copy of the GNU Lesser General Public                                                                    
// License along with this program; if not, write to the Free Software                                                                 
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA                                                                           
// 02111-1307, USA.                                                                                                                    
// =================================================================                                                                   

package opendial.processes;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import opendial.arch.DialogueInterface;
import opendial.domains.Domain;
import opendial.inputs.Observation;
import opendial.state.DialogueState;
import opendial.utils.Logger;

/**
 * A perception process (thread), responsible for updating the dialogue state
 * upon arrival of new observations.
 *
 * <p>More precisely, the perception process works as such: <ul>
 * <li> the process waits for a new observation to appear on its queue;
 * <li> when such observation arrives, the process updates the dialogue state
 * based on the new information;
 * <li> when the observation queue is emptied, the thread goes back to sleep. </li>
 * 
 * @author  Pierre Lison (plison@ifi.uio.no)
 * @version $Date::                      $
 *
 */
public class PerceptionProcess extends Thread {

	// logger
	static Logger log = new Logger("PerceptionProcess", Logger.Level.DEBUG);
	
	// the dialogue state
	DialogueState state;
	
	// the dialogue domain
	Domain domain;
	
	// the observations to process
	Queue<Observation> observationsToProcess;

	List<DialogueInterface> interfaces;
	
	/**
	 * Starts a new perception process, with a given dialogue state and domain
	 * 
	 * @param state the dialogue state
	 * @param domain the dialogue domain
	 */
	public PerceptionProcess(DialogueState state, Domain domain) {
		this.state = state;
		this.domain = domain;
		
		interfaces = new LinkedList<DialogueInterface>();
		
		observationsToProcess = new LinkedList<Observation>();
	}

	
	/**
	 * Adds a new observation to the process queue
	 * 
	 * @param obs the next observation
	 */
	public synchronized void addObservation(Observation obs) {
		observationsToProcess.add(obs);
		
		log.info("New observation perceived!");
		informInterfaces(obs);
		
		notify();
	}
	
	
	/**
	 * Inform the dialogue interfaces listening to the process of the new
	 * observation which has been received.
	 * 
	 * @param obs the new observation
	 */
	private void informInterfaces(Observation obs) {
		log.debug("number of interfaces: " + interfaces.size());
		for (DialogueInterface interface1 : interfaces) {
			interface1.showObservation(obs);
		}
	}


	/**
	 * Runs the update loop, by polling a new observation and updating
	 * the dialogue state with the information it contains, using the
	 * models defined in the domain.  
	 * 
	 * <p>When the queue becomes empty, the process goes to sleep.  
	 */
	@Override
	public void run () {
		while (true) {		
		           
				// the new observation to process
				Observation newObs = observationsToProcess.poll();
				
				while (newObs != null) {
					
					// performing dialogue update
					log.info("--> Initiate dialogue update with the observation: " + newObs);
					updateState(newObs);
					log.info("--> Dialogue update complete");
					
					// polling another observation
					newObs = observationsToProcess.poll();
				}

				// waiting for another observation to appear
				synchronized (this) {
				try { wait();  }
				catch (InterruptedException e) {  }
			}
		}
	}

	
	/**
	 * Updates the dialogue state given the new observation
	 * TODO: implement dialogue state update
	 * 
	 * @param obs the observation to process
	 */
	private synchronized void updateState(Observation obs) {
		state.dummyChange();
	}


	/**
	 * Adds a new dialogue interface listening to observations received
	 * by the process.
	 * 
	 * @param interface1 the dialogue interface
	 */
	public void addInterface(DialogueInterface interface1) {
		interfaces.add(interface1);
	}


}
