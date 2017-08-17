package ch.astrepto.robot.moteurs;

import ch.astrepto.robot.Track;
import ch.astrepto.robot.capteurs.ColorSensor;
import ch.astrepto.robot.capteurs.UltrasonicSensor;
import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.MotorPort;

public class TractionMotor {

	private EV3LargeRegulatedMotor motorLeft, rightMotor;
	private EV3LargeRegulatedMotor[] synchro;

	private static boolean isMoving = false;
	public final static float maxSpeed = 200f;
	public final static float cmInDegres = 0.037699112f;
	public final static float wheelSpacing = 9.5f;
	private final static float lastLimit = 10f; // après, le robot stop
	private final static float secondLimit = 15f; // jusqu'ici, le robot garde 75% de sa vitesse
	private final static float firstLimit = 30f; // passé cette limite, le robot est à plein
							// régime
	private final static float speedAtSecondLimit = 3f/4f;
	private static float currentSpeed;

	public TractionMotor() {
		motorLeft = new EV3LargeRegulatedMotor(MotorPort.B);
		rightMotor = new EV3LargeRegulatedMotor(MotorPort.C);

		synchro = new EV3LargeRegulatedMotor[1];
		synchro[0] = rightMotor;
		motorLeft.synchronizeWith(synchro);
		motorLeft.setAcceleration(2000);
		rightMotor.setAcceleration(2000);

		setSpeed(maxSpeed);
		move(true);

	}

	/**
	 * règle la vitesse et l'ajuste pour chaque roue de traction en fonction du virage
	 * 
	 * @param vitesseActuelle
	 */
	public void setSpeed(float vitesseActuelle) {

		float speedLeftMotor = 0;
		float speedRightMotor = 0;
		// on determine la nouvelle valeur de degré à tourner au robot
		// en fonction de l'endroit sur la piste et du nombre de degré que tourne le robot
		if (Track.trackSide == 1 && Track.trackPart == 1) {
			speedRightMotor = vitesseActuelle;
			// la vitesse en fonction du rayon du centre de la piste
			speedLeftMotor = (Track.largeRadius - wheelSpacing) * vitesseActuelle / Track.largeRadius;
			// puis en fonction du degré de rotation
			speedLeftMotor = vitesseActuelle - ((vitesseActuelle - speedLeftMotor)
					/ DirectionMotor.maxDegree * DirectionMotor.getCurrentDegree());
		} else if (Track.trackSide == -1 && Track.trackPart == -1) {
			speedLeftMotor = Track.smallRadius * vitesseActuelle / (Track.smallRadius + wheelSpacing);
			speedLeftMotor = vitesseActuelle - ((vitesseActuelle - speedLeftMotor)
					/ DirectionMotor.maxDegree * DirectionMotor.getCurrentDegree());
			speedRightMotor = vitesseActuelle;
		} else if (Track.trackSide == 1 && Track.trackPart == -1) {
			speedLeftMotor = (Track.largeRadius - wheelSpacing) * vitesseActuelle / Track.largeRadius;
			speedLeftMotor = vitesseActuelle - ((vitesseActuelle - speedLeftMotor)
					/ DirectionMotor.maxDegree * DirectionMotor.getCurrentDegree());
			speedRightMotor = vitesseActuelle;
		} else if (Track.trackSide == -1 && Track.trackPart == 1) {
			speedRightMotor = vitesseActuelle;
			speedLeftMotor = Track.smallRadius * vitesseActuelle / (Track.smallRadius + wheelSpacing);
			speedLeftMotor = vitesseActuelle - ((vitesseActuelle - speedLeftMotor)
					/ DirectionMotor.maxDegree * DirectionMotor.getCurrentDegree());

		}

		// set la vitesse
		rightMotor.setSpeed(speedRightMotor);
		motorLeft.setSpeed(speedLeftMotor);

		// met à jour la vitesse actuelle
		currentSpeed = vitesseActuelle;

		if (vitesseActuelle == 0)
			isMoving = false;

		if (vitesseActuelle > 0 && isMoving == false) {
			// on indique que le robot est en marche
			isMoving = true;
			// demarre le robot
			move(true);
		}
	}

	public float determineSpeed(float distance) {
		float speed;

		if (distance > firstLimit) {
			speed = maxSpeed;
		} else if (distance <= firstLimit && distance > secondLimit) {
			speed = (speedAtSecondLimit * maxSpeed) + (maxSpeed - (speedAtSecondLimit * maxSpeed)) / (firstLimit - secondLimit)
					* (distance - secondLimit);
		} else if (distance <= secondLimit && distance > lastLimit) {
			speed = (speedAtSecondLimit * maxSpeed) / (secondLimit - lastLimit) * (distance - lastLimit);
		} else {
			speed = 0;
		}
		
		return speed;
	}

	/**
	 * 
	 * @param move
	 *                true pour démarrer, false pour arrêter
	 */
	public void move(boolean move) {
		motorLeft.startSynchronization();

		if (move) {
			motorLeft.backward();
			rightMotor.backward();
		} else {
			motorLeft.stop();
			rightMotor.stop();
		}

		motorLeft.endSynchronization();
	}	
	
	public void resetTacho() {
		motorLeft.resetTachoCount();
		rightMotor.resetTachoCount();
	}

	public int getTachoCount() {
		return (motorLeft.getTachoCount() + rightMotor.getTachoCount()) / 2 * -1;
	}
}
