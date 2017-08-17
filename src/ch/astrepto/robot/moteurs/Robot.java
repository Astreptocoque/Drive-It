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
	private static float intensity = 0;

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

		// GESTION DU RELEVE LUMINEUX DE LA PISTE
		// Est maj si pas "en train de passer le carrefour" et si pas "initialisation d'un
		// dépassement"
		if (!Track.inCrossroads && !Track.overtaking) {
			updateLightIntensity();
		}

		// GESTION DE LA DIRECTION AUTOMATIQUE
		// Est maj si pas "en train de passer le crossroads", si pas "arrivé au crossroads"
		// et si pas "initialisation d'un dépassement"
		if (!Track.inCrossroads && !Track.crossroads && !Track.overtaking) {
			updateDirection();
		}

		// GESTION DE LA VITESSE AUTOMATIQUE
		// Est maj si pas "intialisation d'un dépassement"
		if (!Track.overtaking) {
			updateSpeed();
		}
		// GESTION DE L'ARRIVEE AU CROISEMENT
		// Est maj si "arrivé au crossroads" mais pas "en train de passer le crossroads"
		if (Track.crossroads && !Track.inCrossroads) {
			crossroads();
		}

		// GESTION A L'INTERIEUR DU CROISEMENT
		// Est maj si "en train de passer le crossroads"
		if (Track.inCrossroads) {
			// on attends de l'avoir passé pour redémarrer les fonctions de direction
			detectEndCrossing();
		}
	}

	private void crossroads() {
		// n'est pas mis à la même condition juste en dessous pour accélérer le
		// freinage (sinon lent à cause de goTo)
		if (Track.trackPart == -1)
			// arrête le robot
			tractionMotor.move(false);

		// indique qu'on est en train de passer le croisement
		Track.inCrossroads = true;
		tractionMotor.resetTacho();
		// les roues se remettent droites
		int angle = 0;
		ultrasonicMotor.goTo(-angle, true);
		directionMotor.goTo(angle);

		// si on est au croisement à priorité
		if (Track.trackPart == -1) {
			// lance le balayage de priorité
			waitRightPriorityOk();
			ultrasonicMotor.goTo(0, false);
			tractionMotor.move(true);
		}
	}

	private void detectEndCrossing() {
		// on attends de l'avoir passé pour redémarrer les fonctions de direction
		if (tractionMotor.getTachoCount() >= Track.crossroadsDistance / TractionMotor.cmInDegres) {
			Track.inCrossroads = false;
			Track.crossroads = false;
			Track.justAfterCrossroads = true;
			Track.changeTrackPart();
			Track.changeTrackSide();
			tractionMotor.resetTacho();
		}
	}

	/**
	 * attend que la priorité de droite soit ok pour continuer
	 */
	private void waitRightPriorityOk() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand côté
		if (Track.trackSide == 1) {
			// ArcTan de opposé (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Track.crossroadsDistance + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit côté
		else {
			startDetectionAngle = Math
					.atan((Track.crossroadsDistance - 6d) / (Track.crossroadsDistance + 8d)) * 180d
					/ Math.PI;
			endDetectionAngle = Math.atan((Track.crossroadsDistance - 6d + 40d) / 8d) * 180d / Math.PI;
		}

		// on transforme au préalable les ° du cercle en ° de l'ultrason
		startDetectionAngle = UltrasonicMotor.maxDegree / 90 * startDetectionAngle;
		endDetectionAngle = UltrasonicMotor.maxDegree / 90 * endDetectionAngle;

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
	}

	private void updateDirection() {
		// Maj la direction si "le précédent mvt est fini"
		if (directionMotor.previousMoveComplete() && ultrasonicMotor.previousMoveComplete()) {
			// l'angle est déterminé par la situation du robot sur la piste
			int angle = directionMotor.determineAngle(intensity);

			// si on est juste après le croisement, l'angle est divisé par 2
			// pour atténuer la reprise de piste
			if (Track.justAfterCrossroads) {
				angle /= 2;
				Track.justAfterCrossroads = false;
			}

			ultrasonicMotor.goTo(-angle, true);
			directionMotor.goTo(angle);
		}
	}

	private void updateSpeed() {
		float speed = tractionMotor.determineSpeed(ultrasonic.getDistance());
		tractionMotor.setSpeed(speed);
	}

	private void updateLightIntensity() {
		// Relève la valeur lumineuse actuelle
		intensity = color.getIntensity();

		// Détection du carrefour (+3 pour les variations lumineuses)
		if (intensity <= ColorSensor.trackCrossingValue + 3)
			// Indique qu'on est arrivé au carrefour
			Track.crossroads = true;
	}

	private void overtaking() {
		// UPDATE DIRECTION

		// règle l'angle que les roues doivent prendre pour changer de côté
		int angle;
		if (Track.trackSide == -1) {
			angle = 0;
		} else {
			if (Track.trackPart == 1) {
				// - arcsin(empatement / petit rayon)
				angle = -(int) (Math
						.asin(DirectionMotor.wheelBase
								/ (Track.smallRadius + DirectionMotor.wheelBase))
						* 180d / Math.PI);
			} else {
				// arcsin(empatement / petit rayon)
				angle = (int) (Math
						.asin(DirectionMotor.wheelBase
								/ (Track.smallRadius + DirectionMotor.wheelBase))
						* 180d / Math.PI);
			}
		}
		directionMotor.goTo(angle);

		// la direction est désactivée, ensuite
		if (tractionMotor.getTachoCount() >= (Track.smallRadius + TractionMotor.wheelSpacing) * 2 * Math.PI
				/ TractionMotor.cmInDegres) {
			directionMotor.goTo(0);
		}
		// ensuite
		// detecte color bleu
		// réactive direction

	}

	private void updateTrackInfos() {
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
