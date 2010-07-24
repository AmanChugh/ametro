/*
 * http://code.google.com/p/ametro/
 * Transport map viewer for Android platform
 * Copyright (C) 2009-2010 Roman.Golovanov@gmail.com and other
 * respective project committers (see project home page)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package org.ametro.dialog;

import java.util.List;

import org.ametro.Constants;
import org.ametro.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class LocationSearchDialog extends Activity implements LocationListener, OnClickListener {

	public static final String LOCATION = "LOCATION";

	private Location mLocation;

	private List<String> mProviders;

	private LocationManager mLocationManager;
	private Button mCancelButton;
	
	private static final int REQUEST_ENABLE_LOCATION_SERVICES = 1;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.location_search);
		mCancelButton = (Button)findViewById(R.id.btn_cancel);
		mCancelButton.setOnClickListener(this);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if(!isLocationProvidersEnabled()){
			startActivityForResult(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS), REQUEST_ENABLE_LOCATION_SERVICES); 
			Toast.makeText(this, R.string.msg_location_need_enable_providers, Toast.LENGTH_LONG).show();
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_LOCATION_SERVICES:
			if(!isLocationProvidersEnabled()){
				setResult(RESULT_CANCELED);
				finish();
			}
			break;

		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	protected void onResume() {
		bindLocationProviders();
		super.onResume();
	}

	protected void onPause() {
		unbindLocationProviders();
		super.onPause();
	}

	public void onLocationChanged(Location location) {
		if (location != null) {
			if (Log.isLoggable(Constants.LOG_TAG_MAIN, Log.DEBUG)) {
				Log.d(Constants.LOG_TAG_MAIN,
						"Received location change from provider "
						+ location.getProvider().toUpperCase());
			}
			if (mLocation == null) {
				if (mLocation == null) {
					mLocation = new Location(location);
				}
				finishWithResult();
			}
		}
	}

	private boolean isLocationProvidersEnabled() {
		mProviders = mLocationManager.getProviders(true);
		return mProviders!=null && mProviders.size() > 0;
	}	

	private void bindLocationProviders() {
		mProviders = mLocationManager.getAllProviders();
		for (String provider : mProviders) {
			if (Log.isLoggable(Constants.LOG_TAG_MAIN, Log.DEBUG)) {
				Log.d(Constants.LOG_TAG_MAIN, "Register listener for location provider " + provider);
			}
			mLocationManager.requestLocationUpdates(provider, 0, 0, this);
		}
	}

	private void unbindLocationProviders() {
		if (Log.isLoggable(Constants.LOG_TAG_MAIN, Log.DEBUG)) {
			Log.d(Constants.LOG_TAG_MAIN, "Remove listeners for location providers");
		}		
		mLocationManager.removeUpdates(this);
	}

	private void finishWithResult(){
		if (mLocation != null) {
			Intent data = new Intent();
			data.putExtra(LOCATION, mLocation);
			setResult(RESULT_OK, data);
		} else {
			setResult(RESULT_OK);
		}
		finish();
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle bundle) {
	}

	public void onClick(View v) {
		if(v == mCancelButton){
			setResult(RESULT_CANCELED);
			finish();
		}
	}

}
