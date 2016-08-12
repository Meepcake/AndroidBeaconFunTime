package com.meepcake.androidbeaconfuntime;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.RemoteException;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BeaconConsumer{

    private final String TAG = "Android Beacon Fun Time";

    private static final long SCAN_PERIOD = 10000;

    private Handler beaconHandler = new Handler();

    private ScanFilter mScanFilter;

    private static boolean hasLE = true;
    private boolean mScanning = true;
    public boolean mAdvertising;

    public static ArrayAdapter<String> adapter;

    public BeaconManager beaconManager;

    public static final List<String> resultList = new ArrayList<>();
    public static final List<String> rssiList = new ArrayList<>();
    public static final List<String> otherList = new ArrayList<>();

    public BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseData mAdvertiseData;
    private AdvertiseSettings mAdvertiseSettings;
    private ScanSettings mScanSettings;

    // Handlers
    private Handler scanHandler = new Handler();

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Grab bluetooth Manager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Create instances of both LeScanner and LeAdvertiser.
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        // Ensures that Bluetooth is available on the device and is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        // Check for other features if supported or not.
        if( !BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()){
            Toast.makeText( this, R.string.Multiple_Ads_Not_Supported, Toast.LENGTH_SHORT ).show();
            hasLE = false;
        }else if( !BluetoothAdapter.getDefaultAdapter().isOffloadedFilteringSupported()){
            Toast.makeText( this, R.string.Off_Load_Filtering_Not_Supported, Toast.LENGTH_SHORT ).show();
            hasLE = false;
        }else if( !BluetoothAdapter.getDefaultAdapter().isOffloadedScanBatchingSupported()){
            Toast.makeText( this, R.string.Off_Load_Scan_Batching_Not_Supported, Toast.LENGTH_SHORT ).show();
            hasLE = false;
        }

        // Disable certain buttons if LE Features are not supported.
        if (!hasLE){
            Toast.makeText( this, R.string.System_Does_Not_Meet_Requirements, Toast.LENGTH_LONG ).show();

        }else{
            Toast.makeText( this, R.string.All_Requirements_Met, Toast.LENGTH_SHORT ).show();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v;
            int pageNum = getArguments().getInt(ARG_SECTION_NUMBER);

            // This is where we can separate different layouts based on ARG_SECTION NUMBER
            if (pageNum == 1){


                // Scan
                v = inflater.inflate(R.layout.fragment_scan, container, false);

                // Grab UI Assets
                ListView lv = (ListView) v.findViewById(R.id.listView);
                Button scanButton = (Button) v.findViewById( R.id.btn_ScanStart );

                // Create and set our custom adapter to our list view
                adapter = new CustomAdapter(getContext(),resultList,rssiList,otherList);
                lv.setAdapter(adapter);

                // Set Click Listener for scan Button
                scanButton.setOnClickListener( new View.OnClickListener(){
                    public void onClick(View v){
                        ((MainActivity)getActivity()).scanLeDevice(true);
                    }
                });

            } else if(pageNum == 2) {
                // Transmit
                v  = inflater.inflate(R.layout.fragment_transmit, container, false);

                Button transmitButton = (Button) v.findViewById( R.id.btn_StartBroadcast );

                if (!hasLE){
                    transmitButton.setEnabled(false);
                }

                transmitButton.setOnClickListener( new View.OnClickListener(){
                    public void onClick(View v){

                        ((MainActivity)getActivity()).startBeacon(true,(byte)0x31);
                    }
                });

            }else if(pageNum == 3) {
                // Info
                v  = inflater.inflate(R.layout.fragment_info, container, false);

                Button beaconScanButton = (Button) v.findViewById(R.id.btn_ScanStart);

                beaconScanButton.setOnClickListener( new View.OnClickListener(){
                    public void onClick(View v){
                        ((MainActivity)getActivity()).startBeaconDetect();
                    }
                });

            } else {
                // How the hell would we get here? ._.
                v = inflater.inflate(R.layout.fragment_main, container, false);
            }

            return v;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Scan BT";
                case 1:
                    return "Transmit Beacon";
                case 2:
                    return "Scan Beacon";
            }
            return null;
        }
    }

    /**
    * Starts advertising a beacon with a timeout after 10 seconds.
    **/
    private void startBeacon(boolean enable, byte data){
        if (enable){
            Log.i(TAG,"startBeacon");
            Log.i(TAG,"Building Advertise Data..");

            // Section to build the data stored in the advert.
            AdvertiseData.Builder mBuilder = new AdvertiseData.Builder();
            ByteBuffer mManufacturerData = ByteBuffer.allocate(24);

            byte[] uuid = getIdAsByte(UUID.fromString(getResources().getString(R.string.UUID)));
            mManufacturerData.put(0, (byte)0xBE); // Beacon Identifier
            mManufacturerData.put(1, (byte)0xAC); // Beacon Identifier

            for (int i=2; i<=17; i++) {
                mManufacturerData.put(i, uuid[i-2]); // adding the UUID
            }

            mManufacturerData.put(18, (byte)0x00); // first byte of Major
            mManufacturerData.put(19, data); // second byte of Major
            mManufacturerData.put(20, (byte)0x00); // first minor
            mManufacturerData.put(21, (byte)0x06); // second minor
            mManufacturerData.put(22, (byte)0xB5); // txPower
            mBuilder.addManufacturerData(224, mManufacturerData.array()); // using google's company ID
            mAdvertiseData = mBuilder.build();

            Log.i(TAG,"Building Advertise Settings..");
            // Section to set ad settings.
            AdvertiseSettings.Builder nBuilder = new AdvertiseSettings.Builder();
            nBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
            nBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
            nBuilder.setConnectable(false);
            nBuilder.setTimeout(0);
            mAdvertiseSettings = nBuilder.build();

            mAdvertising = true;

            Log.i(TAG,"Done with advert setup, attempting to start...");
            mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mBeaconCallback);

            beaconHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeAdvertiser.stopAdvertising(mBeaconCallback);
                    mAdvertising = false;
                    Log.i(TAG,"Beacon Stopped");
                }
            },SCAN_PERIOD);
        }else{
            mBluetoothLeAdvertiser.stopAdvertising(mBeaconCallback);
            mAdvertising = false;
            Log.i(TAG,"Beacon Stopped");
        }

    }

    /**
     * Beacon Transmit Callback
     **/
    private AdvertiseCallback mBeaconCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG,"Ad started successfully.");
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG,"Error starting Ad :" + errorCode );
            super.onStartFailure(errorCode);
        }
    };

    /**
     * BLE Scan Method starts the scanner with timeout : SCAN_PERIOD
     **/
    private void scanLeDevice(final boolean enable){
        if (enable){
            Log.e( TAG, "scanLeDevice");
            Toast.makeText( this, R.string.Scan_Started, Toast.LENGTH_SHORT ).show();

            // Clear Lists
            resultList.clear();
            rssiList.clear();
            otherList.clear();

            // Build Scan Settings here
            Log.i(TAG, getResources().getString(R.string.Building_Scan_Settings));
            ScanSettings.Builder mBuilder = new ScanSettings.Builder();
            mBuilder.setReportDelay(0);
            mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
            mScanSettings = mBuilder.build();

            Log.i(TAG,getResources().getString(R.string.Building_Scan_Filter));
            ScanFilter.Builder nBuilder = new ScanFilter.Builder();
            ByteBuffer mManufacturerData = ByteBuffer.allocate(23);
            ByteBuffer mManufacturerDataMask = ByteBuffer.allocate(24);
            byte[] uuid = getIdAsByte(UUID.fromString(getResources().getString(R.string.UUID)));
            mManufacturerData.put(0, (byte)0xBE);
            mManufacturerData.put(1, (byte)0xAC);
            for (int i=2; i<=17; i++) {
                mManufacturerData.put(i, uuid[i-2]);
            }
            for (int i=0; i<=17; i++) {
                mManufacturerDataMask.put((byte)0x01);
            }
            nBuilder.setManufacturerData(224, mManufacturerData.array(), mManufacturerDataMask.array());
            mScanFilter = nBuilder.build();

            mScanning = true;
            Log.i( TAG, "Scan Started");
            // Scan with no Filter to see all devices. Filter will do exactly what you think it does...
            //mBluetoothLeScanner.startScan(Arrays.asList(mScanFilter), mScanSettings, mScanCallback);
            mBluetoothLeScanner.startScan(null, mScanSettings, mScanCallback);

            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    Log.i( TAG, "Scan Stopped");
                    ToastUtils.showToastInUiThread(getApplication(),R.string.Scan_Stopped);
                }
            }, SCAN_PERIOD);

        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            Log.i( TAG, "Scan Stopped");
            ToastUtils.showToastInUiThread(getApplication(),R.string.Scan_Stopped);
        }
    }

    /**
     * Starts beaconmanager and binds to instance.
     **/
    private void startBeaconDetect(){
        // Create instance of beacon manager.
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);
    }

    /**
    * BLE Scanner Callback.
    **/
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());

            // If result is empty or getDevice is null, then return.
            if (result == null)
                return;

            // We have data, lets grab some and pipe them to our lists which are connected to our custom adapter.
            String deviceAddress = result.getDevice().getAddress();
            BluetoothDevice btDevice = result.getDevice();
            String rssi = Integer.toString(result.getRssi());
            String other = Integer.toString(result.getScanRecord().getTxPowerLevel());

            // Adding data to lists.
            resultList.add(deviceAddress);
            rssiList.add(rssi);
            otherList.add(other);

            // Tell our adapter that we have new data to set.
            adapter.notifyDataSetChanged();

            // Code for if you want to connect to the device and start GATT profile...
            //BluetoothDevice btDevice = result.getDevice();
            //connectToDevice(btDevice);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public static byte[] getIdAsByte(java.util.UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    /**
     * Callback for beaconManger
     **/
    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setMonitorNotifier(new MonitorNotifier() {

            @Override
            public void didEnterRegion(Region region) {
                Log.e(TAG, "I just saw an beacon for the first time!");
                ToastUtils.showToastInUiThread(getApplication(),R.string.Beacon_Detected);

                try{
                    beaconManager.startRangingBeaconsInRegion(new Region(null, null, null, null));
                    beaconManager.setRangeNotifier(new RangeNotifier() {
                        @Override
                        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                            if (beacons.size() > 0) {
                                Log.i(TAG, "The first beacon I see is about " + beacons.iterator().next().getDistance()+" meters away.");
                            }
                        }
                    });
                }catch (RemoteException e){

                }
            }

            @Override
            public void didExitRegion(Region region) {
                Log.e(TAG, "I no longer see an beacon");
                ToastUtils.showToastInUiThread(getApplication(),R.string.Beacon_Lost);
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.e(TAG, "I have just switched from seeing/not seeing beacons: " + state);
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(new Region(getResources().getString(R.string.UUID), null, null, null));
        } catch (RemoteException e) {}
    }
}
