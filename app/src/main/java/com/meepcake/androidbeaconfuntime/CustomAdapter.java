package com.meepcake.androidbeaconfuntime;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<String>{
    private final String TAG = "Custom Adapter";
    //Decs
    List<String> macAddress;
    List<String> rssi;
    List<String> other;
    Context c;

    public CustomAdapter(Context context, List<String> macAddress,List<String> rssi,List<String> other) {
        super(context,R.layout.scan_listview_layout, macAddress);

        this.c=context;
        this.macAddress = macAddress;
        this.rssi = rssi;
        this.other = other;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View customView = inflater.inflate(R.layout.scan_listview_layout, parent, false);

        TextView mac_text = (TextView) customView.findViewById(R.id.mac_address);
        TextView rssi_text = (TextView) customView.findViewById(R.id.rssi);
        TextView other_text = (TextView) customView.findViewById(R.id.other);

        mac_text.setText("MAC Address : " + macAddress.get(position));
        rssi_text.setText("RSSI                : " + rssi.get(position));
        other_text.setText("TX Power        : " +  other.get(position));

        return customView;
    }
}
