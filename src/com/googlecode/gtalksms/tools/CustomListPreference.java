package com.googlecode.gtalksms.tools;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.googlecode.gtalksms.R;

public class CustomListPreference extends ListPreference
{   
    private CustomListPreferenceAdapter customListPreferenceAdapter = null;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private ArrayList<ArrayList<Integer>> entries;
    private final ArrayList<RadioButton> rButtonList;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    private int entriesSize;
    
    public CustomListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        rButtonList = new ArrayList<RadioButton>();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        editor = prefs.edit();
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        CharSequence[] prefixes = getEntries();
        
        entries = new ArrayList<ArrayList<Integer>>();
        for (CharSequence prefix : prefixes) {
            ArrayList<Integer> subEntries = new ArrayList<Integer>();
            for (Field f : R.drawable.class.getFields()) {
                if(f.getName().startsWith(prefix.toString())) {
                    try {
                        subEntries.add(f.getInt(null));
                    } catch (Exception e) {}
                }
            }
            entries.add(subEntries);
        }
        
        // Check all entries size
        entriesSize = entries.get(0).size();
        for (ArrayList<Integer> list : entries) {
            if (list.size() != entriesSize) {
                throw new IllegalStateException("ListPreference requires all entries with the same length");
            }
        }
        customListPreferenceAdapter = new CustomListPreferenceAdapter(mContext);

        builder.setAdapter(customListPreferenceAdapter, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
            }
        });
    }

    private class CustomListPreferenceAdapter extends BaseAdapter
    {        
        public CustomListPreferenceAdapter(Context context)
        {
        }

        public int getCount()
        {
            return entriesSize;
        }

        public Object getItem(int position)
        {
            return position;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent)
        {  
            View row = convertView;
            CustomHolder holder;

            if(row == null)
            {                                                                   
                row = mInflater.inflate(R.layout.images_preference_row_picker, parent, false);
                holder = new CustomHolder(row, position);
                row.setTag(holder);
            }

            return row;
        }

        class CustomHolder
        {
            private LinearLayout layout = null;
            private RadioButton rButton = null;

            CustomHolder(View row, int position)
            {    
                layout = (LinearLayout)row.findViewById(R.id.custom_list_view_row_layout);
                if (layout.getChildCount() == 0) {
                    for (ArrayList<Integer> entry : entries) {
                        ImageView img = new ImageView(getContext());
                        img.setImageResource(entry.get(position));
                        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        img.setPadding(5, 0, 0, 0);
                        layout.addView(img);
                    }
                }

                rButton = (RadioButton)row.findViewById(R.id.custom_list_view_row_radio_button);
                rButton.setId(position);
                rButtonList.add(rButton);
                rButton.setChecked(getSharedPreferences().getString(getKey(),"").equals("" + position));
                rButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
                {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                    {
                        if(isChecked)
                        {
                            for(RadioButton rb : rButtonList)
                            {
                                if(rb != buttonView) {
                                    rb.setChecked(false);
                                }
                            }

                            int index = buttonView.getId();
                            getSharedPreferences().edit().putString(getKey(), "" + index).commit();

                            Dialog mDialog = getDialog();
                            mDialog.dismiss();
                        }
                    }
                });
            }
        }
    }
}