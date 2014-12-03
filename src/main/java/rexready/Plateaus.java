package rexready;

public class Plateaus {
	public float lowMax;
	public float mediumMin;
	public float mediumMax;
	public float highMin;

	public Plateaus(float lowMax, float mediumMin, float mediumMax, float highMin) {
		this.lowMax = lowMax;
		this.mediumMin = mediumMin;
		this.mediumMax = mediumMax;
		this.highMin = highMin;
	}

	public float[] calculateWeights(float data) {
		float[] weights = new float[3];
		if (data <= this.lowMax) {
			weights[0] = 1;
		} else if (data < this.mediumMin) {
			float overallDifference = this.mediumMin - this.lowMax;
			float difference = this.mediumMin - data;
			weights[0] = difference / overallDifference;
			weights[1] = 1 - weights[0];
		} else if (data >= this.mediumMin && data <= this.mediumMax) {
			weights[1] = 1;
		} else if (data < this.highMin) {
			float overallDifference = this.highMin - this.mediumMax;
			float difference = this.highMin - data;
			weights[1] = difference / overallDifference;
			weights[2] = 1 - weights[1];
		} else if (data >= this.highMin) {
			weights[2] = 1;
		}

		return weights;
	}

}
