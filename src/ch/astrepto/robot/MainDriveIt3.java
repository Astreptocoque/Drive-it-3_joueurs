package ch.astrepto.robot;

import lejos.hardware.Button;
import lejos.hardware.lcd.LCD;
import lejos.utility.Delay;

public class MainDriveIt3 {

	public static void main(String[] args) {

		// à faire avant de déposer le robot sur la piste
		RobotControls rob = new RobotControls();
		displayStart();
		rob.robotStart();

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

			if (!Track.crossroads && !Track.verifiyFreeWay && Track.hangOnTrack
					&& !Track.ultrasonicRepositioning) {
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
			if (interval >= 155l * 1000l) {
				boucle = false;
			}
		} while (boucle);

		rob.robotStop();

	}

	private static void chooseMode() {

		boolean sortir = true;
		do {
			LCD.clear();
			for (int i = 0; i < 48; i++) {
				drawLine(1, 178, 0, i, 1);
			}
			LCD.drawString("MODE", 7, 1, true);

			drawArrow(-1, 20, 70, 65, 1);
			drawArrow(1, 20, 110, 65, 1);

			LCD.drawString("Solo", 3, 7);
			LCD.drawString("3 autos", 11, 7);

			// drawArrow(1, 20, 45, 45, 1);
			// Button.waitForAnyPress();
			// drawArrow(1, 16, 43, 49, 0);
			boolean boucle = true;
			while (boucle) {
				if (Button.LEFT.isDown()) {
					drawArrow(-1, 16, 70, 69, 0);
					boucle = false;
					RobotControls.mode = 1;

				} else if (Button.RIGHT.isDown()) {
					drawArrow(1, 16, 110, 69, 0);
					boucle = false;
					RobotControls.mode = 2;
				}
			}

			// écran noir
			for (int i = 0; i < 128; i++) {
				drawLine(1, 178, 0, i, 1);
				Delay.msDelay(15);
			}

			LCD.drawString("ACCEPTER ? ", 4, 1, true);
			if (RobotControls.mode == 1) {
				LCD.drawString("Solo", 7, 4, true);
			} else {
				LCD.drawString("3 autos", 6, 4, true);
			}
			LCD.drawString("ESC ou ENTER", 3, 7, true);

			boucle = true;
			while (boucle) {
				if (Button.ESCAPE.isDown()) {
					sortir = true;
					boucle = false;
				} else if (Button.ENTER.isDown()) {
					sortir = false;
					boucle = false;
				}
			}
		} while (sortir);

		// écran blanc
		for (int i = 0; i < 128; i++) {
			drawLine(1, 178, 0, i, 0);
			Delay.msDelay(10);
		}
	}

	private static void displayStart() {
		// écran blanc
		for (int i = 0; i < 128; i++) {
			drawLine(1, 178, 0, i, 1);
			Delay.msDelay(10);
		}

		drawArrow(-1, 20, 66, 61, 0);
		drawArrow(1, 20, 106, 61, 0);
		for (int i = 61; i < 101; i++) {
			drawLine(1, 40, 66, i, 0);
			Delay.msDelay(10);
		}

		drawArrow(-1, 20, 68, 63, 1);
		drawArrow(1, 20, 108, 63, 1);
		for (int i = 63; i < 103; i++) {
			drawLine(1, 40, 69, i, 1);
			Delay.msDelay(10);
		}

		drawArrow(-1, 20, 70, 65, 0);
		drawArrow(1, 20, 110, 65, 0);
		for (int i = 65; i < 105; i++) {
			drawLine(1, 40, 71, i, 0);
			Delay.msDelay(10);
		}
		LCD.drawString("......START.......", 0, 1, true);

		LCD.drawString("ENTER", 7, 5, false);

		Button.ENTER.waitForPress();
		LCD.clear();
	}

	private static void drawArrow(int direction, int size, int x, int y, int color) {
		int xi = 0;
		int yj = 0;
		int i = 1;
		while (yj != size * 2) {

			xi = 0;
			do {
				if (direction == -1)
					LCD.setPixel(x - xi, y + yj, color);
				else
					LCD.setPixel(x + xi, y + yj, color);
				xi += 1;
			} while (xi != i);

			yj += 1;

			if (yj > size)
				i -= 1;
			else
				i += 1;
		}
	}

	private static void drawLine(int direction, int size, int x, int y, int color) {

		if (direction == 1) {
			int xi = 0;
			while (xi != size) {
				LCD.setPixel(x + xi, y, color);
				xi += 1;
			}
		} else {
			int yj = 0;

			while (yj != size) {
				LCD.setPixel(x, y + yj, color);
				yj += 1;
			}
		}
	}
}
