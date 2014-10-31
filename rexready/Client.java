package rexready;

public class Client {

	private int arrival, departure, hotel_value, e1, e2, e3;
	
	public Client(int arrival, int departure, int hotel_value, int e1, int e2,int e3) {
		this.arrival = arrival;
		this.departure = departure;
		this.hotel_value = hotel_value;
		this.e1 = e1;
		this.e2 = e2;
		this.e3 = e3;
	}
	
	public int getMaxValue()
	{
		return arrival + departure + hotel_value + e1 + e2 + e3;
	}
}
