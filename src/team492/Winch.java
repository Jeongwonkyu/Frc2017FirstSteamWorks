/*
 * Copyright (c) 2015 Titan Robotics Club (http://www.titanrobotics.com)
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

import com.ctre.CANTalon.TalonControlMode;

import frclib.FrcCANTalon;
import trclib.TrcUtil;

public class Winch
{
    private FrcCANTalon mainMotor;
    private FrcCANTalon slaveMotor;
    private double motorPower = 0.0;
    private boolean manualOverride = false;
    private boolean motorStarted = false;
    private boolean offGround = false;
    private boolean encoderReset = false;
    private boolean motorSlowed = false;
    private double settlingTime = 0.0;
    private double masterCurrent = 0.0;
    private double slaveCurrent = 0.0;
    private double maxCurrent = 0.0;

    public Winch()
    {
        mainMotor = new FrcCANTalon("WinchMaster", RobotInfo.CANID_WINCH_MASTER);
        slaveMotor = new FrcCANTalon("WinchSlave", RobotInfo.CANID_WINCH_SLAVE);
        slaveMotor.motor.changeControlMode(TalonControlMode.Follower);
        slaveMotor.motor.set(RobotInfo.CANID_WINCH_MASTER);
        mainMotor.setPositionSensorInverted(false);
    }

    public void setManualOverride(boolean override)
    {
        this.manualOverride = override;
    }

    public boolean isUpperLimitSwitchActive()
    {
        return mainMotor.isUpperLimitSwitchActive();
    }

    public boolean isLowerLimitSwitchActive()
    {
        return mainMotor.isLowerLimitSwitchActive();
    }

    public boolean isEncoderReset()
    {
        return encoderReset;
    }

    public boolean isMotorSlowed()
    {
        return motorSlowed;
    }

    public double getPosition()
    {
        return mainMotor.getPosition()*RobotInfo.WINCH_POSITION_SCALE;
    }

    public double getPower()
    {
        return motorPower;
    }

    public void setPower(double power)
    {
        double currTime = TrcUtil.getCurrentTime();

        power = Math.abs(power);
        if (power == 0.0)
        {
            motorStarted = false;
        }
        else if (!motorStarted)
        {
            //
            // Motor current spikes up when starting, so ignore the first half second to allow current to settle.
            //
            motorStarted = true;
            settlingTime = currTime + 0.5;
        }

        if (!offGround && currTime >= settlingTime && getCurrent() >= RobotInfo.WINCH_MOTOR_CURRENT_THRESHOLD)
        {
            offGround = true;
            mainMotor.resetPosition();
            encoderReset = true;
        }

        if (!manualOverride)
        {
            if (touchingPlate())
            {
                power = 0.0;
            }
            else if (offGround && getPosition() >= RobotInfo.WINCH_HEIGHT_THRESHOLD)
            {
                power *= RobotInfo.WINCH_MOTOR_POWER_SCALE;
                motorSlowed = true;
            }
        }

        motorPower = power;
        mainMotor.setPower(motorPower);
    }

    public double getCurrent()
    {
        masterCurrent = mainMotor.motor.getOutputCurrent();
        slaveCurrent = slaveMotor.motor.getOutputCurrent();
        double totalCurrent = Math.abs(masterCurrent) + Math.abs(slaveCurrent);

        if (totalCurrent > maxCurrent) maxCurrent = totalCurrent;

        return totalCurrent;
    }

    public double getMasterCurrent()
    {
        return masterCurrent;
    }

    public double getSlaveCurrent()
    {
        return slaveCurrent;
    }

    public double getMaxCurrent()
    {
        return maxCurrent;
    }

    private boolean touchingPlate()
    {
        return isUpperLimitSwitchActive() || isLowerLimitSwitchActive();
    }

}   //class Winch
