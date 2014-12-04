package rexready;

import java.awt.Image;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

import com.googlecode.charts4j.AxisLabels;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.AxisStyle;
import com.googlecode.charts4j.AxisTextAlignment;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataUtil;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.LinearGradientFill;
import com.googlecode.charts4j.Plots;

public class BasicAgent extends AgentImpl {

	private ClientPreferences[] clientPreferences = new ClientPreferences[8];
	boolean[] clientInTT;
	private float[] bidValues = new float[TACAgent.getAuctionNo()];

	private Map<String, List<Float>> askPrices = new HashMap<String, List<Float>>();
	private Map<String, List<Float>> predictedMinimumFlightPrices = new HashMap<String, List<Float>>();
	private Map<String, List<Float>> bidPrices = new HashMap<String, List<Float>>();

	private JFrame graphFrame;
	private JLabel graphLabel;

	private FlightPricePredictor flightPredictor = new FlightPricePredictor();
	private HotelPricePredictor hotelPredictor = new HotelPricePredictor();

	@Override
	protected void init(ArgEnumerator args) {
		System.err.println("Agent start");

		for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
			askPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
			bidPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
			if (i < 8) {
				predictedMinimumFlightPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
			}
		}
	}

	@Override
	public void gameStarted() {
		System.err.println("Game start");

		graphFrame = new JFrame();
		graphFrame.setSize(600, 450);
		graphFrame.setVisible(true);
		graphLabel = new JLabel();
		graphFrame.add(graphLabel);

		// hotel price prediction
		new Thread(new Runnable() {
			public void run() {
				DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
				Date date = new Date();
				File hotelPredictionFile = new File(dateFormat.format(date) + "hotelPredictions.txt");
				try {
					if (!hotelPredictionFile.exists()) {
						hotelPredictionFile.createNewFile();
					}
					FileWriter fwHotels = new FileWriter(hotelPredictionFile.getAbsoluteFile());
					BufferedWriter bwHotels = new BufferedWriter(fwHotels);
				
					while (agent.getGameTime() < 65000) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					while (agent.getGameTimeLeft() > 0) {
						int t = (int) Math.ceil(agent.getGameTime() / 10000.0);
						hotelPredictor.updateDeltas(t);
						String firstString = String.format("First: %f, %f, %f Counterpart: %f, %f, %f\n" , hotelPredictor.previousPrices[0], hotelPredictor.currentPrices[0], hotelPredictor.deltas[0], hotelPredictor.previousPrices[4], hotelPredictor.currentPrices[4], hotelPredictor.deltas[4]);
						String secondString = String.format("Second: %f, %f, %f Counterpart: %f, %f, %f\n" , hotelPredictor.previousPrices[1], hotelPredictor.currentPrices[1], hotelPredictor.deltas[1], hotelPredictor.previousPrices[5], hotelPredictor.currentPrices[5], hotelPredictor.deltas[5]);
						String thirdString = String.format("Third: %f, %f, %f Counterpart: %f, %f, %f\n" , hotelPredictor.previousPrices[2], hotelPredictor.currentPrices[2], hotelPredictor.deltas[2], hotelPredictor.previousPrices[6], hotelPredictor.currentPrices[6], hotelPredictor.deltas[6]);
						String fourthString = String.format("Fourth: %f, %f, %f Counterpart: %f, %f, %f\n\n" , hotelPredictor.previousPrices[3], hotelPredictor.currentPrices[3], hotelPredictor.deltas[3], hotelPredictor.previousPrices[7], hotelPredictor.currentPrices[7], hotelPredictor.deltas[7]);
//						System.err.println(firstString);
//						System.err.println(secondString);
//						System.err.println(thirdString);
//						System.err.println(fourthString);
						bwHotels.write(firstString);
						bwHotels.newLine();
						bwHotels.write(secondString);
						bwHotels.newLine();
						bwHotels.write(thirdString);
						bwHotels.newLine();
						bwHotels.write(fourthString);
						bwHotels.newLine();
						bwHotels.write("-----");
						bwHotels.newLine();
						bwHotels.flush();
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					bwHotels.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();

		setAuctionAllocations();
		bid();
	}

	@Override
	public void gameStopped() {
		try {
			DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
			Date date = new Date();

			File chartsFile = new File(dateFormat.format(date) + "charts.txt");
			if (!chartsFile.exists()) {
				chartsFile.createNewFile();
			}
			File priceFile = new File(dateFormat.format(date) + "prices.txt");
			if (!priceFile.exists()) {
				priceFile.createNewFile();
			}

			FileWriter fwCharts = new FileWriter(chartsFile.getAbsoluteFile());
			BufferedWriter bwCharts = new BufferedWriter(fwCharts);
			FileWriter fwPrices = new FileWriter(priceFile.getAbsoluteFile());
			BufferedWriter bwPrices = new BufferedWriter(fwPrices);
			for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
				List<Float> askPriceList = askPrices.get(TACAgent.getAuctionTypeAsString(i));
				List<Float> bidPriceList = bidPrices.get(TACAgent.getAuctionTypeAsString(i));
				if (TACAgent.getAuctionCategory(i) == TACAgent.CAT_FLIGHT) {
					long gameTimePast = agent.getGameLength() - agent.getGameTimeLeft();
					int flightDataPointsPast = (int) (gameTimePast / 10000);
					String url = createPredictionChart(i, predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(i)), 54, flightDataPointsPast);
					bwCharts.write(url);
				} else {
					bwCharts.write(createChart(i));
				}
				bwCharts.newLine();

				bwPrices.write("Auction " + i);
				bwPrices.newLine();
				for (Float f : askPriceList) {
					bwPrices.write(f + " ");
				}
				if (TACAgent.getAuctionCategory(i) == TACAgent.CAT_ENTERTAINMENT) {
					bwPrices.newLine();
					for (Float b : bidPriceList) {
						bwPrices.write(b + " ");
					}
				}

				bwPrices.newLine();
			}
			bwCharts.close();
			bwPrices.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void auctionClosed(int auction) {
		int auctionCategory = agent.getAuctionCategory(auction);
		if (auctionCategory == TACAgent.CAT_HOTEL) {
			int hotelID = auction - 8;
			int t = (int) Math.ceil(agent.getGameTime() / 10000.0);
			hotelPredictor.closed[hotelID] = t;
		}
	}

	public void quoteUpdated(Quote quote) {
		int auctionID = quote.getAuction();
		int auctionCategory = TACAgent.getAuctionCategory(auctionID);

		// System.err.println(TACAgent.getAuctionTypeAsString(auctionID) +
		// " -- " + quote.getAskPrice());
		askPrices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(quote.getAskPrice());
		bidPrices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(quote.getBidPrice());

		if (auctionCategory == TACAgent.CAT_FLIGHT) {
			int t = (int) Math.ceil(agent.getGameTime() / 10000.0);
			int flightID = auctionID;
			float currentFlightPrice = quote.getAskPrice();
			if (flightPredictor.previousPrices[flightID] == 0) {
				flightPredictor.previousPrices[flightID] = currentFlightPrice;
			} else {
				flightPredictor.previousPrices[flightID] = flightPredictor.currentPrices[flightID];
			}
			flightPredictor.currentPrices[flightID] = currentFlightPrice;
			flightPredictor.updateProbabilityDistribution(flightID, t);
			float currentPredictedFlightMin = flightPredictor.getProbableMinimumPrice(flightID, t, quote.getAskPrice());
			
			//Static threshold
			float threshold = 80.f;
			
			int alloc = agent.getAllocation(auctionID);
			int ownedTickets = agent.getOwn(auctionID);
			int probablyOwnedTickets = agent.getProbablyOwn(auctionID);
			
			int diff = alloc - ownedTickets - probablyOwnedTickets;
			System.err.println("Owned tickets " + ownedTickets);
			System.err.println("Probably owned tickets " + probablyOwnedTickets);
			System.err.println("Allocated tickets " + alloc);
			System.err.println("-----");
			
			if((currentFlightPrice-currentPredictedFlightMin) < threshold) {
				if(diff > 0) {
					float bidPrice = currentFlightPrice + 50;
					System.err.println("Bid on flight " + auctionID + " for " + currentFlightPrice);
					Bid bid = new Bid(auctionID);
					bid.addBidPoint(diff, bidPrice);
					agent.submitBid(bid);
				}
			}		
			
			predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(currentPredictedFlightMin);

			int flightGraphID = 0;
			if (auctionID == flightGraphID) {
				long gameTimePast = agent.getGameLength() - agent.getGameTimeLeft();
				int flightDataPointsPast = (int) Math.ceil(gameTimePast / 10000);
//				System.err.println(agent.getGameTimeLeft() + " " + flightDataPointsPast);
				
				String url = createPredictionChart(flightGraphID, predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(auctionID)), 54, flightDataPointsPast);
				Image image = null;
				try {
					URL imageLocation = new URL(url);
					image = ImageIO.read(imageLocation);
				} catch (IOException e) {
					e.printStackTrace();
				}
				graphLabel.setIcon(new ImageIcon(image));
				graphFrame.repaint();
			}

		} else if (auctionCategory == TACAgent.CAT_HOTEL) {
			// System.err.println("UPDATED HOTEL");
			int t = (int) Math.ceil(agent.getGameTime() / 10000.0);
			int hotelID = auctionID - 8;
			if (hotelPredictor.initialized[hotelID] == false) {
				hotelPredictor.previousPrices[hotelID] = quote.getAskPrice();
				hotelPredictor.initialized[hotelID] = true;
			} else {
				hotelPredictor.previousPrices[hotelID] = hotelPredictor.currentPrices[hotelID];
			}
			hotelPredictor.currentPrices[hotelID] = quote.getAskPrice();
			
			int alloc = agent.getAllocation(auctionID); // Number of bids
														// allocated
			int hypotheticalQuantityWon = quote.getHQW();
			// If some bids were attempted in this auction and the HQW isn't
			// every ticket bid on
			if (alloc > 0 && quote.hasHQW(agent.getBid(auctionID)) && hypotheticalQuantityWon < alloc) {
				Bid bid = new Bid(auctionID);
				bidValues[auctionID] = updateHotelPrice(quote);
				// System.err.println("Trying to bid at: " +
				// bidValues[auctionID]);
				bid.addBidPoint(alloc, bidValues[auctionID]);

				agent.submitBid(bid);
			}
		} else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT) {
			// System.err.println("UPDATED ENTERTAINMENT");
			int alloc = agent.getAllocation(auctionID);
			int ownedTickets = agent.getOwn(auctionID);
			int allocDiff = alloc - ownedTickets;
			if (allocDiff != 0) {
				Bid bid = new Bid(auctionID);
				bidValues[auctionID] = updateEntertainmentPrice(allocDiff, quote);
				// System.err.println("Trying to bid at: " +
				// bidValues[auctionID]);
				bid.addBidPoint(allocDiff, bidValues[auctionID]);
				agent.submitBid(bid);
			}
		}
	}

	private float updateFlightPrice(int allocDiff, Quote quote) {
		// System.err.println(quote.getAskPrice());
		return quote.getAskPrice();
	}

	private float updateHotelPrice(Quote quote) {
		// System.err.println((quote.getAskPrice() + 50.f));
		return quote.getAskPrice() + 50.f;
	}

	private float updateEntertainmentPrice(int allocDiff, Quote quote) {
		// System.err.println(((quote.getAskPrice() + quote.getBidPrice()) /
		// 2.f));
		return (quote.getAskPrice() + quote.getBidPrice()) / 2.f;
	}

	/**
	 * Allocate how many of flights, hotels and entertainment tickets will be
	 * bid on in each auction.
	 */
	private void setAuctionAllocations() {
		System.err.println("Client | Arrival Day | Departure Day | Hotel Value | Ent 1 | Ent 2 | Ent 3 \n");
		clientInTT = getHotelTypeAssignments();
		for (int i = 0; i < 8; i++) {
			int arrivalPreference = agent.getClientPreference(i, TACAgent.ARRIVAL);
			int departurePreference = agent.getClientPreference(i, TACAgent.DEPARTURE);
			int hotelValue = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
			int[] eValues = new int[3];
			eValues[0] = agent.getClientPreference(i, TACAgent.E1);
			eValues[1] = agent.getClientPreference(i, TACAgent.E2);
			eValues[2] = agent.getClientPreference(i, TACAgent.E3);
			System.err.format("%6d | %11d | %13d | %11d | %5d | %5d | %5d\n", i, arrivalPreference, departurePreference, hotelValue, eValues[0],
					eValues[1], eValues[2]);
			clientPreferences[i] = new ClientPreferences(arrivalPreference, departurePreference, hotelValue, eValues[0], eValues[1], eValues[2]);
			ClientPreferences preferences = clientPreferences[i];

			// Allocate flight ticket bids.
			int[] flightDates = pickFlightDates(preferences);
			// Obtain the auction ID for the flight tickets on the preferred
			// day.
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, flightDates[0]);
			// Increment the number of tickets required from that auction.
			agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, flightDates[1]);
			agent.setAllocation(auction, agent.getAllocation(auction) + 1);

			// Allocate hotel room bids.
			for (int j = flightDates[0]; j < flightDates[1]; j++) {
				int hotelType = clientInTT[i] ? TACAgent.TYPE_GOOD_HOTEL : TACAgent.TYPE_CHEAP_HOTEL;
				auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, hotelType, j);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			}

			// Allocate entertainment ticket bids.
			for (int e = 1; e < 4; e++) {
				auction = bestEntDay(flightDates[0], flightDates[1], e);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			}
		}
	}

	private boolean[] getHotelTypeAssignments() {
		boolean[] inTampa = new boolean[8];
		Map<Integer, Integer> hotelValues = new HashMap<Integer, Integer>();
		for (int i = 0; i < 8; i++) {
			hotelValues.put(agent.getClientPreference(i, TACAgent.HOTEL_VALUE), i);
		}
		Map<Integer, Integer> sortedHotelValues = new TreeMap<Integer, Integer>(hotelValues);

		ArrayList<Integer> topCustomers = new ArrayList<Integer>(sortedHotelValues.values());
		Collections.reverse(topCustomers);
		int ttCount = 5; // The number of TT assignments
		for (int i = 0; i < ttCount; i++) {
			inTampa[topCustomers.get(i)] = true;
		}

		return inTampa;
	}

	private int bestEntDay(int inFlight, int outFlight, int type) {
		for (int i = inFlight; i < outFlight; i++) {
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, i);
			if (agent.getAllocation(auction) < agent.getOwn(auction)) {
				return auction;
			}
		}
		// If no left, just take the first...
		return TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, inFlight);
	}

	private int calculateFlightBidValue() {
		return 1;
	}

	private int calculateHotelBidValue() {
		return 1;
	}

	private int calculateEntertainmentBidValue() {
		return 1;
	}

	private void bid() {
		for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
			// Amount of tickets allocated as required minus the amount already
			// owned = Amount still needed.
			int amountRequired = agent.getAllocation(i) - agent.getOwn(i);

			int bidValue = 0;
			Bid bid;
			switch (TACAgent.getAuctionCategory(i)) {
			case TACAgent.CAT_FLIGHT:
				bidValue = calculateFlightBidValue();
				break;
			case TACAgent.CAT_HOTEL:
				bidValue = calculateHotelBidValue();
				bid = new Bid(i);
				bid.addBidPoint(amountRequired, bidValue);
				agent.submitBid(bid);
				break;
			case TACAgent.CAT_ENTERTAINMENT:
				bidValue = calculateEntertainmentBidValue();
				bid = new Bid(i);
				bid.addBidPoint(amountRequired, bidValue);
				agent.submitBid(bid);
				break;
			}

//			Bid bid = new Bid(i);
//			bid.addBidPoint(amountRequired, bidValue);
//			agent.submitBid(bid);
		}
	}

	/**
	 * Pick arrival and return date based on balance between incorrect date
	 * penalties, ticket prices and entertainment bonuses.
	 * 
	 * @return [0] = Arrival, [1] = Departure.
	 */
	private int[] pickFlightDates(ClientPreferences preferences) {
		int[] ret = { preferences.arrival, preferences.departure };
		return ret;
	}

	@Override
	public void bidUpdated(Bid bid) {
		// System.err.println("Bid " + bid.getID() + " updated.");
		// System.err.println("Auction: " + bid.getAuction());
		// System.err.println("State: " + bid.getProcessingStateAsString());
	}

	@Override
	public void bidRejected(Bid bid) {
		// System.err.println("Bid " + bid.getID() + " rejected.");
	}

	@Override
	public void bidError(Bid bid, int error) {
		// System.err.println("Bid " + bid.getID() + " error. - " +
		// agent.commandStatusToString(error));
	}

	private String createFixedWidthChart(int auctionID, int width) {
		String typeString = TACAgent.getAuctionTypeAsString(auctionID);

		List<Float> priceValues = new ArrayList<Float>(askPrices.get(typeString));
		float lastValue = priceValues.get(priceValues.size() - 1);
		for (int i = priceValues.size(); i < width; i++) {
			priceValues.add(lastValue);
		}

		return createChart(priceValues, typeString);
	}

	private String createChart(int auctionID) {
		String typeString = TACAgent.getAuctionTypeAsString(auctionID);

		List<Float> priceValues = new ArrayList<Float>(askPrices.get(typeString));

		return createChart(priceValues, typeString);
	}

	private String createChart(List<Float> priceValues, String typeString) {
		Data data = DataUtil.scaleWithinRange(0, 1000, priceValues);
		Line line = Plots.newLine(data, Color.GREEN);

		LineChart chart = GCharts.newLineChart(line);
		chart.setSize(550, 400);
		chart.setTitle(typeString, Color.WHITE, 14);
		chart.setGrid(10, 10, 3, 2);

		// Defining axis info and styles
		chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, priceValues.size()));
		chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, 1000));

		AxisLabels xAxisLabel = AxisLabelsFactory.newAxisLabels("Price Changes", 50.0);
		xAxisLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.WHITE, 14, AxisTextAlignment.CENTER));

		AxisLabels yAxisLabel = AxisLabelsFactory.newAxisLabels("Prices", 50.0);
		yAxisLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.WHITE, 14, AxisTextAlignment.CENTER));

		// Adding axis info to chart.
		chart.addXAxisLabels(xAxisLabel);
		chart.addYAxisLabels(yAxisLabel);

		// Defining background and chart fills.
		chart.setBackgroundFill(Fills.newSolidFill(Color.newColor("1F1D1D")));
		LinearGradientFill fill = Fills.newLinearGradientFill(0, Color.newColor("363433"), 100);
		fill.addColorAndOffset(Color.newColor("2E2B2A"), 0);
		chart.setAreaFill(fill);
		String url = chart.toURLString();

		return url;
	}

	private String createPredictionChart(int auctionID, List<Float> predictions, int maxDataPointCount, int pastPointCount) {
		String typeString = TACAgent.getAuctionTypeAsString(auctionID);
		
		int missedPointCount = (pastPointCount - askPrices.get(typeString).size());
		
		List<Float> priceValues = new ArrayList<Float>();
		float firstPriceValue = askPrices.get(typeString).get(0);
		for(int i = 0; i <= missedPointCount; i++){
			priceValues.add(firstPriceValue);
		}
		List<Float> predictionList = new ArrayList<Float>();
		for(int i = 0; i <= missedPointCount; i++){
			predictionList.add(firstPriceValue);
		}
				
		priceValues.addAll(askPrices.get(typeString));
		float lastPriceValue = priceValues.get(priceValues.size() - 1);
		for (int i = priceValues.size(); i < maxDataPointCount; i++) {
			priceValues.add(lastPriceValue);
		}
		predictionList.addAll(predictions);
		float lastPredictionValue = predictionList.get(predictionList.size() - 1);
		for (int i = predictionList.size(); i < maxDataPointCount; i++) {
			predictionList.add(lastPredictionValue);
		}

		Data priceData = DataUtil.scaleWithinRange(0, 1000, priceValues);
		Data predictionData = DataUtil.scaleWithinRange(0, 1000, predictionList);
		Line priceLine = Plots.newLine(priceData, Color.GREEN);
		Line predictionLine = Plots.newLine(predictionData, Color.RED);

		LineChart chart = GCharts.newLineChart(priceLine, predictionLine);
		chart.setSize(550, 400);
		chart.setTitle(typeString, Color.WHITE, 14);
		if(missedPointCount > 0) {
			double r = (1.0 * missedPointCount/maxDataPointCount);
			chart.addVerticalRangeMarker(0, 100.0*r, Color.DARKSLATEGRAY);
		}
		double gr = 100.0 * (5.0 / maxDataPointCount);
		chart.setGrid(gr, 10.0, 3, 2);
		
		// Defining axis info and styles
		chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, priceValues.size()));
		chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, 1000));

		AxisLabels xAxisLabel = AxisLabelsFactory.newAxisLabels("Price Changes", 50.0);
		xAxisLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.WHITE, 14, AxisTextAlignment.CENTER));

		AxisLabels yAxisLabel = AxisLabelsFactory.newAxisLabels("Prices", 50.0);
		yAxisLabel.setAxisStyle(AxisStyle.newAxisStyle(Color.WHITE, 14, AxisTextAlignment.CENTER));

		// Adding axis info to chart.
		chart.addXAxisLabels(xAxisLabel);
		chart.addYAxisLabels(yAxisLabel);

		// Defining background and chart fills.
		chart.setBackgroundFill(Fills.newSolidFill(Color.newColor("1F1D1D")));
		LinearGradientFill fill = Fills.newLinearGradientFill(0, Color.newColor("363433"), 100);
		fill.addColorAndOffset(Color.newColor("2E2B2A"), 0);
		chart.setAreaFill(fill);
		String url = chart.toURLString();

		return url;
	}
}
