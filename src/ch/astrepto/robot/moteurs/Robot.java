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
	 * met à jour l'angle de la rotation du robot pour le prochain mouvement
	 */
	public void updateDirection() {
		// on prend l'intensité actuelle et on ramène la valeur minimum à zéro
		float intensite = color.getIntensity();
		
		// ce code n'est pas dans la bonne méthode mais c'est pour acceleré le freinage
		if(Piste.crossing && ! Piste.currentlyCrossing)
			tractionMotor.move(false);

		// si on n'est pas au croisement et si le précédant mvt est fini
		 
		if (!Piste.crossing && directionMotor.previousMoveComplete() && ultrasonicMotor.previousMoveComplete()) {

			// l'angle est déterminé par la situation du robot sur la piste
			int angle = directionMotor.determineAngle(intensite);

			// les roues tournent de même que l'ultrason
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
		// si on est arrivé au croisement
		if (Piste.crossing && !Piste.currentlyCrossing) {
			tractionMotor.move(false);
			Piste.currentlyCrossing = true;
			Sound.beepSequence();
			tractionMotor.resetTacho();

			// si on est au croisement à priorité
			if (Piste.trackPart == -1) {
				crossing();
			}
			// sinon on redémarre
			else{
				tractionMotor.move(true);
			}
		}
		// si on est actuellement dans le croisement
		if (Piste.currentlyCrossing) {
			// on attends de l'avoir passé pour redémarrer les fonctions de direction
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
	 * met à jour la vitesse de la traction en fonction de la distance
	 */
	public void updateSpeed() {
		float distance = ultrasonic.getDistance();

		tractionMotor.determineSpeed(distance);
	}

	public void updateTrackInfos() {
		// valeur 0 = partieHuit, valeur 1 = cotePiste

		// on relève la couleur du sol
		if (color.getIntensity() >= CapteurCouleur.trackMaxValue - 15)
			// si c'est le blanc, partie -1
			Piste.trackPart = -1;
		else
			// sinon, partie 1
			Piste.trackPart = 1;

		// on commence toujours sur le grand côté
		Piste.trackSide = 1;
	}

	private void crossing() {
		double startDetectionAngle;
		double endDetectionAngle;
		// si on est du grand côté
		if (Piste.trackSide == 1) {
			// ArcTan de opposé (6cm) sur adjacent (long.Piste + 8d, la
			// profondeur du capteur dans le robot. Le tout *180/pi car
			// la atan renvoi un radian
			startDetectionAngle = Math.atan(6d / (Piste.crossingDistance + 8d)) * 180d / Math.PI;
			endDetectionAngle = Math.atan(40d / 8d) * 180d / Math.PI;
		}
		// si on est du petit côté
		else {
			startDetectionAngle = Math.atan((Piste.crossingDistance - 6d) / (Piste.crossingDistance + 8d))
					* 180d / Math.PI;
			endDetectionAngle = Math.atan((Piste.crossingDistance - 6d + 40d) / 8d) * 180d / Math.PI;
		}

		// on transforme au préalable les ° du cercle en ° de l'ultrason
		startDetectionAngle = MoteurUltrason.maxAngle / 90 * startDetectionAngle;
		endDetectionAngle = MoteurUltrason.maxAngle / 90 * endDetectionAngle;

//		System.out.println(startDetectionAngle);
//		System.out.println(endDetectionAngle);
		
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
			if (sens == 1) {
				ultrasonicMotor.goTo((int) endDetectionAngle, false);
				Sound.beepSequence();
			} else {
				ultrasonicMotor.goTo((int) startDetectionAngle, false);
				Sound.beepSequenceUp();
			}

			while (!ultrasonicMotor.previousMoveComplete()) {
				distance = ultrasonic.getDistance();
				// si on détecte un véhicule
				if (distance <= 50) {
					vehicle = true;
				}
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
}
