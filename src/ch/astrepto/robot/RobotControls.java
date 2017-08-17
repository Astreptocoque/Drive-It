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
	 * Gestion du carrefour Une fois le carrefour d�tect�, cette section r�agit en fonction du
	 * c�t� du croisement
	 */
	public void crossroads() {
		// n'est pas mis � la m�me condition juste en dessous pour acc�l�rer le
		// freinage (sinon lent � cause de goTo)
		if (Track.trackPart == -1)
			// arr�te le robot
			tractionMotor.move(false);

		// indique qu'on est en train de passer le croisement
		Track.inCrossroads = true;
		tractionMotor.resetTacho();
		// les roues se remettent droites
		int angle = 0;
		ultrasonicMotor.goTo(-angle, true);
		directionMotor.goTo(angle);

		// si on est au croisement � priorit�
		if (Track.trackPart == -1) {
			// lance le balayage de priorit�
			waitRightPriorityOk();
			ultrasonicMotor.goTo(0, false);
			tractionMotor.move(true);
		}
	}

	/**
	 * Gestion de la d�tection de la fin du carrefour D�tecte la fin du carrefour et maj les
	 * indications de piste
	 */
	public void crossroadsEnd() {
		// on attends de l'avoir pass� pour red�marrer les fonctions de direction
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
	 * Gestion de la priorit� de droite laisse continuer le robot seulement si aucun v�hicule
	 * devant avoir la priorit� n'est d�tect�
	 */
	private void waitRightPriorityOk() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand c�t�
		if (Track.trackSide == 1) {
			// ArcTan de oppos� (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Track.crossroadsLength + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit c�t�
		else {
			startDetectionAngle = Math.atan((Track.crossroadsLength - 6d) / (Track.crossroadsLength + 8d))
					* 180d / Math.PI;
			endDetectionAngle = Math.atan((Track.crossroadsLength - 6d + 40d) / 8d) * 180d / Math.PI;
		}

		// on transforme au pr�alable les � du cercle en � de l'ultrason
		startDetectionAngle = UltrasonicMotor.maxDegree / 90 * startDetectionAngle;
		endDetectionAngle = UltrasonicMotor.maxDegree / 90 * endDetectionAngle;

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
	}

	/**
	 * Gestion de la direction automatique une fois que la pr�c�dente direction est termin�e, la
	 * nouvelle est d�termin�e en fonction de l'intensit� lumineuse d�tect�e
	 */
	public void updateDirection() {
		// Maj la direction si "le pr�c�dent mvt est fini"
		if (directionMotor.previousMoveComplete() && ultrasonicMotor.previousMoveComplete()) {
			// l'angle est d�termin� par la situation du robot sur la piste
			int angle = directionMotor.determineAngle(intensity);

			// si on est juste apr�s le croisement, l'angle est divis� par 2
			// pour att�nuer la reprise de piste
			if (Track.justAfterCrossroads) {
				angle /= 2;
				Track.justAfterCrossroads = false;
			}

			ultrasonicMotor.goTo(-angle, true);
			directionMotor.goTo(angle);
		}
	}

	/**
	 * Gestion de la vitesse automatique la vitesse est d�termin�e en fonction de la distance en
	 * cm mesur�e
	 */
	public void updateSpeed() {
		float speed = tractionMotor.determineSpeed(ultrasonic.getDistance());
		tractionMotor.setSpeed(speed);
	}

	/**
	 * Gestion de la d�tection de l'intensit� lumineuse au sol Rel�ve l'intensit� lumineuse et
	 * d�tecte le croisement
	 */
	public void updateLightIntensity() {
		// Rel�ve la valeur lumineuse actuelle
		intensity = color.getIntensity();

		// D�tection du carrefour (+3 pour les variations lumineuses)
		if (intensity <= ColorSensor.trackCrossingValue + 3)
			// Indique qu'on est arriv� au carrefour
			Track.crossroads = true;
	}

	/**
	 * Gestion des d�passements s'occupe de faire tourner le robot � la bonne "inclinaison" pour
	 * lui faire rejoindre l'autre c�t� de la piste ATTENTION : le d�passement sous-entend
	 * uniquement le virage effectu� pour d�crocher la piste et pouvoir ensuite rejoindre
	 * l'autre c�t�. Du moment que le virage est fait, la variabe "d�passement" est fausse, mais
	 * "hangOnTrack" reste fausse jusqu'� qu'on est � nouveau rejoint la piste
	 */
	public void overtaking() {

		Track.hangOnTrack = false;

		// r�gle l'angle que les roues doivent prendre pour changer de c�t�
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
	 * Gestion de la fin du d�passement. Cette fin comprend 2 parties : la fin du virage pour
	 * rejoind l'autre c�t� et la fin du bout droit jusqu'� l'autre c�t�
	 * 
	 * @param part
	 *                partie de la fin du croisement. Vrai s'il faut fini le virage, faut s'il
	 *                faut rejoindre l'autre c�t�. La valeur de part est la valeur de
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
	 * Arr�te le robot � la fin
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
