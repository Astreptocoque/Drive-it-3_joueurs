package ch.astrepto.robot;

import ch.astrepto.robot.capteurs.ColorSensor;
import ch.astrepto.robot.capteurs.UltrasonicSensor;
import ch.astrepto.robot.moteurs.DirectionMotor;
import ch.astrepto.robot.moteurs.TractionMotor;
import ch.astrepto.robot.moteurs.UltrasonicMotor;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.utility.Delay;

public class RobotControls {

	private DirectionMotor directionMotor;
	private UltrasonicMotor ultrasonicMotor;
	private TractionMotor tractionMotor;
	private ColorSensor color;
	private UltrasonicSensor ultrasonic;
	private static float intensity = 0;
	private static float previousSpeed = 0;
	private static float previousTachoCount = 0;
	private static float previousDistance = 0;
	public static int mode;

	public RobotControls() {
		// mode course � 3
		directionMotor = new DirectionMotor();
		ultrasonicMotor = new UltrasonicMotor();
		color = new ColorSensor();
		ultrasonic = new UltrasonicSensor();
		previousSpeed = TractionMotor.currentSpeed;
		
		System.out.println("Placer le robot sur la piste et presser ENTER");
		Button.ENTER.waitForPressAndRelease();
		Track.updateTrackInfos(color.getIntensity());
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
				if (distance <= UltrasonicSensor.maxDetectedDistance -1)
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
	public void updateDirection(boolean notUltrasonic) {
		// si l'ultrason n'est pas li� aux roues
		if (notUltrasonic) {
			// Maj la direction si "le pr�c�dent mvt est fini"
			if (directionMotor.previousMoveComplete()) {
				// l'angle est d�termin� par la situation du robot sur la piste
				int angle = directionMotor.determineAngle(intensity);

				directionMotor.goTo(angle);
			}
		}
		// si l'ultrason bouge avec les roues
		else {
			// Maj la direction si "le pr�c�dent mvt est fini"
			if (directionMotor.previousMoveComplete() && ultrasonicMotor.previousMoveComplete()) {

				if (Track.ultrasonicRepositioning) {
					Track.ultrasonicRepositioning = false;
					Sound.buzz();
				}
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
	}

	/**
	 * Gestion de la vitesse automatique la vitesse est d�termin�e en fonction de la distance en
	 * cm mesur�e
	 */
	public void updateSpeed() {
		// d�finition de la vitesse
		previousDistance = ultrasonic.getDistance();
		float speed = tractionMotor.determineSpeed(previousDistance);
		System.out.println(speed);
		tractionMotor.setSpeed(speed);
	}

	public void isThereAnOvertaking() {
		// analyse de la vitesse pour �v. commencer un d�passsement
		// si la vitesse pr�c�dente est plus petite, c'est qu'on r�acc�l�re, donc qu'on a
		// atteint la vitesse de l'autre v�hicul

		if (previousSpeed < TractionMotor.currentSpeed && ultrasonic.getDistance() < TractionMotor.firstLimit) {
			float remainingDistance = Track.trackPartLength - tractionMotor.getTachoCount();
			if (remainingDistance > Track.overtakingLength) {
				Track.verifiyFreeWay = true;
				// si on doit tourner l'ultrason � droite
				if ((Track.trackPart == 1 && Track.trackSide == 1)
						|| (Track.trackPart == -1 && Track.trackSide == -1))
					ultrasonicMotor.goTo(UltrasonicMotor.maxDegree, false);
				// sinon � gauche
				else
					ultrasonicMotor.goTo(-UltrasonicMotor.maxDegree, false);

			}
		}
		previousSpeed = TractionMotor.currentSpeed;
	}

	/**
	 * Gestion de l'ultrason pour v�rifier si l'autre c�t� de la piste est libre
	 */
	public void freeWay() {

		if (ultrasonicMotor.previousMoveComplete()) {
			// si la voie est libre (sup�rieur � la largeur de la piste - la largeur du
			// robot - la moiti� du d�grad� (suivi)
			if (ultrasonic.getDistance() > Track.crossroadsLength - TractionMotor.wheelSpacing
					- Track.gradientWidth / 2) {
				// si la distance restante est toujours ok
				float remainingDistance = Track.trackPartLength - tractionMotor.getTachoCount();
				if (remainingDistance > Track.overtakingLength)
					Track.overtaking = true;
			} else {
				Track.ultrasonicRepositioning = true;
				ultrasonicMotor.goTo(-directionMotor.determineAngle(intensity), true);
			}
			Track.verifiyFreeWay = false;
			// pour emp�cher le robot de v�rifier s'il peut d�passer apr�s une
			// v�rification pendant qu'il r�acc�l�re
			previousSpeed = TractionMotor.maxSpeed;
		}
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
		Track.overtaking = false;

		// r�gle l'angle que les roues doivent prendre pour changer de c�t�
		int angle;
		if (Track.trackSide == -1) {
			angle = 0;
			Track.overtakingPart = 2;
		} else {
			Track.overtakingPart = 2;
			// angle des roues en fonction du rayon
			if (Track.trackPart == 1) {
				// - arcsin(empatement / petit rayon)
				angle = -(int) (Math
						.asin(DirectionMotor.wheelBase
								/ (Track.smallRadius + Track.gradientWidth / 2))
						* 180d / Math.PI);
			} else {
				// arcsin(empatement / petit rayon)
				angle = (int) (Math
						.asin(DirectionMotor.wheelBase
								/ (Track.smallRadius + Track.gradientWidth / 2))
						* 180d / Math.PI);
			}

			angle = DirectionMotor.maxDegree / DirectionMotor.maxAngle * angle;
		}

		ultrasonicMotor.goTo(-angle, true);
		directionMotor.goTo(angle);
		previousTachoCount = tractionMotor.getTachoCount();
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
	public void overtakingEnd() {
		if (Track.overtakingPart == 1) {
			if (tractionMotor.getTachoCount() >= previousTachoCount
					+ (Track.smallRadius + Track.gradientWidth / 2) * 2 * Math.PI / 4
							/ TractionMotor.cmInDegres) {
				ultrasonicMotor.goTo(0, true);
				directionMotor.goTo(0);
				Track.overtakingPart = 2;
			}
		} else {
			// s�curit� pour ne pas d�tecter le c�t� actuel
			if (tractionMotor.getTachoCount() >= previousTachoCount + 720) {
				if (intensity <= (ColorSensor.trackMaxValue - 10)) {
					Track.hangOnTrack = true;
					// on change les donn�es de piste
					Track.changeTrackSide();
				}
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

	public void robotStart() {
		tractionMotor.move(true);
	}
}
