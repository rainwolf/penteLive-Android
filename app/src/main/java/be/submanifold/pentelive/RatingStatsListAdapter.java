package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;


/**
 * Created by waliedothman on 23/05/16.
 */
public class RatingStatsListAdapter extends BaseExpandableListAdapter {

    List<RatingStat> ratingStats;
    private LayoutInflater inflater;
    private Activity activity;

    private Context ctx = MyApplication.getContext();

    public RatingStatsListAdapter(List<RatingStat> ratingStats) {
        this.ratingStats = ratingStats;
    }
    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }

    @Override
    public int getGroupCount() {
        return 1;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.ratingStats.size();
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
        convertView.setBackgroundColor(Color.GRAY);
        ((TextView) convertView.findViewById(R.id.textView)).setText(ctx.getString(R.string.rating_stats));

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.rating_stats_row, null);
        }
        convertView.setBackgroundColor(Color.WHITE);
        RatingStat ratingStat = this.ratingStats.get(childPosition);
        int crown = ratingStat.getCrown();

        ((TextView) convertView.findViewById(R.id.nameText)).setText("");
        ((TextView) convertView.findViewById(R.id.detailText)).setText("");
        ((TextView) convertView.findViewById(R.id.ratingText)).setText("");
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setText("");
        String ratingText = ratingStat.getRating(), detailText = "";
        detailText = ctx.getString(R.string.last_total, ratingStat.getLastGame(), ratingStat.getTotalGames());
        ((TextView) convertView.findViewById(R.id.detailText)).setText(detailText);
        ((TextView) convertView.findViewById(R.id.nameText)).setText(ratingStat.getGame());
        ((TextView) convertView.findViewById(R.id.ratingText)).setText(ratingText);
        SpannableStringBuilder sb = new SpannableStringBuilder("\u25A0");
        int ratingInt = Integer.parseInt(ratingText);
        ForegroundColorSpan ratingColor = new ForegroundColorSpan(Color.BLACK);
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
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    @Override
    public void onGroupCollapsed(int groupPosition) {
//        super.onGroupCollapsed(groupPosition);
    }
    @Override
    public void onGroupExpanded(int groupPosition) {
//        super.onGroupExpanded(groupPosition);
    }

    public void updateList(){
        notifyDataSetChanged();
    }
}
