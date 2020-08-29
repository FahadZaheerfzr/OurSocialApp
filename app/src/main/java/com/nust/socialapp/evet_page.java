package com.nust.socialapp;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class evet_page extends AppCompatActivity {

    private FloatingActionButton fab_posts;

    private RecyclerView postList;
    private Toolbar mToolbar;


    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef, PostsRef,interestedref;
    FirebaseRecyclerAdapter<Event, EventViewHolder> FBRA;


    String currentUserID;
    Boolean isinterested=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evet_page);
        fab_posts= findViewById(R.id.fab);
        fab_posts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUsertoCreatePost();
            }
        });

        postList = (RecyclerView) findViewById(R.id.eventpostlist);
        postList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        postList.setLayoutManager(linearLayoutManager);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("profiles");
        PostsRef = FirebaseDatabase.getInstance().getReference().child("Event Posts");
        interestedref = FirebaseDatabase.getInstance().getReference().child("Event Interested");

        // DisplayAllUsersPosts();
    }

    @Override
    protected void onStart() {
        super.onStart();




        FirebaseRecyclerOptions<Event> options = new FirebaseRecyclerOptions.Builder<Event>()
                .setQuery(PostsRef, Event.class)
                .build();

        FBRA = new FirebaseRecyclerAdapter<Event, EventViewHolder>(options) {

            @NonNull
            @Override
            public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {

                View view = LayoutInflater.from(evet_page.this).inflate(R.layout.activity_event_item, parent, false);

                return new EventViewHolder(view);

            }

            @Override
            protected void onBindViewHolder(@NonNull EventViewHolder viewHolder, int position, @NonNull Event model) {
                final String postkey = getRef(position).getKey();
                viewHolder.setFullname(model.getFullname());
                //  viewHolder.setTime(model.getTime());
                // viewHolder.setDate(model.getDate());
                viewHolder.setDescription(model.getDescription());
                viewHolder.setProfileimage(getApplicationContext(), model.getProfileimage());
                viewHolder.setPostimage(getApplicationContext(), model.getPostimage());

                viewHolder.setInterestedStatus(postkey);

                viewHolder.EventInterestedbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        isinterested = true;
                        interestedref.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(isinterested.equals(true)){
                                    if(dataSnapshot.child(postkey).hasChild(currentUserID)){

                                        interestedref.child(postkey).child(currentUserID).removeValue();
                                        isinterested = false;
                                    }else {
                                        interestedref.child(postkey).child(currentUserID).setValue(true);
                                        isinterested = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }

        };

        postList.setAdapter(FBRA);

        FBRA.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(FBRA != null) {
            FBRA.stopListening();
        }
    }


    public static class EventViewHolder extends RecyclerView.ViewHolder
    {
        View mView;

        ImageButton EventInterestedbutton;
        TextView InterestedNumber;
        int countinterested;
        String currentuserid;
        DatabaseReference interestedref;

        public EventViewHolder(View itemView)
        {
            super(itemView);
            mView = itemView;
            EventInterestedbutton = (ImageButton) mView.findViewById(R.id.Eventinterested);
            InterestedNumber = (TextView) mView.findViewById(R.id.eventinterestedtext);
            currentuserid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            interestedref = FirebaseDatabase.getInstance().getReference().child("Event Interested");
        }
        public void setInterestedStatus(final String postkey){
            interestedref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(postkey).hasChild(currentuserid)){

                        countinterested = (int) dataSnapshot.child(postkey).getChildrenCount();
                        EventInterestedbutton.setImageResource(R.drawable.ic_like_active);
                        InterestedNumber.setText(Integer.toString(countinterested)+(" people interested"));

                    }else {

                        countinterested = (int) dataSnapshot.child(postkey).getChildrenCount();
                        EventInterestedbutton.setImageResource(R.drawable.ic_like);
                        InterestedNumber.setText(Integer.toString(countinterested)+(" people interested"));

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        public void setFullname(String fullname)
        {
            TextView username = (TextView) mView.findViewById(R.id.eventfullnameTextView);
            username.setText(fullname);
        }

        public void setProfileimage(Context ctx, String profileimage)
        {
            CircleImageView image = (CircleImageView) mView.findViewById(R.id.authorImageView2);
            Picasso.with(ctx).load(profileimage).into(image);
        }



        public void setDescription(String description)
        {
            TextView PostDescription = (TextView) mView.findViewById(R.id.detailsTextView3);
            PostDescription.setText(description);
        }

        public void setPostimage(Context ctx1,  String postimage)
        {
            ImageView PostImage = (ImageView) mView.findViewById(R.id.eventImageView);
            Picasso.with(ctx1).load(postimage).into(PostImage);
        }

    }

    private void SendUsertoCreatePost() {

        Intent createpost = new Intent(evet_page.this,create_event.class);
        startActivity(createpost);

    }
}