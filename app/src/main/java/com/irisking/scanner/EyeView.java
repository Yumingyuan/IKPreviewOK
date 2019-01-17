package com.irisking.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.irisking.irisalgo.util.Config;
import com.irisking.scanner.IEyeFocus.EyeDetectFocus;
import com.irisking.scanner.IEyeFocus.EyeDetectFocusSingleEye;

public class EyeView extends View
{
    public IEyeFocus eyeDetectView;
    
    public EyeView(Context context) 
    {
        this(context, null, 0);
    }
    
    public EyeView(Context context, AttributeSet attrs) 
    {
    	this(context, attrs, 0);
    }

    public EyeView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        
        if(Config.DEVICE_SINGLE_EYE){
        	eyeDetectView = new EyeDetectFocusSingleEye();
        }else{
        	eyeDetectView = new EyeDetectFocus();
        }
    }

    public void setEyeFocus(IEyeFocus focus)
    {
    	this.eyeDetectView = focus;
    }
    public IEyeFocus getEyeFocus() {
    	return eyeDetectView;
    }
    
    @Override
    protected void onDraw(Canvas canvas) 
    {
        super.onDraw(canvas);

        if (eyeDetectView != null) {
        	eyeDetectView.draw(canvas);
        }
    }
}
