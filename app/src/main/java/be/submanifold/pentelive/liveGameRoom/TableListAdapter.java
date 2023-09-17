package be.submanifold.pentelive.liveGameRoom;

import android.content.Context;

import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.R;

/**
 * Created by waliedothman on 08/01/2017.
 */

public class TableListAdapter extends BaseExpandableListAdapter {

    Map<Integer, Table> tables;
    List<Table> tablesArray;
    private String roomName;
    //    PentePlayer playerData;
    private LayoutInflater inflater;
    private LiveGameRoomActivity activity;
    private Context ctx;

    public TableListAdapter(Map<Integer, Table> tables, String roomName, LiveGameRoomActivity activity) {
        this.tables = tables;
        this.tablesArray = new ArrayList<>(tables.values());
        ctx = MyApplication.getContext();
        this.roomName = roomName;
        this.activity = activity;
    }

    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return tablesArray.size();
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
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }


    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.live_table_header, null);
        }
        convertView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.britishracinggreen));
        String title = roomName + " (" + tablesArray.size() + ")";
        ((TextView) convertView.findViewById(R.id.textView)).setText(title);
        ((Button) convertView.findViewById(R.id.newTableButton)).setOnClickListener(view -> activity.sendEvent("{\"dsgJoinTableEvent\":{\"table\":-1,\"time\":0}}"));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.live_table_row, null);
        }
        Table table = tablesArray.get(childPosition);
        if (table.isOpen()) {
            ((ImageView) convertView.findViewById(R.id.imageView)).setVisibility(View.GONE);
        } else {
            ((ImageView) convertView.findViewById(R.id.imageView)).setVisibility(View.VISIBLE);
        }
        convertView.setBackgroundColor(table.getGameColor());
        ((TextView) convertView.findViewById(R.id.gameNameText)).setText(table.getGameName());
        TextView seatsTextView = ((TextView) convertView.findViewById(R.id.seatsText));
        if (table.getSeats().size() > 0) {
            seatsTextView.setVisibility(View.VISIBLE);
            seatsTextView.setText(table.getSeatsText(seatsTextView.getLineHeight()));
        } else {
            seatsTextView.setVisibility(View.GONE);
        }
        TextView settingsTextView = ((TextView) convertView.findViewById(R.id.settingsText));
        settingsTextView.setText(table.getSettingsText());
        TextView watchingTextView = ((TextView) convertView.findViewById(R.id.watchingText));
        watchingTextView.setText(table.getWatchingText(watchingTextView.getLineHeight()));
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

    public void updateList() {
        this.tablesArray = new ArrayList<>(tables.values());
        notifyDataSetChanged();
    }

    public List<Table> getTablesArray() {
        return tablesArray;
    }

}
