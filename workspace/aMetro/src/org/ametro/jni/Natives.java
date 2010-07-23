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
package org.ametro.jni;

import org.ametro.Constants;
import org.ametro.render.RenderElement;

import android.graphics.RectF;
import android.util.Log;

public class Natives {
	
	public static final boolean INITIALIZED;
	public static boolean REQUESTED;
	
	static {
		boolean ini = false;
		try {
			System.loadLibrary("ametro");
			//ini = true; //  <- Do not use natives now, for performance issues
    		if(Log.isLoggable(Constants.LOG_TAG_MAIN, Log.INFO)){
    			Log.i(Constants.LOG_TAG_MAIN,"Native library initialized");
    		}
		} catch (Throwable t) {
    		if(Log.isLoggable(Constants.LOG_TAG_MAIN, Log.WARN)){
    			Log.w(Constants.LOG_TAG_MAIN, "Native library loading error", t);
    		}
		}
		INITIALIZED = ini;
	}   
	 
	public static native String[] SplitCsvString(String src, char separator);
	public static native RenderElement[] getVisibleRenderElements(RenderElement[] elements, RectF[] views, int filter);
	
	public static void Initialize() { // this is a fake method
		REQUESTED = true;
	}
}
