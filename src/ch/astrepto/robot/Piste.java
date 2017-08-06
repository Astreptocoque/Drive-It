package ch.astrepto.robot;

public class Piste {

	
	public static boolean crossing = false;
	public static int trackSide; // 1 si grand, -1 si petit
	public static int trackPart; // 1 c�t� avec priorit� de droite, -1 c�t� prioritaire
	public static boolean currentlyCrossing = false;
	public final static float crossingDistance = 30; // en cm
	public final static float smallRadius = 10;
	public final static float largeRadius = 35;
	//longueur du croisement � modifier (premi�re valeur)

	public static void changeTrackSide() {
		trackSide *= -1;
	}

	public static void changeTrackPart() {
		trackPart *= -1;
	}
}
