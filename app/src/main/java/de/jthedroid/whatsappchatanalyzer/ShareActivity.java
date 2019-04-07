package de.jthedroid.whatsappchatanalyzer;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        findViewById(R.id.progressBarLoading).setVisibility(View.VISIBLE);
        findViewById(R.id.textViewLoading).setVisibility(View.VISIBLE);

        final LoadingViewModel viewModel = android.arch.lifecycle.ViewModelProviders.of(this).get(LoadingViewModel.class);
        final Observer<Chat> chatObserver = new Observer<Chat>() {
            @Override
            public void onChanged(@Nullable Chat c) {
                if (c != null) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    if (getSupportFragmentManager().findFragmentByTag("headingGraph1") == null) {
                        HeadingFragment headingSender = HeadingFragment.newInstance(getString(R.string.total_messages_time));
                        transaction.add(R.id.linearLayoutSender, headingSender, "headingGraph1");
                    }
                    if (getSupportFragmentManager().findFragmentByTag("graphView1") == null) {
                        TimeGraphFragment tgf = TimeGraphFragment.newInstance(c.getTotalMessagesGraph());       //TODO: import useful data
                        transaction.add(R.id.linearLayoutSender, tgf, "graphView1");
                    }
                    if (getSupportFragmentManager().findFragmentByTag("headingSender") == null) {
                        HeadingFragment headingSender = HeadingFragment.newInstance(String.format(Locale.getDefault(), "%s (%d)", getString(R.string.sent_messages), c.getMsgCount()));
                        transaction.add(R.id.linearLayoutSender, headingSender, "headingSender");
                    }
                    for (Sender sender : c.sortedSenders) {
                        if (getSupportFragmentManager().findFragmentByTag(sender.toString()) == null) {
                            SenderFragment sf = SenderFragment.newInstance(sender.name, sender.getMsgCount(), c.getMaxMsgCount());
                            transaction.add(R.id.linearLayoutSender, sf, sender.toString());
                        }
                    }
                    transaction.commit();
                    findViewById(R.id.progressBarLoading).setVisibility(View.GONE);
                    findViewById(R.id.textViewLoading).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.progressBarLoading).setVisibility(View.GONE);
                    findViewById(R.id.textViewLoading).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.textViewLoading)).setText(R.string.error_loading);
                }
            }
        };
        Intent intent = getIntent();
        if (savedInstanceState == null) {

            //String action = intent.getAction();
            String type = intent.getType();

            if (type != null) {
                //save title String in viewModel
                String title = intent.getStringExtra(Intent.EXTRA_TEXT);
                if (title.contains("\"")) {
                    title = title.substring(title.indexOf('"') + 1, title.lastIndexOf('"'));
                }
                setTitle(title);
                viewModel.title.setValue(title);
                //start loading chat .txt file
                Uri uri = null;
                ArrayList<Parcelable> extraList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (extraList != null && !extraList.isEmpty()) {
                    Parcelable p = extraList.get(0);
                    if (p instanceof Uri) {
                        uri = (Uri) p;
                    }
                }
                if (uri == null) {
                    Toast.makeText(this, R.string.toast_faulty_data, Toast.LENGTH_LONG).show();
                }
                viewModel.load(getContentResolver(), uri);
                viewModel.chat.observe(this, chatObserver);
            }
        } else {
            //load title
            setTitle(viewModel.title.getValue());
            //load data from Chat
            chatObserver.onChanged(viewModel.chat.getValue());
        }
    }
}
