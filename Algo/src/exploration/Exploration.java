package exploration;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import astarpathfinder.FastestPath;
import robot.Robot;
import map.Map;
import config.Constant;
import astarpathfinder.AStarPathFinder;

public class Exploration {
    private FastestPath fp = new FastestPath();
    private Map map;
    private boolean image_stop;
    private ArrayList<String> last8Steps = new ArrayList<String>();
    private boolean looping = false;

    public void Exploration(Robot robot, int time, int percentage, int speed, boolean image_recognition){
        map = robot.getMap();

        // initialise list for tracking robot steps
        if(last8Steps.size() == 0) {
            for (int i = 0; i < 8; i++)
                last8Steps.add("null|");
        }

        if ((time == -1)&&(percentage == 100)) {
            if (image_recognition) {
                image_stop = false;
                ImgRec_Exploration2(robot, time, percentage, speed);
                //ImageRecognition_Exploration(robot, speed);
            } else {
                Limited_Exploration(robot, time, percentage, speed);
            }
        } else {
            Limited_Exploration(robot, time, percentage, speed);
        }
        //corner_calibration(robot);

        int[] path = fp.FastestPath(robot, robot.getWaypoint(), Constant.END, speed, true, false);
        if(path == null)
            return;
        switch (path[0]) {
            case Constant.LEFT:
                robot.rotateLeft();
                //robot.right_align();
                break;
            case Constant.BACKWARD:
                robot.rotateLeft();
                //robot.right_align();
                robot.rotateLeft();
                break;
            case Constant.RIGHT:
                robot.rotateRight();
                break;
            default:
                break;
        }
    };

    private void Limited_Exploration(Robot robot, int time, int percentage, int speed) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        //robot.setDirection(1);

        // exploration using left wall hugging
        do {
            if (time != -1) {
                // to account for the time-limited exploration
                int time_taken = (int) stopwatch.getElapsedTime();
                if (time_taken >= time) {
                    return;
                }
            }
            if (percentage != 100) {
                // to account for the coverage-limited exploration
                if (percent_complete(robot) >= percentage) {
                    return;
                }
            }

            System.out.println("Phase 1");

            // detect if robot is lost
            if (getStepsTaken().equals("A|W|A|W|A|W|A|W|"))
                looping = true;

            if(!looping)
                move(robot, speed, null); // exploration
            else
                recoveryMove(robot, speed, null); // recover when lost

            //corner_calibration(robot);
        } while (!at_pos(robot, Constant.START));

        //find unexplored blocks
        int[] unexplored = unexplored(robot, robot.getPosition());
        //if dead block declare explored

        // go to unexplored blocks using fastest path
        while (unexplored != null) {
            if (time != -1) {
                // to account for the time-limited exploration
                int time_taken = (int) stopwatch.getElapsedTime();
                if (time_taken >= time) {
                    return;
                }
            }
            if (percentage != 100) {
                // to account for the coverage-limited exploration
                if (percent_complete(robot) >= percentage) {
                    return;
                }
            }


            // fastest path to nearest unexplored block
            System.out.println("Phase 2");
            int[] path = fp.FastestPath(robot, null, unexplored, speed, false, true);

            if ((path == null) || (map.getGrid(unexplored[0], unexplored[1]).equals(Constant.UNEXPLORED))) {
                map.setGrid(unexplored[0], unexplored[1], Constant.EXPLORED);
            }
            unexplored = unexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        // return to start point using fastest path
        if (!at_pos(robot, Constant.START)) {
            System.out.println("Phase 3");
            System.out.println(Arrays.toString(robot.getPosition()));
            fp.FastestPath(robot, null, Constant.START, speed, true, true);
        }

        stopwatch.stop();
        System.out.println("Exploration Complete!");
    }

    private void ImgRec_Exploration(Robot robot, int time, int percentage, int speed) {
        System.out.println("ImageRecognition_Exploration(Robot robot)");
        //robot.setDirection(1);
        int[][] checked_obstacles = {{0}};
        boolean img_unexplored = false;
        int[] need_take = null;
        int[] go_to = null;
        boolean move = false;

        //  explore whole arena using standard limited_exploration code w checkedobstacles
        do {
            // detect if robot is lost
            if (getStepsTaken().equals("W|A|W|A|W|A|W|A|") || getStepsTaken().equals("A|W|A|W|A|W|A|W|"))
                looping = true;

            if(!looping)
                checked_obstacles = move(robot, speed, checked_obstacles); // exploration
            else
                recoveryMove(robot, speed, checked_obstacles); // recover when lost

            System.out.println(Arrays.deepToString(checked_obstacles));
            //corner_calibration(robot);
        } while (!at_pos(robot, Constant.START));

        int[] unexplored = unexplored(robot, robot.getPosition());

        // go to unexplored blocks using fastest path
        while (unexplored != null) {
            // fastest path to nearest unexplored block
            System.out.println("Phase 2");
            int[] path = fp.FastestPath(robot, null, unexplored, speed, false, true);
            if ((path == null) || (map.getGrid(unexplored[0], unexplored[1]).equals(Constant.UNEXPLORED))) {
                map.setGrid(unexplored[0], unexplored[1], Constant.OBSTACLE);
            }
            unexplored = unexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        // return to start point using fastest path
        if (!at_pos(robot, Constant.START)) {
            System.out.println("Phase 3");
            System.out.println(Arrays.toString(robot.getPosition()));
            fp.FastestPath(robot, null, Constant.START, speed, true, true);
        }

        //corner_calibration(robot);
        if (!this.image_stop) {
            //returns false
            this.image_stop = robot.captureImage(new int[][] {{-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1}});
        }

        if (!this.image_stop) {
            need_take = picture_taken(robot, robot.getPosition(), checked_obstacles);
            go_to = next_to_obstacle(robot, need_take);
            //finds the grid nearby the obstacle to take pic at
        }

        if (go_to == null) {
            img_unexplored = true;
            need_take = null;
            go_to = unexplored(robot, robot.getPosition());
        }

        //for islands
        while ((go_to != null) && !(this.image_stop)) {
            // fastest path to nearest island
            System.out.println("Phase 2");
            int[] path = fp.FastestPath(robot, null, go_to, 1, true, true); //find FP from current position to go_to grid
            if ((img_unexplored) && ((path == null) || (map.getGrid(go_to[0], go_to[1]).equals(Constant.UNEXPLORED)))) {
                //if the unexplored grid is unreachable ie. path is null, OR that the go_to grid is unexplored
                map.setGrid(go_to[0], go_to[1], Constant.OBSTACLE);
                int[][] temp = new int[checked_obstacles.length + 1][3];
                System.arraycopy(checked_obstacles, 0, temp, 0, checked_obstacles.length);
                temp[checked_obstacles.length] = go_to;
                checked_obstacles = temp;
                //ie. label go_to grid as a checked obstacle.
            } else {
                move = obstacle_on_left(robot, need_take);
            }

            if ((path != null) && move) {
                do {
                    checked_obstacles = move(robot, 1, checked_obstacles);
                    System.out.println(Arrays.deepToString(checked_obstacles));
                } while ((!at_pos(robot, go_to)) && !image_stop);
            }

            checked_obstacles = image_recognition(robot, checked_obstacles);

            // to return to start after exploring finish
            fp.FastestPath(robot, null, Constant.START, 1, true, true);
            //FP go home
            //corner_calibration(robot);

            //terminate after rec-ing 1 island.
            this.image_stop = true;

            robot.updateMap();
        }

        System.out.println("Exploration Complete!");
    }

    private void ImgRec_Exploration2(Robot robot, int time, int percentage, int speed) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        //robot.setDirection(1);
        int[][] checked_obstacles = {{0}};

        // exploration using left wall hugging
        do {
            if (time != -1) {
                // to account for the time-limited exploration
                int time_taken = (int) stopwatch.getElapsedTime();
                if (time_taken >= time) {
                    return;
                }
            }
            if (percentage != 100) {
                // to account for the coverage-limited exploration
                if (percent_complete(robot) >= percentage) {
                    return;
                }
            }

            System.out.println("Phase 1");

            // detect if robot is lost
            if (getStepsTaken().equals("A|W|A|W|A|W|A|W|"))
                looping = true;

            if(!looping)
                checked_obstacles = move(robot, speed, checked_obstacles); // exploration
            else
                recoveryMove(robot, speed, checked_obstacles); // recover when lost
            //System.out.println(Arrays.deepToString(checked_obstacles));
            //corner_calibration(robot);
        } while (!at_pos(robot, Constant.START));

        int[] unexplored = unexplored(robot, robot.getPosition());

        // go to unexplored blocks using fastest path
        while (unexplored != null) {
            if (time != -1) {
                // to account for the time-limited exploration
                int time_taken = (int) stopwatch.getElapsedTime();
                if (time_taken >= time) {
                    return;
                }
            }
            if (percentage != 100) {
                // to account for the coverage-limited exploration
                if (percent_complete(robot) >= percentage) {
                    return;
                }
            }

            // fastest path to nearest unexplored block
            System.out.println("Phase 2");
            int[] path = fp.FastestPath(robot, null, unexplored, speed, false, true);
            if ((path == null) || (map.getGrid(unexplored[0], unexplored[1]).equals(Constant.UNEXPLORED))) {
                map.setGrid(unexplored[0], unexplored[1], Constant.OBSTACLE);
            }
            unexplored = unexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        // return to start point using fastest path
        if (!at_pos(robot, Constant.START)) {
            System.out.println("Phase 3");
            System.out.println(Arrays.toString(robot.getPosition()));
            fp.FastestPath(robot, null, Constant.START, speed, true, true);
        }

        stopwatch.stop();
        System.out.println("Exploration Complete!");
    }

    private void ImageRecognition_Exploration(Robot robot, int speed) {
        //robot.setDirection(1);
        int[][] checked_obstacles = {{0}};
        boolean unexplored = false;
        int[] need_take = null;
        int[] go_to = null;
        boolean move = false;

        //	explore whole arena
        do {
            checked_obstacles = move(robot, speed, checked_obstacles);
            System.out.println(Arrays.deepToString(checked_obstacles));
            //corner_calibration(robot);
        } while (!at_pos(robot, Constant.START));

        //corner_calibration(robot);
        if (!this.image_stop) {
            //returns false
        	this.image_stop = robot.captureImage(new int[][] {{-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1}});
        }

        if (!this.image_stop) {
            need_take = picture_taken(robot, robot.getPosition(), checked_obstacles);
            //System.out.println("DEBUGGING need_take1: "+ Arrays.toString(need_take));
            go_to = next_to_obstacle(robot, need_take);
        }

        if (go_to == null) {
            unexplored = true;
            need_take = null;
            go_to = unexplored(robot, robot.getPosition());
        }

        while ((go_to != null) && !(this.image_stop)) {
            // fastest path to nearest obstacle that photos have not been taken photo of
            System.out.println("Phase 2");
            int[] path = fp.FastestPath(robot, null, go_to, speed, true, true);
            if ((unexplored) && ((path == null) || (map.getGrid(go_to[0], go_to[1]).equals(Constant.UNEXPLORED)))) {
                map.setGrid(go_to[0], go_to[1], Constant.OBSTACLE);
                int[][] temp = new int[checked_obstacles.length + 1][3];
                System.arraycopy(checked_obstacles, 0, temp, 0, checked_obstacles.length);
                temp[checked_obstacles.length] = go_to;
                checked_obstacles = temp;
            } else {
                move = obstacle_on_left(robot, need_take);
            }

            if ((path != null) && move) {
                do {
                    checked_obstacles = move(robot, 1, checked_obstacles);
                    System.out.println(Arrays.deepToString(checked_obstacles));
                } while ((!at_pos(robot, go_to)) && !image_stop);
            }

            image_recognition(robot, checked_obstacles);

            unexplored = false;
            need_take = picture_taken(robot, robot.getPosition(), checked_obstacles);
            go_to = next_to_obstacle(robot, need_take);
            //System.out.println("DEBUGGING need_take2: "+ Arrays.toString(need_take));

            if (go_to == null) {
                unexplored = true;
                need_take = null;
                go_to = unexplored(robot, robot.getPosition());
            }

            if (go_to == null) {
                // to return to start after each "island"
                fp.FastestPath(robot, null, Constant.START, speed, true, true);
                //System.out.println("DEBUGGING Line 205");
                //corner_calibration(robot);
            } else {
                // to corner calibrate after each "island"
                fp.FastestPath(robot, null, nearest_corner(robot), speed, true, true);
                //System.out.println("DEBUGGING Line 210");
                //corner_calibration(robot);
                if (!this.image_stop) {
                    this.image_stop = robot.captureImage(new int[][] {{-1, -1, -1}, {-1, -1, -1}, {-1, -1, -1}});
                }
            }

            robot.updateMap();
        }

        go_to = unexplored(robot, robot.getPosition());
        while ((go_to != null) && this.image_stop) {
            // fastest path to nearest unexplored square
            System.out.println("Phase 3");
            System.out.println(Arrays.toString(robot.getPosition()));

            int[] path = fp.FastestPath(robot, null, go_to, speed, false, true);
            if ((path == null) || (map.getGrid(go_to[0], go_to[1]).equals(Constant.UNEXPLORED))) {
                map.setGrid(go_to[0], go_to[1], Constant.OBSTACLE);
            }

            go_to = unexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        if (!at_pos(robot, Constant.START)) {
            // fastest path to start point
            System.out.println("Phase 4");
            System.out.println(Arrays.toString(robot.getPosition()));
            fp.FastestPath(robot, null, Constant.START, speed, true, true);
        }

        System.out.println("Exploration Complete!");
    }

    private boolean[] crash(int[] isObstacle) {
        boolean[] crash = new boolean[6];
        for (int i=0; i<6; i++) {
            if (isObstacle[i] == 1) {
                crash[i] = true;
            } else {
                crash[i] = false;
            }
        }
        return crash;
    }

    // determines robot movement using left wall hugging
    private int[][] move(Robot robot, int speed, int[][] checked_obstacles) {
        System.out.println(Arrays.toString(robot.getPosition()));
        int[][] checked; // = checked_obstacles;
        robot.updateMap();

        if (!connection.ConnectionSocket.checkConnection()) {
	        try {
	            TimeUnit.MILLISECONDS.sleep(speed);
	        } catch (Exception e) {
            System.out.println(e.getMessage());
	        }
        }

        checked = leftWallHug(robot, checked_obstacles);

        System.out.println("MDF String P1: "+ robot.getMDFString()[0]);
        System.out.println("MDF String P2: "+ robot.getMDFString()[2]);

        return checked;
    }

    // left wall hugging
    private int[][] leftWallHug(Robot robot, int[][] checked_obstacles) {
        int[][] checked = checked_obstacles;

        if ((checked != null)&&(!left_wall(robot))) { // take photo of any obstacle
            checked = image_recognition(robot, checked);
        }

        if (check_left_empty(robot)) { // left empty
            robot.rotateLeft();
            storeStep(Constant.TURN_LEFT);
            if (check_front_empty(robot)) { // after left turn, front empty
                robot.forward(1);
                storeStep(Constant.GO_FORWARD);
                return checked;
            } else {                        // after left turn, front NOT empty
                robot.rotateRight();
                storeStep(Constant.TURN_RIGHT);
                if ((checked != null)&&(!left_wall(robot))) { // after right turn, left NOT wall
                    checked = image_recognition(robot, checked); // left obstacle found > take pic
                }
            }
        } else if ((checked != null)&&(!left_wall(robot))) { // left NOT empty and NOT wall
            checked = image_recognition(robot, checked); // left obstacle found > take pic
        }

        if (check_front_empty(robot)) { // left NOT empty, front is empty
            robot.forward(1);
            storeStep(Constant.GO_FORWARD);
            return checked;
        } else {                        // left NOT empty, front NOT empty
            robot.rotateRight();
            storeStep(Constant.TURN_RIGHT);
            if ((checked != null)&&(!left_wall(robot))) { // after right turn, left NOT wall
                checked = image_recognition(robot, checked); // left obstacle found > take pic
            }
        }

        if (check_front_empty(robot)) { // left NOT empty, front NOT empty, right empty
            robot.forward(1);
            storeStep(Constant.GO_FORWARD);
        } else {                        // left NOT empty, front NOT empty, right NOT empty
            System.out.println("Error during exploration phase 1. All 4 sides blocked.");
        }

        return checked;
    }

    // recover to a position suitable for resuming exploration
    private void recoveryMove(Robot robot, int speed, int[][] checked_obstacles) {
        System.out.println(Arrays.toString(robot.getPosition()));
        int[][] checked = checked_obstacles;
        robot.updateMap();

        if (!connection.ConnectionSocket.checkConnection()) {
            try {
                TimeUnit.MILLISECONDS.sleep(speed);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("!!! Robot is lost !!!\nRecovering to nearest obstacle/wall ...");

        if (check_front_empty(robot) && check_left_empty(robot)) {
            robot.forward(1);
        }else if(!check_front_empty(robot)) {
            System.out.println("Front wall found!");
            robot.rotateRight();
            storeStep("null|");
            looping = false;
        }
        else if(!check_left_empty(robot)){
            System.out.println("Left wall found!");
            storeStep("null|");
            looping = false;
        }
    }

    private void storeStep(String step) {
        last8Steps.remove(0);
        last8Steps.add(step);
        System.out.println("Last "+ last8Steps.size() +" Steps: " + getStepsTaken());
    }

    private String getStepsTaken() {
        String stepsTaken = "";
        for(String s:last8Steps)
            stepsTaken+=s;
        return stepsTaken;
    }

    private int[][] image_recognition(Robot robot, int[][] checked_obstacles) {
        /*try {
            TimeUnit.SECONDS.sleep(1);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }*/

        if (this.image_stop) {
            return checked_obstacles;
        }

        int x = robot.getPosition()[0];
        int y = robot.getPosition()[1];
        int[] default_pos = new int[] {-1, -1, -1}; // x , y and direction of the robot
        int[][] obs_pos = new int[][] {default_pos, default_pos, default_pos}; // op = {[xyz], [xyz], [xyz]}
        boolean take_pic = false;
        int direction = robot.getDirection();

        // Checking the 9x9 grid on the left of the robot based on where it is facing
        switch (direction) {
            case Constant.SOUTH:
                for (int i=0; i<3; i++) {
                    for (int j=0; j<3; j++) {
                        if (Arrays.equals(obs_pos[i], default_pos) && map.getGrid(x+2+j, y-1+i).equals(Constant.OBSTACLE)) {
                            // [8,1]
                            // [10,0], [11,0], [12,0], [10,1], [11,1], [12,1], [10,2], [11,2], [12,2]
                            obs_pos[i] = new int[] {x+2+j, y-1+i, Constant.SOUTH};
                        }
                    }
                }
                break;
            case Constant.NORTH:
                for (int i=0; i<3; i++) {
                    for (int j=0; j<3; j++) {
                        if (Arrays.equals(obs_pos[i], default_pos) &&
                                map.getGrid(x-2-j, y+1-i).equals(Constant.OBSTACLE)) {
                            obs_pos[i] = new int[] {x-2-j, y+1-i, Constant.NORTH};
                        }
                    }
                }
                break;
            case Constant.WEST:
                for (int i=0; i<3; i++) {
                    for (int j=0; j<3; j++) {
                        if (Arrays.equals(obs_pos[i], default_pos) &&
                                map.getGrid(x+1-i, y+2+j).equals(Constant.OBSTACLE)) {
                            obs_pos[i] = new int[] {x+1-i, y+2+j, Constant.WEST};
                        }
                    }
                }
                break;
            case Constant.EAST:
                for (int i=0; i<3; i++) {
                    for (int j=0; j<3; j++) {
                        if (Arrays.equals(obs_pos[i], default_pos) &&
                                map.getGrid(x-1+i, y-2-j).equals(Constant.OBSTACLE)) {
                            obs_pos[i] = new int[] {x-1+i, y-2-j, Constant.EAST};
                        }
                    }
                }
                break;
        }

        // Check if they are the default values or wall or checked obstacles
        for (int k=0; k<3; k++) {
            // [10,0], [11,0], [12,0]
            // [10,1], [11,1], [12,1]
            // [10,2], [11,2], [12,2]
            if (!within_map(obs_pos[k][0], obs_pos[k][1])) {
                obs_pos[k] = default_pos;
            } else {
                for (int[] obstacles : checked_obstacles) {
                    if (Arrays.equals(obstacles, obs_pos[k])) {
                        obs_pos[k] = default_pos;
                        break;
                    }
                }
            }
        }


        for (int m=0; m<3; m++) {
        	// There is an obstacle
            if (!(Arrays.equals(obs_pos[m], default_pos))) { // check for valid obstacle position
                if (check_front_empty(robot)) {
                    checked_obstacles[0][0] = m + 1; // update pos of obs relative to robot left
                }
                if ((checked_obstacles[0][0] < 2) || (!check_front_empty(robot))) {
                    take_pic = true;
                }
            }
        }

        if (take_pic) {
            for (int[] obs : obs_pos) {
                if (!(Arrays.equals(obs, default_pos))) {
                    int len = checked_obstacles.length;
                    int[][] temp = new int[len + 1][3];
                    System.arraycopy(checked_obstacles, 0, temp, 0, len);
                    temp[len] = obs;
                    checked_obstacles = temp;
                }
            }
            checked_obstacles[0][0] = 0;
            this.image_stop = robot.captureImage(obs_pos);
        }

        return checked_obstacles;
    }

    private boolean within_map(int x, int y) {
        return (x <= 19) && (x >= 0) && (y <= 14) && (y >= 0);
    }

    private boolean left_wall(Robot robot) {
        int direction = robot.getDirection();
        int[] pos = robot.getPosition();
        switch (direction) {
            case Constant.NORTH:
                return pos[0] == 1;
            case Constant.SOUTH:
                return pos[0] == 18;
            case Constant.EAST:
                return pos[1] == 1;
            case Constant.WEST:
                return pos[1] == 13;
            default:
                return true;
        }
    }

    private boolean check_left_empty(Robot robot) {
        boolean[] obstacles = crash(robot.updateMap());
        int[] pos = robot.getPosition();
        int direction = robot.getDirection();
        Map map = robot.getMap();

        switch (direction) {
            case Constant.EAST:
                pos[1] -= 2;
                break;
            case Constant.WEST:
                pos[1] += 2;
                break;
            case Constant.SOUTH:
                pos[0] += 2;
                break;
            case Constant.NORTH:
                pos[0] -= 2;
                break;
        }

        return (!obstacles[3]) && (!obstacles[4]) &&
        	(map.getGrid(pos[0], pos[1]).equals(Constant.EXPLORED) || map.getGrid(pos[0], pos[1]).equals(Constant.STARTPOINT)
        			|| map.getGrid(pos[0], pos[1]).equals(Constant.ENDPOINT));
    }

    public boolean check_front_empty(Robot robot){
        boolean[] obstacles = crash(robot.updateMap());
        return (!obstacles[0]) && (!obstacles[1]) && (!obstacles[2]);
    };

    private void corner_calibration(Robot robot) {
        int[] pos = robot.getPosition();
        if (!(((pos[0] == 1) || (pos[0] == 18)) && ((pos[1] == 1) || (pos[1] == 13)))) {
            return;
        }
        robot.updateMap();
        int direction = robot.getDirection();
        if ((pos[0] == 1) && (pos[1] == 13)) {
            switch (direction) {
                case Constant.NORTH:
                    //robot.rotateRight();
                    //robot.rotateRight();
                	robot.rotateLeft();
                    break;
                case Constant.EAST:
                    //robot.rotateRight();
                	robot.rotateLeft();
                	robot.rotateLeft();
                	break;
                case Constant.SOUTH:
                    robot.rotateRight();
                    break;
                default:
                    break;
            }
        } else if ((pos[0] == 18) && (pos[1] == 13)) {
            switch (direction) {
                case Constant.WEST:
                    //robot.rotateRight();
                    //robot.rotateRight();
                	robot.rotateLeft();
                    break;
                case Constant.NORTH:
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                case Constant.EAST:
                    robot.rotateRight();
                    break;
                default:
                    break;
            }
        } else if ((pos[0] == 18) && (pos[1] == 1)) {
            switch (direction) {
                case Constant.SOUTH:
                    //robot.rotateRight();
                    //robot.rotateRight();
                	robot.rotateLeft();
                    break;
                case Constant.WEST:
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                case Constant.NORTH:
                    robot.rotateRight();
                    break;
                default:
                    break;
            }
        } else if ((pos[0] == 1) && (pos[1] == 1)) {
            switch (direction) {
                case Constant.EAST:
                    //robot.rotateRight();
                    //robot.rotateRight();
                	robot.rotateLeft();
                	break;
                case Constant.SOUTH:
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                case Constant.WEST:
                	robot.rotateRight();
                    break;
                default:
                    break;
            }
        }
        robot.calibrate();
        int newdirection = robot.getDirection();

        switch(Math.abs(direction - newdirection + 4) % 4) {
        case 1:
        	robot.rotateRight();
        	break;
        case 2:
        	robot.rotateRight();
        	robot.rotateRight();
        	break;
        case 3:
        	robot.rotateLeft();
        	break;
        }
    }

    private int[] nearest_corner(Robot robot) {
        int[] pos = robot.getPosition();
        int[][] corners = new int[][] {{1,1}, {1, 13}, {18, 1}, {18, 13}};
        int[] costs = new int[4];
        int cheapest_index = 0;

        for (int i=0; i<4; i++) {
            boolean valid = true;
            int x = corners[i][0];
            int y = corners[i][1];
            Map map = robot.getMap();
            int[][] grid = new int[][] {{x-1, y-1}, {x, y-1}, {x+1, y-1}, {x-1, y},
                    {x, y}, {x+1, y}, {x-1, y+1}, {x, y+1}, {x+1, y+1}};
            for (int[] grids : grid) {
                if (!map.getGrid(grids[0], grids[1]).equals(Constant.EXPLORED)) {
                    valid = false;
                }
            }
            if (valid) {
                costs[i] = Math.abs(pos[0] - corners[i][0]) + Math.abs(pos[1] - corners[i][1]);
                if (costs[i] < costs[cheapest_index]) {
                    cheapest_index = i;
                }
            }
        }

        return corners[cheapest_index];
    }

    private boolean at_pos(Robot robot, int[] goal){
        int[] pos = robot.getPosition();
        return (Arrays.equals(pos, goal));
    };

    private int[] unexplored(Robot robot, int[] start) {
        Map map = robot.getMap();
        int lowest_cost = 9999;
        int[] cheapest_pos = null;
        for (int i=0; i<Constant.BOARDWIDTH; i++) {
            for (int j=0; j<Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i,j).equals(Constant.UNEXPLORED)) {
                    int cost = Math.abs(start[0] - i) + Math.abs(start[1] - j);
                    if (cost < lowest_cost) {
                        cheapest_pos = new int[] {i, j};
                        lowest_cost = cost;
                    }
                }
            }
        }
        return cheapest_pos;
    }

    private int[] next_to_obstacle(Robot robot, int[] next) {
        if (next == null) {
            return null;
        }

        int x = next[0];
        int y = next[1];
        int[][] order = new int[][] {{x-1, y-2}, {x, y-2}, {x+1, y-2}, {x+2, y-1}, {x+2, y},
                {x+2, y+1}, {x+1, y+2}, {x, y+2}, {x-1, y+2}, {x-2, y+1}, {x-2, y}, {x-2, y-1}};
        Map map = robot.getMap();

        for (int[] pos : order) {
            if ((within_map(pos[0], pos[1])) && (map.getGrid(pos[0], pos[1]).equals(Constant.EXPLORED))) {
                return pos;
            }
        }
        return null;
    }

    private int[] picture_taken(Robot robot, int[] start, int[][] checked_obstacles) {
        Map map = robot.getMap();
        int lowest_cost = 9999;
        int[] cheapest_pos = null;

        for (int i=0; i<Constant.BOARDWIDTH; i++) {
            for (int j=0; j<Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i,j).equals(Constant.OBSTACLE)) {
                    boolean not_inside = true;
                    for (int k=1; k<checked_obstacles.length; k++) {
                        int[] o_pos = {checked_obstacles[k][0], checked_obstacles[k][1]};
                        int[] cur = {i, j};
                        if (Arrays.equals(o_pos, cur)) {
                            not_inside = false;
                            break;
                        }
                    }
                    if (not_inside) {
                        int cost = Math.abs(start[0] - i) + Math.abs(start[1] - j);
                        if (cost < lowest_cost) {
                            cheapest_pos = new int[]{i, j};
                            lowest_cost = cost;
                        }
                    }
                }
            }
        }
        return cheapest_pos;
    }

    private boolean obstacle_on_left(Robot robot, int[] obstacle) {
        if (obstacle == null) {
            return false;
        }

        int direction = robot.getDirection();
        int[] pos = robot.getPosition();

        switch (direction) {
            case Constant.NORTH:
                if (obstacle[0] == (pos[0] - 2)) {
                    break;
                } else if (obstacle[1] == (pos[1] + 2)) {
                    robot.rotateRight();
                    break;
                } else if (obstacle[1] == (pos[1] - 2)) {
                    robot.rotateLeft();
                    break;
                } else {
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                }
            case Constant.SOUTH:
                if (obstacle[0] == (pos[0] + 2)) {
                    break;
                } else if (obstacle[1] == (pos[1] - 2)) {
                    robot.rotateRight();
                    break;
                } else if (obstacle[1] == (pos[1] + 2)) {
                    robot.rotateLeft();
                    break;
                } else {
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                }
            case Constant.EAST:
                if (obstacle[1] == (pos[1] + 2)) {
                    break;
                } else if (obstacle[0] == (pos[0] + 2)) {
                    robot.rotateRight();
                    break;
                } else if (obstacle[0] == (pos[0] - 2)) {
                    robot.rotateLeft();
                    break;
                } else {
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                }
            case Constant.WEST:
                if (obstacle[1] == (pos[1] - 2)) {
                    break;
                } else if (obstacle[0] == (pos[0] - 2)) {
                    robot.rotateRight();
                    break;
                } else if (obstacle[0] == (pos[0] + 2)) {
                    robot.rotateLeft();
                    break;
                } else {
                    robot.rotateRight();
                    robot.rotateRight();
                    break;
                }
        }
        return true;
    }

    private int percent_complete(Robot robot) {
        Map map = robot.getMap();
        int unexplored = 0;
        for (int i=0; i<Constant.BOARDWIDTH; i++) {
            for (int j=0; j<Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i,j).equals("Unexplored")) {
                    unexplored ++;
                }
            }
        }
        return ((300-unexplored)/3);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* Unused codes
    private void Normal_Exploration(Robot robot) {
        robot.setDirection(1);

        do {
            move(robot, 500, null);
//            corner_calibration(robot);
        } while (!at_pos(robot, Constant.START));

        int[] unexplored = unexplored(robot, robot.getPosition());

        while (unexplored != null) {
            // fastest path to nearest unexplored square
            System.out.println("Phase 2");
            int[] path = fp.FastestPath(robot, null, unexplored, 500, false, true);
            if ((path == null) || (map.getGrid(unexplored[0], unexplored[1]).equals(Constant.UNEXPLORED))) {
                map.setGrid(unexplored[0], unexplored[1], Constant.OBSTACLE);
            }

            unexplored = unexplored(robot, robot.getPosition());
            robot.updateMap();
        }

        if (!at_pos(robot, Constant.START)) {
            // fastest path to start point
            System.out.println("Phase 3");
            System.out.println(Arrays.toString(robot.getPosition()));
            fp.FastestPath(robot, null, Constant.START, 500, true, true);
        }
        System.out.println("Exploration Complete!");
    }

    private int[] furthest(Robot robot, int[][] checked_obstacles) {
        Map map = robot.getMap();
        int highest_cost = -1;
        int[] ex_pos = null;

        for (int i=0; i<Constant.BOARDWIDTH; i++) {
            for (int j=0; j<Constant.BOARDHEIGHT; j++) {
                if (map.getGrid(i,j).equals(Constant.OBSTACLE)) {
                    boolean not_inside = true;
                    for (int k=1; k<checked_obstacles.length; k++) {
                        int[] o_pos = {checked_obstacles[k][0], checked_obstacles[k][1]};
                        int[] cur = {i, j};
                        if (Arrays.equals(o_pos, cur)) {
                            not_inside = false;
                            break;
                        }
                    }
                    if (not_inside) {
                        int cost = Math.abs(Constant.END[0] - i) + Math.abs(Constant.END[1] - j);
                        if (cost > highest_cost) {
                            ex_pos = new int[]{i, j};
                            highest_cost = cost;
                        }
                    }
                }
            }
        }
        return ex_pos;
    }




    */
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}