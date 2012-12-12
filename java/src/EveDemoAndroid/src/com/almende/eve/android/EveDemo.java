package com.almende.eve.android;

import com.almende.cape.CapeClient;
import com.fasterxml.jackson.databind.node.ArrayNode;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class EveDemo extends Activity {
	private Button connect;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eve_demo);
        
        connect = (Button) findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				new ConnectionTask().execute();
			}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_eve_demo, menu);
        return true;
    }
    
    class ConnectionTask extends AsyncTask<Void, String, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			connect();
			return null;
		}
		
	    protected void onProgressUpdate(String message) {
	        // showInfo(message); // TODO
	    }

	    private void connect () {
	    	try {
				cape.login("alex", "alex");
				
				ArrayNode contacts = cape.getContacts(null);
				System.out.println("contacts:" + contacts);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    private CapeClient cape = new CapeClient();
}
