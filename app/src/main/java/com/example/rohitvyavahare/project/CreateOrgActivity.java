package com.example.rohitvyavahare.project;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import static com.example.rohitvyavahare.project.InboxActivity.bitmap;

public class CreateOrgActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    
    private EditText orgTag;
    private AutoCompleteTextView orgState;
    private ProgressDialog progress;
    private static final String TAG = "CreateOrgActivity";
    private SharedPreferences prefs;
    private static final int SELECT_PICTURE = 1;
    private ProgressDialog mProgressDialog;
    FirebaseStorage storage = FirebaseStorage.getInstance();
    private UploadTask uploadTask;
    private String state = "";
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rl1);
        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.activity_create_org, null, true);
        rl.addView(layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        prefs = getSharedPreferences(getString(R.string.private_file), MODE_PRIVATE);

        View hView = navigationView.getHeaderView(0);
        TextView nav_user = (TextView) hView.findViewById(R.id.NavName);
        String username = prefs.getString("user_name", "null");

        Utils util = new Utils();
        if (!username.equals("null")) {
            nav_user.setText(util.capitalizeString(username));
        }


        CircularImageView usr_pic = (CircularImageView) hView.findViewById(R.id.ProfilePic);
        String profile_pic = prefs.getString("profile_pic", "null");
        if (!profile_pic.equals("null")) {

            if (bitmap != null) {
                usr_pic.setImageBitmap(bitmap);
            }
            else {
                bitmap = util.StringToBitMap(profile_pic);
                if (bitmap != null) {
                    usr_pic.setImageBitmap(bitmap);
                }
            }
        }

        findViewById(R.id.EditProfilePic).setOnClickListener(CreateOrgActivity.this);
        findViewById(R.id.btn_creat_org).setOnClickListener(CreateOrgActivity.this);

        orgState = (AutoCompleteTextView) findViewById(R.id.AutoComepleteOrgState);
        final ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.State));

        orgState.setAdapter(adapter);
        orgState.setThreshold(1);
        orgState.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    state = adapter.getItem(position).toString();
                } catch (java.lang.NullPointerException e) {
                    e.printStackTrace();
                }

            }
        });

        orgState.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                state = "";
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onClick(View v) {

        EditText orgName, orgBranch, department, orgAddress, orgCity, orgZip;

        TextView orgCountry;
        final Intent intent;
        final JSONObject org = new JSONObject();

        int i = v.getId();
        if (i == R.id.EditProfilePic) {
            intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, SELECT_PICTURE);
        } else if (i == R.id.btn_creat_org) {

            boolean failFlag = false;

            orgName = (EditText) findViewById(R.id.EditTextOrgName);

            orgTag = (EditText) findViewById(R.id.EditOrgTag);

            orgBranch = (EditText) findViewById(R.id.EditOrgBranch);

            department = (EditText) findViewById(R.id.EditDepartment);

            orgAddress = (EditText) findViewById(R.id.EditOrgAddress);

            orgCountry = (TextView) findViewById(R.id.EditOrgCountry);

            orgCity = (EditText) findViewById(R.id.EditOrgCity);

            orgZip = (EditText) findViewById(R.id.EditOrgZip);

            if (orgName.getText().toString().trim().length() == 0) {
                failFlag = true;
                orgName.setError("A value is required");
            }

            if (!Character.isLetter(orgName.getText().toString().trim().charAt(0))) {
                failFlag = true;
                orgName.setError("Organization name should start with a letter");
            }
            if (orgTag.getText().toString().trim().length() == 0) {
                failFlag = true;
                orgTag.setError("A value is required");
            }
            if (orgTag.getText().toString().trim().length() < 3) {
                failFlag = true;
                orgTag.setError("tag should at least 3 character");
            }
            if (!Character.isLetter(orgTag.getText().toString().trim().charAt(0))) {
                failFlag = true;
                orgTag.setError("Tag should start with a letter");
            }
            if (orgAddress.getText().toString().trim().length() == 0) {
                failFlag = true;
                orgAddress.setError("A value is required");
            }
            if (orgCountry.getText().toString().trim().length() == 0) {
                failFlag = true;
                orgCountry.setError("A value is required");
            }
            if (state == null || state.trim().length() == 0 || !Arrays.asList(getResources().getStringArray(R.array.State)).contains(state)) {
                failFlag = true;
                orgState.setError("A value is required");
            }
            if (orgCity.getText().toString().trim().length() == 0) {
                failFlag = true;
                orgCity.setError("A value is required");
            }
            if (orgZip.getText().toString().trim().length() == 0) {
                failFlag = true;
                orgZip.setError("A value is required");
            }

            if (!failFlag) {

                try {
                    org.put("name", orgName.getText().toString().trim());
                    org.put("tag", orgTag.getText().toString().trim());
                    if (orgBranch.getText().toString().trim().length() == 0) {
                        org.put("branch", "");
                    } else {
                        org.put("branch", orgBranch.getText().toString().trim());
                    }
                    if (department.getText().toString().trim().length() == 0) {
                        org.put("department", "");
                    } else {
                        org.put("department", department.getText().toString().trim());
                    }

                    org.put("address", orgAddress.getText().toString().trim());
                    org.put("country", orgCountry.getText().toString().trim());
                    org.put("state", orgState.getText().toString().trim());
                    org.put("city", orgCity.getText().toString().trim());
                    org.put("zip", orgZip.getText().toString().trim());


                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                }

            }

            if (bitmap != null && !failFlag) {

                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Loading");
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.show();
                final Context c = this.getApplicationContext();


                StorageReference storageRef = storage.getReferenceFromUrl(getString(R.string.firebase_storage));
                StorageReference mountainsRef = storageRef.child(orgName.getText().toString().trim());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();

                uploadTask = mountainsRef.putBytes(data);


                // Listen for state changes, errors, and completion of the upload.
                uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        mProgressDialog.setProgress((int) progress);
                        System.out.println("Upload is " + progress + "% done");
                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                        System.out.println("Upload is paused");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        mProgressDialog.dismiss();
                        Toast.makeText(c, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Handle successful uploads on complete
                        mProgressDialog.dismiss();
                        Uri downloadUrl = taskSnapshot.getMetadata().getDownloadUrl();
                        Log.d(TAG, "downloadUrl :" + downloadUrl);
                        try {
                            org.put("org_pic", downloadUrl);
                            submit(org);
                        } catch (org.json.JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(c, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                        }

                    }
                });

            } else if (!failFlag) {

                try {
                    Log.d(TAG, "default pic");
                    org.put("org_pic", "default");
                    submit(org);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                }

            }

        }

    }

    private void submit(JSONObject obj) {

        Log.d(TAG, "calling submit");

        new GetClass(CreateOrgActivity.this).execute(obj);

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && null != data) {
                Uri selectedImage = data.getData();
                Log.d(TAG, "selectedImageUri: " + selectedImage.toString());
                CircularImageView imageView = (CircularImageView) findViewById(R.id.EditProfilePic);
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
                imageView.setImageBitmap(bitmap);
            }
        } catch (java.io.FileNotFoundException | java.lang.NullPointerException e) {
            Log.e(TAG, e.toString());
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;

        switch (item.getItemId()) {

            case R.id.nav_inbox: {

                intent = new Intent(CreateOrgActivity.this, InboxActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_outbox: {

                intent = new Intent(CreateOrgActivity.this, OutboxActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_add_employee: {

                intent = new Intent(CreateOrgActivity.this, AddEmployeeActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_pair_prg: {

                intent = new Intent(CreateOrgActivity.this, PairOrgActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_add_org: {

                intent = new Intent(CreateOrgActivity.this, CreateOrgActivity.class);
                startActivity(intent);
                break;

            }

            case R.id.nav_settings: {
                intent = new Intent(CreateOrgActivity.this, SettingActivity.class);
                startActivity(intent);
                break;

            }


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class GetClass extends AsyncTask<JSONObject, Void, Void> {

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
        protected Void doInBackground(JSONObject... params) {
            try {

                Log.d(TAG, "In background job");
                final JSONObject newOrg = params[0];


                Uri uri = new Uri.Builder()
                        .scheme("http")
                        .encodedAuthority(getString(R.string.server_ur_templ))
                        .path(getString(R.string.tag))
                        .appendPath(newOrg.getString("tag"))
                        .build();

                URL url = new URL(uri.toString());
                Log.d(TAG, "url:" + url.toString());

                prefs = getSharedPreferences(getString(R.string.private_file), MODE_PRIVATE);
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
                Log.d(TAG, "Get parameters : " + newOrg.getString("tag"));
                Log.d(TAG, "Header parameter : " + auth);
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
                                new AlertDialog.Builder(context)
                                        .setTitle("Tag already exist")
                                        .setMessage("Tag name already exist, Please choose unique tag name")
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                orgTag.setError("Same tag already exist");
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                                break;

                            }
                            case 404: {

                                new PostClass(CreateOrgActivity.this).execute(newOrg);
                                break;

                            }
                            default: {

                                Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                                onPostExecute();

                            }
                        }
                    }
                });

            } catch (IOException | JSONException | NullPointerException e) {
                e.printStackTrace();
                Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                onPostExecute();
            }
            return null;
        }

        protected void onPostExecute() {
            progress.dismiss();
        }

    }

    private class PostClass extends AsyncTask<JSONObject, Void, Void> {

        private Context context;

        public PostClass(Context c) {
            this.context = c;
        }

        protected void onPreExecute() {
            progress = new ProgressDialog(this.context);
            progress.setMessage("Loading");
            progress.show();
        }

        @Override
        protected Void doInBackground(JSONObject... params) {
            try {

                Uri uri = new Uri.Builder()
                        .scheme("http")
                        .encodedAuthority(getString(R.string.server_ur_templ))
                        .path(getString(R.string.org))
                        .build();

                URL url = new URL(uri.toString());
                Log.d(TAG, "url:" + url.toString());

                prefs = getSharedPreferences(getString(R.string.private_file), MODE_PRIVATE);
                String auth = prefs.getString("uid", "null");
                if (auth.equals("null")) {
                    onPostExecute();
                    //@TODO add alert
                }

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization", auth);
                connection.setDoOutput(true);

                Log.d(TAG, "params:" + params.toString());

                final JSONObject newOrg = params[0];

                DataOutputStream dStream = new DataOutputStream(connection.getOutputStream());
                dStream.writeBytes(newOrg.toString());
                dStream.flush();
                dStream.close();

                int responseCode = connection.getResponseCode();

                Log.d(TAG, "Sending 'POST' request to URL : :" + url);
                Log.d(TAG, "Post parameters : " + newOrg.toString());
                Log.d(TAG, "Header parameter : " + auth);
                Log.d(TAG, "Response Code : " + responseCode);

                final int response = responseCode;

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

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        onPostExecute();

                        try {

                            switch (response) {

                                case 200: {

                                    new AlertDialog.Builder(context)
                                            .setTitle("Success")
                                            .setMessage("Organization  " + newOrg.getString("name") + " created successfully")
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(CreateOrgActivity.this, CreateOrgActivity.class);
                                                    finish();
                                                    startActivity(intent);
                                                }
                                            })
                                            .setIcon(R.drawable.ic_done_black_24dp)
                                            .show();

                                    break;
                                }
                                default: {
                                    throw new org.json.JSONException("409");
                                }
                            }

                        } catch (org.json.JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                            onPostExecute();
                        }

                    }
                });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "Opss Something went wrong please try again later", Toast.LENGTH_SHORT).show();
                onPostExecute();
            }
            return null;
        }

        protected void onPostExecute() {
            progress.dismiss();
        }

    }
}
