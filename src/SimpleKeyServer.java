import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.*;
import java.lang.Thread;


public class SimpleKeyServer
{

	static final int FORWARD = 0;
	static final int BACKWARD = 1;
	static final int LEFT = 2;
	static final int RIGHT = 3;
	static final int JUMP = 4;
	static final int CROUCH = 5;

	static int MAX_KEY_TYPES = 6;
	static Robot robot;
	static boolean downKey = false;
	static int[] keyEvents = new int[MAX_KEY_TYPES];
	static int[] keyDelays = new int[MAX_KEY_TYPES];
	static Thread[] mThreads = new Thread[MAX_KEY_TYPES];
	static int[] keyCounts = new int[MAX_KEY_TYPES];

	public static void main(String args[]) throws Exception
	{
		robot = new Robot();
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receiveData = new byte[1024];
		byte[] emptyData = new byte[1024];

		keyEvents[FORWARD] = KeyEvent.VK_W;
		keyEvents[BACKWARD] = KeyEvent.VK_S;
		keyEvents[LEFT] = KeyEvent.VK_A;
		keyEvents[RIGHT] = KeyEvent.VK_D;
		keyEvents[JUMP] = KeyEvent.VK_SPACE;
		keyEvents[CROUCH] = KeyEvent.VK_CONTROL;
		for(int i=0;i<MAX_KEY_TYPES;i++) keyDelays[i]=300;
		keyDelays[FORWARD]=500;
		while(true)
		{
			System.arraycopy(emptyData, 0, receiveData, 0, 1024);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
 			String sentence = new String(receiveData, "UTF-8");
			System.out.println("Got sent"+sentence);
			if (receivePacket.getLength() > 2) {
				int i=0;
				for(i=0;i<1024;i++) {
					if (receiveData[i]==0) {
						break;
					}
				}
				sentence = sentence.substring(0,i);
				SimpleKeyServer.log("Log: "+sentence);
				continue;
			}

			char ch = sentence.charAt(0);
			SimpleKeyServer.log("RECEIVED: "+ch);
			if (ch=='2') {
				SimpleKeyServer.doMousePress(0,InputEvent.BUTTON1_MASK);
			}
			if (ch=='3') {
				// Do space bar
				SimpleKeyServer.doKeyPress(0,KeyEvent.VK_SPACE);
			}
			if (ch=='4') {
				SimpleKeyServer.doKeyPress(0,KeyEvent.VK_1);
			}
			if (ch=='5') {
				SimpleKeyServer.doKeyPress(0,KeyEvent.VK_2);
			}

			if (ch=='w') { // || ch=='s' || ch=='a' || ch=='d' || ch =='j') {
				int thread;
				switch(ch) {
				case 'w' : {
					thread = FORWARD;
					
					break;
				}
				case 's' : {
					thread = BACKWARD;
					break;
				}
				case 'a' : {
					thread = LEFT;
					break;
				}
				case 'd' : {
					thread = RIGHT;
					break;
				}
				case 'j' : {
					thread = JUMP;
					break;
				}
				case 'c' : {
					thread = CROUCH;
					break;
				}
				default: thread = FORWARD;		  
				}

				synchronized (SimpleKeyServer.class) {
					SimpleKeyServer.log("mThreads: thread "+thread+" mThreads[thread] "+mThreads[thread]);
					//if down thread exists, kill it
					if (mThreads[thread] != null && keyCounts[thread] > 0) {
						mThreads[thread].interrupt();
						
						mThreads[thread] = new Thread(new KeyRun(false, thread));
						SimpleKeyServer.log("Running new thread "+mThreads[thread]);
						mThreads[thread].start();
						continue;
					}
					
					//Press the key in thread
					//if down thread exists, kill it

					//start the down thread
					Thread down = new Thread(new KeyRun(true, thread));
					down.start();

					mThreads[thread] = new Thread(new KeyRun(false, thread));
					SimpleKeyServer.log("No thread, Running new thread "+mThreads[thread]);
					mThreads[thread].start();
				}
			}
		}
	}

	public static void doKeyPress(int sleepp, int keyCodep) {
		final int sleep = sleepp;
		final int keyCode = keyCodep;
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					robot.setAutoDelay(40);
					robot.setAutoWaitForIdle(true);
					if (sleep > 0) {
						Thread.sleep(sleep);
					}
					robot.keyPress(keyCode);
					robot.delay(10);
					
					robot.keyRelease(keyCode);
				} catch(Exception E) {}
			}
		});
		t.start();
	}

	static final void log(String l) {
		System.out.println(System.currentTimeMillis()+l);
	}
	
	public static void doMousePress(int sleepp, int keyCodep) {
		final int sleep = sleepp;
		final int keyCode = keyCodep;
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					robot.setAutoDelay(40);
					robot.setAutoWaitForIdle(true);
					if (sleep > 0) {
						Thread.sleep(sleep);
					}
					robot.mousePress(keyCode);
					robot.delay(50);
					robot.mouseRelease(keyCode);
					robot.delay(50);
				} catch(Exception E) {}
			}
		});
		t.start();
	}


	public SimpleKeyServer(boolean up) throws AWTException
	{
		//	java.awt.Toolkit.getDefaultToolkit().beep();
	}

	@SuppressWarnings("unused")
	private void leftClick()
	{
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.delay(200);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
		robot.delay(200);
	}


	@SuppressWarnings("unused")
	private void type(int i)
	{
		robot.delay(40);
		robot.keyPress(i);
		robot.keyRelease(i);
	}

	@SuppressWarnings("unused")
	private void type(String s)
	{
		byte[] bytes = s.getBytes();
		for (byte b : bytes)
		{
			int code = b;
			// keycode only handles [A-Z] (which is ASCII decimal [65-90])
			if (code > 96 && code < 123) code = code - 32;
			robot.delay(40);
			robot.keyPress(code);
			robot.keyRelease(code);
		}
	}
}

class KeyRun implements Runnable {

	private boolean keyDown = false;
	private int keyType = 0;

	public KeyRun(boolean keyDownp, int keyTypep) {
		keyDown = keyDownp;
		keyType = keyTypep;
	}
	@Override
	public void run() {
		try {
		if (keyDown) {
			//SimpleKeyServer.robot.setAutoDelay(40);
			//SimpleKeyServer.robot.setAutoWaitForIdle(true);
			synchronized (SimpleKeyServer.class) {
			SimpleKeyServer.robot.keyPress(SimpleKeyServer.keyEvents[keyType]);
			SimpleKeyServer.keyCounts[keyType]++;
			SimpleKeyServer.log("KeyCounts["+keyType+"]="+SimpleKeyServer.keyCounts[keyType]);
			}
		} else {
			try {
				System.out.println("Start sleep");
				Thread.sleep(SimpleKeyServer.keyDelays[keyType]);
				System.out.println("Done sleep");
				synchronized (SimpleKeyServer.class) {
				//	java.awt.Toolkit.getDefaultToolkit().beep();
					SimpleKeyServer.robot.keyRelease(SimpleKeyServer.keyEvents[keyType]);
					SimpleKeyServer.mThreads[keyType] = null;
					if (SimpleKeyServer.keyCounts[keyType]==0) {
						SimpleKeyServer.log("Race Condition: "+Thread.currentThread()+" keyType:"+keyType);
						//System.exit(1);
					}
					SimpleKeyServer.log("Reducing keyCount on : "+Thread.currentThread()+" for keyType:"+keyType);
					SimpleKeyServer.keyCounts[keyType]--;
				}
			} catch(Exception E) {
				SimpleKeyServer.log("Inner KeyRun keyDown:"+keyDown+" keyType:"+keyType);
			}

		}
		} catch(Exception E) {
			SimpleKeyServer.log("KeyRun keyDown:"+keyDown+" keyType:"+keyType);
			E.printStackTrace();
		}
		// TODO Auto-generated method stub
	}

}