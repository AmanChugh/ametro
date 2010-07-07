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
package org.ametro.directory;

import java.util.HashMap;

import org.ametro.model.ext.ModelLocation;

public class CityStationDictionary
{

	public static class Entity {
		private String mLineSystemName;
		private String mStationSystemName;
		private ModelLocation mLocation;
		
		public String getLineSystemName() {
			return mLineSystemName;
		}

		public String getStationSystemName() {
			return mStationSystemName;
		}

		public ModelLocation getLocation() {
			return mLocation;
		}

		public Entity(String lineSystemName, String stationSystemName, ModelLocation location) {
			super();
			mLineSystemName = lineSystemName;
			mStationSystemName = stationSystemName;
			mLocation = location;
		}
	}
	
	private final HashMap<String, HashMap<String,Entity>> mData;

	public CityStationDictionary(HashMap<String,HashMap<String,Entity>> index){
		mData = index;
	}

	public ModelLocation getStationLocation(String lineSystemName, String stationSystemName){
		Entity r = getRecord(lineSystemName, stationSystemName);
		if(r!=null){
			return r.getLocation();
		}
		return null;
	}

	public Entity getRecord(String lineSystemName, String stationSystemName){
		HashMap<String, Entity> city2rec = mData.get(lineSystemName);
		if(city2rec!=null){
			return city2rec.get(stationSystemName);
		}
		return null;
	}		
}