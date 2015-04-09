package com.csce462.drummachine.MIDI;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Events {
	// Standard MIDI file header, for one-track file
	// 4D, 54... are just magic numbers to identify the
	//  headers
	// Note that because we're only writing one track, we
	//  can for simplicity combine the file and track headers
	public static final int header[] = new int[] {
		0x4d, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
		0x00, 0x00, // single-track format
		0x00, 0x01, // one track
		0x00, 0x10, // 240 ticks per quarter
		0x4d, 0x54, 0x72, 0x6B
	};

	// Standard footer
	public static final int footer[] = new int[] {
			0x01, 0xFF, 0x2F, 0x00
	};

	public static int[] noteOn(int channel, int delta, int note, int velocity) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x90 + channel;
		data[2] = note;
		data[3] = velocity;
		return data;
	}

	public static int[] noteOff(int channel, int delta, int note) {
		int[] data = new int[4];
		data[0] = delta;
		data[1] = 0x80 + channel;
		data[2] = note;
		data[3] = 0;
		return data;
	}

	public static int[] noteOnOff(int channel, int duration, int note, int velocity) {
		int[] data = new int[8];

		//note on
		data[0] = 0x0;
		data[1] = 0x90 + channel;
		data[2] = note;
		data[3] = velocity;

		//note off
		data[4] = duration;
		data[5] = 0x80 + channel;
		data[6] = note;
		data[7] = 0;

		return data;
	}

	/** Store a program-change event at current position */
	public static int[] programChange (int program) {
		int[] data = new int[3];
		data[0] = 0;
		data[1] = 0xC0;
		data[2] = program;
		return data;
	}

	public static int[] tempoChange(int tempoBPM) {
		double tempoRatio = 300 / tempoBPM;
		int tempo_usec = (60*1000*1000)/tempoBPM;

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
		return data;
	}

	public static int[] keySignatureChange(int key, int major_minor) {
		int[] data = new int[6];
		data[0] = 0x00;
		data[1] = 0xFF;
		data[2] = 0x59;
		data[3] = 0x02;
		data[4] = (byte)key;
		data[5] = (byte)major_minor;

		return data;
	}

	public static int[] timeSignatureChange(int a, int b) {
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

		return data;
	}
}
