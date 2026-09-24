from pathlib import Path
import struct
import wave
from array import array

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "build" / "soundtrack" / "PineCreek_MainTheme_IfItRunsItsACar.wav"
OUT = ROOT / "assets" / "audio" / "title_theme.wav"

BPM = 150.0
BAR_SECONDS = 4.0 * 60.0 / BPM
START_BAR = 24
BAR_COUNT = 16

if not SOURCE.exists():
    raise SystemExit("Full theme not found. Run: python3 godot/tools/generate_full_theme.py")

with wave.open(str(SOURCE), "rb") as src:
    if (src.getnchannels(), src.getsampwidth(), src.getframerate()) != (2, 2, 48000):
        raise SystemExit("Expected 48 kHz / 16-bit / stereo PCM full theme")
    rate = src.getframerate()
    start_frame = round(START_BAR * BAR_SECONDS * rate)
    frame_count = round(BAR_COUNT * BAR_SECONDS * rate)
    src.setpos(start_frame)
    frames = src.readframes(frame_count)

# A tiny 6 ms edge fade forces the file seam to digital zero while keeping the
# exact 16-bar duration. This prevents clicks without changing musical timing.
pcm = array("h")
pcm.frombytes(frames)
fade_frames = round(0.006 * rate)
for i in range(fade_frames):
    a = i / max(1, fade_frames - 1)
    j = i * 2
    pcm[j] = int(pcm[j] * a)
    pcm[j + 1] = int(pcm[j + 1] * a)
    k = (frame_count - 1 - i) * 2
    pcm[k] = int(pcm[k] * a)
    pcm[k + 1] = int(pcm[k + 1] * a)
frames = pcm.tobytes()

with wave.open(str(OUT), "wb") as dst:
    dst.setnchannels(2)
    dst.setsampwidth(2)
    dst.setframerate(48000)
    dst.writeframes(frames)

first = struct.unpack_from("<hh", frames, 0)
last = struct.unpack_from("<hh", frames, len(frames) - 4)
print(OUT)
print("start_bar", START_BAR)
print("bars", BAR_COUNT)
print("duration_sec", frame_count / 48000.0)
print("first_frame", first)
print("last_frame", last)
