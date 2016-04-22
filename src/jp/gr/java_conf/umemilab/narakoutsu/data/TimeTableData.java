package jp.gr.java_conf.umemilab.narakoutsu.data;

import java.util.ArrayList;
import java.util.List;

public class TimeTableData {
	private final String platform;
	private final String destination;
	private final String fare;
	private final String time;
	private final String transit;
	private final List<List<String>> hourList;
	
	public static class Builder {
		private String platform = "";
		private String destination = "";
		private String fare = "";
		private String time = "";
		private String transit = "";
		private List<List<String>> hourList = new ArrayList<List<String>>();
		
		public Builder() {};
		
		public Builder platform(String val) {
			platform = val; return this;
		}
		public Builder destination(String val) {
			destination = val; return this;
		}
		public Builder fare(String val) {
			fare = val; return this;
		}
		public Builder time(String val) {
			time = val; return this;
		}
		public Builder transit(String val) {
			transit = val; return this;
		}
		/**
		 * 
		 * @param val	[in] 出発時分の文字列配列
		 * @return
		 */
		public Builder addHour(String[] val) {
			List<String> set = new ArrayList<String>();
			for( String s : val ) {
				if ( !s.equals("") ) {
					set.add(s);
				}
			}
			hourList.add(set); 
			return this;
		}
		public TimeTableData build() {
			return new TimeTableData(this);
		}
	}
	
	private TimeTableData(Builder builder) {
		this.platform = new String(builder.platform);
		this.destination = new String(builder.destination);
		this.fare = new String(builder.fare);
		this.time = new String(builder.time);
		this.transit = new String(builder.transit);
		this.hourList = new ArrayList<List<String>>(builder.hourList);
	}

	public String getPlatform() {
		return platform;
	}

	public String getDestination() {
		return destination;
	}

	public String getFare() {
		return fare;
	}

	public String getTime() {
		return time;
	}

	public String getTransit() {
		return transit;
	}

	public List<List<String>> getHourList() {
		return hourList;
	}	

}
