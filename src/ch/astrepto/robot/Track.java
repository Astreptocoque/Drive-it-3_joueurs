package ch.astrepto.robot;

import ch.astrepto.robot.capteurs.ColorSensor;
import ch.astrepto.robot.moteurs.TractionMotor;

public class Track {

	// VARIABLES POUR LA SITUATION SUR LA PISTE
	public static int trackSide; // 1 si grand, -1 si petit
	public static int trackPart; // 1 c�t� avec priorit� de droite, -1 c�t� prioritaire
	public final static float smallRadius = 10;
	public final static float largeRadius = 35;
	public final static float gradientWidth = 8;

	// VARIABLES POUR LE CARREFOUR
	public static boolean crossroads = false; // si arriv� au carrrefour
	public static boolean inCrossroads = false; // si en train de passer le carrefour
	// var permettant d'att�nuer l'angle d�tect� juste apr�s le carrefour et au d�marrage
	public static boolean justAfterCrossroads = true;

	// VARIABLES POUR LES DEPASSEMENTS
	public static boolean verifiyFreeWay = false; // true si on lance la proc�dure de v�rification
	public static boolean overtaking = false; // si en train de d�passer
	public static int overtakingPart = 0;
	public static boolean hangOnTrack = true; // si en train de suivre la piste (avec le
							// d�grad�)
	public static boolean ultrasonicRepositioning = false;
	public final static float crossroadsLength = 30; // en cm
	// longueur minimal d'un c�t� de la piste
	public final static float trackPartLength = (float) (((smallRadius + gradientWidth) * 2 * Math.PI)/TractionMotor.cmInDegres);
	// le bout droit + le 1/4 du petit virage de la piste + une marge de 10, en degr�s
	public final static float overtakingLength = (float) ((crossroadsLength
			+ ((smallRadius + gradientWidth / 2) * 2 * Math.PI) / 4) + 10) / TractionMotor.cmInDegres;

	/**
	 * Change le c�t� de la piste
	 */
	public static void changeTrackSide() {
		trackSide *= -1;
	}

	/**
	 * Change la partie de la piste (du huit)
	 */
	public static void changeTrackPart() {
		trackPart *= -1;
	}

	/**
	 * Gestion des informations de la piste Appel�e en d�but de programme, cette m�thode permet
	 * au robot de se situer sur la piste. Le robot doit �tre plac� sur le bleu ext�rieur si sur
	 * la partie 1 de la piste, sur le blanc si sur la partie -1 de la piste
	 * 
	 * @param intensity
	 *                l'intensit� lumineuse mesur�e
	 */
	public static void updateTrackInfos(float intensity) {
		// valeur 0 = partieHuit, valeur 1 = cotePiste

		// on rel�ve la couleur du sol
		if (intensity >= ColorSensor.trackMaxValue - 15)
			// si c'est le blanc, partie -1
			Track.trackPart = -1;
		else
			// sinon, partie 1
			Track.trackPart = 1;

		// on commence toujours sur le grand c�t�
		Track.trackSide = 1;
	}

}
