package course.labs.contentproviderlab;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import course.labs.contentproviderlab.provider.PlaceBadgesContract;

public class PlaceViewActivity extends ListActivity implements
		LocationListener, LoaderCallbacks<Cursor> {
	private static final long FIVE_MINS = 5 * 60 * 1000;

	private static String TAG = "Lab-ContentProvider";

	// The last valid location reading
	private Location mLastLocationReading;

	// The ListView's adapter
	// private PlaceViewAdapter mAdapter;
	private PlaceViewAdapter mCursorAdapter;

	// default minimum time between new location readings
	private long mMinTime = 5000;

	// default minimum distance between old and new readings.
	private float mMinDistance = 1000.0f;

	// Reference to the LocationManager
	private LocationManager mLocationManager;

	// A fake location provider used for testing
	private MockLocationProvider mMockLocationProvider;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// TODO - Set up the app's user interface
		// This class is a ListActivity, so it has its own ListView
		ListView listView = getListView();
		registerForContextMenu(listView);
		listView.setFooterDividersEnabled(true);

		// TODO - add a footerView to the ListView
		// You can use footer_view.xml to define the footer
		TextView footerView = (TextView) getLayoutInflater().inflate(
				R.layout.footer_view, null);
		listView.addFooterView(footerView);
		// TODO - When the footerView's onClick() method is called, it must
		// issue the
		// following log call
		// log("Entered footerView.OnClickListener.onClick()");

		footerView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				// When the footerView's onClick() method is called, it must
				// issue the follow log call
				log("Entered footerView.OnClickListener.onClick()");

				boolean isFreshLocation = mLastLocationReading != null
						&& age(mLastLocationReading) < FIVE_MINS;

				if (isFreshLocation) {
					boolean isInList = mCursorAdapter
							.intersects(mLastLocationReading);

					if (isInList) {
						// 2) The current location has been seen before - issue
						// Toast message.
						log("You already have this location badge");
						Toast.makeText(v.getContext(),
								"You already have this location badge",
								Toast.LENGTH_LONG).show();
					} else {
						// 1) The current location is new - download new Place
						// Badge. Issue the
						log("Starting Place Download");
						new PlaceDownloaderTask(PlaceViewActivity.this)
								.execute(mLastLocationReading);
					}

				} else {
					// 3) There is no current location - response is up to you.
					// The best
					// solution is to disable the footerView until you have a
					// location.
					log("Location data is not available");
				}
			}
		});

		// TODO - Create and set empty PlaceViewAdapter
		// ListView's adapter should be a PlaceViewAdapter called mCursorAdapter
		mCursorAdapter = new PlaceViewAdapter(getApplicationContext(), null, 0);
		listView.setAdapter(mCursorAdapter);

		// TODO - Initialize a CursorLoader
		getLoaderManager().initLoader(0, null, this);

	}

	@Override
	protected void onResume() {
		super.onResume();

		if (null == (mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)))
			finish();
		
		mMockLocationProvider = new MockLocationProvider(
				LocationManager.NETWORK_PROVIDER, this);

		// TODO - Check NETWORK_PROVIDER for an existing location reading.
		// Only keep this last reading if it is fresh - less than 5 minutes old.
		mLastLocationReading = mLocationManager
				.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (mLastLocationReading != null
				&& age(mLastLocationReading) > FIVE_MINS) {

			mLastLocationReading = null;

		}

		// TODO - Register to receive location updates from NETWORK_PROVIDER
		mLocationManager
				.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
						FIVE_MINS, mMinDistance, this);
		
	    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance, this);

	}

	@Override
	protected void onPause() {

		mMockLocationProvider.shutdown();
		// TODO - Unregister for location updates
		mLocationManager.removeUpdates(this);
		super.onPause();
	}

	public void addNewPlace(PlaceRecord place) {

		log("Entered addNewPlace()");
		mCursorAdapter.add(place);

	}

	@Override
	public void onLocationChanged(Location currentLocation) {

		// TODO - Handle location updates
		// Cases to consider
		// 1) If there is no last location, keep the current location.
		// 2) If the current location is older than the last location, ignore
		// the current location
		// 3) If the current location is newer than the last locations, keep the
		// current location.

		if (mLastLocationReading == null
				|| currentLocation.getTime() > mLastLocationReading.getTime()) {
			mLastLocationReading = currentLocation;
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		// not implemented
	}

	@Override
	public void onProviderEnabled(String provider) {
		// not implemented
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// not implemented
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		log("Entered onCreateLoader()");

		// TODO - Create a new CursorLoader and return it
		// Get the ContentResolver
		// ContentResolver contentResolver = getContentResolver();

		StringBuffer where = new StringBuffer();
		where.append("((").append(PlaceBadgesContract.COUNTRY_NAME)
				.append(" NOTNULL)");
		where.append(" AND (").append(PlaceBadgesContract.COUNTRY_NAME)
				.append(" != '' )");

		String sortOrder = PlaceBadgesContract._ID + " ASC";

		String columnsToExtract[] = new String[] { PlaceBadgesContract._ID,
				PlaceBadgesContract.COUNTRY_NAME,
				PlaceBadgesContract.PLACE_NAME, PlaceBadgesContract.LAT,
				PlaceBadgesContract.LON };

		return new CursorLoader(this, PlaceBadgesContract.CONTENT_URI,
				columnsToExtract, where.toString(), null, sortOrder);

	}

	@Override
	public void onLoadFinished(Loader<Cursor> newLoader, Cursor newCursor) {

		// TODO - Swap in the newCursor
		mCursorAdapter.swapCursor(newCursor);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> newLoader) {

		// TODO - Swap in a null Cursor
		mCursorAdapter.swapCursor(null);

	}

	private long age(Location location) {
		return System.currentTimeMillis() - location.getTime();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.print_badges:
			ArrayList<PlaceRecord> currData = mCursorAdapter.getList();
			for (int i = 0; i < currData.size(); i++) {
				log(currData.get(i).toString());
			}
			return true;
		case R.id.delete_badges:
			mCursorAdapter.removeAllViews();
			return true;
		case R.id.place_one:
			mMockLocationProvider.pushLocation(37.422, -122.084);
			return true;
		case R.id.place_invalid:
			mMockLocationProvider.pushLocation(0, 0);
			return true;
		case R.id.place_two:
			mMockLocationProvider.pushLocation(38.996667, -76.9275);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private static void log(String msg) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i(TAG, msg);
	}
}