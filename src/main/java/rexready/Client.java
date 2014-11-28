package rexready;

public class Client {

    private Preferences preferences;

    public Client(int arrival, int departure, int hotel_value, int e1, int e2, int e3) {
	preferences = new Preferences(arrival, departure, hotel_value, e1, e2, e3);
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
