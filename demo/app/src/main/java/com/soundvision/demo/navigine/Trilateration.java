package com.soundvision.demo.navigine;

import com.soundvision.demo.location.ffgeojson.PointD;
import com.soundvision.demo.location.flat.BeaconProp;

import java.util.ArrayList;
import java.util.List;

public class Trilateration {

    List<BeaconProp> mLocationBeacons = new ArrayList<>();      //set of beacons at the current location
    List <BeaconProp> mBeaconProp = new ArrayList<>();          //set of beacon measurements
    //List <Double>     mXY = new ArrayList<>();                  //desired X,Y coordinates
    PointD mXY = new PointD(0,0);


   public Trilateration()
    {

    }
/*
   public Trilateration( const Trilateration& trilat )
  : mLocationBeacons( trilat.mLocationBeacons )
  , mBeaconProp     ( trilat.mBeaconProp )
  , mXY             ( trilat.mXY )
  , mCurLocationId  ( trilat.mCurLocationId )
  , mErrorMessage   ( trilat.mErrorMessage )
    {

    }
*/
    

    public void  updateMeasurements( List < BeaconProp > props )
    {
       mBeaconProp = props;
    }


    public void  fillLocationBeacons( List <BeaconProp> props )
    {
       mLocationBeacons = props;
    }


    public int calculateCoordinates()
    {
        //int errorCode = deleteDuplicateMeasurements( mBeaconProp );

        // sort into ascending order (from -100 to 0)
        //std::sort( mBeaconProp.begin(), mBeaconProp.end() );

        //filter out beacons that are not in the map
        mBeaconProp = filterUnknownBeacons (mBeaconProp, mLocationBeacons);

        if (mBeaconProp.size () == 0 )
        {
            return -1;
        }

        // number of measurements that we'll use to obtain coordinates
        int nOfMeas = (mBeaconProp.size() >= 3 ? 3 : mBeaconProp.size());

        List<BeaconProp> mostStrongMeas = new ArrayList<>();
        if (mBeaconProp.size() >= 3)
        {
            for (int i = 0; i < nOfMeas; i++)
            mostStrongMeas.add(mBeaconProp.get(i));
        }

        int errorCode = calculateTrilaterationCoordinates();
        if (errorCode != 0)
            return errorCode;

        int UseOls = 0;
        if (UseOls != 0)
        {
            getLeastSquaresCoordinates();
            getLeastSquaresCoordinates();
        }
/*
#ifdef TRILATERATION_DEBUG_
        printXyToFile ("trilateration_xy_debug.txt");
        printf ("x = %lf \n y = %lf \n",mXY[0],mXY[1]);
#endif
*/
 
        return 0;
    }
    

    // this function erase unknown transmitters that don't presence in a map
// and fill x,y, coordinates of beacons
    public List<BeaconProp>  filterUnknownBeacons( List<BeaconProp> props,
                                          List<BeaconProp> mapBeacons )
    {
        // don't increment iterator in for 'cause there is erase inside loop.
        // otherwise we'll skip 1 element after erased
        List<BeaconProp> match = new ArrayList<>(); 
        for (BeaconProp it : props)
        {
            for (BeaconProp m : mapBeacons) 
            {
                if (m.mac.equals(it.mac)) {
                    match.add(it);
                    break;
                }
            }

        }
        return match;
    }


    public double getX()
    {
        return mXY.X;
    }


    public double getY()
    {
        return mXY.Y;
    }

/*
    public void  printXyToFile( char* filename ) const
    {
        FILE *f;
        f = fopen( filename, "w" );
        fprintf( f, "%lf %lf \n", mXY[ 0 ], mXY[ 1 ] );
        fclose( f );
    }
*/

    public int calculateTrilaterationCoordinates()
    {
        double normalizeCoefficient = 0.0;
        //take revert values, because lower distance then bigger weight
        for (int i = 0; i < mBeaconProp.size(); i++)
        normalizeCoefficient += 1.0 / Math.abs( mBeaconProp.get(i).distance );

        List <Double> weight = new ArrayList<>();
        for (int i = 0; i < mBeaconProp.size(); i++)
        weight.add(0.0);


        for (int i = 0; i < mBeaconProp.size(); i++)
        {
            // calculate probability of being at beacons x,y coordinates
            double v = weight.get(i);
            v += 1.0 / (Math.abs( mBeaconProp.get(i).distance *
                    normalizeCoefficient ));
            weight.set(i, v);

            double beaconX = 0;
            double beaconY = 0;
            beaconX = mBeaconProp.get(i).x;
            beaconY = mBeaconProp.get(i).y;

            //find final coordinates according to probability
            mXY.X += weight.get(i) * beaconX;
            mXY.Y += weight.get(i) * beaconY;
        }
        return 0;
    }

/*
    // this function deletes Duplicate beacon measurements and averages rssi signals
// from equal beacons
    public int deleteDuplicateMeasurements( List<BeaconProp> beaconList )
    {
        //sort measurements in beacon name order
        //std::sort( BeaconProp.begin(), BeaconProp.end(), compareBeaconPropByName );
        //for (int i = 0; i < beaconEntry.size(); i++)
        //for (List<BeaconProp>::iterator itBeaconProp = BeaconProp.begin();
        //itBeaconProp != BeaconProp.end(); ++itBeaconProp)
        int i1 = 0;
        while (i1<beaconList.size() )
        {
            BeaconProp it =beaconList.get(i1);
            //count number of occurrences of itBeaconProp
            //List<BeaconProp>::iterator it = BeaconProp.begin();

            //find first occurrence
            //std::count( BeaconProp.begin(), BeaconProp.end(), *itBeaconProp);
            //it = std::find( it, BeaconProp.end(), *itBeaconProp );

            int nOfMeasFromSameAp = 0;
            double rssi = 0.0, distance = 0.0;

            //find all similar entries
            while (it != BeaconProp.end())
            {
                nOfMeasFromSameAp++;

                if ( it->getDistance() < 0)
                {
                    printf( "beacon id = %s distance = %lf < 0\n",
                            it->getBeaconId(), it->getDistance() );

                    return ERROR_IN_TRILATER;
                }

                //calc sum to get average
                rssi     += it->getRssi();
                distance += it->getDistance();

                //clear duplicate entries
                // we don't clear first occurrence!
                if (nOfMeasFromSameAp != 1)
                    it = BeaconProp.erase( it );
                else it++;

                if (it == BeaconProp.end())
                    break;
                it = std::find( it, BeaconProp.end(), *itBeaconProp );
            }

            if (nOfMeasFromSameAp == 0)
            {
                setErrorMessage("number of measurements from the same AP == 0 ! something wrong!\n");
                printf( "%s\n", getErrorMessage() );
                return ERROR_IN_TRILATER;
            }

            //set average rssi to the beacon that doesn't has duplicates now
            rssi     /= (std::max)(nOfMeasFromSameAp, 1);
            distance /= (std::max)(nOfMeasFromSameAp, 1);

            itBeaconProp->setRssi( rssi );
        }

        if (BeaconProp.size() < 3)
        {
            setErrorMessage( "less then 3 visible beacons in measurements "
                    "It's not enough for trilateration\n" );
            printf( "%s\n", getErrorMessage() );
            return ERROR_NO_SOLUTION_TRILATERATION;
        }

        return 0;
    }
*/

    public void  getLinearSystem( List<Double> matrixA,
                                  List<Double> b,
                                         int dim )
    {
        int nOfVisibleBeacons = mBeaconProp.size ();

        BeaconProp firstBeacon = mBeaconProp.get(0);
        double xFirstBeacon = 0, yFirstBeacon = 0;

        xFirstBeacon = firstBeacon.x;
        yFirstBeacon = firstBeacon.y;

        double firstBeaconDistance = firstBeacon.distance;
        double normaFirstBeacon = xFirstBeacon * xFirstBeacon +
                yFirstBeacon * yFirstBeacon;

        for ( int i = 0; i < nOfVisibleBeacons - 1; i++ )
        {
            // fill the matrix A and right part of linear system b
            double x = 0.0, y = 0.0;
            x = mBeaconProp.get(i + 1).x;
            y = mBeaconProp.get(i + 1).y;

            double distance = mBeaconProp.get(i + 1).distance;

            matrixA.set(i * dim, 2 * (x - xFirstBeacon));
            matrixA.set(i * dim + 1, 2 * (y - yFirstBeacon));

            double norma = x * x + y * y;
            b.set(i, firstBeaconDistance * firstBeaconDistance - distance * distance -
                    normaFirstBeacon + norma);
        }

        return;
    }


    // This function estimate x,y coordinates by triangulation algorithm
// using ordinary least squares method for solving linear system
    public void  getLeastSquaresCoordinates()
    {
        // How many coordinates do we consider? In planar case it equal to 2
        int dim = 2;
        int nOfVisibleBeacons = mBeaconProp.size();
        // create matrix for triangulation linear system, that we will solve
        // for obtaining the coordinates we need at least dim + 1 beacons
        // index [i][j] = i * dim + j

        // By subtracting the last equation from each other we bring our 
        // nonlinear system to linear matrixA

        List <Double> matrixA = new ArrayList<>();
        for (int i = 0; i < (nOfVisibleBeacons - 1) * dim; i++)
            matrixA.add(0.0);
        //((nOfVisibleBeacons - 1) * dim, 0.0);
        List <Double> b = new ArrayList<>();
        for (int i = 0; i < (nOfVisibleBeacons - 1); i++)
            b.add(0.0);
        //(nOfVisibleBeacons - 1, 0.0 );

        getLinearSystem(matrixA, b, dim);
        solveLinearSystem (matrixA, b);
    }


    // this function solve overdetermined linear system 
// by ordinary least squares method x = A_ * B
// where A_ - pseudo inverse matrix
    public void  solveLinearSystem( List <Double> matrixA,
                                    List <Double> b )
    {
        int nOfEquations = b.size();
        int dim = matrixA.size() / nOfEquations;

        List <Double> xy = new ArrayList<>();
        for (int i = 0; i < dim; i++)
            xy.add(0.0);

        List <Double> aTransposeA = new ArrayList<>();
        for (int i = 0; i < dim * dim; i++)
            aTransposeA.add(0.0);

        // find pseudoInverseMatrix
        for (int row = 0; row < dim; row++)
        {
            for (int col = 0; col < dim; col++)
            {
                for ( int inner = 0; inner < nOfEquations; inner++ )
                {
                    // Multiply the row of A_transpose by the column of A 
                    // to get the row, column of multiplyAATranspose.
                    aTransposeA.set(row * dim + col, matrixA.get(inner * dim + row) * matrixA.get(inner * dim + col));
                }
            }
        }

     //vector <double> revertMatrix( dim * dim, 0. );

        List <Double> revertMatrix = new ArrayList<>();
        for (int i = 0; i < dim * dim; i++)
            revertMatrix.add(0.0);

        double det = aTransposeA.get(0) * aTransposeA.get(3) -
                aTransposeA.get(2) * aTransposeA.get(1);

        //simple formula for invert matrix 2x2
        revertMatrix.set(0, aTransposeA.get(3) / det);
        revertMatrix.set(1, -aTransposeA.get(1) / det);
        revertMatrix.set(2, -aTransposeA.get(2) / det);
        revertMatrix.set(3, aTransposeA.get(0) / det);

        //Multiply revertMatrix on A transpose
        //vector <double> matrix2xN (dim * nOfEquations, 0.0);
        List <Double> matrix2xN = new ArrayList<>();
        for (int i = 0; i < dim * nOfEquations; i++)
            matrix2xN.add(0.0);
        for ( int row = 0; row < dim; row++ )
        {
            for ( int col = 0; col < nOfEquations; col++ )
            {
                for ( int inner = 0; inner < dim; inner++ )
                {
                    // Multiply the row of A_transpose by the column of A 
                    // to get the row, column of multiplyAATranspose.
                    matrix2xN.set(row * nOfEquations + col, matrix2xN.get(row * nOfEquations + col) +
                            revertMatrix.get(row * dim + inner) * matrixA.get(col * dim + inner));
                }
            }
        }

        //Multiply matrix2xN on B vector 
        for ( int col = 0; col < dim; col++ )
        {
            for ( int inner = 0; inner < nOfEquations; inner++ )
            {
                xy.set(col, xy.get(col) + matrix2xN.get(col * nOfEquations + inner) * b.get(inner));
            }
        }
        return;
    }

/*
    List<Beacon>::const_iterator findBeaconForMeas( const vector<Beacon>& mapBeacons,
                                                       const string& measureBeaconId )
    {
        List<Beacon>::const_iterator it;
        for (it = mapBeacons.begin(); it != mapBeacons.end(); ++it)
        {
            if (it->getId() == measureBeaconId)
                return it;
        }
        return it;
    }
*/


}
