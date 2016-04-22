package jp.gr.java_conf.umemilab.narakoutsu.data;

import java.util.List;

import android.graphics.Color;

public class TimeTable {
	public static final int COLOR_LABEL = 0xFFFCF888;
	public static final int COLOR_HEADER = 0xFFFFFFCC;
	public static final int COLOR_TIME = Color.WHITE;
	public static final int COLOR_TEXT = Color.BLACK;
	public static final int DAY_KIND_NORMAL = 1;
	public static final int DAY_KIND_SATURDAY = 2;
	public static final int DAY_KIND_SUNDAY = 3;
	private TimeTableLabel ttl;
	private List<TimeTableData> ttd;
	private TimeTable(TimeTableLabel ttl, List<TimeTableData> ttd) {
		this.ttl = ttl;
		this.ttd = ttd;
	}
	public static TimeTable createTimeTable(TimeTableLabel ttl, List<TimeTableData> ttd) {
		return new TimeTable(ttl, ttd);
	}
	public TimeTableLabel getTimeTableLabel() {
		return ttl;
	}
	public List<TimeTableData> getTimeTableDataList() {
		return ttd;
	}

}
