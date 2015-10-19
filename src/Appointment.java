/**
 * @author Caitlin Ross and Erika Mackin
 *
 * Object that contains all info for an appointment
 */

import java.io.Serializable;
import java.util.ArrayList;

@SuppressWarnings("serial")
public class Appointment implements Serializable {
	// all fields are serializable
	private String name;
	private Day day;
	private int start;
	private int end;
	private ArrayList<Integer> participants;
	
	public Appointment(String name, Day day, int start, int end, ArrayList<Integer> participants) {
		this.name = name;
		this.day = day;
		this.start = start;
		this.end = end;
		this.participants = participants;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Day getDay() {
		return this.day;
	}

	public void setDay(Day day) {
		this.day = day;
	}

	public int getStart() {
		return this.start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return this.end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public ArrayList<Integer> getParticipants() {
		return this.participants;
	}

	public void setParticipants(ArrayList<Integer> participants) {
		this.participants = participants;
	}
	
	
	
}
