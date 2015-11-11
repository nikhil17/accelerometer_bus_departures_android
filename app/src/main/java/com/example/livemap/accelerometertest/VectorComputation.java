package com.example.livemap.accelerometertest;


import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * Created by nikhil on 11/10/15.
 */
public class VectorComputation {
    private final int ARRAY_SIZE = 4;
    private int oldest_index = -1;
    private int size = -1;

    Vector3D[] arr;


    //tester main method
    public static void main (String [] args){
        VectorComputation vc = new VectorComputation();
        vc.addVector(new Vector3D(0,0,1));
        vc.addVector(new Vector3D(0,0,1));
        vc.addVector(new Vector3D(0,0,1));
        vc.addVector(new Vector3D(0,0,1));


        vc.addVector(new Vector3D(-1,0,4));
        vc.addVector(new Vector3D(2,4,8));
        vc.addVector(new Vector3D(5,0,0));
        vc.printArray();
        System.out.println(vc.getJostleIndex());
    }

    public VectorComputation(){
        arr = new Vector3D[ARRAY_SIZE];
        initializeArray();
    }

    public void initializeArray(){
        for (int i = 0; i < arr.length; i++) {
            arr[i] = null;
        }
    }

    public void printArray(){
        for(int i = 0; i < arr.length; i++){
            System.out.println(arr[i]);
        }
    }

    /**
     * Computes the jostle index
     *
     * @return the volume of the tetrahedron made using the 4 points in the Vector array
     * returns -1 if the buffer contains less than 4 values
     */
    public double getJostleIndex(){
        if (size != arr.length -1)
            return -1;
        Vector3D A, B, C, D, AB, AC, AD;
        A = arr[0];
        B = arr[1];
        C = arr[2];
        D = arr[3];
        AB = A.subtract(B);
        AC = A.subtract(C);
        AD = A.subtract(D);

        return Math.abs(AD.dotProduct(AB.crossProduct(AC)))/6;
    }

    public void addVector(Vector3D v){
        if(size == -1){
            // first time initialization
            arr[0] = v;
            size = 0;
            oldest_index = 0;
        }
        else{
            if(size < arr.length - 1){
                size++;
                arr[size] = v;
            }
            else{
                if (size == arr.length - 1){
                    if (oldest_index < arr.length -1){
                        arr[oldest_index] = v;
                        oldest_index++;
                    }
                    else{
                        arr[oldest_index] = v;
                        oldest_index = 0;
                        //wrap around
                    }
                }
            }
        }
    }
}