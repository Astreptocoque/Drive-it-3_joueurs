package ch.astrepto.robot.moteurs;

import lejos.hardware.motor.NXTRegulatedMotor;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.NXTTouchSensor;
import lejos.robotics.SampleProvider;

public class UltrasonicMotor {

	private NXTRegulatedMotor ultrasonicMotor;

	private SampleProvider ultrasonicTouchSensor;
	private float[] sampleUltrasonicTouchSensor;

	private final static int maxSpeed = 1000;
	public final static int maxDegree = 2250; // de droit � un bord
	private final static int maxDirectionDegree = 1190;
	private static int currentDegree;

	public UltrasonicMotor() {
		ultrasonicMotor = new NXTRegulatedMotor(MotorPort.D);
		ultrasonicTouchSensor = new NXTTouchSensor(SensorPort.S1).getTouchMode();
		sampleUltrasonicTouchSensor = new float[ultrasonicTouchSensor.sampleSize()];
		ultrasonicMotor.setSpeed(maxSpeed);

		// cadrage
		ultrasonicMotor.forward();
		boolean boucle = true;

		while (boucle) {
			ultrasonicTouchSensor.fetchSample(sampleUltrasonicTouchSensor, 0);

			if (this.sampleUltrasonicTouchSensor[0] == 1) {
				ultrasonicMotor.stop();
				ultrasonicMotor.rotate(-maxDegree);
				boucle = false;
			}
		}
		ultrasonicMotor.resetTachoCount();
	}

	/**
	 * 
	 * @param angleP
	 *                o� l'on veut se rendre
	 * @param boundWithWheels
	 *                si l'ultrason est li� au degr� de la direction
	 */
	public void goTo(int angleP, boolean boundWithWheels) {
		// angleP est l'angle pour le moteur de Direction
		// on donne l'angle auquel on veut se rendre
		int angle;

		// arr�te le moteur s'il est en train de bouger
		if (ultrasonicMotor.isMoving())
			ultrasonicMotor.stop();

		currentDegree = ultrasonicMotor.getTachoCount();

		// si l'angle est li� au roue
		if (boundWithWheels) {

			// mise � l'�chelle de l'angle Direction � l'angle Ultrason
			angle = maxDirectionDegree / DirectionMotor.maxDegree * angleP;
			// transformation de l'angle final en nombre de � que doit faire le robot

			// si l'angle est sup�rieure au maximum � gauche
			if (angleP < -maxDirectionDegree) {
				angle = -maxDirectionDegree;
				// si l'angle est sup�rieur au max � droite
			} else if (angleP > maxDirectionDegree) {
				angle = maxDirectionDegree;
			}

		}
		// si l'ultrason bouge librement (sans les roues)
		else {
			// c'est un b�te angle
			angle = angleP;
		}

		angle = angle - currentDegree;
		ultrasonicMotor.rotate(angle, true);
	}

	/**
	 * Attends que le moteur ai fini son mouvement
	 */
	public void waitComplete() {
		ultrasonicMotor.waitComplete();
	}

	/**
	 * Renvoi si le moteur est en mouvement
	 * 
	 * @return vrai si le moteur ne fait plus de mouvement
	 */
	public boolean previousMoveComplete() {
		return !ultrasonicMotor.isMoving();
	}
}
