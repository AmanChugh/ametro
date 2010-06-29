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
package org.ametro.catalog.storage;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Stack;

import org.ametro.catalog.Catalog;
import org.ametro.catalog.CatalogMap;
import org.ametro.util.StringUtil;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class CatalogDeserializer {

	public static String TAG_CATALOG = "catalog";
	public static String TAG_MAP = "map";
	public static String TAG_LOCALE = "locale";
	public static String TAG_COUNTRY = "country";
	public static String TAG_CITY = "city";
	public static String TAG_DESCRIPTION = "description";
	public static String TAG_CHANGE_LOG = "changelog";
	
	public static String ATTR_URL = "url";
	public static String ATTR_SYSTEM_NAME = "name";
	public static String ATTR_LAST_MODIFIED = "lastModified";
	public static String ATTR_TRANSPORTS = "transports";
	public static String ATTR_VERSION = "version";
	public static String ATTR_CODE = "code";
	public static String ATTR_SIZE = "size";
	public static String ATTR_MIN_VERSION = "minVersion";
	public static String ATTR_CORRUPTED = "corrupted";
	
	public static Catalog deserializeCatalog(BufferedInputStream stream) throws IOException, XmlPullParserException
	{
		ArrayList<CatalogMap> maps = new ArrayList<CatalogMap>();

		String systemName = null;
		String url = null;
		long lastModified = 0;
		long transports = 0;
		long version = 0;
		long size = 0;
		String minVersion = null;
		boolean corrupted = false;
		
		ArrayList<String> mapLocales = new ArrayList<String>();
		
		ArrayList<String> mapCity = new ArrayList<String>();
		ArrayList<String> mapCountry = new ArrayList<String>();
		ArrayList<String> mapDescription = new ArrayList<String>();
		ArrayList<String> mapChangeLog = new ArrayList<String>();
		
		Stack<String> tags = new Stack<String>(); 
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser xpp = factory.newPullParser();
		xpp.setInput( new InputStreamReader( stream ) );
		int eventType = xpp.getEventType();
		
		Catalog catalog = new Catalog();
		
		String tagName = null;
		while (eventType != XmlPullParser.END_DOCUMENT) {
		 if(eventType == XmlPullParser.START_DOCUMENT) {
		     // do nothing ^_^
		 } else if(eventType == XmlPullParser.END_DOCUMENT) {
		     // do nothing ^_^
		 } else if(eventType == XmlPullParser.START_TAG) {
		     tagName = xpp.getName();
		     tags.push(tagName);
		     if(TAG_CATALOG.equals(tagName)){
		    	 catalog.setTimestamp( StringUtil.parseLong(xpp.getAttributeValue("", ATTR_LAST_MODIFIED),0) );
		    	 catalog.setBaseUrl( xpp.getAttributeValue(null, ATTR_URL) );
		     }else if(TAG_MAP.equals(tagName)){
		    	 systemName = xpp.getAttributeValue(null, ATTR_SYSTEM_NAME);
		    	 url = xpp.getAttributeValue(null, ATTR_URL);
		    	 lastModified = StringUtil.parseLong(xpp.getAttributeValue("", ATTR_LAST_MODIFIED),0);
		    	 transports = StringUtil.parseLong(xpp.getAttributeValue("", ATTR_TRANSPORTS),0); 
		    	 version = StringUtil.parseLong(xpp.getAttributeValue("", ATTR_VERSION),0);
		    	 size = StringUtil.parseLong(xpp.getAttributeValue("", ATTR_SIZE),0);
		    	 minVersion = xpp.getAttributeValue("", ATTR_MIN_VERSION);
		    	 corrupted = StringUtil.parseBoolean(xpp.getAttributeValue("", ATTR_CORRUPTED),false);
		     }else if(TAG_LOCALE.equals(tagName)){
		    	 mapLocales.add( xpp.getAttributeValue("", ATTR_CODE) );
		     }
		 } else if(eventType == XmlPullParser.END_TAG) {
		     if(TAG_MAP.equals(tags.peek())){
		    	 while(mapChangeLog.size()<mapLocales.size()){
		    		 mapChangeLog.add("");
		    	 }
		    	 while(mapDescription.size()<mapLocales.size()){
		    		 mapDescription.add("");
		    	 }
		    	 CatalogMap map = new CatalogMap(
		    			 catalog,
		    			 systemName,
		    			 url,
		    			 lastModified,
		    			 transports,
		    			 version,
		    			 size,
		    			 minVersion,
		    			 (String[]) mapLocales.toArray(new String[mapLocales.size()]),
		    			 (String[]) mapCountry.toArray(new String[mapCountry.size()]),
		    			 (String[]) mapCity.toArray(new String[mapCity.size()]),
		    			 (String[]) mapDescription.toArray(new String[mapDescription.size()]),
		    			 (String[]) mapChangeLog.toArray(new String[mapChangeLog.size()]),
		    			 corrupted
		    			 );
		    	 mapLocales.clear();
		    	 mapCity.clear();
		    	 mapCountry.clear();
		    	 mapDescription.clear();
		    	 maps.add(map);
		     }
		     tags.pop();
		 } else if(eventType == XmlPullParser.TEXT && !xpp.isWhitespace()) {
		     if(TAG_COUNTRY.equals(tagName)){
		    	 mapCountry.add( xpp.getText() );
		     }else if (TAG_CITY.equals(tagName)){
		    	 mapCity.add( xpp.getText() );
		     }else if(TAG_DESCRIPTION.equals(tagName)){
		    	 mapDescription.add( xpp.getText() );
		     }else if(TAG_CHANGE_LOG.equals(tagName)){
		    	 mapChangeLog.add( xpp.getText() );
		     }
		 }
		 eventType = xpp.next();
		}
		catalog.setMaps(maps);
		return catalog;
		
	}
	
}
