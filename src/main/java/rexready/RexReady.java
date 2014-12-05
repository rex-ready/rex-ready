package rexready;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

public class RexReady extends AgentImpl {
	
	private FlightPricePredictor flightPredictor = new FlightPricePredictor();
	private HotelPricePredictor hotelPredictor = new HotelPricePredictor();
	private EntertainmentPricePredictor entertainmentPredictor = new EntertainmentPricePredictor();
	private Optimiser optimiser = new Optimiser();
	
	private Map<String, List<Float>> askPrices = new HashMap<String, List<Float>>();
	private Map<String, List<Float>> predictedMinimumFlightPrices = new HashMap<String, List<Float>>();
	private Map<String, List<Float>> bidPrices = new HashMap<String, List<Float>>();
	
	@Override
	protected void init(ArgEnumerator args) {
		System.out.println("init");
		
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
		System.out.println("gameStarted");
		System.out.println("Client | Arrival Day | Departure Day | Hotel Value | Ent 1 | Ent 2 | Ent 3 \n");
		for (int i = 0; i < 8; i++) {
			int arrivalPreference = agent.getClientPreference(i, TACAgent.ARRIVAL);
			int departurePreference = agent.getClientPreference(i, TACAgent.DEPARTURE);
			int hotelValue = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
			int[] eValues = new int[3];
			eValues[0] = agent.getClientPreference(i, TACAgent.E1);
			eValues[1] = agent.getClientPreference(i, TACAgent.E2);
			eValues[2] = agent.getClientPreference(i, TACAgent.E3);
			System.out.format("%6d | %11d | %13d | %11d | %5d | %5d | %5d\n", i, arrivalPreference, departurePreference, hotelValue, eValues[0], eValues[1], eValues[2]);
			optimiser.addClient(new ClientPreferences(arrivalPreference, departurePreference, hotelValue, eValues[0], eValues[1], eValues[2]));
		}
	}

	@Override
	public void gameStopped() {
		System.out.println("gameStopped");
		
		try {
			DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyHHmmss");
			Date date = new Date();

			File chartsFile = new File("data/" + dateFormat.format(date) + "charts.txt");
			if (!chartsFile.exists()) {
				chartsFile.createNewFile();
			}
			File priceFile = new File("data/" + dateFormat.format(date) + "prices.txt");
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
					int flightDataPointsPast = (int) (agent.getGameTime() / 10000);
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
	public void quoteUpdated(Quote quote) {
		System.out.println("quoteUpdated");
		System.out.println(agent.getGameTime());
		
		int t = (int) Math.ceil(agent.getGameTime() / 10000.f);
		
		askPrices.get(TACAgent.getAuctionTypeAsString(quote.getAuction())).add(quote.getAskPrice());
		bidPrices.get(TACAgent.getAuctionTypeAsString(quote.getAuction())).add(quote.getBidPrice());
				
		switch (TACAgent.getAuctionCategory(quote.getAuction())) {
		case TACAgent.CAT_FLIGHT:
			if (flightPredictor.previousPrices[quote.getAuction()] == 0) {
				flightPredictor.previousPrices[quote.getAuction()] = quote.getAskPrice();
			}
			else
			{
				flightPredictor.previousPrices[quote.getAuction()] = flightPredictor.currentPrices[quote.getAuction()];
			}
			flightPredictor.currentPrices[quote.getAuction()] = quote.getAskPrice();
			flightPredictor.updateProbabilityDistribution(quote.getAuction(), t);
			
			int auctionID = quote.getAuction();
			predictedMinimumFlightPrices.get(TACAgent.getAuctionTypeAsString(auctionID)).add(flightPredictor.getProbableMinimumPrice(auctionID, t, agent.getQuote(auctionID).getAskPrice()));
			break;
		case TACAgent.CAT_HOTEL:
			int hotelID = quote.getAuction() - 8;
			if (hotelPredictor.initialized[hotelID] == false) {
				hotelPredictor.previousPrices[hotelID] = quote.getAskPrice();
				hotelPredictor.initialized[hotelID] = true;
			} else {
				hotelPredictor.previousPrices[hotelID] = hotelPredictor.currentPrices[hotelID];
			}
			hotelPredictor.currentPrices[hotelID] = quote.getAskPrice();
			break;
		case TACAgent.CAT_ENTERTAINMENT:
			entertainmentPredictor.currentPrices[quote.getAuction() - 16] = quote.getAskPrice();
			break;
		}
		
		if (quote.getAuction() == 7) {
			hotelPredictor.updateDeltas(t);
			
			entertainmentPredictor.updateDeltas(t);
			PriceData priceData = new PriceData();
			for (int i = 0; i < TACAgent.getAuctionNo(); ++i) {
				switch (TACAgent.getAuctionCategory(i)) {
				case TACAgent.CAT_FLIGHT:
					priceData.setPrice(Good.values()[i], flightPredictor.getProbableMinimumPrice(i, (int) (agent.getGameTime() / 10000.f), agent.getQuote(i).getAskPrice()));
					break;
				case TACAgent.CAT_HOTEL:
					System.out.println("Delta for hotel " + i + "=" + hotelPredictor.deltas[i - 8]);
					priceData.setPrice(Good.values()[i], agent.getQuote(i).getAskPrice() + hotelPredictor.deltas[i - 8] * 2);
					break;
				case TACAgent.CAT_ENTERTAINMENT:
					System.out.println("Delta for entertainment " + i + "=" + entertainmentPredictor.deltas[i - 16]);
					priceData.setPrice(Good.values()[i], agent.getQuote(i).getAskPrice() + entertainmentPredictor.deltas[i - 16]);
					break;
				}
			}
			for (Good good : Good.values()) {
				System.out.println("Predicted price of " + good.name() + " is " + priceData.getPrice(good));
			}
			GoodsList ownedGoods = new GoodsList();
			for (Good good : Good.values()) {
				ownedGoods.setAmount(good, agent.getOwn(good.ordinal()));
			}
			Strategy strategy = optimiser.optimise(priceData, ownedGoods, 5000, 0.2f);
			System.out.println(strategy);
			System.out.println("Projected score: " + strategy.getScore(priceData, ownedGoods));
			agent.clearAllocation();
			// TODO: Clear old bids
			for (Good good : Good.values()) {
				agent.setAllocation(good.ordinal(), strategy.getShoppingList().getAmount(good));
				System.out.println("Allocating " + strategy.getShoppingList().getAmount(good) + " " + good.name() + "s");
			}
			System.out.println("Bid now");
			
			if (agent.getGameTime() > 30000) {
				// FLIGHT BIDDING
				if (agent.getGameTime() > 240000) {
					float maxThreshold = 100.f;
					float minThreshold = 30.f;
					float threshold = minThreshold + ((agent.getGameTime() / (1.f * agent.getGameLength())) * (maxThreshold - minThreshold));
					
					for (int i = 0; i < 8; ++i) {
						int alloc = agent.getAllocation(i);
						int ownedTickets = agent.getOwn(i);
						int probablyOwnedTickets = agent.getProbablyOwn(i);
						
						int diff = alloc - ownedTickets - probablyOwnedTickets;
						
						if((agent.getQuote(i).getAskPrice()-priceData.getPrice(Good.values()[i])) < threshold) {
							if(diff > 0) {
								float bidPrice = agent.getQuote(i).getAskPrice() + 50;
								System.out.println("Bid on flight " + i + " for " + bidPrice);
								Bid bid = new Bid(i);
								bid.addBidPoint(diff, bidPrice);
								agent.submitBid(bid);
							}
						}
					}
				}
				// END FLIGHT BIDDING
				
				// ENTERTAINMENT BIDDING
				for (int i = 16; i < 28; ++i) {
					int alloc = agent.getAllocation(i);
					int ownedTickets = agent.getOwn(i);
					int probablyOwnedTickets = agent.getProbablyOwn(i);
					int diff = alloc - ownedTickets - probablyOwnedTickets;
					if (diff > 0) {
						float bidPrice = Math.min(agent.getQuote(i).getAskPrice(), 200);
						System.out.println("Bid on entertainment " + i + " for " + bidPrice);
						Bid bid = new Bid(i);
						bid.addBidPoint(diff, bidPrice);
						agent.submitBid(bid);
					}
					if (diff < 0) {
						float sellPrice = Math.max(agent.getQuote(i).getAskPrice() - 10, 60);
						System.out.println("Selling unneeded entertainment " + i + " for " + sellPrice);
						Bid bid = new Bid(i);
						bid.addBidPoint(diff, sellPrice);
						agent.submitBid(bid);
					}
				}
				// END ENTERTAINMENT BIDDING
				
				if (agent.getGameTime() % 60000 > 50000) {
					System.out.println("Bid for hotels now");
					for (int i = 8; i < 16; ++i) {
						int alloc = agent.getAllocation(i);
						int ownedTickets = agent.getOwn(i);
						int probablyOwnedTickets = agent.getProbablyOwn(i);
						int diff = alloc - ownedTickets - probablyOwnedTickets;
						if (diff > 0) {
							float bidPrice = priceData.getPrice(Good.values()[i]);
							System.out.println("Bid on hotel " + i + " for " + (bidPrice + 50));
							Bid bid = new Bid(i);
							bid.addBidPoint(diff, bidPrice + 50);
							agent.submitBid(bid);
						}
					}
				}
			}
		}
	}

	@Override
	public void bidUpdated(Bid bid) {
		System.out.println("bidUpdated: " + bid.getAuction() + " (" + bid.getProcessingStateAsString() + ")");
	}

	@Override
	public void bidRejected(Bid bid) {
		System.out.println("bidRejected: " + bid.getAuction() + " (" + bid.getRejectReasonAsString() + ")");
	}

	@Override
	public void bidError(Bid bid, int error) {
		System.out.println("bidError: " + bid.getAuction() + " (" + agent.commandStatusToString(error) + ")");
	}

	@Override
	public void auctionClosed(int auction) {
		System.out.println("auctionClosed: " + auction);
		System.out.println(agent.getGameTime());
		if (TACAgent.getAuctionCategory(auction) == TACAgent.CAT_HOTEL) {
			int hotelID = auction - 8;
			hotelPredictor.closed[hotelID] = (int) Math.ceil(agent.getGameTime() / 10000.f);
		}
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
