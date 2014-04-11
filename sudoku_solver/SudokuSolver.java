import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SudokuSolver {

   // "Near worst case" example from
   // http://en.wikipedia.org/wiki/Sudoku_algorithms
  public static final String testCaseString =
				"153 " + "178 " + "185 " +
				 "221 " + "242 " +
			 	 "335 " + "357 " +
				 "424 " + "461 " +
				 "519 " +
				 "605 " + "677 " + "683 " +
				 "722 " + "741 " +
				 "844 " + "889 ";

  public static final int N = 9;
  
  //Lazy Init Holder Cache Class
  static class CacheHolder {
	  public static Map<String,String> cache = new ConcurrentHashMap<String, String>();
	  
	  //Use fair reentrant lock to read and write to cache
	  static ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
	  
	  public static void writeToCache(String puzzle, String solution) {
		  lock.writeLock().lock();
	        try {
	            getCache().put(puzzle, solution);
	        } finally {
	            lock.writeLock().unlock();
	        }	  
	   }

	  public static String readFromCache(String puzzle) {
		  lock.readLock().lock();
	        try {
	            return getCache().get(puzzle);
	        } finally {
	            lock.readLock().unlock();
	        }
	   }

	  public static void clearCache() {
		  lock.writeLock().lock();
	        try {
	            getCache().clear();
	        } finally {
	            lock.writeLock().unlock();
	        }	  
	   }
  }
  
  private static Map<String,String> getCache() {
	  return CacheHolder.cache;
  }

  public static void main(String[] args) {
    String result = solve( testCaseString );
    boolean correct = isLegalSolution( result, testCaseString );
      System.out.println("Your solver ouput is " + (correct ? "right" : "wrong"));
  }
    

  public static String solve( String initialConfig ) {
        String result = null;
	//TODOBEGIN(DP)
        
      String[] splitStr =   initialConfig.split("\\s");
      int [][] initialConfigMatrix = parsePuzzle( splitStr );

      //Check if in cache, return cached solution
      String cachedSolution = CacheHolder.readFromCache(initialConfig); 
      
      if (cachedSolution != null) {
    	  return cachedSolution;
      }
      
      if (solveSudoku(0,0,initialConfigMatrix))  { // solves in place
          result = matrixToString(initialConfigMatrix);
      }
      else {
    	  System.out.println("UNSOLVABLE");
    	  result = "UNSOLVABLE";
      }
      //Store the solution in cache for future use
      CacheHolder.writeToCache(initialConfig, result);
          
	//TODOEND(DP)
	return result;
  }
  
  static boolean solveSudoku(int i, int j, int[][] grid) {
      if (i == N) {
          i = 0;
          if (++j == N)
              return true;
      }
      if (grid[i][j] != 0)  
          return solveSudoku(i+1,j,grid);

      for (int val = 1; val <= N; ++val) {
          if (isLegalSolution(i,j,val,grid)) {
        	  grid[i][j] = val;
              if (solveSudoku(i+1,j,grid))
                  return true;
          }
      }
      grid[i][j] = 0; 
      return false;
  }  
  
  static boolean isLegalSolution(int i, int j, int val, int[][] grid) {
      for (int k = 0; k < N; ++k)  
          if (val == grid[k][j])
              return false;

      for (int k = 0; k < N; ++k) 
          if (val == grid[i][k])
              return false;

      int rowOffset = (i / 3)*3;
      int colOffset = (j / 3)*3;
      for (int k = 0; k < 3; ++k) 
          for (int m = 0; m < 3; ++m)
              if (val == grid[rowOffset+k][colOffset+m])
                  return false;

      return true;  //Return solution is legal
  }

  static int[][] parsePuzzle(String[] args) {
      int[][] problem = new int[N][N]; 
      for (int n = 0; n < args.length; ++n) {
          int i = Integer.parseInt(args[n].substring(0,1));
          int j = Integer.parseInt(args[n].substring(1,2));
          int val = Integer.parseInt(args[n].substring(2,3));
          problem[i][j] = val;
      }
      return problem;
  }

	//TODOBEGIN(DP)

	//TODOEND(DP)

  public static boolean isLegalSolution( String solution, String initialConfig ) {

    int [][] solutionmatrix = readInput( solution );
    int [][] initialConfigMatrix = readInput( initialConfig );

    if ( !isValid (solutionmatrix) ) {
      return false;
    }

    // check that it's fully filled in
    for (int i = 0; i < N; ++i) {
      for (int j = 0; j < N; ++j) {
        if ( solutionmatrix[i][j] == 0 
                 || solutionmatrix[i][j] < 0 
                 || solutionmatrix[i][j] > 9 ) {
          return false;
        }
        // check that it matches with initialConfigMatrix
        if ( initialConfigMatrix[i][j] != 0  &&
             initialConfigMatrix[i][j] != solutionmatrix[i][j] ) {
           return false;
         }
      }
    }
    return true;
  }

  // Check if a partially filled matrix has any conflicts
  public static boolean isValid(int[][] matrix) {
    // Row constraints
    for (int i = 0; i < N; ++i) {
      boolean[] present = new boolean[N + 1];
      for (int j = 0; j < N; ++j) {
        if (matrix[i][j] != 0 && present[matrix[i][j]]) {
          return false;
        } else {
          present[matrix[i][j]] = true;
        }
      }
    }

    // Column constraints
    for (int j = 0; j < N; ++j) {
      boolean[] present = new boolean[N + 1];
      for (int i = 0; i < N; ++i) {
        if (matrix[i][j] != 0 && present[matrix[i][j]]) {
          return false;
        } else {
          present[matrix[i][j]] = true;
        }
      }
    }

    // Region constraints
    for (int I = 0; I < 3; ++I) {
      for (int J = 0; J < 3; ++J) {
        boolean[] present = new boolean[N + 1];
        for (int i = 0; i < 3; ++i) {
          for (int j = 0; j < 3; ++j) {
            if (matrix[3 * I + i][3 * J + j] != 0 &&
                present[matrix[3 * I + i][3 * J + j]]) {
              return false;
            } else {
              present[matrix[3 * I + i][3 * J + j]] = true;
            }
          }
        }
      }
    }
    return true;
  }

  public static int[][] readInput( String arg ) {
    String [] args = arg.split("\\s");
    return readInput( args );
  }

   public static int[][] readInput( String[] args ) {
     int[][] result = new int[N][];
     for ( int k = 0 ; k < N; k++ ) {
       result[k] = new int[N];
     }
     for ( int k = 0 ; k < args.length; k++ ) {
       int val = new Integer( args[k] );
       // format: 634 -> in row 6, col 4 value is 4
       result[val / 100][(val % 100) / 10] = (val % 10);
     }
     return result;
   }

   static String matrixToString(int[][] matrix) {
     String result = "";
     for ( int i  = 0 ; i < matrix.length; i++ ) {
       for ( int j  = 0 ; j < matrix[0].length; j++ ) {
         result = result + i + j + matrix[i][j] + " ";
       }
     }
     return result;
   }


   static void print(String msg, int[][] matrix) {
     System.out.println(msg);
     for ( int i  = 0 ; i < matrix.length; i++ ) {
       for ( int j  = 0 ; j < matrix[0].length; j++ ) {
         System.out.print(matrix[i][j] + ( (j < 8) ? " " : "\n" ) );
       }
     }
   }
}
