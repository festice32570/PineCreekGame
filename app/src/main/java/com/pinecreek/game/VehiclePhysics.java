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
        dt = Math.min(dt, 0.05f);

        final float absSpeed = Math.abs(s.speed);
        final float forwardLimit = towing
                ? TOW_TOP_SPEED
                : (road ? ROAD_TOP_SPEED : SNOW_TOP_SPEED);
        final float reverseLimit = towing
                ? TOW_REVERSE_SPEED
                : (road ? ROAD_REVERSE_SPEED : SNOW_REVERSE_SPEED);

        float steerInput = (in.left ? -1f : 0f) + (in.right ? 1f : 0f);

        float speedRatio = clamp(absSpeed / Math.max(1f, forwardLimit), 0f, 1f);
        float maxSteerDeg = lerp(34f, 10.5f, smoothStep(speedRatio));
        if (!road) maxSteerDeg *= 0.86f;
        if (towing) maxSteerDeg *= 0.82f;

        float targetSteer = (float)Math.toRadians(maxSteerDeg) * steerInput;

        float steerRateDeg = lerp(120f, 72f, speedRatio);
        float returnRateDeg = lerp(155f, 100f, speedRatio);
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
            float surfaceGrip = road ? 1.0f : 0.68f;
            float highSpeedStability = lerp(1.0f, 0.62f, smoothStep(speedRatio));
            float towStability = towing ? 0.82f : 1.0f;

            float yawRate = (s.speed / WHEELBASE)
                    * (float)Math.tan(s.steer)
                    * surfaceGrip
                    * highSpeedStability
                    * towStability;

            float maxYaw = (float)Math.toRadians(82f);
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
