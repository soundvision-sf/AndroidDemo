package com.soundvision.demo.location.internalpositioningjava;

/**
 * Created by galen.georgiev on 8.11.2017 г..
 */

class InternalProperties
{
    int[][] rssis = new int[100][3];
    int[][] rssisTriangulation = new int[100][3];
    int lowestSearch;
    int lowSearchTill;
    int averageOver;
    int averageUntil;
    int beacReadings;
    int readsRecieved;
    int beaconSwitch;
    int counter;
    int stableCounter;
    int rssiShift;
    double heading;
    double zzOffset;
    double rssiLowPass;
    double motionInterval;
    double positionGridSize;
    double displacementGridSize;

    {
        lowestSearch = 3;
        lowSearchTill = 0;
        averageOver = 4;
        averageUntil = 0;
        beacReadings = 0;
        readsRecieved = 0;
        beaconSwitch = 3;
        counter = 0;
        stableCounter = 0;
        rssiShift = 0;
        heading = 0.0;
        zzOffset = 90.0;
        rssiLowPass = 0.0;
        motionInterval = 1.0 / 50.0;
        positionGridSize = 5.0;
        displacementGridSize = 25.0;

        //RSSI to 25cm Offset
        //rssis[x][0]=minimum offset
        //rssis[x][1]=maximum offset
        //rssis[x][2]=offset scaler
        for (int i = 0; i < 100; i++) {
            if (i < 45) {
                rssis[i][0] = 0;
                rssis[i][1] = 0;
                rssis[i][2] = 5;
                rssisTriangulation[i][0] = 0;
                rssisTriangulation[i][1] = 0;
                rssisTriangulation[i][2] = 5;
            }
            else if (i < 50) {
                rssis[i][0] = 0;
                rssis[i][1] = 2;
                rssis[i][2] = 5;
                rssisTriangulation[i][0] = 0;
                rssisTriangulation[i][1] = 2;
                rssisTriangulation[i][2] = 5;
            }
            else if (i < 54) {
                rssis[i][0] = 1;
                rssis[i][1] = 3;
                rssis[i][2] = 4;
                rssisTriangulation[i][0] = 1;
                rssisTriangulation[i][1] = 3;
                rssisTriangulation[i][2] = 4;
            }
            else if (i < 58) {
                rssis[i][0] = 2;
                rssis[i][1] = 5;
                rssis[i][2] = 4;
                rssisTriangulation[i][0] = 2;
                rssisTriangulation[i][1] = 5;
                rssisTriangulation[i][2] = 4;
            }
            else if (i < 61) {
                rssis[i][0] = 3;
                rssis[i][1] = 6;
                rssis[i][2] = 4;
                rssisTriangulation[i][0] = 3;
                rssisTriangulation[i][1] = 6;
                rssisTriangulation[i][2] = 4;
            }
            else if (i < 63) {
                rssis[i][0] = 5;
                rssis[i][1] = 10;
                rssis[i][2] = 3;
                rssisTriangulation[i][0] = 6;
                rssisTriangulation[i][1] = 9;
                rssisTriangulation[i][2] = 3;
            }
            else if (i < 67) {
                rssis[i][0] = 8;
                rssis[i][1] = 14;
                rssis[i][2] = 3;
                rssisTriangulation[i][0] = 9;
                rssisTriangulation[i][1] = 13;
                rssisTriangulation[i][2] = 3;
            }
            else if (i < 69) {
                rssis[i][0] = 12;
                rssis[i][1] = 20;
                rssis[i][2] = 2;
                rssisTriangulation[i][0] = 13;
                rssisTriangulation[i][1] = 18;
                rssisTriangulation[i][2] = 2;
            }
            else if (i < 71) {
                rssis[i][0] = 16;
                rssis[i][1] = 26;
                rssis[i][2] = 1;
                rssisTriangulation[i][0] = 17;
                rssisTriangulation[i][1] = 24;
                rssisTriangulation[i][2] = 1;
            }
            else if (i < 73) {
                rssis[i][0] = 20;
                rssis[i][1] = 32;
                rssis[i][2] = 1;
                rssisTriangulation[i][0] = 22;
                rssisTriangulation[i][1] = 28;
                rssisTriangulation[i][2] = 1;
            }
            else if (i < 76) {
                rssis[i][0] = 22;
                rssis[i][1] = 38;
                rssis[i][2] = 1;
                rssisTriangulation[i][0] = 24;
                rssisTriangulation[i][1] = 33;
                rssisTriangulation[i][2] = 1;
            }
            else if (i < 78) {
                rssis[i][0] = 26;
                rssis[i][1] = 42;
                rssis[i][2] = 1;
                rssisTriangulation[i][0] = 26;
                rssisTriangulation[i][1] = 42;
                rssisTriangulation[i][2] = 1;
            }
            else if (i < 81) {
                rssis[i][0] = 30;
                rssis[i][1] = 46;
                rssis[i][2] = 1;
                rssisTriangulation[i][0] = 30;
                rssisTriangulation[i][1] = 46;
                rssisTriangulation[i][2] = 1;
            }
            else {
                rssis[i][0] = 34;
                rssis[i][1] = 50;
                rssis[i][2] = 1;
                rssisTriangulation[i][0] = 34;
                rssisTriangulation[i][1] = 50;
                rssisTriangulation[i][2] = 1;
            }
        }
    }
}
