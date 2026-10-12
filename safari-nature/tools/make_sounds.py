#!/usr/bin/env python3
"""
The app's little sounds, synthesised (no third-party audio, nothing to credit): a soft tap, the
discovery jingle, a star sparkle and the level-up fanfare. Writes app/src/main/res/raw/*.wav.

    python3 -I tools/make_sounds.py
"""
import math
import os
import struct
import wave

RATE = 22050
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "android/app/src/main/res/raw")


def note(f):
    return 440.0 * 2 ** ((f - 69) / 12)


def bell(freq, dur, start, buf, vol=0.5, decay=4.0):
    """A soft bell: a sine and two quiet partials, fast attack, exponential decay."""
    n0 = int(start * RATE)
    for i in range(int(dur * RATE)):
        t = i / RATE
        env = min(1.0, t / 0.008) * math.exp(-decay * t)
        v = math.sin(2 * math.pi * freq * t) + 0.35 * math.sin(2 * math.pi * 2 * freq * t) + 0.12 * math.sin(2 * math.pi * 3 * freq * t)
        if n0 + i < len(buf):
            buf[n0 + i] += vol * env * v / 1.47


def write(name, buf):
    peak = max(1e-9, max(abs(x) for x in buf))
    k = 0.85 / peak if peak > 0.85 else 1.0
    with wave.open(os.path.join(OUT, name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, x * k)) * 32767)) for x in buf))


def main():
    os.makedirs(OUT, exist_ok=True)

    # Tap: a short "pop", the pitch falling quickly.
    d = 0.09
    buf = [0.0] * int(d * RATE)
    phase = 0.0
    for i in range(len(buf)):
        t = i / RATE
        f = 900 - 4000 * t
        phase += 2 * math.pi * f / RATE
        buf[i] = 0.5 * math.sin(phase) * math.exp(-38 * t) * min(1, t / 0.003)
    write("tap.wav", buf)

    # Discovery: C major arpeggio up to the octave, the last note held.
    buf = [0.0] * int(1.3 * RATE)
    for k, m in enumerate([72, 76, 79, 84]):
        bell(note(m), 1.3 - k * 0.11, k * 0.11, buf, vol=0.45 if k < 3 else 0.6, decay=3.2 if k == 3 else 6)
    write("discover.wav", buf)

    # Star: two quick high pings.
    buf = [0.0] * int(0.45 * RATE)
    bell(note(91), 0.4, 0.0, buf, vol=0.35, decay=9)
    bell(note(96), 0.35, 0.07, buf, vol=0.35, decay=9)
    write("star.wav", buf)

    # Level up: a little fanfare, G C E then a held C major chord.
    buf = [0.0] * int(1.9 * RATE)
    for k, m in enumerate([67, 72, 76]):
        bell(note(m), 0.4, k * 0.13, buf, vol=0.5, decay=7)
    for m in [72, 76, 79, 84]:
        bell(note(m), 1.5, 0.42, buf, vol=0.32, decay=2.2)
    write("levelup.wav", buf)


if __name__ == "__main__":
    main()
