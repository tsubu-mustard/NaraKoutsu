package jp.gr.java_conf.umemilab.narakoutsu;

import java.util.List;

import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTable;
import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTableData;
import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTableLabel;
import jp.gr.java_conf.umemilab.narakoutsu.http.HttpMethod;
import jp.gr.java_conf.umemilab.narakoutsu.provider.BusStopSearchProvider;
import jp.gr.java_conf.umemilab.util.Log2;

import jp.gr.java_conf.umemilab.narakoutsu.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class TimeTableActivity extends Activity {
	// フォントサイズ (sp)
	private static final int[] FONT_SIZE_ARRAY = {14, 18, 22};

	// フォントサイズインデックス
	private static String[] FONT_SIZE_NAME_LIST;
	private static final int FONT_INDEX_SMALL = 0;
	private static final int FONT_INDEX_MEDIUM = 1;
	private static final int FONT_INDEX_LARGE = 2;
	
	// メニュー: フォントサイズ
	private static final int MENU_FONT_SIZE = 0;
	
	// SharedPreferences 設定
	private static final String SHAREDPREF_SETTINGS = "Settings";
	private static final String SHAREDPREF_KEY_FONT_INDEX = "fontIndex";

	private TextView result;
	private TextView tvDayKind;
	private TextView tvFrom;
	private TextView tvTo;
	private TableLayout tlTimeTable;
	private TextView[][] arrayText = null;
	private Button btBack;
	private Button btForward;
	private TimeTable timeTable;
	private TextView tvPage;
	private int pageIdx;
	private String fromName = "";
	private String toName = "";
	private int dayKind;
	
	private HttpMethod httpMethod;
	private String errorMsg;
	private String noticeMsg;
	
	// フォントサイズ
	private int fontIndex = FONT_INDEX_SMALL;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Intent i = getIntent();
        fromName = i.getStringExtra(BusStopActivity.INTENT_FROM_NAME);
        toName = i.getStringExtra(BusStopActivity.INTENT_TO_NAME);
        dayKind = i.getIntExtra(BusStopActivity.INTENT_DAY_KIND, TimeTable.DAY_KIND_NORMAL);
        
        tvPage = (TextView) findViewById(R.id.tvPage);
        tvDayKind = (TextView) findViewById(R.id.tvDayKind);
        tvFrom = (TextView) findViewById(R.id.tvFrom);
        tvTo = (TextView) findViewById(R.id.tvTo);
        
        FONT_SIZE_NAME_LIST = getResources().getStringArray(R.array.font_size_name_list);
        SharedPreferences sp = getApplicationContext().getSharedPreferences(SHAREDPREF_SETTINGS, MODE_PRIVATE);
        fontIndex = sp.getInt(SHAREDPREF_KEY_FONT_INDEX, FONT_INDEX_SMALL);
        
        
        tvPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        tvDayKind.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        tvFrom.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        tvTo.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        
        switch(dayKind) {
        	case TimeTable.DAY_KIND_NORMAL:
        		tvDayKind.setText("ダイヤ種別：平日");
        		break;
        	case TimeTable.DAY_KIND_SATURDAY:
        		tvDayKind.setText("ダイヤ種別：土曜日");
        		break;
        	case TimeTable.DAY_KIND_SUNDAY:
        		tvDayKind.setText("ダイヤ種別：日曜・祝日");
    			break;
        }
        
        tvFrom.setText("乗車地：" + fromName);
        tvTo.setText("降車地：" + toName);
        
        
        result = (TextView) findViewById(R.id.Test);
        tlTimeTable = (TableLayout) findViewById(R.id.tlTimeTable);
        btBack = (Button) findViewById(R.id.btBack);
        btForward = (Button) findViewById(R.id.btForward);
		btBack.setOnClickListener(mBackOnClickListner);
        btForward.setOnClickListener(mForwardOnClickListner);
        
        if (!Log2.DEBUG) {
        	// デバッグ結果を表示しない
        	result.setVisibility(View.GONE);
        }
        
		if (!HttpMethod.hasNetwork(getApplicationContext())) {
			errorMsg = getString(R.string.error_network);
			removeDialog(DIALOG_ERROR);
			showDialog(DIALOG_ERROR);
			return;
		}
        
		// プログレスダイアログ表示中は回転を許可しない
		Orientation.lockOrientation(this);

		showDialog(DIALOG_TIME_TABLE_PROGRESS);
              
        pageIdx = 0;
        
        new Thread(ReadHtmlRunnable).start();
    }

	private static final int DIALOG_TIME_TABLE_PROGRESS = 0;
	private static final int DIALOG_ERROR = 1;
	private static final int DIALOG_FONT_SIZE = 2;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dlg = null;
		switch(id) {
		case DIALOG_TIME_TABLE_PROGRESS:
			dlg = createTimeTableProgressDialog();
			break;
		case DIALOG_ERROR:
			dlg = createErrorDialog();
			break;
		case DIALOG_FONT_SIZE:
			dlg = createFontDialog();
		default:
			;
		}
		return dlg;
	}

	private Dialog createErrorDialog() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TimeTableActivity.this);
		alertBuilder.setTitle("エラー");
		alertBuilder.setMessage(errorMsg);
		alertBuilder.setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Activity終了
				finish();
			}
		});
		return alertBuilder.create();
	}
	
	private Dialog createTimeTableProgressDialog() {
		ProgressDialog dlg;
		dlg = new ProgressDialog(this);
		dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dlg.setMessage("時刻表を取得しています...");
		dlg.setCancelable(false);
		
		return dlg;
	}	
	
	private Dialog createFontDialog() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TimeTableActivity.this);
		alertBuilder.setTitle(getResources().getString(R.string.menu_font_size))
		.setSingleChoiceItems(FONT_SIZE_NAME_LIST, fontIndex, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				fontIndex = which;
		        Editor edit = getApplicationContext().getSharedPreferences(SHAREDPREF_SETTINGS, MODE_PRIVATE).edit();
		        edit.putInt(SHAREDPREF_KEY_FONT_INDEX, fontIndex);
		        edit.commit();
		        dialog.dismiss();
		        SetTable();
			}
		});
		
		return alertBuilder.create();
	}
	
    
    
    private Runnable ReadHtmlRunnable = new Runnable(){

		public void run() {
			String errorMsg = "";
			
			httpMethod = new HttpMethod();
			String timeTableStr = "";
			List<String> timeTableUrl;
			noticeMsg = "";
			try {
				timeTableUrl = httpMethod.getTimeTableURL(dayKind, fromName, toName);
				if (timeTableUrl.size() < 1) {
					// 通信エラーが発生
					errorMsg = getString(R.string.error_network);
				} else {
					timeTableStr = httpMethod.getTimeTableHtml(timeTableUrl.get(0));
					if (timeTableStr.equals("")) {
						// 通信エラーが発生
						errorMsg = getString(R.string.error_network);
					}
					if (timeTableUrl.size() == 2) {
						noticeMsg = httpMethod.getNoticeString(timeTableUrl.get(1));						
					}
				}
			} catch (NaraKoutsuException e1) {
				switch(e1.getId()) {
				case NaraKoutsuException.ID_BUS_STOP_NOT_FOUND:
					errorMsg = getString(R.string.error_bus_stop_not_found);
					break;
				case NaraKoutsuException.ID_BUS_STOP_NOT_SINGLE:
					errorMsg = getString(R.string.error_bus_stop_not_found);
					break;
				case NaraKoutsuException.ID_ROUTE_NOT_FOUND:
					errorMsg = getString(R.string.error_route_not_found);
					break;
				case NaraKoutsuException.ID_HTML_ERROR:
					errorMsg = getString(R.string.error_network);
					break;
				default:
					;
				}
			}
			
			if (errorMsg.equals("")) {
		        // 項目ごとに分割
				try {
					timeTable = HttpMethod.divideElement(timeTableStr);
				} catch (IllegalStateException e) {
					errorMsg = getString(R.string.error_parse_html);				
				}
			} else {
				timeTable = null;
			}
	        Message msg = new Message();
	        Bundle b = new Bundle();
	        b.putString("debug", timeTableStr);
	        b.putString(MSG_KEY_ERROR_MESSAGE, errorMsg);
	        msg.setData(b);
	        readHtmlHandler.sendMessage(msg);
		}
    	
    };
    
	private static final String MSG_KEY_ERROR_MESSAGE = "Error Message";
    private final Handler readHtmlHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {

    		// プログレスダイアログの終了
    		dismissDialog(DIALOG_TIME_TABLE_PROGRESS);
			// 回転ロックを解除
			Orientation.unlockOrientation(TimeTableActivity.this);
			
			errorMsg = msg.getData().getString(MSG_KEY_ERROR_MESSAGE);
			if (!errorMsg.equals("")) {
				removeDialog(DIALOG_ERROR);
				showDialog(DIALOG_ERROR);
			} else {
			
	    		pageIdx = 0;
	    		// TODO テーブルのNullPointerケア
	    		SetTable();
	    	}
    		// debug出力
    		if (Log2.DEBUG) {
    				result.setText(msg.getData().get("debug").toString());
    		}    		
    	}
    };
    
    private void SetTable() {
    	if (timeTable == null) {
    		return;
    	}
        tvPage.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        tvDayKind.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        tvFrom.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
        tvTo.setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
    	
    	
//		tlTimeTable.setColumnShrinkable(0, true);
		tlTimeTable.setColumnShrinkable(1, true);
		tlTimeTable.setColumnStretchable(1, true);
		tlTimeTable.setBackgroundColor(TimeTable.COLOR_LABEL);
    	
		// tableを作成
		TableRow row;
		TimeTableLabel ttl = timeTable.getTimeTableLabel();
		TimeTableData ttd = timeTable.getTimeTableDataList().get(pageIdx);
		int rowSize = TimeTableLabel.FIXED_PARAM_NUM + ttl.getHourList().size();
		arrayText = new TextView[rowSize][2];
		
		tvPage.setText(String.format("(%d/%d)", pageIdx + 1, timeTable.getTimeTableDataList().size()));
		tlTimeTable.removeAllViews();
		
		int y = 0;
		// のりば
		SetRow(y, arrayText, ttl.getPlatform(), ttd.getPlatform());
		y++;
		// 行き先
		SetRow(y, arrayText, ttl.getDestination(), ttd.getDestination());
		// 運賃
		y++;
		SetRow(y, arrayText, ttl.getFare(), ttd.getFare());
		// 所要時分
		y++;
		SetRow(y, arrayText, ttl.getTime(), ttd.getTime());
		// 乗継有無
		y++;
		SetRow(y, arrayText, ttl.getTransit(), ttd.getTransit());
		
		y++;
		List<String> hourLabelList = ttl.getHourList();
		List<List<String>> hourDataList = ttd.getHourList();
		for ( int h = 0; y < rowSize-1 /* 備考欄の分を引く */; y++, h++ ) {
			row = new TableRow(TimeTableActivity.this);
			arrayText[y][0] = new TextView(TimeTableActivity.this);
			arrayText[y][0].setText(hourLabelList.get(h) + ":00");
			arrayText[y][0].setBackgroundColor(TimeTable.COLOR_LABEL);
			arrayText[y][0].setTextColor(TimeTable.COLOR_TEXT);
			arrayText[y][0].setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
			row.addView(arrayText[y][0]);
			StringBuilder sb = new StringBuilder();
			for( String m : hourDataList.get(h) ) {
				sb.append(m); sb.append(" ");
			}
			arrayText[y][1] = new TextView(TimeTableActivity.this);
			arrayText[y][1].setText(sb.toString());
			arrayText[y][1].setBackgroundColor(TimeTable.COLOR_TIME);
			arrayText[y][1].setTextColor(TimeTable.COLOR_TEXT);
			arrayText[y][1].setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
			row.addView(arrayText[y][1]);
			
			tlTimeTable.addView(row);
		}
		// 備考
		row = new TableRow(TimeTableActivity.this);
		arrayText[y][0] = new TextView(TimeTableActivity.this);
		arrayText[y][0].setText("備考");
		arrayText[y][0].setBackgroundColor(TimeTable.COLOR_LABEL);
		arrayText[y][0].setTextColor(TimeTable.COLOR_TEXT);
		arrayText[y][0].setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
		row.addView(arrayText[y][0]);
		arrayText[y][1] = new TextView(TimeTableActivity.this);
		arrayText[y][1].setText(noticeMsg);
		arrayText[y][1].setBackgroundColor(TimeTable.COLOR_TIME);
		arrayText[y][1].setTextColor(TimeTable.COLOR_TEXT);
		arrayText[y][1].setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
		row.addView(arrayText[y][1]);
		
		tlTimeTable.addView(row);
    	
    }

	private void SetRow(int y, TextView[][] arrayText2, String label,
			String value) {
		TableRow row = new TableRow(TimeTableActivity.this);

		row.setBackgroundColor(TimeTable.COLOR_LABEL);
		arrayText[y][0] = new TextView(TimeTableActivity.this);
		arrayText[y][0].setText(label);
		arrayText[y][0].setBackgroundColor(TimeTable.COLOR_LABEL);
		arrayText[y][0].setTextColor(TimeTable.COLOR_TEXT);
		arrayText[y][0].setLines(1);
		arrayText[y][0].setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
		row.addView(arrayText[y][0]);
		arrayText[y][1] = new TextView(TimeTableActivity.this);
		arrayText[y][1].setText(value);
		arrayText[y][1].setBackgroundColor(TimeTable.COLOR_HEADER);
		arrayText[y][1].setTextColor(TimeTable.COLOR_TEXT);
		arrayText[y][1].setTextSize(TypedValue.COMPLEX_UNIT_SP, FONT_SIZE_ARRAY[fontIndex]);
		row.addView(arrayText[y][1]);
		tlTimeTable.addView(row);		
	}

	private OnClickListener mBackOnClickListner = new OnClickListener() {
		public void onClick(View arg0) {
			pageIdx--;
			if (pageIdx < 0) {
				pageIdx = timeTable.getTimeTableDataList().size() - 1;
			}
			SetTable();
		}
	};
	private OnClickListener mForwardOnClickListner = new OnClickListener() {
		public void onClick(View arg0) {
			pageIdx++;
			int maxPage = timeTable.getTimeTableDataList().size();
			if (pageIdx >= maxPage) {
				pageIdx = 0;
			}
			SetTable();
		}
	};
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}    
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_FONT_SIZE, MENU_FONT_SIZE, getResources().getString(R.string.menu_font_size));
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case MENU_FONT_SIZE:
			removeDialog(DIALOG_FONT_SIZE);
			showDialog(DIALOG_FONT_SIZE);
			break;
		default:
			;
		}
		return super.onOptionsItemSelected(item);
	}

}