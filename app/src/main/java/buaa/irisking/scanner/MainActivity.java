package buaa.irisking.scanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.irisking.irisalgo.bean.IKEnrIdenStatus;
import com.irisking.irisalgo.util.AadharIrisISOFormat7;
import com.irisking.irisalgo.util.Config;
import com.irisking.irisalgo.util.EnrFeatrueStruct;
import com.irisking.irisalgo.util.EnumDeviceType;
import com.irisking.irisalgo.util.EnumEyeType;
import com.irisking.irisalgo.util.FeatureList;
import com.irisking.irisalgo.util.FileUtil;
import com.irisking.irisalgo.util.IKALGConstant;
import com.irisking.irisalgo.util.IrisInfo;
import com.irisking.irisalgo.util.Person;
import com.irisking.irisalgo.util.Preferences;
import buaa.irisking.irisapp.R;
import com.irisking.scanner.callback.CameraPreviewCallback;
import com.irisking.scanner.callback.IrisCaptureCallback;
import com.irisking.scanner.callback.IrisProcessCallback;
import com.irisking.scanner.model.EyePosition;
import com.irisking.scanner.presenter.IrisConfig;
import com.irisking.scanner.presenter.IrisPresenter;
import com.irisking.scanner.util.ImageUtil;
import com.irisking.scanner.util.TimeArray;

// 主文件，完成界面显示，UI控件控制等逻辑
@SuppressWarnings("unused")
public class MainActivity extends Activity implements OnClickListener, RadioGroup.OnCheckedChangeListener {

	private TimeArray uvcTimeArray = new TimeArray();
	
	private String curName = "test";
	
	public boolean previewParaUpdated = false;
	
	// ============声音播放器=============
	//语音提示开关
    public SoundPool soundPool = null;
    private int frameIndex = 0;
    public int fartherId;
    public int closerId;
    public int enrosuccId;
    public int idensuccId;
    public int moveLeftId;
    public int moveRightId;
	//===================================

	//===============控件================
	private Button mIrisRegisterBtn;
	private Button mIrisCaptureBtn;
	private Button mIrisIdenBtn;
	//add by yumingyuan
	private Button mPinIdenBtn;
	//add by yumingyuan
	private TextView mResultTextViewEnrRecFinal;
	private EditText mUserNameEditText; // 显示用户名
	private ImageView leftView; 
	private ImageView rightView; 
	private TextView mFrameRateTextView; // 帧率显示文本
	private IrisPresenter mIrisPresenter;
	private SurfaceView svCamera;
	private RadioGroup mRgEye;
	private RoundProgressBar progressBar;
	private EyeView mEyeView; // 显示提示框的view界面
	//add by yumingyuan 20190117
	private EditText mPinedit;
	//add by yumingyuan
	//===================================
	
	//=========画IR图像=========
	private SurfaceHolder holder;
	private Matrix matrix;
	
	public  int eyeViewWidth = 0;
	public  int eyeViewHeight = 0;
	//==========================
	
	// ======load feature list==========
	FeatureList irisLeftData = new FeatureList();
	FeatureList irisRightData = new FeatureList();

	private SqliteDataBase sqliteDataBase;
	
	private int irisMode = IKALGConstant.IR_IM_EYE_BOTH;
	private int maxFeatureCount = 900;
	//==================================
	 //屏幕中双眼的坐标位置
	private float eyeX1;
	private float eyeX2;
	private float eyeHeight;
	private float hor_scale;//横屏下缩放比例
	
	private IrisConfig.EnrollConfig mEnrollConfig;
	private IrisConfig.CaptureConfig mCaptureConfig;
	private IrisConfig.IdentifyConfig mIdentifyConfig;
	
	SharedPreferences sp;
	private String sp_name = "iris_sp_user";
	private String sp_count_name = "iris_sp_user_count";
    private boolean isStop;
    private static SurfaceHandler mSurfaceHandler;
    public static final int HANDLER_DRAW_IMAGE = 0x0010;
    public static final int HANDLER_UPDATE_TEXT = 0x0011;
    public static final int HANDLER_RESET_UI = 0x0012;
    public static final int HANDLER_RESET_PROGRESS = 0x0013;
    public static final int HANDLER_SHOW_LEFT = 0x0014;
    public static final int HANDLER_SHOW_RIGHT = 0x0015;
    //add by yumingyuan 20190118尝试屏蔽按键
    public static final int FLAG_BackKEY_DISPATCHED = 0x80000000;
    public static final int FLAG_DiialKY_DISPATCHED=0x80000001;
    //add by yumingyuan 20190118尝试屏蔽按键
    private EnrFeatrueStruct leftECEyeFeat;
	private EnrFeatrueStruct rightECEyeFeat;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sqliteDataBase = SqliteDataBase.getInstance(this);
		sp = this.getSharedPreferences(sp_name, Context.MODE_PRIVATE);

		requestWindowFeature(Window.FEATURE_NO_TITLE); // 全屏，不出现图标
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		//add by yumingyuan注册Flag
        getWindow().setFlags(FLAG_BackKEY_DISPATCHED, FLAG_BackKEY_DISPATCHED);//关键代码
		getWindow().setFlags(FLAG_DiialKY_DISPATCHED, FLAG_DiialKY_DISPATCHED);//关键代码
        //add by yumingyuan注册Flag
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		screenUiAdjust();
		
		setContentView(R.layout.activity_iris_recognition);
		
		initSound();
		initUI();
		mSurfaceHandler = new SurfaceHandler(MainActivity.this);
		if(Config.DEVICE_USBCAMERA){
			mIrisPresenter = new IrisPresenter(this, uvcPreviewCallback);
		} else if(Config.DEVICE_DOUBLECAMERA){
			
		} else{
			mIrisPresenter = new IrisPresenter(this, irPreviewCallback);
		}
		
		mEnrollConfig = new IrisConfig.EnrollConfig();
		mCaptureConfig = new IrisConfig.CaptureConfig();
		mIdentifyConfig = new IrisConfig.IdentifyConfig();
		
		initIrisData();
	}
	
	@Override
	protected void onStart() {
		mIrisPresenter.resume();
		isStop = false;
		super.onStart();
	}
	
	private void initSound() {
		soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
        fartherId = soundPool.load(getApplicationContext(), R.raw.farther, 0);
        closerId = soundPool.load(getApplicationContext(), R.raw.closer, 0);
        enrosuccId = soundPool.load(getApplicationContext(), R.raw.enrsucc, 0);
        idensuccId = soundPool.load(getApplicationContext(), R.raw.idensucc, 0);
        moveLeftId = soundPool.load(getApplicationContext(), R.raw.moveleft, 0);
        moveRightId = soundPool.load(getApplicationContext(), R.raw.moveright, 0);
	}
	
	private void initIrisData() {
		// 2017.09.05 10:25修改，从数据库查询所有特征文件
		ArrayList<IrisUserInfo> leftEyeList = (ArrayList<IrisUserInfo>) sqliteDataBase.queryLeftFeature();
		ArrayList<IrisUserInfo> rightEyeList = (ArrayList<IrisUserInfo>) sqliteDataBase.queryRightFeature();

		if ((leftEyeList == null || leftEyeList.size() == 0) && (rightEyeList == null || rightEyeList.size() == 0)) {
			return;
		}
		irisLeftData.clear();
		irisRightData.clear();
		for (int i = 0; i < leftEyeList.size(); i++) {
			irisLeftData.add(new Person(leftEyeList.get(i).m_UserName,leftEyeList.get(i).m_Uid, 1), EnumEyeType.LEFT,
					leftEyeList.get(i).m_LeftTemplate);
		}

		for (int i = 0; i < rightEyeList.size(); i++) {
			irisRightData.add(new Person(rightEyeList.get(i).m_UserName,rightEyeList.get(i).m_Uid, 1), EnumEyeType.RIGHT,
					rightEyeList.get(i).m_RightTemplate);
		}

		mIrisPresenter.setIrisData(irisLeftData, irisRightData, null);//需要把特征传入jar包，以便识别
	}
	
	@Override
	protected void onStop() {
		isStop = true;
		if(mSurfaceHandler != null){
			mSurfaceHandler.removeCallbacksAndMessages(null);
		}
		resetUI();
		mIrisPresenter.pause();
		super.onStop();
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	private int[] textureBuffer;
	private Bitmap gBitmap;
	private byte[] bmpData;
	private int bmpWidth;
	private int bmpHeight;
	
	private void drawImage() {
		if(bmpWidth == 0 || bmpHeight == 0) return;
		if(textureBuffer == null){
			textureBuffer = new int[bmpWidth * bmpHeight];
		}
		if(gBitmap == null){
			gBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.RGB_565);//8 位 RGB位图,没有透明度
		}

		ImageUtil.getBitmap8888(bmpData, bmpHeight, bmpWidth, 0, 0, bmpWidth-1, bmpHeight-1, textureBuffer, 0, 1);
		
		gBitmap.setPixels(textureBuffer, 0, bmpWidth, 0, 0, bmpWidth, bmpHeight);
		
		Canvas canvas = holder.lockCanvas();
		if(canvas != null){
			if(EnumDeviceType.isSpecificDevice(EnumDeviceType.LONGKE) || EnumDeviceType.isSpecificDevice(EnumDeviceType.YLT_BM5300)
					|| EnumDeviceType.isSpecificDevice(EnumDeviceType.HCTX_LS_5512)){
				canvas.scale(1, 1, eyeViewWidth / 2.0f, eyeViewHeight / 2.0f);
			}else{
				canvas.scale(-1, 1, eyeViewWidth / 2.0f, eyeViewHeight / 2.0f);
			}
			canvas.drawBitmap(gBitmap, matrix, null);
			holder.unlockCanvasAndPost(canvas);
		}
	}
	/**
	 * 屏幕UI调整
	 */
	private void screenUiAdjust() {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		int screenWidth = metrics.widthPixels; // 获取屏幕的宽
		int transWid = 0;

		Configuration mConfiguration = this.getResources().getConfiguration();
		
		int ori = mConfiguration.orientation;// 获取屏幕方向
		if (ori == Configuration.ORIENTATION_LANDSCAPE) {	// 如果是横屏，预览区域的宽为当前屏幕宽的80%，根据设备的不同可以动态再进行适配
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			hor_scale = 0.6f;
			eyeViewWidth = (int) (screenWidth * hor_scale);
			transWid = (screenWidth - eyeViewWidth) / 2;
			
		} else if (ori == Configuration.ORIENTATION_PORTRAIT) {	// 竖屏
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			hor_scale = 1.0f;
			eyeViewWidth = screenWidth;// 如果是竖屏，预览区域的宽为屏幕的宽
			transWid = 0;
		}
		
		// 由于图像是16:9的图像
		eyeViewHeight = (int) (eyeViewWidth / 1.777f);
		// 在480*270的分辨率下，双眼相对于左上角的坐标点为（140,110），（340,110） ps：固定坐标点，修改需要咨询虹霸开发人员
		float x = (float) eyeViewWidth / IKALGConstant.IK_DISPLAY_IMG_WIDTH;
		float y = (float) eyeViewHeight / IKALGConstant.IK_DISPLAY_IMG_HEIGHT;
		DecimalFormat df = new DecimalFormat("0.00");
		eyeX1 = Float.parseFloat(df.format(x)) * EnumDeviceType.getCurrentDevice().getDefaultLeftIrisCol()+ transWid;
		eyeX2 = Float.parseFloat(df.format(x)) * EnumDeviceType.getCurrentDevice().getDefaultRightIrisCol()+ transWid;
		eyeHeight = Float.parseFloat(df.format(y)) * EnumDeviceType.getCurrentDevice().getDefaultLeftIrisRow();
	}

	private static class SurfaceHandler extends Handler {
        private WeakReference<MainActivity> handlerReference;

        public SurfaceHandler(MainActivity activity) {
            handlerReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
        	MainActivity activity = handlerReference == null ? null : handlerReference.get();
            if (activity.isStop) {
                return;
            }
            if (activity == null || activity.isFinishing()) {
                return;
            }
            if (msg.what == HANDLER_DRAW_IMAGE) {
                activity.drawImage();
            }
            if (msg.what == HANDLER_UPDATE_TEXT) {
            	activity.mResultTextViewEnrRecFinal.setText(msg.obj.toString());
            }
        	if (msg.what == HANDLER_RESET_UI) {
        		activity.resetUI();
        	}
        	if( msg.what == HANDLER_RESET_PROGRESS){
        		activity.screenUiAdjust();
        		activity.progressBar.setXAndY(activity.eyeX1, activity.eyeX2, activity.eyeHeight);// 设置双眼progressbar的位置
        		activity.progressBar.invalidate();
        	}
        	if(msg.what == HANDLER_SHOW_LEFT){
        		Bitmap leftBm = (Bitmap) msg.obj;
        		activity.leftView.setImageBitmap(leftBm);
        	}
        	if(msg.what == HANDLER_SHOW_RIGHT){
        		Bitmap rightBm = (Bitmap) msg.obj;
        		activity.rightView.setImageBitmap(rightBm);
        	}
        	
        }
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.rgb(0, 0, 0));
            holder.unlockCanvasAndPost(canvas);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };
	protected int distRange = 0;

	// Init view
	private void initUI() {
		svCamera = (SurfaceView) findViewById(R.id.iv_camera);
		LayoutParams svParams = svCamera.getLayoutParams();
		svParams.width = eyeViewWidth;
		svParams.height = eyeViewHeight;
		svCamera.setLayoutParams(svParams);
		
		mEyeView = (EyeView) findViewById(R.id.eye);
		mRgEye = (RadioGroup) findViewById(R.id.rg_eyeGroup);
		mRgEye.setOnCheckedChangeListener(this);
		
		holder = svCamera.getHolder();
		holder.addCallback(surfaceCallback);
		matrix = new Matrix();

		progressBar = (RoundProgressBar) findViewById(R.id.roundProgress);
		progressBar.setXAndY(eyeX1, eyeX2, eyeHeight);// 设置双眼progressbar的位置
		progressBar.setHorScale(hor_scale);
		// Init button
		mIrisRegisterBtn = (Button) findViewById(R.id.btn_register);
		mIrisRegisterBtn.setOnClickListener(this);
		mIrisIdenBtn = (Button) findViewById(R.id.btn_scan);
		mIrisIdenBtn.setOnClickListener(this);
		mIrisCaptureBtn = (Button) findViewById(R.id.btn_capture);
		mIrisCaptureBtn.setOnClickListener(this);

		//add by yumingyuan，初始化PIN认证按钮和密码输入框
		mPinIdenBtn= (Button) findViewById(R.id.btn_pin);
		mPinIdenBtn.setOnClickListener(this);
		mPinedit= (EditText) findViewById(R.id.et_pinpass);
		//add by yumingyuan

		mResultTextViewEnrRecFinal = (TextView) findViewById(R.id.ie_final_result);
		mUserNameEditText = (EditText) findViewById(R.id.et_userName);
		leftView = (ImageView) findViewById(R.id.iv_left);
		rightView = (ImageView) findViewById(R.id.iv_right);
		
		previewParaUpdated = false;
		
		if(Config.DEVICE_SINGLE_EYE){
			progressBar.setVisibility(View.GONE);
			mRgEye.setVisibility(View.GONE);
			mEyeView.setVisibility(View.VISIBLE);
			irisMode = IKALGConstant.IR_IM_EYE_LEFT;
		}
	}
	
	private boolean isActive = false;
	Runnable cliRunnable = new Runnable() {
		
		@Override
		public void run() {
			mIrisRegisterBtn.performClick();
		}
	};
	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		case R.id.btn_register: // 虹膜注册
			
			if(isActive){
				resetUI();
			}else{
				curName = mUserNameEditText.getText().toString();
				if(TextUtils.isEmpty(curName)){
					Toast.makeText(MainActivity.this, R.string.tip_inputusername, Toast.LENGTH_SHORT).show();
					return;
				}
				isActive = true;
				mIrisIdenBtn.setEnabled(false);
				mIrisCaptureBtn.setEnabled(false);
				mIrisRegisterBtn.setText(R.string.stop_register);
				svCamera.setKeepScreenOn(true);
				Preferences.getInstance(getApplicationContext()).setRegisterName(curName);
				
				mEnrollConfig.irisMode = irisMode;
				if(Config.ifSTQC || EnumDeviceType.getCurrentDevice().getDeviceType() == IKALGConstant.IKMSTQC_SingleEye
						|| EnumDeviceType.getCurrentDevice().getDeviceType() == IKALGConstant.IKMSTQC_Dual){
					mEnrollConfig.irisNeedCount = 3;
				}else{
					mEnrollConfig.irisNeedCount = 9;
				}
				mEnrollConfig.overTime = 30;
				mEnrollConfig.singleUse = false;
				mIrisPresenter.startEnroll(mEnrollConfig, processCallback);
				leftView.setImageBitmap(null);
				rightView.setImageBitmap(null);
			}
			//mainHandler.postDelayed(cliRunnable, 5000);
			break;
		case R.id.btn_capture:
			if(isActive){
				resetUI();
			}else{
				curName = mUserNameEditText.getText().toString();
				if(TextUtils.isEmpty(curName)){
					Toast.makeText(MainActivity.this, R.string.tip_inputusername, Toast.LENGTH_SHORT).show();
					return;
				}
				isActive = true;
				mIrisIdenBtn.setEnabled(false);
				mIrisRegisterBtn.setEnabled(false);
				mIrisCaptureBtn.setText(R.string.stop_capture);
				svCamera.setKeepScreenOn(true);
				Preferences.getInstance(getApplicationContext()).setRegisterName(curName);
				
				mCaptureConfig.irisMode = irisMode;
				mCaptureConfig.irisNeedCount = 3;
				mCaptureConfig.overTime = 30;
				mIrisPresenter.startCapture(mCaptureConfig, captureCallback);
			}
			break;
		// 单独虹膜识别
		case R.id.btn_scan:
			if(isActive){
				resetUI();
			}else{
				isActive = true;
				mIrisIdenBtn.setText(R.string.stop_identify);
				mIrisCaptureBtn.setEnabled(false);
				mIrisRegisterBtn.setEnabled(false);
				svCamera.setKeepScreenOn(true);
				
				mIdentifyConfig.irisMode = IKALGConstant.IR_IM_EYE_UNDEF;
				mIdentifyConfig.overTime = 30;
//				mIdentifyConfig.reserve |= IKALGConstant.RESERVE_INFO_I_CONSTANT_LIGHT;		// 保持红外灯常亮
//				mIdentifyConfig.reserve |= IKALGConstant.RESERVE_INFO_I_CONSTANT_PREVIEW;	// 注册、识别结束 不执行Camera的stopPreview()方法
				mIrisPresenter.startIdentify(mIdentifyConfig, processCallback);
			}
			break;
		//add by yumingyuan认证用
		case R.id.btn_pin:
			if(mPinedit.getText().length()==0)//edittext中未输入pin码的处理情况
			{
				Toast.makeText(this,"请输入PIN码以完成解锁",Toast.LENGTH_LONG).show();
			}
			else//输入PIN码，进行合法性检查
			{
			    String S_pin=mPinedit.getText().toString();
				String passhash = null;
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] result=digest.digest(S_pin.getBytes());
                    String properties_file_path = "/data/data/userconfig.properties";
					File file = new File(properties_file_path);
					FileInputStream fin= new FileInputStream(file);
					//FileInputStream fin = openFileInput(properties_file_path);
					byte [] buffer = new byte[300];
					InputStreamReader inputreader = new InputStreamReader(fin);
					BufferedReader buffreader = new BufferedReader(inputreader);
					String line;
					//分行读取
					while (( line = buffreader.readLine()) != null) {
						if(line.contains("userpass"))
						{
							passhash=line.substring(line.indexOf("=")+2);
						}
					}
                   if(convertHashToString(result).equals(passhash.trim()))
					{
						System.exit(0);
					}
					else
					{
						Toast.makeText(this,"PIN码错误",Toast.LENGTH_LONG).show();
					}
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
			break;
			//add by yumingyuan end
		}


	}
	
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch(checkedId){
		case R.id.rb_bothEye:
			irisMode = IKALGConstant.IR_IM_EYE_BOTH;
			break;
		case R.id.rb_leftEye:
			irisMode = IKALGConstant.IR_IM_EYE_LEFT;
			break;
		case R.id.rb_rightEye:
			irisMode = IKALGConstant.IR_IM_EYE_RIGHT;
			break;
		case R.id.rb_undef:
			irisMode = IKALGConstant.IR_IM_EYE_UNDEF;
			break;
		}
	}
	
	public void resetUI(){
		isActive = false;
		maxLeft = 0;
		maxRight = 0;
		
		mResultTextViewEnrRecFinal.setText(" ");

		svCamera.setKeepScreenOn(false);
		
		mIrisIdenBtn.setText(R.string.start_identify);
		mIrisRegisterBtn.setEnabled(true);
		
		mIrisRegisterBtn.setText(R.string.start_register);
		mIrisIdenBtn.setEnabled(true);
		
		mIrisCaptureBtn.setText(R.string.start_capture);
		mIrisCaptureBtn.setEnabled(true);
		
		mIrisPresenter.stopAlgo();
		progressBar.setLeftAndRightProgress(0, 0, 0);
		
		mEyeView.eyeDetectView.reset();
		mEyeView.eyeDetectView.setProgress(0, 0, 0, 0, false,false);
		mEyeView.postInvalidate();
	}
	
	private void updateUIStatus(int status){
		
		String tips = "";
		int m_curDist = IKEnrIdenStatus.getInstance().irisPos.dist;
		
		switch (status) {
		case IKALGConstant.IRIS_FRAME_STATUS_BLINK:
			tips = getString(R.string.tip_blink_eyes);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_MOTION_BLUR:
		case IKALGConstant.IRIS_FRAME_STATUS_FOCUS_BLUR:
			tips = getString(R.string.tip_keep_stable);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_BAD_EYE_OPENNESS:
			tips = getString(R.string.tip_open_eye);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_WITH_GLASS:
			tips = getString(R.string.tip_remove_glasses);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_WITH_GLASS_HEADUP:
			tips = getString(R.string.tip_raise_head);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_WITH_GLASS_HEADDOWN:
			tips = getString(R.string.tip_lower_head);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_CLOSE:
			int suit = EnumDeviceType.getCurrentDevice().getSuitablePosDist();
			int movedist = Math.abs(m_curDist - suit);	
			if (m_curDist != -1) {
				tips = String.format(getString(R.string.tip_move_father_dist), movedist);
			} else {
				tips = getString(R.string.tip_move_father);
			}
			soundPlay(fartherId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_FAR:
			suit = EnumDeviceType.getCurrentDevice().getSuitablePosDist();
			movedist = Math.abs(m_curDist - suit);
			if (m_curDist != -1) {
				tips = String.format(getString(R.string.tip_move_closer_dist), movedist); 
			} else{
				tips = getString(R.string.tip_move_closer);
			}
			soundPlay(closerId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_NOT_FOUND:
			tips = getString(R.string.tip_noeyedetected);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_UNAUTHORIZED_ATTACK:
			tips = getString(R.string.tip_unauthorized_attack);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_CONTACTLENS:
			tips = getString(R.string.tip_remove_contact_lenses);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_ATTACK:
			tips = getString(R.string.tip_do_not_attack);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_OUTDOOR:
			tips = getString(R.string.tip_use_indoors);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_UP:
			tips = getString(R.string.tip_bad_image);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_DOWN:
			tips = getString(R.string.tip_bad_image);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_LEFT:
			tips = getString(R.string.tip_bad_image);
			soundPlay(moveRightId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_EYE_TOO_RIGHT:
			tips = getString(R.string.tip_bad_image);
			soundPlay(moveLeftId);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_SUITABLE:
			tips = getString(R.string.tip_scanning);
			break;
		case IKALGConstant.IRIS_FRAME_STATUS_BAD_IMAGE_QUALITY:
			tips = getString(R.string.tip_bad_image);
			break;
		case IKALGConstant.ERR_INVALIDDATE:
			tips = getString(R.string.tip_invaliddate);
			break;
		case IKALGConstant.ERR_INVALIDDEVICE:
			tips = getString(R.string.tip_invaliddevice);
			break;
		default:
			break;
		}
		Message msg = Message.obtain();
		msg.obj = tips;
		msg.what = HANDLER_UPDATE_TEXT;
		mSurfaceHandler.sendMessage(msg);
	}
	
	private IrisCaptureCallback captureCallback = new IrisCaptureCallback() {
		
		@Override
		public void onUIStatusUpdate(int status){
			updateUIStatus(status);
		}
		
		@Override
		public void onCaptureProgress(int currentLeftCount, int currentRightCount, int needCount) {
			
			maxLeft = maxLeft > currentLeftCount ? maxLeft : currentLeftCount;
			maxRight = maxRight > currentRightCount ? maxRight : currentRightCount;
			
			progressBar.setLeftAndRightProgress(currentLeftCount, currentRightCount, needCount);
			
			mEyeView.eyeDetectView.setProgress(
					currentLeftCount, needCount, currentRightCount, needCount, 
					currentLeftCount >= needCount, currentRightCount >= needCount);
		}

		@Override
		public void onCaptureComplete(int ifSuccess,EnrFeatrueStruct leftEyeFeat, EnrFeatrueStruct rightEyeFeat,EnrFeatrueStruct faceFeat) {
			if(mSurfaceHandler!= null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			// 首先判断是否成功，若失败提示后返回
			if (ifSuccess != IKALGConstant.ALGSUCCESS) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

				if (ifSuccess == IKALGConstant.ERR_OVERTIME) {
					builder.setMessage(R.string.dialog_timeout);
				} else if (ifSuccess == IKALGConstant.ERR_ENROLL_ERRORFEATURE) {
					builder.setMessage(R.string.dialog_registration_failed);
				} else{
					builder.setMessage("ErrorCode:" + ifSuccess);
				}

				builder.setTitle(R.string.dialog_title_notice);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.dialog_button_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
				builder.create().show();

				return;
			}
			soundPool.play(enrosuccId, 1, 1, 1, 0, 1);

			if(Config.DEVICE_SINGLE_EYE){
//				saveIrisFile(curName, leftEyeFeat);
				saveIrisImage(curName, leftEyeFeat);
			}else{
//				saveIrisFile(curName, leftEyeFeat, rightEyeFeat);
				saveIrisImage(curName, leftEyeFeat, rightEyeFeat);
			}
		}

		@Override
		public void onEyeDetected(boolean isValid,EyePosition leftPos,EyePosition rightPos,int captureDistance) {
			mEyeView.eyeDetectView.init(
				EnumDeviceType.getCurrentDevice().getRotateAngle(), 
				EnumDeviceType.getCurrentDevice().getPreviewWidth(), 
				EnumDeviceType.getCurrentDevice().getPreviewHeight(), 
				eyeViewWidth, eyeViewHeight);

			mEyeView.eyeDetectView.setDetectResult(leftPos, rightPos, distRange);
			mEyeView.invalidate();	// 画人员定位圆
		}

		@Override
		public void onAlgoExit() {
			if(mSurfaceHandler != null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
		}
	};
	
	public void saveIrisImage(String name, EnrFeatrueStruct singleEyeFeat) {
		if (singleEyeFeat != null) {
			for (int i = 0; i < singleEyeFeat.enrCount; i++) {
				String filePath = Environment.getExternalStorageDirectory() + "/IK_FaceDemo/StreamCap/" + name;
				FileUtil.saveMonoBMPImage(filePath, singleEyeFeat.irisInfo[i].imgData, IKALGConstant.IKALG_Iris_ImH, IKALGConstant.IKALG_Iris_ImW, "" + irisMode);	
			}
		}
	}
	
	public void saveIrisImage(String name, EnrFeatrueStruct leftEyeFeat, EnrFeatrueStruct rightEyeFeat) {
		if (leftEyeFeat != null) {
			for (int i = 0; i < leftEyeFeat.enrCount; i++) {
				String filePath = Environment.getExternalStorageDirectory() + "/IK_FaceDemo/StreamCap/" + name;
				FileUtil.saveMonoBMPImage(filePath, leftEyeFeat.irisInfo[i].imgData, IKALGConstant.IKALG_Iris_ImH, IKALGConstant.IKALG_Iris_ImW, "L");	
			}
		}
		if (rightEyeFeat != null) {
			for (int i = 0; i < rightEyeFeat.enrCount; i++) {
				String filePath = Environment.getExternalStorageDirectory() + "/IK_FaceDemo/StreamCap/" + name;
				FileUtil.saveMonoBMPImage(filePath, rightEyeFeat.irisInfo[i].imgData, IKALGConstant.IKALG_Iris_ImH, IKALGConstant.IKALG_Iris_ImW, "R");	
			}
		}
	}
	
	private int maxLeft = 0;
	private int maxRight = 0;
	private IrisProcessCallback processCallback = new IrisProcessCallback() {
		@Override
		public void onUIStatusUpdate(int status){
			updateUIStatus(status);
		}
		
		@Override
		public void onEnrollProgress(int currentLeftCount, int currentRightCount, int needCount) {
			
			maxLeft = maxLeft > currentLeftCount ? maxLeft : currentLeftCount;
			maxRight = maxRight > currentRightCount ? maxRight : currentRightCount;

			progressBar.setLeftAndRightProgress(maxLeft, maxRight, needCount);
			
			mEyeView.eyeDetectView.setProgress(
					currentLeftCount, needCount, currentRightCount, needCount, 
					currentLeftCount >= needCount, currentRightCount >= needCount);
		}

		@Override
		public void onEnrollComplete(int ifSuccess,EnrFeatrueStruct leftEyeFeat, EnrFeatrueStruct rightEyeFeat,EnrFeatrueStruct faceFeat) {
			if(mSurfaceHandler != null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			// 首先判断是否成功，若失败提示后返回
			if (ifSuccess != IKALGConstant.ALGSUCCESS) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

				if (ifSuccess == IKALGConstant.ERR_OVERTIME) {
					builder.setMessage(R.string.dialog_timeout);
				} else if (ifSuccess == IKALGConstant.ERR_ENROLL_ERRORFEATURE) {
					builder.setMessage(R.string.dialog_registration_failed);
				} else{
					builder.setMessage("ErrorCode:" + ifSuccess);
				}

				builder.setTitle(R.string.dialog_title_notice);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.dialog_button_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
				builder.create().show();

				return;
			}
			if(Config.ifSTQC || EnumDeviceType.getCurrentDevice().getDeviceType() == IKALGConstant.IKMSTQC_SingleEye
					|| EnumDeviceType.getCurrentDevice().getDeviceType() == IKALGConstant.IKMSTQC_Dual){//only the first one contains iso format data.
				MainActivity.this.leftECEyeFeat = leftEyeFeat;
				MainActivity.this.rightECEyeFeat = rightEyeFeat;
				ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(handleISODataRunnable);
			}
			soundPool.play(enrosuccId, 1, 1, 1, 0, 1);
			
			if(Config.DEVICE_SINGLE_EYE){
				saveIrisFile(curName, leftEyeFeat);
			}else{
				saveIrisFile(curName, leftEyeFeat, rightEyeFeat);
			}
		}

		@Override
		public void onEyeDetected(boolean isValid, EyePosition leftPos, EyePosition rightPos, int captureDistance) {
			mEyeView.eyeDetectView.init(
					EnumDeviceType.getCurrentDevice().getRotateAngle(), 
					EnumDeviceType.getCurrentDevice().getPreviewWidth(), 
					EnumDeviceType.getCurrentDevice().getPreviewHeight(), 
					eyeViewWidth, eyeViewHeight);

				mEyeView.eyeDetectView.setDetectResult(leftPos, rightPos, distRange);
				mEyeView.invalidate();	// 画人员定位圆
		}

		@Override
		public void onIdentifyComplete(int ifSuccess, int matchIndex, int eyeFlag) {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			if (ifSuccess != IKALGConstant.ALGSUCCESS) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

				if (ifSuccess == IKALGConstant.ERR_IDENFAILED) {
					builder.setMessage(R.string.dialog_identification_failed);
				} else if (ifSuccess == IKALGConstant.ERR_OVERTIME) {
					builder.setMessage(R.string.dialog_timeout);
				} else if (ifSuccess == IKALGConstant.ERR_NOFEATURE) {
					builder.setMessage(R.string.dialog_no_feature);
				} else if (ifSuccess == IKALGConstant.ERR_EXCEEDMAXMATCHCAPACITY) {
					builder.setMessage(R.string.dialog_overmuch_feature);
				} else if (ifSuccess == IKALGConstant.ERR_IDEN) {
					builder.setMessage(R.string.dialog_identification_failed);
				} else{
					builder.setMessage("error code:" + ifSuccess);
				}

				builder.setTitle(R.string.dialog_title_notice);
				builder.setCancelable(false);
				builder.setPositiveButton(R.string.dialog_button_ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						});
				builder.create().show();

				return;
			}
			soundPool.play(idensuccId, 1, 1, 1, 0, 1);
			
			String matchName = "";
			if(eyeFlag == EnumEyeType.LEFT){
				matchName = irisLeftData.personAt(matchIndex).getName();
			}else{
				matchName = irisRightData.personAt(matchIndex).getName();
			}
			Toast.makeText(getApplicationContext(),"identification success, matchIndex: " + matchIndex + ", eyeFlag:" + eyeFlag + ", name:" + matchName, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onAlgoExit() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
		}
	};
	
	Runnable handleISODataRunnable = new Runnable() {
		
		@Override
		public void run() {
			IrisInfo leftFirstIris = leftECEyeFeat.irisInfo[0];
			IrisInfo rightFirstIris = rightECEyeFeat.irisInfo[0];
			
			String stqcPath = Environment.getExternalStorageDirectory() + "/PidData/";
//			if(leftFirstIris != null && leftFirstIris.imgData != null){
//				FileUtil.saveMonoBMPImage(stqcPath,leftFirstIris.imgData,480, 640, "LimageData_PP");
//			}
//			
//			if(rightFirstIris != null && rightFirstIris.imgData != null){
//				FileUtil.saveMonoBMPImage(stqcPath,rightFirstIris.imgData,480, 640, "RimageData_PP");	
//			}
//			
//			if(leftFirstIris != null && leftFirstIris.localImg != null)
//				FileUtil.saveMonoBMPImage(stqcPath,leftFirstIris.localImg,480, 640, "LlocalImg_PP");
//			
//			if(rightFirstIris != null && rightFirstIris.localImg != null)
//				FileUtil.saveMonoBMPImage(stqcPath,rightFirstIris.localImg,480, 640, "RlocalImg_PP");	
			
			AadharIrisISOFormat7 irisIsoFormat7 = new AadharIrisISOFormat7();
			if(leftFirstIris != null){
				byte[] isoIrisData = ImageUtil.compress(leftFirstIris.isoIrisData, leftFirstIris.isoWidth, leftFirstIris.isoHeight);
				byte[] isoImgLeft = irisIsoFormat7.getISOFormatHeader(
						isoIrisData, (short) 0,
						(short) leftFirstIris.isoHeight,
						(short) leftFirstIris.isoWidth,
						isoIrisData.length);
//				FileUtil.saveData(isoIrisData, stqcPath , "leftISO.j2k");	
//				FileUtil.saveData(isoImgLeft, stqcPath, "iso_left.iso");
				
				byte[] deCompress = ImageUtil.deCompress(isoIrisData,ImageUtil.compressRate);
				byte[] convertToBitmapArray = FileUtil.convertToBitmapArray(deCompress, leftFirstIris.isoHeight, leftFirstIris.isoWidth);
				Bitmap leftBm = BitmapFactory.decodeByteArray(convertToBitmapArray, 0, convertToBitmapArray.length);
				if(!isStop && mSurfaceHandler != null){
					Message msg = Message.obtain();
					msg.obj = leftBm;
					msg.what = HANDLER_SHOW_LEFT;
					mSurfaceHandler.sendMessage(msg);
				}
			}
			if(rightFirstIris != null){
				byte[] isoIrisData = ImageUtil.compress(rightFirstIris.isoIrisData, rightFirstIris.isoWidth, rightFirstIris.isoHeight);
				byte[] isoImgRight = irisIsoFormat7.getISOFormatHeader(
						isoIrisData, (short) 0,
						(short) rightFirstIris.isoHeight,
						(short) rightFirstIris.isoWidth,
						isoIrisData.length);
//				FileUtil.saveData(isoIrisData, stqcPath , "rightISO.j2k");	
//				FileUtil.saveData(isoImgRight, stqcPath, "iso_right.iso");
				
				byte[] deCompress = ImageUtil.deCompress(isoIrisData,ImageUtil.compressRate);
				byte[] convertToBitmapArray = FileUtil.convertToBitmapArray(deCompress, rightFirstIris.isoHeight, rightFirstIris.isoWidth);
				Bitmap rightBm = BitmapFactory.decodeByteArray(convertToBitmapArray, 0, convertToBitmapArray.length);
				if(!isStop && mSurfaceHandler != null){
					Message msg = Message.obtain();
					msg.obj = rightBm;
					msg.what = HANDLER_SHOW_RIGHT;
					mSurfaceHandler.sendMessage(msg);
				}
			}
		}
	};
	
	private CameraPreviewCallback irPreviewCallback = new CameraPreviewCallback.IRPreviewCallback(){
		@Override
		public void onPreviewFrame(byte[] bmpData, int bmpWidth, int bmpHeight) {
			
			uvcTimeArray.newTime();
			if (uvcTimeArray.count() % 3 == 0) {
				Log.e("iris_verbose","MainActivity onPreviewFrame fps:" + uvcTimeArray.toString() + ", isStop:" + isStop);
			}
			
			MainActivity.this.bmpWidth = bmpWidth;
			MainActivity.this.bmpHeight = bmpHeight;
			MainActivity.this.bmpData = bmpData;
			
			if(previewParaUpdated == false){
				previewParaUpdated = true;
    			
				if(matrix != null){
					matrix.postScale(1.0f*eyeViewWidth/bmpWidth, 1.0f*eyeViewHeight/bmpHeight);
				}
			}
			
			if (!isStop && mSurfaceHandler != null ) {
                mSurfaceHandler.sendEmptyMessage(HANDLER_DRAW_IMAGE);
            }
		}
	};
	
	private CameraPreviewCallback uvcPreviewCallback = new CameraPreviewCallback.UVCPreviewCallback(){
		@Override
		public void onPreviewFrame(byte[] bmpData, int bmpWidth, int bmpHeight) {
			
			uvcTimeArray.newTime();
			if (uvcTimeArray.count() % 3 == 0) {
				Log.e("iris_verbose","MainActivity onPreviewFrame fps:" + uvcTimeArray.toString());
			}
			
			MainActivity.this.bmpWidth = bmpWidth;
			MainActivity.this.bmpHeight = bmpHeight;
			MainActivity.this.bmpData = bmpData;
			
			if(previewParaUpdated == false){
				previewParaUpdated = true;
				if(matrix != null){
					matrix.postScale(1.0f*eyeViewWidth/bmpWidth, 1.0f*eyeViewHeight/bmpHeight);
				}
			}
			
			if (!isStop && mSurfaceHandler != null ) {
                mSurfaceHandler.sendEmptyMessage(HANDLER_DRAW_IMAGE);
            }
		}

		@Override
		public void onCameraConnected() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_PROGRESS);
			}
		}

		@Override
		public void onCameraDisconnected() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			Toast.makeText(MainActivity.this, R.string.dialog_usb_disconnected, Toast.LENGTH_SHORT).show();
		}
		@Override
		public void onCameraDettached() {
			if(mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
			Toast.makeText(MainActivity.this, R.string.dialog_usb_disconnected, Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onDeviceFlip(boolean isFlip) {
			if(isFlip && mSurfaceHandler!=null){
				mSurfaceHandler.sendEmptyMessage(HANDLER_RESET_UI);
			}
		}
	};

	public void onBackPressed() {
		resetUI();
		super.onBackPressed();
	};
	
	@Override
	protected void onDestroy() {
		if(mSurfaceHandler != null){
			mSurfaceHandler.removeCallbacksAndMessages(null);
			mSurfaceHandler = null;
		}
		ThreadPoolProxyFactory.getNormalThreadPoolProxy().remove(handleISODataRunnable);
		
		if(soundPool != null){
            soundPool.autoPause();
            soundPool.release();
            soundPool = null;
        }
		if(mIrisPresenter!=null){
			mIrisPresenter.release();
			mIrisPresenter = null;
		}
		super.onDestroy();
	}

	public void saveIrisFile(String name, EnrFeatrueStruct singleEyeFeat) {
		//如果是单人使用
		deleteIrisData();
		int userCount = sqliteDataBase.getUserCount();

		for(int i=userCount; i>=IrisConfig.LimitNumber; i--){
			sqliteDataBase.removeFirstUser();
		}

		IrisUserInfo userInfo = new IrisUserInfo();

		if (singleEyeFeat != null) {
			userInfo.m_Uid = name;
			userInfo.m_UserName = name;
			userInfo.m_UserFavicon = 0;
			
			if(irisMode == IKALGConstant.IR_IM_EYE_LEFT){
				userInfo.m_LeftTemplate_Count = singleEyeFeat.enrCount;
				userInfo.m_LeftTemplate = new byte[singleEyeFeat.enrCount * IKALGConstant.IKALG_Iris_Enr_CodeLen];
				for(int i=0; i<singleEyeFeat.enrCount; i++){
					System.arraycopy(
							singleEyeFeat.irisInfo[i].irisEnrTemplate, 0, 
							userInfo.m_LeftTemplate, i*IKALGConstant.IKALG_Iris_Enr_CodeLen, 
							IKALGConstant.IKALG_Iris_Enr_CodeLen);
				}
			}else{
				userInfo.m_RightTemplate_Count = singleEyeFeat.enrCount;
				userInfo.m_RightTemplate = new byte[singleEyeFeat.enrCount * IKALGConstant.IKALG_Iris_Enr_CodeLen];
				for(int i=0; i<singleEyeFeat.enrCount; i++){
					System.arraycopy(
							singleEyeFeat.irisInfo[i].irisEnrTemplate, 0, 
							userInfo.m_RightTemplate, i*IKALGConstant.IKALG_Iris_Enr_CodeLen, 
							IKALGConstant.IKALG_Iris_Enr_CodeLen);
				}
			}
			
			userInfo.m_EnrollTime = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
			sqliteDataBase.insertUserData(userInfo);
		}
		//重新查询数据库数据
		initIrisData();
	}
	public static String getUUID(){ 
		String uuid = UUID.randomUUID().toString(); 
		//去掉“-”符号 
		return uuid.replaceAll("-", "");
	}
	@SuppressLint("SimpleDateFormat")
	public void saveIrisFile(String name, EnrFeatrueStruct leftEyeFeat,EnrFeatrueStruct rightEyeFeat) {
		//如果是单人使用
		deleteIrisData();
		int userCount = sqliteDataBase.getUserCount();
		for(int i=userCount; i>=IrisConfig.LimitNumber; i--){
			sqliteDataBase.removeFirstUser();
		}

		IrisUserInfo userInfo = new IrisUserInfo();

		if (leftEyeFeat != null && rightEyeFeat != null) {
			userInfo.m_Uid = getUUID();
			userInfo.m_UserName = name;
			userInfo.m_UserFavicon = 0;
			
			userInfo.m_LeftTemplate = new byte[leftEyeFeat.enrCount * IKALGConstant.IKALG_Iris_Enr_CodeLen];
			for(int i=0; i<leftEyeFeat.enrCount; i++){
				System.arraycopy(
						leftEyeFeat.irisInfo[i].irisEnrTemplate, 0, 
						userInfo.m_LeftTemplate, i*IKALGConstant.IKALG_Iris_Enr_CodeLen, 
						IKALGConstant.IKALG_Iris_Enr_CodeLen);
			}
			
			userInfo.m_RightTemplate = new byte[rightEyeFeat.enrCount * IKALGConstant.IKALG_Iris_Enr_CodeLen];
			for(int i=0; i<rightEyeFeat.enrCount; i++){
				System.arraycopy(
						rightEyeFeat.irisInfo[i].irisEnrTemplate, 0, 
						userInfo.m_RightTemplate, i*IKALGConstant.IKALG_Iris_Enr_CodeLen, 
						IKALGConstant.IKALG_Iris_Enr_CodeLen);
			}
			userInfo.m_LeftTemplate_Count = leftEyeFeat.enrCount;
			userInfo.m_RightTemplate_Count = rightEyeFeat.enrCount;
			userInfo.m_EnrollTime = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
			sqliteDataBase.insertUserData(userInfo);
		}
		//重新查询数据库数据
		initIrisData();
	}

	public void deleteIrisData() {
		if(mEnrollConfig.singleUse) {
			sqliteDataBase.removeAll();
		}
	}
	private void soundPlay(int soundId) {
        frameIndex++;
        if ((frameIndex % 60 == 0) && (soundPool != null)) {
            soundPool.play(soundId, 1, 1, 1, 0, 1);
            frameIndex = 0;
        }
    }
    //add by yumingyuan 20190118重写onkeydown方法，捕捉keycode，返回false则屏蔽按键
    public boolean onKeyDown( int keyCode, KeyEvent event) {
	    //System.out.println(keyCode);
        if (keyCode == 4) {
            return false;
        }
        if(keyCode==5)
		{
			System.out.println(event.getKeyCode());
			return false;
		}
        return super.onKeyDown(keyCode, event);
    }
    //add by yumingyuan 20190118重写onkeydown方法结束
    //add by yumingyuan 20190117将byte[]转换为16进制字符串
    private static String convertHashToString(byte[] hashBytes) {
        String returnVal = "";
        for (int i = 0; i < hashBytes.length; i++) {
			//System.out.println(hashBytes[i]);
			//System.out.println((hashBytes[i] & 0xff));
        	//System.out.println((hashBytes[i] & 0xff) + 0x100);
        	//System.out.println(Integer.toString(( hashBytes[i] & 0xff) + 0x100, 16));
			//System.out.println(Integer.toString(( hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            returnVal += Integer.toString(( hashBytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }
}
