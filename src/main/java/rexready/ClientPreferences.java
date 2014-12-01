package rexready;

public class ClientPreferences {

	private int arrival, departure;
	private int hotelValue;
	private int e1Value, e2Value, e3Value;

	public ClientPreferences(int arrival, int departure, int hotelValue, int e1Value, int e2Value, int e3Value) {
		this.arrival = arrival;
		this.departure = departure;
		this.hotelValue = hotelValue;
		this.e1Value = e1Value;
		this.e2Value = e2Value;
		this.e3Value = e3Value;
	}

	public int getArrival() {
		return arrival;
	}

	public int getDeparture() {
		return departure;
	}

	public int getHotelValue() {
		return hotelValue;
	}

	public int getE1Value() {
		return e1Value;
	}

	public int getE2Value() {
		return e2Value;
	}

	public int getE3Value() {
		return e3Value;
	}

	@Override
	public String toString() {
		String s = "Arrival: " + arrival;
		s += "\nDeparture: " + departure;
		s += "\nHotel Bonus: " + hotelValue;
		s += "Entertainment Bonuses: " + e1Value + ", " + e2Value + ", " + e3Value;
		return s;
	}
}
