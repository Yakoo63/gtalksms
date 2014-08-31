package com.googlecode.gtalksms.panels.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.cmd.Cmd;
import com.googlecode.gtalksms.cmd.CommandHandlerBase;
import com.googlecode.gtalksms.panels.tools.AutoClickEditorActionListener;
import com.googlecode.gtalksms.tools.StringFmt;

public class CommandsTabFragment extends SherlockFragment {
    private ListView mListViewCommands;
    private EditText mEditTextCommand;
    private Button mButtonSend;
    private final List<Cmd> mListCommands = new ArrayList<Cmd>();
    
    @Override
    public void onResume() {
        super.onResume();
        updateCommands();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_commands, container, false);
        mListViewCommands = (ListView)view.findViewById(R.id.ListViewCommands);
        mListViewCommands.setOnItemClickListener( new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Cmd cmd = (Cmd)parent.getItemAtPosition(position);
                final Dialog dialog = new Dialog(getSherlockActivity());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.tab_commands_dialog);
                
                TextView cmdName = (TextView) dialog.findViewById(R.id.textViewCmd);
                cmdName.setText(cmd.getName());

                final EditText cmdArgs = (EditText) dialog.findViewById(R.id.editTextArgs);
                final Button buttonSend = (Button) dialog.findViewById(R.id.buttonSend);
                
                if (cmd.getHelpArgs() == null || cmd.getHelpArgs().equals("")) {
                    cmdArgs.setVisibility(View.INVISIBLE);
                } else {
                    cmdArgs.setHint(cmd.getHelpArgs());
                    cmdArgs.setOnEditorActionListener(new AutoClickEditorActionListener(buttonSend));
                }
                
                buttonSend.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        sendCommandAndVibrate(cmd.getName(), cmdArgs.getText().toString(), v);
                    }
                });

                TextView cmdHelp = (TextView) dialog.findViewById(R.id.textViewCmdHelp);
                cmdHelp.setText(cmd.getHelpMsg());
                
                ListView subCommands = (ListView) dialog.findViewById(R.id.ListViewCommands);
                subCommands.setAdapter(new SubCmdListAdapter(getSherlockActivity(), R.layout.tab_commands_sub_item, cmd));

                Button buttonClose = (Button) dialog.findViewById(R.id.buttonOk);
                buttonClose.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                
                // TODO backup the command args somewhere
//                dialog.setOnDismissListener(new OnDismissListener() {
//                     public void onDismiss(DialogInterface dialog) {
//                    }
//                });
                
                dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                dialog.show();
            }
        });
        
        mEditTextCommand = (EditText) view.findViewById(R.id.editTextCommand);
        mButtonSend = (Button) view.findViewById(R.id.buttonCommandSend);
        
        mEditTextCommand.setOnEditorActionListener(new AutoClickEditorActionListener(mButtonSend));
        mButtonSend.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String cmd = mEditTextCommand.getText().toString();
                int index = cmd.indexOf(':');
                if (index == -1) {
                    sendCommandAndVibrate(cmd, null, v);
                } else  if (index < cmd.length() - 1) {
                    sendCommandAndVibrate( cmd.substring(0, index), cmd.substring(index + 1), v);
                } else {
                    sendCommandAndVibrate( cmd.substring(0, index), null, v);
                }
            }
        });
        
        return view;
    }
    
    public void updateCommands() {
        Set<CommandHandlerBase> commands = MainService.getCommandHandlersSet();
        if (commands != null && commands.size() > 0) {
            mListCommands.clear();
            for (CommandHandlerBase cmdBase : commands) {
                if (cmdBase.getType() != CommandHandlerBase.TYPE_INTERNAL) {
                    Collections.addAll(mListCommands, cmdBase.getCommands());
                }
            }
            Collections.sort(mListCommands, new Comparator<Cmd>() {
                public int compare(Cmd o1, Cmd o2) {
                     return o1.getName().compareTo(o2.getName());
                }
            });
        } 
          
        if (mListViewCommands != null ) {
            mListViewCommands.setAdapter(new CmdListAdapter(getActivity(), R.layout.tab_commands_item, mListCommands));
        }
    }
    
    void sendCommandAndVibrate(String cmd, String args, View view) {
        Intent intent = new Intent(MainService.ACTION_COMMAND);
        intent.putExtra("cmd", cmd);
        if (args != null) {
            intent.putExtra("args", args);
        }
        intent.setClass(getActivity().getBaseContext(), MainService.class);
        getActivity().startService(intent);
        
        view.performHapticFeedback( HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING );
    }
    
    public class CmdListAdapter extends ArrayAdapter<Cmd> {
        
        final LayoutInflater mInflater;
        
        public CmdListAdapter(Activity activity, int textViewResourceId, List<Cmd> list) {
            super(activity, textViewResourceId, list);
            mInflater = activity.getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            
            View row = convertView;
            if (row == null) {
                row = mInflater.inflate(R.layout.tab_commands_item, parent, false);
            }
            
            final Cmd cmd = getItem(position);
            
            ImageView imageView = (ImageView) row.findViewById(R.id.State);
            imageView.setImageResource(cmd.isActive() ? R.drawable.buddy_available : R.drawable.buddy_offline);
            imageView.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    boolean isActive = !cmd.isActive();
                    cmd.setActive(isActive);
                    MainService.updateCommandState();
                    ImageView imageView = (ImageView) view.findViewById(R.id.State);
                    imageView.setImageResource(isActive ? R.drawable.buddy_available : R.drawable.buddy_offline);
                }
            });
            
            TextView textView = (TextView) row.findViewById(R.id.Name);
            textView.setText(cmd.getName());
            
            String alias = StringFmt.join(cmd.getAlias(), ", ");
            textView = (TextView) row.findViewById(R.id.Alias);
            textView.setText(alias + " "); // Adding space because italic text is truncated...
            
            textView = (TextView) row.findViewById(R.id.Help);
            textView.setText(cmd.getHelpSummary() + " "); // Adding space because italic text is truncated...
            
            return row;
        }
    }
    
    public class SubCmdListAdapter extends ArrayAdapter<Cmd.SubCmd> {
        
        final LayoutInflater mInflater;
        final Cmd mCmd;
        
        public SubCmdListAdapter(Activity activity, int textViewResourceId, Cmd cmd) {
            super(activity, textViewResourceId, cmd.getSubCmds());
            mInflater = activity.getLayoutInflater();
            mCmd = cmd;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            
            View row = convertView;
            if (row == null) {
                row = mInflater.inflate(R.layout.tab_commands_sub_item, parent, false);
            }
            
            final Cmd.SubCmd subCmd = getItem(position);
            
            final EditText cmdSubCmd = (EditText) row.findViewById(R.id.editTextSubCmd);
            TextView cmdName = (TextView) row.findViewById(R.id.textViewCmd);
            if (subCmd.getName().startsWith("#") && subCmd.getName().endsWith("#")) {
                cmdName.setVisibility(View.GONE);
                cmdSubCmd.setHint(subCmd.getName());
            } else {
                cmdName.setText(subCmd.getName());
                cmdSubCmd.setVisibility(View.GONE);
            }
            
            final EditText cmdArgs = (EditText) row.findViewById(R.id.editTextArgs);
            final Button buttonSend = (Button) row.findViewById(R.id.buttonSend);
            
            if (subCmd.getHelpArgs() != null) {
                cmdArgs.setHint(subCmd.getHelpArgs());
                cmdArgs.setOnEditorActionListener(new AutoClickEditorActionListener(buttonSend));
                cmdArgs.setVisibility(View.VISIBLE);
            } else {
                cmdArgs.setVisibility(View.INVISIBLE);
            }
            
            buttonSend.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String args = subCmd.getName().startsWith("#") && subCmd.getName().endsWith("#") ? cmdSubCmd.getText().toString() : subCmd.getName();
                    if (! cmdArgs.getText().toString().equals("")) {
                        args += ":" + cmdArgs.getText().toString();
                    }
                    sendCommandAndVibrate(mCmd.getName(), args, v);
                }
            });

            TextView cmdHelp = (TextView) row.findViewById(R.id.textViewCmdHelp);
            cmdHelp.setText(subCmd.getHelpMsg());
            
            return row;
        }
    }
}