package be.submanifold.pentelive.liveGameRoom;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.PentePlayer;
import be.submanifold.pentelive.R;

/**
 * Created by waliedothman on 11/04/16.
 */
public class LobbyListAdapter extends BaseExpandableListAdapter {

    List<LiveGameRoom> rooms = new ArrayList<>();

    Activity activity;

    private LayoutInflater inflater;
    private Context ctx = MyApplication.getContext();

    public LobbyListAdapter() {
    }
    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }
    public void setRooms(List<LiveGameRoom> rooms) {
        this.rooms = rooms;
    }



    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return rooms.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return null;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }


    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.dashboardgroup_layout, null);
        }
        convertView.setBackgroundColor(ContextCompat.getColor(activity, R.color.britishracinggreen));
        ((TextView) convertView.findViewById(R.id.textView)).setText(activity.getString(R.string.rooms) + " (" + rooms.size() + ")");

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.liveroom_row_layout, null);
        }
        convertView.setBackgroundColor(Color.WHITE);

        TextView nameTextView = ((TextView) convertView.findViewById(R.id.nameText));

        nameTextView.setText(rooms.get(childPosition).getRoomText(nameTextView.getLineHeight()));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    @Override
    public void onGroupCollapsed(int groupPosition) {
        super.onGroupCollapsed(groupPosition);
    }
    @Override
    public void onGroupExpanded(int groupPosition) {
        super.onGroupExpanded(groupPosition);
    }

    public void updateList(){
        notifyDataSetChanged();

        if (PentePlayer.mShowAds) {
            ((AdView) activity.findViewById(R.id.adView)).setVisibility(View.VISIBLE);
            ((AdView) activity.findViewById(R.id.adView)).loadAd(new AdRequest.Builder().build());
        } else {
            ((AdView) activity.findViewById(R.id.adView)).setVisibility(View.GONE);
        }
        System.out.println("updateList");
    }
}
