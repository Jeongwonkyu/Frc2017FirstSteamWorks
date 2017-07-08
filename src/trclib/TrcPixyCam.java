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

package trclib;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class implements a platform independent pixy camera. This class is intended to be extended by a platform
 * dependent pixy class which provides the abstract methods required by this class. This class provides the parser
 * to read and parse the object block from the pixy camera. It also provides access to the last detected objects
 * reported by the pixy camera asynchronously.
 */
public abstract class TrcPixyCam implements TrcSerialBusDevice.CompletionHandler
{
    private static final String moduleName = "TrcPixyCam";
    private static final boolean debugEnabled = false;
    private static final boolean tracingEnabled = false;
    private static final TrcDbgTrace.TraceLevel traceLevel = TrcDbgTrace.TraceLevel.API;
    private static final TrcDbgTrace.MsgLevel msgLevel = TrcDbgTrace.MsgLevel.INFO;
    private TrcDbgTrace dbgTrace = null;
    private TrcDbgTrace tracer = TrcDbgTrace.getGlobalTracer();

    private static final boolean USE_BYTE_TRANSACTION = false;

    private static final byte PIXY_SYNC_LOW                     = (byte)0x55;
    private static final byte PIXY_SYNC_LOW_CC                  = (byte)0x56;
    private static final byte PIXY_SYNC_HIGH                    = (byte)0xaa;
    private static final int PIXY_START_WORD                    = 0xaa55;
    private static final int PIXY_START_WORD_CC                 = 0xaa56;
    private static final int PIXY_START_WORDX                   = 0x55aa;

    private static final byte PIXY_CMD_SET_LED                  = (byte)0xfd;
    private static final byte PIXY_CMD_SET_BRIGHTNESS           = (byte)0xfe;
    private static final byte PIXY_CMD_SET_PAN_TILT             = (byte)0xff;

    /**
     * This method issues an asynchronous read of the specified number of bytes from the device.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param length specifies the number of bytes to read.
     */
    public abstract void asyncReadData(RequestTag requestTag, int length);

    /**
     * This method writes the data buffer to the device asynchronously.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param data specifies the data buffer.
     */
    public abstract void asyncWriteData(RequestTag requestTag, byte[] data);

    /**
     * This class implements the pixy camera object block communication protocol. 
     */
    public class ObjectBlock
    {
        public int sync;
        public int checksum;
        public int signature;
        public int centerX;
        public int centerY;
        public int width;
        public int height;
        public int angle;

        public String toString()
        {
            return String.format(
                "sync=0x%04x, chksum=0x%04x, sig=%d, centerX=%3d, centerY=%3d, width=%3d, height=%3d, angle=%3d",
                sync, checksum, signature, centerX, centerY, width, height, angle);
        }
    }   //class ObjectBlock

    /**
     * This is used identify the request type.
     */
    public static enum RequestTag
    {
        SYNC,
        ALIGN,
        CHECKSUM,
        NORMAL_BLOCK,
        COLOR_CODE_BLOCK,
        //
        // Tags for BYTE_TRANSACTION.
        //
        SYNC_LOW,
        SYNC_HIGH,
        CHECKSUM_LOW,
        CHECKSUM_HIGH,
        SIGNATURE_LOW,
        SIGNATURE_HIGH,
        CENTERX_LOW,
        CENTERX_HIGH,
        CENTERY_LOW,
        CENTERY_HIGH,
        WIDTH_LOW,
        WIDTH_HIGH,
        HEIGHT_LOW,
        HEIGHT_HIGH,
        ANGLE_LOW,
        ANGLE_HIGH
    }   //enum RequestTag

    private final String instanceName;
    private ArrayList<ObjectBlock> objects = new ArrayList<>();
    private ObjectBlock[] detectedObjects = null;
    private ObjectBlock currBlock = null;
    private Object objectLock = new Object();
    private int runningChecksum = 0;
    private boolean started = false;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     */
    public TrcPixyCam(final String instanceName)
    {
        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(moduleName + "." + instanceName, tracingEnabled, traceLevel, msgLevel);
        }

        this.instanceName = instanceName;
    }   //TrcPixyCam

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
     * This method starts the pixy camera by queuing the initial read request if not already.
     */
    public void start()
    {
        if (!started)
        {
            started = true;
            if (!USE_BYTE_TRANSACTION)
            {
                asyncReadData(RequestTag.SYNC, 2);
            }
            else
            {
                asyncReadData(RequestTag.SYNC_LOW, 1);
            }
        }
    }   //start

    /**
     * This method writes the data to the device one byte at a time.
     *
     * @param data specifies the buffer containing the data to be written to the device.
     */
    public void asyncWriteBytes(byte[] data)
    {
        byte[] byteData = new byte[1];

        for (int i = 0; i < data.length; i++)
        {
            byteData[0] = data[i];
            asyncWriteData(null, byteData);
        }
    }   //asyncWriteBytes

    /**
     * This method sets the LED to the specified color.
     *
     * @param red specifies the red value.
     * @param green specifies the green value.
     * @param blue specifies the blue value.
     */
    public void setLED(byte red, byte green, byte blue)
    {
        final String funcName = "setLED";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "red=%d,green=%d,blue=%d", red, green, blue);
        }

        byte[] data = new byte[5];
        data[0] = 0x00;
        data[1] = PIXY_CMD_SET_LED;
        data[2] = red;
        data[3] = green;
        data[4] = blue;

        if (!USE_BYTE_TRANSACTION)
        {
            asyncWriteData(null, data);
        }
        else
        {
            asyncWriteBytes(data);
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //setLED

    /**
     * This method sets the camera brightness.
     *
     * @param brightness specifies the brightness value.
     */
    public void setBrightness(byte brightness)
    {
        final String funcName = "setBrightness";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "brightness=%d", brightness);
        }

        byte[] data = new byte[3];
        data[0] = 0x00;
        data[1] = PIXY_CMD_SET_BRIGHTNESS;
        data[2] = brightness;

        if (!USE_BYTE_TRANSACTION)
        {
            asyncWriteData(null, data);
        }
        else
        {
            asyncWriteBytes(data);
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //setBrightness

    /**
     * This method sets the pan and tilt servo positions.
     * @param pan specifies the pan position between 0 and 1000.
     * @param tilt specifies the tilt position between 0 and 1000.
     */
    public void setPanTilt(int pan, int tilt)
    {
        final String funcName = "setPanTilt";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "pan=%d,tilt=%d", pan, tilt);
        }

        if (pan < 0 || pan > 1000 || tilt < 0 || tilt > 1000)
        {
            throw new IllegalArgumentException("Invalid pan/tilt range.");
        }

        byte[] data = new byte[6];
        data[0] = 0x00;
        data[1] = PIXY_CMD_SET_PAN_TILT;
        data[2] = (byte)(pan & 0xff);
        data[3] = (byte)(pan >> 8);
        data[4] = (byte)(tilt & 0xff);
        data[5] = (byte)(tilt >> 8);

        if (!USE_BYTE_TRANSACTION)
        {
            asyncWriteData(null, data);
        }
        else
        {
            asyncWriteBytes(data);
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }
    }   //setPanTilt

    /**
     * This method returns an array of detected object blocks.
     *
     * @return array of detected object blocks, can be null if no object detected.
     */
    public ObjectBlock[] getDetectedObjects()
    {
        final String funcName = "getDetectedObjects";
        ObjectBlock[] objectBlocks = null;

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
        }

        synchronized (objectLock)
        {
            objectBlocks = detectedObjects;
            detectedObjects = null;
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        return objectBlocks;
    }   //getDetectedObjects

    /**
     * This method processes the data from the read completion handler.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param data specifies the data read.
     * @param length specifies the number of bytes read.
     */
    private void processData(RequestTag requestTag, byte[] data, int length)
    {
        final String funcName = "processData";
        int word;

        if (debugEnabled)
        {
            dbgTrace.traceVerbose(funcName, "tag=%s,data=%s,len=%d", requestTag, Arrays.toString(data), length);
        }

        if (!USE_BYTE_TRANSACTION)
        {
            switch (requestTag)
            {
                case SYNC:
                    //
                    // If we don't already have an object block allocated, allocate it now.
                    //
                    if (currBlock == null)
                    {
                        currBlock = new ObjectBlock();
                    }

                    if (length != 2)
                    {
                        //
                        // We should never get here. But if we do, probably due to device read failure, we will initiate
                        // another read for SYNC.
                        //
                        asyncReadData(RequestTag.SYNC, 2);
                        tracer.traceWarn(funcName, "Unexpected data length %d in %s", length, requestTag);
                    }
                    else
                    {
                        word = TrcUtil.bytesToInt(data[0], data[1]);
                        if (word == PIXY_START_WORD || word == PIXY_START_WORD_CC)
                        {
                            //
                            // Found a sync word, initiate the read for CHECKSUM.
                            //
                            currBlock.sync = word;
                            asyncReadData(RequestTag.CHECKSUM, 2);
                        }
                        else if (word == PIXY_START_WORDX)
                        {
                            //
                            // We are word misaligned. Realign it by reading one byte and expecting it to be the high
                            // sync byte.
                            //
                            currBlock.sync = PIXY_START_WORD;
                            asyncReadData(RequestTag.ALIGN, 1);
                            if (debugEnabled)
                            {
                                dbgTrace.traceInfo(funcName, "Word misaligned, realigning...");
                            }
                        }
                        else
                        {
                            //
                            // We don't find the sync word, throw it away and initiate another read for SYNC.
                            //
                            asyncReadData(RequestTag.SYNC, 2);
                            if (word != 0)
                            {
                                tracer.traceWarn(funcName, "Unexpected word 0x%04x read in %s", word, requestTag);
                            }
                        }
                    }
                    break;

                case ALIGN:
                    if (length != 1)
                    {
                        //
                        // We should never come here. Let's throw an exception to catch this unlikely scenario.
                        //
                        throw new IllegalStateException(String.format("Unexpected data length %d in %s.",
                            length, requestTag));
                    }
                    else if (data[0] == PIXY_SYNC_HIGH)
                    {
                        //
                        // Found the expected upper sync byte, so initiate the read for CHECKSUM.
                        //
                        asyncReadData(RequestTag.CHECKSUM, 2);
                    }
                    else
                    {
                        //
                        // Don't see the expected upper sync byte, let's initiate another read for SYNC assuming we are
                        // now word aligned again.
                        //
                        asyncReadData(RequestTag.SYNC, 2);
                        tracer.traceWarn(funcName, "Unexpected data byte 0x%02x in %s", data[0], requestTag);
                    }
                    break;

                case CHECKSUM:
                    if (length != 2)
                    {
                        //
                        // We should never come here. Let's throw an exception to catch this unlikely scenario.
                        //
                        throw new IllegalStateException(String.format("Unexpected data length %d in %s.",
                            length, requestTag));
                    }
                    else
                    {
                        word = TrcUtil.bytesToInt(data[0], data[1]);
                        if (word == PIXY_START_WORD || word == PIXY_START_WORD_CC)
                        {
                            //
                            // We were expecting a checksum but found a sync word. It means that's the end-of-frame.
                            // Save away the sync word for the next frame and initiate the next read for CHECKSUM.
                            //
                            currBlock.sync = word;
                            asyncReadData(RequestTag.CHECKSUM, 2);
                            //
                            // Detected end-of-frame, convert the array list of objects into detected object array.
                            //
                            if (objects.size() > 0)
                            {
                                synchronized (objectLock)
                                {
                                    ObjectBlock[] array = new ObjectBlock[objects.size()];
                                    detectedObjects = objects.toArray(array);
                                    objects.clear();
                                    if (debugEnabled)
                                    {
                                        for (int i = 0; i < detectedObjects.length; i++)
                                        {
                                            dbgTrace.traceInfo(funcName, "[%02d] %s", i, detectedObjects[i].toString());
                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            //
                            // Looks like we have a checksum, save it away and initiate the read for the rest of the
                            // block. If the sync word was PIXY_START_WORD, then it is a 10-byte NORMAL_BLOCK, else it
                            // is a 12-byte COLOR_CODE_BLOCK.
                            //
                            currBlock.checksum = word;
                            if (currBlock.sync == PIXY_START_WORD)
                            {
                                asyncReadData(RequestTag.NORMAL_BLOCK, 10);
                            }
                            else if (currBlock.sync == PIXY_START_WORD_CC)
                            {
                                asyncReadData(RequestTag.COLOR_CODE_BLOCK, 12);
                            }
                            else
                            {
                                //
                                // We should never come here. Let's throw an exception to catch this unlikely scenario.
                                //
                                throw new IllegalStateException(String.format("Unexpected sync word 0x%04x in %s.",
                                    currBlock.sync, requestTag));
                            }
                        }
                    }
                    break;

                case NORMAL_BLOCK:
                case COLOR_CODE_BLOCK:
                    if (requestTag == RequestTag.NORMAL_BLOCK && length != 10 ||
                        requestTag == RequestTag.COLOR_CODE_BLOCK && length != 12)
                    {
                        //
                        // We should never come here. Let's throw an exception to catch this unlikely scenario.
                        //
                        throw new IllegalStateException(String.format("Unexpected data length %d in %s.",
                            length, requestTag));
                    }
                    else
                    {
                        int index;
                        runningChecksum = 0;
                        //
                        // Save away the signature and accumulate checksum.
                        //
                        index = 0;
                        word = TrcUtil.bytesToInt(data[index], data[index + 1]);
                        runningChecksum += word;
                        currBlock.signature = word;
                        //
                        // Save away the object center X and accumulate checksum.
                        //
                        index += 2;
                        word = TrcUtil.bytesToInt(data[index], data[index + 1]);
                        runningChecksum += word;
                        currBlock.centerX = word;
                        //
                        // Save away the object center Y and accumulate checksum.
                        //
                        index += 2;
                        word = TrcUtil.bytesToInt(data[index], data[index + 1]);
                        runningChecksum += word;
                        currBlock.centerY = word;
                        //
                        // Save away the object width and accumulate checksum.
                        //
                        index += 2;
                        word = TrcUtil.bytesToInt(data[index], data[index + 1]);
                        runningChecksum += word;
                        currBlock.width = word;
                        //
                        // Save away the object height and accumulate checksum.
                        //
                        index += 2;
                        word = TrcUtil.bytesToInt(data[index], data[index + 1]);
                        runningChecksum += word;
                        currBlock.height = word;
                        //
                        // If it is a COLOR_CODE_BLOCK, save away the object angle and accumulate checksum.
                        //
                        if (requestTag == RequestTag.COLOR_CODE_BLOCK)
                        {
                            index += 2;
                            word = TrcUtil.bytesToInt(data[index], data[index + 1]);
                            runningChecksum += word;
                            currBlock.angle = word;
                        }

                        if (runningChecksum == currBlock.checksum)
                        {
                            //
                            // Checksum is correct, add the object block.
                            //
                            objects.add(currBlock);
                            currBlock = null;
                        }
                        else
                        {
                            tracer.traceWarn(funcName, "Incorrect checksum %d (expecting %d).",
                                runningChecksum, currBlock.checksum);
                        }
                        //
                        // Initiate the read for the SYNC word of the next block.
                        //
                        asyncReadData(RequestTag.SYNC, 2);
                    }
                    break;

                default:
                    //
                    // We should never come here. Let's throw an exception to catch this unlikely scenario.
                    //
                    throw new IllegalStateException(String.format("Unexpected request tag %s.", requestTag));
            }
        }
        else if (length != 1)
        {
            //
            // We should never come here. In case we do, it is probably due to a read failure. Let's initiate another
            // read for the SYNC_LOW byte.
            //
            asyncReadData(RequestTag.SYNC_LOW, 1);
            tracer.traceWarn(funcName, "Unexpected data length %d in %s", length, requestTag);
        }
        else
        {
            switch (requestTag)
            {
                case SYNC_LOW:
                    //
                    // If we don't already have an object block allocated, allocate it now.
                    //
                    if (currBlock == null)
                    {
                        currBlock = new ObjectBlock();
                    }

                    if (data[0] == PIXY_SYNC_LOW || data[0] == PIXY_SYNC_LOW_CC)
                    {
                        //
                        // Found a sync low byte, initiate the read for SYNC_HIGH.
                        //
                        currBlock.sync = data[0];
                        asyncReadData(RequestTag.SYNC_HIGH, 1);
                    }
                    else
                    {
                        //
                        // We don't find the sync low byte, throw it away and initiate another read for SYNC_LOW.
                        //
                        asyncReadData(RequestTag.SYNC_LOW, 1);
                        if (data[0] != 0)
                        {
                            tracer.traceWarn(funcName, "Unexpected byte 0x%02x read in %s", data[0], requestTag);
                        }
                    }
                    break;

                case SYNC_HIGH:
                    if (data[0] == PIXY_SYNC_HIGH)
                    {
                        //
                        // Found the sync high byte, initiate the read for SYNC_CHECKSUM_LOW.
                        //
                        currBlock.sync = TrcUtil.bytesToInt((byte)currBlock.sync, data[0]);
                        asyncReadData(RequestTag.CHECKSUM_LOW, 1);
                    }
                    else
                    {
                        //
                        // It's not a sync word after all, discard it and initiate the read for SYNC_LOW again.
                        //
                        asyncReadData(RequestTag.SYNC_LOW, 1);
                        tracer.traceWarn(funcName, "Unexpected byte 0x%02x read in %s", data[0], requestTag);
                    }
                    break;

                case CHECKSUM_LOW:
                    currBlock.checksum = data[0];
                    asyncReadData(RequestTag.CHECKSUM_HIGH, 1);
                    break;

                case CHECKSUM_HIGH:
                    currBlock.checksum = TrcUtil.bytesToInt((byte)currBlock.checksum, data[0]);
                    if (currBlock.checksum == PIXY_START_WORD || currBlock.checksum == PIXY_START_WORD_CC)
                    {
                        //
                        // This is not a checksum. It is end-of-frame. Save away the sync word for the next frame
                        // and initiate the read for the next CHECKSUM_LOW.
                        //
                        currBlock.sync = currBlock.checksum;
                        asyncReadData(RequestTag.CHECKSUM_LOW, 1);
                        //
                        // Detected end-of-frame, convert the array list of objects into detected object array.
                        //
                        if (objects.size() > 0)
                        {
                            synchronized (objectLock)
                            {
                                ObjectBlock[] array = new ObjectBlock[objects.size()];
                                detectedObjects = objects.toArray(array);
                                objects.clear();
                                if (debugEnabled)
                                {
                                    for (int i = 0; i < detectedObjects.length; i++)
                                    {
                                        dbgTrace.traceInfo(funcName, "[%02d] %s", i, detectedObjects[i].toString());
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        //
                        // This is a checksum, initiate the read for SIGNATURE_LOW.
                        //
                        asyncReadData(RequestTag.SIGNATURE_LOW, 1);
                    }
                    break;

                case SIGNATURE_LOW:
                    runningChecksum = 0;
                    currBlock.signature = data[0];
                    asyncReadData(RequestTag.SIGNATURE_HIGH, 1);
                    break;

                case SIGNATURE_HIGH:
                    currBlock.signature = TrcUtil.bytesToInt((byte)currBlock.signature, data[0]);
                    runningChecksum += currBlock.signature;
                    if (currBlock.signature < 1 || currBlock.signature > 7)
                    {
                        //
                        // This is not a valid signature, discard it and initiate a read for SYNC_LOW.
                        //
                        asyncReadData(RequestTag.SYNC_LOW, 1);
                        tracer.traceWarn(funcName, "Unexpected signature %x in %s", currBlock.signature, requestTag);
                    }
                    else
                    {
                        //
                        // Found valid signature, initiate a read for CENTERX_LOW.
                        //
                        asyncReadData(RequestTag.CENTERX_LOW, 1);
                    }
                    break;

                case CENTERX_LOW:
                    currBlock.centerX = data[0];
                    asyncReadData(RequestTag.CENTERX_HIGH, 1);
                    break;

                case CENTERX_HIGH:
                    currBlock.centerX = TrcUtil.bytesToInt((byte)currBlock.centerX, data[0]);
                    runningChecksum += currBlock.centerX;
                    asyncReadData(RequestTag.CENTERY_LOW, 1);
                    break;

                case CENTERY_LOW:
                    currBlock.centerY = data[0];
                    asyncReadData(RequestTag.CENTERY_HIGH, 1);
                    break;

                case CENTERY_HIGH:
                    currBlock.centerY = TrcUtil.bytesToInt((byte)currBlock.centerY, data[0]);
                    runningChecksum += currBlock.centerY;
                    asyncReadData(RequestTag.WIDTH_LOW, 1);
                    break;

                case WIDTH_LOW:
                    currBlock.width = data[0];
                    asyncReadData(RequestTag.WIDTH_HIGH, 1);
                    break;

                case WIDTH_HIGH:
                    currBlock.width = TrcUtil.bytesToInt((byte)currBlock.width, data[0]);
                    runningChecksum += currBlock.width;
                    asyncReadData(RequestTag.HEIGHT_LOW, 1);
                    break;

                case HEIGHT_LOW:
                    currBlock.height = data[0];
                    asyncReadData(RequestTag.HEIGHT_HIGH, 1);
                    break;

                case HEIGHT_HIGH:
                    currBlock.height = TrcUtil.bytesToInt((byte)currBlock.height, data[0]);
                    runningChecksum += currBlock.height;
                    if (currBlock.sync == PIXY_START_WORD_CC)
                    {
                        asyncReadData(RequestTag.ANGLE_LOW, 1);
                    }
                    else
                    {
                        if (runningChecksum == currBlock.checksum)
                        {
                            //
                            // Checksum is correct, add the object block.
                            //
                            objects.add(currBlock);
                            currBlock = null;
                        }
                        else
                        {
                            tracer.traceWarn(funcName, "Incorrect checksum %d (expecting %d).",
                                runningChecksum, currBlock.checksum);
                        }
                        //
                        // Initiate the read for the SYNC_LOW of the next block.
                        //
                        asyncReadData(RequestTag.SYNC_LOW, 1);
                    }
                    break;

                case ANGLE_LOW:
                    currBlock.angle = data[0];
                    asyncReadData(RequestTag.ANGLE_HIGH, 1);
                    break;

                case ANGLE_HIGH:
                    currBlock.angle = TrcUtil.bytesToInt((byte)currBlock.angle, data[0]);
                    runningChecksum += currBlock.height;
                    if (runningChecksum == currBlock.checksum)
                    {
                        //
                        // Checksum is correct, add the object block.
                        //
                        objects.add(currBlock);
                        currBlock = null;
                    }
                    else
                    {
                        tracer.traceWarn(funcName, "Incorrect checksum %d (expecting %d).",
                            runningChecksum, currBlock.checksum);
                    }
                    //
                    // Initiate the read for the SYNC_LOW of the next block.
                    //
                    asyncReadData(RequestTag.SYNC_LOW, 1);
                    break;

                default:
                    //
                    // We should never come here. Let's throw an exception to catch this unlikely scenario.
                    //
                    throw new IllegalStateException(String.format("Unexpected request tag %s.", requestTag));
            }
        }
    }   //processData

    //
    // Implements TrcDeviceQueue.CompletionHandler interface.
    //

    /**
     * This method is called when the read operation has been completed.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param address specifies the data address read from if any, can be -1 if none specified.
     * @param data specifies the byte array containing data read.
     * @param error specifies true if the request failed, false otherwise. When true, data is invalid.
     * @return true if retry the read request, false otherwise (always no retry).
     */
    @Override
    public boolean readCompletion(Object requestTag, int address, byte[] data, boolean error)
    {
        final String funcName = "readCompletion";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.CALLBK, "tag=%s,addr=0x%x,data=%s,error=%s",
                requestTag, address, data != null? Arrays.toString(data): "null", Boolean.toString(error));
        }

        if (address == -1 && !error && data != null)
        {
            processData((RequestTag)requestTag, data, data.length);
        }

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.CALLBK, "false");
        }

        return false;
    }   //readCompletion

    /**
     * This method is called when the write operation has been completed.
     *
     * @param requestTag specifies the tag to identify the request. Can be null if none was provided.
     * @param address specifies the data address wrote to if any, can be -1 if none specified.
     * @param length specifies the number of bytes written.
     * @param error specifies true if the request failed to write the specified length, false otherwise.
     *              When true, length is invalid.
     */
    @Override
    public void writeCompletion(Object requestTag, int address, int length, boolean error)
    {
    }   //writeCompletion

}   //class FrcPixyCam
