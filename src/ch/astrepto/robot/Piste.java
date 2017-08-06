package ch.astrepto.robot;

public class Piste {

	
	public static boolean crossing = false;
	public static int trackSide; // 1 si grand, -1 si petit
	public static int trackPart; // 1 côté avec priorité de droite, -1 côté prioritaire
	public static boolean currentlyCrossing = false;
	public final static float crossingDistance = 30; // en cm
	public final static float smallRadius = 10;
	public final static float largeRadius = 35;
	//longueur du croisement à modifier (première valeur)

	public static void changeTrackSide() {
		trackSide *= -1;
	}

	public static void changeTrackPart() {
		trackPart *= -1;
	}
}
