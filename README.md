# RobotCA
This project is under development, it's forked from mtbii/RobotCA. The further development envolves navigating the robot only by using the map. Here it will be possible to make a route with markers, which the robot will then follow. It will also be possible to make an area where the robot will drive randomly in. Furthermore it is possible to add obstacles in the route or in the area, where the robot will then navigate around it.

The further developed application is developed by Capra Robotics and tested on their mobile robots.

### Installation guide
##### PreRequisites
Installed Ubuntu and ROS melodic

##### Rosjava
Follow the following link, Prerequisites and Core sources part is enough. Replace kinetic with melodic everywhere.
http://wiki.ros.org/rosjava/Tutorials/kinetic/Source%20Installation

##### Android studio
Follow the following for installation of android studio
http://wiki.ros.org/android/kinetic/Android%20Studio/Download

After installation, quit android studio.
If failed to load module "canberra-gtk-module", install from terminal
sudo apt install libcanberra-gtk-module libcanberra-gtk3-module

##### RobotCA App
In terminal:
git clone https://github.com/mortenCapra/RobotCA

After it's done, open android studio and open an existing project. Navigate to RobotCA/src/android_foo.
If prompted, install missing SDK packages.
