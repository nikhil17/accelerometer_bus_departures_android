# Accelerometer
Accelerometer

Old repository was here:  
https://github.gatech.edu/wtang45/Accelerometer  
https://github.gatech.edu/NextBUZZ/livemap/commits/Accelerometer  

Current status- 
This application allows users to log accelerometer readings to a local SQLite database on the user's Android phone. 

Application stores last 4 acceleration values in a buffer and uses these values to compute jostle index (volume of the tetrahedron formed by the last 4 values)

Installation Instructions
Open a terminal shell and navigate to a containing directory.

Type in the following command - 
git clone https://github.gatech.edu/NextBUZZ/accelerometer

Open Android Studio and import the project (File -> New-> Import Project)


Database Access
The recorded data is stored on an internal SQLite database. This database can be accessed through a shell. To access the local database on an Android phone first install ADB (Android Debug Bridge).

1)In a command prompt type 
adb devices
Copy the device ID to your clipboard
2) Connect to the device using the commmand 
adb -s <DEVICE-ID> shell
3) Then run as the accelerometer app (without root access the application files cannot be directly accessed)
run-as com.example.livemap.accelerometertest
4) Change directory to databases
cd com.example.livemap.accelerometertest/databases
5) The database (.db file) is stored here


