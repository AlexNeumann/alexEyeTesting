package com.example.phantomnew.alexopencv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class FdActivity_fewerFrames extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    public static final int        JAVA_DETECTOR       = 0;
    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;


    private int learn_frames = 0;
    private Mat teplateR;
    private Mat teplateL;
    int method = 0;

    // matrix for zooming
    private Mat mZoomWindow;
    private Mat mZoomWindow2;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
   // private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private File                   mCascadeFileEye;
    private CascadeClassifier      mJavaDetector;
    private CascadeClassifier      mJavaDetectorEye;


    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;
    private SeekBar mMethodSeekbar;
    private TextView mValue;

    double xCenter = -1;
    double yCenter = -1;

    //Alex variables for math comparisons

    // variables for the minature tile game
    private String tilePosition = "c_2"; // center of screen

    // textViews for visual feedback
    private TextView textTop;
    private TextView textDown;
    private TextView textRight;
    private TextView textLeft;

    // baseline for first 20 frames
    private int baselineFrameCount = 0;
    private List<Double> baselineFrameX = new ArrayList<Double>();
    private List<Double> baselineFrameY = new ArrayList<Double>();
    private double baselineX = 0;
    private double baselineY = 0;
    Boolean baselineRecorded = false;

    // Left Eye
    private double previousX_left = 0;
    private double previousY_left = 0;

    private int framesProcessedMath = 0;
    private List<Double> framesXlist = new ArrayList<Double>();
    private List<Double> framesYlist = new ArrayList<Double>();
    // Right Eye
    private double previousX_right = 0;
    private double previousY_right = 0;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");


                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // load cascade file from application resources
                        InputStream ise = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFileEye = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");
                        FileOutputStream ose = new FileOutputStream(mCascadeFileEye);

                        while ((bytesRead = ise.read(buffer)) != -1) {
                            ose.write(buffer, 0, bytesRead);
                        }
                        ise.close();
                        ose.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier for eye");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileEye.getAbsolutePath());

                        cascadeDir.delete();
                        cascadeDirEye.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    //mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity_fewerFrames() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view_2);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
        mValue = (TextView) findViewById(R.id.method);

        // alex text views
        textTop = (TextView) findViewById(R.id.textTop);
        textDown = (TextView) findViewById(R.id.textDown);
        textRight = (TextView) findViewById(R.id.textRight);
        textLeft = (TextView) findViewById(R.id.textLeft);

        runOnUiThread(new Runnable(){
            @Override
            public void run() {

            }
        });

        mMethodSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser)
            {
                method = progress;
                switch (method) {
                    case 0:
                        mValue.setText("TM_SQDIFF");
                        break;
                    case 1:
                        mValue.setText("TM_SQDIFF_NORMED");
                        break;
                    case 2:
                        mValue.setText("TM_CCOEFF");
                        break;
                    case 3:
                        mValue.setText("TM_CCOEFF_NORMED");
                        break;
                    case 4:
                        mValue.setText("TM_CCORR");
                        break;
                    case 5:
                        mValue.setText("TM_CCORR_NORMED");
                        break;
                }


            }
        });
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mZoomWindow.release();
        mZoomWindow2.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }

        }

        if (mZoomWindow == null || mZoomWindow2 == null)
            CreateAuxiliaryMats();

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
        {	Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
                FACE_RECT_COLOR, 3);
            xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
            yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
            Point center = new Point(xCenter, yCenter);

            Imgproc.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);

            Imgproc.putText(mRgba, "[" + center.x + "," + center.y + "]",
                    new Point(center.x + 20, center.y + 20),
                    Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255,
                            255));

            Rect r = facesArray[i];
            // compute the eye area
            Rect eyearea = new Rect(r.x + r.width / 8,
                    (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
                    (int) (r.height / 3.0));
            // split it
            Rect eyearea_right = new Rect(r.x + r.width / 16,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            Rect eyearea_left = new Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            // draw the area - mGray is working grayscale mat, if you want to
            // see area in rgb preview, change mGray to mRgba
            Imgproc.rectangle(mGray, eyearea_left.tl(), eyearea_left.br(),
                    new Scalar(255, 0, 0, 255), 2);
            Imgproc.rectangle(mGray, eyearea_right.tl(), eyearea_right.br(),
                    new Scalar(255, 0, 0, 255), 2);

            if (learn_frames < 5) {
                teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
                learn_frames++;
            } else {
                // Learning finished, use the new templates for template
                // matching

                // add distinction between left and right so the baseline readings can be taken correctly
                String eyeSide = "right";
                match_eye(eyearea_right, teplateR, method, eyeSide);
                eyeSide = "left";
                match_eye(eyearea_left, teplateL, method, eyeSide);

            }


            // cut eye areas and put them to zoom windows
            //Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
            //Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());


        }

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);

        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void CreateAuxiliaryMats() {
        if (mGray.empty())
            return;

        int rows = mGray.rows();
        int cols = mGray.cols();

        if (mZoomWindow == null) {
            mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2
                    + cols / 10, cols);
            mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2
                    + cols / 10, cols);
        }

    }

    private void match_eye(Rect area, Mat mTemplate, int type, String eyeSide) {
        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size
        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return ;
        }
        Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);

        Imgproc.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 127, 0,
                255));
        Rect rec = new Rect(matchLoc_tx,matchLoc_ty);

        // After looking at the numbers here is what the variables actually do
        // matchLoc_tx = the top left corner of the drawn rectangle (the origin)
        // matchLoc_ty = the opposite corner of the origin. Bottom Right corner coordinates
        //Log.i("CallTest","matchLoc_tx: " + matchLoc_tx);
        //Log.i("CallTest","matchLoc_ty: " + matchLoc_ty);

        if (eyeSide.equals("right")){
            Imgproc.putText(mRgba, "X: " + matchLoc_tx.x + " | Y: " + matchLoc_tx.y,
                new Point(matchLoc.x + area.x - 30 , matchLoc.y + mTemplate.rows() + area.y - 40),
                Core.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(255, 255, 0, 255));
        }


        // if baseline has not been recorded, call getBaselineReading
        if(!baselineRecorded && eyeSide.equals("right")){
            getBaselineReading(matchLoc_tx.x, matchLoc_tx.y);
        }
        else if(baselineRecorded && eyeSide.equals("right")){
            calcEyeMovement(matchLoc_tx.x, matchLoc_tx.y);
        }

        // call method for math calcuations, passing the X and Y coordiantes of the Eye box
        //calcEyeMovement(matchLoc_tx.x, matchLoc_tx.y);
    }

    private void getBaselineReading(double xCoor, double yCoor){
        // use the first 100 frames to get a baseline reading for the left eye
        // assume the user stares at the center of screen
        if(baselineFrameCount < 100){
            baselineFrameX.add(xCoor);
            baselineFrameY.add(yCoor);

            baselineFrameCount = baselineFrameCount + 1;
        }
        else{
            Log.i("BASELINE","Number of baselines frames recorded: " + baselineFrameCount);

            // order the list
            Collections.sort(baselineFrameX);
            Collections.sort(baselineFrameY);

            double totalX = 0;
            double totalY = 0;
            // take the average for the X and Y coordinates and set them as our baseline
            // use the middle 40 frames
            for (int i = 40; i < 60; i++){
                totalX = totalX + baselineFrameX.get(i);
            }
            double avgX;
            avgX = totalX/20;
            baselineX = avgX;

            for (int i = 40; i < 60; i++){
                totalY = totalY + baselineFrameY.get(i);
            }
            double avgY;
            avgY = totalY/20;
            baselineY = avgY;

            baselineRecorded = true;
            Log.i("BASELINE","Baseline successfully recorded with Avg X: " + avgX + " Avg Y: " + avgY);
            for (int i = 0; i < baselineFrameCount; i++){
                Log.i("BASELINE","Frame X: " + baselineFrameX.get(i) + " - # " + i);
            }
        }
    }

    // uses the top left Coordinates of the Eye rectangle
    private void calcEyeMovement(double xCoor, double yCoor){
        // use 40 frames to determine what type of movement there was
        if (framesProcessedMath < 10){
            // more frames are needed
            framesXlist.add(xCoor);
            framesYlist.add(yCoor);
            framesProcessedMath = framesProcessedMath + 1;
        }
        else{
            // order the list of frames
            // use frames 25-35 to find the estimated final coordinate
            Collections.sort(framesXlist);
            Collections.sort(framesYlist);

            double totalX = 0;
            double totalY = 0;
            for(int i = 4; i < 7; i++){
                totalX = totalX + framesXlist.get(i);
                totalY = totalY + framesYlist.get(i);
            }
            totalX = totalX/3;
            totalY = totalY/3;
            Log.i("MOVEMENT","Estimated movement coordinates calculated... X: " + totalX + " Y: " + totalY + " Frames collected: "+ framesProcessedMath);

            // call compareToBaseline to determine if there was a movement in a certain direction
            compareToBaseline(totalX, totalY);

            // reset the frames count and delete the lists for next movement
            framesProcessedMath = 0;
            framesXlist.clear();
            framesYlist.clear();
        }
    }

    private void compareToBaseline(final double x, final double y) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // compare to our baseline reading
                double differenceX = x - baselineX;
                double differenceY = y - baselineY;

                Log.i("MOVEMENT","Difference X: " + differenceX + " Difference Y: " + differenceY);

                // a negative difference for X: means movement RIGHT (irl)
                // a positive difference for X: means movement LEFT (irl)

                // a negative difference for Y: means movement UP
                // a positive difference for Y: means movement DOWN

                // this is just a simple check. it does not take into account diagonal movement yet
                if (differenceX < -2.9) {
                    // Right movement
                    handleMovement("right");
                    textRight.setBackgroundColor(Color.BLUE);
                }
                if (differenceX > 2.9) {
                    // Left movement
                    handleMovement("left");
                    textLeft.setBackgroundColor(Color.BLUE);
                }
                if (differenceY < -2.9) {
                    // Up movement
                    handleMovement("up");
                    textTop.setBackgroundColor(Color.BLUE);
                }
                if (differenceY > 2.9) {
                    // Down movement
                    handleMovement("down");
                    textDown.setBackgroundColor(Color.BLUE);
                }
            }
        });
    };

    // Code from the game already
    public void handleMovement(String direction){
        if(direction == "right"){
            // find current tile position
            String segments[] = tilePosition.split("_");
            String tileCol = segments[segments.length - 1];
            String tileRow = segments[0];

            // convert tileCol to int
            int intTileCol = Integer.parseInt(tileCol);

            // check space to the right of tile
            if(intTileCol < 4){
                int newTileCol = intTileCol + 1;
                String nextTile = tileRow + "_" + Integer.toString(newTileCol);

                // call draw tile
                drawTile(nextTile);
            }
            else{
                Log.i("GAME","There is a wall to your right.");
            }
        }
        else if(direction == "left"){
            // find current tile position
            String segments[] = tilePosition.split("_");
            String tileCol = segments[segments.length - 1];
            String tileRow = segments[0];

            // convert tileCol to int
            int intTileCol = Integer.parseInt(tileCol);

            // check space to the left of tile
            if(intTileCol > 0){
                int newTileCol = intTileCol - 1;
                String nextTile = tileRow + "_" + Integer.toString(newTileCol);

                // call draw tile
                drawTile(nextTile);
            }
            else{
                Log.i("GAME","There is a wall to your left.");
            }
        }
        else if (direction == "up"){
            // find current tile position
            String segments[] = tilePosition.split("_");
            String tileCol = segments[segments.length - 1];
            String tileRow = segments[0];

            if(!tileRow.contains("a")){
                // use ASCII values for letters
                char alpha = tileRow.charAt(0);
                int nextValue = (int)alpha - 1;

                char beta = (char)nextValue;

                String newRow = String.valueOf(beta);
                String nextTile = beta + "_" + tileCol;

                drawTile(nextTile);
            }
            else{
                Log.i("GAME", "There is a wall tile above you.");
            }
        }
        else if (direction == "down"){
            // find current tile position
            String segments[] = tilePosition.split("_");
            String tileCol = segments[segments.length - 1];
            String tileRow = segments[0];

            if (!tileRow.contains("e")) {
                // make use of ASCII values for letters
                char alpha = tileRow.charAt(0);
                int nextValue = (int) alpha + 1;

                char beta = (char) nextValue;

                String newRow = String.valueOf(beta);
                String nextTile = beta + "_" + tileCol;

                drawTile(nextTile);
            }
            else{
                Log.i("GAME", "There is a wall tile to below you.");
            }
        }
    }

    public void drawTile(String newTilePosition){
        // clear the old tilePosition
        int ResID = getResources().getIdentifier(tilePosition, "id", getPackageName());
        ImageView drawSnakeTile = (ImageView) findViewById(ResID);
        drawSnakeTile.setBackgroundColor(Color.WHITE);

        // change the new Tile to red
        Log.i("GAME","drawTile() was called...");
        int ResID2 = getResources().getIdentifier(newTilePosition, "id", getPackageName());
        ImageView drawSnakeTile2 = (ImageView) findViewById(ResID2);
        drawSnakeTile2.setBackgroundColor(Color.RED);

        // update the tilePosition
        tilePosition = newTilePosition;
    }

    public void clearBoard(){
        // render every tile back to white background
        List<String> clearRows = new ArrayList<String>();
        clearRows.addAll((Arrays.asList("a","b","c","d","e")));
        List<String> clearCols = new ArrayList<String>();
        clearCols.addAll((Arrays.asList("0","1","2","3","4")));
        Log.i("CALL","clearBoard() was called...");
        // go through all rows
        for(int i = 0; i < clearRows.size(); i++){
            // for each column of each row
            for (int j = 0; j < clearCols.size(); j++){
                String tileToClear = clearRows.get(i) + "_" + clearCols.get(j);
                Log.i("CALL","tileToClear = " + tileToClear);
                int ResID = getResources().getIdentifier(tileToClear, "id", getPackageName());
                ImageView clearTile = (ImageView) findViewById(ResID);
                clearTile.setBackgroundColor(Color.WHITE);
            }
        }

    }


    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
        Mat template = new Mat();
        Mat mROI = mGray.submat(area);
        MatOfRect eyes = new MatOfRect();
        Point iris = new Point();
        Rect eye_template = new Rect();
        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length;) {
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRgba.submat(eye_only_rectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);
            Imgproc.rectangle(mRgba, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    public void onRecreateClick(View v)
    {
        learn_frames = 0;
        baselineRecorded = false;

        //clear baseline reading
        baselineFrameX.clear();
        baselineFrameY.clear();
        baselineFrameCount = 0;

        Log.i("ClickEvent","Button 'recreate' was clicked...");
        textRight.setBackgroundColor(Color.GRAY);
        textLeft.setBackgroundColor(Color.GRAY);
        textTop.setBackgroundColor(Color.GRAY);
        textDown.setBackgroundColor(Color.GRAY);
        clearBoard();
        tilePosition="c_2";
        drawTile(tilePosition);
    }



}
