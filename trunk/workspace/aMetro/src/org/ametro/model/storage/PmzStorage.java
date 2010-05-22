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
package org.ametro.model.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ametro.R;
import org.ametro.model.LineView;
import org.ametro.model.MapLayerContainer;
import org.ametro.model.MapView;
import org.ametro.model.Model;
import org.ametro.model.SegmentView;
import org.ametro.model.TransportStationInfo;
import org.ametro.model.StationView;
import org.ametro.model.TransferView;
import org.ametro.model.TransportLine;
import org.ametro.model.TransportMap;
import org.ametro.model.TransportSegment;
import org.ametro.model.TransportStation;
import org.ametro.model.TransportTransfer;
import org.ametro.model.ext.ModelLocation;
import org.ametro.model.ext.ModelPoint;
import org.ametro.model.ext.ModelRect;
import org.ametro.model.ext.ModelSpline;
import org.ametro.model.util.CountryLibrary;
import org.ametro.model.util.IniStreamReader;
import org.ametro.model.util.ModelUtil;
import org.ametro.model.util.StationLibrary;
import org.ametro.util.StringUtil;


public class PmzStorage implements IModelStorage {

	private static final String TRANSPORT_TYPE_METRO = "Метро";
	private static final String TRANSPORT_TYPE_TRAIN = "Электричка";
	private static final String TRANSPORT_TYPE_TRAM = "Трамвай";
	private static final String TRANSPORT_TYPE_BUS = "Автобус";
	private static final String TRANSPORT_TYPE_WATER_BUS = "Речной трамвай";
	
	private static final String ENCODING = "windows-1251";

	public Model loadModel(String fileName, Locale locale) throws IOException {
		PmzImporter importer = new PmzImporter(fileName,false);
		try {
			Model model = importer.getModel();
			model.setLocale(locale);
			return model;
		} catch (IOException e) {
			return null;
		}	
	}

	public Model loadModelDescription(String fileName, Locale locale) throws IOException {
		PmzImporter importer = new PmzImporter(fileName,true);
		try {
			Model model = importer.getModel();
			model.setLocale(locale);
			return model;
		} catch (IOException e) {
			return null;
		}	
	}

	public void saveModel(String fileName, Model model) throws IOException {
		throw new NotImplementedException();
	}

	public String[] loadModelLocale(String fileName, Model model, int localeId) throws IOException {
		throw new NotImplementedException();
	}

	public MapView loadModelView(String fileName, Model model, String name) throws IOException {
		throw new NotImplementedException();
	}

	private static class PmzImporter {

		private File mFile;
		private ZipFile mZipFile;
		private Model mModel;
		private boolean mDescriptionOnly;

		private String mCityFile = null;
		private ArrayList<String> mTrpFiles = new ArrayList<String>(); 
		private ArrayList<String> mMapFiles = new ArrayList<String>(); 
		private ArrayList<String> mTxtFiles = new ArrayList<String>();
		private ArrayList<String> mImgFiles = new ArrayList<String>();

		private ArrayList<TransportMap> mTransportMaps = new ArrayList<TransportMap>();

		private ArrayList<TransportLine> mTransportLines = new ArrayList<TransportLine>();

		private ArrayList<TransportStation> mTransportStations = new ArrayList<TransportStation>();

		private ArrayList<TransportSegment> mTransportSegments = new ArrayList<TransportSegment>();
		private ArrayList<TransportTransfer> mTransportTransfers = new ArrayList<TransportTransfer>();

		private ArrayList<MapView> mMapViews = new ArrayList<MapView>();
		private ArrayList<String> mMapViewNames = new ArrayList<String>();

		private ArrayList<MapLayerContainer> mMapLayers = new ArrayList<MapLayerContainer>();
		private ArrayList<String> mMapLayerNames = new ArrayList<String>();

		private HashMap<String,TransportStation> mTransportStationIndex = new HashMap<String,TransportStation>();
		private HashMap<String,TransportLine> mTransportLineIndex = new HashMap<String, TransportLine>();
		private HashMap<String,TransportMap> mTransportMapIndex = new HashMap<String, TransportMap>();

		private ArrayList<String> mTexts = new ArrayList<String>();

		private HashMap<Integer, StationInfo> mStationInfo = new HashMap<Integer, StationInfo>();

		private int[] getMapsNumbers(String[] maps) {
			ArrayList<Integer> res = new ArrayList<Integer>();
			for(String mapSystemName : maps){
				TransportMap map = mTransportMapIndex.get(mapSystemName);
				if(map!=null){
					res.add(map.id);
				}
			}
			return ModelUtil.toIntArray(res);
		}

		private TransportStation getStation(String lineSystemName, String stationSystemName)
		{
			String key = lineSystemName + "\\" + stationSystemName;
			return mTransportStationIndex.get(key);
		}

		private void updateStationIndex(TransportLine line){
			final String lineSystemName = line.systemName;
			for(int id : line.stations){
				TransportStation station = mTransportStations.get(id); 
				String key = lineSystemName + "\\" + station.systemName;
				mTransportStationIndex.put(key, station);
			}
		}

		private int[] appendTextArray(String[] txt){
			if(txt == null) return null;
			final int len = txt.length;
			int[] r = new int[len];
			int base = mTexts.size();
			for(int i = 0; i < len; i++){
				r[i] = base + i;
				mTexts.add(txt[i]);
			}
			return r;
		}

		private int appendLocalizedText(String txt){
			int pos = mTexts.size();
			mTexts.add(txt);
			return pos;
		}

		public PmzImporter(String fileName, boolean descriptionOnly){
			mFile = new File(fileName);
			mModel = null;
			mDescriptionOnly = descriptionOnly;
		}

		public void execute() throws IOException{
			mZipFile = null;
			try{
				mZipFile = new ZipFile(mFile, ZipFile.OPEN_READ);
				mModel = new Model();
				findModelFiles(); // find map files in archive
				importCityFile(); // load data from .cty file - map description
				if(!mDescriptionOnly) { 
					importTrpFiles(); // load data from .trp files 
					importMapFiles(); // load data from .map files
					//importTxtFiles(); // load data from .txt files
				}
				postProcessModel(); // make model from imported data
			}finally{
				if(mZipFile!=null){
					mZipFile.close();
				}
			}
		}

		@SuppressWarnings("unused")
		private void importTxtFiles() throws IOException {
			Collections.sort(mTxtFiles);

			final Model model = mModel;

			for(String fileName : mTxtFiles){
				InputStream stream = mZipFile.getInputStream(mZipFile.getEntry(fileName));
				IniStreamReader ini = new IniStreamReader(new InputStreamReader(stream, ENCODING)); // access as INI file

				boolean addToStationInfo = false;
				String caption = null;
				String prefix = null;
				//String menuName = null;

				while(ini.readNext()){ 
					final String key = ini.getKey(); 
					final String value = ini.getValue();
					final String section = ini.getSection();

					if(section!=null){
						if(section.startsWith("Options")){ 
							if(key.equalsIgnoreCase("AddToInfo")){ 
								addToStationInfo = value.equalsIgnoreCase("1");
							}else if(key.equalsIgnoreCase("CityName")){
								// skip
							}else if(key.equalsIgnoreCase("MenuName")){
								//menuName = (value);
							}else if(key.equalsIgnoreCase("MenuImage")){

							}else if(key.equalsIgnoreCase("Caption")){
								caption = (value);
							}else if(key.equalsIgnoreCase("StringToAdd")){
								String txt = value.trim();
								if(txt.length() > 0){
									if(txt.startsWith("'") && txt.endsWith("'")){
										txt = txt.substring(1, txt.length()-1 ).trim();
									}
									prefix = (txt);
								}
							}			
						}else{
							if(addToStationInfo){
								makeStationInfo(caption, prefix, key, value, section);
							}
						}
					}
				}
			}

			TransportStationInfo[] lst = new TransportStationInfo[mTransportStations.size()];
			for(TransportStation station : mTransportStations){
				StationInfo src = mStationInfo.get(station.id);
				if(src!=null){
					TransportStationInfo info = new TransportStationInfo();
					String[] captions = (String[]) src.captions.toArray(new String[src.captions.size()]);
					info.captions = appendTextArray(captions);
					int len = captions.length;
					int[][] lines = new int[len][];
					for(int i = 0; i<len; i++){
						String caption = captions[i];
						ArrayList<String> textLines = src.data.get(caption);
						if(textLines!=null){
							String[] textLinesArray = (String[]) textLines.toArray(new String[textLines.size()]);
							lines[i] = appendTextArray(textLinesArray);
						}else{
							lines[i] = null;
						}
					}
					info.lines = lines;
					lst[station.id] = info;
				}
			}
			model.stationInfos = lst;

		}

		private void makeStationInfo(String caption, String prefix,
				final String key, final String value, final String section) {
			TransportStation station = getStation(section, key);
			if(station!=null){
				StationInfo info = mStationInfo.get(station.id);
				if(info==null){
					info = new StationInfo();
					mStationInfo.put(station.id, info);
				}
				ArrayList<String> lines = info.data.get(caption);
				if(lines == null){
					lines = new ArrayList<String>();
					info.captions.add(caption);
					info.data.put(caption, lines);
				}
				String[] textLines = StringUtil.fastSplit( value.replace("\\n",";"), ';' );
				for(String textLine : textLines){
					lines.add(prefix + textLine);
				}
			}
		}

		private void importMapFiles() throws IOException {
			final Model model = mModel;

			final ArrayList<LineView> lines = new ArrayList<LineView>();
			final ArrayList<StationView> stations = new ArrayList<StationView>();
			final HashMap<Long, ModelSpline> additionalNodes = new HashMap<Long, ModelSpline>();
			final HashMap<Integer,Integer> stationViews = new HashMap<Integer, Integer>();
			final HashMap<Integer,Integer> lineViewIndex = new HashMap<Integer, Integer>();

			final HashMap<String, LineView> viewsDefaults = new HashMap<String, LineView>();

			for(String fileName : mMapFiles){ // for each .map file in catalog
				InputStream stream = mZipFile.getInputStream(mZipFile.getEntry(fileName));
				IniStreamReader ini = new IniStreamReader(new InputStreamReader(stream, ENCODING)); // access as INI file

				MapView view = new MapView();
				view.id = mMapViews.size();
				view.systemName = fileName;
				view.stationDiameter = 11;
				view.lineWidth = 9;
				view.isUpperCase = true;
				view.isWordWrap = true;
				view.isVector = true;
				view.owner = model;
				mMapViews.add(view);
				mMapViewNames.add(view.systemName);

				TransportLine line = null;
				LineView lineView = null;

				lines.clear();
				stations.clear();
				additionalNodes.clear();
				stationViews.clear();
				lineViewIndex.clear();

				ModelPoint[] coords = null;
				ModelRect[] rects = null;
				Integer[] heights = null;

				while(ini.readNext()){ // for each key in file
					final String key = ini.getKey(); // extract current properties
					final String value = ini.getValue();
					final String section = ini.getSection();
					final boolean isSectionChanged = ini.isSectionChanged(); 

					if(lineView!=null && isSectionChanged){
						makeStationViews(line, lineView, stationViews, stations, coords, rects, heights);
						lineView = null;
						line = null;
						coords = null;
						rects = null;
						heights = null;						
					}

					if(section.startsWith("Options")){ // for line sections
						if(key.equalsIgnoreCase("ImageFileName")){ // store line name parameter
							view.backgroundSystemName = value;
						}else if(key.equalsIgnoreCase("StationDiameter")){
							view.stationDiameter = StringUtil.parseInt(value, view.stationDiameter);
						}else if(key.equalsIgnoreCase("LinesWidth")){
							view.lineWidth = StringUtil.parseInt(value, view.lineWidth);
						}else if(key.equalsIgnoreCase("UpperCase")){
							view.isUpperCase = StringUtil.parseBoolean(value, view.isUpperCase);
						}else if(key.equalsIgnoreCase("WordWrap")){
							view.isWordWrap = StringUtil.parseBoolean(value, view.isWordWrap);
						}else if(key.equalsIgnoreCase("IsVector")){
							view.isVector = StringUtil.parseBoolean(value, view.isVector);
						}else if(key.equalsIgnoreCase("Transports")){
							view.transports = getMapsNumbers(StringUtil.parseStringArray(value));
						}else if(key.equalsIgnoreCase("CheckedTransports")){
							view.transportsChecked = getMapsNumbers(StringUtil.parseStringArray(value));
						}			
					}else if(section.equalsIgnoreCase("AdditionalNodes")){
						makeAdditionalNodes(additionalNodes, stationViews, value);
					}else{
						if(isSectionChanged){
							line = mTransportLineIndex.get(section);
							if(line!=null){
								lineView = new LineView();

								LineView def = viewsDefaults.get(section);
								if(def == null){
									viewsDefaults.put(section, lineView);
								}else{
									lineView.labelColor = def.labelColor;
									lineView.lineColor = def.lineColor;
								}
								lineView.id = lines.size();
								lineView.lineId = line.id;
								lineView.owner = model;
								lines.add(lineView);
								lineViewIndex.put(line.id, lineView.id);


							}
						}
						if(lineView!=null){					
							if(key.equalsIgnoreCase("Color")){ // store line name parameter
								lineView.lineColor = StringUtil.parseColor(value);
							}else if(key.equalsIgnoreCase("LabelsColor")){
								lineView.labelColor = StringUtil.parseColor(value);
							}else if(key.equalsIgnoreCase("Coordinates")){
								coords = StringUtil.parseModelPointArray(value);
							}else if(key.equalsIgnoreCase("Rects")){
								rects = StringUtil.parsePmzModelRectArray(value);
							}else if(key.equalsIgnoreCase("Heights")){
								heights = StringUtil.parseIntegerArray(value);
							}else if(key.equalsIgnoreCase("Rect")){
								lineView.lineNameRect = StringUtil.parseModelRect(value);
							}			
						}

					}

				}
				// finalize map view
				if(lineView!=null){
					makeStationViews(line, lineView, stationViews, stations, coords, rects, heights);
				}
				view.lines = (LineView[]) lines.toArray(new LineView[lines.size()]);
				view.stations = (StationView[]) stations.toArray(new StationView[lines.size()]);
				view.segments = makeSegmentViews(view, lineViewIndex, stationViews, additionalNodes);
				view.transfers = makeTransferViews(view, stationViews);

				fixViewDimensions(view);
			}

		}

		private void makeAdditionalNodes(
				final HashMap<Long, ModelSpline> additionalNodes,
				final HashMap<Integer, Integer> stationViews, final String value) {
			String[] parts = StringUtil.parseStringArray(value);
			String lineSystemName = parts[0];
			TransportStation from = getStation(lineSystemName, parts[1]);
			TransportStation to = getStation(lineSystemName, parts[2]);
			if(from!=null && to!=null){
				boolean isSpline = false;
				int pos = 3;
				final ArrayList<ModelPoint> points = new ArrayList<ModelPoint>();
				while (pos < parts.length) {
					if (parts[pos].contains("spline")) {
						isSpline = true;
						break;
					} else {
						ModelPoint p = new ModelPoint(StringUtil.parseInt(parts[pos].trim(),0), StringUtil.parseInt(parts[pos + 1].trim(),0) );
						points.add(p);
						pos += 2;
					}
				}
				final ModelSpline spline = new ModelSpline();
				spline.isSpline = isSpline;
				spline.points = (ModelPoint[]) points.toArray(new ModelPoint[points.size()]);
				additionalNodes.put(Model.getSegmentKey(from.id, to.id), spline);
			}
		}


		private SegmentView[] makeSegmentViews(MapView view, HashMap<Integer,Integer> lineViewIndex, HashMap<Integer,Integer> stationViewIndex, HashMap<Long,ModelSpline> additionalNodes) {
			final Model model = mModel;
			final ArrayList<SegmentView> segments = new ArrayList<SegmentView>();
			int base = 0;
			for(TransportSegment segment: mTransportSegments){
				Integer fromId = stationViewIndex.get(segment.stationFromId);
				Integer toId = stationViewIndex.get(segment.stationToId);
				boolean visibleStations = fromId!=null && toId!=null;
				if(!visibleStations || ((segment.flags & TransportSegment.TYPE_INVISIBLE) != 0)){
					continue;
				}
				final ModelSpline spline = additionalNodes.get( Model.getSegmentKey(segment.stationFromId, segment.stationToId) );
				if(spline!=null && spline.isZero()){
					continue;
				}else{
					final ModelSpline opposite = additionalNodes.get( Model.getSegmentKey(segment.stationToId, segment.stationFromId) );
					if(opposite!=null && opposite.isZero()){
						continue;
					}
				}

				final SegmentView segmentView = new SegmentView();
				segmentView.id = base++;
				segmentView.lineViewId = lineViewIndex.get(segment.lineId);
				segmentView.segmentId = segment.id;
				segmentView.stationViewFromId = fromId;
				segmentView.stationViewToId = toId;

				segmentView.owner = model;


				if(spline!=null){
					segmentView.spline = spline;
				}else{
					segmentView.spline = null;
				}
				segments.add(segmentView);
			}
			return (SegmentView[]) segments.toArray(new SegmentView[segments.size()]);
		}

		private void fixViewDimensions(MapView view) {
			ModelRect mapRect = ModelUtil.getDimensions(view.stations);

			int xmin = mapRect.left;
			int ymin = mapRect.top;
			int xmax = mapRect.right;
			int ymax = mapRect.bottom;

			int dx = 80 - xmin;
			int dy = 80 - ymin;

			for (StationView station : view.stations) {
				ModelPoint p = station.stationPoint;
				if (p != null && !p.isZero() ) {
					p.offset(dx, dy);
				}
				ModelRect r = station.stationNameRect;
				if (r != null && !r.isZero() ) {
					r.offset(dx, dy);
				}
			}
			for (SegmentView segment : view.segments) {
				ModelSpline spline = segment.spline;
				if (spline != null) {
					ModelPoint[] points = spline.points;
					for (ModelPoint point : points) {
						point.offset(dx, dy);
					}
				}

			}
			view.width = xmax - xmin + 160;
			view.height = ymax - ymin + 160;
		}

		private TransferView[] makeTransferViews(MapView view, HashMap<Integer,Integer> stationViewIndex) {
			final Model model = mModel;
			final ArrayList<TransferView> transfers = new ArrayList<TransferView>();
			int base = 0;
			for(TransportTransfer transfer: mTransportTransfers){
				Integer fromId = stationViewIndex.get(transfer.stationFromId);
				Integer toId = stationViewIndex.get(transfer.stationToId);
				if(fromId!=null && toId!=null){
					final TransferView v = new TransferView();
					v.id = base++;
					v.transferId = transfer.id;
					v.stationViewFromId = fromId;
					v.stationViewToId = toId;
					v.owner = model;
					transfers.add(v);
				}
			}
			return (TransferView[]) transfers.toArray(new TransferView[transfers.size()]);	
		}

		private void makeStationViews(TransportLine line, LineView lineView, HashMap<Integer,Integer> stationViewIndex, ArrayList<StationView> stationViews, ModelPoint[] coords, ModelRect[] rects, Integer[] heights) {
			final int stationsCount = line.stations.length;
			final int pointsCount = coords!=null ? coords.length : 0;
			final int rectsCount = rects!=null ? rects.length : 0;
			final int heightsCount = heights!=null ? heights.length : 0;
			final int[] stations = line.stations;
			int base = stationViews.size();

			for(int i = 0; i < stationsCount && i < pointsCount; i++){
				if(ModelPoint.isNullOrZero( coords[i] )) { 
					continue; // skip station with ZERO coordinates!
				}
				final StationView v = new StationView();
				v.id = base++;
				v.owner = mModel;
				v.lineViewId = lineView.id;
				v.stationId = stations[i];
				v.stationPoint = coords[i];
				v.stationNameRect = (i < rectsCount) ? rects[i] : null;
				v.stationHeight = (i < heightsCount) ? heights[i] : null;
				stationViews.add(v);
				stationViewIndex.put(v.stationId, v.id);
			}


		}

		private void importTrpFiles() throws IOException {
			for(String fileName : mTrpFiles){ // for each .trp file in catalog
				InputStream stream = mZipFile.getInputStream(mZipFile.getEntry(fileName));
				IniStreamReader ini = new IniStreamReader(new InputStreamReader(stream, ENCODING)); // access as INI file

				TransportMap map = new TransportMap(); // create new transport map
				map.id = mTransportMaps.size();
				map.owner = mModel;
				mTransportMaps.add(map);
				map.systemName = fileName; // setup transport map internal name
				mTransportMapIndex.put(map.systemName, map);

				TransportLine line = null; // create loop variables
				String stationList = null; // storing driving data
				String aliasesList = null;
				String drivingList = null; // for single line
				String lineName = null;

				while(ini.readNext()){ // for each key in file
					final String key = ini.getKey(); // extract current properties
					final String value = ini.getValue();
					final String section = ini.getSection();
					final boolean isSectionChanged = ini.isSectionChanged(); 

					if(line!=null && isSectionChanged){ // if end of line
						line.name  = appendLocalizedText(lineName);
						makeLineObjects(line, stationList, drivingList, aliasesList); // make station and segments
						line = null; // clear loop variables
						stationList = null;
						drivingList = null;
						aliasesList = null;
						lineName = null;
					}
					if(section.startsWith("Line")){ // for line sections
						if(isSectionChanged){
							line = new TransportLine(); // at start - create new line object
							line.id = mTransportLines.size();
							line.mapId = map.id;
							line.owner = mModel;
							mTransportLines.add(line);
						}
						if(key.equalsIgnoreCase("Name")){ // store line name parameter
							if(lineName==null){
								lineName = value;
							}
							line.systemName = value;
						}else if(key.equalsIgnoreCase("LineMap")){
							line.lineMapName = value;
						}else if(key.equalsIgnoreCase("Stations")){
							stationList = value;
						}else if(key.equalsIgnoreCase("Driving")){
							drivingList = value;
						}else if(key.equalsIgnoreCase("Delays")){
							line.delays = StringUtil.parseDelayArray(value);
						}else if(key.equalsIgnoreCase("Alias")){
							lineName = value;
						}else if(key.equalsIgnoreCase("Aliases")){
							lineName = value;
						}			

					}else if(section.equalsIgnoreCase("Transfers")){
						makeTransfer(value);
					}else if (section.equalsIgnoreCase("AdditionalInfo")){
						// do nothing at this time
					}else if (section.equalsIgnoreCase("Options")){
						if(key.equalsIgnoreCase("Type")){
							if(TRANSPORT_TYPE_METRO.equalsIgnoreCase(value)){
								map.typeName = R.string.transport_type_metro;
							}else if(TRANSPORT_TYPE_TRAM.equalsIgnoreCase(value)){
								map.typeName = R.string.transport_type_tram;
							}else if(TRANSPORT_TYPE_BUS.equalsIgnoreCase(value)){
								map.typeName = R.string.transport_type_bus;
							}else if(TRANSPORT_TYPE_TRAIN.equalsIgnoreCase(value)){
								map.typeName = R.string.transport_type_train;
							}else if(TRANSPORT_TYPE_WATER_BUS.equalsIgnoreCase(value)){
								map.typeName = R.string.transport_type_water_bus;
							}else{
								map.typeName = R.string.transport_type_default;
							}
						}
					}
				}
				
				if(map.typeName == 0){
					map.typeName = R.string.transport_type_default;
				}
				
				if(line!=null){ // if end of line 
					makeLineObjects(line, stationList, drivingList, aliasesList); // make station and segments
				}

			}
		}


		private void importCityFile() throws IOException {
			String city = null;
			String country = null;
			final ArrayList<String> authors = new ArrayList<String>();
			final ArrayList<String> comments = new ArrayList<String>();
			String[] delays = null;

			InputStream stream = mZipFile.getInputStream(mZipFile.getEntry(mCityFile));
			final IniStreamReader ini = new IniStreamReader(new InputStreamReader(stream, ENCODING));
			while(ini.readNext()){
				final String key = ini.getKey();
				final String value = ini.getValue();
				if(key.equalsIgnoreCase("RusName")){
					city = value;
				}else if(key.equalsIgnoreCase("Country")){
					country = value;
				}else if(key.equalsIgnoreCase("MapAuthors")){
					authors.add(value);
				}else if(key.equalsIgnoreCase("Comment")){
					comments.add(value);
				}else if(key.equalsIgnoreCase("DelayNames")){
					delays = value.split(",");
				}
			}
			final Model m = mModel;
			m.cityName = appendLocalizedText(city);
			m.countryName = appendLocalizedText(country);
			m.authors = appendTextArray((String[]) authors.toArray(new String[authors.size()]));
			m.comments = appendTextArray((String[]) comments.toArray(new String[comments.size()]));
			m.delays = appendTextArray(delays);
			m.textLengthDescription = mTexts.size();
		}


		private void postProcessModel() {
			final Model model = mModel;
			// fill model fields
			model.maps = (TransportMap[]) mTransportMaps.toArray(new TransportMap[mTransportMaps.size()]);
			model.lines = (TransportLine[]) mTransportLines.toArray(new TransportLine[mTransportLines.size()]);
			model.stations = (TransportStation[]) mTransportStations.toArray(new TransportStation[mTransportStations.size()]);
			model.segments = (TransportSegment[]) mTransportSegments.toArray(new TransportSegment[mTransportSegments.size()]);
			model.transfers = (TransportTransfer[]) mTransportTransfers.toArray(new TransportTransfer[mTransportTransfers.size()]);

			model.views = (MapView[]) mMapViews.toArray(new MapView[mMapViews.size()]);
			model.viewNames = (String[]) mMapViewNames.toArray(new String[mMapViewNames.size()]);

			model.layers = (MapLayerContainer[]) mMapLayers.toArray(new MapLayerContainer[mMapLayers.size()]);
			model.layerNames = (String[]) mMapLayerNames.toArray(new String[mMapLayerNames.size()]);

			model.systemName = mFile.getName();

			model.fileSystemName = mFile.getAbsolutePath();
			model.timestamp = mFile.lastModified();

			makeGlobalization();

			if(!mDescriptionOnly){
				StationLibrary lib = StationLibrary.load(mFile);
				if(lib!=null){
					for(TransportStation station : model.stations){
						String lineSystemName = model.lines[station.lineId].systemName;
						String stationSystemName = station.systemName;
						ModelLocation l = lib.getStationLocation(lineSystemName, stationSystemName);
						if(l!=null){
							station.location = l;
						}
					}
				}
			}
		}

		private void makeGlobalization() {
			// prepare 
			final ArrayList<String> localeList = new ArrayList<String>();
			final ArrayList<String[]> textList = new ArrayList<String[]>();
			final String[] originalTexts = mTexts.toArray(new String[mTexts.size()]);
			final String originalLocale = determineLocale(originalTexts);

			// locate country info
			final String country = originalTexts[mModel.countryName];
			final String city = originalTexts[mModel.cityName];
			CountryLibrary.CountryLibraryRecord info = CountryLibrary.search(country,city);
			mModel.location = info!=null ? info.Location : null;

			// make localization
			if(originalLocale.equals(Model.LOCALE_RU)){
				localeList.add(originalLocale);
				textList.add(originalTexts);
				localeList.add(Model.LOCALE_EN);
				textList.add(makeTransliteText(info,originalTexts, true));
			}
			if(originalLocale.equals(Model.LOCALE_EN)){
				// localize description fields
				localeList.add(originalLocale);
				textList.add(makeTransliteText(info,originalTexts, false));
				localeList.add(Model.LOCALE_RU);
				textList.add(originalTexts);

			}

			// setup model
			final Model model = mModel;
			model.locales = (String[]) localeList.toArray(new String[localeList.size()]);
			model.localeTexts = (String[][]) textList.toArray(new String[textList.size()][]);
			model.localeCurrent = model.locales[0];
			model.texts = model.localeTexts[0];
			model.textLength = model.localeTexts[0].length;
		}

		private String[] makeTransliteText(CountryLibrary.CountryLibraryRecord info, final String[] originalTexts, boolean transliterate) { 
			final int len = originalTexts.length;
			final String[] translitTexts = new String[len];
			if(info!=null){
				translitTexts[mModel.countryName] = info.CountryNameEn;
				translitTexts[mModel.cityName] = info.CityNameEn;
			}
			for(int i=0; i<len; i++){
				if(translitTexts[i] == null){
					translitTexts[i] = transliterate ?  StringUtil.toTranslit(originalTexts[i]) : originalTexts[i];
				}
			}
			return translitTexts;
		}

		private String determineLocale(String[] originalTexts) {
			int low = 0;
			int high = 0;
			for(String txt : originalTexts){
				final int len = txt.length();
				for(int i = 0; i<len; i++){
					char ch = txt.charAt(i);
					if( ch >= 128 ){
						high++;
					}else{
						low++;
					}
				}
			}
			String originalLocale = low>high ? Model.LOCALE_EN : Model.LOCALE_RU;
			return originalLocale;
		}

		private void makeLineObjects(TransportLine line, String stationList, String drivingList, String aliasesList) {

			final int mapId = line.mapId;
			
			ArrayList<String> stations = new ArrayList<String>();
			HashSet<SegmentInfo> segments = new HashSet<SegmentInfo>();
			makeDrivingGraph(stationList, drivingList, stations, segments);

			final HashMap<String, String> aliases = makeAliasDictionary(aliasesList);		

			// create stations
			final HashMap<String, Integer> localStationIndex = new HashMap<String, Integer>();
			final int[] stationNumbers = new int[stations.size()]; 
			int index = mTransportStations.size();
			int number = 0;
			for(String stationSystemName : stations){
				final String alias = aliases.get(stationSystemName);
				final TransportStation station = new TransportStation();
				final int id = index++;
				station.id = id;
				station.lineId = line.id;
				station.mapId = mapId;
				station.systemName = stationSystemName;
				station.name = appendLocalizedText( alias!=null ? alias : stationSystemName );

				station.owner = mModel;

				mTransportStations.add(station);
				localStationIndex.put(stationSystemName, id);
				stationNumbers[number] = id;
				number++;
			}
			line.stations = stationNumbers;

			index = mTransportSegments.size();
			for(SegmentInfo si : segments){
				final Integer fromId = localStationIndex.get(si.from);
				final Integer toId = localStationIndex.get(si.to);
				final Integer delay = si.delay;

				if(fromId != null && toId != null){
					final TransportSegment segment = new TransportSegment();
					segment.id = index++;
					segment.mapId = mapId;
					segment.stationFromId = fromId;
					segment.stationToId = toId;
					segment.delay = delay;
					segment.lineId = line.id;
					segment.flags = 0;
					segment.owner = mModel;

					mTransportSegments.add(segment);
				}else{
					System.out.println(si);
				}
			}

			updateStationIndex(line);
			mTransportLineIndex.put(line.systemName, line);
		}

		private HashMap<String, String> makeAliasDictionary(String aliasesList) {
			// fill aliases table
			final HashMap<String,String> aliases = new HashMap<String, String>();
			if(aliasesList!=null){
				// build aliases table
				String[] aliasesTable = StringUtil.parseStringArray(aliasesList);
				// append new station names
				final int len = ( aliasesTable.length / 2 );
				for(int i = 0; i < len; i++){
					final int idx = i * 2;
					final String name = aliasesTable[idx];
					final String displayName = aliasesTable[idx+1];
					aliases.put(name, displayName);
				}
			}
			return aliases;
		}

		private void makeDrivingGraph(String stationList, String drivingList, 
				ArrayList<String> stations, HashSet<SegmentInfo> segments) {

			ModelUtil.StationsString tStations = new ModelUtil.StationsString(stationList);
			ModelUtil.DelaysString tDelays = new ModelUtil.DelaysString(drivingList);

			String toStation;
			Integer toDelay;

			String fromStation = null;
			Integer fromDelay = null;

			String thisStation = tStations.next();
			stations.add(thisStation);

			do {
				if ("(".equals(tStations.getNextDelimeter())) {
					int idx = 0;
					Integer[] delays = tDelays.nextBracket();
					while (tStations.hasNext() && !")".equals(tStations.getNextDelimeter())) {
						boolean isForwardDirection = true;
						String bracketedStationName = tStations.next();
						if (bracketedStationName.startsWith("-")) {
							bracketedStationName = bracketedStationName.substring(1);
							isForwardDirection = !isForwardDirection;
						}

						if (bracketedStationName != null && bracketedStationName.length() > 0) {
							String bracketedStation = bracketedStationName;
							if (isForwardDirection) {
								segments.add(new SegmentInfo(thisStation, bracketedStation, delays.length <= idx ? null : delays[idx]));

							} else {
								segments.add(new SegmentInfo(bracketedStation, thisStation, delays.length <= idx ? null : delays[idx]));
							}
						}
						idx++;
					}

					fromStation = thisStation;

					fromDelay = null;
					toDelay = null;

					if (!tStations.hasNext()) {
						break;
					}

					thisStation = tStations.next();
					stations.add(thisStation);

				} else {

					toStation = tStations.next();
					stations.add(toStation);

					if (tDelays.beginBracket()) {
						Integer[] delays = tDelays.nextBracket();
						toDelay = delays[0];
						fromDelay = delays[1];
					} else {
						toDelay = tDelays.next();
					}

					SegmentInfo this2from = new SegmentInfo(thisStation, fromStation, fromDelay);
					SegmentInfo this2to = new SegmentInfo(thisStation, toStation, toDelay);

					if (fromStation != null && !segments.contains(this2from) ) {
						if (fromDelay == null) {
							final SegmentInfo opposite = new SegmentInfo(fromStation, thisStation, null);
							for(SegmentInfo si : segments){
								if(opposite.equals(si)){
									this2from.delay = si.delay;
								}
							}
						} 
						segments.add(this2from);
					}

					if (toStation != null && !segments.contains(this2to)) {
						segments.add(this2to);
					}

					fromStation = thisStation;

					fromDelay = toDelay;
					toDelay = null;

					thisStation = toStation;
					toStation = null;

					if(!tStations.hasNext()){
						this2from = new SegmentInfo(thisStation, fromStation, fromDelay);

						if (fromStation != null && !segments.contains(this2from) ) {
							if (fromDelay == null) {
								final SegmentInfo opposite = new SegmentInfo(fromStation, thisStation, null);
								for(SegmentInfo si : segments){
									if(opposite.equals(si)){
										this2from.delay = si.delay;
									}
								}
							}
							segments.add(this2from);
						}					

					}
				}

			} while (tStations.hasNext());
		}

		private void makeTransfer(final String value) {
			String[] parts = StringUtil.parseStringArray(value);
			String startLine = parts[0].trim();
			String startStation = parts[1].trim();
			String endLine = parts[2].trim();
			String endStation = parts[3].trim();

			TransportStation from = getStation(startLine, startStation);
			TransportStation to = getStation(endLine, endStation);

			if(from!=null && to!=null){
				TransportTransfer transfer = new TransportTransfer();
				transfer.mapFromId = from.mapId;
				transfer.lineFromId = from.lineId;
				transfer.stationFromId = from.id;
				transfer.mapToId = to.mapId;
				transfer.lineToId = to.lineId;
				transfer.stationToId = to.id; 
				transfer.delay = parts.length > 4 && parts[4].length() > 0 ? StringUtil.parseNullableDelay(parts[4]) : null;
				transfer.flags = parts.length > 5 && parts[5].indexOf(TransportTransfer.INVISIBLE)!=-1 ? TransportTransfer.TYPE_INVISIBLE : 0;
				transfer.id = mTransportTransfers.size();
				transfer.owner = mModel;
				mTransportTransfers.add(transfer);
			}
		}

		private void findModelFiles() {
			final ArrayList<String> trpFiles = mTrpFiles; 
			final ArrayList<String> mapFiles = mMapFiles;
			final ArrayList<String> txtFiles = mTxtFiles;
			final ArrayList<String> imgFiles = mImgFiles;

			Enumeration<? extends ZipEntry> entries = mZipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) continue;
				final String name = entry.getName();
				if(name.endsWith(".map")){
					if(!name.equalsIgnoreCase("metro.map")){
						mapFiles.add(name);	                	
					}else{
						mapFiles.add(0,name);
					}
				}else if(name.endsWith(".trp")){
					if(!name.equalsIgnoreCase("metro.trp")){
						trpFiles.add(name);	                	
					}else{
						trpFiles.add(0,name);
					}
				}else if(name.endsWith(".txt")){
					txtFiles.add(name);	                	

				}else if(name.endsWith(".vec") || name.endsWith(".gif")|| name.endsWith(".png")|| name.endsWith(".bmp")){
					imgFiles.add(name);
				}else if(name.endsWith(".cty")){
					mCityFile = name;
					if(mDescriptionOnly) return;
				}
			}
		}

		public Model getModel() throws IOException{
			if(mModel==null){
				execute();
			}
			return mModel;
		}

	}

	private static class StationInfo
	{
		public ArrayList<String> captions = new ArrayList<String>();
		public HashMap<String, ArrayList<String>> data = new HashMap<String, ArrayList<String>>();
	}

	private static class SegmentInfo
	{
		public String from;
		public String to;
		public Integer delay;

		public SegmentInfo(String from, String to, Integer delay){
			this.from = from;
			this.to = to;
			this.delay = delay;
		}

		public boolean equals(Object obj) {
			SegmentInfo o = (SegmentInfo)obj;
			return from.equals(o.from) && to.equals(o.to);
		}

		public int hashCode() {
			return from.hashCode() + to.hashCode();
		}

		public String toString() {
			return "[FROM:" + from + ";TO:" + to + ";DELAY:" + delay + "]";
		}

	}

}

