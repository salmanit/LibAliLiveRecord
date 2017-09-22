package sage.libaliliverecord.record;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.alibaba.livecloud.live.AlivcMediaFormat;
import com.alibaba.livecloud.live.AlivcMediaRecorder;
import com.alibaba.livecloud.live.AlivcMediaRecorderFactory;
import com.alibaba.livecloud.live.AlivcRecordReporter;
import com.alibaba.livecloud.live.AlivcStatusCode;
import com.alibaba.livecloud.live.OnLiveRecordErrorListener;
import com.alibaba.livecloud.live.OnNetworkStatusListener;
import com.alibaba.livecloud.live.OnRecordStatusListener;
import com.alibaba.livecloud.model.AlivcWatermark;
import com.duanqu.qupai.jni.ApplicationGlue;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sage on 2017/8/9.
 * Description:
 */

public class SurfaceViewAli extends SurfaceView {

    static {

    }

    private void loadLib(Context context) {
        System.loadLibrary("gnustl_shared");
//        System.loadLibrary("ijkffmpeg");//目前使用微博的ijkffmpeg会出现1K再换wifi不重连的情况
        System.loadLibrary("qupai-media-thirdparty");
//        System.loadLibrary("alivc-media-jni");
        System.loadLibrary("qupai-media-jni");
        ApplicationGlue.initialize(context);
    }

    private GestureDetector mDetector;
    private ScaleGestureDetector mScaleDetector;
    private AlivcMediaRecorder mMediaRecorder;
    private AlivcRecordReporter mRecordReporter;
    private Surface mPreviewSurface;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private Map<String, Object> mConfigure = new HashMap<>();//所有的默认配置参数都在这里了

    public SurfaceViewAli(Context context) {
        super(context);
        initDefaultConfig();
        initSome();
    }

    public SurfaceViewAli(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDefaultConfig();
        initSome();
    }

    public SurfaceViewAli(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDefaultConfig();
        initSome();
    }


    private void initSome() {
        getHolder().addCallback(_CameraSurfaceCallback);
        setOnTouchListener(mOnTouchListener);

        //对焦，缩放
        mDetector = new GestureDetector(getContext(), mGestureDetector);
        mScaleDetector = new ScaleGestureDetector(getContext(), mScaleGestureListener);

        mMediaRecorder = AlivcMediaRecorderFactory.createMediaRecorder();
        mMediaRecorder.init(getContext());
        mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
//        mDataStatistics.setReportListener(mReportListener);
        /**this method only can be called after mMediaRecorder.init(),
         * otherwise will return null;*/
        mRecordReporter = mMediaRecorder.getRecordReporter();

        mMediaRecorder.setOnRecordStatusListener(mRecordStatusListener);
        mMediaRecorder.setOnNetworkStatusListener(mOnNetworkStatusListener);
        mMediaRecorder.setOnRecordErrorListener(mOnErrorListener);

        mMediaRecorder.setBeautyLevel(beautyLevel);
    }

    int beautyLevel = AlivcMediaFormat.BEAUTY_LEVEL_FOUR;//1到7，美颜级别，从低到高
    int resolution = AlivcMediaFormat.OUTPUT_RESOLUTION_720P;//从0到5，分别是240,360.。。1080p
    int minBitrate = 500;
    int maxBitrate = 1800;
    int bestBitrate = 1200;
    int initBitrate = 1200;
    boolean screenOrientation = false;
    int frameRate = 30;//帧率
    long timeOut = 20000;//超时重连时间
    private AlivcWatermark mWatermark;

    private void initDefaultConfig() {
        loadLib(getContext().getApplicationContext());
        mConfigure.put(AlivcMediaFormat.KEY_CAMERA_FACING, AlivcMediaFormat.CAMERA_FACING_FRONT);
        mConfigure.put(AlivcMediaFormat.KEY_MAX_ZOOM_LEVEL, 3);
        mConfigure.put(AlivcMediaFormat.KEY_OUTPUT_RESOLUTION, resolution);
        mConfigure.put(AlivcMediaFormat.KEY_MIN_VIDEO_BITRATE, minBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_MAX_VIDEO_BITRATE, maxBitrate * 1000);
        mConfigure.put(AlivcMediaFormat.KEY_BEST_VIDEO_BITRATE, bestBitrate * 1000);//最优码率　（单位：bps）
        mConfigure.put(AlivcMediaFormat.KEY_INITIAL_VIDEO_BITRATE, initBitrate * 1000);//配置码率 500kbps
        mConfigure.put(AlivcMediaFormat.KEY_DISPLAY_ROTATION, screenOrientation ?
                AlivcMediaFormat.DISPLAY_ROTATION_90 : AlivcMediaFormat.DISPLAY_ROTATION_0);
        mConfigure.put(AlivcMediaFormat.KEY_EXPOSURE_COMPENSATION, -1);//曝光度
        mConfigure.put(AlivcMediaFormat.KEY_FRAME_RATE, frameRate);//帧率
        mConfigure.put(AlivcMediaFormat.KEY_RECONNECT_TIMEOUT, timeOut);
        mConfigure.put(AlivcMediaFormat.KEY_AUDIO_BITRATE, 32*1000);//音频码率（建议设置为32000)
        mConfigure.put(AlivcMediaFormat.KEY_AUDIO_SAMPLE_RATE, 44100);//音频采样率（建议设置为44100）
    }


    public void changeWaterMark(String url, int marginX, int marginY, int position) {
//        DisplayMetrics displayMetrics=getContext().getResources().getDisplayMetrics();
//        int screenW=displayMetrics.widthPixels;
//        int screenH=displayMetrics.heightPixels;

        mWatermark = new AlivcWatermark.Builder()
                .watermarkUrl(url)
                .paddingX(marginX)
                .paddingY(marginY)
                .site(position)
                .build();
        if (mConfigure.containsKey(AlivcMediaFormat.KEY_WATERMARK)) {
            mConfigure.remove(AlivcMediaFormat.KEY_WATERMARK);
        }
        mConfigure.put(AlivcMediaFormat.KEY_WATERMARK, mWatermark);
    }

    public void changeOrientation(boolean screenOrientation) {
        ((Activity) getContext()).setRequestedOrientation(screenOrientation ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mConfigure.put(AlivcMediaFormat.KEY_DISPLAY_ROTATION, screenOrientation ?
                AlivcMediaFormat.DISPLAY_ROTATION_90 : AlivcMediaFormat.DISPLAY_ROTATION_0);
        if (mMediaRecorder != null)
            mMediaRecorder.prepare(mConfigure, mPreviewSurface);
    }

    /**
     * 是否开启闪光灯
     */
    public void changeFlashLight(boolean isOn) {
        if (isOn) {
            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_FLASH_MODE_ON);
        } else {
            mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_FLASH_MODE_ON);
        }
    }

    /**
     * 是否开启美颜
     */
    public void changeBeautyState(boolean isChecked) {
        if (isChecked) {
            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
        } else {
            mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
        }
    }

    /**
     * 切换前后摄像头
     */
    public void changeCamera() {
        int currFacing = mMediaRecorder.switchCamera();
        if (currFacing == AlivcMediaFormat.CAMERA_FACING_FRONT) {
            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
        }
        mConfigure.put(AlivcMediaFormat.KEY_CAMERA_FACING, currFacing);
    }

    /**
     * 静音开关
     */
    public void changeMute(boolean isChecked) {
        if (isChecked) {
            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_MUTE_ON);
        } else {
            mMediaRecorder.removeFlag(AlivcMediaFormat.FLAG_MUTE_ON);
        }
    }


    String pushUrl = "";//推流地址
    boolean isRecording = false;

    public void setPushUrl(String pushUrl) {
        this.pushUrl = pushUrl;
    }

    public void startPush() {
        if (TextUtils.isEmpty(pushUrl)) {
            return;
        }
        try {
            mMediaRecorder.startRecord(pushUrl);
            isRecording = true;
        } catch (Exception e) {
        }

    }

    public void stopPush() {
        mMediaRecorder.stopRecord();
        mMediaRecorder.reset();         //释放预览资源
        mMediaRecorder.release();       //释放推流资源
        isRecording = false;
    }

    private final SurfaceHolder.Callback _CameraSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            holder.setKeepScreenOn(true);
            mPreviewSurface = holder.getSurface();
            System.out.println("===============surfaceCreated:" + holder.getSurfaceFrame().toString());
            if (mMediaRecorder != null) {
                mMediaRecorder.prepare(mConfigure, mPreviewSurface);
                mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_BEAUTY_ON);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            mMediaRecorder.setPreviewSize(width, height);
            mPreviewWidth = width;
            mPreviewHeight = height;
            System.out.println("===============surfaceChanged" + width + "/" + height + "==" + format);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mPreviewSurface = null;
            System.out.println("===============surfaceDestroyed");
            //下面2句话取消就可以实现后台推流 但是部分手机不支持
            //mMediaRecorder.stopRecord();
            // mMediaRecorder.reset();
        }
    };

    private GestureDetector.OnGestureListener mGestureDetector = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            if (mPreviewWidth > 0 && mPreviewHeight > 0) {
                float x = motionEvent.getX() / mPreviewWidth;
                float y = motionEvent.getY() / mPreviewHeight;
                mMediaRecorder.focusing(x, y);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }
    };
    private OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mDetector.onTouchEvent(motionEvent);
            mScaleDetector.onTouchEvent(motionEvent);
            return true;
        }
    };
    private ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mMediaRecorder.setZoom(scaleGestureDetector.getScaleFactor());
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        }
    };


    private OnRecordStatusListener mRecordStatusListener = new OnRecordStatusListener() {
        @Override
        public void onDeviceAttach() {
//            mMediaRecorder.addFlag(AlivcMediaFormat.FLAG_AUTO_FOCUS_ON);
        }

        @Override
        public void onDeviceAttachFailed(int facing) {

        }

        @Override
        public void onSessionAttach() {
            if (isRecording && !TextUtils.isEmpty(pushUrl)) {
                mMediaRecorder.startRecord(pushUrl);
            }
            mMediaRecorder.focusing(0.5f, 0.5f);
        }

        @Override
        public void onSessionDetach() {

        }

        @Override
        public void onDeviceDetach() {

        }

        @Override
        public void onIllegalOutputResolution() {
            Log.d(TAG, "selected illegal output resolution:选择输出分辨率过大");
            if (errorResult != null) {
                errorResult.failed(0, "输出分辨率过大");
            }
        }
    };

    String TAG = getClass().getName();
    private OnNetworkStatusListener mOnNetworkStatusListener = new OnNetworkStatusListener() {
        /**
         * 网络较差时的回调，此时推流buffer为满的状态，会执行丢包，此时数据流不能正常推送
         */
        @Override
        public void onNetworkBusy() {
            Log.d("network_status", "==== on network busy ====当前网络状态极差，已无法正常流畅直播，确认要继续直播吗？");
            if (errorResult != null) {
                errorResult.failed(1, "当前网络状态极差，已无法正常流畅直播，确认要继续直播吗？");
            }
        }

        /**
         * 网络空闲状态，此时本地推流buffer不满，数据流可正常发送
         */
        @Override
        public void onNetworkFree() {
            Log.d("network_status", "===== on network free ====");
        }

        @Override
        public void onConnectionStatusChange(int status) {
            Log.d(TAG, "ffmpeg Live stream connection status-->" + status);

            switch (status) {
                case AlivcStatusCode.STATUS_CONNECTION_START:
                    Log.d(TAG, "Start live stream connection!");
                    break;
                case AlivcStatusCode.STATUS_CONNECTION_ESTABLISHED:
                    Log.d(TAG, "Live stream connection is established!");
//                    showIllegalArgumentDialog("链接成功");
                    break;
                case AlivcStatusCode.STATUS_CONNECTION_CLOSED:
                    Log.d(TAG, "Live stream connection is closed!");
//                    mLiveRecorder.stop();
//                    mLiveRecorder.release();
//                    mLiveRecorder = null;
//                    mMediaRecorder.stopRecord();
                    break;
            }
        }

//        @Override
//        public void onFirstReconnect() {
//            ToastUtils.showToast(LiveCameraActivity.this, "首次重连");
//        }

        /**
         * 重连失败
         * @return false:停止重连 true:继续重连
         * 说明: ｓｄｋ检测到检测到需要重连时将会自动执行重连，直到重连成功
         *或者重连尝试超时，
         * 超时时间可以通过{@link AlivcMediaFormat#KEY_RECONNECT_TIMEOUT}设置
         * 默认为５ｓ，超时后将触发此回调，若返回true表示继续开始新的一轮尝试，
         *返回false，
         * 表示不再尝试
         */
        @Override
        public boolean onNetworkReconnectFailed() {
            Log.d(TAG, "Reconnect timeout, not adapt to living:长时间重连失败，已不适合直播，请退出");
            if (errorResult != null) {
                errorResult.failed(0, "长时间重连失败，已不适合直播，请退出..");
            }
            mMediaRecorder.stopRecord();
            return false;
        }
    };

    private OnLiveRecordErrorListener mOnErrorListener = new OnLiveRecordErrorListener() {
        @Override
        public void onError(int errorCode) {
            Log.d(TAG, "Live stream connection error-->" + errorCode);

            switch (errorCode) {
                case AlivcStatusCode.ERROR_ILLEGAL_ARGUMENT:
                    //("-22错误产生");
                case AlivcStatusCode.ERROR_SERVER_CLOSED_CONNECTION:
                case AlivcStatusCode.ERORR_OUT_OF_MEMORY:
                case AlivcStatusCode.ERROR_CONNECTION_TIMEOUT:
                case AlivcStatusCode.ERROR_BROKEN_PIPE:
                case AlivcStatusCode.ERROR_IO:
                case AlivcStatusCode.ERROR_NETWORK_UNREACHABLE:
                    //"Live stream connection error-->"
                    break;
                default:
            }
            if (errorResult != null) {
                errorResult.failed(0, "连接失败..");
            }
        }
    };

    public void setErrorResult(ErrorResult errorResult) {
        this.errorResult = errorResult;
    }

    private ErrorResult errorResult;

}
