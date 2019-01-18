package buaa.irisking.scanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RoundProgressBar extends ImageView {

	/**
	 * 画笔对象的引用
	 */
	private Paint paint;
	
	RectF left_oval;  //用于定义的圆弧的形状和大小的界限
	RectF right_oval;  //用于定义的圆弧的形状和大小的界限

	/**
	 * 圆环的颜色
	 */
	private int roundColor;

	/**
	 * 圆环进度的颜色
	 */
	private int roundProgressColor;

	/**
	 * 中间进度百分比的字符串的颜色
	 */
	private int textColor;

	/**
	 * 中间进度百分比的字符串的字体
	 */
	private float textSize;

	/**
	 * 圆环的宽度
	 */
	private float roundWidth;

	/**
	 * 最大进度
	 */
	private int max;

	/**
	 * 当前进度
	 */
	private int left_progress, right_progress;
	
//	private Bitmap mBackgroundBitmap;
	private int mBGWidth;
	private int mBGHeight;


	private float x1 = 0, x2 = 0, y = 0, radius;
	private float horScale = 1.0f;
	private boolean ifHorScale = false;
	
//	private boolean textIsDisplayable;

	public RoundProgressBar(Context context) {
		this(context, null);
	}

	public RoundProgressBar(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RoundProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		paint = new Paint();
		left_oval = new RectF();
		right_oval = new RectF();

		TypedArray mTypedArray = context.obtainStyledAttributes(attrs, buaa.irisking.irisapp.R.styleable.RoundProgressBar);

		//获取自定义属性和默认值

		//roundProgressColor = mTypedArray.getColor(R.styleable.RoundProgressBar_roundProgressColor, Color.GREEN);
		roundProgressColor = mTypedArray.getColor(buaa.irisking.irisapp.R.styleable.RoundProgressBar_roundProgressColor, context.getResources().getColor(android.R.color.white));
		//textColor = mTypedArray.getColor(R.styleable.RoundProgressBar_textColor, Color.GREEN);
		textColor = mTypedArray.getColor(buaa.irisking.irisapp.R.styleable.RoundProgressBar_textColor,  context.getResources().getColor(android.R.color.white));
		textSize = mTypedArray.getDimension(buaa.irisking.irisapp.R.styleable.RoundProgressBar_textSize, 15);
		roundWidth = mTypedArray.getDimension(buaa.irisking.irisapp.R.styleable.RoundProgressBar_roundWidth, 5);
		max = mTypedArray.getInteger(buaa.irisking.irisapp.R.styleable.RoundProgressBar_max, 100);
//		textIsDisplayable = mTypedArray.getBoolean(R.styleable.RoundProgressBar_textIsDisplayable, true);
//		style = mTypedArray.getInt(R.styleable.RoundProgressBar_style, 0);
		roundColor = mTypedArray.getColor(buaa.irisking.irisapp.R.styleable.RoundProgressBar_roundColor, Color.RED);
		mTypedArray.recycle();
		
//		mBackgroundBitmap = ((BitmapDrawable)getResources().getDrawable(R.drawable.b_3_2_ok_00)).getBitmap();
		setImageDrawable(new ColorDrawable(getResources().getColor(buaa.irisking.irisapp.R.color.ik_full_transparent)));

//		setDrawingCacheEnabled(false);
//		setImageBitmap(mBackgroundBitmap);
		ifHorScale = false;
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(mBGWidth == 0 || mBGHeight == 0){
			mBGWidth = getMeasuredWidth();
			mBGHeight = getMeasuredHeight();
			radius = (float) (mBGWidth * 50 / 360);
		}
		if(horScale != 1.0f && ifHorScale){
			radius *= horScale;
			ifHorScale = false;
		}
		
		/**
		 * 画最外层的大圆环
		 */
		paint.setColor(roundColor); //设置圆环的颜色
		paint.setStyle(Paint.Style.STROKE); //设置空心
		paint.setStrokeWidth(roundWidth); //设置圆环的宽度
		paint.setAntiAlias(true);  //消除锯齿

		canvas.drawCircle(x1, y, radius, paint); //画出圆环
		canvas.drawCircle(x2 , y, radius, paint); //画出圆环

		/**
		 * 画进度百分比
		 */
		paint.setStrokeWidth(0);
		paint.setColor(textColor);
		paint.setTextSize(textSize);
		paint.setTypeface(Typeface.DEFAULT_BOLD); //设置字体


		/**
		 * 画圆弧 ，画圆环的进度
		 */
		paint.setStrokeWidth(roundWidth); //设置圆环的宽度
		paint.setColor(roundProgressColor);  //设置进度的颜色

		left_oval.set(x1 - radius, y - radius, x1 + radius, y + radius);
		right_oval.set(x2 - radius, y - radius, x2 + radius, y + radius);
		
		paint.setStyle(Paint.Style.STROKE);
		if(max <= 0){
			max = 1;
		}
		canvas.drawArc(left_oval, -90, 360 * left_progress / max, false, paint);  //根据进度画圆弧
		canvas.drawArc(right_oval, -90, 360 * right_progress / max, false, paint);  //根据进度画圆弧
		
	}
	public synchronized void setXAndY(float w1,float w2,float height){
		this.x1 = w1;
		this.x2 = w2;
		this.y = height;
	}
	public synchronized void setHorScale(float horScale){
		this.horScale = horScale;
		ifHorScale = true;
	}

	public synchronized int getMax() {
		return max;
	}

	/**
	 * 设置进度的最大值
	 * @param max
	 */
	public synchronized void setMax(int max) {
		if(max < 0){
			throw new IllegalArgumentException("max not less than 0");
		}
		this.max = max;
	}

	/**
	 * 获取进度.需要同步
	 * @return
	 */
	public synchronized int getLeftProgress() {
		return left_progress;
	}
	public synchronized int getRightProgress() {
		return right_progress;
	}

//	/**
//	 * 设置进度，此为线程安全控件，由于考虑多线的问题，需要同步
//	 * 刷新界面调用postInvalidate()能在非UI线程刷新
//	 */
//	public synchronized void setLeftProgress(int leftProgress) {
//		if(leftProgress < 0){
//			throw new IllegalArgumentException("progress not less than 0");
//		}
//		if(leftProgress > max){
//			leftProgress = max;
//		}
//		if(leftProgress <= max){
//			this.left_progress = leftProgress;
//			postInvalidate();
//		}
//	}
//	
//	public synchronized void setRightProgress(int rightProgress) {
//		if(rightProgress < 0){
//			throw new IllegalArgumentException("progress not less than 0");
//		}
//		if(rightProgress > max){
//			rightProgress = max;
//		}
//		if(rightProgress <= max){
//			this.right_progress = rightProgress;
//			postInvalidate();
//		}
//	}
	
	public synchronized void setLeftAndRightProgress(int leftProgress, int rightProgress, int needCount) {
		if(leftProgress <0 || rightProgress < 0){
			throw new IllegalArgumentException("progress not less than 0");
		}

		this.max = needCount;
		this.left_progress = leftProgress;
		this.right_progress = rightProgress;
		postInvalidate();
	}


	public int getCricleColor() {
		return roundColor;
	}

	public void setCricleColor(int cricleColor) {
		this.roundColor = cricleColor;
	}

	public int getCricleProgressColor() {
		return roundProgressColor;
	}

	public void setCricleProgressColor(int cricleProgressColor) {
		this.roundProgressColor = cricleProgressColor;
	}

	public int getTextColor() {
		return textColor;
	}

	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}

	public float getTextSize() {
		return textSize;
	}

	public void setTextSize(float textSize) {
		this.textSize = textSize;
	}

	public float getRoundWidth() {
		return roundWidth;
	}

	public void setRoundWidth(float roundWidth) {
		this.roundWidth = roundWidth;
	}


}
