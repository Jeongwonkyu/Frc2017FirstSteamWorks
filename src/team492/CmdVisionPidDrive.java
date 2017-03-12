/*
 * Copyright (c) 2017 Titan Robotics Club (http://www.titanrobotics.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package team492;

import trclib.TrcEvent;
import trclib.TrcRobot;
import trclib.TrcStateMachine;
import trclib.TrcTimer;

class CmdVisionPidDrive implements TrcRobot.RobotCommand
{
    private static enum State
    {
        DO_DELAY,
        DO_PID_DRIVE,
        DONE
    }   //enum State

    private static final String moduleName = "CmdVisionPidDrive";

    private Robot robot;
    private double delay;

    private double targetDistance;
    private double targetAngle;
    private double drivePowerLimit;

    private TrcEvent event;
    private TrcTimer timer;
    private TrcStateMachine<State> sm;

    CmdVisionPidDrive(Robot robot, double delay, double targetDistance, double targetAngle, double drivePowerLimit)
    {
        this.robot = robot;
        this.delay = delay;
        this.targetDistance = targetDistance;
        this.targetAngle = targetAngle;
        this.drivePowerLimit = drivePowerLimit;
        event = new TrcEvent(moduleName);
        timer = new TrcTimer(moduleName);
        sm = new TrcStateMachine<>(moduleName);
        sm.start(State.DO_DELAY);

        robot.tracer.traceInfo(moduleName, "delay=%.3f, dist=%.1f, angle=%.1f, powerLimit=%.1f",
            delay, targetDistance, targetAngle, drivePowerLimit);
    }   //CmdVisionPidDrive

    //
    // Implements the TrcRobot.RobotCommand interface.
    //

    @Override
    public boolean cmdPeriodic(double elapsedTime)
    {
        boolean done = !sm.isEnabled();
        //
        // Print debug info.
        //
        State state = sm.getState();
        robot.dashboard.displayPrintf(1, "State: %s", state != null? state.toString(): "Disabled");

        if (sm.isReady())
        {
            state = sm.getState();

            switch (state)
            {
                case DO_DELAY:
                    //
                    // Do delay if any.
                    //
                    if (delay == 0.0)
                    {
                        sm.setState(State.DO_PID_DRIVE);
                    }
                    else
                    {
                        timer.set(delay, event);
                        sm.waitForSingleEvent(event, State.DO_PID_DRIVE);
                    }
                    break;

                case DO_PID_DRIVE:
                    //
                    // Drive the set distance and heading.
                    //
                    robot.encoderXPidCtrl.setOutputRange(-drivePowerLimit, drivePowerLimit);
                    robot.sonarDrivePidCtrl.setOutputRange(-drivePowerLimit, drivePowerLimit);
                    robot.visionTurnPidCtrl.setOutputRange(-drivePowerLimit, drivePowerLimit);
                    robot.setVisionPidDriveTarget(0.0, targetDistance, targetAngle, false, event);
                    sm.waitForSingleEvent(event, State.DONE);
                    break;

                case DONE:
                default:
                    //
                    // We are done.
                    //
                    robot.encoderXPidCtrl.setOutputRange(-1.0, 1.0);
                    robot.sonarDrivePidCtrl.setOutputRange(-1.0, 1.0);
                    robot.visionTurnPidCtrl.setOutputRange(-1.0, 1.0);
                    done = true;
                    sm.stop();
                    break;
            }

            robot.traceStateInfo(elapsedTime, state.toString());
        }

        return done;
    }   //cmdPeriodic

}   //class CmdVisionPidDrive
