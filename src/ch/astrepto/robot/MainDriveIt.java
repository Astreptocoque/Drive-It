package ch.astrepto.robot;

import lejos.hardware.Sound;

public class MainDriveIt {

	public static void main(String[] args) {

		// à faire avant de déposer le robot sur la piste
		RobotControls rob = new RobotControls();;
		
		// annonce le début de la course
		Sound.beep();
		

		boolean boucle = true;
		long time1 = System.currentTimeMillis();
		long time2;
		long interval = 0;
		
		do {
			// GESTION DU RELEVE LUMINEUX DE LA PISTE
			// Est maj si pas "en train de passer le carrefour" et si pas
			// "initialisation d'un
			// dépassement"
			if (!Track.inCrossroads && !Track.overtaking) {
				rob.updateLightIntensity();
			}

			// GESTION DE LA DIRECTION AUTOMATIQUE
			// Est maj si pas "en train de passer le crossroads", si pas "arrivé au
			// crossroads",
			// si pas "initialisation d'un dépassement" et si "en train de suivre la
			// piste"
			if (!Track.inCrossroads && !Track.crossroads && !Track.overtaking && Track.hangOnTrack) {
				// si verifiyFreeWay est vrai, l'ultrason ne tourne pas avec les
				// roues
				rob.updateDirection(Track.verifiyFreeWay);
			}

			// GESTION DE LA VITESSE AUTOMATIQUE
			// Est maj si pas "intialisation d'un dépassement" et si pas "vérification
			// peut dépasser")
			if (!Track.overtaking && !Track.verifiyFreeWay && !Track.ultrasonicRepositioning) {
				rob.updateSpeed();
			}

			// GESTION DE L'ARRIVEE AU CROISEMENT
			// Est maj si "arrivé au crossroads" mais pas "en train de passer le
			// crossroads"
			if (Track.crossroads && !Track.inCrossroads) {
				rob.crossroads();
			}

			// GESTION A L'INTERIEUR DU CROISEMENT
			// Est maj si "en train de passer le crossroads"
			if (Track.inCrossroads) {
				// on attends de l'avoir passé pour redémarrer les fonctions de
				// direction
				rob.crossroadsEnd();
			}

			if(!Track.crossroads && !Track.verifiyFreeWay && Track.hangOnTrack && !Track.ultrasonicRepositioning){
				rob.isThereAnOvertaking();
			}
			
			// GESTION DE LA VERIFICATION POUR PASSER SUR L'AUTRE VOIE (VOIE LIBRE)
			// Est maj si "il faut vérifier le chemin"
			if (Track.verifiyFreeWay) {
				rob.freeWay();
			}

			// GESTION DES DEPASSEMENTS
			// Est maj si "initialisation d'un dépassement"
			if (Track.overtaking) {
				rob.overtaking();
			}

			// GESTION DE LA FIN DES DEPASSEMENTS
			// Est maj si pas "accroché à la piste"
			if (!Track.hangOnTrack) {
				rob.overtakingEnd();
			}

			// GESTION DE L'ARRET DE LA COURSE
			// le robot s'arrête après 2 min 30 (et quelques secondes)
			time2 = System.currentTimeMillis();
			interval += time2 - time1;
			time1 = time2;
			if (interval >= 155l * 1000l){
				boucle = false;
			}else{
				
			}
		} while (boucle);

		rob.robotStop();

	}
}
