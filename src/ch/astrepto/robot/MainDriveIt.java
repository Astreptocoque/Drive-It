package ch.astrepto.robot;

import ch.astrepto.robot.moteurs.Robot;
import lejos.hardware.Button;

public class MainDriveIt {

	public static void main(String[] args) {

		// � faire avant de d�poser le robot sur la piste
		Robot robot = new Robot();

		while (true) {
			robot.updateDirection();
			robot.updateSpeed();
			robot.updateMove();

		}

	}
}
