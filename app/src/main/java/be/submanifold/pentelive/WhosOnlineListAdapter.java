package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by waliedothman on 01/11/2016.
 */

public class WhosOnlineListAdapter extends BaseExpandableListAdapter {

    Map<String, List<KothPlayer>> onlinePlayers;
    public List<String> sections;
    Activity activity;

    PentePlayer playerData;
    private LayoutInflater inflater;

    Context ctx = MyApplication.getContext();

    public WhosOnlineListAdapter(PentePlayer player) {
        this.playerData = player;
    }
    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }
    public void setOnlinePlayers(Map<String, List<KothPlayer>> onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
        sections = new ArrayList<>(onlinePlayers.keySet());
        int index = sections.indexOf("Mobile");
        if (index > -1) {
            sections.remove(index);
            sections.add("Mobile");
        }
        index = sections.indexOf("Website");
        if (index > -1) {
            sections.remove(index);
            sections.add("Website");
        }
    }



    @Override
    public int getGroupCount() {
        return onlinePlayers.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return onlinePlayers.get(sections.get(groupPosition)).size();
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
        convertView.setBackgroundColor(Color.GRAY);
        ((TextView) convertView.findViewById(R.id.textView)).setText(sections.get(groupPosition) + " (" + onlinePlayers.get(sections.get(groupPosition)).size() + ")");

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.dashboardrow_layout, null);
        }
        convertView.findViewById(R.id.acceptButton).setVisibility(View.GONE);
        convertView.findViewById(R.id.dismissButton).setVisibility(View.GONE);
        convertView.findViewById(R.id.declineButton).setVisibility(View.GONE);
        convertView.findViewById(R.id.imageView).setVisibility(View.GONE);
        convertView.setBackgroundColor(Color.WHITE);

        ((TextView) convertView.findViewById(R.id.nameText)).setText("");
        ((TextView) convertView.findViewById(R.id.detailText)).setText("");
        ((TextView) convertView.findViewById(R.id.ratingText)).setText("");
        ((TextView) convertView.findViewById(R.id.ratingText)).setVisibility(View.VISIBLE);
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setVisibility(View.VISIBLE);
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setText("");
        String mainText, ratingText = "", detailText = "";
        TextView nameTextView = ((TextView) convertView.findViewById(R.id.nameText));
        TextView detailTextView = ((TextView) convertView.findViewById(R.id.detailText));

        int crown = 0, color = 0;
        KothPlayer player = null;
        player = onlinePlayers.get(sections.get(groupPosition)).get(childPosition);
        ImageView imgVw = (ImageView) convertView.findViewById(R.id.imageView);
        imgVw.setVisibility(View.VISIBLE);
        imgVw.setAlpha(1f);
        if (PentePlayer.loadAvatars){
            Bitmap avatar = null;
            avatar = PentePlayer.avatars.get(player.getName());
            if (avatar == null) {
                imgVw.setAlpha(0.25f);
                imgVw.setImageResource(R.drawable.ic_action_android);
            } else {
                imgVw.setImageBitmap(avatar);
            }
        } else {
            imgVw.setVisibility(View.GONE);
        }

        mainText  = player.getName();
        crown = player.getCrown();
        color = player.getColor();
        detailText = ctx.getString(R.string.total_games, player.getLastGame());
        ratingText = player.getRating();
        SpannableStringBuilder sb = new SpannableStringBuilder(mainText);
        if (color != 0) {
            ForegroundColorSpan fcs = new ForegroundColorSpan(color);
            StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
            sb.setSpan(fcs, 0, mainText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            sb.setSpan(bss, 0, mainText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        Drawable crownIcon = null;
        switch (crown) {
            case 1:
                crownIcon = ContextCompat.getDrawable(activity, R.drawable.crown);
                break;
            case 2:
                crownIcon = ContextCompat.getDrawable(activity, R.drawable.scrown);
                break;
            case 3:
                crownIcon = ContextCompat.getDrawable(activity, R.drawable.bcrown);
                break;
            default:
                if (crown > 3) {
                    int resourceId = ctx.getResources().getIdentifier("kothcrown"+(crown-3),"drawable", ctx.getPackageName());
                    crownIcon = ContextCompat.getDrawable(MyApplication.getContext(), resourceId);
                }
                break;
        }
        if (crownIcon != null && crown > 0) {
            crownIcon.setBounds(0, 0, nameTextView.getLineHeight()*2/3,nameTextView.getLineHeight()*2/3);
            sb.append("   ").setSpan(new ImageSpan(crownIcon, ImageSpan.ALIGN_BASELINE), sb.length() - 1, sb.length(), 0);
        }
        detailTextView.setText(detailText);
        nameTextView.setText(sb);
        ((TextView) convertView.findViewById(R.id.ratingText)).setText(ratingText);
        sb = new SpannableStringBuilder("\u25A0");
        int ratingInt = Integer.parseInt(ratingText);
        ForegroundColorSpan ratingColor = null;new ForegroundColorSpan(color);
        if (ratingInt >= 1900) {
            ratingColor = new ForegroundColorSpan(Color.RED);
        } else if (ratingInt >= 1700) {
            ratingColor = new ForegroundColorSpan(Color.rgb((int) (0.98*255), (int) (0.96*255) ,(int) (0.03*255)));
        } else if (ratingInt >= 1400) {
            ratingColor =  new ForegroundColorSpan(Color.BLUE);
        } else if (ratingInt >= 1000) {
            ratingColor =  new ForegroundColorSpan(Color.rgb(30,130,76));
        } else {
            ratingColor = new ForegroundColorSpan(Color.GRAY);
        }
        sb.setSpan(ratingColor, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setText(sb);

        if (player.isCanBeChallenged()) {
            convertView.setBackgroundColor(Color.rgb(222, 236, 222));
        }
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
