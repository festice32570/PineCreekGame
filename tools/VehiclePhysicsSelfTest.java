package com.pinecreek.game;

public final class VehiclePhysicsSelfTest {
    private static int passed = 0;

    public static void main(String[] args) {
        testIdleDoesNotTurn();
        testForwardLeft();
        testForwardRight();
        testReverseSteering();
        testBrakeNeverFlipsDirection();
        testRoadTopSpeed();
        testSnowIsSlower();
        testTowingCapsSpeed();
        testSteeringReturnsToCenter();
        testFrameRateConsistency();
        testNoNaN();
        System.out.println("VehiclePhysicsSelfTest: " + passed + " tests passed");
    }

    private static VehiclePhysics.State simulate(
            VehiclePhysics.State s,
            VehiclePhysics.Input in,
            boolean road,
            boolean towing,
            float seconds,
            float dt) {
        int steps = Math.max(1, Math.round(seconds / dt));
        for (int i=0; i<steps; i++) {
            VehiclePhysics.step(s, in, road, towing, dt);
        }
        return s;
    }

    private static void testIdleDoesNotTurn() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,0,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.left = true;
        simulate(s,in,true,false,2f,1f/60f);
        near(s.heading,0f,0.0001f,"idle steering changed heading");
        pass();
    }

    private static void testForwardLeft() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,8f,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.left = true;
        simulate(s,in,true,false,1.2f,1f/120f);
        check(s.heading < -0.05f,"forward-left should rotate heading left");
        check(s.x < -0.05f,"forward-left should move toward negative X");
        pass();
    }

    private static void testForwardRight() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,8f,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.right = true;
        simulate(s,in,true,false,1.2f,1f/120f);
        check(s.heading > 0.05f,"forward-right should rotate heading right");
        check(s.x > 0.05f,"forward-right should move toward positive X");
        pass();
    }

    private static void testReverseSteering() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,-4f,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.left = true;
        simulate(s,in,true,false,1.2f,1f/120f);
        check(s.heading > 0.03f,"reverse-left should rotate vehicle nose right");
        check(s.x < -0.03f,"reverse-left should move vehicle rear toward left");
        pass();
    }

    private static void testBrakeNeverFlipsDirection() {
        VehiclePhysics.State fwd = new VehiclePhysics.State(0,0,0,12f,0);
        VehiclePhysics.Input brake = new VehiclePhysics.Input();
        brake.brake = true;
        simulate(fwd,brake,true,false,2f,1f/60f);
        check(fwd.speed >= -0.0001f,"braking forward should not create reverse speed");
        near(fwd.speed,0f,0.01f,"forward braking did not stop");

        VehiclePhysics.State rev = new VehiclePhysics.State(0,0,0,-5f,0);
        simulate(rev,brake,true,false,2f,1f/60f);
        check(rev.speed <= 0.0001f,"braking reverse should not create forward speed");
        near(rev.speed,0f,0.01f,"reverse braking did not stop");
        pass();
    }

    private static void testRoadTopSpeed() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,0,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.throttle = true;
        simulate(s,in,true,false,60f,1f/120f);
        check(s.speed <= VehiclePhysics.ROAD_TOP_SPEED + 0.001f,"road top speed exceeded cap");
        check(s.speed > VehiclePhysics.ROAD_TOP_SPEED * 0.80f,"road acceleration never approached top speed");
        pass();
    }

    private static void testSnowIsSlower() {
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.throttle = true;

        VehiclePhysics.State road = new VehiclePhysics.State(0,0,0,0,0);
        VehiclePhysics.State snow = new VehiclePhysics.State(0,0,0,0,0);
        simulate(road,in,true,false,12f,1f/120f);
        simulate(snow,in,false,false,12f,1f/120f);

        check(road.speed > snow.speed + 2f,"snow should produce clearly lower speed than road");
        check(snow.speed <= VehiclePhysics.SNOW_TOP_SPEED + 0.001f,"snow top speed exceeded cap");
        pass();
    }

    private static void testTowingCapsSpeed() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,0,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.throttle = true;
        simulate(s,in,true,true,60f,1f/120f);
        check(s.speed <= VehiclePhysics.TOW_TOP_SPEED + 0.001f,"towing top speed exceeded cap");
        pass();
    }

    private static void testSteeringReturnsToCenter() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,8f,0);
        VehiclePhysics.Input left = new VehiclePhysics.Input();
        left.left = true;
        simulate(s,left,true,false,.6f,1f/120f);
        check(Math.abs(s.steer) > 0.05f,"steering did not build angle");

        VehiclePhysics.Input neutral = new VehiclePhysics.Input();
        simulate(s,neutral,true,false,1.0f,1f/120f);
        near(s.steer,0f,0.01f,"steering did not self-center");
        pass();
    }

    private static void testFrameRateConsistency() {
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        in.throttle = true;
        in.right = true;

        VehiclePhysics.State a = new VehiclePhysics.State(0,0,0,0,0);
        VehiclePhysics.State b = new VehiclePhysics.State(0,0,0,0,0);
        simulate(a,in,true,false,8f,1f/60f);
        simulate(b,in,true,false,8f,1f/120f);

        near(a.speed,b.speed,0.18f,"60/120 Hz speed diverged too much");
        near(a.heading,b.heading,0.08f,"60/120 Hz heading diverged too much");
        near(a.x,b.x,0.65f,"60/120 Hz X diverged too much");
        near(a.z,b.z,0.65f,"60/120 Hz Z diverged too much");
        pass();
    }

    private static void testNoNaN() {
        VehiclePhysics.State s = new VehiclePhysics.State(0,0,0,0,0);
        VehiclePhysics.Input in = new VehiclePhysics.Input();
        for (int i=0; i<10000; i++) {
            in.throttle = (i % 150) < 100;
            in.reverse = (i % 400) > 360;
            in.brake = (i % 257) < 11;
            in.left = (i % 500) < 210;
            in.right = (i % 500) > 300;
            VehiclePhysics.step(s,in,(i%300)<210,(i%900)>820,1f/120f);
            check(Float.isFinite(s.x),"x became non-finite");
            check(Float.isFinite(s.z),"z became non-finite");
            check(Float.isFinite(s.heading),"heading became non-finite");
            check(Float.isFinite(s.speed),"speed became non-finite");
            check(Float.isFinite(s.steer),"steer became non-finite");
        }
        pass();
    }

    private static void pass() {
        passed++;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static void near(float actual, float expected, float tolerance, String message) {
        if (Math.abs(actual-expected) > tolerance) {
            throw new AssertionError(message + ": expected " + expected + " ± " + tolerance + ", actual " + actual);
        }
    }
}
