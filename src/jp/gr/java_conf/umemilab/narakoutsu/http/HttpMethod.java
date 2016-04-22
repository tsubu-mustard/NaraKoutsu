package jp.gr.java_conf.umemilab.narakoutsu.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.gr.java_conf.umemilab.narakoutsu.NaraKoutsuException;
import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTable;
import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTableData;
import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTableLabel;
import jp.gr.java_conf.umemilab.util.Log2;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class HttpMethod {
	private static final String TAG = HttpMethod.class.getSimpleName();
	private static final String URL = "http://jikoku.narakotsu.co.jp/form/asp/";
	private HttpClient client;
	
	private static String getLabel(String html) {
		String label = "";
        Matcher m = Pattern.compile("<TH NOWRAP BGCOLOR=.+?>(.+?)</TH>").matcher(html);
        if (m.find()) {
        	label = m.group(1);
        } else {
        	throw(new IllegalStateException());        	
        }
        return label;
	}
	private static int getColumnNum(String html) {
		int count = 0;
        Matcher m = Pattern.compile("<TD NOWRAP BGCOLOR=.+?>(.*)</TD>").matcher(html);
        while (m.find()) {
        	String tmp = m.group(1);
        	if ( !tmp.contains("&nbsp;") ) {
	        	count++;
        	}
        }
        Log2.d(TAG, "count: " + count);
		
		return count;
	}
		
	private static List<String> getElement(String html, int columnNum) {
        Matcher m = Pattern.compile("<TD NOWRAP BGCOLOR=\".+\">(.*)</TD>").matcher(html);
        List<String> list = new ArrayList<String>();
        while (m.find()) {
        	String tmp = m.group(1);
        	if ( !tmp.contains("&nbsp;") ) {
	        	tmp = tmp.replace("<B>", "");
	        	tmp = tmp.replace("</B>", "");
	        	tmp = tmp.replace("<BR>", "\n");
	        	list.add(tmp);
        	}
        }
        if (columnNum != list.size()) {
        	throw(new IllegalStateException());
        }
        Log2.d(TAG, list.toString());
		
		return list;
	}
	
	private static List<String> getFare(String html, int columnNum) {
        Matcher m = Pattern.compile(">(\\d+円)<").matcher(html);
        List<String> list = new ArrayList<String>();
        while (m.find()) {
        	list.add(m.group(1));
        }
        if (columnNum != list.size()) {
        	throw(new IllegalStateException());
        }
        Log2.d(TAG, list.toString());
		
		return list;
	}
/*
	private static List<String> getTime(String html, int columnNum) {
        Matcher m = Pattern.compile(">(\\d+分)<").matcher(html);
        List<String> list = new ArrayList<String>();
        while (m.find()) {
        	list.add(m.group(1));
        }
        if (columnNum != list.size()) {
        	throw(new IllegalStateException());
        }
        Log2.d(TAG, list.toString());
		
		return list;
	}
*/	
	public static TimeTable divideElement(String html) {
		TimeTableLabel timeTableLabel;
		List<TimeTableData> timeTableDataList;
		TimeTableLabel.Builder ttlBuilder = new TimeTableLabel.Builder();
		timeTableDataList = new ArrayList<TimeTableData>();

		// HTMLの各行は<TR>タグで区切られる
        Matcher m = Pattern.compile("<TR>((\r|\n|.)+?)</TR>").matcher(html);
        String platformHtml = "";
        String destHtml = "";
        String fareHtml = "";
        String timeHtml = "";
        String transitHtml = "";
    	List<String> platformList = null;
    	List<String> destList = null;
    	List<String> fareList = null;
    	List<String> timeList = null;
    	List<String> transitList = null;
        
        if (m.find()) {
            platformHtml = m.group(1);
        } else {
        	throw(new IllegalStateException());
        }
        if (m.find()) {
            destHtml = m.group(1);
        } else {
        	throw(new IllegalStateException());
        }
        if (m.find()) {
            fareHtml = m.group(1);
        } else {
        	throw(new IllegalStateException());
        }
        if (m.find()) {
            timeHtml = m.group(1);
        } else {
        	throw(new IllegalStateException());
        }
        if (m.find()) {
            transitHtml = m.group(1);
        } else {
        	throw(new IllegalStateException());
        }
        
        // 列の個数
        int columnNum = getColumnNum(platformHtml);
        if (columnNum == 0) {
        	throw(new IllegalStateException());
        }
		List<TimeTableData.Builder> ttdBuilderList = new ArrayList<TimeTableData.Builder>(columnNum);
		for( int i = 0; i < columnNum; i++ ) {
			ttdBuilderList.add(new TimeTableData.Builder());
		}

		// のりば
		ttlBuilder.platform(getLabel(platformHtml));
        platformList = getElement(platformHtml, columnNum);
        for( int i = 0; i < columnNum; i++ ) {
        	ttdBuilderList.get(i).platform(platformList.get(i));
        }
        // 行き先
		ttlBuilder.destination(getLabel(destHtml));
        destList = getElement(destHtml, columnNum);
        for( int i = 0; i < columnNum; i++ ) {
        	ttdBuilderList.get(i).destination(destList.get(i));
        }
        // 運賃
		ttlBuilder.fare(getLabel(fareHtml));
        fareList = getFare(fareHtml, columnNum);
        for( int i = 0; i < columnNum; i++ ) {
        	ttdBuilderList.get(i).fare(fareList.get(i));
        }
        // 所要時間
		ttlBuilder.time(getLabel(timeHtml));
        timeList = getElement(timeHtml, columnNum);
        for( int i = 0; i < columnNum; i++ ) {
        	ttdBuilderList.get(i).time(timeList.get(i));
        }
        // 乗継有無
		ttlBuilder.transit(getLabel(transitHtml));
        transitList = getElement(transitHtml, columnNum);
        for( int i = 0; i < columnNum; i++ ) {
        	ttdBuilderList.get(i).transit(transitList.get(i));
        }
        
        getTimeTable(m, ttlBuilder, ttdBuilderList);
        
        timeTableLabel = ttlBuilder.build();
        for( TimeTableData.Builder b : ttdBuilderList ) {
        	timeTableDataList.add(b.build());
        }
        return TimeTable.createTimeTable(timeTableLabel, timeTableDataList);
	}
	
	private static void getTimeTable(Matcher m, TimeTableLabel.Builder ttlBuilder, List<TimeTableData.Builder> ttdBuilderList) {
        while (m.find()) {
        	// 各時刻行を処理
            Matcher m2 = Pattern.compile("<TH NOWRAP BGCOLOR=\".+?\">のりば</TH>").matcher(m.group(1));
            if (m2.find()) {
            	// 時刻行終了
            	break;
            }
            // 1行分を処理
            m2 = Pattern.compile("<TH NOWRAP BGCOLOR=((\r|\n|.)+?)disphour=(\\d+)\">").matcher(m.group(1));
            if (m2.find()) {
            	// 時刻
            	ttlBuilder.addHour(m2.group(3));
            }
            
            List<String> subList = new ArrayList<String>();
            Matcher m3 = Pattern.compile("<TD NOWRAP BGCOLOR=\".+?\">(.*)</TD>").matcher(m.group(1));
            while (m3.find()) {
            	String tmp = m3.group(1);
	        	tmp = tmp.replace("&nbsp;", "");
	        	tmp = tmp.replace("<B>", "");
	        	tmp = tmp.replace("</B>", "");
	        	tmp = tmp.replace("<BR>", " ");
	        	tmp = tmp.replaceAll("<FONT.+?>", "");
	        	tmp = tmp.replaceAll("</FONT>", "");
	        	subList.add(tmp);
            }
            
            /** subList と ttdBuilderList　の　サイズが異なったらエラー */
            if (subList.size() != ttdBuilderList.size()) {
            	throw(new IllegalStateException());
            }
            
            for ( int i = 0; i < ttdBuilderList.size(); i++ ) {
            	String [] minutesString = subList.get(i).split("\\s");
            	ttdBuilderList.get(i).addHour(minutesString);
            }
        }        
	}
	
	public static boolean hasNetwork(Context ctx) {
		NetworkInfo ni = getNetworkInfo(ctx);
		return (ni != null && ni.isConnected());
	}
	
	private static NetworkInfo getNetworkInfo(Context ctx) {
		ConnectivityManager c = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		return (c == null ? null : c.getActiveNetworkInfo());
	}
	
	public List<String> getTimeTableURL(int dayKind, String fromName, String toName) throws NaraKoutsuException {
		List<String> timeTableUrl = new ArrayList<String>(2);
		byte[] resByte;
		try {
			// 出発地、目的地を指定
			client = new DefaultHttpClient();
			// タイムアウトを10秒に設定
		    HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
		    HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
		    
		    HttpPost post = new HttpPost(URL + "ejhr0201.asp");
		    List<NameValuePair> param = new ArrayList<NameValuePair>(1);
		    param.add(new BasicNameValuePair("dia", "0"));
		    param.add(new BasicNameValuePair("daykind", String.valueOf(dayKind)));
		    param.add(new BasicNameValuePair("fromname", fromName));
		    param.add(new BasicNameValuePair("toname", toName));
		    param.add(new BasicNameValuePair("nextpage", "時刻・運賃表示"));
		    param.add(new BasicNameValuePair("ph1", "時刻・運賃表示"));
		    param.add(new BasicNameValuePair("ph2", "定期運賃早見表"));
		    param.add(new BasicNameValuePair("ph3", "臨時バス案内"));
			post.setEntity(new UrlEncodedFormEntity(param, "Shift_JIS"));
		    // ヘッダ、時刻表、フッタの3つのフレームを取得
		    HttpResponse res = client.execute(post);
		    String frameStr = "";
		    if (res.getStatusLine().getStatusCode() != 200) {
		    	throw(new NaraKoutsuException(NaraKoutsuException.ID_HTML_ERROR));
		    }

	        resByte = EntityUtils.toByteArray(res.getEntity());
	        frameStr = new String(resByte, "Shift_JIS");
		    
		    if ( frameStr.contains("を含む停留所がみつかりません") ) {
		    	throw(new NaraKoutsuException(NaraKoutsuException.ID_BUS_STOP_NOT_FOUND));
		    } else if ( frameStr.contains("停留所名をクリックしてください")) {
		    	throw(new NaraKoutsuException(NaraKoutsuException.ID_BUS_STOP_NOT_SINGLE));
		    } else {
		    
		    	// 時刻表のフレームが存在するか
		        Matcher m = Pattern.compile("SRC=\\\"(.+)\\\".+NAME=\"main\"").matcher(frameStr);
		        if (!m.find()) {
		        	// 存在しなければエラー
			    	return timeTableUrl;	    		
		        }      
		        timeTableUrl.add(URL + m.group(1));
		        
		        // 備考のフレームが存在するか
		        m = Pattern.compile("SRC=\\\"(.+)\\\".+NAME=\"note\"").matcher(frameStr);
		        if (!m.find()) {
		        	// 存在しなければ時刻表だけを格納した状態で返る
			    	return timeTableUrl;	    		
		        }      
		        timeTableUrl.add(URL + m.group(1));
		        
		    }
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}
		
		return timeTableUrl;
		
	}
	
	public String getTimeTableHtml(String url) throws NaraKoutsuException {
		String timeTableStr = "";
		byte[] resByte;
		try {
			client = new DefaultHttpClient();
			// タイムアウトを10秒に設定
		    HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
		    HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
		    
	        HttpGet get = new HttpGet(url);
	        HttpResponse res = client.execute(get);
		    if (res.getStatusLine().getStatusCode() != 200) {
		    	throw(new NaraKoutsuException(NaraKoutsuException.ID_HTML_ERROR));
		    }

	        resByte = EntityUtils.toByteArray(res.getEntity());
	        timeTableStr = new String(resByte, "Shift_JIS");
		    if ( timeTableStr.contains("該当する路線がありません")) {
		    	throw(new NaraKoutsuException(NaraKoutsuException.ID_ROUTE_NOT_FOUND));
		    }
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}
		
		return timeTableStr;
	}	
	
	public String getNoticeString(String url) throws NaraKoutsuException {
		String noticeStr = "";
		byte[] resByte;
		try {
			client = new DefaultHttpClient();
			// タイムアウトを10秒に設定
		    HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
		    HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
		    
	        HttpGet get = new HttpGet(url);
	        HttpResponse res = client.execute(get);

	        if (res.getStatusLine().getStatusCode() != 200) {
		    	throw(new NaraKoutsuException(NaraKoutsuException.ID_HTML_ERROR));
		    }
	        resByte = EntityUtils.toByteArray(res.getEntity());
	        String noticeHtml = new String(resByte, "Shift_JIS");
	        
	        Matcher m = Pattern.compile("<TD.+?>((\r|\n|.)+?)</TD>").matcher(noticeHtml);
	        if (m.find()) {
	        	noticeStr= m.group(1);
        		noticeStr = noticeStr.replaceAll("(\r|\n|<B>|</B>|<FONT.+?>|</FONT>)", "");
        		noticeStr = noticeStr.replaceAll("<BR>", "\n");
	        }
	        
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}
		
		return noticeStr;
	}	
	
	
	public String getBusStopHtml(String stopName) {
        String busStopHtml = "";
		//ejhr0011.asp?dia=0&daykind=1&fromname=%82%AB%82%C3%82%DD%82%C8%82%DD&toname=&stopflg=0		
	    client = new DefaultHttpClient();
	    HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
	    HttpConnectionParams.setSoTimeout(client.getParams(), 10000);
	    
	    try {
	    	HttpGet get = new HttpGet(URL + "ejhr0011.asp?dia=0&daykind=1&fromname=" + URLEncoder.encode(stopName, "Shift_JIS") + "&stopflg=0");
	        HttpResponse res = client.execute(get);
	        if (res.getStatusLine().getStatusCode() == 200) {
		        byte[] resByte = EntityUtils.toByteArray(res.getEntity());
		        busStopHtml = new String(resByte, "Shift_JIS");
	    	}
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}
		
        return busStopHtml;
	}
	
	public static List<String> getBusStopList(String busStopHtml) {
		List<String> list = new ArrayList<String>();
        Matcher m = Pattern.compile("<TD><A HREF=.+?>(.+?)</A></TD>").matcher(busStopHtml);
        while (m.find()) {
        	list.add(m.group(1));
        }
		return list;
	}
	
}
