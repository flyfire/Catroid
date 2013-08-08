package org.catrobat.catroid.test.facedetection;

import java.util.Random;

import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.facedetection.OnFaceDetectedListener;
import org.catrobat.catroid.facedetection.SlowFaceDetector;
import org.catrobat.catroid.test.utils.Reflection;
import org.catrobat.catroid.test.utils.Reflection.ParameterList;

import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.test.InstrumentationTestCase;

public class SlowFaceDetectorTest extends InstrumentationTestCase {

	private static final int DETECTION_WIDTH = 400;
	private static final int DETECTION_HEIGHT = 300;
	private static final float EYE_DISTANCE = 4.0f;

	private static final int COUNTER_INDEX = 0;
	private static final int SIZE_INDEX = 1;
	private static final int X_POSITION_INDEX = 2;
	private static final int Y_POSITION_INDEX = 3;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		ScreenValues.SCREEN_WIDTH = 720;
		ScreenValues.SCREEN_HEIGHT = 1080;
	}

	public void testStartAndStop() {

		Camera camera = null;
		try {
			camera = Camera.open();
		} catch (Exception exc) {
			fail("Camera not available (" + exc.getMessage() + ")");
		} finally {
			if (camera != null) {
				camera.release();
			}
		}

		SlowFaceDetector detector = new SlowFaceDetector();
		assertNotNull("Cannot instantiate SlowFaceDetector", detector);

		try {
			detector.startFaceDetection();
		} catch (Exception exc) {
			fail("Cannot start face detection (" + exc.getMessage() + ")");
		}

		try {
			detector.stopFaceDetection();
		} catch (Exception exc) {
			fail("Cannot stop face detection (" + exc.getMessage() + ")");
		}

		camera = null;
		try {
			camera = Camera.open();
		} catch (Exception exc) {
			fail("Ressources were not propperly released");
		} finally {
			if (camera != null) {
				camera.release();
			}
		}
	}

	public void testOnFaceDetectedListener() {
		SlowFaceDetector detector = new SlowFaceDetector();

		final int[] detectedFaces = new int[4];
		OnFaceDetectedListener detectionListener = new OnFaceDetectedListener() {

			public void onFaceDetected(Point position, int size) {
				detectedFaces[COUNTER_INDEX]++;
				detectedFaces[X_POSITION_INDEX] = position.x;
				detectedFaces[Y_POSITION_INDEX] = position.y;
				detectedFaces[SIZE_INDEX] = size;
			}
		};
		detector.addOnFaceDetectedListener(detectionListener);
		assertEquals("Face Detection Listener receives unexpected calls", 0, detectedFaces[COUNTER_INDEX]);

		PointF centerPoint = new PointF(DETECTION_WIDTH / 2, DETECTION_HEIGHT / 2);
		ParameterList parameters = new ParameterList(centerPoint, Float.valueOf(EYE_DISTANCE),
				Integer.valueOf(DETECTION_WIDTH), Integer.valueOf(DETECTION_HEIGHT));
		Reflection.invokeMethod(detector, "onFaceFound", parameters);
		//detector.onFaceFound(centerPoint, EYE_DISTANCE, DETECTION_WIDTH, DETECTION_HEIGHT);
		assertEquals("Face Detection Listener does not receive call", 1, detectedFaces[COUNTER_INDEX]);

		int expectedSize = (int) (EYE_DISTANCE * 400 / DETECTION_WIDTH);
		assertEquals("Unexpected size of face", expectedSize, detectedFaces[SIZE_INDEX]);

		int expectedXPosition = (int) (centerPoint.x / DETECTION_WIDTH * (-1) * ScreenValues.SCREEN_WIDTH)
				+ ScreenValues.SCREEN_WIDTH / 2;
		assertEquals("Unexpected x position of face", expectedXPosition, detectedFaces[X_POSITION_INDEX]);

		int expectedYPosition = (int) (centerPoint.y / DETECTION_HEIGHT * (-1) * ScreenValues.SCREEN_HEIGHT)
				+ ScreenValues.SCREEN_HEIGHT / 2;
		assertEquals("Unexpected y position of face", expectedYPosition, detectedFaces[Y_POSITION_INDEX]);

		Reflection.invokeMethod(detector, "onFaceFound", parameters);
		assertEquals("Face Detection Listener does not receive calls", 2, detectedFaces[COUNTER_INDEX]);

	}

	public void testFaceSizeBounds() {
		SlowFaceDetector detector = new SlowFaceDetector();

		final int[] faceSize = new int[1];
		OnFaceDetectedListener detectionListener = new OnFaceDetectedListener() {

			public void onFaceDetected(Point position, int size) {
				faceSize[0] = size;
			}
		};
		detector.addOnFaceDetectedListener(detectionListener);

		ParameterList parameters = new ParameterList(new PointF(), Float.valueOf(EYE_DISTANCE),
				Integer.valueOf(DETECTION_WIDTH), Integer.valueOf(DETECTION_HEIGHT));
		Reflection.invokeMethod(detector, "onFaceFound", parameters);
		assertTrue("Face size must not be negative", faceSize[0] >= 0);
		assertTrue("Illegal face size, range is [0,100]", faceSize[0] <= 100);

		Random random = new Random();
		parameters = new ParameterList(new PointF(), Float.valueOf(random.nextInt(DETECTION_WIDTH)),
				Integer.valueOf(DETECTION_WIDTH), Integer.valueOf(DETECTION_HEIGHT));
		Reflection.invokeMethod(detector, "onFaceFound", parameters);
		assertTrue("Face size must not be negative", faceSize[0] >= 0);
		assertTrue("Illegal face size, range is [0,100]", faceSize[0] <= 100);

		detector.removeOnFaceDetectedListener(detectionListener);
	}

	//	public void testCameraPreviewCallback() {
	//		SlowFaceDetectionTester testFaceDetector = new SlowFaceDetectionTester();
	//		assertFalse("Wrong tester setup", testFaceDetector.receivedFrame);
	//
	//		testFaceDetector.startFaceDetection();
	//		try {
	//			Thread.sleep(10000);
	//		} catch (InterruptedException e) {
	//			testFaceDetector.stopFaceDetection();
	//			fail("Thread interrupted unexpectedly");
	//		}
	//		testFaceDetector.stopFaceDetection();
	//		assertTrue("No preview frame received from camera", testFaceDetector.receivedFrame);
	//	}
	//
	//	private class SlowFaceDetectionTester extends SlowFaceDetector {
	//
	//		public boolean receivedFrame = false;
	//
	//		@Override
	//		public void onPreviewFrame(byte[] data, Camera camera) {
	//			receivedFrame = true;
	//			assertNotNull("No data in preview frame", data);
	//			this.notify();
	//		}
	//
	//	}

}
