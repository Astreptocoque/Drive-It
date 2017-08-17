package ch.astrepto.robot;

import ch.astrepto.robot.capteurs.ColorSensor;
import ch.astrepto.robot.capteurs.UltrasonicSensor;
import ch.astrepto.robot.moteurs.DirectionMotor;
import ch.astrepto.robot.moteurs.TractionMotor;
import ch.astrepto.robot.moteurs.UltrasonicMotor;
import lejos.utility.Delay;

public class RobotControls {

	private DirectionMotor directionMotor;
	private UltrasonicMotor ultrasonicMotor;
	private TractionMotor tractionMotor;
	private ColorSensor color;
	private UltrasonicSensor ultrasonic;
	private static float intensity = 0;

	public RobotControls() {
		directionMotor = new DirectionMotor();
		ultrasonicMotor = new UltrasonicMotor();
		color = new ColorSensor();
		ultrasonic = new UltrasonicSensor();
		Track.updateTrackInfos(color.getIntensity());
		// piste = new Piste(1,1);
		tractionMotor = new TractionMotor();
	}

	/**
	 * Gestion du carrefour Une fois le carrefour détecté, cette section réagit en fonction du
	 * côté du croisement
	 */
	public void crossroads() {
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

	/**
	 * Gestion de la détection de la fin du carrefour Détecte la fin du carrefour et maj les
	 * indications de piste
	 */
	public void crossroadsEnd() {
		// on attends de l'avoir passé pour redémarrer les fonctions de direction
		if (tractionMotor.getTachoCount() >= Track.crossroadsLength / TractionMotor.cmInDegres) {
			Track.inCrossroads = false;
			Track.crossroads = false;
			Track.justAfterCrossroads = true;
			Track.changeTrackPart();
			Track.changeTrackSide();
			tractionMotor.resetTacho();
		}
	}

	/**
	 * Gestion de la priorité de droite laisse continuer le robot seulement si aucun véhicule
	 * devant avoir la priorité n'est détecté
	 */
	private void waitRightPriorityOk() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand côté
		if (Track.trackSide == 1) {
			// ArcTan de opposé (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Track.crossroadsLength + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit côté
		else {
			startDetectionAngle = Math.atan((Track.crossroadsLength - 6d) / (Track.crossroadsLength + 8d))
					* 180d / Math.PI;
			endDetectionAngle = Math.atan((Track.crossroadsLength - 6d + 40d) / 8d) * 180d / Math.PI;
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

	/**
	 * Gestion de la direction automatique une fois que la précédente direction est terminée, la
	 * nouvelle est déterminée en fonction de l'intensité lumineuse détectée
	 */
	public void updateDirection() {
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

	/**
	 * Gestion de la vitesse automatique la vitesse est déterminée en fonction de la distance en
	 * cm mesurée
	 */
	public void updateSpeed() {
		float speed = tractionMotor.determineSpeed(ultrasonic.getDistance());
		tractionMotor.setSpeed(speed);
	}

	/**
	 * Gestion de la détection de l'intensité lumineuse au sol Relève l'intensité lumineuse et
	 * détecte le croisement
	 */
	public void updateLightIntensity() {
		// Relève la valeur lumineuse actuelle
		intensity = color.getIntensity();

		// Détection du carrefour (+3 pour les variations lumineuses)
		if (intensity <= ColorSensor.trackCrossingValue + 3)
			// Indique qu'on est arrivé au carrefour
			Track.crossroads = true;
	}

	/**
	 * Gestion des dépassements s'occupe de faire tourner le robot à la bonne "inclinaison" pour
	 * lui faire rejoindre l'autre côté de la piste ATTENTION : le dépassement sous-entend
	 * uniquement le virage effectué pour décrocher la piste et pouvoir ensuite rejoindre
	 * l'autre côté. Du moment que le virage est fait, la variabe "dépassement" est fausse, mais
	 * "hangOnTrack" reste fausse jusqu'à qu'on est à nouveau rejoint la piste
	 */
	public void overtaking() {

		Track.hangOnTrack = false;

		// règle l'angle que les roues doivent prendre pour changer de côté
		int angle;
		if (Track.trackSide == -1) {
			angle = 0;
		} else {
			// angle des roues en fonction du rayon
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
		tractionMotor.setSpeed(TractionMotor.currentSpeed);
	}

	/**
	 * Gestion de la fin du dépassement. Cette fin comprend 2 parties : la fin du virage pour
	 * rejoind l'autre côté et la fin du bout droit jusqu'à l'autre côté
	 * 
	 * @param part
	 *                partie de la fin du croisement. Vrai s'il faut fini le virage, faut s'il
	 *                faut rejoindre l'autre côté. La valeur de part est la valeur de
	 *                Track.overtaking
	 */
	public void overtakingEnd(boolean part) {
		if (part) {
			if (tractionMotor.getTachoCount() >= (Track.smallRadius + TractionMotor.wheelSpacing) * 2
					* Math.PI / TractionMotor.cmInDegres) {
				directionMotor.goTo(0);
				Track.overtaking = false;
			}
		} else {
			if (intensity <= (ColorSensor.trackMaxValue - 10)) {
				Track.hangOnTrack = true;
			}
		}
	}

	/**
	 * Arrête le robot à la fin
	 */
	public void robotStop() {
		// arret du robot
		tractionMotor.move(false);
		// remet les roues droites
		Delay.msDelay(500);
		directionMotor.goTo(0);
		// remet l'ultrason droit
		Delay.msDelay(500);
		ultrasonicMotor.goTo(0, false);
	}
}
