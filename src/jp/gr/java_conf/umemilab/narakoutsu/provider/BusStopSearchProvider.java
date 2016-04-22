package jp.gr.java_conf.umemilab.narakoutsu.provider;

import android.content.SearchRecentSuggestionsProvider;

public class BusStopSearchProvider extends SearchRecentSuggestionsProvider {
	public static final String AUTHORITY = "jp.gr.java_conf.umemilab.narakoutsu.provider.BusStopSearchProvider";
	public BusStopSearchProvider() {
		setupSuggestions(AUTHORITY, 
				BusStopSearchProvider.DATABASE_MODE_QUERIES);
	}

}
