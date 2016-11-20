package be.submanifold.pentelive;

        import android.app.Activity;
        import android.content.Context;
        import android.content.Intent;
        import android.graphics.Bitmap;
        import android.graphics.Color;
        import android.graphics.drawable.Drawable;
        import android.support.design.widget.Snackbar;
        import android.support.v4.content.ContextCompat;
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

        import com.google.android.gms.ads.AdRequest;
        import com.google.android.gms.ads.AdView;

        import java.util.List;

        import static android.view.View.TEXT_ALIGNMENT_CENTER;

/**
 * Created by waliedothman on 11/04/16.
 */
public class KingOfTheHillListAdapter extends BaseExpandableListAdapter {

    List<List<KothPlayer>> hill;
    Activity activity;
    KingOfTheHill kothSummary;

    PentePlayer playerData;
    private LayoutInflater inflater;
    private Context ctx = MyApplication.getContext();

    public KingOfTheHillListAdapter(PentePlayer player) {
        this.playerData = player;
//        player.loadPlayer();
    }
    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }
    public void setHill(List<List<KothPlayer>> hill) {
        this.hill = hill;
    }

    public void setKothSummary(KingOfTheHill kothSummary) {
        this.kothSummary = kothSummary;
    }


    @Override
    public int getGroupCount() {
        return 1 + (hill == null?0:hill.size());
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition == 0) {
            return 1;
        }
        if (hill == null) {
            return 0;
        }
        return hill.get(groupPosition - 1).size();
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
        if (groupPosition > 0) {
            for (KothPlayer player : hill.get(groupPosition - 1) ) {
                if (PentePlayer.mPlayerName.equals(player.getName())) {
                    convertView.setBackgroundColor(ContextCompat.getColor(activity, R.color.orangeDash));
                    break;
                }
            }
        }
        String title;
        String collapsedStr = "(+)";
        switch (groupPosition) {
            case 0: title = ctx.getString(R.string.king_of_the_hill, kothSummary.getGame());
                collapsedStr = "(" + kothSummary.getNumPlayers() + ")";
                break;
            case 1: title = ctx.getString(R.string.top_of_the_hill);
                collapsedStr = "(" + hill.get(groupPosition - 1).size() + ")";
                break;
            default: title = ctx.getString(R.string.step, (hill.size() + 1 - groupPosition));
                collapsedStr = "(" + hill.get(groupPosition - 1).size() + ")";
                break;
        }
        ((TextView) convertView.findViewById(R.id.textView)).setText(title);
        ((TextView) convertView.findViewById(R.id.collapsedText)).setText(collapsedStr);

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
        if (groupPosition == 0) {
            if (childPosition == 0) {
                if (kothSummary.isMember()) {
                    nameTextView.setText(ctx.getString(R.string.leave_hill));
                } else {
                    nameTextView.setText(ctx.getString(R.string.join_hill));
                }
                detailTextView.setText(ctx.getString(R.string.hill_warning));
                ((TextView) convertView.findViewById(R.id.ratingText)).setVisibility(View.GONE);
                ((TextView) convertView.findViewById(R.id.ratingColorText)).setVisibility(View.GONE);
                return convertView;
            }
        }

        int crown = 0, color = 0;
        KothPlayer player = null;
        player = hill.get(groupPosition - 1).get(childPosition);
        ImageView imgVw = (ImageView) convertView.findViewById(R.id.imageView);
        imgVw.setVisibility(View.VISIBLE);
        imgVw.setAlpha(1f);
        if (player.getName().equals(PentePlayer.mPlayerName) && PentePlayer.avatars.get(player.getName())==null){
            imgVw.setImageResource(R.drawable.unread);
        } else if (PentePlayer.loadAvatars){
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
        detailText = ctx.getString(R.string.last_game_on, player.getLastGame());
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
            case 4:
                crownIcon = ContextCompat.getDrawable(activity, R.drawable.kothcrown);
                break;
        }
        if (crown > 0) {
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

        if (playerData.showAds()) {
            ((AdView) activity.findViewById(R.id.adView)).setVisibility(View.VISIBLE);
            ((AdView) activity.findViewById(R.id.adView)).loadAd(new AdRequest.Builder().build());
        } else {
            ((AdView) activity.findViewById(R.id.adView)).setVisibility(View.GONE);
        }

    }
}
