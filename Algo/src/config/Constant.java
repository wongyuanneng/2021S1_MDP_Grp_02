package config;
	
public class Constant {
	// Actual run constraints
	public static final int TIME = -1;
	public static final int PERCENTAGE = 100;
	public static final int SPEED = 1;
	public static final boolean IMAGE_REC = false;

	// Used for all Real and Simulator Events
	public static final int BOARDWIDTH = 20; // By default, this should be 20. This must be greater than 3 as the Robot assumes to take 3x3 grid.
	public static final int BOARDHEIGHT = 15; // By default, this should be 15. This must be greater than 3 as the Robot assumes to take 3x3 grid.
	public static final int STARTPOINTHEIGHT = 3;
	public static final int STARTPOINTWIDTH = 3;
	public static final int ENDPOINTHEIGHT = 3;
	public static final int ENDPOINTWIDTH = 3;
	
	// You need to define your own range for your sensors
	static int front_offset = 0;
	static int left_offset = 0;
	static int right_offset = 0;

	// sensorValue will have this: FR, FC, FL, LB, LF, RF
	public static final double[][] SENSOR_RANGES = {{7.98+front_offset, 20.22+front_offset}, // FR
													{9+front_offset, 20+front_offset}, // FC
													{7.98+front_offset, 20.7+front_offset}, // FL

													{9+left_offset, 23.92+left_offset}, // LB
													{9+left_offset, 23.63+left_offset}, // LF

													{7.98+right_offset, 20+right_offset, 30+right_offset, 40+right_offset, 50+right_offset, 58+right_offset}};
													//15+right_offset, 23+right_offset, 33+right_offset, 43+right_offset, 53+right_offset, 63+right_offset}};


	/*public static final double[][] SENSOR_RANGES = {{13.34+front_offset, 24.22+front_offset}, // FR
													{13.6+front_offset, 25+front_offset}, // FC
													{13+front_offset, 24.7+front_offset}, // FL

													{13.7+left_offset, 23.92+left_offset}, // LB
													{13.55+left_offset, 23.63+left_offset}, // LF

													{15+right_offset, 20+right_offset, 30+right_offset, 45+right_offset, 55+right_offset, 65+right_offset}};
*/
	// Sensor constants used only in Simulator
	public static final int SHORTSENSORMAXRANGE = 3; // This is in number of grid.
	public static final int SHORTSENSOROFFSET = 2; // This is in cm.
	public static final int FARSENSORMAXRANGE = 7; // This is in number of grid.
	public static final int FARSENSOROFFSET = 10; // This is in cm.

	public static final int[][] SENSORDIRECTION = new int [][]{	{0, -1},	//N
																{1, 0},		//E	
																{0, 1},		//S
																{-1, 0}};	//W


	// For Instructing Robot
	public static final int LEFT = 0;
	public static final int FORWARD = 1;
	public static final int RIGHT = 2;
	public static final int BACKWARD = 3;


	// For Timertask in UI Simulator
	public static final int DELAY = 15;


	// For Generating Random Map for Simulator. However, it does not guarantee a maneuverable map.
	public static final boolean RANDOMMAP = false;
	public static final int MAXOBSTACLECOUNT = 16;

	// For UI Simulator Display
	public static final int GRIDHEIGHT = 40;
	public static final int GRIDWIDTH = 40;
	public static final int MARGINLEFT = 100;
	public static final int MARGINTOP = 100;
	public static final String SIMULATORICONIMAGEPATH = ".\\images\\simulator_icon.ico";


	// Used in Map and possibly used in real run and simulator
	public static final String[] POSSIBLEGRIDLABELS = new String[]{"Unexplored", "Explored", "Obstacle", "Waypoint", "Startpoint", "Endpoint"};
	public static final String UNEXPLORED = POSSIBLEGRIDLABELS[0];
	public static final String EXPLORED = POSSIBLEGRIDLABELS[1];
	public static final String OBSTACLE = POSSIBLEGRIDLABELS[2];
	public static final String WAYPOINT = POSSIBLEGRIDLABELS[3];
	public static final String STARTPOINT = POSSIBLEGRIDLABELS[4];
	public static final String ENDPOINT = POSSIBLEGRIDLABELS[5];

	// Map directions
	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public static final int[] START = {1,1};
	public static final int[] END = {18,13};


	// Image path for UI Simulator
	public static final String robotExt = ".gif";
	public static final String UNEXPLOREDIMAGEPATH = ".\\images\\unexplored_grid.png";
	public static final String EXPLOREDIMAGEPATH = ".\\images\\explored_grid.png";
	public static final String OBSTACLEIMAGEPATH = ".\\images\\obstacle_grid.png";
	public static final String WAYPOINTIMAGEPATH = ".\\images\\waypoint_grid.png";
	public static final String STARTPOINTIMAGEPATH = ".\\images\\start_grid.png";
	public static final String ENDPOINTIMAGEPATH = ".\\images\\end_grid.png";
	public static final String ROBOTIMAGEPATH = ".\\images\\robot"+robotExt;
	public static final String ROBOTNIMAGEPATH = ".\\images\\robotN"+robotExt;
	public static final String ROBOTEIMAGEPATH = ".\\images\\robotE"+robotExt;
	public static final String ROBOTSIMAGEPATH = ".\\images\\robotS"+robotExt;
	public static final String ROBOTWIMAGEPATH = ".\\images\\robotW"+robotExt;
	public static final String DIALOGICONIMAGEPATH = ".\\images\\letter-r.png";


	public static final String[] GRIDIMAGEPATH = new String[]{UNEXPLOREDIMAGEPATH,
			EXPLOREDIMAGEPATH,
			OBSTACLEIMAGEPATH,
			WAYPOINTIMAGEPATH,
			STARTPOINTIMAGEPATH,
			ENDPOINTIMAGEPATH};


	public static final String[] ROBOTIMAGEPATHS = new String[] {ROBOTNIMAGEPATH,
			ROBOTEIMAGEPATH,
			ROBOTSIMAGEPATH,
			ROBOTWIMAGEPATH};


	// Avoid changing these values below
	public static final int ROBOTHEIGHT = 100; // By default, this should be twice of the grid height. GRIDHEIGHT * 2
	public static final int ROBOTWIDTH = 100; // By default, this should be twice of the grid width. GRIDWIDTH * 2
	public static final int HEIGHT = BOARDHEIGHT * GRIDHEIGHT + MARGINTOP;
	public static final int WIDTH = BOARDWIDTH * GRIDWIDTH + MARGINLEFT;

	// Connection Constants
	//public static final String IP_ADDRESS = "127.0.0.1";	// Self Conn
    public static final String IP_ADDRESS = "192.168.2.2";	// RPi Conn
	public static final int PORT = 8080;
	public static final int BUFFER_SIZE = 512;

	public static final String START_EXPLORATION = "ES|";
	public static final String FASTEST_PATH = "FS|";
	public static final String IMAGE_STOP = "I";
	public static final String SEND_ARENA = "SendArena";
	public static final String INITIALISING = "starting";
	public static final String SETWAYPOINT = "waypoint";
	public static final String SENSE_ALL = "Z|";
	public static final String GO_FORWARD = "W|";
	public static final String TURN_LEFT = "A|";
	public static final String TURN_RIGHT = "D|";
	public static final String CALIBRATE = "L|";
	public static final String LEFTALIGN = "B|";
	public static final String END_TOUR = "N";

	// Connection Acknowledge
	public static final String IMAGE_ACK = "D";
}
