package com.soundvision.demo.altbeacon

import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.Region

interface CustomRangeNotifier {
	fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?)
}