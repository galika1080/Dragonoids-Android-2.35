/*
Copyright (c) 2016 Robert Atkinson

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Robert Atkinson nor the names of his contributors may be used to
endorse or promote products derived from this software without specific prior
written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESSFOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.firstinspires.ftc.teamcode;

import android.graphics.Color;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name="TeleOp Mecanum Great Again", group="Linear Opmode")  // @Autonomous(...) is the other common choice
public class TeleOpMecanumOriginal extends LinearOpMode {

    /* Declare OpMode members. */
    private ElapsedTime runtime = new ElapsedTime();
    DcMotor motorRF;
    DcMotor motorRB;
    DcMotor motorLF;
    DcMotor motorLB;

    DcMotor motorDisp;

    DcMotor motorShootOne;
    DcMotor motorShootTwo;

    Servo loader;

    Servo leftLift;
    Servo rightLift;

    ColorSensor colorSensor;
    boolean color;

    // hsvValues is an array that will hold the hue, saturation, and value information.
    float hsvValues[] = {0F,0F,0F};

    // values is a reference to the hsvValues array.
    final float values[] = hsvValues;

    double drive;
    double strafe;
    double rotate;

    final static int ENCODER_CPR = 1120;
    final static double WHEEL_CIRC = 4 * Math.PI;

    double fVelocity;

    long fVelocityTime;
    long fLastVelocityTime;

    int fEncoder;
    int fLastEncoder;

    @Override
    public void runOpMode() {
        telemetry.addData("Status", "Initialized");
        telemetry.update();


        /* eg: Initialize the hardware variables. Note that the strings used here as parameters
         * to 'get' must correspond to the names assigned during the robot configuration
         * step (using the FTC Robot Controller app on the phone).
         */
        motorRF = hardwareMap.dcMotor.get("right_drive_front");
        motorRB = hardwareMap.dcMotor.get("right_drive_back");
        motorLF = hardwareMap.dcMotor.get("left_drive_front");
        motorLB = hardwareMap.dcMotor.get("left_drive_back");

        motorDisp = hardwareMap.dcMotor.get("collector");

        motorShootOne = hardwareMap.dcMotor.get("shooterOne");
        motorShootTwo = hardwareMap.dcMotor.get("shooterTwo");

        motorShootOne.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorShootTwo.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        loader = hardwareMap.servo.get("loader");
        loader.setPosition(.5);

        leftLift = hardwareMap.servo.get("leftLift");
        rightLift = hardwareMap.servo.get("rightLift");

        colorSensor = hardwareMap.colorSensor.get("sensor_color");

        leftLift.setDirection(Servo.Direction.REVERSE);

        leftLift.setPosition(1);
        rightLift.setPosition(1);

        motorRF.setDirection(DcMotor.Direction.REVERSE); // Set to REVERSE if using AndyMark motors
        motorRB.setDirection(DcMotor.Direction.REVERSE);// Set to FORWARD if using AndyMark motors


        motorShootTwo.setDirection(DcMotor.Direction.REVERSE);

        // Wait for the game to start (driver presses PLAY)
        waitForStart();
        runtime.reset();

        // run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {
            telemetry.addData("Status", "Run Time: " + runtime.toString());
            //output telemetry of motors
            telemetry.addData("rightFront", + motorRF.getPower());
            telemetry.addData("leftFront", + motorLF.getPower());
            telemetry.addData("rightBack", + motorRB.getPower());
            telemetry.addData("leftBack", + motorLB.getPower());
            detectColor();


            //Run the collector
            if (gamepad2.a) {
                motorDisp.setPower(-0.8);
            }
            else if(gamepad2.b) {
                motorDisp.setPower(.45);
            } else {
                motorDisp.setPower(0);
            }

            //Fire up the shoot motors
            if (gamepad2.left_bumper){
                bangBang();
            } else {
                motorShootOne.setPower(0);
                motorShootTwo.setPower(0);
            }

            //load a ball into the shooter
            if (gamepad2.right_bumper) {
                loader.setPosition(0.3);
            } else {
                loader.setPosition(.5);
            }

            //deploy the cap ball lift
            if (gamepad1.left_bumper) {
                leftLift.setPosition(.4);
                rightLift.setPosition(.4);
            } //store the cap ball lift
            else if (gamepad1.left_trigger>.2) {
                leftLift.setPosition(1.0);
                rightLift.setPosition(1.0);
            } //execute order 66
            else if (gamepad1.right_bumper) {
                leftLift.setPosition(.66);
                rightLift.setPosition(.66);
            }

            float[] input = new float[] {gamepad1.left_stick_x, -gamepad1.left_stick_y};
            float[] processed = processInput(input);

            //RF, LF, RB, LB
            setMotorPower((float)Range.clip(-processed[0], -1.0, 1.0), (float)Range.clip(processed[1], -1.0, 1.0), (float)Range.clip(processed[1], -1.0, 1.0), (float)Range.clip(-processed[0], -1.0, 1.0));
            /*
            drive	= -scaleInputOriginal(gamepad1.left_stick_y);
            strafe	= scaleInputOriginal(gamepad1.left_stick_x);
            rotate	= scaleInputOriginal(gamepad1.right_stick_x);

            motorLF.setPower(Range.clip(drive - strafe + rotate, -1.0, 1.0));
            motorLB.setPower(Range.clip(drive + strafe + rotate, -1.0, 1.0));
            motorRF.setPower(Range.clip(drive + strafe - rotate, -1.0, 1.0));
            motorRB.setPower(Range.clip(drive - strafe - rotate, -1.0, 1.0));

            telemetry.addData("Distance Traveled: ", motorLF.getCurrentPosition() * (WHEEL_CIRC / ENCODER_CPR));
            telemetry.update();*/

        }

    }

    private void setMotorPower(float RF, float LF, float RB, float LB) {
        motorRF.setPower(RF);
        motorLF.setPower(LF);
        motorRB.setPower(RB);
        motorLB.setPower(LB);
    }

    public boolean detectColor () {

        //false is red
        color = false;
        // convert the RGB values to HSV values.
        Color.RGBToHSV(colorSensor.red() * 8, colorSensor.green() * 8, colorSensor.blue() * 8, hsvValues);

        // send the info back to driver station using telemetry function.
        telemetry.addData("Red  ", colorSensor.red());
        telemetry.addData("Blue ", colorSensor.blue());

        if (colorSensor.red()>colorSensor.blue()) {
            color = false;
        }
        else {
            color = true;
        }


        return color;
    }
    public void bangBang () {
        fVelocityTime = System.nanoTime();

        fEncoder = motorShootOne.getCurrentPosition();

        fVelocity = (double) (fEncoder - fLastEncoder) / (fVelocityTime - fLastVelocityTime);

        if (fVelocity >= .92) {
            motorShootOne.setPower(.88);
            motorShootTwo.setPower(.88);
        } else if (fVelocity < .92) {
            motorShootOne.setPower(.92);
            motorShootTwo.setPower(.92);
        }
        fLastEncoder = fEncoder;
        fLastVelocityTime = fVelocityTime;
    }
}