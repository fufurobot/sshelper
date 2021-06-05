/*
 ***************************************************************************
 *   Copyright (C) 2012 by Paul Lutus                                      *
 *   http://arachnoid.com/administration                                   *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package com.arachnoid.sshelper;

import android.content.Context;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.util.Log;

final public class MyEditText extends android.support.v7.widget.AppCompatEditText implements KeyEvent.Callback {

	SSHelperApplication app;
	SSHelperActivity view;
	MyEditText instance;

	public MyEditText(Context context) {
		super(context);
		init(context);
	}

	public MyEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MyEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {


		if(!isInEditMode()) {
			view = (SSHelperActivity) context;
			app = view.app;

			setCursorVisible(true);
			setFocusable(true);
			setFocusableInTouchMode(true);
			setMovementMethod(new ScrollingMovementMethod());
			instance = this;
			setHint("Short press: keyboard, long press: option dialog.");
			setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
					// app.debugLog("MyEditText","--- onLongClick ---");
					if (app != null && app.terminal != null) {
						app.terminal.showDialog();
					}
					return true;
				}
			});
		}
	}

	// This strange hack allows the soft keyboard keys to
	// be sent to the default keyboard handlers below

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		// app.logString("onCreateInputConnection");
		outAttrs.inputType = InputType.TYPE_NULL;
		return new BaseInputConnection(this, false);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// this.onk
		//Log.e("ZZZ: onKeyDown:", "[" + event + "]");
		if (event.isSystem()) {
			// Don't intercept the system keys
			return super.onKeyDown(keyCode, event);
		} else {
			if (app.terminal != null) {
				app.terminal.keyboardInput(event);
			}
		}
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (event.isSystem()) {
			// Don't intercept the system keys
			return super.onKeyUp(keyCode, event);
		}
		return true;
	}

}
