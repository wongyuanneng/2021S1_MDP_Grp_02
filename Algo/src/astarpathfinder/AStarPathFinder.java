package astarpathfinder;

import exploration.Stopwatch;
import robot.Robot;
import map.Map;
import config.Constant;

import java.util.Arrays;

public class AStarPathFinder {
    boolean first = true;
    int direction = -1;
    boolean first_penalty = true;

    public int[] AStarPathFinder(Robot robot, int[] start_pos, int[] end_pos, boolean on_grid) {
        Node start = new Node(start_pos);
        Node curNode;

        Node[] frontier = {start};
        Node[] visited = {};

        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();

        while (true) {
            curNode = lowest_cost(frontier);

            if (curNode == null) {
                System.out.println("Error: open is empty");
                break;
            } else {
                frontier = remove_node(frontier, curNode);
                visited = add_node(visited, curNode);

                // path to goal found, exit loop
                if (((!on_grid) && (can_reach(curNode.pos, end_pos, first))) ||
                        ((on_grid) && (Arrays.equals(curNode.pos, end_pos)))) {
                    //robot.displayMessage("Path found in "+stopwatch.getElapsedTimeDouble()+"s!", 1);
                    System.out.println("Path found in "+stopwatch.getElapsedTimeDouble()+"s!");
                    break;
                }

                //  find more neighbours
                frontier = add_neighbours(robot, frontier, visited, curNode, end_pos);

                // frontier is empty
                if (Arrays.equals(frontier, new Node[]{}) || frontier.length==0) {
                    set_first(false);
                    System.out.println("Error: No possible path");
                    return null;
                }
            }
        }
        stopwatch.stop();
        int[] path = get_path(curNode);
        System.out.println(Arrays.toString(path));
        update_direction(path);
        System.out.println("Path Found");
        return path;
    }

    private void update_direction (int[] path) {
        if (path != null) {
            for (int value : path) {
                switch (value) {
                    case Constant.LEFT:
                        direction = (direction + 3) % 4;
                        break;
                    case Constant.RIGHT:
                        direction = (direction + 1) % 4;
                        break;
                    case Constant.BACKWARD:
                        direction = (direction + 2) % 4;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private boolean can_reach(int[] cur, int[] end, boolean first) {
        int x = end[0];
        int y = end[1];
        int[][] pos;

        if (first) {
            pos = new int[][] {{x-1, y-2}, {x, y-2}, {x+1, y-2}, {x+2, y-1}, {x+2, y}, {x+2, y+1},
                                 {x+1, y+2}, {x, y+2}, {x-1, y+2}, {x-2, y+1}, {x-2, y}, {x-2, y-1}};
        } else {
            pos = new int[][] {{x-1, y-3}, {x, y-3}, {x+1, y-3}, {x+3, y-1}, {x+3, y}, {x+3, y+1},
                               {x+1, y+3}, {x, y+3}, {x-1, y+3}, {x-3, y+1}, {x-3, y}, {x-3, y-1}};
        }

        for (int[] coordinates : pos) {
            if (Arrays.equals(cur, coordinates)) {
                return true;
            }
        }
        return false;
    }

    private String[] print(Node[] list) {
        String[] new_list = new String[list.length];

        if (list.length < 1) {
            return new String[] {};
        }

        for (int i=0; i<list.length; i++) {
            int[] pos = list[i].pos;
            new_list[i] = Arrays.toString(pos);
        }

        return new_list;
    }

    private Node lowest_cost(Node[] list) {
        int cost;

        if (list.length > 0) {
            int l_cost = list[0].cost;
            Node l_node = list[0];

            for (int i = 0; i < list.length; i++) {
                cost = list[i].cost;
                if (cost <= l_cost) {
                    l_cost = cost;
                    l_node = list[i];
                }
            }

            return l_node;
        } else {
            return null;
        }
    }

    private Node[] remove_node(Node[] list, Node node) {
        int index=-1;

        if (list.length < 2) {
            return new Node[] {};
        }

        Node[] new_list = new Node[list.length-1];

        for (int i=0; i<list.length; i++) {
            if (Arrays.equals(list[i].pos, node.pos)) {
                index = i;
                break;
            }
        }

        if (index>-1) {
            System.arraycopy(list, 0, new_list, 0, index);
            for (int j = index; j < new_list.length; j++) {
                new_list[j] = list[j + 1];
            }
        }

        return new_list;
    }

    private Node[] add_node(Node[] list, Node node) {
        Node[] new_list = new Node[list.length+1];

        System.arraycopy(list, 0, new_list, 0, list.length);
        new_list[list.length] = node;

        return new_list;
    }

    private Node[] add_neighbours(Robot robot, Node[] frontier, Node[] visited, Node curNode, int[] end_pos) {
        Node[] neighbours = new Node[4];
        int count = 0;
        int x = curNode.pos[0];
        int y = curNode.pos[1];
        int[][] neighbours_pos = {{x,y+1}, {x-1,y}, {x+1,y}, {x,y-1}};

        for (int i=0; i<4; i++) {
            if (is_valid(robot, neighbours_pos[i])) {
                // add neighbours (must be a valid grid to move to)
                Node neighbour = new Node(neighbours_pos[i]);
                neighbour.parent = curNode;
                neighbour.cost = find_cost(neighbour, end_pos, robot);
                neighbours[count] = neighbour;
                count++;
            }
        }

        for (int j=0; j<count; j++) {
            Node node = neighbours[j];
            if (find_index(node, visited) == -1) { // if NOT in visited
                int index = find_index(node, frontier);
                if ((index > -1) && (node.cost < frontier[index].cost)) { // if in frontier n less cost
                    frontier[index] = node;
                }
                if (index == -1) { // if NOT in frontier
                    frontier = add_node(frontier, node);
                }
            }
        }

        return frontier;
    }

    public boolean is_valid(Robot robot, int[] pos){
        if (pos == null) {
            return false;
        }

        Map map = robot.getMap();
        int x = pos[0];
        int y = pos[1];
        int[][] robot_pos = {{x-1, y+1}, {x, y+1}, {x+1, y+1}, {x-1, y},
                {x, y}, {x+1, y}, {x-1, y-1}, {x, y-1}, {x+1, y-1}};

        if ((0<x)&&(x<19)&&(0<y)&&(y<14)) { // within boundaries
            for (int[] coordinates : robot_pos) {
                if (map.getGrid(coordinates[0], coordinates[1]).equals("Obstacle")) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }

    private int find_cost (Node node, int[] end_pos, Robot robot) {
        node.h_cost = find_h_cost(node, end_pos);
        node.g_cost = find_g_cost(node);
        node.update_cost();
        return node.cost;
    }

    private int find_h_cost(Node cur, int[] end) {
        int x = Math.abs(cur.pos[0] - end[0]);
        int y = Math.abs(cur.pos[1] - end [1]);

        if ((cur.parent.pos[0] == cur.pos[0])&&(x==0)) {
            return y;
        }
        else if ((cur.parent.pos[1] == cur.pos[1])&&(y==0)) {
            return x;
        }
        else {
            return (x + y );
        }
    }

    private int find_g_cost(Node cur) {
        Node prev = cur.parent;

        if (prev == null) {
            // cur node is the start
            return 0;
        } else if ((!first_penalty) && (prev.parent == null)) {
            return prev.g_cost + 1;
        } else {
            int direction = go_where(cur);

            if (direction == Constant.FORWARD) {
                return prev.g_cost + 1;
            } else if ((direction == Constant.LEFT) || (direction == Constant.RIGHT)) {
                return prev.g_cost + 3;
            } else {
                return prev.g_cost + 5;
            }
        }
    }

    private int go_where(Node cur) {
        Node second = cur.parent;
        if (second == null) {
            // start
            return -1;
        }
        Node first = second.parent;
        if (first == null) { // This is only 1 grid away from the start
//            int direction = robot.getDirection();

            if (second.pos[0] == cur.pos[0]) {
                if (second.pos[1] > cur.pos[1]) {
                    if (direction == Constant.NORTH) {
                        return Constant.FORWARD;
                    }
                    else if (direction == Constant.EAST) {
                        return Constant.LEFT;
                    }
                    else if (direction == Constant.SOUTH) {
                        return Constant.BACKWARD;
                    }
                    else if (direction == Constant.WEST) {
                        return Constant.RIGHT;
                    }
                }
                else if (second.pos[1] < cur.pos[1]) {
                    if (direction == Constant.NORTH) {
                        return Constant.BACKWARD;
                    }
                    else if (direction == Constant.EAST) {
                        return Constant.RIGHT;
                    }
                    else if (direction == Constant.SOUTH) {
                        return Constant.FORWARD;
                    }
                    else if (direction == Constant.WEST) {
                        return Constant.LEFT;
                    }
                }
            }
            else if (second.pos[1] == cur.pos[1]) {
                if (second.pos[0] > cur.pos[0]) {
                    if (direction == Constant.NORTH) {
                        return Constant.LEFT;
                    }
                    else if (direction == Constant.EAST) {
                        return Constant.BACKWARD;
                    }
                    else if (direction == Constant.SOUTH) {
                        return Constant.RIGHT;
                    }
                    else if (direction == Constant.WEST) {
                        return Constant.FORWARD;
                    }
                } else if (second.pos[0] < cur.pos[0]) {
                    if (direction == Constant.NORTH) {
                        return Constant.RIGHT;
                    }
                    else if (direction == Constant.EAST) {
                        return Constant.FORWARD;
                    }
                    else if (direction == Constant.SOUTH) {
                        return Constant.LEFT;
                    }
                    else if (direction == Constant.WEST) {
                        return Constant.BACKWARD;
                    }
                }
            }
        }
        else {
            if ((first.pos[0] == second.pos[0])&&(second.pos[0]==cur.pos[0])) {
                if (((first.pos[1] > second.pos[1])&&(second.pos[1] > cur.pos[1])) ||
                        ((first.pos[1] < second.pos[1])&&(second.pos[1] < cur.pos[1]))){
                    return Constant.FORWARD;
                }
                else {
                    return Constant.BACKWARD;
                }
            }
            else if ((first.pos[1] == second.pos[1])&&(second.pos[1]==cur.pos[1])) {
                if (((first.pos[0] > second.pos[0])&&(second.pos[0] > cur.pos[0])) ||
                        ((first.pos[0] < second.pos[0])&&(second.pos[0] < cur.pos[0]))){
                    return Constant.FORWARD;
                }
                else {
                    return Constant.BACKWARD;
                }
            }
            else if (first.pos[0] == second.pos[0]) {
                if (first.pos[1] < second.pos[1]) {
                    if (second.pos[0] < cur.pos[0]) {
                        return Constant.LEFT;
                    } else {
                        return Constant.RIGHT;
                    }
                } else {
                    if (second.pos[0] > cur.pos[0]) {
                        return Constant.LEFT;
                    } else {
                        return Constant.RIGHT;
                    }
                }
            } else {
                if (first.pos[0] < second.pos[0]) {
                    if (second.pos[1] > cur.pos[1]) {
                        return Constant.LEFT;
                    } else {
                        return Constant.RIGHT;
                    }
                } else {
                    if (second.pos[1] < cur.pos[1]) {
                        return Constant.LEFT;
                    } else {
                        return Constant.RIGHT;
                    }
                }
            }
        }
        return -2; // error
    }

    private int find_index(Node node, Node[] list) {
        if (list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                if (Arrays.equals(list[i].pos, node.pos)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int[] get_path(Node node) {
        int[] path = {go_where(node)};
        Node cur = node.parent;

        if (cur == null) {
            return null;
        }

        while (cur.parent != null) {
            if (go_where(cur) >= 0) {
                int[] temp_path = new int[path.length + 1];
                System.arraycopy(path, 0, temp_path, 1, path.length);
                temp_path[0] = go_where(cur);
                path = temp_path;
                cur = cur.parent;
            }
        }

        return path;
    }

    public void set_direction(int direction) {
        this.direction = direction;
    }

    public void set_first(boolean first) {
        this.first = first;
    }

    public void set_first_turn_penalty(boolean first_penalty) {
        this.first_penalty = first_penalty;
    }
}