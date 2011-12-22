/*
 * Copyright (C) 2011 Ben Collins-Sussman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.templaro.opsiz.aka;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class AndroidomaticKeyerActivity extends Activity {
	
	private static final int MENU_EDIT = 0;
	private static final int MENU_COPY = 1;
	private static final int MENU_MOVE_UP = 2;
	private static final int MENU_MOVE_DOWN = 3;
	private static final int MENU_DELETE = 4;
	
	private String TAG = "AndroidomaticKeyer";
	private Thread soundThread;
	private Button playButton;
	private EditText keyerEditText;
	private int hertz = 800; 
	private int speed = 15;
	private int darkness = 0;
	private MorsePlayer player = new MorsePlayer(hertz, speed);
	private ListView messageList;
	private ArrayList<String> messages = new ArrayList<String>();
	private boolean cwMode = true;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Initialize layout");
        setContentView(R.layout.main);
        LoadMessages();
        DisplayMessages();

        soundThread = new Thread(new Runnable() {
	            @Override
	            public void run() {
	        	   player.playMorse();
	            }
	        });
        
        playButton = (Button)findViewById(R.id.playButton);
        playButton.setOnClickListener(playButtonListener);
        
        keyerEditText = (EditText)findViewById(R.id.keyerEditText);
        
    }
    
 
	private void LoadMessages() {
    	 //TODO: Populate String array from SQLite database 
        //for now, read initial strings from an xml resource file
        Log.i(TAG, "Importing canned messages");
        String [] importedMessages = getResources().getStringArray(R.array.messages_array);
        for (String s: importedMessages) {
        	messages.add(s);
        }
        Log.i(TAG, "Canned messages imported");	
	}
	
    private void DisplayMessages() {
    	messageList = (ListView)findViewById(R.id.messageList);
        //TODO: A custom listview -- smaller fonts, etc.
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
        		android.R.layout.simple_list_item_1, messages);
        messageList.setAdapter(adapter);
        registerForContextMenu(messageList);

  	  	messageList.setOnItemClickListener(new OnItemClickListener() {
  	  		public void onItemClick(AdapterView<?> parent, View view,
  	  				int position, long id) {
  	    		keyerEditText.setText(((TextView) view).getText().toString());
  	  		}
  	  	});
  	  Log.i(TAG, "Displaed Message Listview");	
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.mode:
            switchMode();
            return true;
        case R.id.help:
    		startActivity(new Intent(this, Help.class));
            return true;
        case R.id.settings:
        	startActivity(new Intent(this, Settings.class));
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void switchMode() {
    	//TODO: Revise UI to reflect change of mode
    	//(in addition to menu button text changing)
    	Log.i(TAG, "Switched to " + (String) ((cwMode) ? "Hell" : "CW" + " mode"));
    	cwMode = !cwMode;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if(cwMode){
            menu
            .findItem(R.id.mode)
            .setTitle(R.string.toHell_label);
    	}
    	else {
    		menu
    		.findItem(R.id.mode)
    		.setTitle(R.string.toCW_label);
    	}
            return super.onPrepareOptionsMenu(menu);
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo) {
      if (v.getId()==R.id.messageList) {
        menu.setHeaderTitle(getResources().getString(R.string.messageOptions));
        String[] menuItems = getResources().getStringArray(R.array.message_options_array);
        for (int i = 0; i<menuItems.length; i++) {
          menu.add(Menu.NONE, i, i, menuItems[i]);
        }
      }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
    	int menuItemIndex = item.getItemId();
    	String listItemName = messages.get(info.position);
    	switch (menuItemIndex) {
    	case MENU_EDIT:
    		Log.i(TAG, String.format("Editing message %s at index %d", 
    				listItemName, info.position));
    		return true;
    	case MENU_COPY:
    		Log.i(TAG, String.format("Copying message at index %d", info.position));
    		messages.add(info.position, messages.get(info.position));
    		messageList.invalidateViews();
    		return true;
    	case MENU_MOVE_UP:
    		if(info.position>0){
    			Log.i(TAG, String.format("Try to move up message at index %d", info.position));
    			messages.add(info.position-1,messages.get(info.position));
    			messages.remove(info.position+1);
    			messageList.invalidateViews();
    		}
    		else {
    			Toast.makeText(getApplicationContext(), "Can't move up any further.",
    			          Toast.LENGTH_SHORT).show();
    		}
    		return true;
    	case MENU_MOVE_DOWN:
    		Log.i(TAG, String.format("Try to move down message at index %d", info.position));
    		if(info.position<(messages.size()-1)){
    			messages.add(info.position,messages.get(info.position+1));
    			messages.remove(info.position+2);
    			messageList.invalidateViews();
    		}
    		else {
    			Toast.makeText(getApplicationContext(), "Can't move down any further.",
    			          Toast.LENGTH_SHORT).show();
    		}
    		return true;
    	case MENU_DELETE:
    		Log.i(TAG, String.format("Delete message at index %d", info.position));
    		messages.remove(info.position);
    		messageList.invalidateViews();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    } 
    
    private OnClickListener playButtonListener = new OnClickListener() {
        public void onClick(View v) {
        	if (soundThread.isAlive()) {
        		stopMessage();
        	} else {
        		startMessage();
        	}
        }
    };
    
    
    /** Called whenever activity comes (or returns) to foreground. */
    @Override
    public void onStart() {
            super.onStart();
            getSettings();  // make sure user prefs are applied
    }
    
    /** Called by onStart(), to make sure prefs are loaded whenever we return
     *  to the main AKA activity (e.g. after backing out of the 
     *  Preferences activity)
     */
    private void getSettings() {
    		Log.i(TAG, "Loading saved preferences");
             SharedPreferences prefs = 
                     PreferenceManager.getDefaultSharedPreferences(getBaseContext());
             hertz = prefs.getInt("sidetone", 800);
             speed = prefs.getInt("wpm", 15);
             darkness = prefs.getInt("hellTiming", 0);
    }
    
    /** Called when activity stops */
    @Override
    protected void onStop(){
       super.onStop();

      //save the current preferences for next time...
       Log.i(TAG, "Saving current preferences");
       SharedPreferences prefs = 
               PreferenceManager.getDefaultSharedPreferences(getBaseContext());
      SharedPreferences.Editor editor = prefs.edit();
      editor.putInt("sidetone", hertz);
      editor.putInt("wpm", speed);
      editor.putInt("hellTiming", darkness);
      editor.commit();
    }
    
	// Play sound (infinite loop) on separate thread from main UI thread.
    void startMessage() {
    	if (soundThread.isAlive()) {
    		Log.i(TAG, "Trying to stop old thread first...");
    		stopMessage();
    	}
    	Log.i(TAG, "Starting morse thread with new message.");
    	player.setMessage(keyerEditText.getText().toString());
    	player.setSpeed(speed);
    	player.setTone(hertz);
    	soundThread = new Thread(new Runnable() {
	           @Override
	            public void run() {
	        	   player.playMorse();
	            }
	        });
    	soundThread.start();
    	playButton.setText("STOP");
    }
    
    void stopMessage() {
    	if (soundThread.isAlive()) {
    		Log.i(TAG, "Stopping existing morse thread.");
    		soundThread.interrupt();
    		try {
    			soundThread.join();  // wait for thread to die
    		} catch (InterruptedException e) {
    			Log.i(TAG, "Main thread interrupted while waiting for child to die!");
    		}
    	}
    	playButton.setText("START");
    }
}


