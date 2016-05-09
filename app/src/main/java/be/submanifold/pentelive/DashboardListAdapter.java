package be.submanifold.pentelive;

import android.app.Activity;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

/**
 * Created by waliedothman on 11/04/16.
 */
public class DashboardListAdapter extends BaseExpandableListAdapter {

    PentePlayer playerData;
    private LayoutInflater inflater;
    private Activity activity;

    public DashboardListAdapter(PentePlayer player) {
        this.playerData = player;
//        player.loadPlayer();
    }
    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }

    @Override
    public int getGroupCount() {
        return 6;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int count;
        switch (groupPosition) {
            case 0: count = playerData.getMessages().size(); break;
            case 1: count = playerData.getInvitations().size(); break;
            case 2: count = playerData.getActiveGames().size(); break;
            case 3: count = playerData.getPublicInvitations().size(); break;
            case 4: count = playerData.getSentInvitations().size(); break;
            case 5: count = playerData.getNonActiveGames().size(); break;
            default: count = 0; break;
        }
        return count;
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
        convertView.setBackgroundColor(ContextCompat.getColor(activity, R.color.brown));
        String title;
        switch (groupPosition) {
            case 0: title = "Messages (" + playerData.getMessages().size() + ")";
                for (Message message : playerData.getMessages()) {
                    if (message.getUnread().indexOf("unread") > -1) {
                        convertView.setBackgroundColor(Color.BLUE);
                        break;
                    }
                }
                break;
            case 1: title = "Invitations (" + playerData.getInvitations().size() + ")";
                if (playerData.getInvitations().size() > 0) {
                    convertView.setBackgroundColor(Color.BLUE);
                }
                break;
            case 2: title = "Active Games (" + playerData.getActiveGames().size() + ")";
                if (playerData.getActiveGames().size() > 0) {
                    convertView.setBackgroundColor(Color.BLUE);
                }
                break;
            case 3: title = "Public Invitations (" + playerData.getPublicInvitations().size() + ")"; break;
            case 4: title = "Invitations Sent (" + playerData.getSentInvitations().size() + ")"; break;
            case 5: title = "Non-Active Games (" + playerData.getNonActiveGames().size() + ")"; break;
            default: title = "uh-oh"; break;
        }
        ((TextView) convertView.findViewById(R.id.textView)).setText(title);
        String collapsedStr = "(+)";
        if (isExpanded) {
            collapsedStr = "(-)";
        }
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

        ((TextView) convertView.findViewById(R.id.nameText)).setText("");
        ((TextView) convertView.findViewById(R.id.detailText)).setText("");
        ((TextView) convertView.findViewById(R.id.ratingText)).setText("");
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setText("");
        String mainText, ratingText = "", detailText = "";
        int crown = 0, color = 0;
        Game game = null;
        Message message = null;
        switch (groupPosition) {
            case 0: message = playerData.getMessages().get(childPosition);
                break;
            case 1: mainText = playerData.getInvitations().get(childPosition).getOpponentName();
                game = playerData.getInvitations().get(childPosition);
                break;
            case 2: mainText = playerData.getActiveGames().get(childPosition).getOpponentName();
                game = playerData.getActiveGames().get(childPosition);
                break;
            case 3: mainText = playerData.getPublicInvitations().get(childPosition).getOpponentName();
                game = playerData.getPublicInvitations().get(childPosition);
                break;
            case 4: mainText = playerData.getSentInvitations().get(childPosition).getOpponentName();
                game = playerData.getSentInvitations().get(childPosition);
                break;
            case 5: mainText = playerData.getNonActiveGames().get(childPosition).getOpponentName();
                game = playerData.getNonActiveGames().get(childPosition);
                break;
            default: mainText = "uh-oh";
                detailText = "uh-oh";
                break;
        }
        if (groupPosition > 0) {
            mainText = game.getOpponentName();
            crown = game.getCrown();
            color = game.getNameColor();
            detailText = game.getGameType() + " (" + game.getRatedNot() + ") - " + game.getRemainingTime();
            ratingText = game.getOpponentRating();
        } else {
            mainText = message.getAuthor();
            detailText = message.getSubject();
            crown = message.getCrown();
            color = message.getNameColor();
        }
        if (groupPosition == 2 || groupPosition == 5) {
            convertView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
            ImageView imgVw = (ImageView) convertView.findViewById(R.id.imageView);
            if (game.getMyColor().indexOf("white") > -1) {
                imgVw.setImageResource(R.drawable.white_stone);
            } else {
                imgVw.setImageResource(R.drawable.black_stone);
            }
        } else if (groupPosition == 0 && message.unread()) {
            convertView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
            ImageView imgVw = (ImageView) convertView.findViewById(R.id.imageView);
            imgVw.setImageResource(R.drawable.unread);
        } else {
            convertView.findViewById(R.id.imageView).setVisibility(View.GONE);
        }
        SpannableStringBuilder sb = new SpannableStringBuilder(mainText);
        if (color != 0) {
            ForegroundColorSpan fcs = new ForegroundColorSpan(color);
            StyleSpan bss = new StyleSpan(android.graphics.Typeface.BOLD);
            sb.setSpan(fcs, 0, mainText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            sb.setSpan(bss, 0, mainText.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (crown > 0) {
            if (crown == 4) {
                sb.append(" \u2655");
            } else if (crown == 1) {
                sb.append(" \u2654");
            } else {
                sb.append(" \u265B");
            }
        }
        ((TextView) convertView.findViewById(R.id.detailText)).setText(detailText);
        ((TextView) convertView.findViewById(R.id.nameText)).setText(sb);
        if (mainText.indexOf("Anyone") == -1) {
            ((TextView) convertView.findViewById(R.id.ratingText)).setText(ratingText);
            if (groupPosition > 0) {
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
            }
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }


    @Override
    public void onGroupCollapsed(int groupPosition) {
        switch (groupPosition) {
            case 0: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_MESSAGES_COLLAPSED_KEY, true);
                break;
            case 1: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_INVITATIONS_COLLAPSED_KEY, true);
                break;
            case 2: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_ACTIVEGAMES_COLLAPSED_KEY, true);
                break;
            case 3: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_PUBLICINVITATIONS_COLLAPSED_KEY, true);
                break;
            case 4: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_SENTINVITATIONS_COLLAPSED_KEY, true);
                break;
            case 5: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_NONACTIVEGAMES_COLLAPSED_KEY, true);
                break;
        }
        super.onGroupCollapsed(groupPosition);
    }
    @Override
    public void onGroupExpanded(int groupPosition) {
        switch (groupPosition) {
            case 0: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_MESSAGES_COLLAPSED_KEY, false);
                break;
            case 1: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_INVITATIONS_COLLAPSED_KEY, false);
                break;
            case 2: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_ACTIVEGAMES_COLLAPSED_KEY, false);
                break;
            case 3: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_PUBLICINVITATIONS_COLLAPSED_KEY, false);
                break;
            case 4: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_SENTINVITATIONS_COLLAPSED_KEY, false);
                break;
            case 5: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_NONACTIVEGAMES_COLLAPSED_KEY, false);
                break;
        }
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
