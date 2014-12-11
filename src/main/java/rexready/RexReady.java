package rexready;

import java.awt.Dimension;
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

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

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

import rexready.gui.MarketplaceTableModel;
import rexready.gui.StrategyTableModel;
import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.util.ArgEnumerator;

public class RexReady extends AgentImpl {
	
	private FlightPricePredictor flightPredictor;
	private HotelPricePredictor hotelPredictor;
	private EntertainmentPricePredictor entertainmentPredictor;
	private Optimiser optimiser;
	
	private Map<String, List<Float>> askPrices = new HashMap<String, List<Float>>();
	private Map<String, List<Float>> predictedMinimumFlightPrices = new HashMap<String, List<Float>>();
	private Map<String, List<Float>> bidPrices = new HashMap<String, List<Float>>();
	
	private MarketplaceTableModel marketplaceTableModel = new MarketplaceTableModel();
	private StrategyTableModel strategyTableModel = new StrategyTableModel();
	
	private float entertainmentUndercut;
	
	@Override
	protected void init(ArgEnumerator args) {
		JFrame marketplaceWindow = new JFrame("Price data");
		marketplaceWindow.setContentPane(new JScrollPane(new JTable(marketplaceTableModel)));
		marketplaceWindow.setPreferredSize(new Dimension(200, 500));
		marketplaceWindow.pack();
		marketplaceWindow.setVisible(true);
		
		JFrame strategyWindow = new JFrame("Current strategy");
		strategyWindow.setContentPane(new JScrollPane(new JTable(strategyTableModel)));
		strategyWindow.setPreferredSize(new Dimension(1000, 200));
		strategyWindow.pack();
		strategyWindow.setVisible(true);
	}

	@Override
	public void gameStarted() {
		entertainmentUndercut = 0.f;
		optimiser = new Optimiser();
		flightPredictor = new FlightPricePredictor();
		hotelPredictor = new HotelPricePredictor();
		entertainmentPredictor = new EntertainmentPricePredictor();
		askPrices.clear();
		predictedMinimumFlightPrices.clear();
		bidPrices.clear();
		
		for (int i = 0; i < TACAgent.getAuctionNo(); i++) {
			askPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
			bidPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
			if (i < 8) {
				predictedMinimumFlightPrices.put(TACAgent.getAuctionTypeAsString(i), new ArrayList<Float>());
			}
		}
		
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
		
		// Place bids for 4 of every hotel room @ 6 each
		for (int i = 8; i < 16; ++i) {
			Bid bid = new Bid(i);
			bid.addBidPoint(4, 6);
			agent.submitBid(bid);
		}
	}

	@Override
	public void gameStopped() {
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
				priceData.setAvailable(Good.values()[i], !agent.getQuote(i).isAuctionClosed());
				switch (TACAgent.getAuctionCategory(i)) {
				case TACAgent.CAT_FLIGHT:
					priceData.setPrice(Good.values()[i], flightPredictor.getProbableMinimumPrice(i, (int) (agent.getGameTime() / 10000.f), agent.getQuote(i).getAskPrice()));
					break;
				case TACAgent.CAT_HOTEL:
					priceData.setPrice(Good.values()[i], agent.getQuote(i).getAskPrice() + hotelPredictor.deltas[i - 8] * 2);
					break;
				case TACAgent.CAT_ENTERTAINMENT:
					priceData.setPrice(Good.values()[i], agent.getQuote(i).getAskPrice());
					if (agent.getQuote(i).getAskPrice() < 1.f) {
						priceData.setAvailable(Good.values()[i], false);
					}
					break;
				}
			}
			marketplaceTableModel.setPriceData(priceData);
			GoodsList ownedGoods = new GoodsList();
			for (Good good : Good.values()) {
				int amount = agent.getOwn(good.ordinal());
				if (!agent.getQuote(good.ordinal()).isAuctionClosed()) {
					amount += agent.getProbablyOwn(good.ordinal());
				}
				ownedGoods.setAmount(good, amount);
			}
			Strategy strategy = optimiser.optimise(priceData, ownedGoods, 7000, 0.2f);
			strategyTableModel.update(strategy, priceData, ownedGoods);
			float score = strategy.getScore(priceData, ownedGoods);
			float totalCost = 0.f;
			for (int i = 0; i < TACAgent.getAuctionNo(); ++i) {
				totalCost += agent.costs[i];
			}
			System.out.println("Projected score: " + score + " - " + totalCost + " = " + (score - totalCost));
			agent.clearAllocation();
			for (Good good : Good.values()) {
				agent.setAllocation(good.ordinal(), strategy.getShoppingList().getAmount(good));
			}
			
			if (!(strategy.getScore(priceData, ownedGoods) > 0)) {
				System.out.println();
				System.out.println();
				System.out.println("************************************************");
				System.out.println("************************************************");
				System.out.println("*** WARNING: PROJECTED SCORE IS NOT POSITIVE ***");
				System.out.println("***       SKIPPING BIDDING FOR 1 TICK        ***");
				System.out.println("************************************************");
				System.out.println("************************************************");
				System.out.println();
				System.out.println();
			}
			
			if (agent.getGameTime() > 30000 && strategy.getScore(priceData, ownedGoods) > 0) {
				// FLIGHT BIDDING
				if (agent.getGameTime() > 240000) {
					for (int i = 0; i < 8; ++i) {
						int alloc = agent.getAllocation(i);
						int ownedTickets = agent.getOwn(i);
						int probablyOwnedTickets = agent.getProbablyOwn(i);
						
						int diff = alloc - ownedTickets - probablyOwnedTickets;
						
						if((agent.getQuote(i).getAskPrice()-priceData.getPrice(Good.values()[i])) < 10 || agent.getGameTime() > 510000) {
							if(diff > 0) {
								float bidPrice = agent.getQuote(i).getAskPrice() + 50;
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
					Bid bid = new Bid(i);
					if (agent.getGameTime() > 240000 && diff > 0) {
						float bidPrice = Math.min(agent.getQuote(i).getAskPrice(), 200);
						bid.addBidPoint(diff, bidPrice);
					}
					if (agent.getGameTime() > 240000 && diff < 0) {
						float sellPrice = Math.max(agent.getQuote(i).getAskPrice() - 10, 60);
						bid.addBidPoint(diff, sellPrice);
					}
					if (alloc == 0 && ownedTickets == 0 && agent.getGameTime() < 240000) {
						float maxPrice = 300.f;
						if (agent.getQuote(i).getAskPrice() < (maxPrice - entertainmentUndercut) && (maxPrice - entertainmentUndercut) > 200) {
							entertainmentUndercut += 10;
							System.out.println("Someone undercut us on good " + i + ", changing sell price to " + (maxPrice - entertainmentUndercut));
						}
						float sellPrice = maxPrice - entertainmentUndercut;
						if (sellPrice > 200) {
							bid.addBidPoint(-1, sellPrice);
						}
					}
					agent.submitBid(bid);
				}
				// END ENTERTAINMENT BIDDING
				//HOTEL BIDDING
				if (agent.getGameTime() % 60000 > 50000) {
					for (int i = 8; i < 16; ++i) {
						int alloc = agent.getAllocation(i);
						int ownedTickets = agent.getOwn(i);
						int probablyOwnedTickets = agent.getProbablyOwn(i);
						int diff = alloc - ownedTickets - probablyOwnedTickets;
						if (diff > 0) {
							float bidPrice = priceData.getPrice(Good.values()[i]);
							Bid bid = new Bid(i);
							bid.addBidPoint(diff, bidPrice + 50);
							agent.submitBid(bid);
						}
					}
				}
				//END HOTEL BIDDING
			}
		}
	}

	@Override
	public void bidUpdated(Bid bid) {
	}

	@Override
	public void bidRejected(Bid bid) {
		System.out.println("WARNING: Bid rejected: " + bid.getAuction() + " (" + bid.getRejectReasonAsString() + ")");
	}

	@Override
	public void bidError(Bid bid, int error) {
		System.out.println("WARNING: Bid error: " + bid.getAuction() + " (" + agent.commandStatusToString(error) + ")");
	}

	@Override
	public void auctionClosed(int auction) {
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
