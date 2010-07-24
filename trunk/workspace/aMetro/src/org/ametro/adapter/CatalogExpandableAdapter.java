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
 */package org.ametro.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.ametro.Constants;
import org.ametro.GlobalSettings;
import org.ametro.R;
import org.ametro.catalog.Catalog;
import org.ametro.catalog.CatalogMapPair;
import org.ametro.catalog.ICatalogStateProvider;
import org.ametro.catalog.CatalogMapPair.CatalogMapPairCityComparator;
import org.ametro.model.TransportType;
import org.ametro.util.StringUtil;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CatalogExpandableAdapter extends BaseExpandableListAdapter implements Filterable {

	protected Context mContext;
    protected LayoutInflater mInflater;
    
	protected ArrayList<CatalogMapPair> mObjects;
	protected ArrayList<CatalogMapPair> mOriginalValues;
	
	protected int mMode;
	protected String mLanguageCode;
	
	protected String[] mCountries;
	protected HashMap<String,Drawable> mIcons;
    protected CatalogMapPair[][] mRefs;

    protected String[] mStates;
    protected int[] mStateColors;
    
    protected ICatalogStateProvider mStatusProvider;
    
    protected HashMap<Integer,Drawable> mTransportTypes;
    
    private boolean mShowCountryFlags;
    
	private CatalogFilter mFilter;
	private Object mLock = new Object();
	private String mSearchPrefix;
	private Drawable mNoCountryIcon;

    public CatalogMapPair getData(int groupId, int childId) {
        return mRefs[groupId][childId];
    }

    public String getLanguage(){
    	return mLanguageCode;
    }
    
    public void setLanguage(String languageCode)
    {
    	mLanguageCode = languageCode;
    }
    
	public void updateData(Catalog local, Catalog remote)
	{
        synchronized (mLock) {
        	mOriginalValues = CatalogMapPair.diff(local, remote, mMode);
            if (mSearchPrefix == null || mSearchPrefix.length() == 0) {
            	mObjects = new ArrayList<CatalogMapPair>(mOriginalValues);
            } else {
                mObjects = getFilteredData(mSearchPrefix);
            }
        }
		bindData();
		notifyDataSetChanged();
	}
    
    public CatalogExpandableAdapter(Context context, Catalog local, Catalog remote, int mode, int colorsArray, ICatalogStateProvider statusProvider) {
        mContext = context;
        mNoCountryIcon = context.getResources().getDrawable(R.drawable.no_country);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mStates = context.getResources().getStringArray(R.array.catalog_map_states);
		mStateColors = context.getResources().getIntArray(colorsArray);
		mStatusProvider = statusProvider;
		mMode = mode;
		
    	mObjects = CatalogMapPair.diff(local, remote, mode);
        bindData();
		bindTransportTypes();
    }

    public Object getChild(int groupPosition, int childPosition) {
        return mRefs[groupPosition][childPosition];
    }

    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    public int getChildrenCount(int groupPosition) {
        return mRefs[groupPosition].length;
    }

	public static class ViewHolder {
		TextView mCity;
		TextView mCountryISO;
		TextView mStatus;
		TextView mSize;
		ImageView mIsoIcon;
		LinearLayout mImageContainer;
		LinearLayout mCountryFlagContainer;
	}    

	public static class GroupViewHolder {
		TextView mCountry;
	}    
    
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

    	ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.catalog_expandable_list_item, null);
			holder = new ViewHolder();
			holder.mCity = (TextView) convertView.findViewById(android.R.id.text1);
			holder.mCountryISO = (TextView) convertView.findViewById(R.id.country);
			holder.mStatus = (TextView) convertView.findViewById(R.id.state);
			holder.mSize = (TextView) convertView.findViewById(R.id.size);
			holder.mIsoIcon = (ImageView) convertView.findViewById(R.id.iso_icon);
			holder.mImageContainer = (LinearLayout) convertView.findViewById(R.id.icons);
			holder.mCountryFlagContainer = (LinearLayout) convertView.findViewById(R.id.country_flag_panel);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		final String code = mLanguageCode;
		final CatalogMapPair ref = mRefs[groupPosition][childPosition];
		final int state = mStatusProvider.getCatalogState(ref.getLocal(), ref.getRemote());
		holder.mCity.setText(ref.getCity(code));
		holder.mStatus.setText(mStates[state]);
		holder.mStatus.setTextColor(mStateColors[state]);
		holder.mSize.setText( StringUtil.formatFileSize(ref.getSize(),0) );

		if(mShowCountryFlags){
			final String iso = ref.getCountryISO();
			holder.mCountryISO.setText( iso );
			Drawable d = mIcons.get(iso);
			if(d == null){
				File file = new File(Constants.ICONS_PATH, iso + ".png");
				if(file.exists()){
					d = Drawable.createFromPath(file.getAbsolutePath());
				}else{
					d = mNoCountryIcon;
				}
				mIcons.put(iso, d);
			}
			holder.mIsoIcon.setImageDrawable(d);
			holder.mCountryFlagContainer.setVisibility(View.VISIBLE);
		}else{
			holder.mCountryFlagContainer.setVisibility(View.GONE);
		}

		final LinearLayout ll = holder.mImageContainer;
		ll.removeAllViews();
		long transports = ref.getTransports();
		int transportId = 1;
		while(transports>0){
			if((transports % 2)>0){
				ImageView img = new ImageView(mContext);
				img.setImageDrawable(mTransportTypes.get(transportId));
				ll.addView( img );
			}
			transports = transports >> 1;
			transportId = transportId << 1;
		}
		
		return convertView;
		
    }

    public Object getGroup(int groupPosition) {
        return mCountries[groupPosition];
    }

    public int getGroupCount() {
        return mCountries.length;
    }

    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    	GroupViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.catalog_list_group_item, null);
			holder = new GroupViewHolder();
			holder.mCountry = (TextView) convertView.findViewById(R.id.text);
			convertView.setTag(holder);
		} else {
			holder = (GroupViewHolder) convertView.getTag();
		}    	
		holder.mCountry.setText(mCountries[groupPosition]);
		return convertView;
    }

    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public boolean hasStableIds() {
        return false;//true;
    }
    
    protected void bindTransportTypes(){
		mTransportTypes = new HashMap<Integer, Drawable>();
		final Resources res = mContext.getResources();
		mTransportTypes.put( TransportType.UNKNOWN_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.UNKNOWN_ID))  );
		mTransportTypes.put( TransportType.METRO_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.METRO_ID))  );
		mTransportTypes.put( TransportType.TRAM_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.TRAM_ID))  );
		mTransportTypes.put( TransportType.BUS_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.BUS_ID))  );
		mTransportTypes.put( TransportType.TRAIN_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.TRAIN_ID))  );
		mTransportTypes.put( TransportType.WATER_BUS_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.WATER_BUS_ID))  );
		mTransportTypes.put( TransportType.TROLLEYBUS_ID , res.getDrawable(GlobalSettings.getTransportTypeWhiteIconId(TransportType.TROLLEYBUS_ID))  );
    }
    
    protected void bindData() {
        mShowCountryFlags = GlobalSettings.isCountryIconsEnabled(mContext);
    	final String code= GlobalSettings.getLanguage(mContext); 
    	mLanguageCode = code;
        TreeSet<String> countries = new TreeSet<String>();
        TreeMap<String, ArrayList<CatalogMapPair>> index = new TreeMap<String, ArrayList<CatalogMapPair>>();
        CatalogMapPairCityComparator comparator = new CatalogMapPairCityComparator(code);

        for(CatalogMapPair diff : mObjects){
        	final String country = diff.getCountry(code);
        	countries.add(country);
        	ArrayList<CatalogMapPair> cities = index.get(country);
        	if(cities == null){
        		cities = new ArrayList<CatalogMapPair>();
        		index.put(country,cities);
        	}
        	cities.add(diff); 
        }
        mCountries = (String[]) countries.toArray(new String[countries.size()]);
        mIcons = new HashMap<String, Drawable>();
        mRefs = new CatalogMapPair[mCountries.length][];

        int lenc = mCountries.length;
        for(int i=0;i<lenc;i++){
        	final String country = mCountries[i];
        	final ArrayList<CatalogMapPair> diffSet = index.get(country);
			if(diffSet!=null){        	
	        	int len = diffSet.size();
	        	CatalogMapPair[] arr = (CatalogMapPair[]) diffSet.toArray(new CatalogMapPair[len]);
	        	Arrays.sort(arr, comparator);
	        	mRefs[i] = arr;
			}
        }
	}

    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new CatalogFilter();
        }
        return mFilter;
	}
	
    private class CatalogFilter extends Filter {

    	protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
        	mSearchPrefix = prefix.toString();
            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<CatalogMapPair>(mObjects);
                }
            }
            if (prefix == null || prefix.length() == 0) {
                synchronized (mLock) {
                    ArrayList<CatalogMapPair> list = new ArrayList<CatalogMapPair>(mOriginalValues);
                    results.values = list;
                    results.count = list.size();
                }
            } else {
                final ArrayList<CatalogMapPair> newValues = getFilteredData(prefix);
                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mObjects = (ArrayList<CatalogMapPair>) results.values;
        	bindData();
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
	
	/*package*/ ArrayList<CatalogMapPair> getFilteredData(CharSequence prefix) {
		String prefixString = prefix.toString().toLowerCase();

		final ArrayList<CatalogMapPair> values = mOriginalValues;
		final int count = values.size();
		final String code = mLanguageCode;
		final ArrayList<CatalogMapPair> newValues = new ArrayList<CatalogMapPair>(count);

		for (int i = 0; i < count; i++) {
		    final CatalogMapPair value = values.get(i);
		    final String cityName = value.getCity(code).toString().toLowerCase();
		    final String countryName = value.getCountry(code).toString().toLowerCase();

		    // First match against the whole, non-splitted value
		    if (cityName.startsWith(prefixString) || countryName.startsWith(prefixString)) {
		        newValues.add(value);
		    } else {
		    	boolean added = false;
		        final String[] cityWords = cityName.split(" ");
		        final int cityWordCount = cityWords.length;

		        for (int k = 0; k < cityWordCount; k++) {
		            if (cityWords[k].startsWith(prefixString)) {
		                newValues.add(value);
		                added = true;
		                break;
		            }
		        }
		        
		        if(!added){
			        final String[] countryWords = countryName.split(" ");
			        final int countryWordCount = countryWords.length;

			        for (int k = 0; k < countryWordCount; k++) {
			            if (countryWords[k].startsWith(prefixString)) {
			                newValues.add(value);
			                break;
			            }
			        }
		        }
		        
		    }
		}
		return newValues;
	}    
}
