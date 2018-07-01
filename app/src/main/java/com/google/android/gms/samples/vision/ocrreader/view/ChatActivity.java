package com.google.android.gms.samples.vision.ocrreader.view;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.samples.vision.ocrreader.R;
import com.google.android.gms.samples.vision.ocrreader.model.BlindMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity {

    @BindView(R.id.et_message_text)
    EditText mMessageTextEditText;

    @BindView(R.id.bt_send)
    Button mSendButton;

    @BindView(R.id.lv_chats)
    ListView mChatsListView;

    private DatabaseReference mUsersChatDatabaseReference;
    private FirebaseUser mCurrentUser;
    private FirebaseListAdapter<BlindMessage> mChatsFirebaseListAdapter;

    private static final int MESSAGE_MAX_LENGTH = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ButterKnife.bind(this);

        String mUid = getIntent().getStringExtra(Intent.EXTRA_TEXT);

        if (mUid == null)
            throw new IllegalArgumentException("Must pass uid");

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mUsersChatDatabaseReference = FirebaseDatabase.getInstance().getReference()
                        .child("blind-chats")
                        .child(mUid)
                        .child(mCurrentUser.getUid());

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                String author = null;
                if (currentUser != null) {
                    author = usernameFromEmail(currentUser.getEmail());
                }

                BlindMessage message =
                        new BlindMessage(mCurrentUser.getUid(), author, mMessageTextEditText.getText().toString());

                mUsersChatDatabaseReference.push().setValue(message);

                mMessageTextEditText.setText("");

            }
        });

        mMessageTextEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() < 1 ) {
                    mSendButton.setEnabled(false);
                } else {
                    mSendButton.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mMessageTextEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MESSAGE_MAX_LENGTH)});


        Query query = mUsersChatDatabaseReference.limitToLast(50);

        FirebaseListOptions<BlindMessage> options = new FirebaseListOptions.Builder<BlindMessage>()
                .setQuery(query, BlindMessage.class)
                .setLayout(R.layout.message_list_item)
                .build();

        mChatsFirebaseListAdapter = new FirebaseListAdapter<BlindMessage>(options) {
            @Override
            protected void populateView(View v, BlindMessage message, int position) {
                TextView author = v.findViewById(R.id.tv_list_item_user_name);
                TextView text = v.findViewById(R.id.tv_chats_message);

                author.setText(message.getAuthor());
                text.setText(message.getmText());
            }
        };
        mChatsListView.setAdapter(mChatsFirebaseListAdapter);
    }

    private String usernameFromEmail(String email) {
        if (email.contains("@")) {
            return email.split("@")[0];
        } else {
            return email;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mChatsFirebaseListAdapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        mChatsFirebaseListAdapter.stopListening();
    }

}
