package com.yangjunrui.gcffaceprovider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVFile;
import com.avos.avoscloud.AVOSCloud;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.ProgressCallback;
import com.avos.avoscloud.SaveCallback;
import com.dexafree.materialList.cards.BasicButtonsCard;
import com.dexafree.materialList.cards.BasicImageButtonsCard;
import com.dexafree.materialList.cards.OnButtonPressListener;
import com.dexafree.materialList.cards.SmallImageCard;
import com.dexafree.materialList.controller.OnDismissCallback;
import com.dexafree.materialList.model.Card;
import com.dexafree.materialList.view.MaterialListView;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    static final String SETTINGS_INITIALIZED = "init";
    static final String SETTINGS_WELCOMED = "welcome";
    static final String SETTINGS_PERSON = "person";
//    static final String SETTINGS_URLS = "urls";
    static final String SETTINGS_NAME = "name";
    static final String CARD_WELCOME = "welcome";
    static final String CARD_NAME = "name";
    static final String CARD_FACE_ADD = "face_add";
    static final String CARD_FACE = "face";
    static final int ACTION_SELECT_PHOTO = 1;
    static final String AVOS_APP_ID = "nXhyQUoDPPBUPB1UfCA5vEcg";
    static final String AVOS_APP_KEY = "eRCBDDswmcykNY05lqC9UkJq";
    static final String AVOS_PERSON_CLASS = "Person";
    static final String AVOS_PERSON_URLS = "urls";
    //TODO: implement advance sharing
    static final boolean ADVANCE_SHARING = false;
    public Context context;
    private MaterialListView mListView;
    private SharedPreferences mSharedPreferences;
    private BasicButtonsCard mNameCard;
    private AVObject mPerson = null;
    private String mPersonData;

    private AVObject getPerson(){
        if (mPerson == null) {
            try {
                mPerson = AVObject.parseAVObject(mPersonData);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mPerson;
    }

    private void changeName(String name) {
        //TODO: update web data
        mNameCard.setDescription(name);
        mSharedPreferences.edit().putString(SETTINGS_NAME, name).commit();
    }


    private void setInitialized(){
        //TODO: start broadcasting
        mSharedPreferences.edit().putBoolean(SETTINGS_WELCOMED, true).commit();
    }

    private void addFaceCard(String url){
        mListView.add(createFaceCard(url));
    }

    private Card createWelcomeCard() {
        BasicButtonsCard card = new BasicButtonsCard(context);
        card.setTitle("Hello");
        card.setTitleColor(Color.WHITE);
        card.setDescription("Welcome to share your face with your context. You can can change " +
                "your name and add a few faces below. Dismiss this to start sharing your context!");
        card.setDismissible(true);
        card.setTag(new Pair<String, String>(CARD_WELCOME, null));
        card.setLeftButtonText("OK!");
        card.setLeftButtonTextColor(Color.WHITE);
        card.setBackgroundColor(Color.BLUE);
        card.setOnLeftButtonPressedListener(new OnButtonPressListener() {
            @Override
            public void onButtonPressedListener(View view, Card card) {
                mListView.remove(card);
            }
        });
        return card;
    }

    private Card createNameCard(String name) {
        BasicButtonsCard card = new BasicButtonsCard(context);
        card.setTag(new Pair<String, String>(CARD_NAME, null));
        card.setTitle("Name: ");
        card.setDescription(name);
        card.setLeftButtonText("Change");
        card.setOnLeftButtonPressedListener(new OnButtonPressListener() {
            @Override
            public void onButtonPressedListener(View view, Card card) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Title");

                // Set up the input
                final EditText input = new EditText(context);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeName(input.getText().toString());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
        return mNameCard = card;
    }

    private Card createFaceAdderCard() {
        BasicImageButtonsCard card = new BasicImageButtonsCard(context);
        card.setTag(new Pair<String, String>(CARD_FACE_ADD, null));
        card.setTitle("Add new face");
        card.setDescription("Tap the button below to add a face");
        card.setLeftButtonText("Add");
        card.setOnLeftButtonPressedListener(new OnButtonPressListener() {
            @Override
            public void onButtonPressedListener(View view, Card card) {
                //TODO: implement this function
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, ACTION_SELECT_PHOTO);
            }
        });
        return card;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch(requestCode) {
            case ACTION_SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final File tempFile = File.createTempFile("photo", ".jpg");
                        MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri)
                                .compress(Bitmap.CompressFormat.JPEG, 50,
                                        new FileOutputStream(tempFile));
                        final AVFile file = AVFile.withFile("", tempFile);
                        file.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(AVException e) {
                                if (e == null) {
                                    Log.i(TAG, "uploaded file");
                                    showToast("Uploaded!");
                                    getPerson().put(AVOS_PERSON_URLS, getPerson().getJSONArray(AVOS_PERSON_URLS).put(file.getUrl()));
                                    getPerson().saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(AVException e) {
                                            mSharedPreferences.edit().putString(SETTINGS_PERSON, getPerson().toString()).apply();
                                            Log.i(TAG, "urls: " + getPerson().getJSONArray(AVOS_PERSON_URLS));
                                            Log.i(TAG, "person: " + getPerson().toString());
                                        }
                                    });
                                    mListView.add(createFaceCard(file.getUrl()));
                                } else {
                                    Log.e(TAG, "upload failed: " + e.getMessage());
                                    showToast("Upload failed! Check your Internet?");
                                }
                            }
                        }, new ProgressCallback() {
                            @Override
                            public void done(Integer integer) {
                                Log.i(TAG, "upload in progress: "+ integer + "%");
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

    private Card createFaceCard(final String url) {
        Log.i(TAG, "createFaceCard: " + url);
        final SmallImageCard card = new SmallImageCard(context);
        card.setTitle("face");
        card.setTag(new Pair<>(CARD_FACE, url));
        card.setDescription(url);
        card.setDismissible(true);
        card.setImageScaleType(ImageView.ScaleType.FIT_START);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                Bitmap x;

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();

                x = BitmapFactory.decodeStream(input);
                card.setDrawable(new BitmapDrawable(x));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return card;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        AVOSCloud.useAVCloudUS();
        AVOSCloud.initialize(context, AVOS_APP_ID, AVOS_APP_KEY);
        AVOSCloud.setDebugLogEnabled(true);

        mListView = (MaterialListView) this.findViewById(R.id.listView);
        mSharedPreferences =
                this.getSharedPreferences(getString(R.string.settings_file), Context.MODE_PRIVATE);

        mPersonData = mSharedPreferences.getString(SETTINGS_PERSON, "");
        if (!mSharedPreferences.getBoolean(SETTINGS_INITIALIZED, false) || mPersonData == "") {

            mPerson = new AVObject(AVOS_PERSON_CLASS);
            mPerson.put(AVOS_PERSON_URLS, new JSONArray());
            mPerson.saveInBackground(new SaveCallback() {
                @Override
                public void done(AVException e) {
                    if (e == null) {
                        mSharedPreferences.edit().putString(SETTINGS_PERSON, mPerson.toString()).commit();
                        mSharedPreferences.edit().putBoolean(SETTINGS_INITIALIZED, true).commit();
                        Log.i(TAG, "person: " + mPerson.toString());
                        Log.i(TAG, "profile created");
                        showToast("Profile created!");
                    } else {
                        Log.e(TAG, "creation failed: " + e.getMessage());
                        showToast("failed to connect to AVOSCloud, check your Internet?");
                    }
                }
            });
        }

        mListView.setOnDismissCallback(new OnDismissCallback() {
            @Override
            public void onDismiss(Card card, int i) {
                showToast("Card dismissed: " + i);
                Pair<String, String> cardTag = (Pair<String, String>) card.getTag();
                String cardType = (cardTag).first;
                if (cardType.equals(CARD_WELCOME)) {
                    setInitialized();
                } else if (cardType.equals(CARD_FACE)) {
                    JSONArray urls = getPerson().getJSONArray(AVOS_PERSON_URLS);
                    int deleteIdx = -1;
                    for (int j = 0; j < urls.length(); j++) {
                        try {
                            if (urls.getString(j).equals(cardTag.second)) {
                                deleteIdx = j;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    urls.remove(deleteIdx);
                    getPerson().put(AVOS_PERSON_URLS, urls);
                    getPerson().saveInBackground(new SaveCallback() {
                        @Override
                        public void done(AVException e) {
                            Log.i(TAG, "person: " + getPerson().toString());
                            mSharedPreferences.edit().putString(SETTINGS_PERSON, getPerson().toString()).apply();
                            Log.i(TAG, "urls: " + getPerson().getJSONArray(AVOS_PERSON_URLS));
                        }
                    });
                }
            }
        });
        if (!mSharedPreferences.getBoolean(SETTINGS_WELCOMED, false)) {
            mListView.add(createWelcomeCard());
        }
        String name = mSharedPreferences.getString(SETTINGS_NAME, "");
        if (name.equals("")) {
            mListView.add(createNameCard("Your name here"));
        } else {
            mListView.add(createNameCard(name));
        }

        mListView.add(createFaceAdderCard());

        JSONArray urls = getPerson().getJSONArray(AVOS_PERSON_URLS);
        for (int i = 0; i < urls.length(); i++) {
            try {
                addFaceCard(urls.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_main);
    }

    /**
     * A {@link Handler} for showing {@link Toast}s.
     */
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(String text) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.add_face) {
            Set<String> keys = mSharedPreferences.getAll().keySet();
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            for (String key : keys) {
                editor.remove(key);
            }
            editor.commit();
        }

        return super.onOptionsItemSelected(item);
    }
}
