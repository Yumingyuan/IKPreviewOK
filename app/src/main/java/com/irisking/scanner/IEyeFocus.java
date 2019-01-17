package com.irisking.scanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RectF;

import com.irisking.irisalgo.util.EnumEyeType;
import com.irisking.scanner.model.EyePosition;

public interface IEyeFocus {

	public void init(int viewAngle, int picH, int picW, int canvasH, int canvasW);
	
	public void draw(Canvas canvas);

	public void setProgress(int leftPos, int leftTotal, int rightPos, int rightTotal, boolean isLeftOk, boolean isRightOk);
	public void setDetectResult(EyePosition leftIris, EyePosition rightIris, int distEstimation);
	public void setDistanceResult(int distEstimation);
	public void reset();
	
	class EyeDetectFocus implements IEyeFocus {
		private double zoomH = 1.0d;
		private double zoomW = 1.0d;
		// 定位结果
		private EyePosition leftIris = new EyePosition();
		private EyePosition rightIris = new EyePosition();

		private int radius;
		private int startX = 0;
		private int startY = 0;
		private int bigBgX = 0;
		private int bigBgY = 0;
		private int bigEdX = 0;

		// 为单眼注册设置的变量,记录初始大框(识别时)的初始值
		private int initBigBgX = 0;
		private int initBigEdX = 0;

		private boolean ifLeftOk = false;
		private boolean ifRightOk = false;

		private Paint BigRectPaint, eyePaint, eyeFlagPaint, progressPosPaint, progressTotalPaint, idenSuccPaint, textPaint, defauldIR;
		private int progressStrokeWidth = 6;
		private int eyeStrokeWidth = 6;
		private int bigRecWidth = (int) (eyeStrokeWidth * 3);
		private int bigRectAdjust = bigRecWidth / 2;
		private int idenSuccStrokeWidth = 40;

		// 显示绝对布局图像用到的变量
		public int displayImW = 0;
		public int displayImH = 0;
		public float canvas2ROI = (float) 0;
		public float displayStartXPos = (float) 0;
		public float displayStartYPos = (float) 0;
		public float displayEndXPos = (float) 0;
		public float displayEndYPos = (float) 0;

		public int displayDeltaX = 0;
		public int displayDeltaY = 0;
		public int eyeDtRtDeltaX = 0;
		public int eyeDtRtDeltaY = 0;
		// 主要标记了截取图像用于显示时的ROI设置
		public float roiBgXPos = (float) (0); // 显示时需要移动的比例
		public float roiEdXPos = (float) (0); // 显示时需要移动的比例
		public float roiBgYPos = (float) (0); // 显示时需要移动的比例
		public float roiEdYPos = (float) (0); // 显示时需要移动的比例
		// 主要标记了降采样图像用于眼睛检测时的ROI设置，此设置范围应大于用于图像显示的区域
		public float dnImRoiBgXPos = (float) (0); //
		public float dnImRoiEdXPos = (float) (0); //
		public float dnImRoiBgYPos = (float) (0); //
		public float dnImRoiEdYPos = (float) (0); //
		// 主要标记了降采样图像用于眼睛检测时的ROI设置，此设置范围应大于用于图像显示的区域
		public int displayStartX = 0;
		public int displayStartY = 0;
		public int displayEndX = 0;
		public int displayEndY = 0;

		public int preDnRate;
		public int bestIrisRd4Display = 250;

		public EyeDetectFocus() {
			BigRectPaint = new Paint();
			BigRectPaint.setAntiAlias(true);
			BigRectPaint.setColor(Color.GREEN);
			BigRectPaint.setStrokeWidth(bigRecWidth);
			BigRectPaint.setStyle(Paint.Style.STROKE);

			eyePaint = new Paint();
			eyePaint.setAntiAlias(true);
			eyePaint.setColor(Color.GREEN);
			eyePaint.setStrokeWidth(eyeStrokeWidth);
			eyePaint.setStyle(Paint.Style.STROKE);

			eyeFlagPaint = new Paint();
			eyeFlagPaint.setAntiAlias(true);
			eyeFlagPaint.setColor(Color.GREEN);
			eyeFlagPaint.setStrokeWidth(eyeStrokeWidth * 3);
			eyeFlagPaint.setStyle(Paint.Style.STROKE);

			// The big rect of the whole image
			progressPosPaint = new Paint();
			progressPosPaint.setAntiAlias(true);
			progressPosPaint.setColor(Color.GREEN);
			progressPosPaint.setStrokeWidth(progressStrokeWidth);
			progressPosPaint.setStyle(Paint.Style.STROKE);

			progressTotalPaint = new Paint();
			progressTotalPaint.setAntiAlias(true);
			progressTotalPaint.setColor(Color.GRAY);
			progressTotalPaint.setStrokeWidth(progressStrokeWidth);
			progressTotalPaint.setStyle(Paint.Style.STROKE);

			idenSuccPaint = new Paint();
			idenSuccPaint.setAntiAlias(true);
			idenSuccPaint.setColor(Color.GREEN);
			idenSuccPaint.setStrokeWidth(idenSuccStrokeWidth);
			idenSuccPaint.setStyle(Paint.Style.STROKE);

			textPaint = new Paint();
			textPaint.setAntiAlias(true);
			textPaint.setColor(Color.RED);
			textPaint.setTextSize(50);
			textPaint.setStyle(Paint.Style.STROKE);
			textPaint.setStrokeWidth(4);

			defauldIR = new Paint();
			defauldIR.setAntiAlias(true);
			defauldIR.setColor(Color.GRAY);// YELLOW
			// defauldIR.setTextSize(20);
			defauldIR.setStyle(Paint.Style.STROKE);
			// 绘制模式
			PathEffect effect = new DashPathEffect(new float[] { 6, 6 }, 1);
			defauldIR.setAntiAlias(true);
			defauldIR.setPathEffect(effect);
			defauldIR.setStrokeWidth(6);
		}

		@Override
		public void init(int rotateAngle, int previewW, int previewH, int eyeViewWidth, int eyeViewHeight) {
			// 根据设备类型对横屏和竖屏时显示的区域等信息进行配置
			preDnRate = 4;

			if (rotateAngle % 180 == 0) {
				displayImH = (int) (((previewH - 1) / preDnRate) + 1);
				displayImW = (int) (((previewW - 1) / preDnRate) + 1);
				eyeViewHeight = (int) ((float) displayImH * ((float) eyeViewWidth / (float) displayImW));
				// 画布显示时的放大率
				canvas2ROI = 1;

				// 下面的坐标是相对于界面显示时的坐标，即正面人脸坐标
				roiBgYPos = (float) (0);
				roiEdYPos = (float) (1);
				// 眼睛检测中降采样图像的ROI区域设置，是正面人脸坐标
				// NOTE1:X和Y必须是对称的，即dnImRoiBgYPos = 1-dnImRoiEdYPos
				// NOTE2:眼睛检测区域设置要大于设置的显示区域（具体打多少，要根据眼睛检测算法的设置进行配置）
				dnImRoiBgYPos = (float) (0.00);
				dnImRoiEdYPos = (float) (1.00);
				dnImRoiBgXPos = (float) (0.00);
				dnImRoiEdXPos = (float) (1.00);

				roiBgYPos = (float) (0);
				roiEdYPos = (float) (1);
				roiBgXPos = (float) (0);
				roiEdXPos = (float) (1.0);
				canvas2ROI = 1 / (roiEdXPos - roiBgXPos);

				// 图像显示和大框对应区域
				displayDeltaY = (int) (-roiBgYPos * (canvas2ROI * eyeViewHeight));// 显示时图像起始点需要移动的比例
				displayDeltaX = (int) (-roiBgXPos * (canvas2ROI * eyeViewWidth));// 显示时图像起始点需要移动的比例

				// 20150703可能是因为左右反转了，所以水平方向的x要是roiEdXPos-1，但是竖直方向发现还是应该是实际的起始位置
				eyeDtRtDeltaX = (int) ((roiEdXPos - 1) * (canvas2ROI * eyeViewWidth)); // 显示对准框和眼睛检测结果时的坐标偏移(似乎是对准框和图像的左右起始点有差异？？)
				eyeDtRtDeltaY = (int) ((roiBgYPos) * (canvas2ROI * eyeViewHeight)); // 显示对准框和眼睛检测结果时的坐标偏移

				displayStartX = (int) (roiBgXPos * (canvas2ROI * eyeViewWidth));
				displayEndX = (int) (roiEdXPos * (canvas2ROI * eyeViewWidth) - 1);
				displayStartY = (int) (roiBgYPos * (canvas2ROI * eyeViewHeight));
				displayEndY = (int) (roiEdYPos * (canvas2ROI * eyeViewHeight) - 1);

			} else {
				// 原始图像的大小（preDnRate降采样后的图像大小）
				displayImH = (int) (((previewW - 1) / preDnRate) + 1);
				displayImW = (int) (((previewH - 1) / preDnRate) + 1);

				eyeViewWidth = (int) ((float) displayImW * ((float) eyeViewHeight / (float) displayImH));
				;

				// 下面的坐标是相对于界面显示时的坐标，即正面人脸坐标
				// 眼睛检测中降采样图像的ROI区域设置，是正面人脸坐标
				// NOTE1:X和Y必须是对称的，即dnImRoiBgYPos = 1-dnImRoiEdYPos
				// NOTE2:眼睛检测区域设置要大于设置的显示区域（具体打多少，要根据眼睛检测算法的设置进行配置）
				dnImRoiBgXPos = (float) (0);
				dnImRoiEdXPos = (float) (1);
				dnImRoiBgYPos = (float) (0);
				dnImRoiEdYPos = (float) (1);

				roiBgXPos = (float) (0);
				roiEdXPos = (float) (1);
				roiBgYPos = (float) (0);
				roiEdYPos = (float) (1);
				// 画布显示时的放大率
				canvas2ROI = 1;

				// 图像显示区域占整个图像的比例的倒数，以水平方向为基准
				// 可以简单理解为，该值为ROI区域占界面画布的比例除以ROI区域占原始图像的比例。
				// 如ROI区域为中间1/3(即显示区域为原始图像的1/3)，且准备将该ROI全部映射到画布中（ROI区域占界面画布的比例为1），则canvas2ROI
				// = 1/(1/3)；
				// 如ROI区域为中间1/3(即显示区域为原始图像的1/3)，且准备将该ROI映射到画布的中间1/3中(即ROI区域占界面画布的比例为1/3)，则canvas2ROI
				// = (1/3)/(1/3)；
				// canvas2ROI*canvasW就是整个图像对应的显示区域
				// //这个值是可以任意修改的，但应该保证(roiEdXPos-roiBgXPos)*canvas2ROI<=1,否则crop的ROI区域就大于显示画布的大小了，就没有意义了
				if ((roiEdXPos - roiBgXPos) * canvas2ROI > 1) {
					canvas2ROI = 1 / (roiEdXPos - roiBgXPos);
				}

				// 眼睛检测结果要显示在画布上，对眼睛检测结果显示时要求的平移量
				// 如果canvas2ROI任意设置，则需要调整displayStartX
				eyeDtRtDeltaX = (int) ((-roiBgXPos) * (canvas2ROI * eyeViewWidth)); // 显示对准框和眼睛检测结果时的坐标偏移(似乎是对准框和图像的左右起始点有差异？？)
				eyeDtRtDeltaY = (int) ((roiEdYPos - 1) * (canvas2ROI * eyeViewHeight)); // 显示对准框和眼睛检测结果时的坐标偏移

				// 用于显示的画布区域的坐标配置，相对于整个图像对应的大画布（即整个图像对应的画布大小）
				displayStartX = (int) (roiBgXPos * (canvas2ROI * eyeViewWidth));
				displayEndX = (int) (roiEdXPos * (canvas2ROI * eyeViewWidth) - 1);
				displayStartY = (int) (roiBgYPos * (canvas2ROI * eyeViewHeight));
				displayEndY = (int) (roiEdYPos * (canvas2ROI * eyeViewHeight) - 1);
				// 上面得到的4个坐标是相对于整个图像对应的画布大小的坐标，而实际手机界面中，坐上角为0,0点。
				// 因此，需要将上述4个坐标通过displayDeltaX，displayDeltaY两个平移量，转换到相对于手机左上角0,0点的坐标中
				// 因为顶部总是顶在图像上部，所以如果是Y方向有上移量，所以要加一个DeltaX或者DeltaY
				displayDeltaX = (int) (-roiBgXPos * (canvas2ROI * eyeViewWidth));// 显示时图像起始点需要移动的比例
				// 默认竖直使用时，显示区域顶着手机屏幕的上边沿
				displayDeltaY = (int) (-roiBgYPos * (canvas2ROI * eyeViewHeight));// 显示时图像起始点需要移动的比例

				// 如果canvas2ROI*(roiEdXPos-roiBgXPos)<1，则表示ROI显示区域未占满整个屏幕的水平方向，
				// 而如果仍然按照eyeDtRtDeltaX和displayDeltaX进行平移，则结果是永远沿着屏幕的左侧边沿，
				// 即显示区域不是在中间，而是在左上角。因此，需要针对这种情况将显示区域和眼睛检测结果的坐标向右平移
				if (canvas2ROI * (roiEdXPos - roiBgXPos) < 1) {
					eyeDtRtDeltaX = eyeDtRtDeltaX + (int) ((1 - canvas2ROI * (roiEdXPos - roiBgXPos)) * eyeViewWidth / 2);
					displayDeltaX = displayDeltaX + (int) ((1 - canvas2ROI * (roiEdXPos - roiBgXPos)) * eyeViewWidth / 2);
				}
			}
			// 前4个参数表示了相对的比例
			// 1,2两个参数表示原始图像的大小，3,4两个参数表示要实现这个原始图像，需要的画布的大小
			// 即准备的画布的大小，要与图像对应起来。前4个参数为比例关系，第7,8两个参数为平移关系，不能混淆
			// 第7,8两个参数表示了眼睛检测结果要平移的量
			bestIrisRd4Display = 80;
			int canvasH = (int) (canvas2ROI * eyeViewHeight);
			int canvasW = (int) (canvas2ROI * eyeViewWidth);
			if (rotateAngle % 180 == 0) {
				zoomW = (double) canvasW / previewW;
				zoomH = (double) canvasH / previewH;
			} else {
				zoomW = (double) canvasW / previewH;
				zoomH = (double) canvasH / previewW;
			}

			// 眼睛半径
			radius = (int) (bestIrisRd4Display * zoomW);
			// 坐标显示的偏移量
			startX = eyeDtRtDeltaX;
			startY = eyeDtRtDeltaY;
			// 最大框ROI区域的位置
			bigBgX = displayDeltaX + displayStartX + bigRectAdjust;
			if (bigBgX < bigRectAdjust) {
				bigBgX = bigRectAdjust;
			}

			bigBgY = displayDeltaY + displayStartY + bigRectAdjust;
			if (bigBgY < bigRectAdjust) {
				bigBgY = bigRectAdjust;
			}

			bigEdX = displayDeltaX + displayEndX - bigRectAdjust;

			// 记录大框的初始值
			initBigBgX = bigBgX;
			initBigEdX = bigEdX;
		}

		private void draw(Canvas canvas, EyePosition irisPos, int progressPos, int progressTotal, int eyeType, boolean ifEnrOk) {
			if (!irisPos.valid) {
				return;
			}
			// 虹膜定位圆
			canvas.drawCircle(startX + irisPos.irisX, startY + irisPos.irisY, radius, eyePaint);
		}

		private boolean detected = false;

		public void reset(int total) {
			reset();
			leftTotal = rightTotal = total;
		}

		public void reset() {
			detected = false;
			leftState = rightState = 0;
		}

		@Override
		public void draw(Canvas canvas) {
			{
				// 如果以前检测到过，那么就显示上次检测到的结果或者最新的结果
				if (detected) {
					draw(canvas, leftIris, leftState, leftTotal, EnumEyeType.LEFT, ifLeftOk);
					draw(canvas, rightIris, rightState, rightTotal, EnumEyeType.RIGHT, ifRightOk);
				} else {
				}
			}
		}

		public void setFixed(int dnIDLRow, int dnIDLCol, int dnIDRRow, int dnIDRCol) {
			this.leftIris.dnIDRow = (int) (zoomW * dnIDLRow * 4);
			this.leftIris.dnIDCol = (int) (zoomH * dnIDLCol * 4);
			this.rightIris.dnIDRow = (int) (zoomW * dnIDRRow * 4);
			this.rightIris.dnIDCol = (int) (zoomH * dnIDRCol * 4);
		}

		public void setDetectResult(EyePosition leftIris, EyePosition rightIris, int distEstimation) {
			boolean existValid = leftIris.valid || rightIris.valid;

			bigBgX = initBigBgX;
			bigEdX = initBigEdX;

			if (existValid) {
				// this.leftIris = leftIris;
				this.leftIris.valid = leftIris.valid;
				this.leftIris.lrFlag = leftIris.lrFlag;
				this.leftIris.imgX = (int) (zoomW * leftIris.imgX);
				this.leftIris.imgY = (int) (zoomH * leftIris.imgY);

				this.leftIris.irisX = (int) (zoomW * leftIris.irisX);
				this.leftIris.irisY = (int) (zoomH * leftIris.irisY);
				// this.rightIris = rightIris;
				this.rightIris.valid = rightIris.valid;
				this.rightIris.lrFlag = rightIris.lrFlag;
				this.rightIris.imgX = (int) (zoomW * rightIris.imgX);
				this.rightIris.imgY = (int) (zoomH * rightIris.imgY);
				this.rightIris.irisX = (int) (zoomW * rightIris.irisX);
				this.rightIris.irisY = (int) (zoomH * rightIris.irisY);
			}

			// 只要一次检测到，永远就检测到了。
			detected = (detected || existValid);

			if (!existValid) {
				// 曾经检测到眼睛，但本次示检测到，显示灰色
				eyePaint.setColor(Color.GRAY);
				BigRectPaint.setColor(Color.GRAY);
			} else if (distEstimation < 0) {
				eyePaint.setColor(Color.BLUE);
				BigRectPaint.setColor(Color.BLUE);
			} else if (distEstimation == 0) {
				eyePaint.setColor(Color.GREEN);
				BigRectPaint.setColor(Color.GREEN);
			} else if (distEstimation == 100) {
				eyePaint.setColor(Color.GRAY);
				BigRectPaint.setColor(Color.GRAY);
			} else {
				eyePaint.setColor(Color.RED);
				BigRectPaint.setColor(Color.RED);
			}

		}

		private int leftState = 0, leftTotal = 0;
		private int rightState = 0, rightTotal = 0;

		@Override
		public void setProgress(int leftPos, int leftTotal, int rightPos, int rightTotal, boolean isLeftOk, boolean isRightOk) {
			this.leftState = leftPos;
			this.leftTotal = leftTotal;
			this.rightState = rightPos;
			this.rightTotal = rightTotal;
			this.ifLeftOk = isLeftOk;
			this.ifRightOk = isRightOk;
		}

		public void resetNonIris() {
		}

		@Override
		public void setDistanceResult(int distEstimation) {
		}
	}

	class EyeDetectFocusSingleEye implements IEyeFocus {

		private EyePosition leftIris = new EyePosition();
		
		private int eyeStrokeWidth = 12;

		private double zoomH = 1.0d;
		private double zoomW = 1.0d;

		private int smallRectWidth = 300;
		private int smallRectHeight = 200;
		
		private int bigRectWidth = 600;

		private int progressRadius = smallRectWidth / 2 + (int)Math.sqrt(Math.pow(smallRectWidth, 2) + Math.pow(smallRectHeight, 2)) / 2 + eyeStrokeWidth * 2;
		
		private int canvasW = 0;
		private int canvasH = 0;

		private Paint BigRectPaint, defauldIR, eyePaint, eyeFlagPaint, SmallRectPaint;
		
		public EyeDetectFocusSingleEye() {
			
			PathEffect effect = new DashPathEffect(new float[] { 6, 6 }, 1);

			BigRectPaint = new Paint();
			BigRectPaint.setAntiAlias(true);
			BigRectPaint.setColor(Color.GREEN);
			BigRectPaint.setStrokeWidth(4);
			BigRectPaint.setPathEffect(effect);
			BigRectPaint.setStyle(Paint.Style.STROKE);

			SmallRectPaint = new Paint();
			SmallRectPaint.setAntiAlias(true);
			SmallRectPaint.setColor(Color.GREEN);
			SmallRectPaint.setStrokeWidth(eyeStrokeWidth);
			SmallRectPaint.setStyle(Paint.Style.STROKE);

			eyePaint = new Paint();
			eyePaint.setAntiAlias(true);
			eyePaint.setColor(Color.GREEN);
			eyePaint.setStrokeWidth(eyeStrokeWidth);
			eyePaint.setStyle(Paint.Style.STROKE);

			eyeFlagPaint = new Paint();
			eyeFlagPaint.setAntiAlias(true);
			eyeFlagPaint.setColor(Color.GREEN);
			eyeFlagPaint.setStrokeWidth(eyeStrokeWidth * 1);
			eyeFlagPaint.setStyle(Paint.Style.STROKE);

			defauldIR = new Paint();
			defauldIR.setAntiAlias(true);
			defauldIR.setColor(Color.GRAY);// YELLOW
			defauldIR.setStyle(Paint.Style.STROKE);

			defauldIR.setAntiAlias(true);
			defauldIR.setPathEffect(effect);
			defauldIR.setStrokeWidth(6);
		}

		@Override
		public void init(int viewAngle, int picW, int picH, int canvasW, int canvasH) {
			// 因为屏幕是竖向显示，因此实际图像宽度应为picH
			if (viewAngle % 180 == 0) {
				zoomW = (double) canvasW / picW;
				zoomH = (double) canvasH / picH;
			} else {
				zoomW = (double) canvasW / picH;
				zoomH = (double) canvasH / picW;
			}
			
			this.canvasW = canvasW;
			this.canvasH = canvasH;
		}

		private void drawForSingleEye(Canvas canvas, EyePosition irisPos) {
			if (!irisPos.valid) {
				return;
			}
			// 画最大的矩形
			canvas.drawRect(canvasW / 2 - bigRectWidth / 2, 0, canvasW / 2 + bigRectWidth / 2, canvasH, BigRectPaint);
			
			// 画单眼中间的矩形
			canvas.drawRect(
					canvasW / 2 - smallRectWidth / 2, canvasH / 2 - smallRectHeight / 2, 
					canvasW / 2 + smallRectWidth / 2, canvasH / 2 + smallRectHeight / 2, 
					SmallRectPaint);

			RectF rF = new RectF(
					canvasW / 2 - progressRadius / 2, canvasH / 2 - progressRadius / 2, 
					canvasW / 2 + progressRadius / 2, canvasH / 2 + progressRadius / 2);
			
			if (leftState > 0) {
				// 如果左眼正在注册
				float progress = (float) leftState / leftTotal;
				canvas.drawArc(rF, -90, (int) (360 * progress), false, eyeFlagPaint);

			} else if (rightState > 0) {
				// 如果右眼正在注册
				float progress = (float) rightState / rightTotal;
				canvas.drawArc(rF, -90, (int) (360 * progress), false, eyeFlagPaint);
			}
		}

		private boolean detected = false;

		public void reset(int total) {
			reset();
			leftTotal = rightTotal = total;
		}

		public void reset() {
			detected = false;
			leftState = rightState = 0;
		}

		@Override
		public void draw(Canvas canvas) {
			EyePosition drawPosition = new EyePosition();
			drawPosition.irisX = 180 * 2;
			drawPosition.irisX = (int) (0.5 * canvasW);
			drawPosition.irisY = 202;// 170;
			drawPosition.valid = true;
			drawForSingleEye(canvas, drawPosition);
		}

		public void setDistanceResult(int distEstimation) {
			boolean existValid = true;

			// 只要一次检测到，永远就检测到了。
			detected = (detected || existValid);

			if (!existValid) {
				eyePaint.setColor(Color.GRAY);
				BigRectPaint.setColor(Color.GRAY);
			} else if (distEstimation == -1) {
				eyePaint.setColor(Color.BLUE);
				BigRectPaint.setColor(Color.BLUE);
			} else if (distEstimation == 0) {
				eyePaint.setColor(Color.GREEN);
				BigRectPaint.setColor(Color.GREEN);
			} else if (distEstimation == 100) {
				eyePaint.setColor(Color.GRAY);
				BigRectPaint.setColor(Color.GRAY);
			} else if (distEstimation == 1) {
				eyePaint.setColor(Color.RED);
				BigRectPaint.setColor(Color.RED);
			}
		}

		public void setDetectResult(EyePosition leftIris, EyePosition rightIris, int distEstimation) {

			boolean existValid = leftIris.valid || rightIris.valid;

			if (existValid) {
				// this.leftIris = leftIris;
				this.leftIris.valid = leftIris.valid;
				this.leftIris.lrFlag = leftIris.lrFlag;
				this.leftIris.imgX = (int) (zoomW * leftIris.imgX);
				this.leftIris.imgY = (int) (zoomH * leftIris.imgY);
				this.leftIris.irisX = (int) (zoomW * leftIris.irisX);
				this.leftIris.irisY = (int) (zoomH * leftIris.irisY);
			}

			// 只要一次检测到，永远就检测到了。
			detected = (detected || existValid);

			if (!existValid) {
				// 曾经检测到眼睛，但本次示检测到，显示灰色
				eyePaint.setColor(Color.GRAY);
				BigRectPaint.setColor(Color.GRAY);
			} else if (distEstimation < 0) {
				eyePaint.setColor(Color.BLUE);
				BigRectPaint.setColor(Color.BLUE);
			} else if (distEstimation == 0) {
				eyePaint.setColor(Color.GREEN);
				BigRectPaint.setColor(Color.GREEN);
			} else if (distEstimation == 100) {
				eyePaint.setColor(Color.GRAY);
				BigRectPaint.setColor(Color.GRAY);
			} else {
				eyePaint.setColor(Color.RED);
				BigRectPaint.setColor(Color.RED);
			}
		}

		private int leftState = 0, leftTotal = 0;
		private int rightState = 0, rightTotal = 0;

		public void setProgress(int leftPos, int leftTotal, int rightPos, int rightTotal, boolean isLeftOk, boolean isRightOk) {
			this.leftState = leftPos;
			this.leftTotal = leftTotal;
			this.rightState = rightPos;
			this.rightTotal = rightTotal;
		}

		public void resetNonIris() {
		}
	}

}
