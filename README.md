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

How to copy .db file to current directory on your computer

#Type this in one terminal 
adb shell 
run-as com.example.livemap.accelerometertest
cp databases/accelerometer_readings.db /sdcard

#In another terminal
adb pull /sdcard/accelerometer_readings.db .



