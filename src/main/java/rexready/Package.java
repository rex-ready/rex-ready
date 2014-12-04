package rexready;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

public class Package {
	
	private int arrivalDate, departureDate;
	private boolean goodHotel;
	private final BidiMap<Integer, EntertainmentType> entertainment = new DualHashBidiMap<>();
	
	public Package() {
		this(1, 5, false);
	}
	
	public Package(int arrivalDate, int departureDate, boolean goodHotel) {
		setDepartureDate(departureDate);
		setArrivalDate(arrivalDate);
		this.goodHotel = goodHotel;
	}
	
	public Package(Package old) {
		this(old.arrivalDate, old.departureDate, old.goodHotel);
		entertainment.putAll(old.entertainment);
	}

	public int getArrivalDate() {
		return arrivalDate;
	}
	
	public void setArrivalDate(int arrivalDate) {
		if (arrivalDate < 1 || arrivalDate > 4) {
			throw new IllegalArgumentException("arrivalDate is out of range");
		}
		if (arrivalDate >= departureDate) {
			throw new IllegalArgumentException("arrivalDate is not before departureDate");
		}
		for (int i = 1; i < arrivalDate; ++i) {
			entertainment.remove(i);
		}
		this.arrivalDate = arrivalDate;
	}
	
	public int getDepartureDate() {
		return departureDate;
	}
	
	public void setDepartureDate(int departureDate) {
		if (departureDate < 2 || departureDate > 5) {
			throw new IllegalArgumentException("departureDate is out of range");
		}
		if (departureDate <= arrivalDate) {
			throw new IllegalArgumentException("departureDate is not after arrivalDate");
		}
		for (int i = departureDate; i < 5; ++i) {
			entertainment.remove(i);
		}
		this.departureDate = departureDate;
	}
	
	public boolean isGoodHotel() {
		return goodHotel;
	}
	
	public void setGoodHotel(boolean goodHotel) {
		this.goodHotel = goodHotel;
	}
	
	public void setEntertainment(int date, EntertainmentType type) {
		if (date < arrivalDate || date >= departureDate) {
			throw new IllegalArgumentException("date is out of range");
		}
		entertainment.put(date, type);
	}
	
	public boolean contains(EntertainmentType type) {
		return entertainment.containsValue(type);
	}
	
}
