import java.lang.*;

public class CheckTimeout {
	protected long startTime;
	protected long duration;

	public CheckTimeout(){
		startTime = System.currentTimeMillis(); // in miliseconds!!!
		duration = 0;
	}

	public CheckTimeout(long duration){
		startTime = System.currentTimeMillis(); // in miliseconds!!!
		this.duration = duration;
	}

	public boolean isTimeout(){
		if ((System.currentTimeMillis() - startTime) >= duration){
			return true;
		} else {
			return false;
		}
	}

	public long getStartTime(){
		return startTime;
	}

	public long getDuration(){
		return duration;
	}

	public void resetStartTime(){
		startTime = System.currentTimeMillis(); // in miliseconds!!!
	}

	public void resetDuration(long duration){
		this.duration = duration;
	}

	/*public static void main(String args[]){
		CheckTimeout c1 = new CheckTimeout(3000);
		while(!c1.isTimeout()){}
		System.out.println("Time is up!!!");
	}*/


}