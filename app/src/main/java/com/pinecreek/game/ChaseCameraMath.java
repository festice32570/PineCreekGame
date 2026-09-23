package com.pinecreek.game;

/**
 * Pure math for the third-person chase camera.
 *
 * The important design goal is that steering moves/rotates the VEHICLE first.
 * The screen should not rotate one-to-one with steering input.
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
        float deadZone = (float)Math.toRadians(10.0);
        float absDelta = Math.abs(delta);
        boolean steeringNow = Math.abs(steeringAngle) > Math.toRadians(2.0);

        if (steeringNow && absDelta > deadZone) {
            float excess = absDelta - deadZone;
            float follow = 1f - (float)Math.exp(-dt * (1.45f + speedAbs * .018f));
            cameraHeading = VehiclePhysics.normalizeAngle(
                    cameraHeading + Math.signum(delta) * excess * follow);
        } else if (!steeringNow) {
            float follow = 1f - (float)Math.exp(-dt * (1.15f + speedAbs * .012f));
            cameraHeading = VehiclePhysics.normalizeAngle(cameraHeading + delta * follow);
        }

        return cameraHeading;
    }
}
