package ch.astrepto.robot.capteurs;

import java.util.ArrayList;
import java.util.Collections;

import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.NXTUltrasonicSensor;
import lejos.robotics.SampleProvider;

public class CapteurUltrason {

	private SampleProvider capteurUltrason;
	private float[] sampleCapteurUltrason;

	
	public CapteurUltrason(){
		capteurUltrason = new NXTUltrasonicSensor(SensorPort.S2).getDistanceMode();
		sampleCapteurUltrason = new float[capteurUltrason.sampleSize()];
	}
	
	public float getDistance() {
		float distance;
		capteurUltrason.fetchSample(sampleCapteurUltrason, 0);
		
		distance = sampleCapteurUltrason[0] *100;
		
		if(distance > 60 || distance == Float.POSITIVE_INFINITY){
			distance = 60;
		}
		
		return distance;
	}
}
