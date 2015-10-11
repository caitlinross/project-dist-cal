/**
 * 
 */

/**
 * @author Caitlin Ross and Erika Mackin
 *
 */
import java.util.ArrayList;

public class Appointment {

	private String name;
	private Day day;
	private int start;
	private int end;
	private ArrayList<Integer> participants;
	
	protected enum Day{
		SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
	}
	
	public Appointment(String name, Day day, int start, int end, ArrayList<Integer> participants) {
		// TODO Auto-generated constructor stub
		this.name = name;
		this.setDay(day);
		this.setStart(start);
		this.setEnd(end);
		this.setParticipants(new ArrayList<Integer>());	
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public Day getDay() {
		return day;
	}

	public void setDay(Day day) {
		this.day = day;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public ArrayList<Integer> getParticipants() {
		return participants;
	}

	public void setParticipants(ArrayList<Integer> participants) {
		this.participants = participants;
	}
	
	
	
}
