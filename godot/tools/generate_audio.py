from pathlib import Path
import math, random, struct, wave

RATE = 48000
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets" / "audio"
OUT.mkdir(parents=True, exist_ok=True)
rng = random.Random(32570)

def clamp(v):
    return max(-1.0, min(1.0, v))

def write_mono(name, samples):
    peak = max(0.001, max(abs(x) for x in samples))
    gain = 0.94 / peak
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        data = bytearray()
        for s in samples:
            data += struct.pack("<h", int(clamp(s * gain) * 32767))
        w.writeframes(data)

def write_stereo(name, left, right):
    peak = max(0.001, max(max(abs(x) for x in left), max(abs(x) for x in right)))
    gain = 0.91 / peak
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(RATE)
        data = bytearray()
        for l, r in zip(left, right):
            data += struct.pack("<hh", int(clamp(l*gain)*32767), int(clamp(r*gain)*32767))
        w.writeframes(data)

def env(t, attack, release, length):
    a = min(1.0, t / max(0.001, attack))
    r = min(1.0, max(0.0, length - t) / max(0.001, release))
    return a * r

# Title loop is derived from the 3-minute full theme by make_title_loop.py.
# Original looping road-radio instrumental: 8 bars, 90 BPM.
beat = 60.0 / 90.0
bars = 8
duration = bars * 4 * beat
count = int(duration * RATE)
L = [0.0] * count
R = [0.0] * count
chords = [
    (98.00, [196.00, 246.94, 293.66]),
    (82.41, [164.81, 196.00, 246.94]),
    (65.41, [130.81, 164.81, 196.00]),
    (73.42, [146.83, 185.00, 220.00]),
]
for i in range(count):
    t = i / RATE
    bar = int(t / (beat*4)) % bars
    root, tones = chords[bar % 4]
    phase_in_beat = (t % beat) / beat
    bass_env = math.exp(-phase_in_beat * 4.8)
    bass = math.sin(2*math.pi*root*t) * 0.13 * bass_env
    pad = sum(math.sin(2*math.pi*f*t + j*0.7) for j, f in enumerate(tones)) * 0.018
    trem = 0.75 + 0.25*math.sin(2*math.pi*0.18*t)
    kick_phase = t % beat
    kick = math.sin(2*math.pi*(54 + 18*math.exp(-kick_phase*18))*kick_phase)
    kick *= math.exp(-kick_phase*13) * 0.16
    half = t % (beat*2)
    snare = 0.0
    if half > beat and half < beat + 0.16:
        q = half - beat
        snare = (rng.random()*2-1) * math.exp(-q*18) * 0.055
    eighth = t % (beat/2)
    hat = 0.0
    if eighth < 0.032:
        hat = (rng.random()*2-1) * math.exp(-eighth*95) * 0.011
    dry = bass + pad*trem + kick + snare + hat
    L[i] += dry * 0.88
    R[i] += dry * 0.88

# Sparse twang melody, intentionally original and simple.
scale = [196.00, 220.00, 246.94, 293.66, 329.63, 392.00]
melody = [0,2,3,1,4,3,2,5,4,2,1,3,0,2,4,1]
step = beat/2
for n, note in enumerate(melody):
    start = (8*beat) + n*step
    length = step*0.78
    freq = scale[note]
    i0, i1 = int(start*RATE), min(count, int((start+length)*RATE))
    for i in range(i0, i1):
        tt = (i-i0)/RATE
        e = env(tt,0.018,0.16,length) * math.exp(-tt*1.2)
        pluck = (math.sin(2*math.pi*freq*tt) + 0.33*math.sin(2*math.pi*freq*2*tt)) * e * 0.07
        L[i] += pluck * 0.78
        R[i] += pluck * 0.98
write_stereo("pine_creek_radio.wav", L, R)
# Old full-size pickup V8 idle loop.
# 720 RPM -> 12 crank rev/s -> 48 combustion events/s for a four-stroke V8.
# All components are periodic divisors/multiples of 48 Hz, so the 4-second loop
# is seamless and does not need continuous broadband noise.
duration = 4.0
samples = []
fire_hz = 48.0
for i in range(int(duration*RATE)):
    t = i / RATE
    p = 2*math.pi*fire_hz*t
    burble = 0.90 + 0.055*math.sin(2*math.pi*6.0*t+0.4) + 0.030*math.sin(2*math.pi*12.0*t+1.1)
    combustion = (
        0.78*math.sin(p)
        + 0.38*math.sin(2*p+0.28)
        + 0.23*math.sin(3*p+0.70)
        + 0.14*math.sin(4*p+1.10)
        + 0.08*math.sin(6*p+0.20)
    )
    crank = 0.31*math.sin(2*math.pi*24.0*t+0.35) + 0.13*math.sin(2*math.pi*12.0*t)
    exhaust = math.tanh((combustion*burble + crank) * 1.45)
    # A low mechanical component gives weight without a broadband hiss floor.
    mechanical = 0.035*math.sin(2*math.pi*96.0*t+0.2) + 0.018*math.sin(2*math.pi*144.0*t+0.6)
    samples.append(exhaust*0.30 + mechanical)
write_mono("engine_idle.wav", samples)

# Packed-snow tyre loop: low rolling body plus discrete compressed-snow crunches.
# No continuous white-noise bed; all high-frequency content exists in short grains.
duration = 6.0
count = int(duration * RATE)
samples = [0.0] * count

# Seamless low tyre/body rumble. All frequencies complete an integer number of
# cycles in the six-second loop.
for i in range(count):
    t = i / RATE
    weight = (
        0.036 * math.sin(2*math.pi*42.0*t)
        + 0.025 * math.sin(2*math.pi*67.0*t + 0.4)
        + 0.014 * math.sin(2*math.pi*93.0*t + 1.1)
    )
    texture = 0.80 + 0.12*math.sin(2*math.pi*0.5*t) + 0.08*math.sin(2*math.pi*(5.0/6.0)*t + 0.7)
    lug = max(0.0, math.sin(2*math.pi*2.0*t + 0.2))**10 * 0.012
    samples[i] = weight * texture + lug

# Crunch events approximate compacted snow breaking under tread blocks. Instead
# of broadband hiss they use short clusters of inharmonic resonances. Circular
# indexing keeps grains near the boundary seamless.
centers = [0.18,0.64,1.11,1.73,2.29,2.94,3.37,4.03,4.58,5.17,5.72]
for gi, center in enumerate(centers):
    width = 0.040 + (gi % 4) * 0.008
    amp = 0.040 + (gi % 3) * 0.008
    freqs = [310 + gi*17, 540 + gi*23, 870 + gi*29, 1370 + gi*31]
    phases = [0.2*gi, 0.7+0.11*gi, 1.5+0.07*gi, 2.2+0.13*gi]
    radius = int(width * RATE)
    center_i = int(center * RATE)
    for o in range(-radius, radius + 1):
        x = o / max(1, radius)
        window = (1.0 - x*x) ** 2
        idx = (center_i + o) % count
        tt = o / RATE
        grain = 0.0
        for f, ph in zip(freqs, phases):
            grain += math.sin(2*math.pi*f*tt + ph)
        samples[idx] += grain * (amp / len(freqs)) * window

write_mono("snow_roll.wav", samples)

# Lateral tyre scrub / snow spray.
duration=1.8
samples=[]
lp=0.0
for i in range(int(duration*RATE)):
    t=i/RATE
    raw=rng.random()*2-1
    lp=lp*0.93+raw*0.07
    band=(raw-lp)
    grit=math.sin(2*math.pi*(1280+210*math.sin(2*math.pi*1.9*t))*t)
    samples.append((band*0.14+grit*0.025)*env(t,0.05,0.25,duration))
write_mono("snow_skid.wav", samples)
# Two-tone mechanical horn.
duration=0.62
samples=[]
for i in range(int(duration*RATE)):
    t=i/RATE
    e=env(t,0.018,0.12,duration)
    a=math.sin(2*math.pi*338*t)+0.18*math.sin(2*math.pi*676*t)
    b=math.sin(2*math.pi*424*t+0.2)+0.15*math.sin(2*math.pi*848*t)
    samples.append((a*0.23+b*0.18)*e)
write_mono("truck_horn.wav", samples)

# Short dashboard/UI click.
duration=0.09
samples=[]
for i in range(int(duration*RATE)):
    t=i/RATE
    e=math.exp(-t*52)
    samples.append((math.sin(2*math.pi*920*t)*0.22 + math.sin(2*math.pi*1380*t)*0.10)*e)
write_mono("ui_click.wav", samples)

# Pause thump / radio duck cue.
duration=0.22
samples=[]
for i in range(int(duration*RATE)):
    t=i/RATE
    e=math.exp(-t*18)
    samples.append((math.sin(2*math.pi*(145-70*t)*t)*0.24)*e)
write_mono("pause_cue.wav", samples)

for p in sorted(OUT.glob("*.wav")):
    print(p.name, p.stat().st_size)
