package com.google.android.gms.samples.vision.ocrreader.view;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.samples.vision.ocrreader.R;
import com.google.android.gms.samples.vision.ocrreader.model.User;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UsersActivity extends AppCompatActivity {

    @BindView(R.id.lv_user_list)
    ListView mUserListView;

    private FirebaseListAdapter<User> mFirebaseListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        ButterKnife.bind(this);

        DatabaseReference mUsersDatabaseReference = FirebaseDatabase.getInstance().getReference()
                .child("blind-users");

        Query query = mUsersDatabaseReference.limitToLast(50);

        FirebaseListOptions<User> options = new FirebaseListOptions.Builder<User>()
                .setQuery(query, User.class)
                .setLayout(R.layout.user_list_item)
                .build();

        mFirebaseListAdapter = new FirebaseListAdapter<User>(options) {
            @Override
            protected void populateView(View v, User user, int position) {
                TextView userName = v.findViewById(R.id.tv_list_item_user_name);
                userName.setText(user.getUsername());
            }
        };

        mUserListView.setAdapter(mFirebaseListAdapter);

        /*
        mUserListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                DatabaseReference mUserReference = mFirebaseListAdapter.getRef(position);
                String uid = mUserReference.getKey();

                Intent chatActivityIntent = new Intent(UsersActivity.this, ChatActivity.class);
                chatActivityIntent.putExtra(Intent.EXTRA_TEXT, uid);
                startActivity(chatActivityIntent);
            }
        });
        */
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFirebaseListAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFirebaseListAdapter.stopListening();
    }
}
