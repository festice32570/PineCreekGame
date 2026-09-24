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

# Pine Creek title theme: original hard-rock / heavy-metal instrumental.
# 132 BPM, 16 bars. Synthesized offline and rendered to local PCM WAV.
beat = 60.0 / 132.0
bars = 16
duration = bars * 4 * beat
count = int(duration * RATE)
L = [0.0] * count
R = [0.0] * count

def add_tone(buf_l, buf_r, start, length, freq, amp, pan=0.0, drive=1.0, decay=2.2):
    i0 = max(0, int(start * RATE))
    i1 = min(len(buf_l), int((start + length) * RATE))
    for i in range(i0, i1):
        tt = (i - i0) / RATE
        e = env(tt, 0.004, min(0.09, length * 0.35), length) * math.exp(-tt * decay)
        # Odd/even harmonics + soft clipping approximate an overdriven guitar.
        x = (
            math.sin(2*math.pi*freq*tt)
            + 0.58*math.sin(2*math.pi*freq*2*tt + 0.10)
            + 0.36*math.sin(2*math.pi*freq*3*tt + 0.35)
            + 0.19*math.sin(2*math.pi*freq*5*tt + 0.75)
        )
        x = math.tanh(x * drive) * amp * e
        lg = math.sqrt(max(0.0, (1.0 - pan) * 0.5))
        rg = math.sqrt(max(0.0, (1.0 + pan) * 0.5))
        buf_l[i] += x * lg
        buf_r[i] += x * rg

def add_power_chord(start, length, root, amp=0.13, muted=False):
    d = 7.5 if muted else 2.0
    a = amp * (0.72 if muted else 1.0)
    add_tone(L, R, start, length, root, a, -0.58, 2.9, d)
    add_tone(L, R, start, length, root*1.5, a*0.72, -0.58, 2.6, d)
    add_tone(L, R, start+0.0027, length, root*1.006, a, 0.58, 3.0, d)
    add_tone(L, R, start+0.0027, length, root*1.509, a*0.70, 0.58, 2.7, d)

# Main riff: E - G - A - C/D turnaround. Deliberately original.
E2, G2, A2, C3, D3 = 82.41, 98.00, 110.00, 130.81, 146.83
riff = [E2,E2,G2,E2, A2,E2,C3,D3, E2,E2,G2,E2, A2,C3,D3,C3]
step = beat / 2.0
for bar in range(bars):
    base = bar * beat * 4
    for n in range(8):
        root = riff[(bar*8+n) % len(riff)]
        muted = (n not in [3,7])
        add_power_chord(base+n*step, step*0.82, root, 0.105 if muted else 0.14, muted)

# Bass follows quarter-note roots with a warm clipped low-end.
for bar in range(bars):
    base = bar * beat * 4
    roots = [E2, G2 if bar % 4 == 1 else E2, A2 if bar % 4 == 2 else E2, D3/2 if bar % 4 == 3 else E2]
    for q, root in enumerate(roots):
        f = root / 2.0
        start = base + q*beat
        i0, i1 = int(start*RATE), min(count, int((start+beat*0.92)*RATE))
        for i in range(i0, i1):
            tt=(i-i0)/RATE
            e=env(tt,0.006,0.10,beat*0.92)*math.exp(-tt*1.5)
            x=math.sin(2*math.pi*f*tt)+0.34*math.sin(2*math.pi*f*2*tt)
            x=math.tanh(x*1.8)*0.18*e
            L[i]+=x*0.78; R[i]+=x*0.78

# Drums: punchy kick/snare; cymbal noise exists only around explicit hits.
for bar in range(bars):
    base=bar*beat*4
    for q in range(4):
        start=base+q*beat
        # kick
        i0,i1=int(start*RATE),min(count,int((start+0.18)*RATE))
        for i in range(i0,i1):
            tt=(i-i0)/RATE
            f=64.0-25.0*min(1.0,tt/0.16)
            x=math.sin(2*math.pi*f*tt)*math.exp(-tt*20)*0.34
            L[i]+=x; R[i]+=x
        # snare on 2 and 4
        if q in [1,3]:
            ss=start
            j0,j1=int(ss*RATE),min(count,int((ss+0.18)*RATE))
            lp=0.0
            for i in range(j0,j1):
                tt=(i-j0)/RATE
                raw=rng.random()*2-1
                lp=lp*0.62+raw*0.38
                tone=math.sin(2*math.pi*190*tt)*0.12
                x=(lp*0.18+tone)*math.exp(-tt*17)
                L[i]+=x*0.92; R[i]+=x
    # eighth-note closed hats, short transients only
    for h in range(8):
        start=base+h*step
        i0,i1=int(start*RATE),min(count,int((start+0.055)*RATE))
        hp_prev=0.0
        for i in range(i0,i1):
            tt=(i-i0)/RATE
            raw=rng.random()*2-1
            hp=raw-hp_prev*0.55
            hp_prev=raw
            x=hp*math.exp(-tt*70)*0.030
            L[i]+=x*0.72; R[i]+=x*0.90

# Simple lead hook over the second half.
lead = [329.63,392.00,440.00,392.00, 329.63,293.66,329.63,246.94]
for bar in range(8,16):
    base=bar*beat*4
    for n,f in enumerate(lead):
        if (bar+n) % 3 == 0:
            continue
        add_tone(L,R,base+n*step,step*0.72,f,0.045,0.18,1.55,2.8)

# Tiny fade at loop seam to eliminate clicks without creating a long fade-in.
seam = int(0.012*RATE)
for i in range(seam):
    a=i/max(1,seam-1)
    L[i]*=a; R[i]*=a
    L[-1-i]*=a; R[-1-i]*=a

write_stereo("title_theme.wav", L, R)

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
