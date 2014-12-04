package rexready;

public class ClientPreferences {

	public final int arrival, departure;
	public final int hotelValue;
	public final int e1Value, e2Value, e3Value;

	public ClientPreferences(int arrival, int departure, int hotelValue, int e1Value, int e2Value, int e3Value) {
		this.arrival = arrival;
		this.departure = departure;
		this.hotelValue = hotelValue;
		this.e1Value = e1Value;
		this.e2Value = e2Value;
		this.e3Value = e3Value;
	}

	@Override
	public String toString() {
		String s = "Arrival: " + arrival;
		s += "\nDeparture: " + departure;
		s += "\nHotel Bonus: " + hotelValue;
		s += "Entertainment Bonuses: " + e1Value + ", " + e2Value + ", " + e3Value;
		return s;
	}
	
	public int getUtility(Package pkg) {
		int travelPenalty = 100 * (Math.abs(pkg.getArrivalDate() - arrival) + Math.abs(pkg.getDepartureDate() - departure));
		int hotelBonus = pkg.isGoodHotel() ? hotelValue : 0;
		int funBonus = 0;
		if (pkg.contains(EntertainmentType.ALLIGATOR_WRESTLING)) funBonus += e1Value;
		if (pkg.contains(EntertainmentType.AMUSEMENT)) funBonus += e1Value;
		if (pkg.contains(EntertainmentType.MUSEUM)) funBonus += e1Value;
		return 1000 - travelPenalty + hotelBonus + funBonus;
	}
	
}
