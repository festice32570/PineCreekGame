package com.pinecreek.game;

/**
 * Deterministic, lightweight arcade vehicle model.
 *
 * Coordinate convention:
 * heading == 0 points toward negative Z.
 * positive X is screen/world right.
 */
public final class VehiclePhysics {
    private VehiclePhysics() {}

    public static final float WHEELBASE = 3.15f;
    public static final float ROAD_TOP_SPEED = 23.5f;
    public static final float SNOW_TOP_SPEED = 12.8f;
    public static final float ROAD_REVERSE_SPEED = 8.2f;
    public static final float SNOW_REVERSE_SPEED = 5.4f;
    public static final float TOW_TOP_SPEED = 15.2f;
    public static final float TOW_REVERSE_SPEED = 4.8f;

    public static final class State {
        public float x;
        public float z;
        public float heading;
        public float speed;
        public float steer;

        public State() {}

        public State(float x, float z, float heading, float speed, float steer) {
            this.x=x;
            this.z=z;
            this.heading=heading;
            this.speed=speed;
            this.steer=steer;
        }

        public State copy() {
            return new State(x,z,heading,speed,steer);
        }
    }

    public static final class Input {
        public boolean left;
        public boolean right;
        public boolean throttle;
        public boolean brake;
        public boolean reverse;
    }

    public static void step(State s, Input in, boolean road, boolean towing, float dt) {
        if (dt <= 0f) return;

        // Keep integration deterministic across 30/60/120 Hz callers and
        // occasional long frames. Physics owns the fixed sub-step so every
        // caller gets the same behavior.
        float remaining = Math.min(dt, 0.10f);
        while (remaining > 0f) {
            float h = Math.min(remaining, 1f / 120f);
            stepSubstep(s, in, road, towing, h);
            remaining -= h;
        }
    }

    private static void stepSubstep(
            State s, Input in, boolean road, boolean towing, float dt) {
        final float absSpeed = Math.abs(s.speed);
        final float forwardLimit = towing
                ? TOW_TOP_SPEED
                : (road ? ROAD_TOP_SPEED : SNOW_TOP_SPEED);
        final float reverseLimit = towing
                ? TOW_REVERSE_SPEED
                : (road ? ROAD_REVERSE_SPEED : SNOW_REVERSE_SPEED);

        float steerInput = (in.left ? -1f : 0f) + (in.right ? 1f : 0f);

        float speedRatio = clamp(absSpeed / Math.max(1f, forwardLimit), 0f, 1f);
        // Arcade-racer steering curve. Digital touch buttons need far less
        // lock at road speed than a real steering wheel input would imply.
        float maxSteerDeg;
        if (absSpeed <= 4f) {
            maxSteerDeg = 30f;
        } else if (absSpeed <= 10f) {
            maxSteerDeg = lerp(30f, 12f, (absSpeed - 4f) / 6f);
        } else {
            maxSteerDeg = lerp(12f, 3.5f, clamp((absSpeed - 10f) / 10f, 0f, 1f));
        }
        if (!road) maxSteerDeg *= 0.80f;
        if (towing) maxSteerDeg *= 0.76f;

        float targetSteer = (float)Math.toRadians(maxSteerDeg) * steerInput;

        // Slower rack response at speed stops a one-second button hold from
        // becoming an emergency full-lock turn.
        float steerRateDeg = lerp(112f, 30f, speedRatio);
        float returnRateDeg = lerp(150f, 86f, speedRatio);
        float rate = (float)Math.toRadians(steerInput == 0f ? returnRateDeg : steerRateDeg);
        s.steer = moveToward(s.steer, targetSteer, rate * dt);

        if (in.throttle && !in.reverse) {
            if (s.speed < -0.15f) {
                s.speed = moveToward(s.speed, 0f, (road ? 17f : 11f) * dt);
            } else {
                float baseAccel = road ? 9.2f : 5.0f;
                if (towing) baseAccel *= 0.68f;
                float ratio = clamp(Math.max(0f, s.speed) / Math.max(1f, forwardLimit), 0f, 1f);
                float taper = 1f - 0.76f * (float)Math.pow(ratio, 1.35);
                s.speed += baseAccel * Math.max(0.20f, taper) * dt;
            }
        } else if (in.reverse && !in.throttle) {
            if (s.speed > 0.15f) {
                s.speed = moveToward(s.speed, 0f, (road ? 17f : 11f) * dt);
            } else {
                float reverseAccel = road ? 6.0f : 3.3f;
                if (towing) reverseAccel *= 0.66f;
                float ratio = clamp(Math.abs(Math.min(0f, s.speed)) / Math.max(1f, reverseLimit), 0f, 1f);
                float taper = 1f - 0.68f * ratio;
                s.speed -= reverseAccel * Math.max(0.28f, taper) * dt;
            }
        }

        if (in.brake) {
            s.speed = moveToward(s.speed, 0f, (road ? 18.5f : 12.0f) * dt);
        } else if (!in.throttle && !in.reverse) {
            float coast = road ? 1.15f : 2.35f;
            if (towing) coast += 0.5f;
            s.speed = moveToward(s.speed, 0f, coast * dt);
        }

        s.speed = clamp(s.speed, -reverseLimit, forwardLimit);

        if (Math.abs(s.speed) > 0.08f) {
            float surfaceGrip = road ? 1.0f : 0.62f;
            // Strong speed-sensitive yaw damping. Around 50 km/h this is about
            // 0.30, giving a controlled lane-change arc instead of a snap turn.
            float highSpeedStability = 1f / (1f + 6f * speedRatio * speedRatio);
            float towStability = towing ? 0.78f : 1.0f;

            float yawRate = (s.speed / WHEELBASE)
                    * (float)Math.tan(s.steer)
                    * surfaceGrip
                    * highSpeedStability
                    * towStability;

            float maxYaw = (float)Math.toRadians(56f);
            yawRate = clamp(yawRate, -maxYaw, maxYaw);

            float midHeading = s.heading + yawRate * dt * 0.5f;
            s.heading = normalizeAngle(s.heading + yawRate * dt);
            s.x += (float)Math.sin(midHeading) * s.speed * dt;
            s.z -= (float)Math.cos(midHeading) * s.speed * dt;
        }
    }

    public static float suggestedEnginePitch(float speed) {
        return 0.68f + Math.min(1.0f, Math.abs(speed) / 22f);
    }

    public static float suggestedEngineVolume(float speed) {
        return 0.10f + Math.min(0.19f, Math.abs(speed) / 95f);
    }

    static float normalizeAngle(float a) {
        final float twoPi = (float)(Math.PI * 2.0);
        while (a > Math.PI) a -= twoPi;
        while (a < -Math.PI) a += twoPi;
        return a;
    }

    static float moveToward(float value, float target, float amount) {
        if (value < target) return Math.min(target, value + amount);
        if (value > target) return Math.max(target, value - amount);
        return value;
    }

    static float smoothStep(float x) {
        x = clamp(x,0f,1f);
        return x*x*(3f-2f*x);
    }

    static float lerp(float a, float b, float t) {
        return a + (b-a)*t;
    }

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}

[executed on device: festice-virtual-machine (07fc5208-706b-4ca8-850a-ef91db884468)]