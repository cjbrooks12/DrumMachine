package com.csce462.drummachine;

/*
  A simple Java class that writes a MIDI file

  (c)2011 Kevin Boone, all rights reserved
*/

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;


public class MIDIWriter {
	// Note lengths
	//  We are working with 32 ticks to the crotchet. So
	//  all the other note lengths can be derived from this
	//  basic figure. Note that the longest note we can
	//  represent with this code is one tick short of a
	//  two semibreves (i.e., 8 crotchets)

	static final int SEMIQUAVER = 4;
	static final int QUAVER = 8;
	static final int CROTCHET = 16;
	static final int MINIM = 32;
	static final int SEMIBREVE = 64;

	// Standard MIDI file header, for one-track file
	// 4D, 54... are just magic numbers to identify the
	//  headers
	// Note that because we're only writing one track, we
	//  can for simplicity combine the file and track headers
	static final int header[] = new int[]
			{
					0x4d, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
					0x00, 0x00, // single-track format
					0x00, 0x01, // one track
					0x00, 0x10, // 16 ticks per quarter
					0x4d, 0x54, 0x72, 0x6B
			};

	// Standard footer
	static final int footer[] = new int[] {
			0x01,
			0xFF,
			0x2F,
			0x00
	};

	// The collection of events to play, in time order
	protected Vector<int[]> playEvents;

	/** Construct a new MidiFile with an empty playback event list */
	public MIDIWriter() {
		playEvents = new Vector<>();
	}


	/** Write the stored MIDI events to a file */
	public void writeToFile (File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);

		fos.write (intArrayToByteArray (header));
		playEvents.add(footer); //set footer to the end of everything

		// Calculate the amount of track data
		// _Do_ include the footer but _do not_ include the
		// track header

		int size = 0;
		for (int i = 0; i < playEvents.size(); i++)
		size += playEvents.elementAt(i).length;

		// Write out the track data size in big-endian format
		// Note that this math is only valid for up to 64k of data
		//  (but that's a lot of notes)
		int high = size / 256;
		int low = size - (high * 256);
		fos.write ((byte) 0);
		fos.write ((byte) 0);
		fos.write ((byte) high);
		fos.write ((byte) low);

		// Write out the note, etc., events
		for (int i = 0; i < playEvents.size(); i++) {
			fos.write (intArrayToByteArray (playEvents.elementAt(i)));
		}

		fos.close();
	}


	/** Convert an array of integers which are assumed to contain
	 unsigned bytes into an array of bytes */
	protected static byte[] intArrayToByteArray (int[] ints) {
		int l = ints.length;
		byte[] out = new byte[ints.length];
		for (int i = 0; i < l; i++)
		{
			out[i] = (byte) ints[i];
		}
		return out;
	}


	/** Store a note-on event */
	public void noteOn (int channel, int delta, int note, int velocity)
	{
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x90 + channel;
		data[2] = note;
		data[3] = velocity;
		playEvents.add (data);
	}

	/** Store a note-off event */
	public void noteOff (int channel, int delta, int note) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x80 + channel;
		data[2] = note;
		data[3] = 0;
		playEvents.add (data);
	}

	public void tempoChange(int tempoBPM) {
		double tempoRatio = 300 / tempoBPM;
		int tempo_usec = (int)(200000*tempoRatio);

		byte[] intBytes = ByteBuffer
				.allocate(4)
				.order(ByteOrder.BIG_ENDIAN)
				.putInt(tempo_usec).array();

		int[] data = new int[7];
		data[0] = 0x00;
		data[1] = 0xFF;
		data[2] = 0x51;
		data[3] = 0x03;
		data[4] = intBytes[1];
		data[5] = intBytes[2];
		data[6] = intBytes[3];
		playEvents.add(data);
	}

	public void keySigChange(int key, int major_minor) {
		int[] data = new int[6];
		data[0] = 0x00;
		data[1] = 0xFF;
		data[2] = 0x59;
		data[3] = 0x02;
		data[4] = (byte)key;
		data[5] = (byte)major_minor;

		playEvents.add(data);
	}

	public void timeSigChange(int a, int b) {
		int num = a;
		int den = (int)(Math.log(b) / Math.log(2)); //note for one beat is in powers of two

		int[] data = new int[8];
		data[0] = 0x00;
		data[1] = 0xFF;
		data[2] = 0x58;
		data[3] = 0x04;
		data[4] = num; // numerator
		data[5] = den; // denominator
		data[6] = 0x30; // ticks per click (not used)
		data[7] = 0x08;  // 32nd notes per crotchet

		playEvents.add(data);
	}

	/** Store a program-change event at current position */
	public void progChange (int prog) {
		int[] data = new int[3];
		data[0] = 0;
		data[1] = 0xC0;
		data[2] = prog;
		playEvents.add(data);
	}

	/** Store a note-on event followed by a note-off event a note length
	 later. There is no delta value — the note is assumed to
	 follow the previous one with no gap. */
	public void noteOnOffNow (int channel, int duration, int note, int velocity) {
		noteOn(channel, 0, note, velocity);
		noteOff(channel, duration, note);
	}

	public void noteSequenceFixedVelocity (int channel, int[] sequence, int velocity) {
		boolean lastWasRest = false;
		int restDelta = 0;
		for (int i = 0; i < sequence.length; i += 2) {
			int note = sequence[i];
			int duration = sequence[i + 1];
			if (note < 0) {
				// This is a rest
				restDelta += duration;
				lastWasRest = true;
			}
			else {
				// A note, not a rest
				if (lastWasRest) {
					noteOn(channel, restDelta, note, velocity);
					noteOff(channel, duration, note);
				}
				else {
					noteOn(channel, 0, note, velocity);
					noteOff(channel, duration, note);
				}
				restDelta = 0;
				lastWasRest = false;
			}
		}
	}
}

