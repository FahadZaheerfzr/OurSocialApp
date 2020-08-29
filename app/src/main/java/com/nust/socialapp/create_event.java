package com.nust.socialapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.Continuation;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;


public class create_event extends AppCompatActivity {

    private ImageButton Selecteventimg;
    private EditText eventtitle;
    private EditText eventdetails;
    private Button CreateEvent;

    private static final int Gallery_Pick = 1;
    private Uri ImageUri;
    private String Description,title;

    private StorageReference PostsImagesRefrence;
    private DatabaseReference UsersRef, PostsRef;
    private FirebaseAuth mAuth;

    private ProgressDialog loadingBar;


    private String saveCurrentDate, saveCurrentTime, postRandomName, downloadUrl, current_user_id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        loadingBar = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();
        PostsImagesRefrence = FirebaseStorage.getInstance().getReference();
        current_user_id = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("profiles");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Event Posts");

        CreateEvent = (Button) findViewById(R.id.createeventbutton);
        Selecteventimg = (ImageButton) findViewById(R.id.eventimage);
        eventtitle = (EditText) findViewById(R.id.eventtitle);
        eventdetails = (EditText) findViewById(R.id.eventdetail);

        Selecteventimg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenGallery();
            }
        });

        CreateEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ValidatePostInfo();
            }
        });
    }


    private void OpenGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Gallery_Pick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==Gallery_Pick && resultCode==RESULT_OK && data!=null)
        {
            ImageUri = data.getData();
            Selecteventimg.setImageURI(ImageUri);
        }
    }





    private void ValidatePostInfo()
    {
        Description = eventdetails.getText().toString();
        title = eventtitle.getText().toString();
        if(ImageUri == null)
        {
            Toast.makeText(this, "Please select post image...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(Description))
        {
            Toast.makeText(this, "Please say something about your image...", Toast.LENGTH_SHORT).show();
        }
        else if(TextUtils.isEmpty(title))
        {
            Toast.makeText(this, "Please give title...", Toast.LENGTH_SHORT).show();
        }
        else
        {
            loadingBar.setTitle("Add New Post");
            loadingBar.setMessage("Please wait, while we are updating your new post...");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(true);

            StoringImageToFirebaseStorage();
        }
    }

    private void StoringImageToFirebaseStorage()
    {
        Calendar calFordDate = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
        saveCurrentDate = currentDate.format(calFordDate.getTime());

        Calendar calFordTime = Calendar.getInstance();
        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calFordDate.getTime());

        postRandomName = saveCurrentDate + saveCurrentTime;


        StorageReference filePath = PostsImagesRefrence.child("Event Images").child(ImageUri.getLastPathSegment() + postRandomName + ".jpg");

        filePath.putFile(ImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return filePath.getDownloadUrl();
            }}).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task)
            {
                if(task.isSuccessful())
                {
                    downloadUrl = task.getResult().toString();
                    Toast.makeText(create_event.this, "image uploaded successfully to Storage...", Toast.LENGTH_SHORT).show();

                    SavingPostInformationToDatabase();

                }
                else
                {
                    String message = task.getException().getMessage();
                    Toast.makeText(create_event.this, "Error occured: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void SavingPostInformationToDatabase() {
        UsersRef.child(current_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    String userFullName = dataSnapshot.child("username").getValue().toString();
                    String userProfileImage = dataSnapshot.child("photoUrl").getValue().toString();

                    HashMap postsMap = new HashMap();
                    postsMap.put("uid", current_user_id);
                    postsMap.put("date", saveCurrentDate);
                    postsMap.put("time", saveCurrentTime);
                    postsMap.put("description", Description);
                    postsMap.put("title", title);
                    postsMap.put("postimage", downloadUrl);
                    postsMap.put("profileimage", userProfileImage);
                    postsMap.put("fullname", userFullName);
                    PostsRef.child(current_user_id + postRandomName).updateChildren(postsMap)
                            .addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task)
                                {
                                    if(task.isSuccessful())
                                    {
                                        SendToEventActivity();
                                        Toast.makeText(create_event.this, "New Post is updated successfully.", Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                    else
                                    {
                                        Toast.makeText(create_event.this, "Error Occured while updating your post.", Toast.LENGTH_SHORT).show();
                                        loadingBar.dismiss();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void SendToEventActivity () {
        Intent createEventtoEvent = new Intent(this, evet_page.class);
        startActivity(createEventtoEvent);
    }
}