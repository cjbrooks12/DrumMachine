package com.csce462.drummachine.MIDI;

public class Notes {
	public enum Key {
		C ("C", 		0),  CS("C\u266F",  -1), CF("C\u266D", 	1),
		D ("D", 		2),  DS("D\u266F", 	 1), DF("D\u266D", 	3),
		E ("E", 		4),  ES("E\u266F", 	 3), EF("E\u266D", 	5),
		F ("F", 		5),  FS("F\u266F", 	 4), FF("F\u266D", 	6),
		G ("G", 		7),  GS("G\u266F", 	 6), GF("C\u266D", 	8),
		A ("A", 		9),  AS("A\u266F", 	 8), AF("A\u266D", 	10),
		B ("B", 		11), BS("B\u266F", 	10), BF("B\u266D", 	12);

		public String name;
		public int value;

		Key(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}

	public enum Percussion {
		Bass1(35),
		Bass2(36),
		Snare(38),
		RideCymbal(59),
		HiHatClosed(42),
		HiHatOpen(46),
		CrashCymbal(49),
		ChinaCymbal(52);

		public int value;

		Percussion(int value) {
			this.value = value;
		}
	}

	public enum Scale {
		Major(0, 2, 4, 5, 7, 9, 11, 12),
		Minor(0, 2, 3, 5, 7, 8, 10, 12);

		public int[] notes;
		Scale(int... notes) {
			this.notes = notes;
		}
	}

	public static int noteValue(int note, int octave, int keyOffset) {
		return note + ((octave+1)*12) + keyOffset;
	}
}
