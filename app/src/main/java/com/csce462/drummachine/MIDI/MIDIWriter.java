package com.csce462.drummachine.MIDI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;


public class MIDIWriter {
	// The collection of events to play, in time order
	protected ArrayList<int[]> playEvents;

	public MIDIWriter() {
		playEvents = new ArrayList<>();
	}

	public void addEvent(int[] event) {
		playEvents.add(event);
	}

	public void writeToFile (File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);

		//Calculate the amount of track data. Include the footer but do not
		//include the track header

		int size = Events.footer.length;
		for (int i = 0; i < playEvents.size(); i++)
		size += playEvents.get(i).length;

		byte[] intBytes = ByteBuffer
				.allocate(4)
				.order(ByteOrder.BIG_ENDIAN)
				.putInt(size).array();

		int[] trackSize = new int[4];
		trackSize[0] = intBytes[0];
		trackSize[1] = intBytes[1];
		trackSize[2] = intBytes[2];
		trackSize[3] = intBytes[3];

		playEvents.add(0, Events.header);
		playEvents.add(1, trackSize);
		playEvents.add(Events.footer); //set footer to the end of everything

		// Write out the note, etc., events
		for (int i = 0; i < playEvents.size(); i++) {
			fos.write (intArrayToByteArray (playEvents.get(i)));
		}

		fos.close();
	}

	/** Convert an array of integers which are assumed to contain
	 unsigned bytes into an array of bytes */
	private static byte[] intArrayToByteArray (int[] ints) {
		byte[] out = new byte[ints.length];
		for (int i = 0; i < ints.length; i++) {
			out[i] = (byte) ints[i];
		}
		return out;
	}
}

