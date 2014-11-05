package rexready;

public class Client {

    private Preferences preferences;

    public Client(int arrival, int departure, int hotel_value, int e1, int e2, int e3) {
	preferences = new Preferences(arrival, departure, hotel_value, e1, e2, e3);
    }

    public int travelPenalty(TravelPackage travelPackage) {
	return 100 * (Math.abs(travelPackage.getArrivalDate() - preferences.getArrival()) + Math.abs(travelPackage.getDepartureDate() - preferences.getDeparture()));
    }

    public int hotelBonus(TravelPackage travelPackage) {
	return travelPackage.includesTampaTowers() ? preferences.getHotelValue() : 0;
    }

    public int funBonus(EntertainmentPackage entertainmentPackage) {
	boolean[] includes = entertainmentPackage.includesEntertainmentTypes();
	return (includes[0] ? preferences.getE1Value() : 0)
		+ (includes[1] ? preferences.getE2Value() : 0)
		+ (includes[2] ? preferences.getE2Value() : 0);
    }

    public int utility(TravelPackage travelPackage, EntertainmentPackage entertainmentPackage) {
	return 1000 - travelPenalty(travelPackage) + hotelBonus(travelPackage) + funBonus(entertainmentPackage);
    }

    public int getMaxValue() {
	return getMaxTravelValue() + getMaxEntertainmentValue();
    }

    public int getMaxTravelValue() {
	return 1000 + preferences.getHotelValue();
    }

    public int getMaxEntertainmentValue() {
	return preferences.getE1Value() + preferences.getE2Value() + preferences.getE3Value();
    }
    
    public Preferences getPreferences() {
	return preferences;
    }
}
