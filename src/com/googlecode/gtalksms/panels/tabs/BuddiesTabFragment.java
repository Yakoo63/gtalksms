package com.googlecode.gtalksms.panels.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppBuddies;
import com.googlecode.gtalksms.xmpp.XmppFriend;

public class BuddiesTabFragment extends SherlockFragment {
    private ListView mBuddiesListView;
    private EditText mEditTextBuddy;
    private Button mButtonAdd;
    private ArrayList<HashMap<String, String>> mFriends = new ArrayList<HashMap<String, String>>();
   
    private OnItemLongClickListener mOnBuddyDeleted = new OnItemLongClickListener() {
        @SuppressWarnings("unchecked")
        public boolean onItemLongClick(AdapterView<?> a, View v, int position, long id) {
            HashMap<String, String> map = (HashMap<String, String>) mBuddiesListView.getItemAtPosition(position);
            
            // TODO make a separated thread
            if (XmppBuddies.getInstance(getActivity().getBaseContext()).removeFriend(map.get("userid")) == true ) {
                mFriends.remove(position);
                updateBuddiesList();
            }
            return true;
        }
    };
    
    private OnItemClickListener mOnBuddySelected = new OnItemClickListener() {
        @SuppressWarnings("unchecked")
        public void onItemClick(AdapterView<?> a, View v, int position, long id) {
            HashMap<String, String> map = (HashMap<String, String>) mBuddiesListView.getItemAtPosition(position);
            AlertDialog.Builder adb = new AlertDialog.Builder(getSherlockActivity());
            adb.setTitle(map.get("name"));
            
            String user = map.get("userid");
            StringBuilder sb = new StringBuilder(user);
            sb.append(Tools.LineSep);
            sb.append(Tools.LineSep);
            for (String key : map.keySet()) {
                try {
                    if (key.startsWith("location_")) {
                        sb.append(key.substring(10 + user.length()));
                        sb.append(": ");
                        sb.append(map.get(key));
                        sb.append(Tools.LineSep);
                    }
                } catch(Exception e) {
                    Log.e(Tools.LOG_TAG, "Failed to decode buddy name", e);
                }
            }
            adb.setMessage(sb.toString());
            adb.setPositiveButton("Ok", null);
            adb.show();
        }
    };
    
    @Override
    public void onResume() {
        super.onResume();
        updateBuddiesList();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_buddies, container, false);
        
        mEditTextBuddy = (EditText) view.findViewById(R.id.editTextBuddyName);
        mButtonAdd = (Button) view.findViewById(R.id.buttonBuddyAdd);
        mBuddiesListView = (ListView) view.findViewById(R.id.ListViewBuddies);
        
        mBuddiesListView.setOnItemClickListener(mOnBuddySelected);
        mBuddiesListView.setOnItemLongClickListener(mOnBuddyDeleted);
        mButtonAdd.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                XmppBuddies.getInstance(getActivity().getBaseContext()).addFriend(mEditTextBuddy.getText().toString());
            }
        });
        
        return view;
    }
    
    public void updateBuddy(String userId, String userFullId, String name, String status, int stateInt) {
        String stateImg = getStateImg(stateInt);

        boolean exist = false;

        for (HashMap<String, String> map : mFriends) {
            if (map.get("userid").equals(userId)) {
                exist = true;                          
                if (stateInt == XmppFriend.OFFLINE) {
                    map.remove("location_" + userFullId);
                    
                    for (String key : map.keySet()) {
                        if (key.startsWith("location_")) {
                            try {
                                stateImg = getStateImg(stateInt);
                                break; 
                            } catch (Exception e) {}
                        }
                    }
                } else if (userFullId != null) {
                    map.put("location_" + userFullId, XmppFriend.stateToString(stateInt));
                }
                map.put("state", stateImg);
                map.put("status", status);
                break;
            }
        }

        if (!exist) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("name", name);
            map.put("status", status);
            map.put("userid", userId);
            map.put("state", stateImg);
            if (userFullId != null && stateInt != XmppFriend.OFFLINE) {
                map.put("location_" + userFullId, XmppFriend.stateToString(stateInt)+ "\n");
            }
            
            mFriends.add(map);
        }
        if (mBuddiesListView != null) {
            updateBuddiesList();
        }
    }
    
    private static String getStateImg(int stateType) {
        String state = String.valueOf(R.drawable.buddy_offline);
        switch (stateType) {
            case XmppFriend.AWAY:
            case XmppFriend.EXAWAY:
                state = String.valueOf(R.drawable.buddy_away);
                break;
            case XmppFriend.BUSY:
                state = String.valueOf(R.drawable.buddy_busy);
                break;
            case XmppFriend.ONLINE:
                state = String.valueOf(R.drawable.buddy_available);
                break;
        }
        
        return state;
    }
    
    private void updateBuddiesList() {
        Collections.sort(mFriends, new Comparator<HashMap<String, String>> () {
            public int compare(HashMap<String, String> object1, HashMap<String, String> object2) {
                if (object1.get("name") != null && object2.get("name") != null) {
                    return object1.get("name").compareTo(object2.get("name"));
                }
                return object1.get("userid").compareTo(object2.get("userid"));
            }});
        
        if (mBuddiesListView != null) {
            mBuddiesListView.setAdapter(new SimpleAdapter(
                    getSherlockActivity().getBaseContext(), mFriends, R.layout.buddyitem, 
                    new String[] { "state", "name", "status" }, 
                    new int[] { R.id.buddyState, R.id.buddyName, R.id.buddyStatus }));
        }
    }
}