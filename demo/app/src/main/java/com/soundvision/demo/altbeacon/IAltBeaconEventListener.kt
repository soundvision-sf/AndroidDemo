package com.soundvision.demo.altbeacon

import com.soundvision.demo.location.ffgeojson.PointD

interface IAltBeaconEventListener {

	fun onLocationUpdate(pt: PointD)
}