package com.googlecode.gtalksms.panels;

import java.io.File;
import java.util.ArrayList;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.googlecode.gtalksms.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.SettingsManager;
import com.googlecode.gtalksms.cmd.Cmd;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;

public class CmdManager extends Activity {
    
    private SettingsManager mSettingsMgr;
    private ListView mListView;
    private MainService mMainService;
    
    private ServiceConnection _mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMainService = ((MainService.LocalBinder) service).getService();
            refresh();
        }

        public void onServiceDisconnected(ComponentName className) {
            mMainService = null;
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, _mainServiceConnection, Context.BIND_AUTO_CREATE);

        mSettingsMgr = SettingsManager.getSettingsManager(this);
        Log.initialize(mSettingsMgr);
        
        setContentView(R.layout.cmd_panel);
     //   LinearLayout layout = (LinearLayout) findViewById(R.id.mainLayout);
        mListView = (ListView)findViewById(R.id.listView);
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
    }
    
    public void refresh() {
        if (mMainService != null) {
            List<Cmd> cmds = new ArrayList<Cmd>();
            for (CommandHandlerBase cmdBase : mMainService.getCommandSet()) {
                if (cmdBase.getType() != CommandHandlerBase.TYPE_INTERNAL) {
                    for (Cmd cmd : cmdBase.getCommands()) {
                        cmds.add(cmd);
                    }
                }
            }
            
            CmdListAdapter adapter = new CmdListAdapter(this, R.layout.cmd_item, cmds.toArray(new Cmd[cmds.size()]), mSettingsMgr);
            mListView.setAdapter(adapter);
            mListView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.GONE);
        }
        
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(_mainServiceConnection);
    }
}
