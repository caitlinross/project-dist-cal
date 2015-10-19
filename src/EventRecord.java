/**
 * @author Caitlin Ross and Erika Mackin
 *
 */

import java.io.Serializable;


public class EventRecord implements Serializable{
	/**
	 * 
	 */
	private String operation;
	private int time;
	private int nodeId;
	private Appointment appointment;
	
	/**
	 * 
	 */
	public EventRecord(String operation, int time, int nodeId) {
		// TODO Auto-generated constructor stub
		this.setOperation(operation);
		this.setTime(time);
		this.setNodeId(nodeId);
	}
	
	public EventRecord(String operation, int time, int nodeId, Appointment appointment) {
		// TODO Auto-generated constructor stub
		this.setOperation(operation);
		this.setTime(time);
		this.setNodeId(nodeId);
		this.setAppointment(appointment);
	}

	/**
	 * @return the operation
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/**
	 * @return the time
	 */
	public int getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(int time) {
		this.time = time;
	}

	/**
	 * @return the nodeId
	 */
	public int getNodeId() {
		return nodeId;
	}

	/**
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the appointment
	 */
	public Appointment getAppointment() {
		return appointment;
	}

	/**
	 * @param appointment the appointment to set
	 */
	public void setAppointment(Appointment appointment) {
		this.appointment = appointment;
	}
	
	

}
