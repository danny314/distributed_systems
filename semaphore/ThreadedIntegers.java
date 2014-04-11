import java.util.concurrent.Semaphore;


public class ThreadedIntegers {
	
	Semaphore sem = new Semaphore(1,true);
	static Integer i = 1;
	
	public static void main(String[] args) {
		ThreadedIntegers ti = new ThreadedIntegers();
		
		new IntegerCounter(ti.sem,false).start();
		new IntegerCounter(ti.sem,true).start();
	}
	
	public static synchronized Integer getI() {
		return i;
	}
	public static synchronized void incrementI() {
		i = i + 1;
	}
	
}

class IntegerCounter extends Thread {
	Semaphore sem;
	boolean isEven;
	public IntegerCounter(Semaphore sem, boolean isEven) {
		this.sem = sem;
		this.isEven = isEven;
		setName(isEven ? "Even thread" : "Odd thread ");
	}
	public void run() {
		while (ThreadedIntegers.getI() < 101 ) {
			if (isEven ? ThreadedIntegers.getI() % 2 == 0 : ThreadedIntegers.getI() % 2 != 0) {
				try {
					sem.acquire();
					if (ThreadedIntegers.getI() < 101) {
						System.out.println(getName() + " " + ThreadedIntegers.getI());
					}
					ThreadedIntegers.incrementI();	
					sem.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}

