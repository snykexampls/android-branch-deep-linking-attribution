package io.branch.testbed;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	EditText txtUrl;
	Button cmdRefreshUrl;
	TextView txtInstallCount;
	TextView txtInstallCreditCount;
	TextView txtEventCount;
	TextView txtEventCreditCount;
	Button cmdRefreshCounts;
	Button cmdCreditInstall;
	Button cmdCreditBuy;
	Button cmdCommitBuy;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		txtUrl = (EditText) findViewById(R.id.editReferralUrl);
		cmdRefreshUrl = (Button) findViewById(R.id.cmdRefreshURL);
		txtInstallCount = (TextView) findViewById(R.id.txtInstallCount);
		txtEventCount = (TextView) findViewById(R.id.txtEventCount);
		cmdRefreshCounts = (Button) findViewById(R.id.cmdRefreshCounts);
		cmdCreditInstall = (Button) findViewById(R.id.cmdCreditInstall);
		cmdCreditBuy = (Button) findViewById(R.id.cmdCreditBuy);
		cmdCommitBuy = (Button) findViewById(R.id.cmdCommitBuyAction);
		
		cmdRefreshUrl.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//txtUrl.setText(Branch.getReferralURL());
			}
		});
		
		cmdRefreshCounts.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		cmdCreditInstall.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		cmdCreditBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
		cmdCommitBuy.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
			}
		});
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		final Branch branch = Branch.getInstance(this.getApplicationContext(), "fake_key");
		branch.initUserSession(new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams) {
				Log.i("BranchTestBed", "branch init complete!");
				JSONObject params = branch.getReferringParams();
				try {
					Iterator<?> keys = params.keys();
					while (keys.hasNext()) {
						String key = (String) keys.next();
						Log.i("BranchTestBed", key + ", " + referringParams.getString(key));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}



}