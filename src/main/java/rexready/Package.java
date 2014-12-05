package rexready;

import java.util.Random;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Package {
	
	private int arrivalDate, departureDate;
	private boolean goodHotel;
	private final BidiMap<Integer, EntertainmentType> entertainment = new DualHashBidiMap<>();
	
	public Package() {
		this(1, 5, false);
	}
	
	public Package(ClientPreferences client) {
		this(client.arrival, client.departure, client.hotelValue > 100);
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
		if (type == null) {
			entertainment.remove(date);
		}
		else {
			entertainment.put(date, type);
		}
	}
	
	public boolean contains(EntertainmentType type) {
		return entertainment.containsValue(type);
	}
	
	public void mutate(Random r, float mutationRate) {
		if (r.nextFloat() < mutationRate) {
			setArrivalDate(r.nextInt(getDepartureDate() - 1) + 1);
		}
		if (r.nextFloat() < mutationRate) {
			setDepartureDate(r.nextInt(5 - getArrivalDate()) + getArrivalDate() + 1);
		}
		if (r.nextFloat() < mutationRate) {
			setGoodHotel(r.nextBoolean());
		}
		for (int day = getArrivalDate(); day < getDepartureDate(); ++day) {
			setEntertainment(day, null);
			if (r.nextFloat() > 0.75f) {
				setEntertainment(day, EntertainmentType.values()[r.nextInt(EntertainmentType.values().length)]);
			}
		}
	}

	public GoodsList getShoppingList() {
		GoodsList result = new GoodsList();

		if (arrivalDate == 1) result.setAmount(Good.INFLIGHT_1, 1);
		if (arrivalDate == 2) result.setAmount(Good.INFLIGHT_2, 1);
		if (arrivalDate == 3) result.setAmount(Good.INFLIGHT_3, 1);
		if (arrivalDate == 4) result.setAmount(Good.INFLIGHT_4, 1);

		if (departureDate == 2) result.setAmount(Good.OUTFLIGHT_2, 1);
		if (departureDate == 3) result.setAmount(Good.OUTFLIGHT_3, 1);
		if (departureDate == 4) result.setAmount(Good.OUTFLIGHT_4, 1);
		if (departureDate == 5) result.setAmount(Good.OUTFLIGHT_5, 1);
		
		if (goodHotel) {
			if (arrivalDate <= 1 && departureDate > 1) result.setAmount(Good.GOOD_HOTEL_1, 1);
			if (arrivalDate <= 2 && departureDate > 2) result.setAmount(Good.GOOD_HOTEL_2, 1);
			if (arrivalDate <= 3 && departureDate > 3) result.setAmount(Good.GOOD_HOTEL_3, 1);
			if (arrivalDate <= 4 && departureDate > 4) result.setAmount(Good.GOOD_HOTEL_4, 1);
		}
		else {
			if (arrivalDate <= 1 && departureDate > 1) result.setAmount(Good.CHEAP_HOTEL_1, 1);
			if (arrivalDate <= 2 && departureDate > 2) result.setAmount(Good.CHEAP_HOTEL_2, 1);
			if (arrivalDate <= 3 && departureDate > 3) result.setAmount(Good.CHEAP_HOTEL_3, 1);
			if (arrivalDate <= 4 && departureDate > 4) result.setAmount(Good.CHEAP_HOTEL_4, 1);
		}
		
		if (entertainment.get(1) == EntertainmentType.ALLIGATOR_WRESTLING) result.setAmount(Good.ALLIGATOR_1, 1);
		if (entertainment.get(2) == EntertainmentType.ALLIGATOR_WRESTLING) result.setAmount(Good.ALLIGATOR_2, 1);
		if (entertainment.get(3) == EntertainmentType.ALLIGATOR_WRESTLING) result.setAmount(Good.ALLIGATOR_3, 1);
		if (entertainment.get(4) == EntertainmentType.ALLIGATOR_WRESTLING) result.setAmount(Good.ALLIGATOR_4, 1);

		if (entertainment.get(1) == EntertainmentType.AMUSEMENT) result.setAmount(Good.AMUSEMENT_1, 1);
		if (entertainment.get(2) == EntertainmentType.AMUSEMENT) result.setAmount(Good.AMUSEMENT_2, 1);
		if (entertainment.get(3) == EntertainmentType.AMUSEMENT) result.setAmount(Good.AMUSEMENT_3, 1);
		if (entertainment.get(4) == EntertainmentType.AMUSEMENT) result.setAmount(Good.AMUSEMENT_4, 1);

		if (entertainment.get(1) == EntertainmentType.MUSEUM) result.setAmount(Good.MUSEUM_1, 1);
		if (entertainment.get(2) == EntertainmentType.MUSEUM) result.setAmount(Good.MUSEUM_2, 1);
		if (entertainment.get(3) == EntertainmentType.MUSEUM) result.setAmount(Good.MUSEUM_3, 1);
		if (entertainment.get(4) == EntertainmentType.MUSEUM) result.setAmount(Good.MUSEUM_4, 1);
		
		return result;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
}
