package ch.astrepto.robot.capteurs;

import ch.astrepto.robot.Piste;
import lejos.hardware.Sound;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorMode;

public class CapteurCouleur {
	
	private EV3ColorSensor colorSensor;
	private SensorMode colorSensorMode;
	private float[] sampleColorSensor;
	
	public static final float trackMaxValue = 62; // blanc
	public static final float trackMinValue = 11; // bleu foncé
	public static final float trackCrossingValue = 2; // ligne noire
	
	public CapteurCouleur(){
		colorSensor = new EV3ColorSensor(SensorPort.S3);
		colorSensorMode = colorSensor.getRedMode();
		sampleColorSensor = new float[colorSensorMode.sampleSize()];
	}
	
	/**
	 * 
	 * @return la valeur du capteur, entre 0 et 100
	 */
	public float getIntensity() {
		colorSensorMode.fetchSample(sampleColorSensor, 0);
		// on change l'échelle de 0 à 1 en 0 à 100
		float intensity = sampleColorSensor[0] * 100;
		
		// ajustement de la valeur et détection év. du croisement
		if (intensity > trackMaxValue)
			intensity = trackMaxValue;
		// si on est au croisement (+3 pour les variations lumineuses)
		else if (intensity <= trackCrossingValue + 3)
			// on indique qu'on est au croisement
			Piste.crossing = true;
		
		return intensity;
	}
}
