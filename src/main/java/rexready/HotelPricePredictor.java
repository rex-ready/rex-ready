package rexready;

public class HotelPricePredictor {
	public static final int T = 54;
	public static final float[] deltaValues = {8, 20, 33};

	public float[] deltas = new float[8];
	public float[] previousPrices = new float[8];
	public float[] currentPrices = new float[8];
	public boolean[] initialized = new boolean[8];
	public int[] closed = new int[8];
	public Plateaus priceSizePlateaus = new Plateaus(40, 60, 100, 140);
	public Plateaus priceDifferencePlateaus = new Plateaus(10, 15, 25, 30);


	public void updateDeltas(int t) {
		for (int i = 0; i < 8; i++) {
			deltas[i] = updateDeltaForAuction(i, t);
		}

	}

	public float updateDeltaForAuction(int auctionID, int t) {
		float[] delta = new float[3];

		int type = 1;
		if (auctionID > 3) // good hotel
		{
			type = -1;
		}
		int counterpartID = auctionID + 4 * type;

		float[] prices = new float[2];
		prices[0] = currentPrices[auctionID];
		prices[1] = currentPrices[counterpartID];
		float[] priceDifferences = new float[2];
		priceDifferences[0] = currentPrices[auctionID] - previousPrices[auctionID];
		priceDifferences[1] = currentPrices[counterpartID] - previousPrices[counterpartID];
		float[][] priceSize = new float[2][3];
		float[][] priceChangeSpeed = new float[2][3];

		for (int i = 0; i < 2; i++) {
			priceSize[i] = priceSizePlateaus.calculateWeights(prices[i]);
			priceChangeSpeed[i] = priceDifferencePlateaus.calculateWeights(priceDifferences[i]);
		}

		if (closed[counterpartID] == 0) // if the counterpart auction is still
										// open
		{
			// if p is high and priceChange is fast, delta is big
			delta[2] += (priceSize[0][2] + priceChangeSpeed[0][2]) / 2.0f;
			// if p is high and cP is high and C is not fast, delta is big
			delta[2] += (priceSize[0][2] + priceSize[1][2] + (priceChangeSpeed[0][0] + priceChangeSpeed[0][1])) / 3.0f;
			// if p is high and CP is not high and c is not quick, delta is
			// medium
			delta[1] += (priceSize[0][2] + (priceSize[1][0] + priceSize[1][1]) + (priceChangeSpeed[0][0] + priceChangeSpeed[0][1])) / 3.0f;
			// if p is low and CP is high then delta is medium
			delta[1] += (priceSize[0][0] + priceSize[1][2]) / 2.0;
			// if P is low and CP is not high then delta is small
			delta[0] += (priceSize[0][0] + (priceSize[1][0] + priceSize[1][1])) / 2.0;
			// if P is medium and CP is high then delta is medium
			delta[1] += (priceSize[0][1] + priceSize[1][2]) / 2.0;
			// if p is medium and CP is not high and c is not slow then delta is
			// medium
			delta[1] += (priceSize[0][1] + (priceSize[1][0] + priceSize[1][1]) + (priceChangeSpeed[0][1] + priceChangeSpeed[0][2])) / 3.0f;
			// if p is medium and CP is not high and c is slow then delta is
			// small
			delta[0] += (priceSize[0][1] + (priceSize[1][0] + priceSize[1][1]) + priceChangeSpeed[0][0]) / 3.0f;
		} else if (t - closed[counterpartID] < 6) // if the counterpart auction
													// has just closed (within a
													// minute)
		{
			// if p is high and c is not slow and CC is quick then delta is very
			// big
			delta[2] += ((priceSize[0][2] + (priceChangeSpeed[0][1] + priceChangeSpeed[0][2]) + priceChangeSpeed[1][2]) / 3.0) * 2.0;
			// if p is high and c is not slow and CC is not quick then delta is
			// big
			delta[2] += (priceSize[0][2] + (priceChangeSpeed[0][1] + priceChangeSpeed[0][2]) + (priceChangeSpeed[1][0] + priceChangeSpeed[1][1])) / 3.0;
			// if p is high and c is slow and Cc is quick then delta is big
			delta[2] += (priceSize[0][2] + priceChangeSpeed[0][0] + priceChangeSpeed[1][2]) / 3.0;
			// if p is high and c is slow and cc is not quick then delta is
			// medium
			delta[1] += (priceSize[0][2] + priceChangeSpeed[0][0] + (priceChangeSpeed[1][0] + priceChangeSpeed[1][1])) / 3.0;
			// if p is medium and c is quick then delta is big
			delta[2] += (priceSize[0][1] + priceChangeSpeed[0][2]) / 2.0;
			// if p is medium and c is medium and cc is quick then delta is big
			delta[2] += (priceSize[0][1] + priceChangeSpeed[0][1] + priceChangeSpeed[1][2]) / 3.0;
			// if p is medium and c is medium and cc is not quick then delta is
			// medium
			delta[1] += (priceSize[0][1] + priceChangeSpeed[0][1] + (priceChangeSpeed[1][0] + priceChangeSpeed[1][1])) / 3.0;
			// if p is medium and c is slow and cc is quick then delta is medium
			delta[1] += (priceSize[0][1] + priceChangeSpeed[0][0] + priceChangeSpeed[1][2]) / 3.0;
			// if p is medium and c is slow and cc is not quick then delta is
			// small
			delta[0] += (priceSize[0][1] + priceChangeSpeed[0][0] + (priceChangeSpeed[1][0] + priceChangeSpeed[1][1])) / 3.0;
			// if p is low and c is slow and cc is quick then delta is medium
			delta[1] += (priceSize[0][0] + priceChangeSpeed[0][0] + priceChangeSpeed[1][2]) / 3.0;
			// if p is low and c is not slow then delta is medium
			delta[1] += (priceSize[0][0] + (priceChangeSpeed[0][1] + priceChangeSpeed[0][2])) / 2.0;
			// if p is low and c is slow and cc is not quick then delta is small
			delta[0] += (priceSize[0][0] + priceChangeSpeed[0][0] + (priceChangeSpeed[1][0] + priceChangeSpeed[1][1])) / 3.0;
		} else // if it has been closed for more than a minute
		{
			// if p is high and c is not slow then delta is big
			delta[2] += (priceSize[0][2] + (priceChangeSpeed[0][1] + priceChangeSpeed[0][2])) / 2.0f;
			// if p is high and c is slow then delta is medium
			delta[1] += (priceSize[0][2] + priceChangeSpeed[0][0]) / 2.0f;
			// if p is medium and c is quick then delta is big
			delta[2] += (priceSize[0][1] + priceChangeSpeed[0][2]) / 2.0;
			// if p is medium and c is medium then delta is medium
			delta[1] += (priceSize[0][1] + priceChangeSpeed[0][1]) / 2.0;
			// if p is not high c is not slow then delta is small
			delta[0] += ((priceSize[0][0] + priceSize[0][1]) + priceChangeSpeed[0][0]) / 2.0f;
			// if p is low and c is not slow then delta is medium
			delta[1] += (priceSize[0][0] + (priceChangeSpeed[0][1] + priceChangeSpeed[0][2])) / 2.0f;
		}

		float sum = 0;
		for (int i = 0; i < 3; i++) {
			sum += delta[i];
		}
		for (int i = 0; i < 3; i++) {
			if (sum != 0)
				delta[i] /= sum;
		}

		float realValue = 0;

		for (int i = 0; i < 3; i++) {
			realValue += delta[i] * deltaValues[i];
		}

		return realValue;
	}
}

// cheap hotels are first 4
