package rexready;

public class Client {

	private int arrivalDate, departureDate, hotel_value, e1, e2, e3;
	
	public Client(int arrival, int departure, int hotel_value, int e1, int e2,int e3) {
		this.arrivalDate = arrival;
		this.departureDate = departure;
		this.hotel_value = hotel_value;
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
	}
	
	public int travelPenalty(TravelPackage travelPackage) {
		return 100 * (Math.abs(travelPackage.arrivalDate - arrivalDate) + Math.abs(travelPackage.departureDate - departureDate));
	}
	
	public int hotelBonus(TravelPackage travelPackage) {
		return travelPackage.tampaTowers ? hotel_value : 0;
	}
	
	public int funBonus(EntertainmentPackage entertainmentPackage) {
		boolean[] includes = entertainmentPackage.includesEntertainmentTypes();
		return (includes[0] ? e1 : 0) +
				(includes[1] ? e2 : 0) +
				(includes[2] ? e3 : 0);
	}
	
	public int utility(TravelPackage travelPackage, EntertainmentPackage entertainmentPackage) {
		return 1000 - travelPenalty(travelPackage) + hotelBonus(travelPackage) + funBonus(entertainmentPackage);
	}
	
	public int getMaxValue() {
		return getMaxTravelValue() + getMaxEntertainmentValue();
	}
	
	public int getMaxTravelValue() {
		return 1000 + hotel_value;
	}
	
	public int getMaxEntertainmentValue() {
		return e1 + e2 + e3;
	}
}
