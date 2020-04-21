package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class InfoActivity extends Activity {
    private ListView infoList;

    private class InfoManager {
        private String infoBody;
        private String infoTitle;
        private String infoURL;

        public InfoManager(String infoTitle2, String infoBody2, String infoURL2) {
            this.infoTitle = infoTitle2;
            this.infoBody = infoBody2;
            this.infoURL = infoURL2;
        }

        public String getInfoTitle() {
            return this.infoTitle;
        }

        public String getInfoBody() {
            return this.infoBody;
        }

        public String getInfoURL() {
            return this.infoURL;
        }
    }

    public class InformationAdapter extends ArrayAdapter<InfoManager> {
        private ViewHolder viewHolder;

        private class ViewHolder {
            /* access modifiers changed from: private */
            public TextView infoBody;
            /* access modifiers changed from: private */
            public TextView infoTitle;

            private ViewHolder() {
            }
        }

        public InformationAdapter(Context context, int textViewResourceId, ArrayList<InfoManager> items) {
            super(context, textViewResourceId, items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_info_item, parent, false);
                this.viewHolder = new ViewHolder();
                this.viewHolder.infoTitle = (TextView) convertView.findViewById(R.id.tv_info_title);
                this.viewHolder.infoBody = (TextView) convertView.findViewById(R.id.tv_info_body);
                convertView.setTag(this.viewHolder);
            } else {
                this.viewHolder = (ViewHolder) convertView.getTag();
            }
            InfoManager item = (InfoManager) getItem(position);
            if (item != null) {
                this.viewHolder.infoTitle.setText(item.getInfoTitle());
                this.viewHolder.infoBody.setText(item.getInfoBody());
            }
            return convertView;
        }
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        this.infoList = (ListView) findViewById(R.id.lv_info);
    }

    public void onResume() {
        super.onResume();
        ArrayList<InfoManager> infoManagerArrayList = new ArrayList<>();
        int length = getResources().getStringArray(R.array.info_title_array).length;
        for (int index = 0; index < length; index++) {
            infoManagerArrayList.add(new InfoManager(getResources().getStringArray(R.array.info_title_array)[index], getResources().getStringArray(R.array.info_body_array)[index], getResources().getStringArray(R.array.info_url_array)[index]));
        }
        final InformationAdapter adapter = new InformationAdapter(this, R.layout.row_info_item, infoManagerArrayList);
        this.infoList.setAdapter(adapter);
        this.infoList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent webIntent = new Intent("android.intent.action.VIEW");
                webIntent.setData(Uri.parse(((InfoManager) adapter.getItem(i)).getInfoURL()));
                InfoActivity.this.startActivity(webIntent);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_info, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == 16908332) {
            finish();
        } else if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
