package org.ametro.model.util;

import java.util.ArrayList;
import java.util.List;

import org.ametro.model.StationView;
import org.ametro.model.ext.ModelPoint;
import org.ametro.model.ext.ModelRect;
import org.ametro.util.StringUtil;

import android.graphics.Point;
import android.graphics.Rect;


public class ModelUtil {

	public static class DelaysString {

		private String mText;
		// private String[] mParts;
		private int mPos;
		private int mLen;

		public DelaysString(String text) {
			// text = text.replaceAll("\\(","");
			// text = text.replaceAll("\\)","");
			// mParts = text.split(",");
			mText = text;
			mLen = text != null ? mText.length() : 0;
			mPos = 0;
		}

		public boolean beginBracket() {
			return mText != null && mPos < mLen && mText.charAt(mPos) == '(';
		}

		private String nextBlock() {
			if (mText == null)
				return null;
			int nextComma = mText.indexOf(",", beginBracket() ? mText.indexOf(
					")", mPos) : mPos);
			String block = nextComma != -1 ? mText.substring(mPos, nextComma)
					: mText.substring(mPos);
			mPos = nextComma != -1 ? nextComma + 1 : mLen;
			return block;
		}

		public Integer next() {
			return StringUtil.parseNullableDelay(nextBlock());
		}

		public Integer[] nextBracket() {
			if (mText == null)
				return null;
			String block = nextBlock();
			return StringUtil.parseDelayArray(block.substring(1, block.length() - 1));
		}
	}

	public static class StationsString {
		private String mText;
		private String mDelimeters;
		private int mPos;
		private int mLen;
		private String mNextDelimeter;

		public String getNextDelimeter() {
			return mNextDelimeter;
		}

		public StationsString(String text) {
			mText = text;
			mLen = text.length();
			mDelimeters = ",()";
			reset();
		}

		public void reset(){
			mPos = 0;
			skipToContent();
		}

		public boolean hasNext() {
			int saved = mPos;
			skipToContent();
			boolean result = mPos != mLen;
			mPos = saved;
			return result;
		}

		public String next() {
			skipToContent();
			if (mPos == mLen) {
				return "";
			}
			int pos = mPos;
			String symbol = null;
			boolean quotes = false;
			while (pos < mLen
					&& (!mDelimeters.contains(symbol = mText.substring(pos,
							pos + 1)) || quotes)) {
				if ("\"".equals(symbol)) {
					quotes = !quotes;
				}
				pos++;
			}
			int end = symbol == null ? pos - 1 : pos;
			mNextDelimeter = symbol;
			String text = mText.substring(mPos, end);
			mPos = end;
			if (text.startsWith("\"") && text.endsWith("\""))
				text = text.substring(1, text.length() - 1);
			return text;
		}

		private void skipToContent() {
			String symbol;
			String symbolNext = (mPos < mLen) ? mText.substring(mPos, mPos + 1)
					: null;
			while (mPos < mLen && mDelimeters.contains(symbol = symbolNext)) {
				if ("(".equals(symbol)) {
					mPos++;
					return;
				} else if (")".equals(symbol)) {
				}
				mPos++;
				symbolNext = (mPos < mLen) ? mText.substring(mPos, mPos + 1)
						: null;
				if (",".equals(symbol) && !"(".equals(symbolNext))
					return;
			}
		}
	}

	public 	static int[] toIntArray(List<Integer> src) {
		int[] res = new int[src.size()];
		for (int i = 0; i < src.size(); i++) {
			res[i] = src.get(i);
		}
		return res;
	}

	public static long[] toLongArray(ArrayList<Long> src) {
		long[] res = new long[src.size()];
		for (int i = 0; i < src.size(); i++) {
			res[i] = src.get(i);
		}
		return res;
	}
	
	public static ModelRect getDimensions(StationView stations[]) {
		int xmin = Integer.MAX_VALUE;
		int ymin = Integer.MAX_VALUE;
		int xmax = Integer.MIN_VALUE;
		int ymax = Integer.MIN_VALUE;

		for (StationView station : stations) {
			ModelPoint p = station.stationPoint;
			if (p != null) {
				if (xmin > p.x)
					xmin = p.x;
				if (ymin > p.y)
					ymin = p.y;

				if (xmax < p.x)
					xmax = p.x;
				if (ymax < p.y)
					ymax = p.y;
			}
			ModelRect r = station.stationNameRect;
			if (r != null) {
				if (xmin > r.left)
					xmin = r.left;
				if (ymin > r.top)
					ymin = r.top;
				if (xmin > r.right)
					xmin = r.right;
				if (ymin > r.bottom)
					ymin = r.bottom;

				if (xmax < r.left)
					xmax = r.left;
				if (ymax < r.top)
					ymax = r.top;
				if (xmax < r.right)
					xmax = r.right;
				if (ymax < r.bottom)
					ymax = r.bottom;
			}
		}
		return new ModelRect(xmin, ymin, xmax, ymax);
	}

	public static Rect toRect(ModelRect r) {
		return new Rect(r.left,r.top,r.right,r.bottom);
	}

	public static Point toPoint(ModelPoint p) {
		return new Point(p.x,p.y);
	}

}
