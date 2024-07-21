## Concept
Using an App with the Android DJI-SDK as a Bridge between DJI Drones and Windows.

Two Programms:
* Android App written in Java
* Windows App written in C++/Qt
  
Connected with TCP, communicating using Json Dictionaries

(At time of creation no SDK existed to control DJI Drones from Windows.)

## Usage
1. Start TCP Server in Windows programm
2. Connect from Android to TCP Server
3. Connect from Android to DJI Drone
4. Create Waypoint Mission as txt-file
```
# general mission options
waypointCount 9
maxSpeed 15
autoSpeed 5
headingMode waypoint

# then add list of coordinates
# points can have options
wp 49.1313 8.9128 10
  heading 80
  speed 10
  action stay 2000
  action photo

# or only be a position 
wp 49.1413 8.9228 20
wp 49.1513 8.9328 30
wp 49.1613 8.9428 40
```
5. Load File into Windows program
6. Send Mission to Android/Drone and control 

![TestProg](https://github.com/user-attachments/assets/26055163-e450-4c92-b6de-0c99ceda71f1)

Information about Battery Status, Position, Speed, Mission Status are regularly sent from Android and displayed in Windows  

Mission can be paused/resumed/aborted, Drone can be called back to designated Home point and landed
