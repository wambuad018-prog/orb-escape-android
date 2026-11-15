# Orb Escape

A small Android mobile game prototype built with Kotlin and Jetpack Compose.

## Gameplay
- Guide the flying character by dragging across the screen; it respawns in the center.
- Collect green crystals between the moving red barriers to replenish energy and score.
- Occasionally collect a gold shield pickup between barriers for 50 points and protection during a barrier crossing.
- Slip through the changing openings in each segmented barrier; barriers start fast, accelerate to 20 px/frame, and arrive closer together.
- Watch the decorative comets, stars, and nebula lights as you play.

## Run locally
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run the `app` configuration on an emulator or connected Android device.

## Google Play packaging
1. Update `applicationId` and app metadata in `app/build.gradle.kts`.
2. Generate a signed app bundle (`.aab`) in Android Studio or with Gradle.
3. Upload the bundle to Google Play Console.
4. Add the privacy policy, screenshots, app description, and content rating.

## Notes
This is a playable MVP and can be expanded with sound, menus, levels, and shop systems.
