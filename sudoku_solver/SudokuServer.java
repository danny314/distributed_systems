import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SudokuServer {
  static int PORT = -1;
  // no matter how many concurrent requests you get,
  // do not have more than three solvers running concurrently
  final static int MAXPARALLELTHREADS = 3;

  // this method exists for testing purposes, since
  // we want to clear out the singleton cache for 
  // subsequent junit tests.
  static void resetcache() {
	  SudokuSolver.CacheHolder.clearCache();
    //TODOBEGIN(DP)
    //TODOEND(DP)
  }

  public static void start(int portNumber ) throws IOException {
    PORT = portNumber;
    Runnable serverThread = new ThreadedSudokuServer();
    Thread t = new Thread( serverThread );
    t.start();
  }
}

//TODOBEGIN(DP)
class ThreadedSudokuServer implements Runnable {
  public void run() {
	  //Create thread pool executor
	  ExecutorService executor = Executors.newFixedThreadPool(SudokuServer.MAXPARALLELTHREADS); 
	  try {
		    ServerSocket serversock = new ServerSocket(SudokuServer.PORT);
	        for (;;) { // forever, accept connections
	        Socket sock = serversock.accept();
	        BufferedReader br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
	        PrintWriter dos = new PrintWriter(sock.getOutputStream());
	        String query = br.readLine();
	        //System.out.println(new Date() + " Received request = " + query.hashCode());
	        
	        Runnable worker = new SolverThread(br, dos,query);
	        //System.out.println(new Date() + " Launching worker thread for = " + query.hashCode());
	        executor.execute(worker);
	       }
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
  }
}

class SolverThread implements Runnable {
	BufferedReader br;
	PrintWriter pw;
	String input;
	public SolverThread(BufferedReader br, PrintWriter pw, String input) {
		this.br = br;
		this.pw = pw;
		this.input = input;
	}
	@Override
	public void run() {
		String solution = SudokuSolver.solve(input);
		pw.write(solution);
		pw.flush();
		try {
			br.close();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
//TODOEND(DP)
