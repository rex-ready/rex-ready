package rexready;

public class FlightPricePredictor {
	public static final int c = 10;
	public static final int d = 30;
	public static final int T = 54;

	public float[][] probabilityDistributions = new float[8][c + d + 1];
	public float[] previousPrices = new float[8];
	public float[] currentPrices = new float[8];

	// this finds the range that the perturbation value is drawn from
	public int[] getPerturbationRange(int t, int z) {
		float y = (float) c + ((float) t / (float) T) * ((float) z - (float) c);
		int a, b;
		if (y > 0) {
			a = -c;
			b = (int) Math.ceil(y);
		} else if (y < 0) {
			a = (int) Math.floor(y);
			b = c;
		} else {
			a = -c;
			b = c;
		}

		return new int[] { a, b };
	}

	public void initProbibilityDistribution(int auction) {
		for (int i = 0; i <= d + c; i++) {
			probabilityDistributions[auction][i] = 1.0f / (c + d + 1);
		}
	}

	public void updateProbabilityDistribution(int auction, int t) {
		float priceDifference = currentPrices[auction] - previousPrices[auction];
		float[] probabilityDistribution = probabilityDistributions[auction];
		float sum = 0;
		for (int z = -c; z <= d; z++) {
			int[] range = getPerturbationRange(t, z);
			float p = 0;
			if (priceDifference >= range[0] && priceDifference <= range[1]) {
				p = 1.0f / (range[1] - range[0]);
			}
			probabilityDistribution[z + c] = p * probabilityDistribution[z + c];
			sum += probabilityDistribution[z + c];
		}
		for (int z = 0; z <= d + c; z++) {
			if (sum != 0)
				probabilityDistribution[z] /= sum;
		}
		if (sum == 0) {
			initProbibilityDistribution(auction);
		}
		probabilityDistributions[auction] = probabilityDistribution;
	}

	public void updateAllProbabilityDistributions(int t) {
		for (int i = 0; i < 8; i++) {
			updateProbabilityDistribution(i, t);
		}
	}

	public float getExpectedPerturbation(int t, int z, float price) {
		int[] change = getPerturbationRange(t, z);
		float minChange = Math.max(150.0f - price, change[0]);
		float maxChange = Math.min(800.0f - price, change[1]);

		return (maxChange + minChange) / 2.0f;
	}

	public float getExpectedMinimumPriceChange(int t, int z, float price) {
		float expectedChange = 0;
		float minimum = 100000000;

		for (int i = t; i < T; i++) {
			expectedChange += getExpectedPerturbation(i, z, price);
			if (expectedChange < minimum)
				minimum = expectedChange;
		}
		return minimum;
	}

	public float getProbableMinimumPrice(int auction, int t, float price) {
		float probablePrice = 0;

		for (int z = -c; z <= d; z++) {
			float expectedPrice = price + getExpectedMinimumPriceChange(t, z, price);

			probablePrice += expectedPrice * probabilityDistributions[auction][z + c];
		}
		return probablePrice;
	}

	public float[] getAllProbableMinumumPrices(int t) {
		float[] prices = new float[8];
		for (int i = 0; i < 8; i++) {
			prices[i] = getProbableMinimumPrice(i, t, currentPrices[i]);
		}
		return prices;
	}
}
