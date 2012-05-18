package com.googlecode.gtalksms.panels.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.Cmd;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;
import com.googlecode.gtalksms.panels.CmdListAdapter;

public class CommandsTabFragment extends SherlockFragment {
    private ListView mListViewCommands;
    private List<Cmd> mListCommands = new ArrayList<Cmd>();
    
    @Override
    public void onResume() {
        super.onResume();
        updateCommands(null);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_commands, container, false);
        mListViewCommands = (ListView)view.findViewById(R.id.ListViewCommands);
        mListViewCommands.setOnItemClickListener( new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cmd cmd = (Cmd)parent.getItemAtPosition(position);
                boolean isActive = !cmd.isActive();
                cmd.setActive(isActive);
                ImageView imageView = (ImageView) view.findViewById(R.id.State);
                imageView.setImageResource(isActive ? R.drawable.buddy_available : R.drawable.buddy_offline);
            }
        });
        return view;
    }
    
    public void updateCommands(Set<CommandHandlerBase> commands) {
        if (commands != null && commands.size() > 0) {
            mListCommands.clear();
            for (CommandHandlerBase cmdBase : commands) {
                if (cmdBase.getType() != CommandHandlerBase.TYPE_INTERNAL) {
                    for (Cmd cmd : cmdBase.getCommands()) {
                        mListCommands.add(cmd);
                    }
                }
            }
            Collections.sort(mListCommands, new Comparator<Cmd>() {
                public int compare(Cmd o1, Cmd o2) {
                     return o1.getName().compareTo(o2.getName());
                }
            });
        } 
          
        if (mListViewCommands != null ) {
            mListViewCommands.setAdapter(new CmdListAdapter(getActivity(), R.layout.cmd_item, mListCommands.toArray(new Cmd[mListCommands.size()])));
        }
    }
}