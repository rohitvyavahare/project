package com.example.rohitvyavahare.project;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.rohitvyavahare.project.R.string.inbox;

public class InboxActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // declare the color generator and drawable builder
    private ColorGenerator mColorGenerator = ColorGenerator.MATERIAL;
    private TextDrawable.IBuilder mDrawableBuilder;
    private ProgressDialog progress;
    private static final String TAG = "InboxActivity";
    protected DrawerLayout drawer;

    // list of data items
    private List<ListData> mDataList = new ArrayList<>();
    private SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Utils util = new Utils();
    static Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl1);
        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.content_side_bar, null, true);
        rl.addView(layout);
        prefs = getSharedPreferences(getString(R.string.private_file), MODE_PRIVATE);
        editor = prefs.edit();

        Toolbar toolbar;
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View hView = navigationView.getHeaderView(0);
        TextView nav_user = (TextView) hView.findViewById(R.id.NavName);
        String username = prefs.getString("user_name", "null");

        if (!username.equals("null")) {
            nav_user.setText(util.capitalizeString(username));
        }


        CircularImageView usr_pic = (CircularImageView) hView.findViewById(R.id.ProfilePic);
        String profile_pic = prefs.getString("profile_pic", "null");
        if (!profile_pic.equals("null")) {

            bitmap = util.StringToBitMap(profile_pic);
            if (bitmap != null) {
                usr_pic.setImageBitmap(bitmap);
            }
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InboxActivity.this, PlaceOrderActivity.class);
                startActivity(intent);
            }
        });

        mDrawableBuilder = TextDrawable.builder()
                .round();


        String last_active = prefs.getString(getString(R.string.last_active), "1");

        Long time = Long.parseLong(last_active);
        Long current_time = System.currentTimeMillis();
        Long time_diff = current_time - time;

        Log.d(TAG, "time : " + time);
        Log.d(TAG, "current_time : " + current_time);
        Log.d(TAG, "time_diff : " + time_diff);

        if (time < 2 || time_diff > (86400000 * 7)) {
            Log.d(TAG, "No acivity since : " + time);

            final Context c = this.getApplicationContext();

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Utils util = new Utils();
                    util.updateActivity(c);
                }
            });

            editor.putString(getString(R.string.hard_reload_inbox), "true");
            editor.putString(getString(R.string.hard_reload_outbox), "true");
            editor.putString(getString(R.string.hard_reload_pair_org), "true");
            editor.commit();

        }

        final String refreshToken = prefs.getString(getString(R.string.refresh_token), "null");
        String first_token = prefs.getString("first_token", "null");

        if (refreshToken.equals("null")) {
            Log.d(TAG, "Refresh token is null");
            editor.putString(getString(R.string.hard_reload_inbox), "true");
            editor.commit();
        } else if (first_token.equals("true")) {

            final Context c = this.getApplicationContext();

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Utils util = new Utils();
                    util.sendTokentoServer(refreshToken, prefs.getString("uid", "null"), c);
                }
            });

            editor.putString("first_token", "false");
            editor.commit();

        }

        Bundle bundle = getIntent().getExtras();

        if (bundle != null && bundle.getString("message") != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            builder.setMessage(bundle.getString("message"));
            builder.setCancelable(true);
            builder.setNeutralButton(android.R.string.ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            action();
                        }
                    });

            AlertDialog alert11 = builder.create();
            alert11.show();
        } else {
            action();
        }

    }

    private void action() {

        ListView listView = (ListView) findViewById(R.id.listView);
        final TextView empty = (TextView) findViewById(R.id.empty);
        final TextView currentOrgName = (TextView) findViewById(R.id.currentOrg);


        String default_org = prefs.getString("default_org", "null");
        String hard_reload = prefs.getString(getString(R.string.hard_reload_inbox), "null");

        Log.d(TAG, "Hard reload :" + hard_reload);
        Log.d(TAG, "Default org :" + default_org);
        if (!default_org.equals("null") && !hard_reload.equals("null") && !hard_reload.equals("true")) {
            try {
                JSONObject org = new JSONObject(prefs.getString("default_org", "{type : null}"));
                String string = prefs.getString(org.getString("id"), "null");
                Log.d(TAG, "default org found: " + org);

                Log.d(TAG, "default org data found: " + string);

                if (org.has("name")) {
                    StyleSpan boldStyle = new StyleSpan(Typeface.BOLD);
                    Utils util = new Utils();
                    util.setTextWithSpan(currentOrgName, "Current organization: " + org.getString("name"), org.getString("name"), boldStyle);
                }

                if (!string.equals("null")) {

                    String[] inbox = string.split(",");
                    for (String data : inbox) {
                        mDataList.add(new ListData(data));
                    }
                    // init the list view and its adapter

                    if (inbox.length > 0) {
                        listView.setAdapter(new SampleAdapter());
                        empty.setVisibility(View.INVISIBLE);

                    } else {
                        listView.setEmptyView(empty);
                    }
                } else if (org.has("type") && org.getString("type").equals("null")) {
                    empty.setText(getString(R.string.no_org_msg));
                } else {
                    Log.d(TAG, "Empty Inbox");
                    //new GetOrg(InboxActivity.this).execute(getIntent().getExtras());
                }
            } catch (org.json.JSONException e) {
                e.printStackTrace();
                Toast.makeText(this.getApplicationContext(), "Something went wrong while retrieving Inbox, Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Empty Inbox, going to make GET request");
            new GetOrg(InboxActivity.this).execute(getIntent().getExtras());
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                ListData data = mDataList.get(position);

                String orders = prefs.getString(data.getData(), "null");

                Log.d(TAG, "Clicked at position :" + position);
                Bundle orderData = new Bundle();
                Log.d(TAG, "Data :" + data.getData());
                orderData.putString("orders", orders);
                Log.d(TAG, "clicked org_name : " + data.getData());
                orderData.putString("org_name", data.getData());
                orderData.putString("type", "inbox");
                Intent intent = new Intent(InboxActivity.this, OrdersActivity.class);
                for (String key : orderData.keySet()) {
                    Log.d(TAG, key + " is a key in the bundle");
                }
                intent.putExtras(orderData);
                startActivity(intent);
            }
        });
    }

    private class SampleAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDataList.size();
        }

        @Override
        public ListData getItem(int position) {
            return mDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(InboxActivity.this, R.layout.list_item_layout, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ListData item = getItem(position);

            // provide support for selected state
            updateCheckedState(holder, item);
            holder.textView.setText(item.data);

            return convertView;
        }

        private void updateCheckedState(ViewHolder holder, ListData item) {

            Log.d(TAG, "setting image view");
            TextDrawable drawable = mDrawableBuilder.build(String.valueOf(item.data.charAt(0)), mColorGenerator.getColor(item.data));

            Log.d(TAG, "item.data " + item.data);

            String pic = prefs.getString(item.data + "_pic", "null");
            if (!pic.equals("null") && !pic.equals("default")) {
                holder.imageView.setImageBitmap(util.getBitmapFromURL(pic));
            } else {
                Log.d(TAG, "no org_pic");
                holder.imageView.setImageDrawable(drawable);
            }


            holder.view.setBackgroundColor(Color.TRANSPARENT);
            holder.checkIcon.setVisibility(View.GONE);
        }
    }

    private static class ViewHolder {

        private View view;

        private ImageView imageView;

        private TextView textView;

        private ImageView checkIcon;

        private ViewHolder(View view) {
            this.view = view;
            imageView = (ImageView) view.findViewById(R.id.imageView);
            textView = (TextView) view.findViewById(R.id.textView);
            checkIcon = (ImageView) view.findViewById(R.id.check_icon);
        }
    }

    private static class ListData {

        private String data;

        public ListData(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.side_bar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;

        switch (item.getItemId()) {

            case R.id.nav_inbox: {
                intent = new Intent(InboxActivity.this, InboxActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_outbox: {

                intent = new Intent(InboxActivity.this, OutboxActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_add_employee: {

                intent = new Intent(InboxActivity.this, AddEmployeeActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_pair_prg: {

                intent = new Intent(InboxActivity.this, PairOrgActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_add_org: {

                intent = new Intent(InboxActivity.this, CreateOrgActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_settings: {

                intent = new Intent(InboxActivity.this, SettingActivity.class);
                startActivity(intent);
                break;

            }


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class GetClass extends AsyncTask<Bundle, Void, Void> {

        private final Context context;

        public GetClass(Context c) {
            context = c;
        }

        protected void onPreExecute() {
            progress = new ProgressDialog(this.context);
            progress.setMessage("Loading");
            progress.show();
        }

        @Override
        protected Void doInBackground(Bundle... params) {
            try {

                Log.d(TAG, "In background job");

                final JSONObject obj = new JSONObject(prefs.getString("default_org", "null"));

                Log.d(TAG, "default org: " + obj.toString());


                Uri uri = new Uri.Builder()
                        .scheme("http")
                        .encodedAuthority(getString(R.string.server_ur_templ))
                        .path(getString(R.string.org))
                        .appendPath(obj.getString("id"))
                        .appendPath(getString(inbox))
                        .build();
                //@TODO add band as query parameter

                URL url = new URL(uri.toString());
                Log.d(TAG, "url:" + url.toString());

                prefs = getSharedPreferences(getString(R.string.private_file), MODE_PRIVATE);
                String auth = prefs.getString("uid", "null");

                Log.d(TAG, "auth " + auth);
                if (auth.equals("null")) {
                    onPostExecute();
                    //@TODO add alert
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", auth);

                final int responseCode = connection.getResponseCode();
                final int response = responseCode;

                Log.d(TAG, "Sending 'GET' request to URL : :" + url);
                Log.d(TAG, "Get parameters : " + prefs.getString("default_org", "null"));
                Log.d(TAG, "Response Code : " + responseCode);

                final StringBuilder sb = new StringBuilder();
                String line;
                BufferedReader br;

                try {
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } catch (IOException ioe) {
                    br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                Log.d(TAG, "Response from GET :" + sb.toString());
                final ListView listView = (ListView) findViewById(R.id.listView);
                final View empty = findViewById(R.id.empty);

                Utils util = new Utils();
                util.getPairedOrgs(InboxActivity.this, obj.getString("id"), obj.getString("name"));
                onPostExecute();

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        switch (response) {
                            case 200: {
                                try {
                                    JSONArray jArray = new JSONArray(sb.toString());
                                    HashMap<String, JSONArray> map = new HashMap<>();
                                    StringBuilder sb = new StringBuilder();
                                    if (mDataList.size() > 0) {
                                        mDataList.clear();
                                    }
                                    for (int i = 0; i < jArray.length(); ++i) {
                                        JSONObject rec = jArray.getJSONObject(i);
                                        rec = rec.getJSONObject("doc");
                                        JSONObject value = rec.getJSONObject("value");
                                        value.put("id", rec.getString("_id"));

                                        Log.d(TAG, "Value :" + value.toString());

                                        if (obj.getString("band").trim().equals("3")) {
                                            if (value.getString("status").trim().equals("acknowledged")) {
                                                if (map.containsKey(value.getString("from"))) {
                                                    map.get(value.getString("from")).put(value);
                                                } else {
                                                    mDataList.add(new ListData(value.getString("from")));
                                                    sb.append(value.getString("from")).append(",");
                                                    JSONArray arr = new JSONArray();
                                                    arr.put(value);
                                                    map.put(value.getString("from"), arr);
                                                }

                                            }

                                        } else {
                                            if (map.containsKey(value.getString("from"))) {
                                                map.get(value.getString("from")).put(value);
                                            } else {
                                                mDataList.add(new ListData(value.getString("from")));
                                                sb.append(value.getString("from")).append(",");
                                                JSONArray arr = new JSONArray();
                                                arr.put(value);
                                                map.put(value.getString("from"), arr);
                                            }
                                        }
                                    }

                                    Log.d(TAG, "Caching orgs :" + map.keySet());
                                    for (String s : map.keySet()) {

                                        Log.d(TAG, "Key :" + s);
                                        Log.d(TAG, "Value for " + s + " :" + map.get(s).toString());

                                        editor.putString(s, map.get(s).toString());
                                    }
                                    editor.putString(getString(R.string.hard_reload_inbox), "false");
                                    editor.putString(obj.getString("id"), sb.toString());
                                    editor.commit(); //TODO research on apply method

                                    if (map.keySet().size() > 0) {
                                        listView.setAdapter(new SampleAdapter());
                                        empty.setVisibility(View.INVISIBLE);
                                    } else {

                                        listView.setEmptyView(empty);
                                    }
                                    editor.commit(); //TODO research on apply method
                                    break;
                                } catch (org.json.JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, "Something went wrong while retrieving Inbox, Please try again", Toast.LENGTH_SHORT).show();
                                    break;
                                }
                            }
                            case 404: {
                                editor.putString(getString(R.string.hard_reload_inbox), "false");
                                editor.commit(); //TODO research on apply method
                                Log.d(TAG, "404 response");
                                if (empty != null)
                                    listView.setEmptyView(empty);
                                break;

                            }
                        }
                    }
                });

            } catch (IOException | JSONException | NullPointerException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        onPostExecute();
                        Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                    }
                });
                onPostExecute();
            }
            return null;
        }

        protected void onPostExecute() {
            progress.dismiss();
        }

    }

    private class GetOrg extends AsyncTask<Bundle, Void, Void> {

        private final Context context;

        public GetOrg(Context c) {
            context = c;
        }

        protected void onPreExecute() {
            progress = new ProgressDialog(this.context);
            progress.setMessage("Loading");
            progress.show();
        }

        @Override
        protected Void doInBackground(Bundle... params) {
            try {

                Log.d(TAG, "In background job");

                Uri uri = new Uri.Builder()
                        .scheme("http")
                        .encodedAuthority(getString(R.string.server_ur_templ))
                        .path(getString(R.string.account))
                        .appendPath(prefs.getString("uid", "null"))
                        .appendPath(getString(R.string.orgs))
                        .build();

                URL url = new URL(uri.toString());
                Log.d(TAG, "url:" + url.toString());

                String auth = prefs.getString("uid", "null");
                if (auth.equals("null")) {
                    onPostExecute();
                    //@TODO add alert
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", auth);

                final int responseCode = connection.getResponseCode();
                final int response = responseCode;

                Log.d(TAG, "Sending 'GET' request to URL : :" + url);
                Log.d(TAG, "Get parameters : " + prefs.getString("uid", "null"));
                Log.d(TAG, "Response Code : " + responseCode);

                final StringBuilder sb = new StringBuilder();
                String line;
                BufferedReader br;

                try {
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } catch (IOException ioe) {
                    br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                while ((line = br.readLine()) != null) {
                    sb.append(line + "\n");
                }
                br.close();

                Log.d(TAG, "Response from GET :" + sb.toString());

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        onPostExecute();
                        switch (response) {
                            case 200: {
                                try {
                                    JSONArray jArray = new JSONArray(sb.toString());
                                    JSONArray orgs = new JSONArray();
                                    for (int i = 0; i < jArray.length(); ++i) {
                                        JSONObject rec = jArray.getJSONObject(i);
                                        orgs.put(rec);
                                    }

                                    if (orgs.length() > 0) {
                                        Log.d(TAG, "Caching orgs :" + orgs.toString());
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString("orgs", orgs.toString());
                                        editor.putString("default_org", orgs.get(0).toString());
                                        editor.apply(); //TODO research on apply method
                                        new GetClass(InboxActivity.this).execute(getIntent().getExtras());
                                    } else {
                                        TextView empty = (TextView) findViewById(R.id.empty);
                                        empty.setText(getString(R.string.no_org_msg));
                                        ListView listView = (ListView) findViewById(R.id.listView);
                                        listView.setEmptyView(empty);
                                    }

                                    break;
                                } catch (org.json.JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(context, "Something went wrong while retrieving Inbox, Please try again", Toast.LENGTH_SHORT).show();
                                    onPostExecute();
                                    break;
                                }
                            }
                            default: {
                                Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        onPostExecute();
                        Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                    }
                });
                onPostExecute();
            }
            return null;
        }

        protected void onPostExecute() {
            progress.dismiss();
        }

    }

}
