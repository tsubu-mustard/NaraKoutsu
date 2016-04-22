package jp.gr.java_conf.umemilab.narakoutsu;

import java.util.List;

import jp.gr.java_conf.umemilab.narakoutsu.data.TimeTable;
import jp.gr.java_conf.umemilab.narakoutsu.http.HttpMethod;
import jp.gr.java_conf.umemilab.narakoutsu.provider.BusStopSearchProvider;
import jp.gr.java_conf.umemilab.util.Log2;

import jp.gr.java_conf.umemilab.narakoutsu.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

public class BusStopActivity extends Activity {
	private static final String TAG = BusStopActivity.class.getSimpleName();
	private Button btTimeTable;
	private EditText etFromName;
	private EditText etToName;
	private Button btFrom;
	private Button btTo;
	private RadioGroup rgDayKind;
	/** 検索結果を反映するEditText */
	private EditText etSearch;
	
	public static final String INTENT_FROM_NAME = "FromName";
	public static final String INTENT_TO_NAME = "ToName";
	public static final String INTENT_DAY_KIND = "DayKind";
	
	private List<String> busStopList;
	private String queryString;
	
	private String errorMsg;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 起動時にキーボードを表示しない
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.bus_stop);
        
        etFromName = (EditText) findViewById(R.id.etFrom);
        etToName = (EditText) findViewById(R.id.etTo);
        
        btFrom = (Button) findViewById(R.id.btFrom);
        btFrom.setOnClickListener(fromClickListener);
        btTo = (Button) findViewById(R.id.btTo);
        btTo.setOnClickListener(toClickListener);
        
        btTimeTable = (Button) findViewById(R.id.btTimeTable);
        btTimeTable.setOnClickListener(timeTableClickListener);
        
        rgDayKind = (RadioGroup) findViewById(R.id.rgDayKind);
        
		if (etSearch == null) {
			// 検索結果の受け皿を用意しておく
			etSearch = etFromName;
		}
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
        final String queryAction = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(queryAction)) {
        	Log2.d(TAG, "ACTION_SEARCH");
             	
        	// 検索バーでサーチボタンが押された場合
        	doSearchWithIntent(intent);
        } else if (Intent.ACTION_VIEW.equals(queryAction)) {
        	if (intent.getFlags() == Intent.FLAG_ACTIVITY_NEW_TASK) {
            	Log2.d(TAG, "ACTION_VIEW: NEW_TASK");     
            	// 検索バーで候補が選択された場合
        		final String queryString = intent.getStringExtra(SearchManager.QUERY);
            	if (etSearch != null && queryString != null) {
            		// 検索結果をEditTextに反映
            		etSearch.setText(queryString);
            	}
        	}
        }
	}
	
	private void doSearchWithIntent(final Intent queryIntent) {
		final String queryString = queryIntent.getStringExtra(SearchManager.QUERY);
		doSearchWithQuery(queryString);
	}

	private void doSearchWithQuery(final String queryString) {
		this.queryString = queryString;
		// サーバと通信し、候補を表示
		// 検索文字列をWebサイトに渡し、候補を取得する。
		if (!HttpMethod.hasNetwork(getApplicationContext())) {
			errorMsg = getString(R.string.error_network);
			removeDialog(DIALOG_ERROR);
			showDialog(DIALOG_ERROR);
			return;
		}

		// プログレスダイアログ表示中は回転を許可しない
		Orientation.lockOrientation(this);
		
		showDialog(DIALOG_LIST_PROGRESS);
		Thread busStopThread = new Thread(busStopRunner);
		busStopThread.start();
	}
	
	private HttpMethod httpMethod;
	private static final int MSG_END_BUS_STOP_RUNNER = 1;
	private static final String MSG_KEY_ERROR_MESSAGE = "Error Message";
	private Runnable busStopRunner = new Runnable() {
		public void run() {
			String errorMsg = "";
			httpMethod = new HttpMethod();
			String busStopHtml = httpMethod.getBusStopHtml(queryString);
			if (busStopHtml.equals("")) {
				// 通信エラーが発生
				errorMsg = getString(R.string.error_network);
			}
			busStopList = HttpMethod.getBusStopList(busStopHtml);	
			if (busStopList.size() == 0) {
				errorMsg = getString(R.string.error_bus_stop_not_found);
			}
	        Message msg = new Message();
	        msg.what = MSG_END_BUS_STOP_RUNNER;
	        Bundle b = new Bundle();
	        b.putString(MSG_KEY_ERROR_MESSAGE, errorMsg);
	        msg.setData(b);
			busStopHandler.sendMessage(msg);			
		}
	};
	
    private final Handler busStopHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what) {
			case MSG_END_BUS_STOP_RUNNER:
				dismissDialog(DIALOG_LIST_PROGRESS);
				errorMsg = msg.getData().getString(MSG_KEY_ERROR_MESSAGE);
				if (!errorMsg.equals("")) {
					removeDialog(DIALOG_ERROR);
					showDialog(DIALOG_ERROR);
				} else {
					// 前のダイアログを消さないと同じものが出てしまう
					removeDialog(DIALOG_BUS_STOP_LIST);
					showDialog(DIALOG_BUS_STOP_LIST);
				}				
				// 回転ロックを解除
				Orientation.unlockOrientation(BusStopActivity.this);
				break;
			default:
				;
			}
		}
    	
    };
	
	private OnClickListener fromClickListener = new OnClickListener() {
		public void onClick(View v) {
			// 検索結果を表示するEditTextを記憶
	    	etSearch = etFromName;
        	// 注意ダイアログを表示
			onSearchRequested();
		}		
	};
	private OnClickListener toClickListener = new OnClickListener() {
		public void onClick(View v) {
			// 検索結果を表示するEditTextを記憶
	    	etSearch = etToName;
        	// 注意ダイアログを表示
			onSearchRequested();			
		}		
	};
		
	@Override
	public boolean onSearchRequested() {
		// 検索開始時はエディットテキストの文字列を初期値とする
		startSearch(etSearch.getText().toString(), false, null, false);
		return true;
	}

	private OnClickListener timeTableClickListener = new OnClickListener() {
		public void onClick(View arg0) {
			// 停留所を取得
			String fromName = etFromName.getText().toString();
			String toName = etToName.getText().toString();
			int dayKind = TimeTable.DAY_KIND_NORMAL;
			
			// ダイヤ種別を取得
			switch (rgDayKind.getCheckedRadioButtonId()) {
				case R.id.rNormal:
					dayKind = TimeTable.DAY_KIND_NORMAL;
					break;
				case R.id.rSat:
					dayKind = TimeTable.DAY_KIND_SATURDAY;
					break;
				case R.id.rSun:
					dayKind = TimeTable.DAY_KIND_SUNDAY;					
					break;
			}
			
			// 乗車地と降車地が同じ場合はエラー
			if (fromName.equals(toName)) {
				errorMsg = getString(R.string.error_bus_stop_same);
				removeDialog(DIALOG_ERROR);
				showDialog(DIALOG_ERROR);				
			} else {
			
				// 時刻表Activityを起動
				Intent i = new Intent(BusStopActivity.this, TimeTableActivity.class);
				i.putExtra(INTENT_FROM_NAME, fromName);
				i.putExtra(INTENT_TO_NAME, toName);
				i.putExtra(INTENT_DAY_KIND, dayKind);
				startActivity(i);
			}
			
		};
	
	};

	private static final int DIALOG_BUS_STOP_LIST = 0;
	private static final int DIALOG_LIST_PROGRESS = 1;
	private static final int DIALOG_ERROR = 2;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dlg = null;
		switch(id) {
		case DIALOG_BUS_STOP_LIST:
			dlg = createBusStopListDialog();
			break;
		case DIALOG_LIST_PROGRESS:
			dlg = createListProgressDialog();
			break;
		case DIALOG_ERROR:
			dlg = createErrorDialog();
			break;
		default:
			;
		}
		return dlg;
	}

	private Dialog createErrorDialog() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(BusStopActivity.this);
		alertBuilder.setTitle("エラー");
		alertBuilder.setMessage(errorMsg);
		alertBuilder.setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				;
			}
		});
		return alertBuilder.create();
	}

	private Dialog createListProgressDialog() {
		ProgressDialog dlg;
		dlg = new ProgressDialog(this);
		dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dlg.setMessage("停留所リストを取得しています...");
		dlg.setCancelable(false);

		return dlg;
	}	

	private Dialog createBusStopListDialog() {
		// TODO Auto-generated method stub
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(BusStopActivity.this);
		
		alertBuilder.setTitle("停留所を選択してください");
		alertBuilder.setItems(busStopList.toArray(new String[0]), busStopListClickListener);
		
		return alertBuilder.create();		
	}
	
	private DialogInterface.OnClickListener busStopListClickListener = new DialogInterface.OnClickListener() {

		public void onClick(DialogInterface dialog, int which) {
			String busStop = BusStopActivity.this.busStopList.get(which);
    		SearchRecentSuggestions srs = new SearchRecentSuggestions(BusStopActivity.this, 
    				BusStopSearchProvider.AUTHORITY,
    				BusStopSearchProvider.DATABASE_MODE_QUERIES);
    		srs.saveRecentQuery(busStop, null);
			BusStopActivity.this.etSearch.setText(busStop);
		}		
	};
	
	private static final int MENU_SWAP = 0;
	private static final int MENU_CLEAR_HISTORY = 1;
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_SWAP, MENU_SWAP, "乗降車地入替").setIcon(R.drawable.ic_menu_swap);
		menu.add(Menu.NONE, MENU_CLEAR_HISTORY, MENU_CLEAR_HISTORY, "検索履歴消去").setIcon(R.drawable.ic_menu_history);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case MENU_SWAP:
			// 発着停入替
			String from = etFromName.getText().toString();
			String to = etToName.getText().toString();
			etFromName.setText(to);
			etToName.setText(from);
			break;
		case MENU_CLEAR_HISTORY:
    		SearchRecentSuggestions srs = new SearchRecentSuggestions(BusStopActivity.this, 
    				BusStopSearchProvider.AUTHORITY,
    				BusStopSearchProvider.DATABASE_MODE_QUERIES);
			srs.clearHistory();
			break;
		default:
			;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
	}
	
	
}
