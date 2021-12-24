package com.soundvision.demo.altbeacon

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.soundvision.demo.BaseActivity
import com.soundvision.demo.R
import com.soundvision.demo.altbeaconjava.CustomFusionRssiFilter
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.service.RangedBeacon

class AltBeaconApp private constructor() {

	companion object {

		private const val TAG: String = "AltBeaconApp.kt"
		private const val STOP_MONITORING_DELAY: Long = 50

		@Volatile
		private var INSTANCE: AltBeaconApp? = null
		fun getInstance(): AltBeaconApp {
			synchronized(this) {
				var instance = INSTANCE
				if (instance == null) {
					instance = AltBeaconApp()
					INSTANCE = instance
				}
				//smart cast to non-null
				return instance
			}
		}
	}

	private var currentRegion: Region? = null
	private var customRangeNotifier: CustomRangeNotifier? = null

	//region Public methods
	fun init(ctx: Context?, customRangeNotifier: CustomRangeNotifier) {
		val beaconManager = BeaconManager.getInstanceForApplication(ctx!!)

		// By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
		// find a different type of beacon, you must specify the byte layout for that beacon's
		// advertisement with a line like below.  The example shows how to find a beacon with the
		// same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
		// layout expression for other beacon types, do a web search for "setBeaconLayout"
		// including the quotes.
		//
		//beaconManager.getBeaconParsers().clear();
		//beaconManager.getBeaconParsers().add(new BeaconParser().
		//        setBeaconLayout("m:0-1=4c00,i:2-24v,p:24-24"));

		// By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
		// find a different type of beacon like Eddystone or iBeacon, you must specify the byte layout
		// for that beacon's advertisement with a line like below.
		//
		// If you don't care about AltBeacon, you can clear it from the defaults:
		beaconManager.beaconParsers.clear()

		// The example shows how to find iBeacon.
		beaconManager.beaconParsers.add(
			BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
		)
		//setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
		//BeaconParser().setBeaconLayout("m:2-3=abeac"));

		/*
        beaconManager.getBeaconParsers()
            .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
        // Detect the telemetry (TLM) frame:
        // Detect the telemetry (TLM) frame:
                beaconManager.getBeaconParsers()
                    .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))
        // Detect the URL frame:
        // Detect the URL frame:
                beaconManager.getBeaconParsers()
                    .add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))

        */
		// enabling debugging will send lots of verbose debug information from the library to Logcat
		// this is useful for troubleshooting problmes
		BeaconManager.setDebug(true)

		// The BluetoothMedic code here, if included, will watch for problems with the bluetooth
		// stack and optionally:
		// - power cycle bluetooth to recover on bluetooth problems
		// - periodically do a proactive scan or transmission to verify the bluetooth stack is OK
		// BluetoothMedic.getInstance().enablePowerCycleOnFailures(this)
		// BluetoothMedic.getInstance().enablePeriodicTests(this, BluetoothMedic.SCAN_TEST + BluetoothMedic.TRANSMIT_TEST)

		// By default, the library will scan in the background every 5 minutes on Android 4-7,
		// which will be limited to scan jobs scheduled every ~15 minutes on Android 8+
		// If you want more frequent scanning (requires a foreground service on Android 8+),
		// configure that here.
		// If you want to continuously range beacons in the background more often than every 15 mintues,
		// you can use the library's built-in foreground service to unlock this behavior on Android
		// 8+.   the method below shows how you set that up.

		setupForegroundService(ctx)
		beaconManager.setEnableScheduledScanJobs(false)
		beaconManager.backgroundBetweenScanPeriod = 0
		beaconManager.backgroundScanPeriod = 1100
		beaconManager.foregroundBetweenScanPeriod = 0
		beaconManager.foregroundScanPeriod = 1100
		//NOTE: not from the alt beacon lib. demo
		RangedBeacon.setSampleExpirationMilliseconds(10000)

		// Ranging callbacks will drop out if no beacons are detected
		// Monitoring callbacks will be delayed by up to 25 minutes on region ex6it
		// beaconManager.setIntentScanningStrategyEnabled(true)

		// The code below will start "monitoring" for beacons matching the region definition below
		// the region definition is a wildcard that matches all beacons regardless of identifiers.
		// if you only want to detect beacons with a specific UUID, change the id1 paremeter to
		// a UUID like Identifier.parse("2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6")
		//region = Region("radius-uuid", null, null, null)
		val region = Region("all-beacons-region", null, null, null)
		//val region = Region("all-beacons-region", Identifier.parse("417192b8-533d-4c3d-b5f3-d56a4be8fdce"), null, null)
		//val region = Region("all-beacons-region", Identifier.parse("d546df97475747efbe093e2dcbdd0c77"), null, null)
		currentRegion = region
		beaconManager.startMonitoring(region)
		beaconManager.startRangingBeacons(region)
		//		// These two lines set up a Live Data observer so this Activity can get beacon data from the Application class
		//		val regionViewModel = BeaconManager.getInstanceForApplication(ctx).getRegionViewModel(region)
		//		// observer will be called each time the monitored regionState changes (inside vs. outside region)
		//		regionViewModel.regionState.observeForever { integer ->
		//			if (integer == MonitorNotifier.OUTSIDE) {
		//				//Log.d(TAG, "outside beacon region: "+region)
		//			} else {
		//				//Log.d(TAG, "inside beacon region: "+region)
		//				//sendNotification()
		//			}
		//		}
		//		// observer will be called each time a new list of beacons is ranged (typically ~1 second in the foreground)
		//		regionViewModel.rangedBeacons.observe(ctx as LifecycleOwner, observerBeacons!!)

		this.customRangeNotifier = customRangeNotifier
		beaconManager.addMonitorNotifier(centralMonitoringNotifier)
		beaconManager.addRangeNotifier(centralRangingNotifier)

		//added later
		BeaconManager.setRssiFilterImplClass(CustomFusionRssiFilter::class.java)
		CustomFusionRssiFilter.setSampleExpirationMilliseconds(5000L)
	}

	fun onDestroy(ctx: Context?) {
		val context: Context = ctx ?: return
		currentRegion?.let { tempRegion ->
			val beaconManager = BeaconManager.getInstanceForApplication(context)
			//remove notifiers
			beaconManager.removeMonitorNotifier(centralMonitoringNotifier)
			beaconManager.removeRangeNotifier(centralRangingNotifier)
			this.customRangeNotifier = null
			//remove regions
			beaconManager.stopMonitoring(tempRegion)
			beaconManager.stopRangingBeacons(tempRegion)
			//stop service
			Handler().postDelayed({
				beaconManager.shutdownIfIdle()
			}, STOP_MONITORING_DELAY)
		}
	}
	//endregion

	//region Private methods
	private fun setupForegroundService(ctx: Context?) {
		val context: Context = ctx ?: return
		val builder = NotificationCompat.Builder(context, "BeaconReferenceApp")
		builder.setSmallIcon(R.drawable.ic_launcher_background)
		builder.setContentTitle("Scanning for Beacons")
		val intent = Intent(context, BaseActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
			context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
		)
		builder.setContentIntent(pendingIntent)

		val notificationManager = context.getSystemService(
			Context.NOTIFICATION_SERVICE
		) as NotificationManager

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				"beacon-ref-notification-id",
				"My Notification Name", NotificationManager.IMPORTANCE_DEFAULT
			)
			channel.description = "My Notification Channel Description"
			notificationManager.createNotificationChannel(channel)

			//notification builder
			builder.setChannelId(channel.id)
		}

		BeaconManager.getInstanceForApplication(context).enableForegroundServiceScanning(builder.build(), 456)
	}

	private fun sendNotification(ctx: Context?) {
		val context: Context = ctx ?: return
		val builder = NotificationCompat.Builder(context, "beacon-ref-notification-id")
			.setContentTitle("Beacon Reference Application")
			.setContentText("A beacon is nearby.")
			.setSmallIcon(R.drawable.ic_launcher_background)
		val stackBuilder = TaskStackBuilder.create(context)
		stackBuilder.addNextIntent(Intent(context, BaseActivity::class.java))
		val resultPendingIntent = stackBuilder.getPendingIntent(
			0,
			PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
		)
		builder.setContentIntent(resultPendingIntent)
		val notificationManager =
			context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(1, builder.build())
	}
	//endregion

	private val centralMonitoringNotifier: MonitorNotifier = object : MonitorNotifier {
		override fun didEnterRegion(region: Region?) {
			Log.d(TAG, "inside beacon region: $region")
		}

		override fun didExitRegion(region: Region?) {
			Log.d(TAG, "outside beacon region: $region")
		}

		override fun didDetermineStateForRegion(state: Int, region: Region?) {
			if (state == MonitorNotifier.OUTSIDE) {
				Log.d(TAG, "outside beacon region: $region")
			} else {
				Log.d(TAG, "inside beacon region: $region")
				//sendNotification()
			}
		}
	}

	private val centralRangingNotifier: RangeNotifier =
		RangeNotifier { beacons, region -> customRangeNotifier?.didRangeBeaconsInRegion(beacons, region) }

	/* Observer centralMonitoringObserver = new Observer<Integer> { state ->
        if (state == MonitorNotifier.OUTSIDE) {
            //Log.d(TAG, "outside beacon region: "+region)
        }
            else {
            //Log.d(TAG, "inside beacon region: "+region)
            //sendNotification()
        }
   }*/

	//	val centralRangingObserver = Observer { o, arg ->
	////		val beacons = arg as Collection<Beacon>
	////		for (beacon in beacons) {
	////			Log.d(TAG, "$beacon about ${beacon.distance} meters away")
	////		}
	//	}
}