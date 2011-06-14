package com.googlecode.gtalksms.cmd;

import android.content.Intent;
import android.net.Uri;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class UrlsCmd extends CommandHandlerBase {
    public UrlsCmd(MainService mainService) {
        super(mainService, new String[] {"http", "https"}, CommandHandlerBase.TYPE_COPY);
    }
    
    @Override
    public void execute(Command c) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(c.getOriginalCommand()));
        Intent intent = Intent.createChooser(target, getString(R.string.chat_choose_activity));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sContext.startActivity(intent);
    }
    
    @Override
    public String[] help() {
        String[] s = { 
                getString(R.string.chat_help_urls, makeBold("\"http\""))
                };
        return s;
    }
}
