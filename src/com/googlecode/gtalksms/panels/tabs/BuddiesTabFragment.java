package com.googlecode.gtalksms.panels.tabs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.panels.tools.AutoClickEditorActionListener;
import com.googlecode.gtalksms.xmpp.XmppBuddies;
import com.googlecode.gtalksms.xmpp.XmppFriend;

public class BuddiesTabFragment extends SherlockFragment {
    private ListView mBuddiesListView;
    private EditText mEditTextBuddy;
    private Button mButtonAdd;
    private BuddyAdapter mCurrentBuddyAdapter;
    private final ArrayList<Buddy> mAdapterArray = new ArrayList<Buddy>();
    private final TreeMap<String, Buddy> mFriends = new TreeMap<String, Buddy>();
    private SettingsManager mSettingsMgr;

    @Override
    public void onResume() {
        super.onResume();
        updateBuddiesList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_buddies, container, false);

        mSettingsMgr = SettingsManager.getSettingsManager(view.getContext());
        mEditTextBuddy = (EditText) view.findViewById(R.id.editTextBuddyName);
        mButtonAdd = (Button) view.findViewById(R.id.buttonBuddyAdd);
        mBuddiesListView = (ListView) view.findViewById(R.id.ListViewBuddies);

        mEditTextBuddy.setOnEditorActionListener(new AutoClickEditorActionListener(mButtonAdd));

        mButtonAdd.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Button button = (Button) v;
                button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

                String name = mEditTextBuddy.getText().toString();
                if (!name.equals("")) {
                    XmppBuddies.getInstance(getActivity().getBaseContext()).addFriend(name);
                    mEditTextBuddy.setText("");
                    mEditTextBuddy.setHint(R.string.panel_buddies_add_message);
                }
            }
        });

        mBuddiesListView.setAdapter(mCurrentBuddyAdapter = new BuddyAdapter(getSherlockActivity(), R.layout.tab_buddies_item, mAdapterArray));
        mCurrentBuddyAdapter.sort(new Comparator<Buddy>() {
            public int compare(Buddy b1, Buddy b2) {
                return b1.getName().compareToIgnoreCase(b2.getName());
            }
        });

        return view;
    }

    public void updateBuddy(String userId, String userFullId, String name, String statusMsg, int state) {
        if (userId == null) {
            return;
        }

        Buddy buddy;
        if (!mFriends.containsKey(userId)) {
            buddy = new Buddy(userId, name, statusMsg, state);
            mFriends.put(userId, buddy);
            mAdapterArray.add(buddy);
        } else {
            buddy = mFriends.get(userId);
        }

        if (state == XmppFriend.OFFLINE) {
            buddy.removeLocation(userFullId);
            state = buddy.getFirstState(state);
        } else if (userFullId != null) {
            buddy.addOrUpdateLocation(userFullId, statusMsg, state);
        }

        if (name != null) {
            buddy.setName(name);
        }

        buddy.setStatusMsg(statusMsg);
        buddy.setState(state);

        updateBuddiesList();
    }

    private void updateBuddiesList() {
        if (mCurrentBuddyAdapter != null) {
            mCurrentBuddyAdapter.notifyDataSetChanged();
        }
    }

    private int getStateRes(int state) {
        switch (state) {
            case XmppFriend.AWAY:
            case XmppFriend.EXAWAY:
                return R.drawable.buddy_away;
            case XmppFriend.BUSY:
                return R.drawable.buddy_busy;
            case XmppFriend.FFC:
            case XmppFriend.ONLINE:
                return R.drawable.buddy_available;
        }
        return R.drawable.buddy_offline;
    }

    public class Buddy {
        class Location {
            final String Name;
            final String Resource;
            final String StatusMsg;
            final int State;

            Location(String name, String statusMsg, int state) {
                Name = name;
                StatusMsg = statusMsg;
                State = state;
                int index = Name.indexOf('/');
                Resource = (index == -1 || index + 2 > Name.length()) ? Name : Name.substring(index + 1);
            }
        }

        private String mName;
        private String mStatusMsg;
        private String mUserId;
        private int mState;

        final TreeMap<String, Location> mLocations = new TreeMap<String, Location>();

        public Buddy(String userId, String name, String statusMsg, int state) {
            mUserId = userId;
            mName = name;
            mStatusMsg = statusMsg;
            mState = state;
        }

        public String getName() {
            return mName == null ? mUserId : mName;
        }

        public String getUserId() {
            return mUserId;
        }

        public String getStatusMsg() {
            return mStatusMsg;
        }

        public int getState() {
            return mState;
        }

        public void setName(String val) {
            mName = val;
        }

        public void setUserId(String val) {
            mUserId = val;
        }

        public void setStatusMsg(String val) {
            mStatusMsg = val;
        }

        public void setState(int val) {
            mState = val;
        }

        public void addOrUpdateLocation(String fullId, String statusMsg, int state) {
            mLocations.put(fullId, new Location(fullId, statusMsg, state));
        }

        public void removeLocation(String fullId) {
            if (fullId != null) {
                mLocations.remove(fullId);
            }
        }

        public int getFirstState(int state) {
            if (mLocations.isEmpty()) {
                return state;
            }
            return mLocations.get(mLocations.firstKey()).State;
        }

        public TreeMap<String, Location> getLocations() {
            return mLocations;
        }
    }

    public class BuddyAdapter extends ArrayAdapter<Buddy> {
        private final LayoutInflater mInflater;

        public BuddyAdapter(Activity activity, int textViewResourceId, ArrayList<Buddy> buddies) {
            super(activity, textViewResourceId, buddies);
            mInflater = activity.getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View row = convertView;
            if (row == null) {
                row = mInflater.inflate(R.layout.tab_buddies_item, parent, false);
            }

            final Buddy buddy = getItem(position);
            LinearLayout buddyView = (LinearLayout) row.findViewById(R.id.buddyInfo);
            CheckBox starCheck = (CheckBox) row.findViewById(R.id.buddyStar);
            TextView nameView = (TextView) row.findViewById(R.id.buddyName);
            TextView statusView = (TextView) row.findViewById(R.id.buddyStatus);
            ImageView stateView = (ImageView) row.findViewById(R.id.buddyState);
            ImageView deleteView = (ImageView) row.findViewById(R.id.buddyToDelete);

            starCheck.setChecked(mSettingsMgr.getNotifiedAddresses().contains(buddy.getUserId()));
            starCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mSettingsMgr.getNotifiedAddresses().add(buddy.getUserId());
                    } else {
                        mSettingsMgr.getNotifiedAddresses().remove(buddy.getUserId());
                    }
                }
            });
            nameView.setText(buddy.getName());

            statusView.setText(buddy.getStatusMsg());
            stateView.setImageResource(getStateRes(buddy.getState()));

            buddyView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ArrayList<HashMap<String, String>> locations = new ArrayList<HashMap<String, String>>();
                    for (Buddy.Location location : buddy.getLocations().values()) {
                        HashMap<String, String> map = new HashMap<String, String>();
                        map.put("name", location.Resource);
                        map.put("status", location.StatusMsg);
                        map.put("state", String.valueOf(getStateRes(location.State)));
                        locations.add(map);
                    }

                    final Dialog dialog = new Dialog(getSherlockActivity());
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.setContentView(R.layout.tab_buddies_dialog);

                    final EditText buddyName = (EditText) dialog.findViewById(R.id.editTextBuddyName);
                    buddyName.setText(buddy.getName());

                    TextView userId = (TextView) dialog.findViewById(R.id.buddyUserId);
                    userId.setText(buddy.getUserId());

                    ListView lvLocations = (ListView) dialog.findViewById(R.id.ListViewLocations);
                    lvLocations.setAdapter(new SimpleAdapter(getSherlockActivity().getBaseContext(), locations, R.layout.tab_buddies_location, new String[] { "state", "name",
                            "status" }, new int[] { R.id.buddyState, R.id.buddyName, R.id.buddyStatus }));

                    Button buttonClose = (Button) dialog.findViewById(R.id.buttonOk);
                    buttonClose.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    Button buttonInvite = (Button) dialog.findViewById(R.id.buttonInvite);
                    buttonInvite.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            XmppBuddies.getInstance(getActivity().getBaseContext()).addFriend(buddy.getUserId());
                            dialog.dismiss();
                        }
                    });

                    dialog.setOnDismissListener(new OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            String name = buddyName.getText().toString();
                            if (!name.equals(buddy.getName())) {
                                XmppBuddies.getInstance(getActivity().getBaseContext()).renameFriend(buddy.getUserId(), name);
                                buddy.setName(name);
                                updateBuddiesList();
                            }
                        }
                    });

                    dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                    dialog.show();
                }
            });

            deleteView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ImageView button = (ImageView) v;
                    button.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);

                    new AlertDialog.Builder(getActivity()).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.panel_buddies_popup_button_delete_title)
                            .setMessage(String.format(getString(R.string.panel_buddies_popup_button_delete), buddy.getName()))
                            .setPositiveButton(R.string.panel_buddies_popup_button_ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mSettingsMgr.getNotifiedAddresses().remove(buddy.getUserId());
                                    mFriends.remove(buddy.getUserId());
                                    mAdapterArray.remove(buddy);
                                    updateBuddiesList();

                                    new Thread(new Runnable() {
                                        public void run() {
                                            XmppBuddies.getInstance(getActivity().getBaseContext()).removeFriend(buddy.getUserId());
                                        }
                                    }).start();
                                }
                            }).setNegativeButton(R.string.panel_buddies_popup_button_cancel, null).show();
                }
            });
            return row;
        }
    }
}