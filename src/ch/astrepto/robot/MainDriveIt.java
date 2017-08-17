package ch.astrepto.robot;

import ch.astrepto.robot.moteurs.DirectionMotor;
import ch.astrepto.robot.moteurs.Robot;
import lejos.utility.Delay;

public class MainDriveIt {

	public static void main(String[] args) {

		// à faire avant de déposer le robot sur la piste
		Robot robot = new Robot();

		while (true) {
			robot.run();

		}


	}
}
