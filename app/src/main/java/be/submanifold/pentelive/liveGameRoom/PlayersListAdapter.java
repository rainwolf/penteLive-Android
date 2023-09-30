package be.submanifold.pentelive.liveGameRoom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.submanifold.pentelive.MyApplication;
import be.submanifold.pentelive.PentePlayer;
import be.submanifold.pentelive.R;

/**
 * Created by waliedothman on 08/01/2017.
 */

public class PlayersListAdapter extends BaseExpandableListAdapter {

    Map<String, LivePlayer> players;
    List<LivePlayer> playersArray;
    int game = 1;
    private final String roomName;
    //    PentePlayer playerData;
    private LayoutInflater inflater;

    private final Context ctx;

    public PlayersListAdapter(Map<String, LivePlayer> players, String roomName, int game) {
        this.players = players;
        this.playersArray = new ArrayList<>(players.values());
        ctx = MyApplication.getContext();
        this.roomName = roomName;
        this.game = game;
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
        return playersArray.size();
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
            convertView = inflater.inflate(R.layout.dashboardgroup_layout, null);
        }
        convertView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.britishracinggreen));
        String title = roomName + " (" + playersArray.size() + ")";
        ((TextView) convertView.findViewById(R.id.textView)).setText(title);
//        String collapsedStr = "(+)";
//        if (isExpanded) {
//            collapsedStr = "(-)";
//        }
//        ((TextView) convertView.findViewById(R.id.collapsedText)).setText(collapsedStr);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.live_player_row, null);
        }
        convertView.setBackgroundColor(Color.WHITE);
        LivePlayer player = playersArray.get(childPosition);

        ImageView imgVw = convertView.findViewById(R.id.imageView);
        imgVw.setVisibility(View.VISIBLE);
        imgVw.setAlpha(1f);
        Bitmap avatar = null;
        if (PentePlayer.loadAvatars && PentePlayer.avatars != null) {
            avatar = PentePlayer.avatars.get(player.getName());
        }
        if (avatar != null) {
            imgVw.setImageBitmap(avatar);
        } else if (PentePlayer.loadAvatars) {
            imgVw.setAlpha(0.25f);
            imgVw.setImageResource(R.drawable.ic_action_android);
        } else {
            imgVw.setVisibility(View.GONE);
        }
        TextView nameTextView = convertView.findViewById(R.id.nameText);
        nameTextView.setText(player.coloredNameString(nameTextView.getLineHeight()));
        int rating = player.getRating(game);
        ((TextView) convertView.findViewById(R.id.ratingText)).setText("" + rating);
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setText(player.coloredRatingSquare(rating));
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
        this.playersArray = new ArrayList<>(players.values());
        notifyDataSetChanged();
    }
}
