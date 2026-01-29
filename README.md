# Net-Test Android Scaffolding

## Package structure
- `com.example.nettest.ui` - UI layer, activities, and ViewModels.
- `com.example.nettest.service` - Foreground service scaffolding.
- `com.example.nettest.vpn` - `MyVpnService` local-only `VpnService` placeholder.
- `com.example.nettest.vpn` - `VpnService` implementation placeholder.
- `com.example.nettest.overlay` - Overlay service scaffolding.
- `com.example.nettest.domain` - Domain layer (use-cases, entities).
- `com.example.nettest.data` - Data layer (repositories, data sources).

## Required permissions
- `android.permission.FOREGROUND_SERVICE` for foreground work.
- `android.permission.SYSTEM_ALERT_WINDOW` for overlays.
- `android.permission.BIND_VPN_SERVICE` declared on the VPN service.

## Gradle configuration
- Kotlin + Android app module with Compose + Material 3 dependencies.
- `minSdk 16` for Android 4.1+ support, `targetSdk 34` for modern behavior.

## Foreground service setup
- `ForegroundNetworkService` includes TODOs for notification channel and
  foreground notification startup.

## Notes
- This is scaffold-only: no production logic is implemented yet.
