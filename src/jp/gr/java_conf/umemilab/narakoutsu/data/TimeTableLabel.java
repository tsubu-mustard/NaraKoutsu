package jp.gr.java_conf.umemilab.narakoutsu.data;

import java.util.ArrayList;
import java.util.List;

public class TimeTableLabel {
	public static final int FIXED_PARAM_NUM = 6; // 固定された項目の数。platform, destination, fare, time, transit, 備考。
	private final String platform;
	private final String destination;
	private final String fare;
	private final String time;
	private final String transit;
	private final List<String> hourList;
	
	public static class Builder {
		private String platform = "";
		private String destination = "";
		private String fare = "";
		private String time = "";
		private String transit = "";
		private List<String> hourList = new ArrayList<String>();
		
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
		public Builder addHour(String val) {
			hourList.add(val); return this;
		}
		public TimeTableLabel build() {
			return new TimeTableLabel(this);
		}
	}
	
	private TimeTableLabel(Builder builder) {
		this.platform = new String(builder.platform);
		this.destination = new String(builder.destination);
		this.fare = new String(builder.fare);
		this.time = new String(builder.time);
		this.transit = new String(builder.transit);
		this.hourList = new ArrayList<String>(builder.hourList);
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

	public List<String> getHourList() {
		return hourList;
	}	
}
