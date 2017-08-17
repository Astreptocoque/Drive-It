package ch.astrepto.robot.capteurs;

import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.NXTUltrasonicSensor;
import lejos.robotics.SampleProvider;

public class UltrasonicSensor {

	private SampleProvider capteurUltrason;
	private float[] sampleCapteurUltrason;
	
	private final static int maxDetectedDistance = 60;

	public UltrasonicSensor() {
		capteurUltrason = new NXTUltrasonicSensor(SensorPort.S2).getDistanceMode();
		sampleCapteurUltrason = new float[capteurUltrason.sampleSize()];
	}

	/**
	 * 
	 * @return la distance mesurée. Si la distance est supérieure à 60 ou infinie, elle est égal
	 *         à 60
	 */
	public float getDistance() {
		float distance;
		capteurUltrason.fetchSample(sampleCapteurUltrason, 0);

		distance = sampleCapteurUltrason[0] * 100;

		if (distance > maxDetectedDistance || distance == Float.POSITIVE_INFINITY) {
			distance = maxDetectedDistance;
		}

		return distance;
	}
}
