package com.pinecreek.game;

public final class ChaseCameraMathSelfTest {
    private static int passed = 0;

    public static void main(String[] args) {
        testSteeringDeadZoneKeepsScreenStable();
        testLargeTurnMovesCameraButNotOneToOne();
        testCameraRecentersAfterSteeringRelease();
        testThirtySixtyOneTwentyConsistency();
        testAngleWrap();
        System.out.println("ChaseCameraMathSelfTest: " + passed + " tests passed");
    }

    private static void testSteeringDeadZoneKeepsScreenStable() {
        float vehicle = (float)Math.toRadians(10);
        float camera = 0f;
        float steer = (float)Math.toRadians(18);
        for (int i=0;i<120;i++) {
            camera = ChaseCameraMath.updateHeading(vehicle,camera,steer,8f,1f/120f);
        }
        near(camera,0f,(float)Math.toRadians(.25),
                "camera rotated inside active steering dead-zone");
        pass();
    }

    private static void testLargeTurnMovesCameraButNotOneToOne() {
        float vehicle = (float)Math.toRadians(40);
        float camera = 0f;
        float steer = (float)Math.toRadians(20);
        for (int i=0;i<60;i++) {
            camera = ChaseCameraMath.updateHeading(vehicle,camera,steer,12f,1f/60f);
        }
        check(camera > 0f,"camera should eventually follow a large turn");
        check(camera < (float)Math.toRadians(20),
                "camera followed too aggressively while steering");
        pass();
    }

    private static void testCameraRecentersAfterSteeringRelease() {
        float vehicle = (float)Math.toRadians(32);
        float camera = 0f;
        for (int i=0;i<240;i++) {
            camera = ChaseCameraMath.updateHeading(vehicle,camera,0f,10f,1f/120f);
        }
        check(Math.abs(VehiclePhysics.normalizeAngle(vehicle-camera))
                        < Math.toRadians(4),
                "camera failed to re-center after steering release");
        pass();
    }

    private static void testThirtySixtyOneTwentyConsistency() {
        float a=0f,b=0f,c=0f;
        float vehicle=(float)Math.toRadians(42);
        float steer=(float)Math.toRadians(16);

        for(int i=0;i<60;i++)
            a=ChaseCameraMath.updateHeading(vehicle,a,steer,14f,1f/30f);
        for(int i=0;i<120;i++)
            b=ChaseCameraMath.updateHeading(vehicle,b,steer,14f,1f/60f);
        for(int i=0;i<240;i++)
            c=ChaseCameraMath.updateHeading(vehicle,c,steer,14f,1f/120f);

        near(a,c,(float)Math.toRadians(.8),"30/120 Hz camera heading diverged");
        near(b,c,(float)Math.toRadians(.4),"60/120 Hz camera heading diverged");
        pass();
    }

    private static void testAngleWrap() {
        float vehicle=(float)Math.toRadians(-179);
        float camera=(float)Math.toRadians(179);
        for(int i=0;i<120;i++) {
            camera=ChaseCameraMath.updateHeading(vehicle,camera,0f,6f,1f/120f);
            check(Float.isFinite(camera),"camera heading became non-finite");
        }
        float delta=Math.abs(VehiclePhysics.normalizeAngle(vehicle-camera));
        check(delta < Math.toRadians(3.5),"camera took long way around angle wrap");
        pass();
    }

    private static void pass(){ passed++; }

    private static void check(boolean ok,String message){
        if(!ok) throw new AssertionError(message);
    }

    private static void near(float actual,float expected,float tolerance,String message){
        if(Math.abs(actual-expected)>tolerance){
            throw new AssertionError(message+": expected "+expected+" ± "+tolerance+", actual "+actual);
        }
    }
}
