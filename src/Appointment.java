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
	private String sAMPM;
	private String eAMPM;
	private ArrayList<Integer> participants;
	
	// use these indices in the calendar arrays
	private int startIndex;
	private int endIndex;
	
	public Appointment(String name, Day day, int start, int end, String sAMPM, String eAMPM, ArrayList<Integer> participants) {
		this.name = name;
		this.day = day;
		this.start = start;
		this.end = end;
		this.sAMPM = sAMPM;
		this.eAMPM = eAMPM;
		this.participants = participants;
		this.startIndex = convertTime(start, sAMPM);
		this.endIndex = convertTime(end, eAMPM);
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
	
	public static int convertTime(int time, String amPM){
		int index = 0;
		// TODO convert time to appropriate array index
		if (time == 1200){
			if (amPM.equals("AM"))
				index = 0;
			else
				index = 24;
		}
		else if (time == 1230){
			if (amPM.equals("AM"))
				index = 1;
			else
				index = 25;
		}
		else {
			index = time/100*2;
			if (time % 60 == 30)
				index++;
			if (amPM.equals("PM"))
				index += 24;
		}
		return index;
	}
	
}
