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

package frclib;

import java.util.Arrays;

import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.SerialPort;
import trclib.TrcDbgTrace;
import trclib.TrcPixyCam;

/**
 * This class implements a platform dependent pixy camera that is either connected to an I2C bus or a Serial Port.
 * It provides access to the last detected objects reported by the pixy camera asynchronously.
 */
public class FrcPixyCam extends TrcPixyCam
{
    private static final String moduleName = "FrcPixyCam";
    private static final boolean debugEnabled = false;
    private static final boolean tracingEnabled = false;
    private static final TrcDbgTrace.TraceLevel traceLevel = TrcDbgTrace.TraceLevel.API;
    private static final TrcDbgTrace.MsgLevel msgLevel = TrcDbgTrace.MsgLevel.INFO;
    private TrcDbgTrace dbgTrace = null;

    public static final I2C.Port DEF_I2C_PORT = I2C.Port.kOnboard;
    public static final int DEF_I2C_ADDRESS = 0x54;

    public static final SerialPort.Port DEF_SERIAL_PORT = SerialPort.Port.kOnboard;
    public static final int DEF_BAUD_RATE = 19200;
    public static final int DEF_DATA_BITS = 8;
    public static final SerialPort.Parity DEF_PARITY = SerialPort.Parity.kNone;
    public static final SerialPort.StopBits DEF_STOP_BITS = SerialPort.StopBits.kOne;

    private FrcI2cDevice i2cPixy = null;
    private FrcSerialPort serialPixy = null;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param port specifies the I2C port on the RoboRIO.
     * @param devAddress specifies the I2C address of the device.
     */
    public FrcPixyCam(final String instanceName, I2C.Port port, int devAddress)
    {
        super(instanceName);

        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(moduleName + "." + instanceName, tracingEnabled, traceLevel, msgLevel);
        }

        i2cPixy = new FrcI2cDevice(instanceName, port, devAddress);
    }   //FrcPixyCam

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param port specifies the I2C port on the RoboRIO.
     */
    public FrcPixyCam(final String instanceName, I2C.Port port)
    {
        this(instanceName, port, DEF_I2C_ADDRESS);
    }   //FrcPixyCam

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param port specifies the serial port on the RoboRIO.
     * @param baudRate specifies the baud rate.
     * @param dataBits specifies the number of data bits.
     * @param parity specifies the parity type.
     * @param stopBits specifies the number of stop bits.
     */
    public FrcPixyCam(
        final String instanceName, SerialPort.Port port, int baudRate, int dataBits, SerialPort.Parity parity,
        SerialPort.StopBits stopBits)
    {
        super(instanceName);

        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(moduleName + "." + instanceName, tracingEnabled, traceLevel, msgLevel);
        }

        serialPixy = new FrcSerialPort(instanceName, port, baudRate, dataBits, parity, stopBits);
    }   //FrcPixyCam

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param port specifies the serial port on the RoboRIO.
     * @param baudRate specifies the baud rate.
     */
    public FrcPixyCam(final String instanceName, SerialPort.Port port, int baudRate)
    {
        this(instanceName, port, baudRate, DEF_DATA_BITS, DEF_PARITY, DEF_STOP_BITS);
    }   //FrcPixyCam

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param port specifies the serial port on the RoboRIO.
     */
    public FrcPixyCam(final String instanceName, SerialPort.Port port)
    {
        this(instanceName, port, DEF_BAUD_RATE, DEF_DATA_BITS, DEF_PARITY, DEF_STOP_BITS);
    }   //FrcPixyCam

    //
    // Implements TrcPixyCam abstract methods.
    //

    /**
     * This method checks if the pixy camera is enabled.
     *
     * @return true if pixy camera is enabled, false otherwise.
     */
    public boolean isEnabled()
    {
        final String funcName = "isEnabled";
        boolean enabled = i2cPixy != null? i2cPixy.isTaskEnabled():
                          serialPixy != null? serialPixy.isTaskEnabled(): false;

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API, "=%s", Boolean.toString(enabled));
        }

        return enabled;
    }   //isEnable

    /**
     * This method enables/disables the pixy camera.
     *
     * @param enabled specifies true to enable pixy camera, false to disable.
     */
    public void setEnabled(boolean enabled)
    {
        final String funcName = "setEnabled";
        boolean wasEnabled = isEnabled();

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "enanbled=%s", Boolean.toString(enabled));
        }

        if (i2cPixy != null)
        {
            i2cPixy.setTaskEnabled(enabled);
        }
        else if (serialPixy != null)
        {
            serialPixy.setTaskEnabled(enabled);
        }

        if (!wasEnabled && enabled)
        {
            start();
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //setEnabled

    /**
     * This method issues an asynchronous read of the specified number of bytes from the device.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param length specifies the number of bytes to read.
     */
    @Override
    public void asyncReadData(RequestTag requestTag, int length)
    {
        final String funcName = "asyncReadData";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "tag=%s,length=%d",
                requestTag != null? requestTag: "null", length);
        }

        if (i2cPixy != null)
        {
            i2cPixy.asyncRead(requestTag, length, null, this);
        }
        else if (serialPixy != null)
        {
            serialPixy.asyncRead(requestTag, length, null, this);
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //asyncReadData

    /**
     * This method writes the data buffer to the device asynchronously.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param data specifies the data buffer.
     */
    @Override
    public void asyncWriteData(RequestTag requestTag, byte[] data)
    {
        final String funcName = "asyncWriteData";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "tag=%s,data=%s,length=%d",
                requestTag != null? requestTag: "null", Arrays.toString(data), data.length);
        }

        if (i2cPixy != null)
        {
            i2cPixy.asyncWrite(requestTag, data, data.length, null, null);
        }
        else if (serialPixy != null)
        {
            serialPixy.asyncWrite(requestTag, data, data.length, null, null);
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //asyncWriteData

}   //class FrcPixyCam
