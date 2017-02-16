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

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import trclib.TrcDbgTrace;

/**
 * This class implements an OpenCV face detector using the provided classifier.
 */
public class FrcFaceDetector extends FrcOpenCVDetector<MatOfRect>
{
    private static final String moduleName = "FrcFaceDetector";
    private static final boolean debugEnabled = false;
    private static final boolean tracingEnabled = false;
    private static final TrcDbgTrace.TraceLevel traceLevel = TrcDbgTrace.TraceLevel.API;
    private static final TrcDbgTrace.MsgLevel msgLevel = TrcDbgTrace.MsgLevel.INFO;
    private TrcDbgTrace dbgTrace = null;

    private static final int NUM_OBJECT_BUFFERS = 2;

    private MatOfRect[] detectedFacesBuffers;
    private CascadeClassifier faceDetector;
    private volatile Rect[] faceRects = null;
    private volatile Mat image = null;
    private boolean videoOutEnabled = false;

    /**
     * Constructor: Create an instance of the object.
     *
     * @param instanceName specifies the instance name.
     * @param classifierPath specifies the file path for the classifier.
     * @param videoIn specifies the video input stream.
     * @param videoOut specifies the video output stream.
     */
    public FrcFaceDetector(
        final String instanceName, final String classifierPath, CvSink videoIn, CvSource videoOut)
    {
        super(instanceName, videoIn, videoOut);

        if (debugEnabled)
        {
            dbgTrace = new TrcDbgTrace(moduleName, tracingEnabled, traceLevel, msgLevel);
        }

        //
        // Preallocate MatOfRect since this is expensive to allocate.
        //
        detectedFacesBuffers = new MatOfRect[NUM_OBJECT_BUFFERS];
        for (int i = 0; i < detectedFacesBuffers.length; i++)
        {
            detectedFacesBuffers[i] = new MatOfRect();
        }
        setPreallocatedObjectBuffers(detectedFacesBuffers);

        faceDetector = new CascadeClassifier(classifierPath);
        if (faceDetector.empty())
        {
            throw new RuntimeException("Failed to load Cascade Classifier <" + classifierPath + ">");
        }
    }   //FrcFaceDetector

    /**
     * This method returns an array of rectangles of last detected faces.
     *
     * @return array of rectangle of last detected faces.
     */
    public Rect[] getFaceRects()
    {
        final String funcName = "getFaceRects";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API);
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        return faceRects;
    }   //getFaceRects

    /**
     * This method update the video stream with the detected faces overlay on the image as rectangles.
     *
     * @param color specifies the color of the rectangle outline overlay onto the detected faces.
     * @param thickness specifies the thickness of the rectangle outline.
     */
    public void putFrame(Scalar color, int thickness)
    {
        if (image != null)
        {
            super.putFrame(image, faceRects, color, thickness);
        }
    }   //putFrame

    /**
     * This method update the video stream with the detected faces overlay on the image as rectangles.
     */
    public void putFrame()
    {
        if (image != null)
        {
            super.putFrame(image, faceRects, new Scalar(0, 255, 0), 0);
        }
    }   //putFrame

    /**
     * This method enables/disables the video out stream.
     *
     * @param enabled specifies true to enable video out stream, false to disable.
     */
    public void setVideoOutEnabled(boolean enabled)
    {
        final String funcName = "setVideoOutEnabled";

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.API, "enabled=%s", Boolean.toString(enabled));
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.API);
        }

        videoOutEnabled = enabled;
    }   //setVideoOutEnabled

    //
    // Implements the FrcVisionTask.VisionProcesor interface.
    //

    /**
     * This method is called to detect objects in the image frame.
     *
     * @param image specifies the image to be processed.
     * @param detectedTargets specifies the preallocated buffer to hold the detected targets.
     * @param detectedObjects specifies the object rectangle array to hold the detected objects.
     * @return detected objects, null if none detected.
     */
    @Override
    public MatOfRect detectObjects(Mat image, MatOfRect detectedObjects)
    {
        final String funcName = "detectedObjects";
        MatOfRect detected = null;

        if (debugEnabled)
        {
            dbgTrace.traceEnter(funcName, TrcDbgTrace.TraceLevel.CALLBK, "image=%s,objRects=%s",
                image.toString(), detectedObjects.toString());
        }

        faceDetector.detectMultiScale(image, detectedObjects);
        if (!detectedObjects.empty())
        {
            faceRects = detectedObjects.toArray();
            detected = detectedObjects;
        }
        else
        {
            faceRects = null;
        }

        if (videoOutEnabled)
        {
            putFrame();
        }

        this.image = image;

        if (debugEnabled)
        {
            dbgTrace.traceExit(funcName, TrcDbgTrace.TraceLevel.CALLBK, "=%s", Boolean.toString(detected != null));
        }

        return detected;
    }   //detectedObjects

}   //class FrcFaceDetector
