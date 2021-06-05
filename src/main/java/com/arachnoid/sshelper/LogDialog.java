/***************************************************************************
 *   Copyright (C) 2013 by Paul Lutus                                      *
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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

final public class LogDialog extends Dialog {
	SSHelperActivity activity;
	public LogDialog(SSHelperActivity p) {
		super(p);
		activity = p;
		// TODO Auto-generated constructor stub
	}
	
	@SuppressLint("InflateParams")
	@Override
	 protected void onCreate(final Bundle savedInstanceState)
	 {
	  super.onCreate(savedInstanceState);
	  RelativeLayout ll=(RelativeLayout) LayoutInflater.from(activity).inflate(R.layout.log_dialog, null);
	  setContentView(ll);
	 }

}
