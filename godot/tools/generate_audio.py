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
    hat = (rng.random()*2-1) * math.exp(-eighth*45) * 0.016
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
# Heavy old pickup idle loop.
duration = 3.0
samples = []
low_noise = 0.0
for i in range(int(duration*RATE)):
    t = i/RATE
    wobble = 1.0 + 0.025*math.sin(2*math.pi*1.7*t) + 0.012*math.sin(2*math.pi*0.43*t)
    f = 27.5*wobble
    p = 2*math.pi*f*t
    combustion = math.sin(p) + 0.46*math.sin(2*p+0.35) + 0.21*math.sin(3*p+1.1)
    low_noise = low_noise*0.965 + (rng.random()*2-1)*0.035
    samples.append(combustion*0.20 + low_noise*0.19)
write_mono("engine_idle.wav", samples)

# Packed-snow rolling loop.
duration = 4.0
samples = []
lp = 0.0
for i in range(int(duration*RATE)):
    t=i/RATE
    raw=rng.random()*2-1
    lp=lp*0.82+raw*0.18
    granular = raw-lp
    thump = max(0.0, math.sin(2*math.pi*2.3*t))**10
    samples.append(granular*0.10 + lp*0.05 + thump*0.035)
fade=int(0.18*RATE)
for i in range(fade):
    a=i/fade
    samples[i] *= a
    samples[-1-i] *= a
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
