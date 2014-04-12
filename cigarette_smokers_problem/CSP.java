import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;

/**
 *@author Puneet Bansal
 */
public class CSP {
	// agent
	static Semaphore agentSem = new Semaphore(1);
	static Semaphore tobacco = new Semaphore(0);
	static Semaphore paper = new Semaphore(0);
	static Semaphore matches = new Semaphore(0);

	// Pusher
	static Semaphore tobaccoSem = new Semaphore(0);
	static Semaphore matchSem = new Semaphore(0);
	static Semaphore paperSem = new Semaphore(0);

	// Ingredients on table
	static Boolean isTobacco = Boolean.FALSE;
	static Boolean isMatch = Boolean.FALSE;
	static Boolean isPaper = Boolean.FALSE;

	static Semaphore mutex = new Semaphore(1);

	static class agentA implements Runnable {
		public void run() {
			while (true) {
				try {
					agentSem.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("agentA about to release tobacco and paper");
				tobacco.release();
				paper.release();
			}
		}
	}

	static class agentB implements Runnable {
		public void run() {
			while (true) {
				try {
					agentSem.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out
						.println("agentB about to release tobacco and matches");
				tobacco.release();
				matches.release();
			}
		}
	}

	static class agentC implements Runnable {
		public void run() {
			while (true) {
				try {
					agentSem.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("agentC about to release paper and matches");
				paper.release();
				matches.release();
			}
		}
	}

	static class smokerMatches implements Runnable {
		public void run() {
			while (true) {
				try {
					matchSem.acquire();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("smoker with matches ready");
				agentSem.release();
			}
		}
	}

	static class smokerTobacco implements Runnable {
		public void run() {
			while (true) {
				try {
					tobaccoSem.acquire();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("smoker with tobacco ready");
				agentSem.release();
			}
		}
	}

	static class smokerPaper implements Runnable {
		public void run() {
			while (true) {
				try {
					paperSem.acquire();
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("smoker with paper ready");
				agentSem.release();
			}
		}
	}

	static class TobaccoPusher implements Runnable {
		public void run() {
			while (true) {
				try {
					tobacco.acquire();
					mutex.acquire();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (isPaper) {
					isPaper = false;
					matchSem.release();
				} else if (isMatch) {
					isMatch = false;
					paperSem.release();
				} else {
					isTobacco = true;
				}
				mutex.release();	

			}
		}
	}

	static class MatchesPusher implements Runnable {
		public void run() {
			while (true) {
				try {
					matches.acquire();
					mutex.acquire();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (isPaper) {
					isPaper = false;
					tobaccoSem.release();
				} else if (isTobacco) {
					isTobacco = false;
					paperSem.release();
				} else {
					isMatch = true;
				}
				mutex.release();	
			}
		}
	}

	static class PaperPusher implements Runnable {
		public void run() {
			while (true) {
				try {
					paper.acquire();
					mutex.acquire();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (isMatch) {
					isMatch = false;
					tobaccoSem.release();
				} else if (isTobacco) {
					isTobacco = false;
					matchSem.release();
				} else {
					isPaper = true;
				}
				mutex.release();	
			}
		}
	}

	public static void main(String[] args) {
		List<Thread> tList = new ArrayList<Thread>();
		tList.add(new Thread(new CSP.agentA()));
		tList.add(new Thread(new CSP.agentB()));
		tList.add(new Thread(new CSP.agentC()));
		tList.add(new Thread(new CSP.TobaccoPusher()));
		tList.add(new Thread(new CSP.MatchesPusher()));
		tList.add(new Thread(new CSP.PaperPusher()));
		tList.add(new Thread(new CSP.smokerMatches()));
		tList.add(new Thread(new CSP.smokerTobacco()));
		tList.add(new Thread(new CSP.smokerPaper()));
		
		for (Thread t : tList) {
			t.start();
		}
		try {
			for (Thread t : tList) {
				t.join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
