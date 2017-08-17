package ch.astrepto.robot;

public class Track {

	public static boolean crossroads = false;
	public static int trackSide; // 1 si grand, -1 si petit
	public static int trackPart; // 1 c�t� avec priorit� de droite, -1 c�t� prioritaire
	public static boolean inCrossroads = false;
	public static boolean overtaking = false;
	// var permettant d'att�nuer l'angle d�tect� juste apr�s le croisement et au d�marrage
	public static boolean justAfterCrossroads = true;
	public final static float crossroadsDistance = 30; // en cm
	public final static float smallRadius = 10;
	public final static float largeRadius = 35;
	// longueur du croisement � modifier (premi�re valeur)

	public static void changeTrackSide() {
		trackSide *= -1;
	}

	public static void changeTrackPart() {
		trackPart *= -1;
	}
}
