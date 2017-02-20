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

import frclib.FrcCANTalon;

public class Winch
{
    private FrcCANTalon motor;
//    private DigitalInput proximitySensor;

    public Winch()
    {
        motor = new FrcCANTalon("WinchMotor", RobotInfo.CANID_WINCH);
        motor.setPositionSensorInverted(false);
//        proximitySensor = new DigitalInput(RobotInfo.DIN_PROXIMITY_SENSOR);
    }

    public boolean isLimitSwitchActive()
    {
        return !motor.isUpperLimitSwitchActive();
    }

    public double getPosition()
    {
        return motor.getPosition()*RobotInfo.WINCH_POSITION_SCALE; 
    }

    public void setPower(double power)
    {
        double scale = 1.0; 
//        if (proximitySensor.get())
//            scale = RobotInfo.WINCH_POWER_SCALE;
        motor.setPower(power / scale);
    }
  
}   //class Winch
