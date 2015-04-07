package com.csce462.drummachine;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.csce462.drummachine.util.SystemUiHider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends Activity {
	private static final boolean AUTO_HIDE = true;
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
	private static final boolean TOGGLE_ON_CLICK = true;
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	private SystemUiHider mSystemUiHider;

	Context context;

	private SeekBar tempoSlider;
	private TextView tempoTV;
	private TextView playButton;
	private TextView clearButton;
	private TextView octaveButton;
	private TextView keyButton;
	private TextView majorMinorButton;

	MusicPlayerThread musicPlayerThread;
	private boolean isPlaying;

	private GridView buttonsGrid;
	private GridView sequenceGrid;

	private int tempo;
	private int octave = 4;
	private int key = 0;
	private boolean isMajor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.context = this;

		setContentView(R.layout.activity_main);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View setTempoButton = findViewById(R.id.setTempoButton);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, setTempoButton, HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if(mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if(mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						}
						else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
						}

						if(visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		setTempoButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				}
				else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		tempoSlider = (SeekBar) findViewById(R.id.tempo_seekbar);
		tempoSlider.setOnTouchListener(mDelayHideTouchListener);
		tempoSlider.setOnSeekBarChangeListener(tempoSliderChanged);
		tempo = 60;

		tempoTV = (TextView) findViewById(R.id.tempo_tv);

		buttonsGrid = (GridView) findViewById(R.id.buttonsGrid);
		sequenceGrid = (GridView) findViewById(R.id.sequenceGrid);

		clearButton = (TextView) findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<ButtonView> gridButtons = ((ButtonsAdapter) buttonsGrid.getAdapter()).getItems();

				for(ButtonView buttonView : gridButtons) {
					buttonView.setChecked(false);
				}
			}
		});

		isPlaying = false;
		playButton = (TextView) findViewById(R.id.playButton);
		playButton.getBackground().setColorFilter(
				Color.parseColor("#11A800"), PorterDuff.Mode.MULTIPLY
		);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(!isPlaying) {
					musicPlayerThread = new MusicPlayerThread();
					musicPlayerThread.execute();
					playButton.setText("Stop");
					playButton.getBackground().setColorFilter(
							Color.parseColor("#FF4D4D"), PorterDuff.Mode.MULTIPLY
					);
					isPlaying = true;
				}
				else {
					musicPlayerThread.cancel(true);
					playButton.setText("Play");
					playButton.getBackground().setColorFilter(
							Color.parseColor("#11A800"), PorterDuff.Mode.MULTIPLY
					);

					isPlaying = false;
				}
			}
		});

		octave = 4;
		octaveButton = (TextView) findViewById(R.id.octaveButton);
		octaveButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				octave = (octave + 1) % 7;
				octaveButton.setText("Octave (" + octave + ")");
			}
		});

		key = 0;
		keyButton = (TextView) findViewById(R.id.keyButton);
		keyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MIDIWriter.Key[] keys = MIDIWriter.Key.values();

				key = (key + 1) % keys.length;
				keyButton.setText("Key (" + keys[key].name + ")");
			}
		});

		isMajor = true;
		majorMinorButton = (TextView) findViewById(R.id.major_minorButton);
		majorMinorButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				isMajor = !isMajor;
				if(isMajor) {
					majorMinorButton.setText("Major");
				}
				else {
					majorMinorButton.setText("Minor");
				}
			}
		});

		setupGridButtons();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}


	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if(AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

//Tempo slider and textview control
//------------------------------------------------------------------------------
	private SeekBar.OnSeekBarChangeListener tempoSliderChanged = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			tempo = progress + 45;
			tempoTV.setText(tempo + " BPM");
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

		}
	};

//GridView for buttons
//------------------------------------------------------------------------------
	private void setupGridButtons() {
		ArrayList<ButtonView> buttonsList = new ArrayList<>();
		for(int i = 0; i < 64; i++) {
			buttonsList.add(new ButtonView(context));
		}
		ButtonsAdapter buttonsAdapter = new ButtonsAdapter(buttonsList);
		buttonsAdapter.setItemsClickable(true);
		buttonsGrid.setAdapter(buttonsAdapter);
		buttonsGrid.setOnItemClickListener(buttonClickListener);

		ArrayList<ButtonView> sequenceList = new ArrayList<>();
		for(int i = 0; i < 8; i++) {
			sequenceList.add(new ButtonView(context));
		}
		ButtonsAdapter sequenceAdapter = new ButtonsAdapter(sequenceList);
		sequenceAdapter.setItemsClickable(false);
		sequenceGrid.setAdapter(sequenceAdapter);
	}

	private class ButtonView extends FrameLayout implements Checkable {
		boolean isChecked;
		int row;
		int column;

		private int[] colors = new int[] {
				Color.parseColor("#67FFB1"),
				Color.parseColor("#91E86A"),
				Color.parseColor("#FFF674"),
				Color.parseColor("#E8BB5E"),
				Color.parseColor("#FF9A67"),
				Color.parseColor("#E457E8"),
				Color.parseColor("#895FFF"),
				Color.parseColor("#7AB4FF")
		};

		public ButtonView(Context context) {
			super(context);

			setClickable(false);
		}

		public void setPosition(int row, int column) {
			this.row = row;
			this.column = column;
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			super.onMeasure(widthMeasureSpec, widthMeasureSpec);
		}

		@Override
		public void setChecked(boolean checked) {
			isChecked = checked;

			Drawable background = getBackground();
			if(isChecked) background.setColorFilter(colors[row], PorterDuff.Mode.MULTIPLY);
			else background.setColorFilter(null);
		}

		@Override
		public boolean isChecked() {
			return isChecked;
		}

		@Override
		public void toggle() {
			setChecked(!isChecked);

		}

		public void setButtonClickable(boolean clickable) {
			if(clickable) setBackgroundResource(R.drawable.background_button);
			else setBackgroundResource(R.drawable.bg_sequence_button);
		}
	}

	private AdapterView.OnItemClickListener buttonClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int column = position % 8;
			int row = (int) Math.floor(position / 8);

			ButtonView button = (ButtonView) view;
			button.toggle();
		}
	};

	private class ButtonsAdapter extends BaseAdapter {

		ArrayList<ButtonView> buttons;

		public ButtonsAdapter(ArrayList<ButtonView> buttons) {
			this.buttons = buttons;

			int i = 0;
			for(ButtonView button : buttons) {
				int column = i % 8;
				int row = (int) Math.floor(i / 8);
				button.setPosition(row, column);
				i++;
			}
		}

		@Override
		public int getCount() {
			return buttons.size();
		}

		@Override
		public Object getItem(int position) {
			return buttons.get(position);
		}

		public ArrayList<ButtonView> getItems() {
			return buttons;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return buttons.get(position);
		}

		public void setItemsClickable(boolean clickable) {
			for(ButtonView buttonView : buttons) buttonView.setButtonClickable(clickable);
		}
	}


//Play sequence handler
//------------------------------------------------------------------------------
	private class MusicPlayerThread extends AsyncTask<Void, Integer, Void> {
		ArrayList<ButtonView> sequenceButtons;
		MediaPlayer[] mediaPlayer = new MediaPlayer[8];

		public MusicPlayerThread() {
			sequenceButtons = ((ButtonsAdapter) sequenceGrid.getAdapter()).getItems();
		}

		private void createMIDIFile(int column) {
			MIDIWriter mf = new MIDIWriter();
			mf.tempoChange(tempo);
			mf.keySigChange(0, 0);
			mf.timeSigChange(4, 4);
			mf.progChange(88);

			//turn on appropriate notes in column
			for(int row = 0; row < 8; row++) {
				int position = column + row*8;

				if(((ButtonView) buttonsGrid.getAdapter().getItem(position)).isChecked()) {

					int noteValue;
					if(isMajor) {
						noteValue = MIDIWriter.noteValue(
								MIDIWriter.MajorScale.values()[7-row].noteValue,
								octave,
								key
						);
					}
					else {
						noteValue = MIDIWriter.noteValue(
								MIDIWriter.MinorScale.values()[7-row].noteValue,
								octave,
								key
						);
					}

					mf.noteOn (0, noteValue, 127);
				}
			}

			//turn off appropriate notes in column
			boolean isFirst = true;
			for(int row = 0; row < 8; row++) {
				int position = column + row*8;

				if(((ButtonView) buttonsGrid.getAdapter().getItem(position)).isChecked()) {
					int noteValue;
					if(isMajor) {
						noteValue = MIDIWriter.noteValue(
								MIDIWriter.MajorScale.values()[7-row].noteValue,
								octave,
								key
						);
					}
					else {
						noteValue = MIDIWriter.noteValue(
								MIDIWriter.MinorScale.values()[7-row].noteValue,
								octave,
								key
						);
					}

					int time;
					if(isFirst) {
						isFirst = false;
						time = MIDIWriter.CROTCHET;
					}
					else {
						time = 0;
					}

					mf.noteOff(time, noteValue);
				}
			}


			try {
				File file = new File(context.getCacheDir(), "note.mid");
				mf.writeToFile(file);
				Uri fileURI = Uri.fromFile(file);
				if(mediaPlayer[column] != null) {
					mediaPlayer[column].release();
					mediaPlayer[column] = null;
				}
				mediaPlayer[column] = MediaPlayer.create(context, fileURI);
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}


		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);

			for(ButtonView buttonView : sequenceButtons) {
				buttonView.setChecked(false);
			}
			sequenceButtons.get(values[0]).setChecked(true);
		}

		@Override
		protected Void doInBackground(Void... params) {
			int column = 0;
			while(true) {
				if(isCancelled()) return null;
				else {
					createMIDIFile(column);
					publishProgress(column);
					mediaPlayer[column].start();

					column = (column + 1) % 8;

					try {
						Thread.sleep((60*1000)/tempo);
					}
					catch(InterruptedException ie) {
						ie.printStackTrace();
					}
				}
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);

			for(MediaPlayer mp: mediaPlayer) {
				mp.release();
				mp = null;
			}

			for(ButtonView buttonView : sequenceButtons) {
				buttonView.setChecked(false);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();

			for(MediaPlayer mp: mediaPlayer) {
				if(mp != null) {
					mp.release();
					mp = null;
				}
			}

			for(ButtonView buttonView : sequenceButtons) {
				buttonView.setChecked(false);
			}
		}
	}


}
