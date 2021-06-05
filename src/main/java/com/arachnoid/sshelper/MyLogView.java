package com.arachnoid.sshelper;

import android.content.Context;
import android.graphics.Typeface;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

final public class MyLogView extends android.support.v7.widget.AppCompatTextView {
    SSHelperApplication app;
    SSHelperActivity activity;
    long lastTouchTime = 0;
    // time is in milliseconds
    long touchDelayMS = 15000;

    public MyLogView(Context context) {
        super(context);
        init(context);
    }

    public MyLogView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyLogView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MyLogView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        if (!isInEditMode()) {
            activity = (SSHelperActivity) context;
            app = activity.app;
            setTypeface(Typeface.MONOSPACE);
            setMovementMethod(new ScrollingMovementMethod());
            setHorizontallyScrolling(true);
            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    lastTouchTime = System.currentTimeMillis();
                    return false;
                }
            });
            setOnLongClickListener(new OnLongClickListener() {

                public boolean onLongClick(View v) {
                    // app.debugLog("MyEditText","--- onLongClick ---");
                    if (app.logView != null) {
                        app.logView.showDialog();
                    }
                    return true;
                }
            });
        }
    }

    // if user touches display, disable bottom-gravity for n seconds
    // this allows the user to scroll and examine the log

    protected void update() {
        if (!isInEditMode()) {
            long time = System.currentTimeMillis();
            if (time > (lastTouchTime + touchDelayMS)) {
                this.setGravity(Gravity.BOTTOM);
                // app.debugLog("update log display","gravity = bottom");
            } else {
                this.setGravity(Gravity.NO_GRAVITY);
                // app.debugLog("update log display","gravity = none");
            }
        }
    }

    protected void showDialog() {
        if (!isInEditMode()) {
            dismissDialog();
            if (activity != null) {
                app.logConfigDialog = new LogDialog(activity);
                app.logConfigDialog.setOwnerActivity(activity);
                app.logConfigDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
                app.logConfigDialog.setContentView(R.layout.log_dialog);
                app.logConfigDialog.setTitle("SSHelper Log Display Options");
                app.logConfigDialog.setFeatureDrawableResource(
                        Window.FEATURE_LEFT_ICON, R.mipmap.ic_launcher_foreground);
                app.logConfigDialog.show();
            }
        }
    }

    protected void dismissDialog() {
        if (!isInEditMode()) {
            try {
                if (app.logConfigDialog != null) {
                    app.logConfigDialog.dismiss();
                    app.logConfigDialog = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
