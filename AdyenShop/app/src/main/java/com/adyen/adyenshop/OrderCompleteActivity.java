package com.adyen.adyenshop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by andrei on 3/10/16.
 */
public class OrderCompleteActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_complete);

        String message = getIntent().getStringExtra("completionMessage");
        TextView orderCompletionMessageTextView = (TextView)findViewById(R.id.order_completion_message);
        orderCompletionMessageTextView.setText(message);

        Button continueButton = (Button) findViewById(R.id.button_continue_shopping);
        continueButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(OrderCompleteActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        OrderCompleteActivity.this.startActivity(intent);
    }

}
