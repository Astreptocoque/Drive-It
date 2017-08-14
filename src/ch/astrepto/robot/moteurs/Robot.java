package ch.astrepto.robot.moteurs;

import ch.astrepto.robot.Track;
import ch.astrepto.robot.capteurs.ColorSensor;
import ch.astrepto.robot.capteurs.UltrasonicSensor;

public class Robot {

	DirectionMotor directionMotor;
	UltrasonicMotor ultrasonicMotor;
	TractionMotor tractionMotor;
	ColorSensor color;
	UltrasonicSensor ultrasonic;

	public Robot() {
		directionMotor = new DirectionMotor();
		ultrasonicMotor = new UltrasonicMotor();
		color = new ColorSensor();
		ultrasonic = new UltrasonicSensor();
		updateTrackInfos();
		// piste = new Piste(1,1);
		tractionMotor = new TractionMotor();

	}

	public void run() {

		// UPDATE DIRECTION
		// ne fait pas si dans le croisement
		if (!Track.currentlyCrossing) {
			// on prend l'intensit� actuelle et on ram�ne la valeur minimum � z�ro
			float intensity = color.getIntensity();

			// si on est au croisement (+3 pour les variations lumineuses)
			if (intensity <= ColorSensor.trackCrossingValue + 3)
				// on indique qu'on est au croisement
				Track.crossing = true;

			// si on n'est pas au croisement et si le pr�c�dant mvt est fini
			if (!Track.crossing && directionMotor.previousMoveComplete()
					&& ultrasonicMotor.previousMoveComplete() ) {
				// l'angle est d�termin� par la situation du robot sur la piste
				int angle = directionMotor.determineAngle(intensity);
				
				ultrasonicMotor.goTo(-angle, true);
				directionMotor.goTo(angle);
			}
		}
		// UPDATE SPEED
		float speed = tractionMotor.determineSpeed(ultrasonic.getDistance());
		tractionMotor.setSpeed(speed);

		// UPDATE MOVE
		updateMove();
	}

	private void updateMove() {
		// si on est arriv� au croisement
		if (Track.crossing && !Track.currentlyCrossing) {

			// n'est pas mis � la m�me condition juste en dessous pour acc�l�rer le
			// freinage (sinon lent � cause de goTo)
			if (Track.trackPart == -1)
				// arr�te le robot
				tractionMotor.move(false);

			// indique qu'on est dans le croisement
			Track.currentlyCrossing = true;
			tractionMotor.resetTacho();
			// les roues se remettent droites
			int angle = 0;
			ultrasonicMotor.goTo(-angle, true);
			directionMotor.goTo(angle);

			// si on est au croisement � priorit�
			if (Track.trackPart == -1) {
				// lance le balayage de priorit�
				crossing();
			}
		}
		// si on est actuellement dans le croisement
		if (Track.currentlyCrossing) {
			// on attends de l'avoir pass� pour red�marrer les fonctions de direction
			if (tractionMotor.getTachoCount() >= Track.crossingDistance / TractionMotor.cmInDegres) {
				Track.currentlyCrossing = false;
				Track.crossing = false;
				Track.justAfterCrossing = true;
				Track.changeTrackPart();
				Track.changeTrackSide();
				tractionMotor.resetTacho();
			}
		}
	}

	/**
	 * m�thode d'action a effectu� pour passer le croisement
	 */
	private void crossing() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand c�t�
		if (Track.trackSide == 1) {
			// ArcTan de oppos� (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Track.crossingDistance + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit c�t�
		else {
			startDetectionAngle = Math.atan((Track.crossingDistance - 6d) / (Track.crossingDistance + 8d))
					* 180d / Math.PI;
			endDetectionAngle = Math.atan((Track.crossingDistance - 6d + 40d) / 8d) * 180d / Math.PI;
		}

		// on transforme au pr�alable les � du cercle en � de l'ultrason
		startDetectionAngle = UltrasonicMotor.maxAngle / 90 * startDetectionAngle;
		endDetectionAngle = UltrasonicMotor.maxAngle / 90 * endDetectionAngle;

		// System.out.println(startDetectionAngle);
		// System.out.println(endDetectionAngle);

		// l'ultrason se rend au d�but de son trac� de mesure
		ultrasonicMotor.goTo((int) startDetectionAngle, false);
		ultrasonicMotor.waitComplete();

		// on commence la detection
		boolean blockedTrack = true;
		int sens = 1;
		float distance;
		boolean vehicle = false;

		// on r�p�te tant que la piste n'est pas libre
		while (blockedTrack) {

			// l'ultrason boug
			if (sens == 1)
				ultrasonicMotor.goTo((int) endDetectionAngle, false);
			else
				ultrasonicMotor.goTo((int) startDetectionAngle, false);

			while (!ultrasonicMotor.previousMoveComplete()) {
				distance = ultrasonic.getDistance();
				// si on d�tecte un v�hicule
				if (distance <= 50)
					vehicle = true;
			}
			// � la fin de la d�tection, on regarde si un v�hicule a �t� d�tect�
			if (vehicle) {
				vehicle = false;
				sens *= -1;
			}
			// sinon on sort de la boucle blocked track
			else {
				blockedTrack = false;
			}

		}
		ultrasonicMotor.goTo(0, false);
		tractionMotor.move(true);

	}

	public void updateTrackInfos() {
		// valeur 0 = partieHuit, valeur 1 = cotePiste

		// on rel�ve la couleur du sol
		if (color.getIntensity() >= ColorSensor.trackMaxValue - 15)
			// si c'est le blanc, partie -1
			Track.trackPart = -1;
		else
			// sinon, partie 1
			Track.trackPart = 1;

		// on commence toujours sur le grand c�t�
		Track.trackSide = 1;
	}

}
