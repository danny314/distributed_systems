import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

// used to create a random set of strings used to
// for insertion, deletion, and lookup
class DNAStrings {

  static String data[];
  static Random rnd = new Random(0);
  static String bases[] = {"A", "C", "G", "T"};

  static String randomDna() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0 ; i < 10; i++ ) {
      sb.append(bases[rnd.nextInt(4)]);
    }
    return sb.toString();
  }

  public static void initDNAStrings(int n) {
    data = new String[n];
    for ( int i = 0 ; i < n; i++ ) {
      data[i] = DNAStrings.randomDna();
    }
  }

}

// performs ConcDS.N random operations on the shared cache
class Client implements Runnable {

  Random rnd = new Random();
  // dummy var, used to keep JVM from discarding calls to containsKey below
  int count;

  public void run() {
    for (int i = 0; i < ConcDS.N; i++ ) {
      int r = rnd.nextInt(100);
      int index =  rnd.nextInt( DNAStrings.data.length );
      // 2% puts
      if ( r <= 2 ) {
        ConcDS.cache.put( DNAStrings.data[ index ], DNAStrings.data[ index ] );
      // 1% remove
      } else if ( r <= 3 ) {
        ConcDS.cache.remove( DNAStrings.data[ index ] );
      } else {
        // 97% lookups
        if ( ConcDS.cache.containsKey( DNAStrings.data[ index ] ) ) {
          count++;
        }
      }
    }
  }
}

interface Cache {
  public void put(String k, String v);
  public void remove(String k);
  public boolean containsKey(String k);
  public int size();
}

// regular hashmap requires locking for each operation
class RegularHashMapCache implements Cache {

  Map<String,String> cache = new HashMap<String,String>();

  public void put(String k, String v) {
    synchronized (cache) {
      cache.put( k, v);
    }
  }

  public void remove(String k) {
    synchronized (cache) {
      cache.remove( k );
    }
  }

  public boolean containsKey(String k) {
    synchronized (cache) {
      return cache.containsKey(k);
    }
  }

  public int size() {
    synchronized (cache) {
      return cache.size();
    }
  }

}

class ConcurrentHashMapCache implements Cache {

	  Map<String,String> cache = new ConcurrentHashMap<String,String>();

	  public void put(String k, String v) {
	      cache.put( k, v);
	  }

	  public void remove(String k) {
	      cache.remove( k );
	  }

	  public boolean containsKey(String k) {
	      return cache.containsKey(k);
	  }

	  public int size() {
	      return cache.size();
	  }

  // DP class - implement the class
  // compose in a ConcurrentHashMap
    
}

public class ConcDS {

  static Cache cache;
  final static int N = 100000;
  final static int numStrings = 10000;
  final static int numThreads = 100;

  public static void main(String[] args) {
    DNAStrings.initDNAStrings(numStrings);

    cache = new RegularHashMapCache();
    benchmark(numThreads);

    cache = new ConcurrentHashMapCache();
    benchmark(numThreads);
  }

  // launches numThreads threads
  static void benchmark(int numThreads) {

    List<Thread> tList = new ArrayList<Thread>();

    for (int i = 0 ; i < numThreads; i++ ) {
      tList.add( new Thread( new Client() ) );
    }

    long start = System.nanoTime();
    for (Thread t : tList ) {
      t.start();
    }

    try {
      for (Thread t : tList ) {
        t.join();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    long finish = System.nanoTime();
    System.out.println("Total time (ms) = " + (finish - start)/1000000);
  }

}