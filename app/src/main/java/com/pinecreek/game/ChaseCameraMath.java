package com.pinecreek.game;

/**
 * Pure math for the third-person chase camera.
 *
 * Design goal:
 * - the truck moves/turns inside the frame first
 * - the screen does not rotate one-to-one with steering
 * - after the turn is established (or steering is released), the camera
 *   smoothly catches up like an arcade racing chase camera
 */
public final class ChaseCameraMath {
    private ChaseCameraMath() {}

    public static float updateHeading(
            float vehicleHeading,
            float cameraHeading,
            float steeringAngle,
            float speedAbs,
            float dt) {

        dt = Math.max(0f, Math.min(.05f, dt));
        float delta = VehiclePhysics.normalizeAngle(vehicleHeading - cameraHeading);
        float absDelta = Math.abs(delta);
        boolean steeringNow = Math.abs(steeringAngle) > Math.toRadians(1.5);

        // While steering, allow the car to rotate visibly inside the frame.
        // 18 degrees is intentional: smaller steering corrections should move
        // the car across the display without making the whole screen rotate.
        float activeDeadZone = (float)Math.toRadians(18.0);

        if (steeringNow) {
            if (absDelta > activeDeadZone) {
                float excess = absDelta - activeDeadZone;
                float rate = 0.62f + Math.min(.48f, speedAbs * .012f);
                float follow = 1f - (float)Math.exp(-dt * rate);
                cameraHeading = VehiclePhysics.normalizeAngle(
                        cameraHeading + Math.signum(delta) * excess * follow);
            }
        } else {
            // Once steering is released, gently re-center the chase camera.
            float neutralDeadZone = (float)Math.toRadians(2.0);
            if (absDelta > neutralDeadZone) {
                float rate = 1.35f + Math.min(.85f, speedAbs * .022f);
                float follow = 1f - (float)Math.exp(-dt * rate);
                cameraHeading = VehiclePhysics.normalizeAngle(
                        cameraHeading + delta * follow);
            }
        }

        return cameraHeading;
    }
}
