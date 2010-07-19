/*
 * http://code.google.com/p/ametro/
 * Transport map viewer for Android platform
 * Copyright (C) 2009-2010 Roman.Golovanov@gmail.com and other
 * respective project committers (see project home page)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.ametro.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;

public class CheckedLinearLayout extends LinearLayout implements Checkable {

	private CheckedTextView mText;
	
	public CheckedLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckedLinearLayout(Context context) {
		super(context);
	}

	public boolean isChecked() {
		return getTextView().isChecked();
	}

	public void setChecked(boolean checked) {
		CheckedTextView view = getTextView();
		if(view.isEnabled()){
			view.setChecked(checked);
		}
	}

	public void toggle() {
		CheckedTextView view = getTextView();
		if(view.isEnabled()){
			view.toggle();
		}
	}

	private CheckedTextView getTextView(){
		if(mText==null){
			mText = (CheckedTextView)findViewById(android.R.id.text1);
		}
		return mText;
	}

}
