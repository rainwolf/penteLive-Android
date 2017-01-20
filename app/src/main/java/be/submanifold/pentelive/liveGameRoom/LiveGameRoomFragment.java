package be.submanifold.pentelive.liveGameRoom;

import be.submanifold.pentelive.*;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link LiveGameRoomFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link LiveGameRoomFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LiveGameRoomFragment extends Fragment {

    private LiveGameRoomActivity activity;
    private PlayersListAdapter playersListAdapter;
    private TableListAdapter tableListAdapter;
    private TextView mainRoomTextView;
    private Context ctx = MyApplication.getContext();

    private String roomName;

    private OnFragmentInteractionListener mListener;

    public LiveGameRoomFragment() {
        // Required empty public constructor
    }

    public static LiveGameRoomFragment newInstance(String roomName) {
        LiveGameRoomFragment fragment = new LiveGameRoomFragment();
        Bundle args = new Bundle();
        args.putString("roomName", roomName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomName = getArguments().getString("roomName");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_live_game_room, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ((TabLayout) getView().findViewById(R.id.segments)).addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getText() == getString(R.string.players)) {
                    ((ExpandableListView) getView().findViewById(R.id.playersList)).setVisibility(View.VISIBLE);
                    ((ExpandableListView) getView().findViewById(R.id.tablesList)).setVisibility(View.GONE);
                } else {
                    ((ExpandableListView) getView().findViewById(R.id.playersList)).setVisibility(View.GONE);
                    ((ExpandableListView) getView().findViewById(R.id.tablesList)).setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        activity = (LiveGameRoomActivity) getActivity();
        ExpandableListView expandableList = (ExpandableListView) getView().findViewById(R.id.playersList);
        playersListAdapter = new PlayersListAdapter(activity.tablesAndPlayers.players, roomName, 1);
        expandableList.setAdapter(playersListAdapter);
        playersListAdapter.setInflater(activity.getLayoutInflater());
        expandableList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                return true;
            }
        });
        expandableList.expandGroup(0);

        expandableList = (ExpandableListView) getView().findViewById(R.id.tablesList);
        tableListAdapter = new TableListAdapter(activity.tablesAndPlayers.tables, roomName, activity);
        expandableList.setAdapter(tableListAdapter);
        tableListAdapter.setInflater(activity.getLayoutInflater());
        expandableList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                return true;
            }
        });
        expandableList.expandGroup(0);
        expandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                int tableId = tableListAdapter.getTablesArray().get(i1).getId();
                activity.sendEvent("{\"dsgJoinTableEvent\":{\"table\":"+tableId+",\"time\":0}}");
                return true;
            }
        });



        if (PentePlayer.mShowAds) {
            ((AdView) getView().findViewById(R.id.adView)).loadAd(new AdRequest.Builder().build());
        } else {
            ((AdView) getView().findViewById(R.id.adView)).setVisibility(View.GONE);
        }

        mainRoomTextView = (TextView) getView().findViewById(R.id.mainRoomTextView);
        mainRoomTextView.setMovementMethod(new ScrollingMovementMethod());
        mainRoomTextView.setMovementMethod(LinkMovementMethod.getInstance());
        super.onViewCreated(view, savedInstanceState);

        activity.connectSocket();

        mainRoomTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                final EditText input = new EditText(activity);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);
                builder.setPositiveButton(activity.getString(R.string.send), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String m_Text = input.getText().toString();
                        if (!"".equals(m_Text)) {
                            String event = "{\"dsgTextMainRoomEvent\":{\"text\":\""+m_Text+"\", \"time\":0}}";
                            if (mListener != null) {
                                mListener.sendEvent(event);
                            }
                        }
                    }
                });
//                builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
                AlertDialog dlg = builder.create();
                dlg.show();
                dlg.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        });
    }

    public void updateMainRoom() {
        playersListAdapter.updateList();
        tableListAdapter.updateList();
        mainRoomTextView.setText(activity.tablesAndPlayers.mainRoomText);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void sendEvent(String event);
    }
}
