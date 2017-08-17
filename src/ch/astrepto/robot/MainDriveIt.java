package ch.astrepto.robot;

import java.sql.Time;
import java.util.Date;

import javax.swing.Timer;

import lejos.hardware.Sound;

public class MainDriveIt {

	public static void main(String[] args) {

		// � faire avant de d�poser le robot sur la piste
		RobotControls rob = new RobotControls();
		// annonce le d�but de la course
		Sound.beep();

		boolean boucle = true;
		long time = (int) System.currentTimeMillis();

		do{
			// GESTION DU RELEVE LUMINEUX DE LA PISTE
			// Est maj si pas "en train de passer le carrefour" et si pas
			// "initialisation d'un
			// d�passement"
			if (!Track.inCrossroads && !Track.overtaking) {
				rob.updateLightIntensity();
			}

			// GESTION DE LA DIRECTION AUTOMATIQUE
			// Est maj si pas "en train de passer le crossroads", si pas "arriv� au
			// crossroads",
			// si pas "initialisation d'un d�passement" et si "en train de suivre la
			// piste"
			if (!Track.inCrossroads && !Track.crossroads && !Track.overtaking && Track.hangOnTrack) {
				rob.updateDirection();
			}

			// GESTION DE LA VITESSE AUTOMATIQUE
			// Est maj si pas "intialisation d'un d�passement"
			if (!Track.overtaking) {
				rob.updateSpeed();
			}

			// GESTION DE L'ARRIVEE AU CROISEMENT
			// Est maj si "arriv� au crossroads" mais pas "en train de passer le
			// crossroads"
			if (Track.crossroads && !Track.inCrossroads) {
				rob.crossroads();
			}

			// GESTION A L'INTERIEUR DU CROISEMENT
			// Est maj si "en train de passer le crossroads"
			if (Track.inCrossroads) {
				// on attends de l'avoir pass� pour red�marrer les fonctions de
				// direction
				rob.crossroadsEnd();
			}

			// GESTION DES DEPASSEMENTS
			// Est maj si "initialisation d'un d�passement"
			if (Track.overtaking) {
				rob.overtaking();
			}
			
			/**
			 * GESTION DE LA FIN DES DEPASSEMENTS
			 * Est maj si pas "accroch� � la piste"
			 */
			if(!Track.hangOnTrack){
				rob.overtakingEnd(Track.overtaking);
			}
			
			// GESTION DE L'ARRET DE LA COURSE
			// le robot s'arr�te apr�s 2 min 30 (et quelques secondes)
			time = System.currentTimeMillis() - time;
			if(time >= 153 * 1000)
				boucle = false;
			
		}while(boucle);

	}
}
