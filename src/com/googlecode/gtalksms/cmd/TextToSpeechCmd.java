package com.googlecode.gtalksms.cmd;

import java.util.Locale;

import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.tools.Tools;

public class TextToSpeechCmd extends CommandHandlerBase implements OnInitListener {

    // TODO ADD Global Settings for Locale and Engine
    TextToSpeech mTts;
    Locale mLocale;
    boolean mTtsAvailable;

    public TextToSpeechCmd(MainService mainService) {
        super(mainService, 
                CommandHandlerBase.TYPE_MESSAGE, 
                new Cmd("tts", "say"), 
                new Cmd("tts-lang", "ttslang"), 
                new Cmd("tts-lang-list", "ttslanglist"), 
                new Cmd("tts-engine", "ttsengine"), 
                new Cmd("tts-engine-list", "ttsenginelist"));
        mLocale = Locale.getDefault();
    }

    protected void execute(String cmd, String args) {
        Log.i(Tools.LOG_TAG, "TTS: " + cmd + " (" + args + ")");
        
        if (isMatchingCmd("tts", cmd)) {
            if (mTtsAvailable) {
                mTts.speak(args, TextToSpeech.QUEUE_ADD, null);
            } else {
                send(getString(R.string.chat_tts_installation));
                sContext.startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
            }
        } else if (isMatchingCmd("tts-engine-list", cmd)) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {
                send(getString(R.string.android_version_incompatible, "ICE CREAM SANDWICH"));
            } else {
                StringBuilder sb = new StringBuilder(getString(R.string.chat_tts_engines));
                for (EngineInfo engine : mTts.getEngines()) {
                    sb.append(engine.label + " - " + engine.name + "\n");
                }
                send(sb.substring(0, Math.max(0,sb.length() - 1)));
            }
        } else if (isMatchingCmd("tts-lang-list", cmd)) {
            StringBuilder sb = new StringBuilder(getString(R.string.chat_tts_languages));
            for (Locale locale : Locale.getAvailableLocales()) {
                switch (mTts.isLanguageAvailable(locale)) {
                    case TextToSpeech.LANG_AVAILABLE:
                        sb.append(locale.getDisplayLanguage());
                        if (locale.getDisplayCountry() != null && locale.getDisplayCountry().length() > 0)  {
                            sb.append(" (" + locale.getDisplayCountry() + ")");
                        }
                        sb.append(" - " + locale.getLanguage());
                        
                        if (locale.getDisplayCountry() != null && locale.getDisplayCountry().length() > 0)  {
                            sb.append(":" + locale.getCountry());
                        }
                        sb.append("\n");
                        break;
                }
            }
            send(sb.substring(0, Math.max(0,sb.length() - 1)));
        } else if (isMatchingCmd("tts-engine", cmd)) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {
                send(getString(R.string.android_version_incompatible, "ICE CREAM SANDWICH"));
            } else {
                mTts = new TextToSpeech(sContext, this, args);
                send(getString(R.string.chat_tts_engine) + mTts.getDefaultEngine());
            }
        } else if (isMatchingCmd("tts-lang", cmd)) {
            String[] argList = splitArgs(args);
            if (argList.length == 1) {
                mLocale = new Locale(args);
                mTts.setLanguage(mLocale);
            } else if (argList.length == 2) {
                mLocale = new Locale(argList[0], argList[1]);
                mTts.setLanguage(mLocale);
            } 
            send(getString(R.string.chat_tts_language) + mTts.getLanguage().getDisplayName());
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
        if (status == TextToSpeech.SUCCESS && mTts != null) {
            Log.i(Tools.LOG_TAG, "TTS initialized!");
            mTts.setLanguage(mLocale);
            mTtsAvailable = true;
        } else {
            Log.e(Tools.LOG_TAG, "Can't initialise TTS!");
            mTtsAvailable = false;
        }
    }
    
    @Override
    public void setup() {
        mTts = new TextToSpeech(sContext, this);
    }
    
    @Override
    public void cleanUp() {
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }
}
