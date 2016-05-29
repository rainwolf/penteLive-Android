package be.submanifold.pentelive;

import android.app.Activity;
import android.content.Intent;
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

/**
 * Created by waliedothman on 11/04/16.
 */
public class DashboardListAdapter extends BaseExpandableListAdapter {

    public static final int MESSAGESGROUP = 0;
    public static final int INVITATIONSGROUP = 1;
    public static final int ACTIVEGAMESGROUP = 2;
    public static final int PUBLICINVITATIONSGROUP = 3;
    public static final int SENTINVITATIONSGROUP = 5;
    public static final int NONACTIVEGAMESGROUP = 6;
    public static final int TOURNAMENTGROUP = 4;


    PentePlayer playerData;
    private LayoutInflater inflater;
    private Activity activity;
    private boolean asked2GetStarted;

    public DashboardListAdapter(PentePlayer player) {
        this.playerData = player;
        asked2GetStarted = false;
//        player.loadPlayer();
    }
    public void setInflater(LayoutInflater inflater, Activity activity) {
        this.inflater = inflater;
        this.activity = activity;
    }

    @Override
    public int getGroupCount() {
        return 7;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        int count;
        switch (groupPosition) {
            case MESSAGESGROUP: count = playerData.getMessages().size(); break;
            case INVITATIONSGROUP: count = playerData.getInvitations().size(); break;
            case ACTIVEGAMESGROUP: count = playerData.getActiveGames().size(); break;
            case PUBLICINVITATIONSGROUP: count = playerData.getPublicInvitations().size(); break;
            case SENTINVITATIONSGROUP: count = playerData.getSentInvitations().size(); break;
            case NONACTIVEGAMESGROUP: count = playerData.getNonActiveGames().size(); break;
            case TOURNAMENTGROUP: count = playerData.getTournaments().size(); break;
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
        convertView.setBackgroundColor(ContextCompat.getColor(activity, R.color.britishracinggreen));
        String title;
        switch (groupPosition) {
            case MESSAGESGROUP: title = "Messages (" + playerData.getMessages().size() + ")";
                for (Message message : playerData.getMessages()) {
                    if (message.getUnread().indexOf("unread") > -1) {
                        convertView.setBackgroundColor(Color.BLUE);
                        break;
                    }
                }
                break;
            case INVITATIONSGROUP: title = "Invitations (" + playerData.getInvitations().size() + ")";
                if (playerData.getInvitations().size() > 0) {
                    convertView.setBackgroundColor(Color.BLUE);
                }
                break;
            case ACTIVEGAMESGROUP: title = "Active Games (" + playerData.getActiveGames().size() + ")";
                if (playerData.getActiveGames().size() > 0) {
                    convertView.setBackgroundColor(Color.BLUE);
                }
                break;
            case PUBLICINVITATIONSGROUP: title = "Public Invitations (" + playerData.getPublicInvitations().size() + ")"; break;
            case SENTINVITATIONSGROUP: title = "Invitations Sent (" + playerData.getSentInvitations().size() + ")"; break;
            case NONACTIVEGAMESGROUP: title = "Non-Active Games (" + playerData.getNonActiveGames().size() + ")"; break;
            case TOURNAMENTGROUP: title = "Tournaments (" + playerData.getTournaments().size() + ")"; break;
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
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setVisibility(View.VISIBLE);
        ((TextView) convertView.findViewById(R.id.ratingColorText)).setText("");
        String mainText, ratingText = "", detailText = "";
        int crown = 0, color = 0;
        Game game = null;
        Message message = null;
        Tournament tournament = null;
        switch (groupPosition) {
            case MESSAGESGROUP: message = playerData.getMessages().get(childPosition);
                break;
            case INVITATIONSGROUP: game = playerData.getInvitations().get(childPosition);
                break;
            case ACTIVEGAMESGROUP: game = playerData.getActiveGames().get(childPosition);
                break;
            case PUBLICINVITATIONSGROUP: game = playerData.getPublicInvitations().get(childPosition);
                break;
            case SENTINVITATIONSGROUP: game = playerData.getSentInvitations().get(childPosition);
                break;
            case NONACTIVEGAMESGROUP: game = playerData.getNonActiveGames().get(childPosition);
                break;
            case TOURNAMENTGROUP: tournament = playerData.getTournaments().get(childPosition);
                break;
            default: mainText = "uh-oh";
                detailText = "uh-oh";
                break;
        }
        if (groupPosition == MESSAGESGROUP) {
            mainText = message.getAuthor();
            detailText = message.getSubject();
            crown = message.getCrown();
            color = message.getNameColor();
        } else if (groupPosition == TOURNAMENTGROUP) {
            ((TextView) convertView.findViewById(R.id.ratingColorText)).setVisibility(View.GONE);
            mainText = "\u2B24  " + tournament.getName();
            if (tournament.getTournamentState().equals("1")) {
                detailText = "Registration is open until " + tournament.getDate();
            } else if (tournament.getTournamentState().equals("2")) {
                detailText = "Registration closed. Start: " + tournament.getDate();
            } else {
                detailText = "Tournament started. Current round: " + tournament.getRound();
            }
            ratingText = "(" + tournament.getGame() + ")";
            crown = 0;
            color = 0;
        } else {
            mainText = game.getOpponentName();
            crown = game.getCrown();
            color = game.getNameColor();
            detailText = game.getGameType() + " (" + game.getRatedNot() + ") - " + game.getRemainingTime();
            ratingText = game.getOpponentRating();
        }
        if (groupPosition == ACTIVEGAMESGROUP || groupPosition == NONACTIVEGAMESGROUP) {
            convertView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
            ImageView imgVw = (ImageView) convertView.findViewById(R.id.imageView);
            if (game.getMyColor().indexOf("white") > -1) {
                imgVw.setImageResource(R.drawable.white_stone);
            } else {
                imgVw.setImageResource(R.drawable.black_stone);
            }
        } else if (groupPosition == MESSAGESGROUP && message.unread()) {
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
        if (groupPosition == TOURNAMENTGROUP) {
            ForegroundColorSpan fcs = new ForegroundColorSpan(Color.GREEN);
            if (tournament.getTournamentState().equals("2")) {
                fcs = new ForegroundColorSpan(ContextCompat.getColor(activity, R.color.orange));
            }
            sb.setSpan(fcs, 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        TextView nameTextView = ((TextView) convertView.findViewById(R.id.nameText));
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
        ((TextView) convertView.findViewById(R.id.detailText)).setText(detailText);
        nameTextView.setText(sb);
        if (mainText.indexOf("Anyone") == -1) {
            ((TextView) convertView.findViewById(R.id.ratingText)).setText(ratingText);
            if (groupPosition != MESSAGESGROUP && groupPosition != TOURNAMENTGROUP) {
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
            case MESSAGESGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_MESSAGES_COLLAPSED_KEY, true);
                break;
            case INVITATIONSGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_INVITATIONS_COLLAPSED_KEY, true);
                break;
            case ACTIVEGAMESGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_ACTIVEGAMES_COLLAPSED_KEY, true);
                break;
            case PUBLICINVITATIONSGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_PUBLICINVITATIONS_COLLAPSED_KEY, true);
                break;
            case SENTINVITATIONSGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_SENTINVITATIONS_COLLAPSED_KEY, true);
                break;
            case NONACTIVEGAMESGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_NONACTIVEGAMES_COLLAPSED_KEY, true);
                break;
            case TOURNAMENTGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_TOURNAMENTS_COLLAPSED_KEY, true);
                break;
        }
        super.onGroupCollapsed(groupPosition);
    }
    @Override
    public void onGroupExpanded(int groupPosition) {
        switch (groupPosition) {
            case MESSAGESGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_MESSAGES_COLLAPSED_KEY, false);
                break;
            case INVITATIONSGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_INVITATIONS_COLLAPSED_KEY, false);
                break;
            case ACTIVEGAMESGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_ACTIVEGAMES_COLLAPSED_KEY, false);
                break;
            case PUBLICINVITATIONSGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_PUBLICINVITATIONS_COLLAPSED_KEY, false);
                break;
            case SENTINVITATIONSGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_SENTINVITATIONS_COLLAPSED_KEY, false);
                break;
            case NONACTIVEGAMESGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_NONACTIVEGAMES_COLLAPSED_KEY, false);
                break;
            case TOURNAMENTGROUP: PrefUtils.saveBooleanToPrefs(activity, PrefUtils.PREFS_TOURNAMENTS_COLLAPSED_KEY, false);
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

        if (!asked2GetStarted && (playerData.getActiveGames().size() == 0) && (playerData.getNonActiveGames().size() == 0) && (playerData.getSentInvitations().size() == 0)) {
            asked2GetStarted = true;
            ((MainActivity) activity).ask2GetStarted();
        }
    }
}
