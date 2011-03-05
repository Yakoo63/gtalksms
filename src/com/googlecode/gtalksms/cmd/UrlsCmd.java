package com.googlecode.gtalksms.cmd;

import android.content.Intent;
import android.net.Uri;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class UrlsCmd extends Command {
    public UrlsCmd(MainService mainService) {
        super(mainService, new String[] {"http", "https"});
    }
    
    @Override
    public void execute(String cmd, String args) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(cmd + ":" + args));
        Intent intent = Intent.createChooser(target, getString(R.string.chat_choose_activity));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(intent);
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_urls, makeBold("\"http\""))
                };
        return s;
    }
}
