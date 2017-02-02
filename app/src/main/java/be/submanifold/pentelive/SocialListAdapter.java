package be.submanifold.pentelive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import be.submanifold.pentelive.liveGameRoom.LivePlayer;

/**
 * Created by waliedothman on 02/02/2017.
 */

public class SocialListAdapter  extends BaseExpandableListAdapter {


    List<LivePlayer> playersArray;

    private LayoutInflater inflater;

    private boolean following = false;

    public void setFollowing(boolean following) {
        this.following = following;
    }

    private Context ctx;

    public SocialListAdapter() {
        this.playersArray = new ArrayList<>();
        ctx = MyApplication.getContext();
    }
    public void setInflater(LayoutInflater inflater) {
        this.inflater = inflater;
    }
    public void setPlayersArray(List<LivePlayer> playersArray) { this.playersArray = playersArray; }
    public List<LivePlayer> getPlayersArray() { return playersArray; }


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
        if (following) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.dashboardgroup_layout, null);
            }
            convertView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.britishracinggreen));
            String title = ctx.getString(R.string.hold_to_unfollow);
            ((TextView) convertView.findViewById(R.id.textView)).setText(title);
        } else {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.empty, null);
            }
        }
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.social_row, null);
        }
        convertView.setBackgroundColor(Color.WHITE);
        LivePlayer player = playersArray.get(childPosition);

        ImageView imgVw = (ImageView) convertView.findViewById(R.id.imageView);
        imgVw.setVisibility(View.VISIBLE);
        imgVw.setAlpha(1f);
        Bitmap avatar = null;
        avatar = PentePlayer.avatars.get(player.getName());
        if (avatar != null) {
            imgVw.setImageBitmap(avatar);
        } else if (PentePlayer.loadAvatars) {
            imgVw.setAlpha(0.25f);
            imgVw.setImageResource(R.drawable.ic_action_android);
        } else {
            imgVw.setVisibility(View.GONE);
        }
        TextView nameTextView = ((TextView) convertView.findViewById(R.id.nameText));
        nameTextView.setText(player.coloredNameString(nameTextView.getLineHeight()));
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
    }
}
