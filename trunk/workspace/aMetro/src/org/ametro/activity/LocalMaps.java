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

package org.ametro.activity;

import java.util.ArrayList;
import java.util.Locale;

import org.ametro.Constants;
import org.ametro.MapUri;
import org.ametro.R;
import org.ametro.adapter.LocalCatalogAdapter;
import org.ametro.catalog.Catalog;
import org.ametro.catalog.CatalogMapDifference;
import org.ametro.catalog.storage.CatalogStorage;
import org.ametro.catalog.storage.ICatalogStorageListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.OnChildClickListener;

public class LocalMaps extends Activity implements ICatalogStorageListener, OnChildClickListener {

	private static final int MODE_WAIT = 0;
	private static final int MODE_LIST = 1;
	private static final int MODE_EMPTY = 2;
	
	public static final String EXTRA_FAVORITES_ONLY = "FAVORITES_ONLY";
	
	private CatalogStorage mStorage;
	private Catalog mLocal;
	private Catalog mOnline;
	private boolean mDownloading;
	
	private int mMode;
	
	private LocalCatalogAdapter mAdapter;
	private ArrayList<CatalogMapDifference> mCatalogDifferences;
	private ExpandableListView mList;
	
	private boolean mFavoritesOnly;
	
	private final int MAIN_MENU_REFRESH = 1;
	private final int MAIN_MENU_LOCATION = 2;
	private final int MAIN_MENU_SETTINGS = 3;
	private final int MAIN_MENU_ABOUT = 4;
	
	private final static int REQUEST_SETTINGS = 1;
	private final static int REQUEST_LOCATION = 2;
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MAIN_MENU_REFRESH, 0, R.string.menu_refresh).setIcon(android.R.drawable.ic_menu_rotate);
		menu.add(0, MAIN_MENU_LOCATION, 1, R.string.menu_location).setIcon(android.R.drawable.ic_menu_mylocation);
		menu.add(0, MAIN_MENU_SETTINGS, 2, R.string.menu_settings).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MAIN_MENU_ABOUT, 3, R.string.menu_about).setIcon(android.R.drawable.ic_menu_help);

		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(MAIN_MENU_LOCATION).setVisible(true);
		return super.onPrepareOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MAIN_MENU_REFRESH:
			setWaitView();
			mStorage.requestLocalCatalog(true);
			return true;
		case MAIN_MENU_LOCATION:
			startActivityForResult(new Intent(this, SearchLocation.class), REQUEST_LOCATION);
			return true;
		case MAIN_MENU_SETTINGS:
			startActivityForResult(new Intent(this, Settings.class), REQUEST_SETTINGS);
			return true;
		case MAIN_MENU_ABOUT:
			startActivity(new Intent(this, About.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent data = getIntent();
		if(data!=null){
			mFavoritesOnly = data.getBooleanExtra(EXTRA_FAVORITES_ONLY, false);
		}else{
			mFavoritesOnly = false;
		}
		
		mStorage = AllMaps.Instance.getStorage();
		setWaitView();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SETTINGS:
			//updateLocationState();
			break;
		case REQUEST_LOCATION:
			if(resultCode == RESULT_OK){
//				Location location = data.getParcelableExtra(SearchLocation.LOCATION);
//				mLocationSearchTask = new LocationSearchTask();
//				mLocationSearchTask.execute(location);
			}
			if(resultCode == RESULT_CANCELED){
				Toast.makeText(this,R.string.msg_location_unknown, Toast.LENGTH_SHORT).show();			
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}	
	
	protected void onResume() {
		mLocal = mStorage.getLocalCatalog();
		mOnline = mStorage.getOnlineCatalog();
		mStorage.addCatalogChangedListener(this);
		if(mLocal == null){
			mStorage.requestLocalCatalog(false);
		}else if(mMode != MODE_LIST){
			setListView();
		}
		super.onResume();
	}
	
	protected void onPause() {
		mStorage.removeCatalogChangedListener(this);
		super.onPause();
	}
	
	private void setListView() {
		if(mLocal.getMaps().size()>0 || (mOnline!=null && mOnline.getMaps().size()>0)){
			if(Log.isLoggable(Constants.LOG_TAG_MAIN, Log.DEBUG)){
				Log.d(Constants.LOG_TAG_MAIN, "Setup map list view");
			}
			setContentView(R.layout.maps_list);
			mCatalogDifferences = Catalog.diff(mLocal, mOnline, Catalog.MODE_LEFT_JOIN);
			setContentView(R.layout.browse_catalog_main);
			mList = (ExpandableListView)findViewById(R.id.browse_catalog_list);
			mAdapter = new LocalCatalogAdapter(this, mCatalogDifferences, Locale.getDefault().getLanguage() ); 
			mList.setAdapter(mAdapter);
			mList.setOnChildClickListener(this);
			mMode = MODE_LIST;
		}else if(mOnline == null && mDownloading){
			setWaitView();
		}else{
			setEmptyView();
		}
	}
	
	private void setWaitView() {
		setContentView(R.layout.maps_wait);
		mMode = MODE_WAIT;
	}
	
	private void setEmptyView() {
		setContentView(R.layout.maps_list_empty);
		((TextView)findViewById(R.id.maps_message)).setText(mFavoritesOnly ? R.string.msg_no_maps_in_favorites : R.string.msg_no_maps_in_local);
		mMode = MODE_EMPTY;
	} 

	public void onCatalogLoaded(int catalogId, Catalog catalog) {
		if(catalogId == CatalogStorage.CATALOG_LOCAL){
			if(catalog != null){
				mLocal = catalog;
				setListView();
			}else{
				setEmptyView();
			}
			if(mOnline == null){
				mDownloading = true;
				mStorage.requestOnlineCatalog(false); 
			}		
		}
		if(catalogId == CatalogStorage.CATALOG_ONLINE){
			mOnline = catalog;
			mDownloading = false;
			setListView();
		}
	}

	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		CatalogMapDifference diff = (CatalogMapDifference)mAdapter.getChild(groupPosition, childPosition);
		if(diff.isLocalAvailable()){
			String fileName = diff.getLocalUrl();
			Intent i = new Intent();
			i.setData(MapUri.create( mLocal.getBaseUrl() + "/" + fileName));
			
			AllMaps.Instance.setResult(RESULT_OK, i);
			AllMaps.Instance.finish();
		}else{
			Intent i = new Intent(this, BrowseMapDetails.class);
			i.putExtra(BrowseMapDetails.ONLINE_MAP_URL, diff.getRemoteUrl());
			startActivity(i);
		}
		return true;
	}
	
}
