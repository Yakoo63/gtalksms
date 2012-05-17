package com.googlecode.gtalksms.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.Cmd;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;

public class CmdManager extends Activity  {
    
    private ListView mListView;
    private TextView mTextView;
    private MainService mMainService;
    
    private ServiceConnection _mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMainService = ((MainService.LocalBinder) service).getService();
            refresh();
        }

        public void onServiceDisconnected(ComponentName className) {
            mMainService = null;
            refresh();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cmd_panel);
        
        mTextView = (TextView)findViewById(R.id.TextView);
        mTextView.setText(R.string.cmd_manager_error_not_connected);
        
        mListView = (ListView)findViewById(R.id.ListView);
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cmd cmd = (Cmd)parent.getItemAtPosition(position);
                boolean isActive = !cmd.isActive();
                
                cmd.setActive(isActive);
                ImageView imageView = (ImageView) view.findViewById(R.id.State);
                imageView.setImageResource(isActive ? R.drawable.buddy_available : R.drawable.buddy_offline);
            }
        });
        mTextView.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
        
        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, _mainServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void refresh() {
        if (mMainService != null && mMainService.getCommandSet().size() > 0) {
            List<Cmd> cmds = new ArrayList<Cmd>();
            for (CommandHandlerBase cmdBase : mMainService.getCommandSet()) {
                if (cmdBase.getType() != CommandHandlerBase.TYPE_INTERNAL) {
                    for (Cmd cmd : cmdBase.getCommands()) {
                        cmds.add(cmd);
                    }
                }
            }
            Collections.sort(cmds, new Comparator<Cmd>() {
                    public int compare(Cmd o1, Cmd o2) {
                         return o1.getName().compareTo(o2.getName());
                    }
                });
            
            CmdListAdapter adapter = new CmdListAdapter(this, R.layout.cmd_item, cmds.toArray(new Cmd[cmds.size()]));
            mListView.setAdapter(adapter);
            mListView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
        } else {
            mTextView.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(_mainServiceConnection);
    }

}
