package com.googlecode.gtalksms.cmd;

import java.util.Locale;

import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;

public class TextToSpeechCmd extends CommandHandlerBase implements OnInitListener {

    // TODO ADD Global Settings for Locale and Engine
    private TextToSpeech mTts;
    private Locale mLocale;
    private boolean mTtsAvailable;

    public TextToSpeechCmd(MainService mainService) {
        super(mainService, CommandHandlerBase.TYPE_MESSAGE, "TTS",
                new Cmd("tts", "say"), 
                new Cmd("tts-lang", "ttslang"), 
                new Cmd("tts-lang-list", "ttslanglist"), 
                new Cmd("tts-engine", "ttsengine"), 
                new Cmd("tts-engine-list", "ttsenginelist"));
    }

    @Override
    protected void onCommandActivated() {
        mLocale = Locale.getDefault();
    }

    @Override
    protected void onCommandDeactivated() {
        mLocale = null;
        if (mTts != null) {
            try {
                mTts.shutdown();
            } catch (Exception e) {
                // Don't care
            }

            mTts = null;
        }
    }

    private TextToSpeech getTts() { return mTts == null ? mTts = new TextToSpeech(sContext, this) : mTts; }

    protected void execute(Command cmd) {
        Log.i("TTS: " + cmd.getOriginalCommand());
        
        if (isMatchingCmd(cmd, "tts")) {
            if (mTtsAvailable) {
                getTts().speak(cmd.getAllArg1(), TextToSpeech.QUEUE_ADD, null);
            } else {
                send(R.string.chat_tts_installation);
                sContext.startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
            }
        } else if (isMatchingCmd(cmd, "tts-engine-list")) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {
                send(R.string.android_version_incompatible, "ICE CREAM SANDWICH");
            } else {
                StringBuilder sb = new StringBuilder(getString(R.string.chat_tts_engines));
                for (EngineInfo engine : getTts().getEngines()) {
                    sb.append(engine.label).append(" - ").append(engine.name).append("\n");
                }
                send(sb.substring(0, Math.max(0,sb.length() - 1)));
            }
        } else if (isMatchingCmd(cmd, "tts-lang-list")) {
            StringBuilder sb = new StringBuilder(getString(R.string.chat_tts_languages));
            for (Locale locale : Locale.getAvailableLocales()) {
                switch (getTts().isLanguageAvailable(locale)) {
                    case TextToSpeech.LANG_AVAILABLE:
                        sb.append(locale.getDisplayLanguage());
                        if (locale.getDisplayCountry() != null && locale.getDisplayCountry().length() > 0)  {
                            sb.append(" (").append(locale.getDisplayCountry()).append(")");
                        }
                        sb.append(" - ").append(locale.getLanguage());
                        
                        if (locale.getDisplayCountry() != null && locale.getDisplayCountry().length() > 0)  {
                            sb.append(":").append(locale.getCountry());
                        }
                        sb.append("\n");
                        break;
                }
            }
            send(sb.substring(0, Math.max(0,sb.length() - 1)));
        } else if (isMatchingCmd(cmd, "tts-engine")) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {
                send(R.string.android_version_incompatible, "ICE CREAM SANDWICH");
            } else {
                mTts = new TextToSpeech(sContext, this, cmd.getAllArg1());
                send(getString(R.string.chat_tts_engine) + getTts().getDefaultEngine());
            }
        } else if (isMatchingCmd(cmd, "tts-lang")) {
            String arg1 = cmd.getArg1();
            String arg2 = cmd.getArg2();
            if (!arg1.equals("") && !arg2.equals("")) {
                mLocale = new Locale(arg1, arg2);
                getTts().setLanguage(mLocale);
            } else if (!arg1.equals("")) {
                mLocale = new Locale(arg1);
                getTts().setLanguage(mLocale);
            } 
            send(getString(R.string.chat_tts_language) + getTts().getLanguage().getDisplayName());
        }
    }
    
    @Override
    protected void initializeSubCommands() {
        mCommandMap.get("tts").setHelp(R.string.chat_help_tts, "#text to speech#");
        mCommandMap.get("tts-lang").setHelp(R.string.chat_help_ttslang, "#fr or fr:CA#");
        mCommandMap.get("tts-engine").setHelp(R.string.chat_help_ttsengine, "#engine#");
        mCommandMap.get("tts-lang-list").setHelp(R.string.chat_help_ttslanglist);
        mCommandMap.get("tts-engine-list").setHelp(R.string.chat_help_ttsenginelist);
   }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && getTts() != null) {
            Log.i("TTS initialized!");
            getTts().setLanguage(mLocale);
            mTtsAvailable = true;
        } else {
            Log.e("Can't initialise TTS!");
            mTtsAvailable = false;
        }
    }
}
