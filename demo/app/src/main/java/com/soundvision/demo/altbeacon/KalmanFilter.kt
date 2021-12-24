package com.soundvision.demo.altbeacon

/**
 * Simple implementation of the Kalman Filter for 1D data.
 * Originally written in JavaScript by Wouter Bulten
 *
 * Now rewritten into Java
 * 2017
 *
 * @license MIT License
 *
 * @author Sifan Ye
 * @author Andreas Eppler
 */
class KalmanFilter {

	private var A: Double = 1.0
	private var B: Double = 0.0
	private var C: Double = 1.0
	private var R: Double
	private var Q: Double
	private var cov: Double = Double.NaN
	private var x: Double = Double.NaN

	/**
	 * Constructor
	 *
	 * @param R Process noise
	 * @param Q Measurement noise
	 * @param A State vector
	 * @param B Control vector
	 * @param C Measurement vector
	 */
	constructor(R: Double, Q: Double, A: Double, B: Double, C: Double) {
		this.R = R
		this.Q = Q
		this.A = A
		this.B = B
		this.C = C
		cov = Double.NaN
		x = Double.NaN // estimated signal without noise
	}

	/**
	 * Constructor
	 *
	 * @param R Process noise
	 * @param Q Measurement noise
	 */
	constructor(R: Double, Q: Double) {
		this.R = R
		this.Q = Q
	}

	/**
	 * Filters a measurement
	 *
	 * @param measurement The measurement value to be filtered
	 * @param u The controlled input value
	 * @return The filtered value
	 */
	fun filter(measurement: Double, u: Double): Double {
		if (java.lang.Double.isNaN(x)) {
			x = 1 / C * measurement
			cov = 1 / C * Q * (1 / C)
		} else {
			val predX = A * x + B * u
			val predCov = A * cov * A + R

			// Kalman gain
			val K = predCov * C * (1 / (C * predCov * C + Q))

			// Correction
			x = predX + K * (measurement - C * predX)
			cov = predCov - K * C * predCov
		}
		return x
	}

	/**
	 * Filters a measurement
	 *
	 * @param measurement The measurement value to be filtered
	 * @return The filtered value
	 */
	fun filter(measurement: Double): Double {
		val u = 0.0
		if (java.lang.Double.isNaN(x)) {
			x = 1 / C * measurement
			cov = 1 / C * Q * (1 / C)
		} else {
			val predX = A * x + B * u
			val predCov = A * cov * A + R

			// Kalman gain
			val K = predCov * C * (1 / (C * predCov * C + Q))

			// Correction
			x = predX + K * (measurement - C * predX)
			cov = predCov - K * C * predCov
		}
		return x
	}

	/**
	 * Set the last measurement.
	 * @return The last measurement fed into the filter
	 */
	fun lastMeasurement(): Double = x

	/**
	 * Sets measurement noise
	 *
	 * @param noise The new measurement noise
	 */
	fun setMeasurementNoise(noise: Double) {
		Q = noise
	}

	/**
	 * Sets process noise
	 *
	 * @param noise The new process noise
	 */
	fun setProcessNoise(noise: Double) {
		R = noise
	}
}