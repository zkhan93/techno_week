package com.app.iw;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.app.innovationweek.R;
import com.app.iw.model.Option;
import com.app.iw.model.Question;
import com.app.iw.model.User;
import com.app.iw.model.dao.DaoSession;
import com.app.iw.model.firebase.LeaderboardItem;
import com.app.iw.model.firebase.Response;
import com.app.iw.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class QuestionActivity extends AppCompatActivity implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = QuestionActivity.class.getSimpleName();

    @BindView(R.id.radio_group_options)
    RadioGroup optionRadioGroup;
    @BindView(R.id.submit)
    Button submit;
    @BindView(R.id.statement)
    TextView textViewQuestionStatement;
    @BindView(R.id.timer)
    TextView timer;
    @BindView(R.id.edit_text_fib)
    EditText editTextFIB;
    @BindView(R.id.image)
    ImageView questionImageView;

    @BindView(R.id.progress)
    View progress;
    @BindView(R.id.progress_msg)
    TextView progressMsg;

    @BindView(R.id.content)
    View content;
    @BindView(R.id.error)
    View error;

    @BindView(R.id.retry)
    Button retry;
    @BindView(R.id.error_msg)
    TextView errorMsg;
    @BindView(R.id.status)
    TextView status;

    private CountDownTimer countDownTimer;
    private DatabaseReference dbRef, questionRef, responseRef, leaderBoardRef, currentQuestionRef;
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    private String quizId, questionId, Uid;
    private ValueEventListener questionListener, responseListener, currentQuestionListener;

    private DaoSession daoSession;
    private long startTime;
    /**
     * The question this activity is showing
     */
    private Question question;

    {
        currentQuestionListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue(String.class) != null) {
                    questionId = dataSnapshot.getValue(String.class);
                    questionRef = dbRef.child(quizId).child(questionId);
                    responseRef = dbRef.child("response").child(quizId).child(Uid).child(questionId);
                    questionRef.addValueEventListener(questionListener);
                } else {
                    Toast.makeText(getApplicationContext(), "No question active right now for this quiz!", Toast.LENGTH_LONG).show();
                    QuestionActivity.this.finish();
                }
                Log.d(TAG, "Current Question Id is: " + questionId);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "No Questions Present");
                Toast.makeText(getApplicationContext(), "No Question active right now!", Toast.LENGTH_LONG).show();
                QuestionActivity.this.finish();

            }
        };
        questionListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                Log.d(TAG, "datasnap after fetching: " + dataSnapshot.toString());
                question = dataSnapshot.getValue(Question.class);
                responseRef.addListenerForSingleValueEvent(responseListener);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.w(TAG, "Question Listener", databaseError.toException());
                Toast.makeText(getApplicationContext(), "There was an error retrieving the question. Please contact the admins.", Toast.LENGTH_LONG).show();
            }
        };
        responseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "response datasnbap:" + dataSnapshot);
                if (dataSnapshot.getValue() == null) {
                    // Record start time
                    responseRef.child("startTime").setValue(ServerValue.TIMESTAMP);
                    Toast.makeText(getApplicationContext(), "Start Time Recorded", Toast.LENGTH_LONG).show();
                } else {

                    if (dataSnapshot.hasChild("endTime")) {
                        Toast.makeText(getApplicationContext(), "You have already answered this question.", Toast.LENGTH_LONG).show();
                        QuestionActivity.this.finish();
                    } else {
                        Log.d(TAG, "no startTime");
                    }
                }
                calculateTime();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "response failed:" + databaseError);
            }
        };
    }


    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        ButterKnife.bind(this);
        retry.setOnClickListener(this);
        submit.setOnClickListener(this);
        showProgress(null);
        //initialize the question listener
        dbRef = FirebaseDatabase.getInstance().getReference();
        Uid = Utils.getUid(getApplicationContext());
        if (savedInstanceState == null) {
            Intent launchIntent = getIntent();
            if (launchIntent != null) {
                Bundle bundle = launchIntent.getExtras();
                quizId = bundle.getString("quiz_id");
                questionId = bundle.getString("question_id");
            }
        } else {
            Uid = savedInstanceState.getString("uid");
            quizId = savedInstanceState.getString("quiz_id");
            questionId = savedInstanceState.getString("question_id");
            question = savedInstanceState.getParcelable("question");
        }


        //check if user eligible for this quiz
        daoSession = ((EchelonApplication) getApplication()).getDaoSession();
        User user = daoSession.getUserDao().load(Utils.getUid(QuestionActivity.this));
        Log.d(TAG, "User is: " + user);

        if (!user.getCanThinkQuick() && quizId.equals("thinkQuick")) {
            Toast.makeText(getApplicationContext(), "You have not nominated yourself for this quiz during the nomination period. You are not eligible to answer questions for this quiz.", Toast.LENGTH_LONG).show();
            QuestionActivity.this.finish();
        }

        Log.d(TAG, "Bundle is: " + savedInstanceState);
        Log.d(TAG, "Uid is: " + Uid);
        Log.d(TAG, "quizId is: " + quizId);

        leaderBoardRef = dbRef.child("leaderboard").child(quizId).child(Uid);
        currentQuestionRef = dbRef.child("currentQuestion").child(quizId);
        currentQuestionRef.addListenerForSingleValueEvent(currentQuestionListener);
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.isInternetConnected(getApplicationContext())) {
            status.setVisibility(View.GONE);
            submit.setEnabled(true);
        } else {
            status.setVisibility(View.VISIBLE);
            submit.setEnabled(false);
        }
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);


    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("quiz_id", quizId);
        outState.putString("question_id", questionId);
        outState.putParcelable("question", question);
        outState.putString("uid", Uid);
        super.onSaveInstanceState(outState);
    }

    private void showProgress(String message) {
        progressMsg.setText(message == null || message.isEmpty() ? getString(R.string.loading)
                : message);
        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);
        error.setVisibility(View.GONE);


    }

    private void hideProgress(boolean showError, String errorMessage) {
        progress.setVisibility(View.GONE);
        if (showError) {
            content.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        } else {
            content.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
        }
    }

    public void onRadioButtonOptionClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
    }

    /**
     * Sets the question to the layout
     * Question id is set as tag in the question Statement text view
     * Option ids are set as tag in the option radio button
     * The optionId of the correct Option is stored as tag in the radioGroup
     * The correct fib response is stored as tag in fib edit text
     */
    private void setQuestion() {

        textViewQuestionStatement.setTag(question.getQuestionId());
        textViewQuestionStatement.setText(question.getStatement());
        // set the options if options are present
        if (question.getOptions() == null || question.getOptions().size() == 0) {
            // this is an fib question, so hide the options
            optionRadioGroup.setVisibility(View.GONE);
            // and prepare the view for fib
            editTextFIB.setVisibility(View.VISIBLE);
            // and save the correct response as tag
            editTextFIB.setTag(question.getFibAnswer());
            // and hide the radio group
            optionRadioGroup.setVisibility(View.GONE);
        } else {
            // set the options
            int i = 0;
            for (Map.Entry<String, Option> entry : question.getOptions().entrySet()) {

                RadioButton radioButton = ((RadioButton) optionRadioGroup.getChildAt(i++));
                radioButton.setText(entry.getValue().getValue());
                radioButton.setTag(entry.getKey());
                if (entry.getValue().getCorrect())
                    optionRadioGroup.setTag(entry.getKey());
            }
            // show the options
            optionRadioGroup.setVisibility(View.VISIBLE);
            //hide the fib
            editTextFIB.setVisibility(View.GONE);
        }
        // check if the optional image uri is present
        if (question.getImgUri() != null) {
            //show the image view
            questionImageView.setVisibility(View.VISIBLE);
            //load the image using Picasso
            Picasso.with(this)
                    .load(question.getImgUri())
                    .placeholder(R.drawable.loading_placeholder)
                    .error(R.drawable.loading_placeholder)
                    .into(questionImageView, new Callback() {
                        @Override
                        public void onSuccess() {
//                            //startTimer();
                        }

                        @Override
                        public void onError() {

                        }
                    });

        } else {
            questionImageView.setVisibility(View.GONE);
        }
        hideProgress(false, null);
    }

    // 10 minutes = 1000 * 60 * 10 milliseconds
    // check if the timer had already started by Fetching remaining seconds from firebase RTD for that user
    // if yes, start the countdown timer with those milli seconds
    //if no start the timer with initial  value 10 minutes and System.currentTimeMillis. persist the states in RDB
    //if yes, start the timer with the remaining seconds and persist the data base
    private void calculateTime() {
        //max time is 15 minutes hard coded

        final long elapsedSeconds = (System.currentTimeMillis() - question.getStartTime()) / 1000;
        System.out.println(TAG + ": Elapsed seconds are:" + elapsedSeconds);
        countDownTimer = new CountDownTimer((question.getMaxTime() * 60 - elapsedSeconds) * 1000, 1000) {
            long minLeft, secLeft;

            @Override
            public void onTick(long millisecondsLeft) {
                secLeft = millisecondsLeft / 1000;
                if (secLeft <= 300)
                    timer.setTextColor(ContextCompat.getColor(QuestionActivity.this, R.color.colorPrimary));
                else
                    timer.setTextColor(ContextCompat.getColor(QuestionActivity.this, R.color.black));
                if (secLeft > 60) {
                    minLeft = secLeft / 60;
                    secLeft = secLeft % 60;
                    timer.setText(getString(R.string.timer_msg, minLeft, secLeft));
                } else {
                    timer.setText(getString(R.string.timer_msg_sec, secLeft));
                }

            }

            @Override
            public void onFinish() {
                timer.setText(getString(R.string.timer_expire));
                submit.setEnabled(false);
                countDownTimer.cancel();
            }
        };
        countDownTimer.start();
        setQuestion();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retry:

                break;
            case R.id.submit:
                showProgress(getString(R.string.saving_response));
                //this was a mcq
                if (question.getOptions() != null && question.getOptions().size() != 0) {
                    String answer = (String) findViewById(optionRadioGroup.getCheckedRadioButtonId()).getTag();
                    int score = optionRadioGroup.getTag().toString().equals(answer) ? 5 : 0;
                    saveResponse(answer, score);
                } else /*This was an fib*/ {
                    String answer = editTextFIB.getText().toString();
                    int score = editTextFIB.getTag().toString().equalsIgnoreCase(answer.trim()) ? 5 : 0;
                    saveResponse(answer, score);
                }
                break;
        }
    }

    private void saveResponse(final String answer, final int score) {
        responseRef.child("endTime").setValue(ServerValue.TIMESTAMP).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    //start and end time recorded. Now get values, compare time difference and save score
                    responseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            final Response response = dataSnapshot.getValue(Response.class);
                            long difference = response.getEndTime() - response.getStartTime();

                            System.out.println(TAG + " Difference is: " + difference);


                            if (question.getStartTime() > response.getEndTime() || question.getEndTime() < response.getEndTime()) {
                                Toast.makeText(getApplicationContext(), "You exceeded the time limit. Your response is invalid.", Toast.LENGTH_LONG).show();
                                response.setScore(0);
                                response.setResponse(answer);
                                response.setDuration(difference);
                                response.setLimitExceeded(true);
                            } else {
                                //update score and time duration for that response, although duration is redundant
                                response.setScore(score);
                                response.setResponse(answer);
                                response.setDuration(difference);
                                response.setLimitExceeded(false);
                            }
                            responseRef.setValue(response).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Toast.makeText(getApplicationContext(), "Your response has been saved.", Toast.LENGTH_LONG).show();
                                    //response is saved, update leaderboard
                                    leaderBoardRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            LeaderboardItem leaderboardItem;
                                            //if the user is answering for the first time
                                            if (dataSnapshot.getValue(LeaderboardItem.class) != null)
                                                leaderboardItem = dataSnapshot.getValue(LeaderboardItem.class);
                                            else
                                                //if the user has answered below
                                                leaderboardItem = new LeaderboardItem();
                                            System.out.println(TAG + "LeaderboardItem is: " + leaderboardItem);

                                            leaderboardItem.setTotalScore(leaderboardItem.getTotalScore() + response.getScore());
                                            leaderboardItem.setTotalTime(leaderboardItem.getTotalTime() + response.getDuration());
                                            if (response.getScore() > 0)
                                                leaderboardItem.setCorrect(leaderboardItem.getCorrect() + 1);
                                            else
                                                leaderboardItem.setIncorrect(leaderboardItem.getIncorrect() + 1);
                                            leaderBoardRef.setValue(leaderboardItem);
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                        }
                                    });
                                    QuestionActivity.this.finish();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("is_internet_connected")) {
            if (sharedPreferences.getBoolean("is_internet_connected", false)) {
                //remove status text and enable submit button
                status.setVisibility(View.GONE);
                submit.setEnabled(true);
            } else {
                //make status text visible and disable submit button
                submit.setEnabled(false);
                status.setVisibility(View.VISIBLE);
            }
        }
    }
}
