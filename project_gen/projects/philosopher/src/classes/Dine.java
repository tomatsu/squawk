public class Dine{
	public static void main(String[] args){

		int x=10;

		Log.msg(String.valueOf(x));
 
		Chopstick[] chopistics = new Chopstick[5];

		//initlize the chopistics
		for(int i=0; i< chopistics.length; i++){
			chopistics[i] = new Chopstick("C: "+i);
		}
		Philosopher[] philosophers = new Philosopher[5];
		//for(i=0; i<philosophers.length; i++){
		philosophers[0] = new Philosopher("P: 0 - ", chopistics[0], chopistics[1]);
		philosophers[1] = new Philosopher("P: 1 - ", chopistics[1], chopistics[2]);
		philosophers[2] = new Philosopher("P: 2 - ", chopistics[2], chopistics[3]);
		philosophers[3] = new Philosopher("P: 3 - ", chopistics[3], chopistics[4]);
		philosophers[4] = new Philosopher("P: 4 - ", chopistics[4], chopistics[0]);

		for(int i=0;i<philosophers.length;i++){
			Log.msg("Thred "+ i);
			Thread t= new Thread( philosophers[i]);
			t.start();
		}
 	}
}

// State : 2 = Eat, 1 = think
class Philosopher extends Thread
{
	private Chopstick _leftChopistick;
	private Chopstick _rightChopistick;

	private String _name;
	private int _state;

	public Philosopher ( String name, Chopstick _left, Chopstick _right){
		this._state = 1;
		this._name = name;
		_leftChopistick = _left;
		_rightChopistick = _right;
	}
 
	public void eat()
	{
		if(! _leftChopistick.used){
			if(!_rightChopistick.used){
				_leftChopistick.take();
				_leftChopistick.take();

				Log.msg(_name + " : Eat");
				
				Log.Delay(1000);

				_leftChopistick.release();
		 		_rightChopistick.release();
			}
		}		
		think();
	}

	public void think(){
		 	this._state = 1;
		 	Log.msg(_name + " : Think");
		 	Log.Delay(1000);
		
	}

	public void run(){
		for(int i=0; i<=10; i++){
			eat();
		}
	}
}

class Log{

	public static void msg(String msg){
		System.out.println(msg);
	}
	public static void Delay(int ms){
		try{
			Thread.sleep(ms);
		}
		catch(InterruptedException ex){ }
	}
}

class Chopstick{

	public boolean used;
	public String _name;

	public Chopstick(String _name){
		this._name = _name;
	}

	public synchronized void take() {
		Log.msg ("Used :: " + _name );
		this.used = true;
	}
	public synchronized void release() {
		Log.msg ("Released :: " + _name );
		this.used = false ;
	}
}
