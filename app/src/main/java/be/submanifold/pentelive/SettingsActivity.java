package be.submanifold.pentelive;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(getString(R.string.settings));
        myToolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(myToolbar);


        ((ToggleButton) findViewById(R.id.avatarToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(SettingsActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, false));
        ((ToggleButton) findViewById(R.id.tbOnlyToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(SettingsActivity.this, PrefUtils.PREFS_TBONLY_KEY, false));
        ((ToggleButton) findViewById(R.id.inAppSoundsToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(SettingsActivity.this, PrefUtils.PREFS_INAPPSOUNDSOFF_KEY, false));
        ((ToggleButton) findViewById(R.id.noBeginnerWarningsToggleButton)).setChecked(PrefUtils.getBooleanFromPrefs(SettingsActivity.this, PrefUtils.PREFS_NOBEGINNERACCEPTREMIND_KEY, false));
        ((ToggleButton) findViewById(R.id.emailMeToggleButton)).setChecked(PentePlayer.emailMe);
        ((ToggleButton) findViewById(R.id.personalizeAdsToggleButton)).setChecked(PentePlayer.personalizeAds);
        ((ToggleButton) findViewById(R.id.avatarToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanToPrefs(SettingsActivity.this, PrefUtils.PREFS_LOADAVATARS_KEY, isChecked);
                if (!isChecked) {
                    PentePlayer.avatars.clear();
                    PentePlayer.pendingAvatarChecks.clear();
                }
            }
        });
        ((ToggleButton) findViewById(R.id.tbOnlyToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanToPrefs(SettingsActivity.this, PrefUtils.PREFS_TBONLY_KEY, isChecked);
                PentePlayer.showOnlyTB = isChecked;
            }
        });
        ((ToggleButton) findViewById(R.id.inAppSoundsToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanToPrefs(SettingsActivity.this, PrefUtils.PREFS_INAPPSOUNDSOFF_KEY, isChecked);
            }
        });
        ((ToggleButton) findViewById(R.id.noBeginnerWarningsToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PrefUtils.saveBooleanToPrefs(SettingsActivity.this, PrefUtils.PREFS_NOBEGINNERACCEPTREMIND_KEY, isChecked);
            }
        });
        ((ToggleButton) findViewById(R.id.personalizeAdsToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ChangeAdsPersonalizationPreferenceTask task = new ChangeAdsPersonalizationPreferenceTask(isChecked);
                task.execute((Void) null);
            }
        });
        ((ToggleButton) findViewById(R.id.emailMeToggleButton)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ChangeEmailPreferenceTask task = new ChangeEmailPreferenceTask(isChecked);
                task.execute((Void) null);
            }
        });
        ((Button) findViewById(R.id.preferencesButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://www.pente.org/gameServer/myprofile/prefs?name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                Intent intent = new Intent(SettingsActivity.this, WebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        });
        ((Button) findViewById(R.id.subscribeButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://www.pente.org/gameServer/subscriptions?name2=" + PentePlayer.mPlayerName + "&password2=" + PentePlayer.mPassword;
                Intent intent = new Intent(SettingsActivity.this, WebViewActivity.class);
                intent.putExtra("url", url);
                startActivity(intent);
            }
        });
        ((Button) findViewById(R.id.colorButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PentePlayer.mSubscriber) {
                    return;
                }
                ColorPickerDialogBuilder
                        .with(SettingsActivity.this)
                        .setTitle(getString(R.string.choose_color))
                        .initialColor(PentePlayer.myColor)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(12)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {
//                                toast("onColorSelected: 0x" + Integer.toHexString(selectedColor));
                            }
                        })
                        .setPositiveButton("ok", new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                PentePlayer.myColor = selectedColor;
                                ChangeColorTask colorTask = new ChangeColorTask(Integer.toHexString(selectedColor).substring(2));
                                colorTask.execute((Void) null);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();
            }
        });
        ((Button) findViewById(R.id.avatarButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PentePlayer.mSubscriber) {
                    return;
                }
                final CharSequence[] items = {getString(R.string.take_photo), getString(R.string.choose_from_library),
                        getString(R.string.cancel)};
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(getString(R.string.change_avatar));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (items[item].equals(getString(R.string.take_photo))) {
                            Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                            // Ensure that there's a camera activity to handle the intent
                            if (takePicture.resolveActivity(getPackageManager()) != null) {
                                // Create the File where the photo should go
                                File photoFile = null;
                                try {
                                    photoFile = createImageFile();
                                } catch (IOException ex) {
                                    System.out.println("oops");
                                }
                                // Continue only if the File was successfully created
                                if (photoFile != null) {
                                    Uri photoURI = FileProvider.getUriForFile(SettingsActivity.this,
                                            "be.submanifold.pentelive.fileprovider",
                                            photoFile);
                                    takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                                    startActivityForResult(takePicture, 0);
                                }
                            }
                        } else if (items[item].equals(getString(R.string.choose_from_library))) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, 1);//one can be replaced with any action code
                        } else if (items[item].equals(getString(R.string.cancel))) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.activityResumed(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.activityPaused();
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if (resultCode != RESULT_OK) {
//            System.out.println("result kitty fuck " + resultCode);
            return;
        }
        Bitmap imageBitmap = null;
        if (requestCode == 1) {
            Uri selectedImage = imageReturnedIntent.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
//            Bundle extras = imageReturnedIntent.getExtras();
//            imageBitmap = (Bitmap) extras.get("data");
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / 300, photoH / 300);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        }
        if (imageBitmap == null) {
            return;
        }
//        ((ImageView) findViewById(R.id.avatarView)).setImageBitmap(imageBitmap);
//        if (resultCode != RESULT_OK) {
//            System.out.println("result kitty fuck " + resultCode);
//            return;
//        }
//        if (selectedImage == null) {
//            System.out.println("kitty fuck");
//            return;
//        }
//        try {
        Bitmap bitmap = imageBitmap;
//        System.out.println("kitty image " + bitmap.getWidth() + " " + bitmap.getHeight());
        float maxSize = 300f;
        Bitmap newImg = scaleBitmap(bitmap, maxSize);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        newImg.compress(Bitmap.CompressFormat.JPEG, 75, stream);
        byte[] byteArray = stream.toByteArray();
        while (byteArray.length > 65535) {
            maxSize -= 10f;
            newImg = scaleBitmap(bitmap, maxSize);
            stream = new ByteArrayOutputStream();
            newImg.compress(Bitmap.CompressFormat.JPEG, 75, stream);
            byteArray = stream.toByteArray();
        }

        UploadAvatarTask avatarTask = new UploadAvatarTask(byteArray);
        avatarTask.execute((Void) null);

//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap scaleBitmap(Bitmap bm, float maxSize) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float ratio;
        if (width > height) {
            ratio = maxSize / width;
        } else {
            ratio = maxSize / height;
        }

        bm = Bitmap.createScaledBitmap(bm, (int) (ratio * width), (int) (ratio * height), true);
        return bm;
    }

    private static class UploadAvatarTask extends AsyncTask<Void, Void, Boolean> {

        private final byte[] bytes;

        UploadAvatarTask(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                DataOutputStream dataOutputStream;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                //                System.out.println("colorStr was " + colorString);
                URL url = new URL("https://www.pente.org/gameServer/changeAvatar");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
//creating new dataoutputstream
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"avatar\";filename=\""
                        + 1 + "\"" + lineEnd);
                dataOutputStream.writeBytes("Content-Type: image/jpg" + lineEnd);
                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.write(bytes, 0, bytes.length);
                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                dataOutputStream.flush();
                dataOutputStream.close();

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for loadplayer was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

                System.out.println(output.toString());


            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

        }

        @Override
        protected void onCancelled() {
        }
    }

    private static class ChangeColorTask extends AsyncTask<Void, Void, Boolean> {

        private final String colorString;

        ChangeColorTask(String colorString) {
            this.colorString = colorString;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                System.out.println("colorStr was " + colorString);
                URL url = new URL("https://www.pente.org/gameServer/changeColor?changeNameColor=" + colorString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for loadplayer was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                String dashboardString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {

        }

        @Override
        protected void onCancelled() {
        }
    }

    private static class ChangeEmailPreferenceTask extends AsyncTask<Void, Void, Boolean> {

        private final boolean emailMe;

        ChangeEmailPreferenceTask(boolean emailMeChoice) {
            this.emailMe = emailMeChoice;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                System.out.println("colorStr was " + colorString);
                URL url = new URL("https://www.pente.org/gameServer/changeEmailPreference?emailMe=" +
                        (emailMe ? "Y" : "N"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for changeEmailPreference was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                String dashboardString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                PentePlayer.emailMe = emailMe;
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

    private class ChangeAdsPersonalizationPreferenceTask extends AsyncTask<Void, Void, Boolean> {

        private final boolean personalizeAds;

        ChangeAdsPersonalizationPreferenceTask(boolean personalizeAds) {
            this.personalizeAds = personalizeAds;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
//                System.out.println("colorStr was " + colorString);
                URL url = new URL("https://www.pente.org/gameServer/changeAdsPreference?personalizeAds=" +
                        (personalizeAds ? "Y" : "N"));
                if (PentePlayer.development) {
                    new URL("https://development.pente.org/gameServer/changeAdsPreference?personalizeAds=" +
                            (personalizeAds ? "Y" : "N"));
                }
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("response code for personalizeAdsPreference was " + responseCode);
                    return false;
                }

                StringBuilder output = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//                System.out.println("output===============" + br);
                String line = "";
                while ((line = br.readLine()) != null) {
                    output.append(line + "\n");
                }
                br.close();

//                System.out.println(output);

                String dashboardString = output.toString();

            } catch (IOException e1) {
                e1.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            if (success) {
                PentePlayer.personalizeAds = personalizeAds;
                PrefUtils.saveBooleanToPrefs(SettingsActivity.this, PrefUtils.PREFS_PERSONALIZEDADS_KEY, personalizeAds);
            }
        }

        @Override
        protected void onCancelled() {
        }
    }

}
