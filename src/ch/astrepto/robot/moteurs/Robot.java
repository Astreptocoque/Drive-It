package ch.astrepto.robot.moteurs;

import ch.astrepto.robot.Piste;
import ch.astrepto.robot.capteurs.CapteurCouleur;
import ch.astrepto.robot.capteurs.CapteurUltrason;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.lcd.LCD;

public class Robot {

	MoteurDirection directionMotor;
	MoteurUltrason ultrasonicMotor;
	MoteurTraction tractionMotor;
	CapteurCouleur color;
	CapteurUltrason ultrasonic;

	public Robot() {
		directionMotor = new MoteurDirection();
		ultrasonicMotor = new MoteurUltrason();
		color = new CapteurCouleur();
		ultrasonic = new CapteurUltrason();
		updateTrackInfos();
		// piste = new Piste(1,1);
		tractionMotor = new MoteurTraction();

	}

	/**
	 * met � jour l'angle de la rotation du robot pour le prochain mouvement
	 */
	public void updateDirection() {
		// on prend l'intensit� actuelle et on ram�ne la valeur minimum � z�ro
		float intensite = color.getIntensity();
		
		// ce code n'est pas dans la bonne m�thode mais c'est pour acceler� le freinage
		if(Piste.crossing && ! Piste.currentlyCrossing)
			tractionMotor.move(false);

		// si on n'est pas au croisement et si le pr�c�dant mvt est fini
		 
		if (!Piste.crossing && directionMotor.previousMoveComplete() && ultrasonicMotor.previousMoveComplete()) {

			// l'angle est d�termin� par la situation du robot sur la piste
			int angle = directionMotor.determineAngle(intensite);

			// les roues tournent de m�me que l'ultrason
			ultrasonicMotor.goTo(-angle, true);
			directionMotor.goTo(angle);

		}
		// si on arrive au croisement
		else if (Piste.crossing && !Piste.currentlyCrossing) {
			// on remet les roues droites
			ultrasonicMotor.goTo(0, true);
			directionMotor.goTo(0);
		}

	}

	public void updateMove() {
		// si on est arriv� au croisement
		if (Piste.crossing && !Piste.currentlyCrossing) {
			tractionMotor.move(false);
			Piste.currentlyCrossing = true;
			Sound.beepSequence();
			tractionMotor.resetTacho();

			// si on est au croisement � priorit�
			if (Piste.trackPart == -1) {
				crossing();
			}
			// sinon on red�marre
			else{
				tractionMotor.move(true);
			}
		}
		// si on est actuellement dans le croisement
		if (Piste.currentlyCrossing) {
			// on attends de l'avoir pass� pour red�marrer les fonctions de direction
			if (tractionMotor.getTachoCount() >= Piste.crossingDistance / MoteurTraction.cmInDegres) {
				Piste.currentlyCrossing = false;
				Piste.crossing = false;
				Piste.changeTrackPart();
				Piste.changeTrackSide();
				LCD.clear();
				LCD.drawString("TrackPart " + Piste.trackPart, 0, 0);
				LCD.drawString("TrackSide " + Piste.trackSide, 0, 1);
			}
		}
	}

	/**
	 * met � jour la vitesse de la traction en fonction de la distance
	 */
	public void updateSpeed() {
		float distance = ultrasonic.getDistance();

		tractionMotor.determineSpeed(distance);
	}

	public void updateTrackInfos() {
		// valeur 0 = partieHuit, valeur 1 = cotePiste

		// on rel�ve la couleur du sol
		if (color.getIntensity() >= CapteurCouleur.trackMaxValue - 15)
			// si c'est le blanc, partie -1
			Piste.trackPart = -1;
		else
			// sinon, partie 1
			Piste.trackPart = 1;

		// on commence toujours sur le grand c�t�
		Piste.trackSide = 1;
	}

	private void crossing() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand c�t�
		if (Piste.trackSide == 1) {
			// ArcTan de oppos� (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Piste.crossingDistance + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit c�t�
		else {
			startDetectionAngle = Math.atan((Piste.crossingDistance - 6d) / (Piste.crossingDistance + 8d))
					* 180d / Math.PI;
			endDetectionAngle = Math.atan((Piste.crossingDistance - 6d + 40d) / 8d) * 180d / Math.PI;
		}

		// on transforme au pr�alable les � du cercle en � de l'ultrason
		startDetectionAngle = MoteurUltrason.maxAngle / 90 * startDetectionAngle;
		endDetectionAngle = MoteurUltrason.maxAngle / 90 * endDetectionAngle;

//		System.out.println(startDetectionAngle);
//		System.out.println(endDetectionAngle);
		
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
			if (sens == 1) {
				ultrasonicMotor.goTo((int) endDetectionAngle, false);
				Sound.beepSequence();
			} else {
				ultrasonicMotor.goTo((int) startDetectionAngle, false);
				Sound.beepSequenceUp();
			}

			while (!ultrasonicMotor.previousMoveComplete()) {
				distance = ultrasonic.getDistance();
				// si on d�tecte un v�hicule
				if (distance <= 50) {
					vehicle = true;
				}
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
}
