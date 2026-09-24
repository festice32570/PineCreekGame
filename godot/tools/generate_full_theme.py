from pathlib import Path
import ctypes
import math
import struct
import wave
from array import array

RATE = 48000
BPM = 150.0
BEAT = 60.0 / BPM
BAR = BEAT * 4.0
BARS = 112
DURATION = BARS * BAR + 0.8
COUNT = int(DURATION * RATE)
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build" / "soundtrack"
OUT.mkdir(parents=True, exist_ok=True)
WAV_PATH = OUT / "PineCreek_MainTheme_IfItRunsItsACar.wav"

L = array("f", [0.0]) * COUNT
R = array("f", [0.0]) * COUNT

def clamp(v, lo=-1.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v

def add_lr(i, left, right):
    if 0 <= i < COUNT:
        L[i] += left
        R[i] += right

def pan_gains(pan):
    return math.sqrt((1.0 - pan) * 0.5), math.sqrt((1.0 + pan) * 0.5)

def add_guitar(start, length, freq, amp=0.12, pan=-0.55, muted=False):
    i0 = max(0, int(start * RATE))
    i1 = min(COUNT, int((start + length) * RATE))
    lg, rg = pan_gains(pan)
    decay = 10.0 if muted else 2.7
    for i in range(i0, i1):
        t = (i - i0) / RATE
        attack = min(1.0, t / 0.004)
        release = min(1.0, max(0.0, length - t) / 0.055)
        env = attack * release * math.exp(-t * decay)
        p = 2.0 * math.pi * freq * t
        x = (
            math.sin(p)
            + 0.58 * math.sin(2*p + 0.13)
            + 0.35 * math.sin(3*p + 0.51)
            + 0.18 * math.sin(5*p + 0.92)
        )
        x = math.tanh(x * 3.25) * amp * env
        add_lr(i, x * lg, x * rg)

def power_chord(start, length, root, amp=0.13, muted=False):
    add_guitar(start, length, root, amp, -0.62, muted)
    add_guitar(start, length, root * 1.5, amp * 0.70, -0.62, muted)
    add_guitar(start + 0.003, length, root * 1.006, amp, 0.62, muted)
    add_guitar(start + 0.003, length, root * 1.509, amp * 0.70, 0.62, muted)

def add_bass(start, length, freq, amp=0.18):
    i0 = max(0, int(start * RATE))
    i1 = min(COUNT, int((start + length) * RATE))
    for i in range(i0, i1):
        t = (i - i0) / RATE
        env = min(1.0, t / 0.005) * min(1.0, max(0.0, length - t) / 0.07) * math.exp(-t * 1.55)
        p = 2 * math.pi * freq * t
        x = math.tanh((math.sin(p) + 0.30*math.sin(2*p+0.2)) * 1.8) * amp * env
        add_lr(i, x * 0.78, x * 0.78)

def add_kick(start, amp=0.34):
    length = 0.17
    i0 = int(start * RATE)
    i1 = min(COUNT, int((start + length) * RATE))
    phase = 0.0
    for i in range(i0, i1):
        t = (i - i0) / RATE
        f = 72.0 - 38.0 * min(1.0, t / length)
        phase += 2 * math.pi * f / RATE
        x = math.sin(phase) * math.exp(-t * 23.0) * amp
        add_lr(i, x, x)

def add_snare(start, amp=0.26):
    length = 0.19
    i0 = int(start * RATE)
    i1 = min(COUNT, int((start + length) * RATE))
    seed = int(start * 10000) ^ 0x32570
    lp = 0.0
    for i in range(i0, i1):
        # deterministic lightweight noise
        seed = (1664525 * seed + 1013904223) & 0xffffffff
        raw = ((seed >> 8) / 0xffffff) * 2.0 - 1.0
        lp = lp * 0.64 + raw * 0.36
        t = (i - i0) / RATE
        x = (lp * 0.24 + math.sin(2*math.pi*185*t)*0.10) * math.exp(-t*17.0) * (amp / 0.26)
        add_lr(i, x * 0.93, x)

def add_hat(start, amp=0.028, open_hat=False):
    length = 0.16 if open_hat else 0.045
    i0 = int(start * RATE)
    i1 = min(COUNT, int((start + length) * RATE))
    seed = int(start * 100000) ^ 0x71C
    prev = 0.0
    decay = 24.0 if open_hat else 82.0
    for i in range(i0, i1):
        seed = (1103515245 * seed + 12345) & 0x7fffffff
        raw = (seed / 0x7fffffff) * 2.0 - 1.0
        hp = raw - prev * 0.72
        prev = raw
        t = (i - i0) / RATE
        x = hp * math.exp(-t * decay) * amp
        add_lr(i, x * 0.72, x * 0.94)

def add_crash(start, amp=0.055):
    length = 0.65
    i0 = int(start * RATE)
    i1 = min(COUNT, int((start + length) * RATE))
    seed = int(start * 7777) ^ 0xDEADBEEF
    prev = 0.0
    for i in range(i0, i1):
        seed = (1664525 * seed + 1013904223) & 0xffffffff
        raw = ((seed >> 8) / 0xffffff) * 2.0 - 1.0
        hp = raw - prev * 0.82
        prev = raw
        t = (i - i0) / RATE
        x = hp * math.exp(-t * 5.2) * amp
        add_lr(i, x * 0.88, x)

def add_lead(start, length, freq, amp=0.052, pan=0.15):
    i0 = int(start * RATE)
    i1 = min(COUNT, int((start + length) * RATE))
    lg, rg = pan_gains(pan)
    for i in range(i0, i1):
        t = (i - i0) / RATE
        env = min(1.0,t/0.012) * min(1.0,max(0.0,length-t)/0.10) * math.exp(-t*1.0)
        vib = 1.0 + 0.006 * math.sin(2*math.pi*5.2*t)
        p = 2*math.pi*freq*vib*t
        x = math.tanh((math.sin(p)+0.25*math.sin(2*p+0.2))*2.1) * amp * env
        add_lr(i, x*lg, x*rg)

# Notes
E2, G2, A2, C3, D3 = 82.41, 98.00, 110.00, 130.81, 146.83
roots = [E2, G2, A2, C3, D3]

def section_of(bar):
    if bar < 8: return "intro"
    if bar < 24: return "verse1"
    if bar < 40: return "chorus1"
    if bar < 56: return "verse2"
    if bar < 72: return "chorus2"
    if bar < 88: return "breakdown"
    if bar < 104: return "final"
    return "outro"

verse_seq = [E2,E2,G2,E2, A2,E2,C3,D3]
chorus_seq = [E2,G2,A2,C3, E2,G2,D3,C3]
break_seq = [E2,E2,E2,G2, E2,E2,C3,D3]

for bar in range(BARS):
    sec = section_of(bar)
    base = bar * BAR
    seq = verse_seq if "verse" in sec or sec == "intro" else chorus_seq if "chorus" in sec or sec == "final" else break_seq

    # Guitar riff
    for n in range(8):
        root = seq[n]
        muted = sec in ("intro","verse1","verse2") and n not in (3,7)
        if sec == "breakdown":
            muted = n not in (2,5,7)
        length = BEAT*0.43 if muted else BEAT*0.82
        amp = 0.095 if muted else (0.145 if sec in ("chorus1","chorus2","final") else 0.125)
        power_chord(base + n*(BEAT/2), length, root, amp, muted)

    # Bass quarter notes
    qroots = [seq[0], seq[2], seq[4], seq[6]]
    for q, root in enumerate(qroots):
        add_bass(base + q*BEAT, BEAT*0.90, root/2.0, 0.19 if sec != "breakdown" else 0.22)

    # Drums
    if bar not in (0,1):
        if bar % 4 == 0 or sec in ("chorus1","chorus2","final","breakdown"):
            add_crash(base, 0.045 if sec != "final" else 0.060)

        # kick patterns
        if sec in ("chorus1","chorus2","final"):
            for k in (0, 0.5, 1.0, 1.5, 2.0, 2.25, 2.5, 3.0, 3.5):
                add_kick(base + k*BEAT, 0.31)
        elif sec == "breakdown":
            for k in (0, 0.25, 0.75, 1.5, 2.0, 2.25, 3.0, 3.25, 3.5):
                add_kick(base + k*BEAT, 0.34)
        else:
            for k in (0, 1.5, 2.0, 3.25):
                add_kick(base + k*BEAT, 0.31)

        add_snare(base + BEAT, 0.25)
        add_snare(base + BEAT*3, 0.27)
        for h in range(8):
            add_hat(base + h*(BEAT/2), 0.022 if sec not in ("chorus1","chorus2","final") else 0.027, open_hat=(h==7 and sec!="breakdown"))

    # Chorus lead hook
    if sec in ("chorus1","chorus2","final") and bar % 2 == 0:
        lead_notes = [329.63,392.00,440.00,493.88,440.00,392.00,329.63,293.66]
        for n, f in enumerate(lead_notes):
            add_lead(base + n*(BEAT/2), BEAT*0.42, f, 0.045 if sec!="final" else 0.055, 0.18)

# Brief silence/space in breakdown for call-and-response.
for bar in (76, 80, 84):
    start = int((bar*BAR + BEAT*2.8)*RATE)
    end = min(COUNT, int((bar*BAR + BAR)*RATE))
    for i in range(start,end):
        L[i] *= 0.38
        R[i] *= 0.38

# --- Growled vocals via local libespeak-ng, then DSP into a death-metal shout ---
lib = ctypes.CDLL("libespeak-ng.so.1")
CB = ctypes.CFUNCTYPE(ctypes.c_int, ctypes.POINTER(ctypes.c_short), ctypes.c_int, ctypes.c_void_p)
voice_chunks = []

@CB
def _cb(wav_ptr, numsamples, events):
    if numsamples > 0 and wav_ptr:
        voice_chunks.append(ctypes.string_at(wav_ptr, numsamples * 2))
    return 0

lib.espeak_Initialize.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_char_p, ctypes.c_int]
lib.espeak_Initialize.restype = ctypes.c_int
ES_RATE = lib.espeak_Initialize(1, 0, None, 0)
lib.espeak_SetSynthCallback.argtypes = [CB]
lib.espeak_SetSynthCallback(_cb)
lib.espeak_SetVoiceByName.argtypes = [ctypes.c_char_p]
lib.espeak_SetVoiceByName(b"en-us")
lib.espeak_SetParameter.argtypes = [ctypes.c_int, ctypes.c_int, ctypes.c_int]
lib.espeak_Synth.argtypes = [ctypes.c_void_p, ctypes.c_size_t, ctypes.c_uint, ctypes.c_int, ctypes.c_uint, ctypes.c_uint, ctypes.POINTER(ctypes.c_uint), ctypes.c_void_p]

def synth_phrase(text):
    voice_chunks.clear()
    # espeakRATE=1, VOLUME=2, PITCH=3, RANGE=4
    lib.espeak_SetParameter(1, 142, 0)
    lib.espeak_SetParameter(2, 100, 0)
    lib.espeak_SetParameter(3, 18, 0)
    lib.espeak_SetParameter(4, 14, 0)
    data = text.encode("utf-8")
    uid = ctypes.c_uint(0)
    lib.espeak_Synth(data, len(data)+1, 0, 1, 0, 0, ctypes.byref(uid), None)
    lib.espeak_Synchronize()
    raw = b"".join(voice_chunks)
    src = array("h")
    src.frombytes(raw)
    if not src:
        return array("f")
    # Linear resample to 48 kHz, with a slight slowdown for extra weight.
    target_rate = RATE
    speed = 0.94
    out_len = int(len(src) * target_rate / ES_RATE / speed)
    out = array("f", [0.0]) * out_len
    ratio = ES_RATE * speed / target_rate
    lp = 0.0
    env = 0.0
    delay = int(0.012 * target_rate)
    for j in range(out_len):
        pos = j * ratio
        k = int(pos)
        if k >= len(src)-1:
            break
        frac = pos-k
        x = (src[k]*(1-frac)+src[k+1]*frac)/32768.0
        lp += 0.24 * (x - lp)
        env += 0.018 * (abs(lp) - env)
        y = math.tanh(lp * 5.2) * 0.72
        # chest/sub growl follows speech envelope; deterministic, not broadband noise
        t = j/target_rate
        sub = (math.sin(2*math.pi*62*t) + 0.42*math.sin(2*math.pi*93*t+0.4)) * env * 0.42
        y += sub
        if j >= delay:
            y += out[j-delay] * 0.18
        out[j] = clamp(y, -0.96, 0.96)
    return out

def add_vocal(start, text, amp=0.30, pan=0.0):
    vox = synth_phrase(text)
    i0 = int(start * RATE)
    lg, rg = pan_gains(pan)
    # two tiny echo taps for a large, ridiculous metal-vocal space
    d1 = int(0.085*RATE)
    d2 = int(0.165*RATE)
    for j, x in enumerate(vox):
        i = i0+j
        if i >= COUNT:
            break
        v = x*amp
        add_lr(i, v*lg, v*rg)
        if i+d1 < COUNT:
            add_lr(i+d1, v*0.19*lg, v*0.19*rg)
        if i+d2 < COUNT:
            add_lr(i+d2, v*0.11*rg, v*0.11*lg)

vocal_cues = [
    (6, 0.0, "PINE CREEK!", 0.38),
    (10, 0.0, "SNOW TO THE DOORS!", 0.27),
    (14, 0.0, "IF IT RUNS, IT'S A CAR!", 0.34),
    (18, 0.0, "BOB SAYS GOOD TRUCK!", 0.29),
    (22, 0.0, "STILL RUNS!", 0.32),

    (24, 0.0, "PINE CREEK! DRIVE IT TILL IT BREAKS!", 0.34),
    (28, 0.0, "PINE CREEK! WINTER NEVER WAITS!", 0.34),
    (32, 0.0, "IF IT RUNS, IT'S A CAR!", 0.36),
    (36, 0.0, "WELCOME TO PINE CREEK!", 0.34),

    (42, 0.0, "KEVIN'S LOOSE AGAIN!", 0.30),
    (46, 0.0, "THE MAYOR BROUGHT A PLOW!", 0.29),
    (50, 0.0, "THREE TOWN HALLS!", 0.30),
    (54, 0.0, "NOTHING MAKES SENSE!", 0.34),

    (56, 0.0, "PINE CREEK! DRIVE IT TILL IT BREAKS!", 0.34),
    (60, 0.0, "PINE CREEK! WINTER NEVER WAITS!", 0.34),
    (64, 0.0, "IF IT RUNS, IT'S A CAR!", 0.36),
    (68, 0.0, "WELCOME TO PINE CREEK!", 0.34),

    (74, 0.0, "CHECK ENGINE?", 0.31),
    (76, 2.0, "STILL RUNS!", 0.37),
    (78, 0.0, "THEN DRIVE!", 0.39),
    (80, 2.0, "MORE SNOW!", 0.36),
    (82, 0.0, "BIGGER TRUCK!", 0.36),
    (84, 2.0, "BAD IDEA!", 0.36),
    (86, 0.0, "DO IT ANYWAY!", 0.39),

    (88, 0.0, "PINE CREEK!", 0.40),
    (92, 0.0, "DRIVE IT TILL IT BREAKS!", 0.36),
    (96, 0.0, "IF IT RUNS, IT'S A CAR!", 0.38),
    (100,0.0, "NOTHING MAKES SENSE!", 0.38),
    (105,0.0, "WELCOME TO PINE CREEK!", 0.40),
    (109,0.0, "THAT'S NORMAL!", 0.42),
]
for bar, beat_off, text, amp in vocal_cues:
    add_vocal(bar*BAR + beat_off*BEAT, text, amp)

# Master: soft clip then normalize to -1 dBFS-ish.
peak = 0.001
for i in range(COUNT):
    L[i] = math.tanh(L[i] * 1.12)
    R[i] = math.tanh(R[i] * 1.12)
    a = abs(L[i])
    b = abs(R[i])
    if a > peak: peak = a
    if b > peak: peak = b
gain = 0.89 / peak

# Short fade in/out.
fade = int(0.045*RATE)
outfade = int(1.4*RATE)
for i in range(fade):
    a = i/max(1,fade-1)
    L[i] *= a
    R[i] *= a
for i in range(outfade):
    a = 1.0 - i/max(1,outfade-1)
    idx = COUNT-outfade+i
    L[idx] *= a
    R[idx] *= a

with wave.open(str(WAV_PATH), "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(RATE)
    block = 8192
    for start in range(0, COUNT, block):
        end = min(COUNT, start+block)
        pcm = array("h")
        pcm.extend(int(clamp(L[i]*gain)*32767) for i in range(start,end))
        # interleave by rebuilding compactly
        inter = array("h")
        for i in range(start,end):
            inter.append(int(clamp(L[i]*gain)*32767))
            inter.append(int(clamp(R[i]*gain)*32767))
        w.writeframes(inter.tobytes())

print(WAV_PATH)
print("duration_sec", DURATION)
print("sample_rate", RATE)
print("peak_before_norm", peak)
print("size_bytes", WAV_PATH.stat().st_size)

