package ch.astrepto.robot;

public class MainDriveIt {

	public static void main(String[] args) {

		// à faire avant de déposer le robot sur la piste
		RobotControls rob = new RobotControls();

		while (true) {
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
				rob.updateDirection();
			}

			// GESTION DE LA VITESSE AUTOMATIQUE
			// Est maj si pas "intialisation d'un dépassement"
			if (!Track.overtaking) {
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

			// GESTION DES DEPASSEMENTS
			// Est maj si "initialisation d'un dépassement"
			if (Track.overtaking) {
				rob.overtaking();
			}
		}

	}
}
