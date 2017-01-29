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

package frclib;

import com.ctre.CANTalon;

import trclib.TrcDbgTrace;
import trclib.TrcMotorController;

public class FrcCANTalon extends CANTalon implements TrcMotorController
{
    private static final String moduleName = "FrcCANTalon";
    private static final boolean debugEnabled = false;
    private static final boolean tracingEnabled = false;
    private static final TrcDbgTrace.TraceLevel traceLevel = TrcDbgTrace.TraceLevel.API;
    private static final TrcDbgTrace.MsgLevel msgLevel = TrcDbgTrace.MsgLevel.INFO;
    private TrcDbgTrace dbgTrace = null;

    private String instanceName;
    private boolean feedbackDeviceIsPot = false;
    private boolean limitSwitchesSwapped = false;
    private boolean revLimitSwitchNormalOpen = false;
    private boolean fwdLimitSwitchNormalOpen = false;
    private double zeroPosition = 0.0;
    private boolean softLowerLimitEnabled = false;
    private boolean softUpperLimitEnabled = false;
    private double softLowerLimit = 0.0;
    private double softUpperLimit = 0.0;

    /**
     * This method is called by all constructor overloads to do common initialization.
     *
     * @param instanceName specifies the instance name.
     */
    private void commonInit(final String instanceName)
    {
        this.instanceName = instanceName;
        resetPosition();

        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(moduleName + "." + instanceName, tracingEnabled, traceLevel, msgLevel);
        }
    }   //commonInit

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param deviceNumber specifies the CAN ID of the device.
     * @param controlPeriodMs specifies the period in msec to send the CAN control frame.
     *                        Period is bounded to 1 msec to 95 msec.
     * @param enablePeriodMs specifies the period in msec to send the enable control frame.
     *                       Period is bounded to 1 msec to 95 msec. This typically is not
     *                       required to specify, however, this could be used to minimize the
     *                       time between robot-enable and talon-motor-drive.
     */
    public FrcCANTalon(final String instanceName, int deviceNumber, int controlPeriodMs, int enablePeriodMs)
    {
        super(deviceNumber, controlPeriodMs, enablePeriodMs);
        commonInit(instanceName);
    }   //FrcCANTalon

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param deviceNumber specifies the CAN ID of the device.
     * @param controlPeriodMs specifies the period in msec to send the CAN control frame.
     *                        Period is bounded to 1 msec to 95 msec.
     */
    public FrcCANTalon(final String instanceName, int deviceNumber, int controlPeriodMs)
    {
        super(deviceNumber, controlPeriodMs);
        commonInit(instanceName);
    }   //FrcCANTalon

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param deviceNumber specifies the CAN ID of the device.
     */
    public FrcCANTalon(final String instanceName, int deviceNumber)
    {
        super(deviceNumber);
        commonInit(instanceName);
    }   //FrcCANTalon

    /**
     * This method returns the instance name.
     *
     * @return instance name.
     */
    public String toString()
    {
        return instanceName;
    }   //toString

    /**
     * This method swaps the forward and reverse limit switches. By default, the lower limit switch is associated
     * with the reverse limit switch and the upper limit switch is associated with the forward limit switch. This
     * method will swap the association.
     *
     * @param swapped specifies true to swap the limit switches, false otherwise.
     */
    public void setLimitSwitchesSwapped(boolean swapped)
    {
        final String funcName = "setLimitSwitchesSwapped";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "swapped=%s", Boolean.toString(swapped));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        limitSwitchesSwapped = swapped;
    }   //setLimitSwitchesSwapped

    //
    // Overriding CANTalon specific methods.
    //

    /**
     * This method configures the forward limit switch to be normally open (i.e. active when close).
     *
     * @param normalOpen specifies true for normal open, false for normal close.
     */
    @Override
    public void ConfigFwdLimitSwitchNormallyOpen(boolean normalOpen)
    {
        final String funcName = "ConfigFwdLimitSwitchNormallyOpen";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "normalOpen=%s", Boolean.toString(normalOpen));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        super.ConfigFwdLimitSwitchNormallyOpen(normalOpen);
        fwdLimitSwitchNormalOpen = normalOpen;
    }   //ConfigFwdLimitSwitchNormallyOpen

    /**
     * This method configures the reverse limit switch to be normally open (i.e. active when close).
     *
     * @param normalOpen specifies true for normal open, false for normal close.
     */
    @Override
    public void ConfigRevLimitSwitchNormallyOpen(boolean normalOpen)
    {
        final String funcName = "ConfigRevLimitSwitchNormallyOpen";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "normalOpen=%s", Boolean.toString(normalOpen));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        super.ConfigRevLimitSwitchNormallyOpen(normalOpen);
        revLimitSwitchNormalOpen = normalOpen;
    }   //ConfigRevLimitSwitchNormallyOpen

    /**
     * This method sets the feedback device type.
     *
     * @param devType specifies the feedback device type.
     */
    @Override
    public void setFeedbackDevice(FeedbackDevice devType)
    {
        final String funcName = "setFeedbackDevice";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "devType=%s", devType.toString());
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        super.setFeedbackDevice(devType);
        feedbackDeviceIsPot = devType == FeedbackDevice.AnalogPot;
    }   //setFeedbackDevice

    //
    // Implements TrcMotorController interface.
    //

    /**
     * This method returns the state of the motor controller direction.
     *
     * @return true if the motor direction is inverted, false otherwise.
     */
    @Override
    public boolean getInverted()
    {
        final String funcName = "getInverted";
        boolean inverted = super.getInverted();

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "=%s", Boolean.toString(inverted));
        }

        return inverted;
    }   //getInverted

    /**
     * This method returns the motor position by reading the position sensor. The position sensor can be an encoder
     * or a potentiometer.
     *
     * @return current motor position.
     */
    @Override
    public double getPosition()
    {
        final String funcName = "getPosition";
        double pos = super.getPosition();
        
        if (feedbackDeviceIsPot)
        {
            pos -= zeroPosition;
        }

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "=%f", pos);
        }

        return pos;
    }   //getPosition

    /**
     * This method gets the last set power.
     *
     * @return the last setPower value.
     */
    @Override
    public double getPower()
    {
        final String funcName = "getPower";
        double power = super.get();

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "=%f", power);
        }

        return power;
    }   //getPower

    /**
     * This method returns the state of the lower limit switch.
     *
     * @return true if lower limit switch is active, false otherwise.
     */
    @Override
    public boolean isLowerLimitSwitchActive()
    {
        final String funcName = "isLowerLimitSwitchActive";
        boolean isActive = limitSwitchesSwapped? !(fwdLimitSwitchNormalOpen^isFwdLimitSwitchClosed()):
                                                 !(revLimitSwitchNormalOpen^isRevLimitSwitchClosed());

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "=%s", Boolean.toString(isActive));
        }

        return isActive;
    }   //isLowerLimitSwitchClosed

    /**
     * This method returns the state of the upper limit switch.
     *
     * @return true if upper limit switch is active, false otherwise.
     */
    @Override
    public boolean isUpperLimitSwitchActive()
    {
        final String funcName = "isUpperLimitSwitchActive";
        boolean isActive = limitSwitchesSwapped? !(revLimitSwitchNormalOpen^isRevLimitSwitchClosed()):
                                                 !(fwdLimitSwitchNormalOpen^isFwdLimitSwitchClosed());

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "=%s", Boolean.toString(isActive));
        }

        return isActive;
    }   //isUpperLimitSwitchActive

    /**
     * This method resets the motor position sensor, typically an encoder. This method emulates a reset for a
     * potentiometer.
     */
    public void resetPosition()
    {
        final String funcName = "resetPosition";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        if (feedbackDeviceIsPot)
        {
            zeroPosition = super.getPosition();
        }
        else
        {
            super.setPosition(0.0);
        }
    }   //resetPosition

    /**
     * This method enables/disables motor brake mode. In motor brake mode, set power to 0 would stop the motor very
     * abruptly by shorting the motor wires together using the generated back EMF to stop the motor. When brakMode
     * is false (i.e. float/coast mode), the motor wires are just disconnected from the motor controller so the motor
     * will stop gradually.
     *
     * @param enabled specifies true to enable brake mode, false otherwise.
     */
    public void setBrakeModeEnabled(boolean enabled)
    {
        final String funcName = "setBrakeModeEnabled";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "enabled=%s", Boolean.toString(enabled));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        super.enableBrakeMode(enabled);
    }   //setBrakeModeEnabled

    /**
     * This method sets the output power of the motor controller.
     *
     * @param power specifies the output power for the motor controller in the range of -1.0 to 1.0.
     */
    public void setPower(double power)
    {
        final String funcName = "setPower";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "power=%f", power);
        }

        if (softLowerLimitEnabled && power < 0.0 && getPosition() <= softLowerLimit ||
            softUpperLimitEnabled && power > 0.0 && getPosition() >= softUpperLimit)
        {
            power = 0.0;
        }

        super.set(power);

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "! (power=%f)", power);
        }
    }   //setPower

    /**
     * This method inverts the position sensor direction. This may be rare but there are scenarios where the motor
     * encoder may be mounted somewhere in the power train that it rotates opposite to the motor rotation. This will
     * cause the encoder reading to go down when the motor is receiving positive power. This method can correct this
     *  situation.
     *
     * @param inverted specifies true to invert position sensor direction, false otherwise.
     */
    public void setPositionSensorInverted(boolean inverted)
    {
        final String funcName = "setPositionSensorInverted";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "inverted=%s", Boolean.toString(inverted));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        super.reverseSensor(inverted);
    }   //setPositionSensorInverted

    /**
     * This method enables/disables soft limit switches.
     *
     * @param lowerLimitEnabled specifies true to enable lower soft limit switch, false otherwise.
     * @param upperLimitEnabled specifies true to enable upper soft limit switch, false otherwise.
     */
    public void setSoftLimitEnabled(boolean lowerLimitEnabled, boolean upperLimitEnabled)
    {
        final String funcName = "setSoftLimitEnabled";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "lowerEnabled=%s,upperEnabled=%s",
                Boolean.toString(lowerLimitEnabled), Boolean.toString(upperLimitEnabled));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        softLowerLimitEnabled = lowerLimitEnabled;
        softUpperLimitEnabled = upperLimitEnabled;
    }   //setSoftLimitEnabled

    /**
     * This method sets the lower soft limit.
     *
     * @param position specifies the position of the lower limit.
     */
    public void setSoftLowerLimit(double position)
    {
        final String funcName = "setSoftLowerLimit";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "position=%f", position);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        softLowerLimit = position;
    }   //setSoftLowerLimit

    /**
     * This method sets the upper soft limit.
     *
     * @param position specifies the position of the upper limit.
     */
    public void setSoftUpperLimit(double position)
    {
        final String funcName = "setSoftUpperLimit";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "position=%f", position);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        softUpperLimit = position;
    }   //setSoftUpperLimit

}   //class FrcCANTalon
