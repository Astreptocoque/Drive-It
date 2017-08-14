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
			// on prend l'intensité actuelle et on ramène la valeur minimum à zéro
			float intensity = color.getIntensity();

			// si on est au croisement (+3 pour les variations lumineuses)
			if (intensity <= ColorSensor.trackCrossingValue + 3)
				// on indique qu'on est au croisement
				Track.crossing = true;

			// si on n'est pas au croisement et si le précédant mvt est fini
			if (!Track.crossing && directionMotor.previousMoveComplete()
					&& ultrasonicMotor.previousMoveComplete() ) {
				// l'angle est déterminé par la situation du robot sur la piste
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
		// si on est arrivé au croisement
		if (Track.crossing && !Track.currentlyCrossing) {

			// n'est pas mis à la même condition juste en dessous pour accélérer le
			// freinage (sinon lent à cause de goTo)
			if (Track.trackPart == -1)
				// arrête le robot
				tractionMotor.move(false);

			// indique qu'on est dans le croisement
			Track.currentlyCrossing = true;
			tractionMotor.resetTacho();
			// les roues se remettent droites
			int angle = 0;
			ultrasonicMotor.goTo(-angle, true);
			directionMotor.goTo(angle);

			// si on est au croisement à priorité
			if (Track.trackPart == -1) {
				// lance le balayage de priorité
				crossing();
			}
		}
		// si on est actuellement dans le croisement
		if (Track.currentlyCrossing) {
			// on attends de l'avoir passé pour redémarrer les fonctions de direction
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
	 * méthode d'action a effectué pour passer le croisement
	 */
	private void crossing() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand côté
		if (Track.trackSide == 1) {
			// ArcTan de opposé (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Track.crossingDistance + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit côté
		else {
			startDetectionAngle = Math.atan((Track.crossingDistance - 6d) / (Track.crossingDistance + 8d))
					* 180d / Math.PI;
			endDetectionAngle = Math.atan((Track.crossingDistance - 6d + 40d) / 8d) * 180d / Math.PI;
		}

		// on transforme au préalable les ° du cercle en ° de l'ultrason
		startDetectionAngle = UltrasonicMotor.maxAngle / 90 * startDetectionAngle;
		endDetectionAngle = UltrasonicMotor.maxAngle / 90 * endDetectionAngle;

		// System.out.println(startDetectionAngle);
		// System.out.println(endDetectionAngle);

		// l'ultrason se rend au début de son tracé de mesure
		ultrasonicMotor.goTo((int) startDetectionAngle, false);
		ultrasonicMotor.waitComplete();

		// on commence la detection
		boolean blockedTrack = true;
		int sens = 1;
		float distance;
		boolean vehicle = false;

		// on répète tant que la piste n'est pas libre
		while (blockedTrack) {

			// l'ultrason boug
			if (sens == 1)
				ultrasonicMotor.goTo((int) endDetectionAngle, false);
			else
				ultrasonicMotor.goTo((int) startDetectionAngle, false);

			while (!ultrasonicMotor.previousMoveComplete()) {
				distance = ultrasonic.getDistance();
				// si on détecte un véhicule
				if (distance <= 50)
					vehicle = true;
			}
			// à la fin de la détection, on regarde si un véhicule a été détecté
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

		// on relève la couleur du sol
		if (color.getIntensity() >= ColorSensor.trackMaxValue - 15)
			// si c'est le blanc, partie -1
			Track.trackPart = -1;
		else
			// sinon, partie 1
			Track.trackPart = 1;

		// on commence toujours sur le grand côté
		Track.trackSide = 1;
	}

}
